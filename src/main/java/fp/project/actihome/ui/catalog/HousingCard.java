package fp.project.actihome.ui.catalog;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.ui.components.Chip;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.ScoreDisc;
import fp.project.actihome.ui.theme.Animacion;
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

	/** Cuánto sube la ficha al pasar el ratón, en puntos. */
	private static final int SUBIDA = 2;

	/** Cuánto está realzada, de 0 a 1. Mueve a la vez la subida y el zoom. */
	private transient double realce;

	private transient javax.swing.Timer animacion;

	/** La foto, guardada para poder acercarla desde el realce. */
	private transient ImagePlaceholder imagen;

	/** Alto de la foto de tarjeta. */
	private static final int ALTO_FOTO = 140;

	private final transient Housing housing;
	private final boolean disponible;
	private final boolean seleccionadoParaComparar;
	private final transient Consumer<Boolean> alCambiarComparacion;

	/**
	 * @param seleccionadoParaComparar si esta ficha ya está en la selección de
	 *                                  comparar (F16), para que el chip nazca en
	 *                                  el estado correcto tras un redibujado
	 * @param alCambiarComparacion     qué hacer al marcar o desmarcar el chip de
	 *                                  comparar
	 */
	public HousingCard(Housing housing, int resenas, boolean disponible, boolean seleccionadoParaComparar,
			Runnable alAbrir, Consumer<Boolean> alCambiarComparacion) {

		super(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.SM + "[]" + Space.XXS + "[]"
				+ Space.XS + "[]" + Space.XS + "[]" + Space.SM + "[]"));

		this.housing = housing;
		this.disponible = disponible;
		this.seleccionadoParaComparar = seleccionadoParaComparar;
		this.alCambiarComparacion = alCambiarComparacion;

		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		add(foto(), "h " + ALTO_FOTO + "!, growx");
		add(Labels.caps(Textos.t("catalogo.numero") + " " + housing.getHousingCode() + " · " + housing.getLocation()));
		add(nombre());
		add(Labels.muted(datos(resenas)));
		add(comparaChip());
		add(pie());

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				alAbrir.run();
			}
		});

		registrarRealce();
	}

	/**
	 * Chip para añadir o quitar esta ficha de la comparación (F16).
	 *
	 * <p>
	 * Va en su propia fila y no superpuesto a la foto: encima de una fotografía
	 * real el contorno fino del chip sin marcar —sin relleno propio— se leería
	 * distinto según lo clara u oscura que sea cada foto, y el sistema no tiene
	 * ningún fondo garantizado para ese caso. En su propia fila, sobre el fondo
	 * de la página, el contraste es el mismo que el de cualquier otro chip del
	 * catálogo.
	 */
	private JPanel comparaChip() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]", "[]"));
		fila.setOpaque(false);

		Chip comparar = new Chip(Textos.t("catalogo.comparar.chip"), seleccionadoParaComparar);
		comparar.addActionListener(e -> alCambiarComparacion.accept(comparar.isSelected()));
		fila.add(comparar);

		return fila;
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

		ImagePlaceholder imagen = new ImagePlaceholder(Textos.tipoDeAlojamiento(housing.getType()),
				disponible ? Textos.t("catalogo.disponibilidad.disponible") : Textos.t("catalogo.disponibilidad.reservada"),
				disponible, housing.getImage());

		imagen.setDestacado(Destacado.de(housing));
		this.imagen = imagen;

		capa.add(imagen, "pos 0 0 container.x2 container.y2");

		return capa;
	}

	/**
	 * Registra el realce: al pasar el ratón, la ficha entera sube y la foto se
	 * acerca.
	 *
	 * <p>
	 * <b>Un solo valor mueve las dos cosas</b>, y no dos animaciones en paralelo. Con
	 * temporizadores separados, dos gestos que deben leerse como uno acaban
	 * desincronizados —basta un fotograma perdido— y el ojo lo detecta antes de saber
	 * qué está viendo.
	 *
	 * <p>
	 * <b>El ratón se escucha en la ficha, no en la foto.</b> Leyendo el nombre y el
	 * precio el cursor está sobre el texto, que es justo el momento en que alguien
	 * está decidiendo; una foto que solo reaccionara a su propio hover se apagaría
	 * ahí.
	 */
	private void registrarRealce() {

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent e) {
				animarHacia(1);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				animarHacia(0);
			}
		});
	}

	private void animarHacia(double destino) {

		Animacion.cancelar(animacion);
		animacion = Animacion.animar(this, realce, destino, Animacion.CONTROL, v -> {

			realce = v;

			if (imagen != null) {
				imagen.setZoom(v);
			}

			// Se repinta el padre incluyendo la franja que la ficha deja libre al subir.
			// Sin eso queda un rastro de lo que había antes pegado al borde inferior.
			if (getParent() != null) {
				getParent().repaint(getX(), getY(), getWidth(), getHeight() + SUBIDA);
			}
		});
	}

	/**
	 * Sube la ficha entera, contenido incluido.
	 *
	 * <p>
	 * Se sobrescribe {@code paint} y no {@code paintComponent} porque hay que mover
	 * también a los hijos, y {@code paintComponent} solo pinta el fondo del propio
	 * panel. Trasladar el {@code Graphics} es además mucho más barato que cambiar el
	 * borde o la posición: no dispara ningún pase de layout, y aquí se hace sesenta
	 * veces por segundo.
	 */
	@Override
	public void paint(java.awt.Graphics g) {

		if (realce <= 0) {
			super.paint(g);
			return;
		}

		java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
		g2.translate(0, -(int) Math.round(SUBIDA * realce));

		super.paint(g2);

		g2.dispose();
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
		precio.setFont(Typography.sansSemiBold(Typography.PRICE));
		fila.add(precio, "aligny bottom");

		fila.add(Labels.muted(Textos.t("catalogo.card.porNoche")), "aligny bottom, gapbottom 3");
		fila.add(Labels.caps(Textos.t("catalogo.card.ver")), "aligny bottom, gapbottom 3");

		return fila;
	}
}
