package fp.project.actihome.ui.components;

import java.awt.Dialog;
import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;

/**
 * Un diálogo modal de sí/no antes de una acción que no se puede deshacer.
 *
 * <p>
 * <b>No existía ningún precedente de esto en el proyecto</b> (Fase 7.5.3,
 * cancelar una reserva). {@link fp.project.actihome.ui.brand.AboutDialog} es el
 * único diálogo modal que había, y es puramente informativo —un botón,
 * "Cerrar"—, así que no sirve de plantilla para preguntar algo y esperar una
 * respuesta. Este sigue el mismo patrón de construcción ({@code JDialog},
 * {@code Page} como raíz, {@code pack()} en vez de un tamaño fijo) y añade lo
 * que faltaba: dos botones y un resultado.
 *
 * <p>
 * <b>Bloquea hasta que se cierra</b>, como {@code JOptionPane.showConfirmDialog}:
 * {@link JDialog#setVisible} con modalidad {@code APPLICATION_MODAL} no
 * devuelve el control hasta que el diálogo se dispone, así que
 * {@link #preguntar} puede devolver directamente un {@code boolean} en vez de
 * pedir un callback — quien lo llama sigue leyendo su propio método de arriba
 * abajo, sin partir la lógica en dos sitios.
 */
public final class Confirmacion {

	private static final int ANCHO_TEXTO = 380;

	private Confirmacion() {
	}

	/**
	 * Muestra la pregunta y espera. Devuelve {@code true} solo si se pulsó el
	 * botón de confirmar; Escape, cerrar la ventana o "Cancelar" devuelven
	 * {@code false}.
	 */
	public static boolean preguntar(Window padre, String titulo, String mensaje, String textoConfirmar) {

		JDialog dialogo = new JDialog(padre, titulo, Dialog.ModalityType.APPLICATION_MODAL);
		boolean[] confirmado = { false };

		JPanel raiz = new Page(new MigLayout("wrap 1, " + Space.insets(Space.XXL, Space.XXL, Space.XL, Space.XXL),
				"[" + ANCHO_TEXTO + "!]", "[]" + Space.LG + "[]" + Space.XL + "[]"));

		raiz.add(Labels.title(titulo), "growx");
		raiz.add(new WrappingText(mensaje), "growx");
		raiz.add(acciones(dialogo, confirmado, textoConfirmar), "growx");

		dialogo.setContentPane(raiz);

		// pack(), no un tamaño fijo: el texto se ajusta al ancho declarado y es el
		// alto el que se adapta a cuánto ocupe en cada escalado del sistema. Misma
		// razón que ya documenta AboutDialog.
		dialogo.pack();

		dialogo.setResizable(false);
		dialogo.setLocationRelativeTo(padre);

		Foco.alPulsarEscape(dialogo, dialogo::dispose);

		dialogo.setVisible(true);

		return confirmado[0];
	}

	private static JPanel acciones(JDialog dialogo, boolean[] confirmado, String textoConfirmar) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]", ""));
		fila.setOpaque(false);

		fila.add(Buttons.primary(textoConfirmar, e -> {
			confirmado[0] = true;
			dialogo.dispose();
		}), "height 44!");

		fila.add(Buttons.link(Textos.t("confirmacion.cancelar"), e -> dialogo.dispose()));

		return fila;
	}
}
