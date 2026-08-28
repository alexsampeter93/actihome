package fp.project.actihome.ui.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Disco con la puntuación de un alojamiento o de una reseña.
 *
 * <p>
 * Un círculo relleno con el acento y la nota dentro, en serif. Aparece en tres
 * tamaños según el sitio: pequeño sobre la foto en la vista de cuadrícula,
 * mediano en los listados y grande en el detalle de una reseña.
 *
 * <p>
 * Dos detalles que hacen que se vea bien y que suelen descuidarse:
 * <ul>
 * <li>El número se <b>centra ópticamente</b>, no matemáticamente. Centrar por
 * la caja del texto deja la cifra ligeramente baja, porque esa caja incluye el
 * espacio reservado para las letras con rasgo descendente (la "g", la "p") que
 * aquí no existen. Se corrige usando la altura real de los dígitos.</li>
 * <li>Cuando no hay puntuación se dibuja un círculo <b>hueco</b> con un guion,
 * en lugar de un cero. Un alojamiento sin reseñas no vale cero: no tiene
 * nota, y decir cero sería mentir sobre él.</li>
 * </ul>
 */
public class ScoreDisc extends JComponent {

	private static final long serialVersionUID = 1L;

	public enum Tamano {

		PEQUENO(30, 12f), MEDIANO(44, 16f), GRANDE(64, 24f);

		private final int diametro;
		private final float cuerpo;

		Tamano(int diametro, float cuerpo) {
			this.diametro = diametro;
			this.cuerpo = cuerpo;
		}
	}

	private Double puntuacion;
	private final Tamano tamano;

	public ScoreDisc(Double puntuacion, Tamano tamano) {

		this.puntuacion = puntuacion;
		this.tamano = tamano;
		setPreferredSize(new Dimension(tamano.diametro, tamano.diametro));
		setMinimumSize(new Dimension(tamano.diametro, tamano.diametro));
	}

	public void setPuntuacion(Double puntuacion) {
		this.puntuacion = puntuacion;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int d = Math.min(getWidth(), getHeight());
		boolean sinNota = puntuacion == null;

		if (sinNota) {
			g2.setColor(Theme.FIELD_BORDER);
			g2.drawOval(0, 0, d - 1, d - 1);
		} else {
			g2.setColor(Theme.acc());
			g2.fillOval(0, 0, d, d);
		}

		String texto = sinNota ? "—" : String.format("%.1f", puntuacion).replace('.', ',');

		g2.setFont(Typography.sansSemiBold(tamano.cuerpo));
		g2.setColor(sinNota ? Theme.mut() : Theme.onAccent());

		int anchoTexto = g2.getFontMetrics().stringWidth(texto);
		// Altura real de los dígitos, sin contar el espacio de los rasgos
		// descendentes: es lo que permite centrar ópticamente.
		int altoDigitos = g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent();

		g2.drawString(texto, (d - anchoTexto) / 2, (d + altoDigitos) / 2);

		g2.dispose();
	}
}
