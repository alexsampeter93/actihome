package fp.project.actihome.ui.brand;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.Timer;

import fp.project.actihome.ui.theme.BrandAssets;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Pantalla de bienvenida animada: el lockup "CocoBrain presenta" mientras
 * arranca la aplicación.
 *
 * <p>
 * <b>Por qué existe.</b> Levantar el contexto de Spring y conectar con MySQL
 * tarda un par de segundos, y hasta ahora eran un vacío: ni ventana, ni icono,
 * ni señal de que hubiera pasado algo. Ese hueco es además el único sitio donde
 * CocoBrain puede ser protagonista sin quitarle nada a ActiHome: cuando aparece
 * la aplicación, la marca ya se ha apartado.
 *
 * <p>
 * <b>La animación.</b> Tres movimientos, ninguno gratuito:
 * <ul>
 * <li><b>Entrada</b>: la ventana se desvanece de invisible a opaca mientras el
 * lockup sube unos píxeles hasta asentarse. Aparecer de golpe se percibe como
 * un parpadeo; aparecer con un gesto corto se percibe como una presentación.</li>
 * <li><b>Barra de carga</b>: una línea fina en el color de la estación que
 * avanza mientras se levanta la aplicación. No mide el progreso real —Spring no
 * lo publica— pero sí comunica lo importante: que algo está ocurriendo.</li>
 * <li><b>Salida</b>: se desvanece en lugar de desaparecer, y lo hace <em>a la
 * vez</em> que aparece el login. El corte seco entre dos ventanas es lo que
 * hace que un arranque parezca a trompicones.</li>
 * </ul>
 *
 * <p>
 * Las curvas de la animación no son lineales sino <b>ease-out</b>: rápidas al
 * principio y frenando al final. Es como se mueven las cosas físicas y es lo
 * que distingue una animación agradable de una que parece mecánica.
 *
 * <p>
 * <b>Cuidado con el hilo.</b> Todo esto vive en el hilo de eventos de Swing,
 * porque es el único desde el que se pueden tocar componentes. Como el arranque
 * ocurre en {@code main}, hay que saltar a ese hilo con
 * {@code invokeAndWait}: con {@code invokeLater} el arranque seguiría mientras
 * el splash todavía no existe.
 */
public final class SplashScreen {

	private static final int ANCHO = 620;
	private static final int ALTO = 420;

	/** Duración del desvanecido de entrada, en milisegundos. */
	private static final float ENTRADA = 420f;

	/** Duración del desvanecido de salida. */
	private static final float SALIDA = 300f;

	/** Cuánto tarda la barra en llegar a su tope mientras se espera. */
	private static final float RECORRIDO_BARRA = 2700f;

	/** Tope de la barra mientras carga: nunca llega al final hasta que termina de verdad. */
	private static final float TOPE_BARRA = 0.92f;

	/**
	 * Tiempo mínimo en pantalla.
	 *
	 * <p>
	 * Casi tres segundos, y son a propósito: la aplicación arranca en menos de dos,
	 * así que sin este mínimo el splash se iría antes de que diera tiempo a leerlo.
	 * Es el único sitio del proyecto donde se hace esperar al usuario adrede, y se
	 * justifica porque lo que se está mostrando es la marca.
	 */
	private static final long MINIMO_VISIBLE = 2900;

	/** Red de seguridad: si nadie lo cierra, se cierra solo. */
	private static final int MAXIMO_VISIBLE = 8000;

	private static JWindow ventana;
	private static Lienzo lienzo;
	private static Timer animacion;
	private static long mostradoEn;
	private static boolean puedeDesvanecerse;

	private SplashScreen() {
	}

	/** Muestra el splash. Si faltan las imágenes, no hace nada. */
	public static void mostrar() {

		if (BrandAssets.lockupCocoBrain() == null) {
			return;
		}

		ejecutarEnElHiloDeEventos(() -> {

			puedeDesvanecerse = soportaTransparencia();

			lienzo = new Lienzo();

			ventana = new JWindow();
			ventana.setContentPane(lienzo);
			ventana.setSize(ANCHO, ALTO);
			ventana.setLocationRelativeTo(null);
			ventana.setAlwaysOnTop(true);

			if (puedeDesvanecerse) {
				ventana.setOpacity(0f);
			}

			ventana.setVisible(true);
			mostradoEn = System.currentTimeMillis();

			// 16 ms ≈ 60 fotogramas por segundo, que es el ritmo al que una animación
			// deja de percibirse como una sucesión de saltos.
			animacion = new Timer(16, e -> avanzar());
			animacion.start();

			Timer seguro = new Timer(MAXIMO_VISIBLE, e -> descartar());
			seguro.setRepeats(false);
			seguro.start();
		});
	}

	/** Un fotograma de la animación de entrada y de la barra. */
	private static void avanzar() {

		if (ventana == null) {
			return;
		}

		long transcurrido = System.currentTimeMillis() - mostradoEn;

		float entrada = suavizar(Math.min(1f, transcurrido / ENTRADA));
		float carga = Math.min(TOPE_BARRA, suavizar(transcurrido / RECORRIDO_BARRA));

		if (puedeDesvanecerse) {
			ventana.setOpacity(entrada);
		}

		lienzo.actualizar(entrada, carga);
	}

	/**
	 * Cierra el splash con un desvanecido, esperando antes a que haya estado en
	 * pantalla el tiempo mínimo.
	 */
	public static void cerrar() {

		if (ventana == null) {
			return;
		}

		long visible = System.currentTimeMillis() - mostradoEn;

		if (visible < MINIMO_VISIBLE) {
			try {
				Thread.sleep(MINIMO_VISIBLE - visible);
			} catch (InterruptedException e) {
				// Si alguien interrumpe la espera se cierra sin más: no merece la pena
				// retrasar el arranque por una animación. Se restaura la marca de
				// interrupción para no ocultársela a quien venga después.
				Thread.currentThread().interrupt();
			}
		}

		ejecutarEnElHiloDeEventos(SplashScreen::desvanecer);
	}

	/**
	 * Completa la barra y desvanece la ventana.
	 *
	 * <p>
	 * No bloquea: el desvanecido corre en su propio temporizador mientras el
	 * arranque continúa, así que el login aparece <em>durante</em> la salida del
	 * splash en lugar de después. Ese solape es lo que hace que la transición se
	 * perciba como una sola cosa.
	 */
	private static void desvanecer() {

		if (ventana == null) {
			return;
		}

		if (animacion != null) {
			animacion.stop();
		}

		lienzo.actualizar(1f, 1f);

		if (!puedeDesvanecerse) {
			descartar();
			return;
		}

		long inicio = System.currentTimeMillis();

		Timer salida = new Timer(16, null);
		salida.addActionListener(e -> {

			if (ventana == null) {
				salida.stop();
				return;
			}

			float avance = Math.min(1f, (System.currentTimeMillis() - inicio) / SALIDA);
			ventana.setOpacity(Math.max(0f, 1f - suavizar(avance)));

			if (avance >= 1f) {
				salida.stop();
				descartar();
			}
		});
		salida.start();
	}

	/** Cierra la ventana si sigue abierta. Puede llamarse las veces que haga falta. */
	private static void descartar() {

		if (animacion != null) {
			animacion.stop();
			animacion = null;
		}

		if (ventana != null) {
			ventana.dispose();
			ventana = null;
			lienzo = null;
		}
	}

	/**
	 * Curva <i>ease-out</i> cúbica: empieza rápido y frena al final. Una animación
	 * lineal se percibe mecánica porque nada en el mundo físico se mueve así.
	 */
	private static float suavizar(float t) {

		float x = Math.max(0f, Math.min(1f, t));
		float inverso = 1f - x;

		return 1f - inverso * inverso * inverso;
	}

	/**
	 * No todos los sistemas permiten ventanas semitransparentes. Si este no puede,
	 * el splash aparece y desaparece sin desvanecido: pierde el acabado, pero
	 * funciona igual. Preguntar antes es más barato que capturar la excepción
	 * después.
	 */
	private static boolean soportaTransparencia() {

		GraphicsDevice pantalla = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		return pantalla.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
	}

	/**
	 * Muestra el splash aislado, sin Spring ni base de datos, para poder verlo y
	 * ajustar la animación sin arrancar la aplicación entera:
	 *
	 * <pre>
	 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.brand.SplashScreen"
	 * </pre>
	 *
	 * Simula un arranque de dos segundos, que es lo que tarda la aplicación real.
	 */
	public static void main(String[] args) throws InterruptedException {

		// Maven arranca su JVM en modo "sin pantalla" por defecto; hay que desactivarlo
		// igual que hace la aplicación real antes de crear cualquier ventana.
		System.setProperty("java.awt.headless", "false");
		fp.project.actihome.ui.theme.ActiHomeTheme.install();

		mostrar();
		Thread.sleep(2000);
		cerrar();

		// Se espera a que termine el desvanecido antes de salir del proceso.
		Thread.sleep((long) SALIDA + 300);
		System.exit(0);
	}

	private static void ejecutarEnElHiloDeEventos(Runnable tarea) {

		if (EventQueue.isDispatchThread()) {
			tarea.run();
			return;
		}

		try {
			EventQueue.invokeAndWait(tarea);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (InvocationTargetException e) {
			System.err.println("[SplashScreen] No se pudo mostrar: " + e.getCause());
		}
	}

	/** El dibujo del splash: fondo, lockup, barra de carga y firma. */
	private static class Lienzo extends JPanel {

		private static final long serialVersionUID = 1L;

		private float entrada;
		private float carga;

		void actualizar(float entrada, float carga) {

			this.entrada = entrada;
			this.carga = carga;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {

			super.paintComponent(g);

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			int ancho = getWidth();
			int alto = getHeight();

			pintarFondo(g2, ancho, alto);
			pintarLockup(g2, ancho, alto);
			pintarBarra(g2, ancho, alto);
			pintarFirma(g2, ancho, alto);

			// Filo de un píxel: sin marco de ventana, sobre un escritorio claro el
			// splash se derramaría sin este límite.
			g2.setColor(new Color(0, 0, 0, 46));
			g2.drawRect(0, 0, ancho - 1, alto - 1);

			g2.dispose();
		}

		private void pintarFondo(Graphics2D g2, int ancho, int alto) {

			BufferedImage fondo = BrandAssets.fondo();

			if (fondo == null) {
				g2.setColor(Theme.bg());
				g2.fillRect(0, 0, ancho, alto);
				return;
			}

			// Modo "cubrir": se escala hasta llenar el lienzo conservando la proporción
			// y se recorta lo que sobra. Estirar la imagen hasta que encaje deforma
			// horizontes y círculos, y es de las cosas que peor se ven.
			double escala = Math.max((double) ancho / fondo.getWidth(), (double) alto / fondo.getHeight());
			int nuevoAncho = (int) Math.ceil(fondo.getWidth() * escala);
			int nuevoAlto = (int) Math.ceil(fondo.getHeight() * escala);

			g2.drawImage(fondo, (ancho - nuevoAncho) / 2, (alto - nuevoAlto) / 2, nuevoAncho, nuevoAlto, null);
		}

		private void pintarLockup(Graphics2D g2, int ancho, int alto) {

			BufferedImage lockup = BrandAssets.lockupCocoBrain();

			if (lockup == null) {
				return;
			}

			int anchoLockup = (int) (ancho * 0.78);
			int altoLockup = anchoLockup * lockup.getHeight() / lockup.getWidth();

			// Se asienta: entra 14 píxeles más abajo y sube hasta su sitio.
			int desplazamiento = (int) ((1f - entrada) * 14);

			g2.drawImage(lockup, (ancho - anchoLockup) / 2, (alto - altoLockup) / 2 - 16 + desplazamiento, anchoLockup,
					altoLockup, null);
		}

		private void pintarBarra(Graphics2D g2, int ancho, int alto) {

			int margen = 120;
			int y = alto - 54;
			int largo = ancho - margen * 2;

			g2.setColor(new Color(0, 0, 0, 26));
			g2.fillRect(margen, y, largo, 2);

			g2.setColor(Theme.acc());
			g2.fillRect(margen, y, (int) (largo * carga), 2);
		}

		private void pintarFirma(Graphics2D g2, int ancho, int alto) {

			g2.setFont(Typography.label(10f));
			g2.setColor(Theme.mut());

			String firma = "ACTIHOME";
			int anchoFirma = g2.getFontMetrics().stringWidth(firma);

			g2.drawString(firma, (ancho - anchoFirma) / 2, alto - 26);
		}
	}
}
