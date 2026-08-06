package fp.project.actihome.ui.catalog;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;

/**
 * Botón circular de "añadir", del handoff: sin degradados ni animaciones.
 *
 * <p>
 * Flota sobre la lista del catálogo en la esquina inferior derecha y solo lo ven
 * los administradores. Se dibuja a mano porque es un disco con una cruz, no un
 * botón con etiqueta: pasarlo por {@code Buttons} obligaría a meter un glifo
 * {@code +} en una fuente, y las de este proyecto no garantizan más que el
 * repertorio latino básico.
 *
 * <p>
 * <b>Vivía como clase interna de {@code ShowHousingsFrame}</b> hasta que aquel
 * archivo se partió. No dependía de nada de la pantalla —solo recibe la acción
 * que ejecuta—, así que estar dentro no le aportaba nada y sí impedía verlo como
 * lo que es: una pieza más del catálogo.
 */
public class BotonMas extends JComponent {

	private static final long serialVersionUID = 1L;

	/** El lado del disco. Es un icono, no un texto: no depende de ninguna fuente. */
	private static final int LADO = 44;

	private final transient Runnable accion;
	private boolean encima;

	public BotonMas(Runnable accion) {

		this.accion = accion;

		setPreferredSize(new Dimension(LADO, LADO));
		setMinimumSize(new Dimension(LADO, LADO));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setToolTipText(Textos.t("catalogo.publicar.tooltipCorto"));

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				BotonMas.this.accion.run();
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				encima = true;
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				encima = false;
				repaint();
			}
		});
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int lado = Math.min(getWidth(), getHeight());
		Color fondo = encima ? Theme.acc() : Theme.hdr();

		g2.setColor(fondo);
		g2.fillOval(0, 0, lado, lado);

		g2.setColor(Theme.bg());
		int centro = lado / 2;
		int brazo = lado / 6;
		g2.fillRect(centro - brazo, centro - 1, brazo * 2, 2);
		g2.fillRect(centro - 1, centro - brazo, 2, brazo * 2);

		g2.dispose();
	}
}
