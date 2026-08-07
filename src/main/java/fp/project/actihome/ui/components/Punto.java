package fp.project.actihome.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

import fp.project.actihome.ui.theme.Typography;

/**
 * Un disco pequeño que acompaña a una etiqueta de estado.
 *
 * <p>
 * <b>Por qué existe una pieza para algo tan pequeño.</b> Un estado escrito solo
 * con palabras —"Disponible", "Reservada"— obliga a leer para saber si la
 * respuesta es buena o mala. Un punto de color se resuelve antes de leer, y
 * cuando se lee la palabra ya solo confirma. Es la diferencia entre informar y
 * hacer trabajar a quien mira.
 *
 * <p>
 * <b>Y por qué no basta con el color.</b> El punto no sustituye al texto, lo
 * acompaña: alrededor del 8 % de los hombres no distinguen bien el rojo del
 * verde, y una interfaz que codifique un estado <em>solo</em> en el color deja
 * a esa gente sin el dato. La palabra siempre está.
 *
 * <p>
 * <b>El tamaño sale de la fuente, no de un número.</b> Es la regla que más
 * veces se ha tenido que reaprender en este proyecto: cualquier medida que
 * acompañe a un texto tiene que crecer con él, o en un Windows al 150 % el
 * punto se queda de juguete al lado de su etiqueta.
 */
public class Punto extends JComponent {

	private static final long serialVersionUID = 1L;

	private final transient java.util.function.Supplier<Color> color;

	/**
	 * @param color se pide en cada pintado y no se guarda, para que el punto siga a
	 *              la estación activa sin suscribirse a nada — igual que el resto
	 *              del vocabulario visual
	 */
	public Punto(java.util.function.Supplier<Color> color) {

		this.color = color;

		int lado = lado();
		setPreferredSize(new Dimension(lado, lado));

		// Si se declara el preferido hay que declarar el mínimo. Un componente propio
		// que solo define el preferido anuncia un mínimo de CERO, y el layout lo
		// aplasta en cuanto falta sitio, sin avisar y sin que se note en una ventana
		// grande.
		setMinimumSize(new Dimension(lado, lado));
	}

	/** Poco más de un tercio de la altura de una línea de texto pequeño. */
	private static int lado() {
		return Math.max(6, Math.round(Typography.sans(Typography.BODY_SM).getSize() * 0.42f));
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int lado = Math.min(getWidth(), getHeight());

		g2.setColor(color.get());
		g2.fillOval((getWidth() - lado) / 2, (getHeight() - lado) / 2, lado, lado);

		g2.dispose();
	}
}
