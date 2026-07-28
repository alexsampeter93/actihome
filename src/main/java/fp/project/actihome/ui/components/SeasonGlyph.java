package fp.project.actihome.ui.components;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import javax.swing.JComponent;

import fp.project.actihome.ui.theme.Season;
import fp.project.actihome.ui.theme.Theme;

/**
 * El pequeño símbolo de la estación que acompaña al wordmark en la cabecera.
 *
 * <p>
 * Un brote, un sol, una hoja o un copo, según la estación activa, dibujados con
 * el color de acento sobre la barra oscura. Es un detalle diminuto —16 píxeles—
 * y hace un trabajo desproporcionado: es lo que convierte el cambio de estación
 * en algo que se <em>nota</em> también arriba, y no solo en el color del fondo.
 *
 * <p>
 * <b>Por qué está dibujado a mano y no es un icono.</b> En el handoff son SVG
 * de cuatro trazos que heredan {@code var(--acc)}. Traerlos como archivo
 * obligaría a mantener cuatro ficheros y a recolorearlos en cada cambio de
 * estación. Cuatro trazos de Java2D se leen igual de bien, pesan cero y toman el
 * color de acento en cada pintado sin que nadie tenga que acordarse.
 *
 * <p>
 * <b>Detalle técnico: el sistema de coordenadas.</b> El diseño original está
 * definido sobre un lienzo de 20×20. En lugar de recalcular cada punto para el
 * tamaño real del componente, se escala la transformación
 * ({@link Graphics2D#scale}) y se dibuja con los números del diseño tal cual.
 * Así el glifo es nítido a cualquier tamaño y las coordenadas del código se
 * pueden comparar una a una con la especificación.
 */
public class SeasonGlyph extends JComponent {

	private static final long serialVersionUID = 1L;

	/** Lado del lienzo de diseño. Todas las coordenadas de abajo van sobre él. */
	private static final double LIENZO = 20.0;

	public SeasonGlyph() {
		this(16);
	}

	public SeasonGlyph(int lado) {
		setPreferredSize(new Dimension(lado, lado));
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		double escala = Math.min(getWidth(), getHeight()) / LIENZO;
		g2.scale(escala, escala);

		g2.setColor(Theme.acc());

		switch (Theme.estacion()) {
		case PRIMAVERA:
			brote(g2);
			break;
		case VERANO:
			sol(g2);
			break;
		case OTONO:
			hoja(g2);
			break;
		case INVIERNO:
		default:
			copo(g2);
			break;
		}

		g2.dispose();
	}

	/** Un tallo y una yema: lo que asoma en primavera. */
	private void brote(Graphics2D g2) {

		g2.setStroke(new BasicStroke(1.7f));
		g2.draw(new Line2D.Double(10, 18, 10, 9));
		g2.draw(new Ellipse2D.Double(7, 2.5, 6, 6));
	}

	/** Disco y ocho rayos. El verano del proyecto es luz, no mar. */
	private void sol(Graphics2D g2) {

		g2.fill(new Ellipse2D.Double(6.6, 6.6, 6.8, 6.8));

		g2.setStroke(new BasicStroke(1.5f));
		g2.draw(new Line2D.Double(10, 1.5, 10, 4.6));
		g2.draw(new Line2D.Double(10, 15.4, 10, 18.5));
		g2.draw(new Line2D.Double(1.5, 10, 4.6, 10));
		g2.draw(new Line2D.Double(15.4, 10, 18.5, 10));
		g2.draw(new Line2D.Double(4, 4, 6.2, 6.2));
		g2.draw(new Line2D.Double(13.8, 13.8, 16, 16));
		g2.draw(new Line2D.Double(16, 4, 13.8, 6.2));
		g2.draw(new Line2D.Double(4, 16, 6.2, 13.8));
	}

	/** Hoja inclinada con su rabillo. */
	private void hoja(Graphics2D g2) {

		AffineTransform anterior = g2.getTransform();
		g2.rotate(Math.toRadians(38), 10.5, 8);
		g2.fill(new Ellipse2D.Double(10.5 - 3.2, 8 - 5, 6.4, 10));
		g2.setTransform(anterior);

		g2.setStroke(new BasicStroke(1.5f));
		g2.draw(new Line2D.Double(8.8, 12.5, 6.5, 18));
	}

	/** Tres trazos cruzados: el copo mínimo que sigue leyéndose a 16px. */
	private void copo(Graphics2D g2) {

		g2.setStroke(new BasicStroke(1.5f));
		g2.draw(new Line2D.Double(10, 2, 10, 18));
		g2.draw(new Line2D.Double(3.2, 6, 16.8, 14));
		g2.draw(new Line2D.Double(16.8, 6, 3.2, 14));
	}

	/** Nombre accesible de lo que se está dibujando. */
	public static String descripcion(Season estacion) {

		switch (estacion) {
		case PRIMAVERA:
			return "Brote";
		case VERANO:
			return "Sol";
		case OTONO:
			return "Hoja";
		default:
			return "Copo de nieve";
		}
	}
}
