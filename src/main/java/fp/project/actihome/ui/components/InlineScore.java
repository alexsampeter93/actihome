package fp.project.actihome.ui.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * La pareja "nota + barra fina" que acompaña a una puntuación: "4,2 ────".
 *
 * <p>
 * Aparece en la fila del catálogo y en el detalle de un alojamiento, y las dos
 * veces con el mismo tratamiento —cifra en serif, barra de acento proporcional
 * a la nota sobre 5—, solo que a tamaños distintos. Antes de este componente
 * cada pantalla pintaba su propia barra a mano; extraerlo aquí es la regla del
 * sistema aplicada con disciplina: <b>si algo se repite una tercera vez, se
 * añade al sistema, no a la pantalla que lo necesita</b>.
 *
 * <p>
 * Deliberadamente <b>no</b> incluye el texto que suele ir después ("12
 * reseñas", "de Lucía"...): eso varía tanto de una pantalla a otra que forzarlo
 * dentro del componente lo complicaría sin necesidad. Quien lo use añade esas
 * etiquetas al lado, en su propia fila.
 */
public class InlineScore extends JPanel {

	private static final long serialVersionUID = 1L;

	public InlineScore(Double puntuacion, float tamanoNota, int anchoBarra) {

		super(new MigLayout(Space.insets(0), "[]" + Space.SM + "[]", ""));
		setOpaque(false);

		JLabel nota = Labels.body(Formato.nota(puntuacion));
		nota.setFont(Typography.sansSemiBold(tamanoNota));

		add(nota);
		add(new Barra(puntuacion, anchoBarra), "w " + anchoBarra + "!, h 3!, aligny center");
	}

	/** El carril y su relleno, proporcional a la nota sobre 5. */
	private static class Barra extends JComponent {

		private static final long serialVersionUID = 1L;

		private final transient Double nota;

		Barra(Double nota, int ancho) {
			this.nota = nota;
			setPreferredSize(new Dimension(ancho, 3));
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int ancho = getWidth();
			int alto = getHeight();

			g2.setColor(Theme.HAIRLINE);
			g2.fillRect(0, 0, ancho, alto);

			if (nota != null) {
				g2.setColor(Theme.acc());
				g2.fillRect(0, 0, (int) Math.round(ancho * Math.max(0, Math.min(5.0, nota)) / 5.0), alto);
			}

			g2.dispose();
		}
	}
}
