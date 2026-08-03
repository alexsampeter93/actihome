package fp.project.actihome.ui.catalog;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.Collections;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Amenity;
import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.ui.components.Chip;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.components.InlineScore;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

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

	private final transient Housing housing;
	private final boolean disponible;

	/**
	 * @param resenas    cuántas reseñas tiene, ya contadas por quien construye la
	 *                   lista
	 * @param disponible si el alojamiento tiene una estancia en curso ahora mismo,
	 *                   ya calculado por quien construye la lista
	 * @param alAbrir    qué hacer al pulsar la fila
	 * @param alIntercambiar acción de intercambio, o {@code null} si no procede para
	 *                   este usuario
	 */
	public HousingRow(Housing housing, int resenas, boolean disponible, Runnable alAbrir, Runnable alIntercambiar) {

		super(new MigLayout(Space.insets(Space.LG, 0, Space.LG, 0), "[46%:46%:46%]" + Space.HUGE + "[grow,fill]",
				"[]"));

		this.housing = housing;
		this.disponible = disponible;

		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		add(foto(), "h " + ALTO_FOTO + "!, growx");
		add(informacion(resenas, alIntercambiar), "aligny top");

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				alAbrir.run();
			}
		});
	}

	private ImagePlaceholder foto() {

		return new ImagePlaceholder(housing.getType(), disponible ? "Disponible" : "Reservada", disponible,
				housing.getImage());
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

	/** "Nº 10001 —— Sierra Nevada, Granada" */
	private JPanel referencia() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.SM + "[]" + Space.SM + "[]", ""));
		fila.setOpaque(false);

		JLabel numero = Labels.capsAccent("Nº " + housing.getHousingCode());
		fila.add(numero);

		// El guion largo doble separa la referencia de la ubicación sin gritar. Es un
		// recurso editorial: una raya fina hace el mismo trabajo que un punto medio
		// pero con menos ruido.
		fila.add(new Raya(), "w 26!, h 1!, aligny center");
		fila.add(Labels.caps(housing.getLocation()));

		return fila;
	}

	private JLabel nombre() {

		JLabel etiqueta = Labels.cardTitle(housing.getName());
		etiqueta.setFont(Typography.serifMedium(Typography.CARD_TITLE_LG));
		return etiqueta;
	}

	/** Nota, barra fina, número de reseñas, habitaciones y propietario. */
	private JPanel valoracion(int resenas) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0),
				"[]" + Space.MD + "[]" + Space.MD + "[]" + Space.MD + "[]", ""));
		fila.setOpaque(false);

		fila.add(new InlineScore(housing.getScore(), 19f, 64));

		fila.add(Labels.muted(resenas == 0 ? "sin reseñas" : Formato.plural(resenas, "reseña", "reseñas")));
		fila.add(Labels.muted(Formato.plural(housing.getNumberOfRooms(), "habitación", "habitaciones")));
		fila.add(Labels.muted("de " + housing.getOwner().getUsername()));

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
	private JPanel pension() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.SM + "[]" + Space.SM + "[]" + Space.SM + "[]",
				""));
		fila.setOpaque(false);

		fila.add(Labels.caps("Pensión"));
		fila.add(comida("Desayuno", housing.isBreakfast()));
		fila.add(comida("Comida", housing.isLunch()));
		fila.add(comida("Cena", housing.isDinner()));

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

	private JPanel comodidades() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "", ""));
		fila.setOpaque(false);

		for (Amenity amenity : Amenity.values()) {

			if (amenity.presenteEn(housing)) {
				fila.add(Chip.informativo(amenity.etiqueta()), "gapright " + Space.XS);
			}
		}

		if (fila.getComponentCount() == 0) {
			fila.add(Labels.muted("Sin comodidades declaradas"));
		}

		return fila;
	}

	private JPanel pie(Runnable alIntercambiar) {

		// "por noche" va pegado al precio y no empujado al otro extremo: es su unidad
		// de medida, no un dato aparte. Lo que se separa hacia la derecha es la acción.
		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XS + "[]push[]", "[]"));
		fila.setOpaque(false);

		JLabel precio = Labels.price(Formato.precioCorto(housing.getPricePerNight()));
		precio.setFont(Typography.serif(28f));
		fila.add(precio, "aligny bottom");

		fila.add(Labels.muted("por noche"), "aligny bottom, gapbottom 4");

		if (alIntercambiar != null) {
			fila.add(enlace("Intercambiar →←", true, alIntercambiar), "aligny bottom, gapbottom 4");
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
