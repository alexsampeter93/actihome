package fp.project.actihome.ui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Grupo de opciones excluyentes en forma de texto subrayado.
 *
 * <p>
 * Es el control de ordenación del catálogo (Mejor valorados · Precio menor ·
 * Precio mayor). Frente al {@link Segmented}, este no dibuja caja: la opción
 * activa se marca con un subrayado de acento y nada más.
 *
 * <p>
 * <b>Cuándo usar uno y cuándo el otro.</b> El segmentado tiene más peso visual y
 * sirve para decisiones que cambian <em>qué</em> se está viendo —la vista de
 * lista o la de cuadrícula—. Estas opciones cambian solo <em>en qué orden</em>,
 * que es una decisión menor, y por eso van con el tratamiento más ligero. Que
 * dos controles hagan lo mismo con distinto énfasis no es una incoherencia: es
 * jerarquía.
 *
 * <p>
 * Como en el selector de estación, la opción activa se marca con color
 * <b>y</b> subrayado. Dos canales en lugar de uno: quien no distinga bien los
 * colores sigue viendo cuál está elegida.
 */
public class OptionLinks extends JPanel {

	private static final long serialVersionUID = 1L;

	private int activo;

	public OptionLinks(int inicial, Consumer<Integer> alCambiar, String... opciones) {

		super(new MigLayout(Space.insets(0), "", "[]"));

		this.activo = inicial;
		setOpaque(false);

		for (int i = 0; i < opciones.length; i++) {

			final int indice = i;
			Opcion opcion = new Opcion(opciones[i], indice);

			opcion.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {

					if (indice != activo) {
						activo = indice;
						repaint();
						alCambiar.accept(indice);
					}
				}
			});

			add(opcion, "gapleft " + (i == 0 ? 0 : Space.MD));
		}
	}

	public int getActivo() {
		return activo;
	}

	private class Opcion extends JComponent {

		private static final long serialVersionUID = 1L;

		private static final int GROSOR = 2;
		private static final int AIRE = 6;

		private final String texto;
		private final int indice;
		private boolean encima;

		Opcion(String texto, int indice) {

			this.texto = texto.toUpperCase();
			this.indice = indice;

			setFont(Typography.label(11f));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			addMouseListener(new MouseAdapter() {

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
		public Dimension getPreferredSize() {

			return new Dimension(getFontMetrics(getFont()).stringWidth(texto) + 2,
					getFontMetrics(getFont()).getHeight() + AIRE + GROSOR);
		}

		/** Ver la nota de {@code SeasonSelector.Pestana}: sin mínimo, se aplasta. */
		@Override
		public Dimension getMinimumSize() {
			return getPreferredSize();
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			boolean esActivo = indice == activo;
			Color tinta = esActivo ? Theme.acc() : encima ? Theme.txt() : Theme.mut();

			g2.setFont(getFont());
			g2.setColor(tinta);
			g2.drawString(texto, 0, g2.getFontMetrics().getAscent());

			if (esActivo) {
				g2.setColor(Theme.acc());
				g2.fillRect(0, getHeight() - GROSOR, getWidth() - 2, GROSOR);
			}

			g2.dispose();
		}
	}
}
