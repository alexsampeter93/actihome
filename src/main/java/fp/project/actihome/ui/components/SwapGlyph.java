package fp.project.actihome.ui.components;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

import javax.swing.JComponent;

import fp.project.actihome.ui.theme.Theme;

/**
 * El símbolo del intercambio: dos flechas opuestas, una encima de la otra.
 *
 * <p>
 * <b>Existe porque el carácter "⇄" (U+21C4) no está en las fuentes del
 * proyecto.</b> Ni Fraunces ni Archivo lo incluyen, y cuando a Swing le falta un
 * glifo dibuja un rectángulo vacío. Estuvo así desde la Fase 4 —el enlace
 * "Intercambiar ⇄" del detalle de alojamiento— sin que se notara, porque un
 * cuadradito junto a un texto correcto se lee como un icono raro y no como un
 * fallo.
 *
 * <p>
 * Es exactamente la misma trampa que ya obligó a dibujar las estrellas de
 * {@link StarRating} con geometría. La regla, ahora sí generalizada: <b>de las
 * fuentes empaquetadas solo se puede dar por hecho el repertorio latino
 * básico</b>. Flechas simples (← →), rayas, comillas angulares y el euro sí
 * están; las flechas dobles, el check y la estrella no. Ante la duda, se
 * comprueba con {@code Font.canDisplay(codePoint)} o se dibuja.
 *
 * <p>
 * Dibujarlo tiene además dos ventajas sobre el carácter: se tiñe con el color
 * que toque —lo resuelve al pintar, como el resto del sistema— y escala sin
 * depender de que la fuente tenga un tamaño óptico adecuado.
 */
public class SwapGlyph extends JComponent {

	private static final long serialVersionUID = 1L;

	private final int lado;
	private final boolean sobreAcento;

	/**
	 * @param lado        alto y ancho del glifo
	 * @param sobreAcento si se pinta encima de una superficie de acento, en cuyo
	 *                    caso el trazo va en el color de texto sobre acento; si no,
	 *                    en el propio acento
	 */
	public SwapGlyph(int lado, boolean sobreAcento) {

		this.lado = lado;
		this.sobreAcento = sobreAcento;

		setPreferredSize(new Dimension(lado, lado));
		setMinimumSize(new Dimension(lado, lado));
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int d = Math.min(getWidth(), getHeight());
		double grosor = Math.max(1.4, d / 14.0);

		g2.setColor(sobreAcento ? Theme.onAccent() : Theme.acc());
		g2.setStroke(new BasicStroke((float) grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		double margen = d * 0.22;
		double largo = d - margen * 2;
		double separacion = d * 0.16;
		double punta = d * 0.13;

		// Flecha superior, apuntando a la derecha.
		double y1 = d / 2.0 - separacion;
		g2.draw(flecha(margen, y1, margen + largo, y1, punta, true));

		// Flecha inferior, apuntando a la izquierda.
		double y2 = d / 2.0 + separacion;
		g2.draw(flecha(margen + largo, y2, margen, y2, punta, false));

		g2.dispose();
	}

	/** Un segmento horizontal con su punta de flecha en el extremo de destino. */
	private Path2D flecha(double xInicio, double yInicio, double xFin, double yFin, double punta,
			boolean haciaLaDerecha) {

		Path2D camino = new Path2D.Double();

		camino.moveTo(xInicio, yInicio);
		camino.lineTo(xFin, yFin);

		int sentido = haciaLaDerecha ? -1 : 1;

		camino.moveTo(xFin + sentido * punta, yFin - punta * 0.72);
		camino.lineTo(xFin, yFin);
		camino.lineTo(xFin + sentido * punta, yFin + punta * 0.72);

		return camino;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(lado, lado);
	}
}
