package fp.project.actihome.ui.dev;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import fp.project.actihome.model.entities.Amenity;
import fp.project.actihome.ui.components.IconoDeComodidad;
import fp.project.actihome.ui.components.IconoDeTipo;
import fp.project.actihome.ui.theme.ActiHomeTheme;
import fp.project.actihome.ui.theme.Animacion;
import fp.project.actihome.ui.theme.Season;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Dibuja los siete iconos de comodidad grandes y en sus dos estados.
 *
 * <pre>
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.MirarIconos"
 * </pre>
 *
 * <p>
 * <b>Existe porque un icono no se puede juzgar al tamaño al que se usa.</b> En
 * la ficha del catálogo miden dieciséis puntos: a esa escala, un dibujo bien
 * proporcionado y uno mal proporcionado son la misma mancha gris, y la única
 * forma de saber si el asa de la taza está donde debe es verla a doscientos.
 * Es el mismo motivo por el que existe {@code MedirGlifos} — comprobar a ojo,
 * pero a un tamaño en el que el ojo pueda comprobar algo.
 *
 * <p>
 * <b>Dibuja también el estado encendido</b>, y eso contesta la pregunta que de
 * verdad importa en estos siete: si el icono <em>apagado</em> ya se entiende.
 * Una animación puede cambiar la intensidad o mover una pieza, pero <b>no puede
 * decidir si el icono existe</b>: la primera versión de estos se dejaba sin
 * dibujar los arcos del wifi mientras nadie pasara el ratón, y en reposo era un
 * punto suelto. En la captura del catálogo no se veía; a doscientos puntos,
 * cantaba.
 *
 * <p>
 * No arranca Spring ni toca la base de datos: estos iconos no saben qué es un
 * alojamiento, solo qué es una comodidad.
 */
public final class MirarIconos {

	private static final String SALIDA = "docs/progreso/iconos-comodidad.png";
	private static final String SALIDA_TIPOS = "docs/progreso/iconos-tipo.png";

	private static final int LADO = 200;
	private static final int AIRE = 40;

	private MirarIconos() {
	}

	public static void main(String[] args) throws IOException {

		System.setProperty("java.awt.headless", "false");
		ActiHomeTheme.install();
		Animacion.desactivarParaHerramientas();

		Theme.cambiarA(Season.OTONO);

		Amenity[] comodidades = Amenity.values();

		int ancho = AIRE + comodidades.length * (LADO + AIRE);
		int alto = AIRE * 3 + 2 * (LADO + AIRE);

		BufferedImage lamina = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);

		Graphics2D g2 = lamina.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g2.setColor(Theme.bg());
		g2.fillRect(0, 0, ancho, alto);

		// Dos filas: arriba apagados, abajo encendidos del todo. Es la comparación que
		// hace falta, porque el fallo típico está en el estado que nadie mira.
		for (int fila = 0; fila < 2; fila++) {

			for (int i = 0; i < comodidades.length; i++) {

				IconoDeComodidad icono = new IconoDeComodidad(comodidades[i]);
				icono.setSize(LADO, LADO);
				icono.setEncendido(fila);

				Graphics2D celda = (Graphics2D) g2.create(AIRE + i * (LADO + AIRE), AIRE + fila * (LADO + AIRE), LADO,
						LADO);
				icono.paint(celda);
				celda.dispose();
			}
		}

		g2.setFont(Typography.sans(Typography.BODY_SM));
		g2.setColor(Theme.mut());

		for (int i = 0; i < comodidades.length; i++) {
			g2.drawString(comodidades[i].etiqueta(), AIRE + i * (LADO + AIRE), alto - AIRE / 2);
		}

		g2.setColor(new Color(0, 0, 0, 40));
		g2.drawLine(0, AIRE + LADO + AIRE / 2, ancho, AIRE + LADO + AIRE / 2);

		g2.dispose();

		ImageIO.write(lamina, "png", new File(SALIDA));
		System.out.println("Lamina generada: " + SALIDA);

		dibujarTipos();

		System.exit(0);
	}

	/**
	 * La otra lámina: los cuatro tipos de alojamiento.
	 *
	 * <p>
	 * Van aparte y no en la misma imagen porque son otra familia y otra pregunta.
	 * En los de comodidad lo que hay que juzgar es si el estado apagado ya se
	 * entiende; aquí lo que hay que juzgar es si los cuatro <b>se distinguen entre
	 * sí</b>, que es un problema distinto: cualquiera de ellos, mirado solo, se lee
	 * como un edificio, y el fallo solo aparece al ponerlos en fila.
	 */
	private static void dibujarTipos() throws IOException {

		String[] tipos = { "Casa", "Apartamento", "Villa", "Cabaña" };

		int ancho = AIRE + tipos.length * (LADO + AIRE);
		int alto = AIRE * 2 + LADO + AIRE;

		BufferedImage lamina = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);

		Graphics2D g2 = lamina.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g2.setColor(Theme.bg());
		g2.fillRect(0, 0, ancho, alto);

		for (int i = 0; i < tipos.length; i++) {

			// Se dibuja a mano y no con un componente porque IconoDeTipo es un Icon: se le
			// pide que se pinte en unas coordenadas, y el color lo toma del componente que
			// se le pasa. Aquí se le pasa un panel con el color puesto a mano.
			javax.swing.JPanel portador = new javax.swing.JPanel();
			portador.setForeground(Theme.accText());

			IconoDeTipoGrande icono = new IconoDeTipoGrande(tipos[i], LADO);
			icono.paintIcon(portador, g2, AIRE + i * (LADO + AIRE), AIRE);
		}

		g2.setFont(Typography.sans(Typography.BODY_SM));
		g2.setColor(Theme.mut());

		for (int i = 0; i < tipos.length; i++) {
			g2.drawString(tipos[i], AIRE + i * (LADO + AIRE), alto - AIRE / 2);
		}

		g2.dispose();

		ImageIO.write(lamina, "png", new File(SALIDA_TIPOS));
		System.out.println("Lamina generada: " + SALIDA_TIPOS);
	}

	/**
	 * Un {@link IconoDeTipo} al tamaño que se le pida.
	 *
	 * <p>
	 * {@code IconoDeTipo} calcula su lado desde la fuente porque en la aplicación
	 * tiene que acompañar a una etiqueta. Aquí hace falta lo contrario —un tamaño
	 * grande y fijo— y en vez de añadirle un constructor que solo usaría esta
	 * herramienta, se envuelve: la clase de producción no se ensucia con una
	 * necesidad que no es suya.
	 */
	private static final class IconoDeTipoGrande implements javax.swing.Icon {

		private final IconoDeTipo delegado;
		private final int lado;

		private IconoDeTipoGrande(String tipo, int lado) {
			this.delegado = new IconoDeTipo(tipo);
			this.lado = lado;
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
		public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {

			Graphics2D g2 = (Graphics2D) g.create();

			double escala = lado / (double) delegado.getIconWidth();
			g2.translate(x, y);
			g2.scale(escala, escala);

			delegado.paintIcon(c, g2, 0, 0);

			g2.dispose();
		}
	}
}
