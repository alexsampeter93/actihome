package fp.project.actihome.ui.components;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

import fp.project.actihome.ui.theme.Theme;

/**
 * Línea de separación de un píxel.
 *
 * <p>
 * En este diseño el <em>hairline</em> hace el trabajo que en otras interfaces
 * hacen las sombras y las cajas: separar sin encerrar. Es el recurso de la
 * maquetación editorial, donde una línea fina basta para decir "aquí termina
 * una cosa y empieza otra" sin añadir peso visual.
 *
 * <p>
 * Un píxel de verdad: no se dibuja con un borde de componente porque en
 * pantallas HiDPI acabaría escalado a dos o tres píxeles y perdería la
 * finura que le da sentido.
 */
public class Hairline extends JComponent {

	private static final long serialVersionUID = 1L;

	private final boolean horizontal;

	private Hairline(boolean horizontal) {

		this.horizontal = horizontal;
		setPreferredSize(horizontal ? new Dimension(1, 1) : new Dimension(1, 1));
	}

	/** Separador horizontal: ocupa todo el ancho disponible. */
	public static Hairline horizontal() {
		return new Hairline(true);
	}

	/** Separador vertical: ocupa todo el alto disponible. */
	public static Hairline vertical() {
		return new Hairline(false);
	}

	@Override
	protected void paintComponent(Graphics g) {

		g.setColor(Theme.HAIRLINE);

		if (horizontal) {
			g.fillRect(0, 0, getWidth(), 1);
		} else {
			g.fillRect(0, 0, 1, getHeight());
		}
	}
}
