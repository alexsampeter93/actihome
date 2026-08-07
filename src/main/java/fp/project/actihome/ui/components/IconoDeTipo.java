package fp.project.actihome.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import javax.swing.Icon;

import fp.project.actihome.ui.theme.Typography;

/**
 * Los cuatro tipos de alojamiento, dibujados: casa, apartamento, villa y
 * cabaña.
 *
 * <p>
 * <b>Cuatro siluetas que tienen que distinguirse entre sí a dieciséis
 * puntos</b>, que es un problema más difícil que dibujar cuatro edificios
 * bonitos. Un icono de "casa" genérico serviría para los cuatro, así que cada
 * uno se construye alrededor de <em>lo único que no comparte con los otros</em>:
 *
 * <ul>
 * <li><b>Casa</b> — tejado a dos aguas y chimenea. Es la forma canónica, y por
 * eso lleva el rasgo que ninguno de los otros tres tiene.</li>
 * <li><b>Apartamento</b> — bloque recto con ventanas en rejilla. Sin tejado
 * inclinado, que es justo lo que un piso no tiene.</li>
 * <li><b>Villa</b> — dos volúmenes de distinta altura, tejado plano y una línea
 * de agua delante. Lo que la separa de una casa no es el tamaño, es que se
 * extiende en horizontal.</li>
 * <li><b>Cabaña</b> — tejado en A que baja hasta el suelo, sin paredes
 * visibles.</li>
 * </ul>
 *
 * <h2>Es un {@link Icon}, no un componente</h2>
 *
 * <p>
 * A diferencia de {@link IconoDeComodidad}, que vive suelto en una fila y por
 * eso es un {@code JComponent}, este siempre va <b>dentro de un chip</b>. Y un
 * {@code Chip} es un {@code JToggleButton}, que ya sabe colocar un icono junto a
 * su etiqueta, alinearlo con la línea base y separarlo con
 * {@code setIconTextGap}. Meterle un componente dentro habría significado
 * reimplementar todo eso a mano.
 *
 * <p>
 * <b>El color se pide al componente que lo pinta</b> ({@code c.getForeground()})
 * y no al tema. Es lo que hace que el icono acompañe al chip cuando está
 * marcado: el chip ya decide si su contenido va en el color sobre acento o en el
 * gris secundario, y preguntar al tema por separado daría dos colores distintos
 * dentro de la misma pastilla.
 *
 * <p>
 * <b>Aquí no hay animación</b>, y es deliberado. El chip ya tiene su propio
 * estado de seleccionado; añadir un movimiento al dibujo sería un segundo
 * mensaje sobre lo mismo.
 */
public class IconoDeTipo implements Icon {

	/** Mismo grosor relativo que los siete iconos de comodidad. */
	private static final double GROSOR = 0.065;

	private final String tipo;
	private final int lado;

	/**
	 * @param tipo el nombre tal cual está en {@code UploadHousingFrame.TIPOS}. Si no
	 *             se reconoce no se dibuja nada, que es mejor que dibujar un icono
	 *             equivocado: un tipo nuevo añadido al modelo debe notarse como
	 *             ausencia y no disfrazarse de casa
	 */
	public IconoDeTipo(String tipo) {

		this.tipo = tipo == null ? "" : tipo;
		this.lado = Math.max(13, Math.round(Typography.sans(Typography.BODY_SM).getSize() * 1.15f));
	}

	@Override
	public int getIconWidth() {
		return lado;
	}

	@Override
	public int getIconHeight() {
		return lado;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		g2.translate(x, y);

		Color color = c != null && c.getForeground() != null ? c.getForeground() : Color.DARK_GRAY;
		g2.setColor(color);
		g2.setStroke(new BasicStroke((float) (lado * GROSOR), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));

		switch (tipo) {
		case "Casa":
			casa(g2, lado);
			break;
		case "Apartamento":
			apartamento(g2, lado);
			break;
		case "Villa":
			villa(g2, lado);
			break;
		case "Cabaña":
			cabana(g2, lado);
			break;
		default:
			break;
		}

		g2.dispose();
	}

	/** Tejado a dos aguas, cuerpo y chimenea. */
	private void casa(Graphics2D g2, double lado) {

		Path2D tejado = new Path2D.Double();
		tejado.moveTo(lado * 0.10, lado * 0.46);
		tejado.lineTo(lado * 0.50, lado * 0.16);
		tejado.lineTo(lado * 0.90, lado * 0.46);
		g2.draw(tejado);

		g2.draw(new Rectangle2D.Double(lado * 0.20, lado * 0.46, lado * 0.60, lado * 0.38));

		// La chimenea: el rasgo que ninguno de los otros tres tiene, y por eso está.
		g2.draw(new Line2D.Double(lado * 0.72, lado * 0.30, lado * 0.72, lado * 0.14));
	}

	/** Bloque recto con ventanas en rejilla. Sin tejado inclinado. */
	private void apartamento(Graphics2D g2, double lado) {

		g2.draw(new Rectangle2D.Double(lado * 0.22, lado * 0.12, lado * 0.56, lado * 0.72));

		// Cuatro ventanas: las suficientes para que se lea "varias plantas" y las
		// pocas suficientes para que a dieciséis puntos no se empasten en una mancha.
		for (int fila = 0; fila < 2; fila++) {
			for (int columna = 0; columna < 2; columna++) {

				double vx = lado * (0.33 + 0.22 * columna);
				double vy = lado * (0.26 + 0.22 * fila);

				g2.draw(new Rectangle2D.Double(vx, vy, lado * 0.12, lado * 0.12));
			}
		}

		// La puerta, que ancla el edificio al suelo y evita que se lea como una
		// ventana gigante.
		g2.draw(new Rectangle2D.Double(lado * 0.44, lado * 0.68, lado * 0.14, lado * 0.16));
	}

	/** Dos volúmenes de distinta altura, tejado plano y una línea de agua delante. */
	private void villa(Graphics2D g2, double lado) {

		g2.draw(new Rectangle2D.Double(lado * 0.10, lado * 0.34, lado * 0.36, lado * 0.34));
		g2.draw(new Rectangle2D.Double(lado * 0.46, lado * 0.18, lado * 0.44, lado * 0.50));

		// El agua. Es lo que convierte "dos cajas" en "una villa" sin dibujar una
		// piscina entera, que a este tamaño no cabría.
		Path2D agua = new Path2D.Double();
		agua.moveTo(lado * 0.08, lado * 0.82);
		agua.curveTo(lado * 0.28, lado * 0.74, lado * 0.42, lado * 0.90, lado * 0.60, lado * 0.82);
		agua.curveTo(lado * 0.74, lado * 0.76, lado * 0.82, lado * 0.88, lado * 0.92, lado * 0.82);

		g2.draw(agua);
	}

	/** Tejado en A que baja hasta el suelo. Sin paredes visibles. */
	private void cabana(Graphics2D g2, double lado) {

		Path2D silueta = new Path2D.Double();
		silueta.moveTo(lado * 0.14, lado * 0.84);
		silueta.lineTo(lado * 0.50, lado * 0.14);
		silueta.lineTo(lado * 0.86, lado * 0.84);
		g2.draw(silueta);

		g2.draw(new Line2D.Double(lado * 0.08, lado * 0.84, lado * 0.92, lado * 0.84));

		// El travesaño y la puerta bajo el vértice: es lo que hace que la A se lea
		// como una construcción y no como un triángulo suelto.
		g2.draw(new Line2D.Double(lado * 0.31, lado * 0.52, lado * 0.69, lado * 0.52));
		g2.draw(new Rectangle2D.Double(lado * 0.41, lado * 0.64, lado * 0.18, lado * 0.20));
	}
}
