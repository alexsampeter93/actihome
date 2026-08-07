package fp.project.actihome.ui.catalog;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.Collections;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Amenity;
import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.ui.components.Avatar;
import fp.project.actihome.ui.components.Chip;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.components.IconoDeComodidad;
import fp.project.actihome.ui.components.InlineScore;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.theme.Animacion;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;
import fp.project.actihome.ui.components.FilaFluida;

/**
 * Una fila del catálogo en vista de lista.
 *
 * <p>
 * Foto grande a la izquierda —el 46 % del ancho— y toda la información a la
 * derecha, en un orden que no es casual: <b>número y ubicación · nombre · nota ·
 * pensión · comodidades · precio y acciones</b>. Va de lo que sitúa a lo que
 * decide. El precio queda abajo del todo, junto a las acciones, porque es el
 * último dato que se mira antes de pulsar.
 *
 * <p>
 * <b>Por qué una fila ancha y no una rejilla de tarjetas pequeñas.</b> Las dos
 * vistas existen y el usuario elige, pero la lista es la que llega por defecto:
 * en una fila caben la puntuación, la pensión, las comodidades y el propietario
 * —lo que de verdad hace falta para descartar— mientras que en una tarjeta
 * pequeña solo cabe una foto y un precio, y obliga a entrar en cada una para
 * comparar. En una aplicación de escritorio, donde sobra ancho, la lista
 * aprovecha mejor la pantalla.
 *
 * <p>
 * <b>Toda la fila es pulsable</b>, no solo el enlace "Ver estancia". Obligar a
 * acertar en un texto de setenta píxeles cuando hay una fila de cuatrocientos es
 * gratuitamente difícil.
 */
public class HousingRow extends JPanel {

	private static final long serialVersionUID = 1L;

	/** La foto, guardada para poder acercarla al pasar el raton por la fila. */
	private transient ImagePlaceholder imagen;

	/** Cuanto esta realzada la fila, de 0 a 1: mueve el zoom y los iconos. */
	private transient double realce;

	/** Los iconos de comodidad, para encenderlos desde el realce. */
	private final transient java.util.List<IconoDeComodidad> iconos = new java.util.ArrayList<>();

	private transient javax.swing.Timer animacion;

	/**
	 * Alto de la foto.
	 *
	 * <p>
	 * El handoff pide 290px. Aquí son 200, y es una desviación consciente: el mockup
	 * es una página web, que se recorre con la rueda y puede permitirse filas
	 * enormes. En una ventana de escritorio de 840px de alto, con cabecera, hero y
	 * colofón fijos, una fila de 290 deja <b>una sola estancia visible</b>, y un
	 * catálogo en el que no se pueden comparar dos alojamientos sin hacer scroll no
	 * es un catálogo. Con 200 caben dos, y tres al maximizar.
	 *
	 * <p>
	 * La proporción resultante —unos 480×200— es panorámica, que es además la
	 * proporción natural de una foto de alojamiento: el paisaje o la fachada entran
	 * mejor apaisados que cuadrados.
	 */
	private static final int ALTO_FOTO = 190;

	/** Cuántas comodidades se enseñan antes de resumir el resto con un "+N". */
	private static final int MAXIMO_COMODIDADES = 3;

	private final transient Housing housing;
	private final boolean disponible;
	private final boolean seleccionadoParaComparar;
	private final transient Consumer<Boolean> alCambiarComparacion;

	/**
	 * @param resenas    cuántas reseñas tiene, ya contadas por quien construye la
	 *                   lista
	 * @param disponible si el alojamiento tiene una estancia en curso ahora mismo,
	 *                   ya calculado por quien construye la lista
	 * @param seleccionadoParaComparar si esta fila ya está en la selección de
	 *                   comparar (F16), para que el chip nazca en el estado
	 *                   correcto tras un redibujado
	 * @param alAbrir    qué hacer al pulsar la fila
	 * @param alIntercambiar acción de intercambio, o {@code null} si no procede para
	 *                   este usuario
	 * @param alCambiarComparacion qué hacer al marcar o desmarcar el chip de
	 *                   comparar
	 */
	public HousingRow(Housing housing, int resenas, boolean disponible, boolean seleccionadoParaComparar,
			Runnable alAbrir, Runnable alIntercambiar, Consumer<Boolean> alCambiarComparacion) {

		super(new MigLayout(Space.insets(Space.LG, 0, Space.LG, 0), "[46%:46%:46%]" + Space.HUGE + "[grow,fill]",
				"[]"));

		this.housing = housing;
		this.disponible = disponible;
		this.seleccionadoParaComparar = seleccionadoParaComparar;
		this.alCambiarComparacion = alCambiarComparacion;

		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		add(foto(), "h " + ALTO_FOTO + "!, growx");
		add(informacion(resenas, alIntercambiar), "aligny top");

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				alAbrir.run();
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				realzar(1);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				realzar(0);
			}
		});
	}

	private ImagePlaceholder foto() {

		ImagePlaceholder imagen = new ImagePlaceholder(Textos.tipoDeAlojamiento(housing.getType()),
				disponible ? Textos.t("catalogo.disponibilidad.disponible") : Textos.t("catalogo.disponibilidad.reservada"),
				disponible, housing.getImage());

		imagen.setDestacado(Destacado.de(housing));
		this.imagen = imagen;

		return imagen;
	}

	/**
	 * Acerca la foto al pasar el ratón por la fila.
	 *
	 * <p>
	 * <b>Aquí la fila NO se eleva, a diferencia de la ficha de cuadrícula</b>, y la
	 * diferencia es de forma, no de capricho. Una tarjeta es un objeto suelto y
	 * levantarla se lee como coger una carta de una baraja; una fila ancha separada
	 * de las de arriba y abajo por una línea fina es parte de una lista continua, y
	 * verla despegarse de sus vecinas se lee como un fallo de pintado. Lo que sí
	 * funciona en las dos es la foto: es la parte que se comporta como una imagen y
	 * no como una superficie.
	 */
	private void realzar(double destino) {

		Animacion.cancelar(animacion);
		animacion = Animacion.animar(this, realce, destino, Animacion.CONTROL, v -> {

			realce = v;

			if (imagen != null) {
				imagen.setZoom(v);
			}

			// Los iconos se encienden ESCALONADOS, cada uno con un poco de retraso sobre
			// el anterior. Encendiéndose los tres a la vez el efecto es un parpadeo; en
			// cascada se lee como una lectura, que es lo que el ojo está haciendo.
			for (int i = 0; i < iconos.size(); i++) {

				double propio = Math.max(0, Math.min(1, (v - i * 0.12) / 0.7));
				iconos.get(i).setEncendido(propio);
			}
		});
	}

	private JPanel informacion(int resenas, Runnable alIntercambiar) {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.XS + "[]" + Space.SM + "[]" + Space.SM + "[]" + Space.SM + "[]push[]"));
		panel.setOpaque(false);

		panel.add(referencia());
		panel.add(nombre());
		panel.add(valoracion(resenas));
		panel.add(pension());
		panel.add(comodidades());
		panel.add(pie(alIntercambiar));

		return panel;
	}

	/** "Nº 10001 —— Sierra Nevada, Granada", con el chip de comparar al otro extremo. */
	private FilaFluida referencia() {

		// FilaFluida y no una fila rigida: cuatro elementos de ancho variable -y uno de
		// ellos la ubicacion, que puede ser "Marbella, Malaga"- exigian la SUMA de todos
		// sus anchos, y esa suma no cabia en una ventana de 1024. Ahora exige solo el
		// mas ancho y dobla en dos lineas cuando hace falta. Es la regla numero 1 de la
		// adaptabilidad del proyecto, que a esta fila no se le habia aplicado.
		FilaFluida fila = new FilaFluida(Space.SM, Space.XS);

		JLabel numero = Labels.capsAccent(Textos.t("catalogo.numero") + " " + housing.getHousingCode());
		fila.add(numero);

		// El guion largo doble separa la referencia de la ubicación sin gritar. Es un
		// recurso editorial: una raya fina hace el mismo trabajo que un punto medio
		// pero con menos ruido.
		fila.add(new Raya(), "w 26!, h 1!, aligny center");
		fila.add(Labels.caps(housing.getLocation()));

		Chip comparar = new Chip(Textos.t("catalogo.comparar.chip"), seleccionadoParaComparar);
		comparar.addActionListener(e -> alCambiarComparacion.accept(comparar.isSelected()));
		fila.add(comparar, "aligny center");

		return fila;
	}

	private JLabel nombre() {

		JLabel etiqueta = Labels.cardTitle(housing.getName());
		etiqueta.setFont(Typography.serifMedium(Typography.CARD_TITLE_LG));
		return etiqueta;
	}

	/** Nota, barra fina, número de reseñas, habitaciones y propietario. */
	private FilaFluida valoracion(int resenas) {

		FilaFluida fila = new FilaFluida(Space.MD, Space.XS);

		fila.add(new InlineScore(housing.getScore(), 19f, 64));

		fila.add(Labels.muted(resenas == 0 ? Textos.t("catalogo.sinResenas")
				: Formato.plural(resenas, Textos.t("palabra.resena.singular"), Textos.t("palabra.resena.plural"))));
		fila.add(Labels.muted(Formato.plural(housing.getNumberOfRooms(), Textos.t("palabra.habitacion.singular"),
				Textos.t("palabra.habitacion.plural"))));
		fila.add(anfitrion(), "aligny center");

		return fila;
	}

	/**
	 * El anfitrión, con su avatar de iniciales delante (Fase 8.4).
	 *
	 * <p>
	 * Sustituye al texto suelto "de Marcos". El nombre sigue estando, así que el
	 * avatar no añade información — lo que añade es <b>una persona</b>: en un
	 * listado de alojamientos, un disco con iniciales convierte al propietario en
	 * alguien concreto en lugar de en un dato más de la ficha, que es exactamente
	 * lo que distingue a un alojamiento de particular de una habitación de hotel.
	 */
	private JPanel anfitrion() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XS + "[]", "[]"));
		fila.setOpaque(false);

		fila.add(Avatar.relleno(housing.getOwner().getName(), housing.getOwner().getSurname(), 22),
				"w 22!, h 22!, aligny center");
		fila.add(Labels.muted(Textos.t("catalogo.row.de", housing.getOwner().getUsername())), "aligny center");

		return fila;
	}

	/**
	 * Línea de pensión: lo incluido en texto normal, lo no incluido tachado.
	 *
	 * <p>
	 * Tachar en vez de ocultar es una decisión de diseño con fondo: si solo se
	 * listara lo incluido, "Desayuno" a secas no dice si las otras dos comidas no
	 * existen o si nadie las ha rellenado. Con las tres siempre a la vista y dos
	 * tachadas, la ausencia es información y no un hueco.
	 */
	private FilaFluida pension() {

		FilaFluida fila = new FilaFluida(Space.SM, Space.XS);

		fila.add(Labels.caps(Textos.t("catalogo.row.pension")));
		fila.add(comida(Textos.t("catalogo.row.desayuno"), housing.isBreakfast()));
		fila.add(comida(Textos.t("catalogo.row.comida"), housing.isLunch()));
		fila.add(comida(Textos.t("catalogo.row.cena"), housing.isDinner()));

		return fila;
	}

	private JLabel comida(String texto, boolean incluida) {

		if (incluida) {
			return Labels.body(texto);
		}

		JLabel etiqueta = Labels.muted(texto);

		// STRIKETHROUGH es un atributo de la fuente, no una propiedad del JLabel: en
		// Swing el tachado se pide a la tipografía. La alternativa sería etiquetar en
		// HTML, que se lleva mal con las fuentes registradas en tiempo de ejecución.
		etiqueta.setFont(etiqueta.getFont()
				.deriveFont(Collections.singletonMap(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON)));

		return etiqueta;
	}

	/**
	 * Las comodidades, <b>como mucho tres</b> y con el resto contado (Fase 8.4).
	 *
	 * <p>
	 * Antes se pintaban todas las que tuviera el alojamiento. Con siete
	 * disponibles, las fichas más completas sacaban siete chips en fila y ocurría
	 * lo contrario de lo que se buscaba: cuando todo está destacado, nada lo está,
	 * y una ficha con siete chips se lee peor que una con tres aunque ofrezca más.
	 * Además hacía que dos fichas seguidas tuvieran alturas muy distintas y la
	 * lista se viera irregular.
	 *
	 * <p>
	 * El resto no se esconde, se cuenta ("+2"). Es la diferencia entre resumir y
	 * ocultar: quien vea el "+2" sabe que hay más y que la ficha completa se lo
	 * dirá. {@link Amenity#values()} tiene un orden fijo, así que las tres que se
	 * enseñan son siempre las mismas para un alojamiento dado — un listado que
	 * cambia de contenido al recargarlo se lee como ruido.
	 */
	private JPanel comodidades() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "", ""));
		fila.setOpaque(false);

		int pintadas = 0;
		int restantes = 0;

		for (Amenity amenity : Amenity.values()) {

			if (!amenity.presenteEn(housing)) {
				continue;
			}

			if (pintadas < MAXIMO_COMODIDADES) {
				fila.add(comodidad(amenity), "gapright " + Space.MD);
				pintadas++;
			} else {
				restantes++;
			}
		}

		if (restantes > 0) {
			fila.add(Labels.muted("+" + restantes), "gapright " + Space.XS);
		}

		if (pintadas == 0) {
			fila.add(Labels.muted(Textos.t("catalogo.row.sinComodidades")));
		}

		return fila;
	}

	/**
	 * Un icono dibujado más su etiqueta.
	 *
	 * <p>
	 * <b>Sustituye al chip de texto que había antes</b>, y el cambio es más que
	 * decorativo. Tres chips seguidos —cada uno con su contorno redondeado— son tres
	 * cajas compitiendo en una fila que ya lleva nota, pensión, precio y dos
	 * acciones; el contorno no aportaba nada porque estas comodidades no son
	 * pulsables ni seleccionables, solo se leen. Un icono y una palabra dicen lo
	 * mismo con la mitad de tinta, y además se reconocen sin leer.
	 *
	 * <p>
	 * El icono se guarda en una lista para poder encenderlo desde el realce de la
	 * fila entera: ver {@link #realzar(double)}.
	 */
	private JPanel comodidad(Amenity amenity) {

		JPanel grupo = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XXS + "[]", "[]"));
		grupo.setOpaque(false);

		IconoDeComodidad icono = new IconoDeComodidad(amenity);
		iconos.add(icono);

		grupo.add(icono, "aligny center");
		grupo.add(Labels.muted(Textos.etiquetaDe(amenity)), "aligny center");

		return grupo;
	}

	private JPanel pie(Runnable alIntercambiar) {

		// "por noche" va pegado al precio y no empujado al otro extremo: es su unidad
		// de medida, no un dato aparte. Lo que se separa hacia la derecha es la acción.
		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XS + "[]push[]", "[]"));
		fila.setOpaque(false);

		JLabel precio = Labels.price(Formato.precioCorto(housing.getPricePerNight()));
		precio.setFont(Typography.serif(28f));
		fila.add(precio, "aligny bottom");

		fila.add(Labels.muted(Textos.t("catalogo.card.porNoche")), "aligny bottom, gapbottom 4");

		if (alIntercambiar != null) {
			fila.add(enlace(Textos.t("catalogo.row.intercambiar"), true, alIntercambiar), "aligny bottom, gapbottom 4");
		}

		return fila;
	}

	/**
	 * Enlace de acción dentro de la fila.
	 *
	 * <p>
	 * Se construye a mano en lugar de con {@code Buttons.link} porque la fila entera
	 * ya responde al clic: si estos fueran botones de verdad, el clic del botón y el
	 * de la fila competirían y habría que ir cancelando la propagación en cada uno.
	 */
	private JLabel enlace(String texto, boolean acento, Runnable accion) {

		JLabel etiqueta = acento ? Labels.price(texto) : Labels.body(texto);
		etiqueta.setFont(Typography.sansSemiBold(Typography.BODY_SM));
		etiqueta.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		etiqueta.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				e.consume();
				accion.run();
			}
		});

		return etiqueta;
	}

	/** Raya fina de separación dentro de una línea de texto. */
	private static class Raya extends JComponent {

		private static final long serialVersionUID = 1L;

		@Override
		protected void paintComponent(Graphics g) {

			g.setColor(Theme.mut());
			g.fillRect(0, getHeight() / 2, getWidth(), 1);
		}
	}
}
