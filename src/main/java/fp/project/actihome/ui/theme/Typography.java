package fp.project.actihome.ui.theme;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/**
 * Tipografía del sistema de diseño de ActiHome.
 *
 * <p>
 * El handoff define dos familias: <b>Spectral</b> (serif) para display, títulos,
 * precios y cifras, y <b>Manrope</b> (sans) para la interfaz y el cuerpo de
 * texto. Ambas se distribuyen bajo licencia SIL OFL, así que se empaquetan
 * dentro del jar ({@code src/main/resources/fonts/}) en lugar de depender de
 * que estén instaladas en el equipo o de descargarlas de internet: la
 * aplicación debe verse igual en cualquier ordenador y sin conexión.
 *
 * <p>
 * <b>Por qué se guardan los objetos {@link Font} en campos</b> en vez de
 * escribir {@code new Font("Manrope SemiBold", ...)}: Swing solo distingue
 * PLAIN, BOLD e ITALIC. Pesos intermedios como Medium (500), SemiBold (600) o
 * ExtraBold (800) no son representables con esas constantes, y localizarlos por
 * nombre depende de cómo el sistema operativo agrupe la familia. Cargando cada
 * archivo por separado y derivando solo el tamaño, el peso siempre es el
 * correcto en Windows, macOS y Linux.
 *
 * <p>
 * Si un archivo faltara, se cae con elegancia a las fuentes lógicas de Java
 * ({@code Dialog} / {@code Serif}): la aplicación se verá peor, pero no se
 * romperá.
 */
public final class Typography {

	private static final String FONT_PATH = "/fonts/";

	// --- Escala tipográfica del handoff (en puntos) ---
	// Tener la escala en constantes evita que cada pantalla invente su propio
	// tamaño: es la diferencia entre un sistema y una colección de decisiones
	// sueltas.
	public static final float HERO = 46f;
	public static final float SCREEN_TITLE = 32f;
	public static final float CARD_TITLE_LG = 32f;
	public static final float CARD_TITLE = 21f;
	public static final float PRICE_LG = 34f;
	public static final float PRICE = 22f;
	public static final float BODY = 15f;
	public static final float BODY_SM = 14f;
	public static final float LABEL = 11f;

	/** Separación entre letras de las etiquetas en versalita (0,18 em). */
	public static final float LABEL_TRACKING = 0.18f;

	private static Font sansRegular;
	private static Font sansMedium;
	private static Font sansSemiBold;
	private static Font sansBold;
	private static Font sansExtraBold;

	private static Font serifRegular;
	private static Font serifMedium;
	private static Font serifSemiBold;
	private static Font serifItalic;

	private Typography() {
	}

	/**
	 * Carga las fuentes empaquetadas y las registra en el entorno gráfico. Debe
	 * llamarse una sola vez, al arrancar, antes de construir ninguna ventana.
	 */
	static void register() {

		sansRegular = load("Manrope-Regular.ttf", Font.SANS_SERIF);
		sansMedium = load("Manrope-Medium.ttf", Font.SANS_SERIF);
		sansSemiBold = load("Manrope-SemiBold.ttf", Font.SANS_SERIF);
		sansBold = load("Manrope-Bold.ttf", Font.SANS_SERIF);
		sansExtraBold = load("Manrope-ExtraBold.ttf", Font.SANS_SERIF);

		serifRegular = load("Spectral-Regular.ttf", Font.SERIF);
		serifMedium = load("Spectral-Medium.ttf", Font.SERIF);
		serifSemiBold = load("Spectral-SemiBold.ttf", Font.SERIF);
		serifItalic = load("Spectral-Italic.ttf", Font.SERIF);
	}

	/**
	 * Lee un archivo de fuente del classpath y lo registra.
	 *
	 * @param fileName     nombre del archivo dentro de {@code /fonts/}
	 * @param logicalFallback fuente lógica a usar si la carga falla
	 */
	private static Font load(String fileName, String logicalFallback) {

		try (InputStream in = Typography.class.getResourceAsStream(FONT_PATH + fileName)) {

			if (in == null) {
				System.err.println("[Typography] No se encontró la fuente " + fileName + ", se usa la fuente del sistema.");
				return new Font(logicalFallback, Font.PLAIN, 12);
			}

			Font font = Font.createFont(Font.TRUETYPE_FONT, in);

			// El registro en el GraphicsEnvironment no es imprescindible para usar el
			// objeto Font, pero sí para que la fuente sea localizable por nombre desde
			// HTML embebido en JLabel y desde las propiedades de FlatLaf.
			GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);

			return font;

		} catch (FontFormatException | IOException e) {
			System.err.println("[Typography] No se pudo cargar " + fileName + ": " + e.getMessage());
			return new Font(logicalFallback, Font.PLAIN, 12);
		}
	}

	// --- Sans (Manrope): interfaz, cuerpo, etiquetas ---

	public static Font sans(float size) {
		return sansRegular.deriveFont(size);
	}

	public static Font sansMedium(float size) {
		return sansMedium.deriveFont(size);
	}

	public static Font sansSemiBold(float size) {
		return sansSemiBold.deriveFont(size);
	}

	public static Font sansBold(float size) {
		return sansBold.deriveFont(size);
	}

	public static Font sansExtraBold(float size) {
		return sansExtraBold.deriveFont(size);
	}

	// --- Serif (Spectral): display, títulos, precios, cifras ---

	public static Font serif(float size) {
		return serifRegular.deriveFont(size);
	}

	public static Font serifMedium(float size) {
		return serifMedium.deriveFont(size);
	}

	public static Font serifSemiBold(float size) {
		return serifSemiBold.deriveFont(size);
	}

	public static Font serifItalic(float size) {
		return serifItalic.deriveFont(size);
	}

	/**
	 * Etiqueta en versalita: sans SemiBold, pequeña y con las letras separadas.
	 *
	 * <p>
	 * Es el recurso tipográfico que da el aire editorial al diseño ("TIPO",
	 * "ORDENAR", "DISPONIBLE"). En CSS sería {@code letter-spacing}; en Java se
	 * consigue con el atributo {@link TextAttribute#TRACKING}, que se expresa
	 * como fracción del tamaño de la fuente. El texto en mayúsculas lo pone quien
	 * use la etiqueta, no la fuente.
	 */
	public static Font label(float size) {
		return sansSemiBold.deriveFont(size)
				.deriveFont(Collections.singletonMap(TextAttribute.TRACKING, LABEL_TRACKING));
	}

	public static Font label() {
		return label(LABEL);
	}

	/**
	 * Serif con las letras separadas: el tratamiento del wordmark "ACTIHOME".
	 *
	 * <p>
	 * Es la única combinación del sistema que mezcla los dos recursos —la serif de
	 * display y el espaciado de la versalita— y existe solo para la marca. El
	 * espaciado es algo menor que el de las etiquetas ({@code .14em} frente a
	 * {@code .18em}): a 20px, el mismo tracking que a 11px desharía la palabra.
	 *
	 * <p>
	 * Un detalle tipográfico que conviene saber: <b>el espaciado entre letras debe
	 * bajar según sube el tamaño</b>. Las mayúsculas pequeñas necesitan aire para no
	 * empastarse; las grandes ya lo tienen de serie, y añadírselo las convierte en
	 * letras sueltas en vez de en una palabra.
	 */
	public static Font serifTracked(float size) {
		return serifMedium.deriveFont(size).deriveFont(Collections.singletonMap(TextAttribute.TRACKING, 0.14f));
	}
}
