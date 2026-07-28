package fp.project.actihome.ui.components;

import java.awt.Color;
import java.util.function.Supplier;

import javax.swing.JLabel;

import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Fábrica de etiquetas del sistema de diseño.
 *
 * <p>
 * Cada método devuelve una etiqueta ya vestida con la tipografía y el color que
 * le corresponden por su papel. Una pantalla nunca escribe
 * {@code setFont(...)} ni {@code setForeground(...)}: pide "un título de
 * pantalla" o "una etiqueta en versalita" y se despreocupa.
 *
 * <p>
 * <b>Cómo siguen a la estación activa.</b> El color no se guarda: se resuelve
 * en cada pintado sobrescribiendo {@link JLabel#getForeground()}. Swing llama a
 * ese método cada vez que dibuja, así que la etiqueta siempre usa el color de
 * la estación vigente <em>sin</em> necesidad de suscribirse a
 * {@link Theme}, sin listas de oyentes y sin riesgo de fugas de memoria. Basta
 * con que algo provoque un repintado.
 *
 * <p>
 * Es un patrón que conviene interiorizar: <b>guardar un valor derivado obliga a
 * mantenerlo al día; calcularlo cuando hace falta no obliga a nada.</b>
 */
public final class Labels {

	private Labels() {
	}

	/** Título de portada: serif grande. Uno por pantalla, como mucho. */
	public static JLabel hero(String texto) {
		return crear(texto, Typography.serifMedium(Typography.HERO), Theme::txt);
	}

	/** Título de pantalla o de sección importante. */
	public static JLabel title(String texto) {
		return crear(texto, Typography.serifMedium(Typography.SCREEN_TITLE), Theme::txt);
	}

	/** Nombre de una ficha dentro de un listado. */
	public static JLabel cardTitle(String texto) {
		return crear(texto, Typography.serifMedium(Typography.CARD_TITLE), Theme::txt);
	}

	/** Cuerpo de texto. */
	public static JLabel body(String texto) {
		return crear(texto, Typography.sans(Typography.BODY), Theme::txt);
	}

	/** Texto secundario: metadatos, apoyos, aclaraciones. */
	public static JLabel muted(String texto) {
		return crear(texto, Typography.sans(Typography.BODY_SM), Theme::mut);
	}

	/** Precio destacado, en serif y con el color de acento. */
	public static JLabel price(String texto) {
		return crear(texto, Typography.serif(Typography.PRICE_LG), Theme::acc);
	}

	/** Precio en tamaño de ficha. */
	public static JLabel priceSmall(String texto) {
		return crear(texto, Typography.serif(Typography.PRICE), Theme::acc);
	}

	/**
	 * Etiqueta en versalita: mayúsculas, pequeña y con las letras separadas.
	 *
	 * <p>
	 * Es el recurso que da el aire editorial ("TIPO", "ORDENAR", "DISPONIBLE"). El
	 * texto se pasa a mayúsculas aquí para que quien la use no tenga que acordarse.
	 */
	public static JLabel caps(String texto) {
		return crear(texto.toUpperCase(), Typography.label(), Theme::mut);
	}

	/** Versalita en color de acento, para encabezar secciones. */
	public static JLabel capsAccent(String texto) {
		return crear(texto.toUpperCase(), Typography.label(), Theme::acc);
	}

	/** Texto sobre la barra de cabecera oscura. */
	public static JLabel onHeader(String texto) {
		return crear(texto, Typography.sans(Typography.BODY_SM), Theme::bg);
	}

	/** Texto secundario sobre la barra de cabecera oscura. */
	public static JLabel capsOnHeader(String texto) {
		return crear(texto.toUpperCase(), Typography.label(), Theme::mutSobreOscuro);
	}

	/**
	 * El wordmark "ACTIHOME": serif con las letras separadas, sobre fondo oscuro.
	 *
	 * <p>
	 * Existe como fábrica —en lugar de un {@code JLabel} con {@code setForeground}
	 * en la pantalla— por la razón de siempre en este sistema: un color asignado se
	 * queda congelado. Al cambiar de estación, la barra se repinta con el
	 * {@code hdr} nuevo pero el wordmark seguiría escrito con el {@code bg} de la
	 * estación anterior. Es un fallo que solo aparece al usar el selector, es decir,
	 * justo cuando alguien está mirando.
	 */
	public static JLabel brand(String texto, float tamano) {
		return crear(texto.toUpperCase(), Typography.serifTracked(tamano), Theme::bg);
	}

	/** Titular de display sobre fondo oscuro (claim del login, hero del panel). */
	public static JLabel displayOnHeader(String texto, float tamano) {
		return crear(texto, Typography.serifMedium(tamano), Theme::bg);
	}

	/** Frase editorial en cursiva, con el color de acento. */
	public static JLabel editorial(String texto) {
		return crear(texto, Typography.serifItalic(16f), Theme::acc);
	}

	/** Frase editorial en cursiva sobre fondo oscuro. */
	public static JLabel editorialOnHeader(String texto) {
		return crear(texto, Typography.serifItalic(17f), Theme::mutSobreOscuro);
	}

	/** Mensaje de error junto a un formulario. */
	public static JLabel error(String texto) {
		return crear(texto, Typography.sansSemiBold(Typography.BODY_SM), () -> Theme.DANGER);
	}

	private static JLabel crear(String texto, java.awt.Font fuente, Supplier<Color> color) {

		JLabel etiqueta = new ThemedLabel(texto, color);
		etiqueta.setFont(fuente);
		return etiqueta;
	}

	/**
	 * Etiqueta que resuelve su color en cada pintado en lugar de guardarlo.
	 */
	private static final class ThemedLabel extends JLabel {

		private static final long serialVersionUID = 1L;

		private final transient Supplier<Color> color;

		private ThemedLabel(String texto, Supplier<Color> color) {
			super(texto);
			this.color = color;
		}

		@Override
		public Color getForeground() {

			// Swing llama a este método durante la construcción de la clase padre,
			// cuando el campo todavía no está asignado. Sin esta guarda, cualquier
			// JLabel con color dinámico revienta con NullPointerException al crearse:
			// es la trampa clásica al extender componentes de Swing.
			return color == null ? super.getForeground() : color.get();
		}
	}
}
