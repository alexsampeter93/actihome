package fp.project.actihome.ui.theme;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tipografía del sistema de diseño de ActiHome.
 *
 * <p>
 * Dos familias: <b>Fraunces</b> (serif) para display, títulos, precios y
 * cifras, y <b>Archivo</b> (sans) para la interfaz y el cuerpo de texto. Ambas
 * se distribuyen bajo licencia SIL OFL, así que se empaquetan dentro del jar
 * ({@code src/main/resources/fonts/}) en lugar de depender de que estén
 * instaladas en el equipo o de descargarlas de internet: la aplicación debe
 * verse igual en cualquier ordenador y sin conexión.
 *
 * <p>
 * <b>Por qué se cambiaron Spectral y Manrope (Fase 8.2).</b> La razón la dio el
 * usuario y es certera: se leían como «las fuentes típicas de IA». No es una
 * impresión vaga. Manrope pertenece a la misma familia de grotescas geométricas
 * que Inter, Plus Jakarta y Space Grotesk, que son literalmente las que traen
 * por defecto las plantillas de las que salen las interfaces generadas, y el
 * ojo entrenado las reconoce aunque no sepa nombrarlas. Un sistema de diseño
 * que se declara «editorial premium, explícitamente anti-plantilla SaaS»
 * (CLAUDE.md §6) y luego usa la tipografía canónica de esa estética se está
 * contradiciendo en el elemento más visible que tiene.
 *
 * <p>
 * Las sustitutas no son «otra sans y otra serif cualquiera», están elegidas por
 * lo contrario de lo que se descartó. <b>Archivo</b> es una grotesca de origen
 * periodístico —diseñada para titulares y texto de prensa impresa—, así que
 * aporta la neutralidad que una interfaz necesita sin el redondeo geométrico
 * que delata a las otras. <b>Fraunces</b> es una serif de contraste alto con
 * rasgos deliberadamente irregulares: es lo más lejos de «genérica» que se
 * puede llegar manteniendo la legibilidad de un texto largo.
 *
 * <p>
 * <b>Tamaños ópticos, que es lo que Spectral no tenía.</b> Fraunces se dibuja
 * en varias versiones según el tamaño al que vaya a leerse, y esto es
 * tipografía de verdad, no un adorno: una letra pensada para 46px tiene los
 * trazos finos <em>más</em> finos y las letras <em>más</em> juntas, porque a ese
 * tamaño el ojo ya distingue el detalle y el aire sobra. La misma letra a 15px
 * se rompería — los trazos finos desaparecerían y las letras se empastarían.
 * Por eso {@link #serif(float)} y sus hermanas <b>eligen el archivo según el
 * tamaño que se les pide</b>: por debajo de {@value #UMBRAL_OPTICO}px usan la
 * variante de texto (9pt) y por encima la de display (144pt). Quien llama no se
 * entera de nada, sigue pidiendo «serif a 46».
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

	private static final Logger log = LoggerFactory.getLogger(Typography.class);

	private static final String FONT_PATH = "/fonts/";

	// --- Escala tipográfica del handoff (en puntos) ---
	// Tener la escala en constantes evita que cada pantalla invente su propio
	// tamaño: es la diferencia entre un sistema y una colección de decisiones
	// sueltas.
	public static final float HERO = 46f;
	public static final float SCREEN_TITLE = 32f;
	public static final float DETAIL_TITLE = 36f;
	public static final float CARD_TITLE_LG = 32f;
	public static final float CARD_TITLE = 21f;
	public static final float PRICE_LG = 34f;
	public static final float PRICE = 22f;
	public static final float BODY = 15f;
	public static final float BODY_SM = 14f;
	public static final float LABEL = 11f;

	/** Separación entre letras de las etiquetas en versalita (0,18 em). */
	public static final float LABEL_TRACKING = 0.18f;

	/**
	 * A partir de aquí, la serif se pide en su variante de display en vez de en la
	 * de texto. El corte está donde la escala del handoff separa el «nombre de
	 * ficha» pequeño (21px) de los títulos de pantalla (28px en adelante).
	 */
	private static final float UMBRAL_OPTICO = 26f;

	// Tres pesos de sans y tres de serif, ni uno más. La auditoría de la Fase 8.3
	// encontró que sansBold, sansExtraBold y serifSemiBold no se llamaban desde
	// ningún sitio: cuatro archivos de fuente (~590 KB, un tercio del peso
	// tipográfico del jar) que se cargaban al arrancar para no dibujarse nunca.
	// Se retiraron con los métodos. La regla del proyecto es "si algo no existe en
	// el sistema, se añade al sistema" — añadir *cuando* haga falta, no por si
	// acaso.
	private static Font sansRegular;
	private static Font sansMedium;
	private static Font sansSemiBold;

	// Serif en dos tallas ópticas. La de texto se usa por debajo del umbral y la
	// de display por encima; ver la nota de clase sobre por qué esto no es un
	// adorno.
	private static Font serifTextoRegular;
	private static Font serifTextoSemiBold;
	private static Font serifTextoItalic;

	private static Font serifDisplayRegular;
	private static Font serifDisplaySemiBold;
	private static Font serifDisplayItalic;

	private Typography() {
	}

	/**
	 * Carga las fuentes empaquetadas y las registra en el entorno gráfico. Debe
	 * llamarse una sola vez, al arrancar, antes de construir ninguna ventana.
	 */
	static void register() {

		sansRegular = load("Archivo-Regular.ttf", Font.SANS_SERIF);
		sansMedium = load("Archivo-Medium.ttf", Font.SANS_SERIF);
		sansSemiBold = load("Archivo-SemiBold.ttf", Font.SANS_SERIF);

		serifTextoRegular = load("Fraunces9pt-Regular.ttf", Font.SERIF);
		serifTextoSemiBold = load("Fraunces9pt-SemiBold.ttf", Font.SERIF);
		serifTextoItalic = load("Fraunces9pt-Italic.ttf", Font.SERIF);

		serifDisplayRegular = load("Fraunces144pt-Regular.ttf", Font.SERIF);
		serifDisplaySemiBold = load("Fraunces144pt-SemiBold.ttf", Font.SERIF);
		serifDisplayItalic = load("Fraunces144pt-Italic.ttf", Font.SERIF);
	}

	/** Elige entre la talla de texto y la de display según el tamaño pedido. */
	private static Font optica(Font texto, Font display, float size) {
		return (size < UMBRAL_OPTICO ? texto : display).deriveFont(size);
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
				log.warn("No se encontro la fuente {}, se usa la del sistema", fileName);
				return new Font(logicalFallback, Font.PLAIN, 12);
			}

			Font font = Font.createFont(Font.TRUETYPE_FONT, in);

			// El registro en el GraphicsEnvironment no es imprescindible para usar el
			// objeto Font, pero sí para que la fuente sea localizable por nombre desde
			// HTML embebido en JLabel y desde las propiedades de FlatLaf.
			GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);

			return font;

		} catch (FontFormatException | IOException e) {
			log.warn("No se pudo cargar la fuente {}", fileName, e);
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

	// --- Serif (Fraunces): display, títulos, precios, cifras ---
	// Cada método elige solo su talla óptica: quien llama pide "serif a 46" y no
	// necesita saber que por debajo de 26 se sirve otro archivo.

	public static Font serif(float size) {
		return optica(serifTextoRegular, serifDisplayRegular, size);
	}

	/**
	 * El peso de trabajo de los títulos: 29 de los 37 usos de la serif en la
	 * aplicación pasan por aquí.
	 *
	 * <p>
	 * <b>Fraunces no tiene Medium (500)</b>, que es lo que tenía Spectral, así que
	 * este método sirve SemiBold (600). No es una equivalencia perezosa: Fraunces
	 * es de contraste más alto que Spectral —sus trazos finos son más finos—, y a
	 * igual peso nominal se ve más ligera. El 600 de Fraunces pesa en pantalla
	 * aproximadamente lo que pesaba el 500 de Spectral; conservar el nombre del
	 * método es lo correcto porque lo que nombra es <em>el papel</em> («el peso de
	 * los títulos»), no el número del eje.
	 */
	public static Font serifMedium(float size) {
		return optica(serifTextoSemiBold, serifDisplaySemiBold, size);
	}

	public static Font serifItalic(float size) {
		return optica(serifTextoItalic, serifDisplayItalic, size);
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

	// --- Altos de control, medidos desde la fuente ---
	// Regla del proyecto (CLAUDE.md §4): ningún tamaño que dependa de texto puede
	// ser una constante. Los altos de 38 (campo), 44 (botón) y 32 (control
	// compacto) estaban escritos a pelo en veinte sitios, ajustados mirando una
	// pantalla al 100 %. Con el escalado de Windows al 150 % la fuente mide vez y
	// media y la caja seguía midiendo lo mismo, así que el texto quedaba cortado
	// por arriba y por abajo. Es el mismo fallo que ya se corrigió en
	// Field.textArea y que al campo de una línea nunca le llegó.
	//
	// Lo que se declara aquí no es un número de píxeles, es la intención: una
	// línea de texto más el aire que le corresponde.

	/**
	 * Etiqueta de usar y tirar, solo para preguntarle a Swing cuánto mide una
	 * fuente. {@code JComponent.getFontMetrics(Font)} funciona sin que el
	 * componente llegue a mostrarse nunca, así que no hace falta ninguna ventana.
	 */
	private static javax.swing.JLabel regla;

	/** Alto real de una línea de la sans al tamaño dado, según la fuente cargada. */
	public static int altoDeLinea(float size) {

		if (regla == null) {
			regla = new javax.swing.JLabel();
		}

		return regla.getFontMetrics(sans(size)).getHeight();
	}

	/**
	 * Ancho de un espacio en la fuente dada: la separación natural entre dos
	 * palabras.
	 *
	 * <p>
	 * Hace falta cuando dos palabras de una misma frase van en <b>etiquetas
	 * distintas</b> —el titular del catálogo lo hace, porque el renderizado HTML de
	 * Swing se lleva mal con las fuentes registradas en tiempo de ejecución—. Ahí
	 * no hay ningún carácter de espacio entre ellas: la separación la pone el
	 * layout, y si se declara como un número deja de ser un espacio y pasa a ser
	 * una coincidencia. Con Spectral, {@code Space.SM} (12px) pasaba por espacio; al
	 * cambiar a Fraunces, que lo tiene más estrecho, la misma constante se leía como
	 * dos espacios seguidos.
	 */
	public static int anchoDeEspacio(Font fuente) {

		if (regla == null) {
			regla = new javax.swing.JLabel();
		}

		return regla.getFontMetrics(fuente).charWidth(' ');
	}

	/**
	 * Alto de un control de una línea: campo de texto, desplegable, buscador.
	 *
	 * <p>
	 * El sumando es aire, no un ajuste: es lo que separa el texto del borde de la
	 * caja por arriba y por abajo. Se recalibró al cambiar de tipografía (Fase 8.2)
	 * y el motivo merece anotarse, porque es justo lo que este método existe para
	 * absorber: <b>Archivo tiene la interlínea más apretada que Manrope</b> (18px
	 * frente a 21 al mismo cuerpo de 15), así que con el aire de antes las cajas
	 * habrían encogido de 38 a 34 píxeles sin que nadie lo pidiera. Cambiar de
	 * fuente no debe cambiar el tamaño de los controles.
	 */
	public static int altoDeControl() {
		return altoDeLinea(BODY) + Space.LG;
	}

	/**
	 * Alto de un botón de acción. Se define <b>a partir del campo</b> y no desde
	 * cero, porque la intención del diseño es exactamente esa: un botón pesa un
	 * peldaño más que un campo. Escrito así, la relación se conserva sola aunque
	 * vuelva a cambiar la tipografía.
	 */
	public static int altoDeBoton() {
		return altoDeControl() + Space.XS;
	}

	/** Alto de un control compacto: los filtros del catálogo, que van en cuerpo pequeño. */
	public static int altoDeControlCompacto() {
		return altoDeLinea(BODY_SM) + Space.MD;
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
		return serifMedium(size).deriveFont(Collections.singletonMap(TextAttribute.TRACKING, 0.14f));
	}
}
