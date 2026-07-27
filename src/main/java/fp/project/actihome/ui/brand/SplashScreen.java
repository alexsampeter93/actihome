package fp.project.actihome.ui.brand;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JPanel;
import javax.swing.JWindow;

import fp.project.actihome.ui.theme.BrandAssets;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Pantalla de bienvenida: el lockup "CocoBrain presenta" mientras arranca la
 * aplicación.
 *
 * <p>
 * <b>Por qué existe.</b> Levantar el contexto de Spring y crear la conexión con
 * MySQL tarda unos tres segundos, y hasta ahora esos tres segundos eran
 * <em>nada</em>: ni ventana, ni icono, ni señal de que hubiera pasado algo.
 * Quien abría la aplicación no sabía si había arrancado o si el doble clic no
 * había funcionado.
 *
 * <p>
 * Ese hueco es además el único sitio donde CocoBrain puede ser protagonista sin
 * quitarle nada a ActiHome: cuando aparece la aplicación, la marca ya se ha
 * apartado. Es la diferencia entre firmar una obra y ponerse delante de ella.
 *
 * <p>
 * <b>Detalles que hacen que se comporte bien:</b>
 * <ul>
 * <li>Es un {@link JWindow} y no un {@code JFrame}: sin marco, sin barra de
 * título y <b>sin entrada en la barra de tareas</b>. Un splash que aparece
 * junto a la ventana real en la barra de tareas resulta chapucero.</li>
 * <li>Se muestra <b>antes</b> de arrancar Spring, así que aparece de inmediato;
 * y se crea dentro del hilo de eventos, que es el único desde el que Swing
 * permite tocar componentes.</li>
 * <li>Garantiza un tiempo mínimo en pantalla. Sin él, en un arranque rápido el
 * splash parpadearía durante una fracción de segundo, que se percibe como un
 * fallo gráfico y no como una presentación.</li>
 * </ul>
 */
public final class SplashScreen {

	private static final int ANCHO = 620;
	private static final int ALTO = 420;

	/** Tiempo mínimo visible, en milisegundos. */
	private static final long MINIMO_VISIBLE = 1400;

	private static JWindow ventana;
	private static long mostradoEn;

	private SplashScreen() {
	}

	/** Muestra el splash. Si faltan las imágenes, no hace nada. */
	public static void mostrar() {

		if (BrandAssets.lockupCocoBrain() == null) {
			return;
		}

		ejecutarEnElHiloDeEventos(() -> {

			ventana = new JWindow();
			ventana.setContentPane(new Lienzo());
			ventana.setSize(ANCHO, ALTO);
			ventana.setLocationRelativeTo(null);
			ventana.setVisible(true);

			mostradoEn = System.currentTimeMillis();
		});
	}

	/**
	 * Cierra el splash, esperando si hace falta a que haya estado en pantalla el
	 * tiempo mínimo.
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
				// Si alguien interrumpe la espera, se cierra sin más: no merece la pena
				// retrasar el arranque por una animación. Se restaura la marca de
				// interrupción para no ocultársela a quien venga después.
				Thread.currentThread().interrupt();
			}
		}

		ejecutarEnElHiloDeEventos(() -> {
			ventana.dispose();
			ventana = null;
		});
	}

	/**
	 * Swing solo puede tocarse desde el hilo de eventos. Como esto se llama desde
	 * {@code main}, hay que saltar a ese hilo y <b>esperar</b> a que termine: si no
	 * se esperara, el arranque seguiría mientras el splash aún no existe.
	 */
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

	/** El dibujo del splash: fondo, lockup centrado y firma. */
	private static class Lienzo extends JPanel {

		private static final long serialVersionUID = 1L;

		@Override
		protected void paintComponent(Graphics g) {

			super.paintComponent(g);

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			int ancho = getWidth();
			int alto = getHeight();

			BufferedImage fondo = BrandAssets.fondo();

			if (fondo != null) {
				dibujarCubriendo(g2, fondo, ancho, alto);
			} else {
				g2.setColor(Theme.bg());
				g2.fillRect(0, 0, ancho, alto);
			}

			BufferedImage lockup = BrandAssets.lockupCocoBrain();

			if (lockup != null) {
				int anchoLockup = (int) (ancho * 0.78);
				int altoLockup = anchoLockup * lockup.getHeight() / lockup.getWidth();
				g2.drawImage(lockup, (ancho - anchoLockup) / 2, (alto - altoLockup) / 2 - 10, anchoLockup, altoLockup,
						null);
			}

			g2.setFont(Typography.label(10f));
			g2.setColor(Theme.mut());
			String firma = "ACTIHOME";
			int anchoFirma = g2.getFontMetrics().stringWidth(firma);
			g2.drawString(firma, (ancho - anchoFirma) / 2, alto - 26);

			// Hairline de borde: sin marco de ventana, sin este filo el splash se
			// derramaría sobre el escritorio si el fondo del usuario también es claro.
			g2.setColor(new Color(0, 0, 0, 40));
			g2.drawRect(0, 0, ancho - 1, alto - 1);

			g2.dispose();
		}

		/**
		 * Dibuja la imagen cubriendo todo el lienzo sin deformarla, recortando lo que
		 * sobre. Es el equivalente al {@code background-size: cover} de CSS: estirar
		 * una foto para que encaje es de las cosas que peor se ven.
		 */
		private void dibujarCubriendo(Graphics2D g2, BufferedImage imagen, int ancho, int alto) {

			double escala = Math.max((double) ancho / imagen.getWidth(), (double) alto / imagen.getHeight());
			int nuevoAncho = (int) Math.ceil(imagen.getWidth() * escala);
			int nuevoAlto = (int) Math.ceil(imagen.getHeight() * escala);

			g2.drawImage(imagen, (ancho - nuevoAncho) / 2, (alto - nuevoAlto) / 2, nuevoAncho, nuevoAlto, null);
		}
	}
}
