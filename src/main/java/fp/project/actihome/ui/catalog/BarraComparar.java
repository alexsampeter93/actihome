package fp.project.actihome.ui.catalog;

import java.awt.Window;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.Toast;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;

/**
 * La selección de alojamientos a comparar, y la barra flotante que la anuncia
 * (F16).
 *
 * <p>
 * <b>Es una sola clase y no dos, y esa es la decisión que la justifica.</b> El
 * conjunto de identificadores seleccionados y la barra que lo enseña son la
 * misma cosa contada dos veces: la barra existe <em>porque</em> hay selección, y
 * su texto es el tamaño del conjunto. Mientras vivían separados en
 * {@code ShowHousingsFrame} —un {@code Set} por un lado, cuatro campos de widget
 * por otro y tres métodos para mantenerlos sincronizados— cualquiera podía tocar
 * uno y olvidar el otro. Aquí es imposible: quien cambia la selección pasa por
 * este objeto y la barra se entera sola.
 *
 * <p>
 * Reutiliza el idioma visual de {@link Toast} —bloque sólido con
 * {@code Theme.hdr()} y texto claro— en vez de inventar una superficie nueva: es
 * la única pieza del sistema pensada para flotar sobre el contenido con fondo
 * propio.
 */
public class BarraComparar extends JPanel {

	private static final long serialVersionUID = 1L;

	/**
	 * Cuántos se pueden comparar a la vez.
	 *
	 * <p>
	 * Tres, y no es una limitación técnica: cuatro columnas de atributos ya no
	 * caben cómodamente en una ventana de escritorio, y comparar seis cosas a la
	 * vez no es comparar, es volver a leer un catálogo.
	 */
	public static final int MAXIMO = 3;

	/** Por debajo de dos no hay nada que comparar, así que la barra no aparece. */
	private static final int MINIMO_PARA_ENSENAR = 2;

	/**
	 * Los seleccionados, en el orden en que se marcaron.
	 *
	 * <p>
	 * {@code LinkedHashSet} y no {@code HashSet}: la pantalla de comparación pone
	 * una columna por alojamiento, y si el orden cambiara entre visitas las
	 * columnas bailarían sin motivo. El orden en que se marcaron es el único que
	 * el usuario puede predecir.
	 */
	private final transient Set<Long> seleccion = new LinkedHashSet<>();

	private final transient Runnable repintarLista;

	private final JLabel etiqueta;
	private final JButton boton;
	private final JLabel enlaceCancelar;

	/**
	 * @param repintarLista qué hacer cuando la selección cambie de una forma que la
	 *                      lista tenga que reflejar
	 * @param alComparar    qué hacer al pulsar "Comparar", con los identificadores
	 *                      elegidos
	 */
	public BarraComparar(Runnable repintarLista, java.util.function.Consumer<List<Long>> alComparar) {

		super(new MigLayout(Space.insets(Space.SM, Space.LG, Space.SM, Space.LG),
				"[]" + Space.MD + "[]" + Space.MD + "[]", "[]"));

		this.repintarLista = repintarLista;

		setOpaque(true);
		setBackground(Theme.hdr());
		setVisible(false);

		etiqueta = Labels.onHeader("");
		add(etiqueta, "aligny center");

		boton = Buttons.primary(Textos.t("catalogo.comparar.boton"),
				e -> alComparar.accept(new ArrayList<>(seleccion)));
		add(boton, "aligny center");

		enlaceCancelar = Labels.onHeader(Textos.t("catalogo.comparar.cancelar"));
		enlaceCancelar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		enlaceCancelar.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				limpiar();
				repintarLista.run();
			}
		});
		add(enlaceCancelar, "aligny center");
	}

	/**
	 * Si este alojamiento está marcado. Lo pregunta cada fila al pintarse.
	 *
	 * <p>
	 * <b>No se llama {@code contains}, y el compilador tuvo razón al impedirlo.</b>
	 * Esta clase extiende {@code JPanel}, y {@code Component} ya tiene un
	 * {@code contains(Point)} que responde a otra pregunta completamente distinta —
	 * si unas coordenadas caen dentro del componente—. Llamarlos igual habría
	 * compilado en algunos usos y no en otros, con un mensaje de error que habla de
	 * {@code Point} cuando el problema es de nombre.
	 */
	public boolean estaSeleccionado(Long housingId) {
		return seleccion.contains(housingId);
	}

	/**
	 * Marca o desmarca un alojamiento.
	 *
	 * <p>
	 * <b>Rechazar el cuarto exige repintar la lista entera</b>, y conviene saber
	 * por qué: el chip ya se dibujó marcado en cuanto el usuario lo pulsó —así es
	 * como responde un {@code JToggleButton}— y la única forma de devolverlo a su
	 * estado real, sin guardar una referencia al chip concreto, es reconstruir la
	 * fila desde el estado que sí es la fuente de verdad, que es este conjunto.
	 *
	 * @param origen       la ventana sobre la que se enseña el aviso de límite
	 * @param housing      el alojamiento pulsado
	 * @param seleccionado el estado al que acaba de pasar el chip
	 */
	public void alternar(Window origen, Housing housing, boolean seleccionado) {

		if (seleccionado) {

			if (seleccion.size() >= MAXIMO) {

				Toast.mostrar(origen, Textos.t("catalogo.comparar.limite.prefijo") + " " + MAXIMO + " "
						+ Textos.t("catalogo.comparar.limite.sufijo"));

				repintarLista.run();
				return;
			}

			seleccion.add(housing.getId());

		} else {
			seleccion.remove(housing.getId());
		}

		actualizar();
	}

	/** Vacía la selección y esconde la barra. No repinta la lista: eso lo decide quien llama. */
	public void limpiar() {

		seleccion.clear();
		actualizar();
	}

	/** Pone la barra al día: si se ve, y con qué texto. */
	public void actualizar() {

		boolean visible = seleccion.size() >= MINIMO_PARA_ENSENAR;
		setVisible(visible);

		if (visible) {
			etiqueta.setText(seleccion.size() + " " + Textos.t("catalogo.comparar.seleccionados"));
		}
	}

	/** Reescribe lo que depende del idioma (Fase 7.6). */
	public void actualizarTextos() {

		boton.setText(Textos.t("catalogo.comparar.boton"));
		enlaceCancelar.setText(Textos.t("catalogo.comparar.cancelar"));

		actualizar();
	}
}
