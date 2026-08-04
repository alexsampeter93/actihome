package fp.project.actihome.ui.catalog;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.ScoreDisc;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Typography;

/**
 * Una tarjeta del catálogo en vista de cuadrícula.
 *
 * <p>
 * La misma estancia que dibuja {@link HousingRow}, reducida a lo que cabe en un
 * tercio de ancho: foto, puntuación, referencia, nombre, un par de datos y el
 * precio. Fuera quedan la pensión, las comodidades y el propietario.
 *
 * <p>
 * <b>Qué se cae y por qué.</b> No es un recorte arbitrario: se conserva lo que
 * sirve para <em>elegir a quién mirar</em> —foto, nombre, sitio, nota y precio—
 * y se retira lo que sirve para <em>decidir</em>, que se consulta en el detalle.
 * La cuadrícula es para barrer muchas opciones deprisa; la lista, para
 * compararlas.
 *
 * <p>
 * La puntuación pasa de barra a disco. En la fila hay sitio para una barra
 * horizontal junto a la cifra; aquí no, y el disco sobre la foto ocupa una
 * esquina que de otro modo estaría vacía.
 */
public class HousingCard extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Alto de la foto de tarjeta. */
	private static final int ALTO_FOTO = 140;

	private final transient Housing housing;
	private final boolean disponible;

	public HousingCard(Housing housing, int resenas, boolean disponible, Runnable alAbrir) {

		super(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.SM + "[]" + Space.XXS + "[]" + Space.XS + "[]" + Space.SM + "[]"));

		this.housing = housing;
		this.disponible = disponible;

		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		add(foto(), "h " + ALTO_FOTO + "!, growx");
		add(Labels.caps(Textos.t("catalogo.numero") + " " + housing.getHousingCode() + " · " + housing.getLocation()));
		add(nombre());
		add(Labels.muted(datos(resenas)));
		add(pie());

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				alAbrir.run();
			}
		});
	}

	/**
	 * La foto con el disco de puntuación encima.
	 *
	 * <p>
	 * Se superponen con posicionamiento absoluto de MigLayout: la foto ocupa la
	 * celda entera y el disco se ancla al borde derecho mediante
	 * {@code container.x2}, una expresión que MigLayout recalcula al redimensionar.
	 * Es la alternativa a un {@code JLayeredPane}, que para dos componentes obliga a
	 * gestionar los tamaños a mano.
	 */
	private JPanel foto() {

		JPanel capa = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]", "[grow,fill]"));
		capa.setOpaque(false);

		// El disco se añade ANTES que la foto, aunque vaya encima.
		//
		// Es una de esas cosas de Swing que solo se aprenden fallando: los hijos de un
		// contenedor se pintan del último índice al primero, así que **el que se añade
		// primero es el que queda arriba del todo**. Añadiendo la foto primero, el
		// disco quedaba pintado debajo y era invisible.
		if (housing.getScore() != null) {
			capa.add(new ScoreDisc(housing.getScore(), ScoreDisc.Tamano.PEQUENO), "pos (container.x2-42) 12");
		}

		capa.add(new ImagePlaceholder(Textos.tipoDeAlojamiento(housing.getType()),
				disponible ? Textos.t("catalogo.disponibilidad.disponible") : Textos.t("catalogo.disponibilidad.reservada"),
				disponible, housing.getImage()), "pos 0 0 container.x2 container.y2");

		return capa;
	}

	private JLabel nombre() {

		JLabel etiqueta = Labels.cardTitle(housing.getName());
		etiqueta.setFont(Typography.serifMedium(Typography.CARD_TITLE));
		return etiqueta;
	}

	private String datos(int resenas) {

		String notas = resenas == 0 ? Textos.t("catalogo.sinResenas")
				: Formato.plural(resenas, Textos.t("palabra.resena.singular"), Textos.t("palabra.resena.plural"));

		return notas + " · " + Formato.plural(housing.getNumberOfRooms(), Textos.t("palabra.habitacion.singular"),
				Textos.t("palabra.habitacion.plural"));
	}

	private JPanel pie() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XS + "[]push[]", "[]"));
		fila.setOpaque(false);

		JLabel precio = Labels.price(Formato.precioCorto(housing.getPricePerNight()));
		precio.setFont(Typography.serif(Typography.PRICE));
		fila.add(precio, "aligny bottom");

		fila.add(Labels.muted(Textos.t("catalogo.card.porNoche")), "aligny bottom, gapbottom 3");
		fila.add(Labels.caps(Textos.t("catalogo.card.ver")), "aligny bottom, gapbottom 3");

		return fila;
	}
}
