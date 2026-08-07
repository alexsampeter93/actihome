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

		System.exit(0);
	}
}
