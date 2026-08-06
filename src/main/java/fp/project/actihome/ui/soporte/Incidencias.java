package fp.project.actihome.ui.soporte;

import java.awt.EventQueue;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Qué pasa cuando algo revienta sin que nadie lo haya previsto.
 *
 * <p>
 * <b>El problema que resuelve, y era un agujero completo.</b> Hasta ahora la
 * aplicación no tenía <em>ningún</em> manejo global de errores. Un fallo no
 * previsto —un {@code NullPointerException} al pintar, un disco lleno al guardar
 * una foto— terminaba de una de estas dos formas, las dos malas:
 *
 * <ul>
 * <li>La traza salía por la salida de error estándar. Y ActiHome se reparte como
 * un {@code .exe} generado con {@code jpackage} <b>sin consola</b>, así que esa
 * salida <b>no la lee nadie, nunca</b>. Es literalmente escribir en un sitio que
 * no existe.</li>
 * <li>El usuario veía que "no pasa nada": el botón no responde, la ventana se
 * queda igual. Sin mensaje y sin rastro que poder pedirle después.</li>
 * </ul>
 *
 * <p>
 * <b>Las tres cosas que hace, y por qué las tres.</b>
 *
 * <p>
 * <b>1. Lo registra en un fichero.</b> Es lo que convierte "me falla, no sé por
 * qué" en algo diagnosticable. El fichero vive junto a la base de datos, en
 * {@code ~/.actihome/logs/}, que es el único sitio donde esta aplicación escribe
 * en tiempo de ejecución.
 *
 * <p>
 * <b>2. Se lo dice al usuario.</b> Un fallo silencioso es peor que uno visible:
 * quien no sabe que algo ha fallado vuelve a intentarlo, y a veces repite el
 * efecto a medias. El mensaje no enseña la traza —no le sirve de nada— sino
 * dónde está el fichero, que es lo único que puede aportar.
 *
 * <p>
 * <b>3. Solo avisa una vez.</b> Un fallo al pintar se repite en cada repintado,
 * decenas de veces por segundo. Sin el freno, la primera excepción abriría una
 * cascada de diálogos que dejaría la aplicación inutilizable — el manejador
 * sería peor que el problema. A partir del segundo, se sigue registrando todo y
 * ya no se interrumpe.
 *
 * <p>
 * <b>Por qué {@code JOptionPane} y no los componentes del sistema de diseño.</b>
 * Esto se ejecuta precisamente cuando algo ya ha ido mal, y puede haber ido mal
 * el propio tema. Un diálogo que dependiera de {@code Theme}, de las fuentes
 * empaquetadas o de la mascota podría fallar al construirse y dejarnos sin el
 * único aviso que teníamos. Aquí lo feo es lo correcto: un manejador de errores
 * tiene que depender de lo mínimo.
 */
public final class Incidencias {

	private static final Logger log = LoggerFactory.getLogger(Incidencias.class);

	/** Dónde acaba el registro. Solo para poder decírselo al usuario. */
	private static final File CARPETA_DE_REGISTRO = new File(System.getProperty("user.home"), ".actihome/logs");

	/** Si ya se ha avisado en pantalla. Ver el punto 3 del javadoc. */
	private static final AtomicBoolean YA_AVISADO = new AtomicBoolean(false);

	private Incidencias() {
	}

	/**
	 * Instala el manejador. Se llama una sola vez, al arrancar.
	 *
	 * <p>
	 * <b>Cubre también el hilo de la interfaz</b>, y eso no es evidente. Desde Java
	 * 7, una excepción que escapa de un manejador de eventos de Swing acaba en el
	 * manejador de excepciones no capturadas del hilo, así que este de aquí las
	 * recoge sin necesidad del antiguo {@code sun.awt.exception.handler} —una
	 * propiedad interna del JDK que además ya no se puede dar por buena—.
	 *
	 * <p>
	 * Que eso sea así de verdad no se da por supuesto: lo comprueba
	 * {@code IncidenciasTest}, lanzando una excepción dentro del hilo de eventos y
	 * exigiendo que este manejador la vea.
	 */
	public static void instalar() {

		Thread.setDefaultUncaughtExceptionHandler((hilo, fallo) -> registrar(hilo.getName(), fallo));
	}

	/**
	 * Registra un fallo y, la primera vez, avisa al usuario.
	 *
	 * <p>
	 * Visible para el test. No es una concesión: la alternativa sería que el test
	 * fabricara un hilo de verdad y esperase a que muriese, que es más andamiaje
	 * para comprobar lo mismo.
	 */
	static void registrar(String hilo, Throwable fallo) {

		log.error("Fallo no controlado en el hilo '{}'", hilo, fallo);

		if (!YA_AVISADO.compareAndSet(false, true)) {
			return;
		}

		// El aviso se pide al hilo de eventos en lugar de mostrarse aquí: este
		// manejador puede dispararse desde cualquier hilo, y un diálogo de Swing
		// construido fuera del hilo de eventos es exactamente la clase de error que
		// estamos intentando registrar.
		EventQueue.invokeLater(Incidencias::avisar);
	}

	private static void avisar() {

		try {
			JOptionPane.showMessageDialog(null,
					"Se ha producido un error inesperado.\n\n"
							+ "La aplicación puede seguir funcionando, pero conviene reiniciarla.\n"
							+ "El detalle técnico está en:\n" + CARPETA_DE_REGISTRO.getAbsolutePath(),
					"ActiHome", JOptionPane.ERROR_MESSAGE);

		} catch (RuntimeException ex) {
			// Si ni siquiera se puede abrir un diálogo -sin entorno gráfico, o con el
			// sistema de ventanas ya caído- no hay nada más que intentar. Lo que no puede
			// pasar es que el manejador de errores lance su propia excepción y se coma la
			// original, que es la que de verdad interesa.
			log.error("Ademas, no se pudo avisar al usuario", ex);
		}
	}
}
