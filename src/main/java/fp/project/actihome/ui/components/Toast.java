package fp.project.actihome.ui.components;

import java.awt.Color;
import java.awt.Point;
import java.awt.Window;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Aviso breve y no bloqueante, anclado abajo de la ventana que lo lanza, que
 * se cierra solo.
 *
 * <p>
 * Fase 7.8: hasta ahora ningún guardado con éxito daba ninguna señal —la
 * pantalla simplemente navegaba a otro sitio, y quien no se fijara en el
 * cambio no tenía forma de saber si había ido bien—. Un diálogo modal habría
 * sido peor que nada: frenar el flujo para decir "ya está" pesa más que la
 * propia confirmación. Por eso esto no bloquea ni pide ningún clic: se pinta,
 * espera {@value #DURACION_MS} ms y se destruye sola.
 *
 * <p>
 * Simplificación consciente: sin esquinas redondeadas ni animación de
 * entrada/salida. El sistema de diseño pide radios pequeños en superficies,
 * pero un {@link JWindow} sin forma personalizada ya cumple el resto de la
 * regla —sin sombra difusa, un único bloque de color— y no compensaba la
 * complejidad de recortar la ventana para un matiz que aquí apenas se nota.
 */
public final class Toast {

	private static final int DURACION_MS = 2400;

	private Toast() {
	}

	/** Se ignora en silencio si {@code padre} no está visible: no hay dónde anclarla. */
	public static void mostrar(Window padre, String mensaje) {

		if (padre == null || !padre.isShowing()) {
			return;
		}

		JWindow ventana = new JWindow(padre);

		JLabel texto = new JLabel(mensaje);
		texto.setForeground(Color.WHITE);
		texto.setFont(Typography.sansMedium(Typography.BODY_SM));

		JPanel contenido = new JPanel(
				new MigLayout(Space.insets(Space.SM, Space.LG, Space.SM, Space.LG), "[]", "[]"));
		contenido.setBackground(Theme.hdr());
		contenido.add(texto);

		ventana.setContentPane(contenido);
		ventana.pack();

		Point base = padre.getLocationOnScreen();
		int x = base.x + (padre.getWidth() - ventana.getWidth()) / 2;
		int y = base.y + padre.getHeight() - ventana.getHeight() - Space.XXL;
		ventana.setLocation(x, y);

		ventana.setVisible(true);

		Timer temporizador = new Timer(DURACION_MS, e -> ventana.dispose());
		temporizador.setRepeats(false);
		temporizador.start();
	}
}
