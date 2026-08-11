package fp.project.actihome.ui.dev;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComponent;

import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.SearchField;
import fp.project.actihome.ui.theme.ActiHomeTheme;
import fp.project.actihome.ui.theme.Animacion;
import fp.project.actihome.ui.theme.Season;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Retrata los botones y el buscador <b>en todos sus estados y en las cuatro
 * estaciones</b>, en una sola lámina.
 *
 * <pre>
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.MirarControles"
 * </pre>
 *
 * <p>
 * <b>Un control no se puede juzgar en reposo.</b> La mitad del carácter de un
 * botón está en cómo responde, y eso no sale en una captura de pantalla: hay que
 * pasar el ratón por encima, y para entonces ya no puedes comparar con el estado
 * anterior porque lo has perdido de vista. Aquí están los tres estados uno al lado
 * del otro, que es la única forma de ver si la diferencia se nota lo suficiente —
 * y si el reposo se sostiene solo, que es lo que más se olvida.
 *
 * <p>
 * <b>Y en las cuatro estaciones, porque un estado puede funcionar en tres.</b> Es
 * la lección del token de acento partido en dos (ADR-008): el amarillo de verano
 * rompe cosas que en primavera, otoño e invierno se ven perfectas. Una lámina de
 * una sola estación habría dado por bueno el conjunto tres veces de cada cuatro.
 *
 * <p>
 * Mismo espíritu que {@code MirarIconos}: no arranca Spring ni toca la base de
 * datos, porque un botón no sabe qué es un alojamiento.
 */
public final class MirarControles {

	private static final String SALIDA = "docs/progreso/controles.png";

	private static final int ANCHO_CELDA = 260;
	private static final int ALTO_CELDA = 52;
	private static final int AIRE = 26;
	private static final int ALTO_CABECERA = 34;
	private static final int ANCHO_ROTULO = 92;

	private static final String[] ESTADOS = { "reposo", "encima", "pulsado", "reposo", "encima", "pulsado", "reposo",
			"encima" };

	private MirarControles() {
	}

	public static void main(String[] args) throws IOException {

		System.setProperty("java.awt.headless", "false");
		ActiHomeTheme.install();

		// Sin animaciones: cada estado se aplica de golpe, así que la lámina retrata el
		// valor final y no un fotograma a medias. Ver Animacion.desactivarParaHerramientas.
		Animacion.desactivarParaHerramientas();

		Season[] estaciones = Season.values();

		int columnas = ESTADOS.length + 1;
		int ancho = AIRE + ANCHO_ROTULO + columnas * (ANCHO_CELDA + AIRE);
		int alto = AIRE + ALTO_CABECERA + estaciones.length * (ALTO_CELDA + AIRE) + AIRE;

		BufferedImage lamina = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);

		Graphics2D g2 = lamina.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, ancho, alto);

		cabecera(g2);

		for (int fila = 0; fila < estaciones.length; fila++) {

			Theme.cambiarA(estaciones[fila]);

			int y = AIRE + ALTO_CABECERA + fila * (ALTO_CELDA + AIRE);

			// El fondo de la estación detrás de toda la fila: un botón secundario o un
			// enlace se juzgan contra el papel sobre el que van, no contra blanco.
			g2.setColor(Theme.bg());
			g2.fillRect(0, y - AIRE / 2, ancho, ALTO_CELDA + AIRE);

			g2.setFont(Typography.label(11f));
			g2.setColor(Theme.mut());
			g2.drawString(estaciones[fila].nombre().toUpperCase(), AIRE, y + ALTO_CELDA / 2 + 4);

			dibujarFila(g2, y);
		}

		g2.dispose();

		ImageIO.write(lamina, "png", new File(SALIDA));
		System.out.println("Lamina generada: " + SALIDA);

		System.exit(0);
	}

	private static void cabecera(Graphics2D g2) {

		String[] titulos = { "PRIMARIO", "", "", "SECUNDARIO", "", "", "ENLACE", "", "BUSCADOR" };

		g2.setFont(Typography.label(10f));

		for (int i = 0; i < titulos.length; i++) {

			int x = AIRE + ANCHO_ROTULO + i * (ANCHO_CELDA + AIRE);

			if (!titulos[i].isEmpty()) {
				g2.setColor(new Color(0, 0, 0, 150));
				g2.drawString(titulos[i], x, AIRE + 2);
			}

			if (i < ESTADOS.length) {
				g2.setColor(new Color(0, 0, 0, 90));
				g2.setFont(Typography.sans(11f));
				g2.drawString(ESTADOS[i], x, AIRE + 18);
				g2.setFont(Typography.label(10f));
			}
		}
	}

	private static void dibujarFila(Graphics2D g2, int y) {

		JComponent[] celdas = { boton(Buttons.primary("Reservar", null), false, false),
				boton(Buttons.primary("Reservar", null), true, false),
				boton(Buttons.primary("Reservar", null), true, true),
				boton(Buttons.secondary("Cancelar", null), false, false),
				boton(Buttons.secondary("Cancelar", null), true, false),
				boton(Buttons.secondary("Cancelar", null), true, true),
				boton(Buttons.link("Ver resenas", null), false, false),
				boton(Buttons.link("Ver resenas", null), true, false), buscador(false), buscador(true) };

		for (int i = 0; i < celdas.length; i++) {

			int x = AIRE + ANCHO_ROTULO + i * (ANCHO_CELDA + AIRE);

			JComponent celda = celdas[i];

			// **A su tamaño preferido, no al de la celda.** Estirar un botón a 200 puntos
			// para que llene el hueco de la lámina retrata un botón que no existe: en la
			// aplicación mide lo que pide su rótulo más su relleno, y las proporciones de
			// las escuadras y del filete dependen justamente de eso. La primera versión
			// de esta lámina condenó al secundario por un defecto que era suyo.
			int ancho = Math.min(ANCHO_CELDA, celda.getPreferredSize().width);
			int alto = Math.min(ALTO_CELDA, celda.getPreferredSize().height);

			celda.setSize(ancho, alto);
			forzarLayout(celda);

			Graphics2D destino = (Graphics2D) g2.create(x, y + (ALTO_CELDA - alto) / 2, ancho, alto);

			celda.paint(destino);
			destino.dispose();
		}
	}

	/**
	 * Coloca los hijos de un componente que nunca ha estado en una ventana.
	 *
	 * <p>
	 * <b>Sin esto el buscador salía en blanco</b>, y costó un rato entender por qué:
	 * {@code paint} dibuja a los hijos en las coordenadas que tengan, y un
	 * contenedor que nadie ha validado los tiene todos en 0×0. Los botones se
	 * libraban porque se dibujan enteros a sí mismos; en cuanto un control tiene
	 * piezas dentro —la lupa y el campo—, hay que colocarlas a mano. {@code validate()}
	 * no sirve: comprueba primero si el componente es visible y descendiente de una
	 * ventana, y aquí no lo es.
	 */
	private static void forzarLayout(java.awt.Container contenedor) {

		contenedor.doLayout();

		for (java.awt.Component hijo : contenedor.getComponents()) {

			if (hijo instanceof java.awt.Container) {
				forzarLayout((java.awt.Container) hijo);
			}
		}
	}

	/**
	 * Un botón puesto en el estado que se quiere retratar.
	 *
	 * <p>
	 * <b>Se manipula el modelo, no el componente.</b> {@code ButtonModel} es donde
	 * Swing guarda "el ratón está encima" y "está pulsado", y es exactamente la
	 * misma fuente que escucha el botón para animarse. Tocar ahí retrata el estado
	 * de verdad; simular el aspecto por otra vía retrataría lo que la herramienta
	 * cree que pasa, que es como se dan por buenas cosas que no lo están.
	 */
	private static AbstractButton boton(AbstractButton boton, boolean encima, boolean pulsado) {

		boton.getModel().setRollover(encima);
		boton.getModel().setArmed(pulsado);
		boton.getModel().setPressed(pulsado);

		return boton;
	}

	private static JComponent buscador(boolean enfocado) {

		SearchField campo = new SearchField("Buscar por nombre o ciudad", () -> {
			// La lámina no filtra nada.
		});

		campo.mostrarEnfocado(enfocado);

		return campo;
	}
}
