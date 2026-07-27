package fp.project.actihome.ui.theme;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

/**
 * Genera las capturas de la guía de estilo en las cuatro estaciones, sin abrir
 * ninguna ventana.
 *
 * <pre>
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.theme.ThemeSnapshots"
 * </pre>
 *
 * <p>
 * <b>Por qué existe.</b> Capturar la pantalla es frágil: depende de que la
 * ventana esté visible, de que nada la tape, de que tenga el foco y de que el
 * contenido quepa sin hacer scroll. Durante esta misma fase, un intento de
 * capturar la guía acabó fotografiando otra aplicación que estaba encima, y
 * otro se quedó a medias porque el contenido no cabía en la ventana.
 *
 * <p>
 * <b>La alternativa.</b> Un componente de Swing sabe dibujarse sobre cualquier
 * lienzo, no solo sobre la pantalla. Aquí se le da como lienzo una imagen en
 * memoria: el resultado es idéntico, pero es determinista, no necesita que la
 * ventana esté visible y no tiene límite de tamaño, así que entra la guía
 * entera de una vez. Es también la forma en que se generan las galerías de
 * componentes de los sistemas de diseño serios.
 *
 * <p>
 * <b>El detalle que lo hace funcionar.</b> Un componente que nunca se ha
 * mostrado no tiene tamaño: su ancho y su alto son cero, y pintarlo daría una
 * imagen vacía. Por eso se mete en un {@link JFrame} y se llama a
 * {@code pack()}, que recorre el árbol calculando tamaños y posiciones. La
 * ventana no llega a mostrarse nunca.
 */
public final class ThemeSnapshots {

	private static final String DESTINO = "docs/progreso";
	private static final int ANCHO = 1460;

	private ThemeSnapshots() {
	}

	public static void main(String[] args) throws IOException {

		ActiHomeTheme.install();

		String prefijo = args.length > 0 ? args[0] : "guia";

		for (Season estacion : Season.values()) {

			Theme.cambiarA(estacion);

			String nombre = prefijo + "-" + estacion.name().toLowerCase() + ".png";
			File salida = new File(DESTINO, nombre);

			ImageIO.write(dibujar(new StyleGuidePanel(false)), "png", salida);
			System.out.println("Captura generada: " + salida.getPath());
		}
	}

	/** Coloca el panel en una ventana sin mostrarla, lo mide y lo pinta en una imagen. */
	private static BufferedImage dibujar(StyleGuidePanel panel) {

		JFrame ventana = new JFrame();
		ventana.setUndecorated(true);
		ventana.getContentPane().add(panel);

		// addNotify hace que el árbol de componentes sea "displayable": a partir de
		// aquí tienen contexto gráfico y pueden medir texto. Sin esto, cualquier
		// cálculo de tamaño que dependa de la tipografía saldría a cero.
		//
		// No se usa pack(): pack ajusta la VENTANA al contenido, y una ventana de 2800
		// píxeles de alto no cabe en la pantalla, así que Windows la recorta y el
		// layout se calcula sobre un alto que no es el real. El resultado fueron
		// componentes pintados en sitios equivocados. Aquí no queremos una ventana
		// usable: queremos maquetar a tamaño completo, aunque ese tamaño no quepa en
		// ningún monitor.
		ventana.addNotify();

		// Dos pasadas: la primera fija el ancho para que los layouts calculen cuántas
		// filas necesitan; la segunda aplica el alto resultante.
		panel.setSize(ANCHO, 100);
		disponer(panel);

		int alto = panel.getPreferredSize().height;
		panel.setSize(new Dimension(ANCHO, alto));
		disponer(panel);

		BufferedImage imagen = new BufferedImage(ANCHO, alto, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = imagen.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// printAll y no paint: printAll pinta el componente y todos sus hijos de forma
		// síncrona, que es justo lo que hace falta al dibujar fuera de la pantalla.
		panel.printAll(g2);

		g2.dispose();
		ventana.dispose();

		return imagen;
	}

	/**
	 * Recorre el árbol colocando cada componente.
	 *
	 * <p>
	 * Un componente que nunca se ha mostrado no recibe el ciclo de validación que
	 * Swing dispara al pintar en pantalla, así que hay que provocarlo a mano: cada
	 * contenedor coloca a sus hijos y luego cada hijo coloca a los suyos.
	 * {@code getTreeLock()} es el cerrojo que Swing usa para que nadie modifique la
	 * jerarquía a mitad del recorrido.
	 */
	private static void disponer(java.awt.Component componente) {

		synchronized (componente.getTreeLock()) {

			componente.doLayout();

			if (componente instanceof java.awt.Container) {
				for (java.awt.Component hijo : ((java.awt.Container) componente).getComponents()) {
					disponer(hijo);
				}
			}
		}
	}
}
