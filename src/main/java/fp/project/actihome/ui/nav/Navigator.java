package fp.project.actihome.ui.nav;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.ui.theme.BrandAssets;
import fp.project.actihome.ui.theme.Layout;

/**
 * Único responsable de abrir y cerrar ventanas.
 *
 * <p>
 * <b>El problema que resuelve.</b> Hasta ahora cada pantalla navegaba por su
 * cuenta:
 *
 * <pre>
 * dispose();
 * ShowHousingsFrame destino = context.getBean(ShowHousingsFrame.class);
 * destino.setVisible(true);
 * </pre>
 *
 * Eso significa que diecisiete clases conocen a las demás, que todas guardan
 * una referencia al {@code ApplicationContext} de Spring —una ventana no
 * debería saber que Spring existe— y que la regla de "cerrar la anterior antes
 * de abrir la siguiente" está repetida diecisiete veces, con la posibilidad de
 * que a alguna se le olvide.
 *
 * <p>
 * Con el navegador, una pantalla dice a dónde quiere ir y se desentiende de
 * cómo:
 *
 * <pre>
 * navigator.ir(ShowHousingsFrame.class);
 * navigator.ir(HousingDetailsFrame.class, frame -&gt; frame.loadDetails(housing));
 * </pre>
 *
 * <p>
 * <b>Concepto: acoplamiento.</b> Dos clases están acopladas cuando cambiar una
 * obliga a tocar la otra. Antes, renombrar o dividir una pantalla obligaba a
 * revisar todas las que navegaban hacia ella. Ahora las pantallas no se
 * conocen: cada una conoce al navegador, y el navegador no conoce a ninguna en
 * concreto. Es la diferencia entre diecisiete piezas enredadas y diecisiete
 * piezas colgando de un eje.
 *
 * <p>
 * Y de paso resuelve dos bugs de raíz, sin tener que acordarse en cada
 * pantalla:
 * <ul>
 * <li><b>B13</b>: ninguna ventana declaraba qué hacer al pulsar la X, así que
 * regía el valor por defecto de Swing —ocultarla— y el proceso Java se quedaba
 * vivo en segundo plano. El navegador lo fija al mostrarla.</li>
 * <li>La ventana anterior se cierra <b>después</b> de mostrar la nueva. Al
 * revés, cerrar la última ventana visible puede terminar la aplicación antes de
 * que aparezca la siguiente.</li>
 * </ul>
 */
@Component
@Profile("!test")
public class Navigator {

	private final ApplicationContext context;

	/** La ventana que el usuario está viendo ahora mismo. */
	private JFrame visible;

	public Navigator(ApplicationContext context) {
		this.context = context;
	}

	/** Abre una pantalla y cierra la anterior. */
	public <T extends JFrame> void ir(Class<T> destino) {
		ir(destino, ventana -> {
			// Sin preparación previa.
		});
	}

	/**
	 * Abre una pantalla, dándole antes los datos que necesite.
	 *
	 * <p>
	 * La preparación se ejecuta <b>antes</b> de mostrarla, porque los frames
	 * recargan sus datos dentro de {@code setVisible} y para entonces ya deben
	 * saber, por ejemplo, de qué alojamiento hablan.
	 *
	 * @param preparar qué hacer con la ventana antes de mostrarla
	 */
	public <T extends JFrame> void ir(Class<T> destino, Consumer<T> preparar) {

		T ventana = context.getBean(destino);
		preparar.accept(ventana);
		mostrar(ventana);
	}

	private void mostrar(JFrame ventana) {

		JFrame anterior = visible;

		// Pulsar la X cierra la aplicación. Es una app de una sola ventana: ocultarla
		// y dejar el proceso vivo, que es el comportamiento por defecto de Swing, solo
		// sirve para dejar javaw.exe consumiendo memoria sin que nadie lo sepa.
		ventana.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		// El icono se pone aquí y no en cada pantalla: es el navegador quien las
		// muestra, así que es el único sitio por el que pasan todas. Si mañana se añade
		// una ventana, hereda el icono sin que nadie tenga que acordarse.
		List<Image> iconos = BrandAssets.iconosDeAplicacion();

		if (!iconos.isEmpty()) {
			ventana.setIconImages(iconos);
		}

		// La misma configuración gráfica para las dos llamadas, y calculada de la
		// fuente de verdad ("anterior", si sigue viva; si no, la propia ventana). Ver
		// la nota en heredarGeometria: pedírsela a "ventana" da el monitor primario
		// mientras su par nativo no exista, que es justo lo que produce el salto en
		// sistemas con más de un monitor.
		GraphicsConfiguration configuracion = anterior != null && anterior.isDisplayable()
				? anterior.getGraphicsConfiguration()
				: ventana.getGraphicsConfiguration();

		fijarMinimoSegunElContenido(ventana, configuracion);
		heredarGeometria(anterior, ventana);

		visible = ventana;
		ventana.setVisible(true);

		// Se cierra CUALQUIER otra ventana viva, no solo la que el navegador recuerda
		// haber mostrado.
		//
		// El motivo es que el rediseño va por fases y conviven dos estilos de
		// navegación: las pantallas nuevas usan este navegador y las que quedan por
		// rediseñar siguen haciendo "dispose(); getBean(...); setVisible(true)" por su
		// cuenta. Cuando una de esas navega, el navegador se queda apuntando a una
		// ventana ya cerrada; en el siguiente salto cerraría esa —que ya no está— en
		// lugar de la que el usuario tiene delante, y acabarían dos ventanas abiertas a
		// la vez. En una aplicación de una sola ventana eso se lee directamente como
		// que algo se ha roto.
		//
		// Preguntar por las ventanas vivas en vez de fiarse de lo apuntado hace que el
		// navegador sea correcto pase lo que pase fuera de él. Cuando no queden
		// pantallas del estilo antiguo, esto se podrá simplificar.
		//
		// El orden importa y no es negociable: **primero se muestra la nueva y después
		// se cierran las demás**. Al revés, cerrar la última ventana viva puede terminar
		// la aplicación antes de que aparezca la siguiente.
		for (Window abierta : Window.getWindows()) {

			if (abierta != ventana && abierta instanceof JFrame && abierta.isDisplayable()) {
				abierta.dispose();
			}
		}

		// Redundante con el bucle de arriba, pero explícito: la ventana que el navegador
		// sí conocía queda cerrada seguro.
		if (anterior != null && anterior != ventana) {
			anterior.dispose();
		}
	}

	/**
	 * Da a la pantalla nueva el tamaño y la posición que tenía la anterior.
	 *
	 * <p>
	 * <b>Segunda versión de este método, y la primera tenía un fallo de fondo.</b>
	 * Calculaba el tamaño como el mayor entre el anterior, el de diseño y
	 * {@code loQueNecesitaElContenido(ventana)} — el tamaño <em>preferido</em> del
	 * contenido de la pantalla de destino. Eso funciona para un formulario, cuyo
	 * preferido es una medida razonable. Pero para el catálogo, cuya lista vive en
	 * un {@code JScrollPane} sin más, el preferido <b>no está acotado por la
	 * ventana</b>: es el alto de <em>todas</em> las filas apiladas. Medido con seis
	 * alojamientos: <b>2289 píxeles</b>.
	 *
	 * <p>
	 * Ese número entraba en el {@code Math.max(...)}, así que la primera vez que se
	 * navegaba al catálogo la ventana se inflaba de golpe casi a pantalla completa
	 * — el salto que reportó el usuario—, y como el tamaño resultante pasaba a ser
	 * la "anterior" de la siguiente navegación, **se quedaba pegado** para el resto
	 * de la sesión: ir y volver entre catálogo e intercambio no volvía a encoger
	 * nunca la ventana. Cuantas más pantallas usaran listas o formularios largos,
	 * más partes de la aplicación acumulaban el mismo problema — de ahí que el
	 * usuario lo viera "cada vez con más pantallas".
	 *
	 * <p>
	 * <b>La corrección de fondo, no un ajuste del número.</b> Ya no hace falta que
	 * el navegador adivine cuánto necesita cada pantalla: desde que todas usan
	 * {@link fp.project.actihome.ui.components.FilaFluida} y
	 * {@link fp.project.actihome.ui.components.Rescate}, <b>cualquier pantalla se
	 * ve correcta a cualquier tamaño</b> por encima del mínimo del sistema — y eso
	 * está verificado por {@code MedirResponsive}, no es una esperanza. Así que el
	 * navegador deja de intentar ajustar el tamaño al contenido de cada destino:
	 * <b>conserva exactamente el tamaño que la ventana ya tenía</b>, sea cual sea,
	 * y solo lo recalcula desde cero la primera vez que se abre una ventana en la
	 * sesión.
	 *
	 * <ul>
	 * <li>El usuario decide el tamaño —a mano, o maximizando— y ese tamaño no
	 * vuelve a moverse solo mientras navega.</li>
	 * <li>Ninguna pantalla puede quedar "más apretada de lo que se diseñó" porque
	 * ya no existe ese concepto: todas se adaptan al espacio que haya.</li>
	 * <li>El único suelo que se respeta es el mínimo del sistema
	 * ({@link fp.project.actihome.ui.theme.Layout#MINIMO_DE_VENTANA}), y de eso ya
	 * se encarga {@link #fijarMinimoSegunElContenido}: Swing no deja que la ventana
	 * baje de su propio mínimo, así que aquí no hace falta comprobarlo otra vez.</li>
	 * </ul>
	 *
	 * <p>
	 * Maximizada es un caso aparte: no es un tamaño sino un estado, y se traslada
	 * como tal. Copiar los píxeles de una ventana maximizada dejaría una ventana
	 * del tamaño de la pantalla pero en estado normal, que al restaurar se comporta
	 * de forma rara y tapa la barra de tareas.
	 *
	 * <p>
	 * Nada de esto guarda estado entre ejecuciones: es solo continuidad dentro de
	 * una sesión. Recordar el tamaño al cerrar la aplicación sería otra cosa, y
	 * necesitaría dónde guardarlo.
	 */
	private void heredarGeometria(JFrame anterior, JFrame ventana) {

		if (anterior == null || !anterior.isDisplayable()) {

			// Primera ventana de la sesión: su tamaño de diseño, acotado a la pantalla.
			// Aquí sí tiene sentido preguntarle al contenido, porque no hay nada de lo que
			// heredar todavía. Y no hay "anterior" a la que preguntar por el monitor, así
			// que aquí sí vale la configuración gráfica de la propia ventana —de
			// arranque, es la única que existe—.
			Dimension necesaria = loQueNecesitaElContenido(ventana);
			GraphicsConfiguration configuracion = ventana.getGraphicsConfiguration();

			ventana.setSize(acotarAPantalla(configuracion, Math.max(ventana.getWidth(), necesaria.width),
					Math.max(ventana.getHeight(), necesaria.height)));

			ventana.setLocationRelativeTo(null);
			return;
		}

		if ((anterior.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
			ventana.setExtendedState(Frame.MAXIMIZED_BOTH);
			return;
		}

		ventana.setExtendedState(Frame.NORMAL);

		Rectangle previa = anterior.getBounds();

		// **La configuración gráfica se pide a "anterior", no a "ventana", y es la
		// otra mitad del arreglo del salto.** "ventana" puede ser una pantalla que se
		// construye ahora mismo, o una que estuvo visible antes y se cerró con
		// {@code dispose()} — en los dos casos su par nativo no existe todavía, y
		// {@code getGraphicsConfiguration()} devuelve la del monitor <b>primario</b>
		// por defecto, sea cual sea el monitor real donde está la aplicación. Con dos
		// monitores a escalados distintos —el caso del usuario, portátil más un
		// monitor de 27"—, acotar y encajar con esa configuración equivocada empuja la
		// ventana de vuelta al monitor primario en cuanto se navega a una pantalla que
		// no estuviera ya mostrada. "anterior" sigue viva en este punto —se cierra
		// después, no antes— así que su configuración es siempre la real.
		GraphicsConfiguration configuracion = anterior.getGraphicsConfiguration();

		// Sin Math.max contra "lo que necesita el destino": ese era el origen del
		// salto de tamaño. El tamaño se hereda tal cual, solo acotado a la pantalla
		// actual por si el usuario ha cambiado de monitor.
		ventana.setSize(acotarAPantalla(configuracion, previa.width, previa.height));

		// **Se conserva la esquina, no el centro.** Antes se recentraba sobre el centro
		// de la ventana anterior, y eso producía un salto adicional: en cuanto el
		// tamaño cambiaba aunque fuera unos píxeles, la posición cambiaba también y la
		// ventana aparecía desplazada respecto a donde estaba la anterior. Manteniendo
		// la esquina superior izquierda, dos pantallas del mismo tamaño se superponen
		// exactamente y no hay movimiento.
		ventana.setLocation(previa.x, previa.y);

		encajarEnPantalla(configuracion, ventana);
	}

	/**
	 * Impide que la ventana se pueda encoger por debajo de lo que su contenido
	 * necesita.
	 *
	 * <p>
	 * <b>El fallo que corrige, y por qué la primera corrección no bastó.</b> Cada
	 * pantalla declaraba su mínimo con números escritos a mano
	 * ({@code setMinimumSize(new Dimension(1180, 760))}), ajustados mirando capturas
	 * en una máquina concreta. El primer arreglo fue preguntarle al contenido cuál
	 * era su mínimo real y quedarse con el mayor de los dos — y ahí estaba el error:
	 * <b>tomar el mayor conserva el número escrito a mano</b>, que era justo el
	 * problema. En un portátil con el escalado de Windows al 150 % la ventana
	 * dispone de 1280×660 puntos lógicos, así que un mínimo de 1180×760 no se puede
	 * cumplir: la ventana no se dejaba encoger hasta un tamaño en el que se viera
	 * bien.
	 *
	 * <p>
	 * Ahora no hay números por pantalla. El mínimo sale de dos cosas, las dos
	 * medidas en la máquina donde se ejecuta:
	 *
	 * <ul>
	 * <li>lo que el <b>contenido</b> exige de verdad, que tras hacer la interfaz
	 * adaptable se ha quedado en muy poco;</li>
	 * <li>el <b>suelo del sistema</b> ({@link Layout#MINIMO_DE_VENTANA}), que es el
	 * tamaño para el que se garantiza que la aplicación se ve bien, verificado
	 * automáticamente por {@code MedirResponsive}.</li>
	 * </ul>
	 *
	 * <p>
	 * El tope de pantalla no es opcional y aquí es lo que salva el caso del
	 * portátil: si el escritorio es más pequeño que el suelo, manda el escritorio.
	 * Un mínimo mayor que la pantalla deja una ventana que no se puede colocar ni
	 * cerrar cómodamente.
	 */
	private void fijarMinimoSegunElContenido(JFrame ventana, GraphicsConfiguration configuracion) {

		Dimension contenido = ventana.getContentPane().getMinimumSize();
		Insets bordes = ventana.getInsets();

		int ancho = contenido.width + bordes.left + bordes.right;
		int alto = contenido.height + bordes.top + bordes.bottom;

		ventana.setMinimumSize(acotarAPantalla(configuracion, Math.max(ancho, Layout.MINIMO_DE_VENTANA.width),
				Math.max(alto, Layout.MINIMO_DE_VENTANA.height)));
	}

	/**
	 * El tamaño de ventana que hace falta para que el contenido quepa entero.
	 *
	 * <p>
	 * <b>Este método es el arreglo de un fallo real y repetido.</b> Cada pantalla
	 * declara su tamaño con un {@code setSize(...)} de números fijos, y esos
	 * números se ajustaron mirando capturas generadas a 1400×900. Pero el tamaño
	 * que ocupa un formulario <em>no</em> es una constante: depende de cuánto miden
	 * las fuentes y los controles, y eso cambia con el <b>escalado del sistema</b>.
	 * En un Windows al 125 %, todo mide un cuarto más, así que un formulario que en
	 * la captura entraba justo deja los botones fuera de la ventana.
	 *
	 * <p>
	 * Por eso el tamaño no puede salir solo de una constante escrita a mano: hay
	 * que preguntárselo al contenido ya construido, que es quien sabe cuánto mide
	 * de verdad en la máquina donde se está ejecutando. Preguntarlo aquí, en el
	 * navegador, lo arregla <b>en las diecisiete pantallas a la vez</b> en lugar de
	 * ir parcheando la que se detecte rota.
	 *
	 * <p>
	 * Al preferido del contenido hay que sumarle los <i>insets</i> de la ventana:
	 * la barra de título y los bordes que pone Windows, que no forman parte del
	 * área de contenido pero sí del tamaño de la ventana.
	 */
	private Dimension loQueNecesitaElContenido(JFrame ventana) {

		Dimension contenido = ventana.getContentPane().getPreferredSize();
		Insets bordes = ventana.getInsets();

		return new Dimension(contenido.width + bordes.left + bordes.right,
				contenido.height + bordes.top + bordes.bottom);
	}

	/**
	 * Limita un tamaño al área utilizable de la pantalla.
	 *
	 * <p>
	 * Es la otra mitad del arreglo anterior: preguntarle al contenido cuánto
	 * necesita puede devolver un número enorme —una lista larga pediría el alto de
	 * todas sus filas—, y una ventana más grande que el escritorio es tan inservible
	 * como una que se queda corta. Con el tope, la lista simplemente usa su barra de
	 * desplazamiento, que es para lo que está.
	 */
	private Dimension acotarAPantalla(GraphicsConfiguration configuracion, int ancho, int alto) {

		if (configuracion == null) {
			return new Dimension(ancho, alto);
		}

		Rectangle pantalla = configuracion.getBounds();
		Insets margenes = Toolkit.getDefaultToolkit().getScreenInsets(configuracion);

		int maxAncho = pantalla.width - margenes.left - margenes.right;
		int maxAlto = pantalla.height - margenes.top - margenes.bottom;

		return new Dimension(Math.min(ancho, maxAncho), Math.min(alto, maxAlto));
	}

	/** Empuja la ventana dentro del área utilizable si se ha salido. */
	private void encajarEnPantalla(GraphicsConfiguration configuracion, JFrame ventana) {

		// Una ventana que todavía no se ha mostrado nunca puede no tener configuración
		// gráfica asignada. Sin esta guarda, navegar a una pantalla recién construida
		// reventaría con un NullPointerException.
		if (configuracion == null) {
			return;
		}

		Rectangle pantalla = configuracion.getBounds();
		Insets margenes = Toolkit.getDefaultToolkit().getScreenInsets(configuracion);

		int minX = pantalla.x + margenes.left;
		int minY = pantalla.y + margenes.top;
		int maxX = pantalla.x + pantalla.width - margenes.right - ventana.getWidth();
		int maxY = pantalla.y + pantalla.height - margenes.bottom - ventana.getHeight();

		// Los max(minX, ...) de fuera cubren el caso de una ventana más ancha que la
		// pantalla: ahí maxX queda por debajo de minX y sin ellos la ventana saldría
		// colocada fuera por el otro lado.
		int x = Math.max(minX, Math.min(ventana.getX(), Math.max(minX, maxX)));
		int y = Math.max(minY, Math.min(ventana.getY(), Math.max(minY, maxY)));

		ventana.setLocation(x, y);
	}
}
