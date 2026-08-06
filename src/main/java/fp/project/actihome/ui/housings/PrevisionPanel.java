package fp.project.actihome.ui.housings;

import java.awt.Dimension;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.exceptions.WeatherUnavailableException;
import fp.project.actihome.model.services.PrevisionDiaria;
import fp.project.actihome.model.services.WeatherService;
import fp.project.actihome.ui.components.IconoDelCielo;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;

/**
 * El tiempo previsto en el alojamiento, en la ficha de detalle (F18).
 *
 * <p>
 * <b>Por qué esto está en una aplicación de alojamientos.</b> Porque el producto
 * entero está organizado por estaciones —el selector cambia la paleta, y cada
 * alojamiento declara en cuál luce más— y decir qué tiempo hará de verdad
 * convierte esa idea en información útil en lugar de decorativa. Para elegir una
 * cabaña de montaña importa si va a nevar.
 *
 * <p>
 * <b>Todo en esta clase está construido para desaparecer sin ruido.</b> Es la
 * regla que gobierna cualquier dato que venga de fuera:
 * <ul>
 * <li>Si el alojamiento no tiene coordenadas, el panel no se construye siquiera.
 * No hay hueco vacío ni "sin datos": es información que nadie ha echado en
 * falta.</li>
 * <li>Si la consulta falla —sin red, el proveedor caído, un tiempo de espera
 * agotado—, el panel se oculta. <b>No se enseña un mensaje de error</b>: nadie
 * pidió esta información, así que fallar al obtenerla no es un problema del
 * usuario, y un aviso rojo por no poder decir el tiempo sería una interrupción
 * gratuita.</li>
 * <li>Mientras se espera se ve una línea discreta, no un bloqueo. La ficha está
 * completa y usable desde el primer instante; el tiempo llega después si
 * llega.</li>
 * </ul>
 *
 * <p>
 * <b>La consulta va en un {@code SwingWorker}</b>, la única excepción al modelo
 * de un solo hilo del proyecto y justificada por el tipo de espera: una consulta
 * a H2 tarda microsegundos, una petición HTTP puede no contestar nunca. En el
 * hilo de la interfaz, esa espera congelaría la ventana entera.
 */
public class PrevisionPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/**
	 * Cuántos días se enseñan.
	 *
	 * <p>
	 * Cinco y no dieciséis, que es lo que el proveedor daría. Más allá de una
	 * semana la previsión pierde fiabilidad rápidamente, así que enseñar dieciséis
	 * días sería aparentar una precisión que el dato no tiene. Y cinco columnas es
	 * lo que cabe cómodamente bajo la galería sin robarle sitio.
	 */
	private static final int DIAS = 5;

	/** Lado del icono, en puntos. Se pasa al dibujo, que escala solo. */
	private static final int ICONO = 30;

	private final transient WeatherService weatherService;
	private final transient Housing housing;

	private final JLabel estado;
	private final JPanel tira;

	/**
	 * @param housing        el alojamiento; si no está localizado, este panel se
	 *                       queda invisible para siempre
	 * @param weatherService de dónde sale la previsión
	 */
	public PrevisionPanel(Housing housing, WeatherService weatherService) {

		super(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XS + "[]"));

		this.housing = housing;
		this.weatherService = weatherService;

		setOpaque(false);

		estado = Labels.muted(Textos.t("detalle.tiempo.consultando"));
		add(estado);

		tira = new JPanel(new MigLayout(Space.insets(0), "", "[]"));
		tira.setOpaque(false);
		tira.setVisible(false);
		add(tira);

		// **El mínimo cero es lo que permite que la columna izquierda se encoja.** Sin
		// él, cinco columnas de texto fijan un suelo que en una ventana de 1024 empuja
		// el precio y el botón de reservar fuera de la pantalla, que es exactamente el
		// fallo que se corrigió en la galería. Un adorno informativo nunca puede ser lo
		// que decide el ancho mínimo de una pantalla.
		setMinimumSize(new Dimension(0, 0));

		if (!housing.estaLocalizado()) {
			setVisible(false);
			return;
		}

		consultar();
	}

	/**
	 * Pide la previsión sin bloquear la interfaz.
	 *
	 * <p>
	 * <b>Se lanza una sola vez, desde el constructor, y no en cada
	 * {@code setVisible}.</b> Es la regla del proyecto —construir va en el
	 * constructor, refrescar va en {@code setVisible}— y aquí tiene además una
	 * consecuencia práctica: este panel se crea de nuevo cada vez que se abre una
	 * ficha, así que consultar en {@code setVisible} sería consultar dos veces por
	 * visita. La caché del servicio absorbe las repeticiones, pero no hay que
	 * apoyarse en ella para tapar una llamada de más.
	 */
	private void consultar() {

		new SwingWorker<List<PrevisionDiaria>, Void>() {

			@Override
			protected List<PrevisionDiaria> doInBackground() throws WeatherUnavailableException {
				return weatherService.previsionDe(housing.getLatitude(), housing.getLongitude(), DIAS);
			}

			@Override
			protected void done() {

				try {
					pintar(get());

				} catch (InterruptedException ex) {
					// Reponer la marca antes de salir: este worker se puede cancelar al navegar a
					// otra pantalla, y tragarse la interrupción deja al hilo creyendo que nadie le
					// ha pedido parar.
					Thread.currentThread().interrupt();
					setVisible(false);

				} catch (ExecutionException ex) {
					// Sin red o proveedor caído. El panel entero se retira: la ficha estaba
					// completa sin él y lo sigue estando.
					setVisible(false);
				}
			}
		}.execute();
	}

	private void pintar(List<PrevisionDiaria> dias) {

		if (dias.isEmpty()) {
			setVisible(false);
			return;
		}

		estado.setText(Textos.t("detalle.tiempo.titulo"));
		tira.removeAll();

		for (PrevisionDiaria dia : dias) {
			tira.add(columna(dia), "gapright " + Space.MD);
		}

		tira.setVisible(true);
		revalidate();
		repaint();
	}

	/** Un día: su nombre, el icono y las dos temperaturas. */
	private JPanel columna(PrevisionDiaria dia) {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[center]", "[]2[]2[]"));
		panel.setOpaque(false);

		panel.add(Labels.caps(nombreDelDia(dia.fecha())), "alignx center");
		panel.add(new IconoDelCielo(dia.cielo(), ICONO), "alignx center");
		panel.add(Labels.muted(grados(dia.maxima()) + " / " + grados(dia.minima())), "alignx center");

		return panel;
	}

	/**
	 * "HOY", o las tres primeras letras del día.
	 *
	 * <p>
	 * <b>El idioma sale de {@code Textos} y no del sistema.</b> {@code Locale}
	 * traduce solo los nombres de los días, así que dejarlo en el idioma del
	 * sistema pondría "MON" junto a un texto en español en cuanto alguien abriera
	 * la aplicación en un Windows en inglés. Es exactamente la clase de mezcla que
	 * la Fase 7.6 fue a eliminar: <b>o está todo en un idioma, o el trabajo de
	 * traducir no ha servido de nada</b>.
	 */
	private String nombreDelDia(LocalDate fecha) {

		if (fecha.equals(LocalDate.now())) {
			return Textos.t("detalle.tiempo.hoy");
		}

		// Textos.idioma() es el que el usuario eligió en Ajustes, que es el único que
		// vale aquí.
		return fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, Textos.idioma());
	}

	/**
	 * Redondea al grado.
	 *
	 * <p>
	 * El proveedor da un decimal —"28.4"— y ese decimal es ruido: nadie decide un
	 * viaje por cuatro décimas, y la propia previsión no tiene esa precisión a
	 * cinco días vista. Enseñar más cifras de las que el dato aguanta es aparentar
	 * exactitud.
	 */
	private String grados(double temperatura) {
		return Math.round(temperatura) + "°";
	}
}
