package fp.project.actihome.ui.dev;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Reduce los originales de {@code assets/} al tamaño en que la aplicación los
 * usa y los deja en {@code src/main/resources/images/}.
 *
 * <pre>
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.GenerarAssets"
 * </pre>
 *
 * <p>
 * <b>Por qué existe esta herramienta en vez de arrastrar los archivos a mano.</b>
 * Los originales pesan entre 1 y 1,6 MB cada uno; las dieciséis piezas que
 * necesita la aplicación caben en menos de 3 MB. Pero la diferencia no es solo
 * de peso: reducir bien una imagen con transparencia tiene dos trampas que un
 * editor cualquiera no resuelve solo, y las dos están explicadas abajo. Tenerlo
 * en código hace que regenerar todo sea repetible y que las decisiones queden
 * escritas.
 *
 * <p>
 * <b>Trampa 1: el alfa premultiplicado.</b> Un PNG guarda un color también en
 * los píxeles <em>invisibles</em>, y nada obliga a que ese color sea
 * significativo. Al reducir, la interpolación promedia píxeles vecinos: si
 * mezcla uno opaco del borde de la figura con uno invisible de color oscuro, el
 * resultado es un píxel semitransparente oscuro, y repetido por todo el
 * contorno eso es una <b>orla sucia</b> alrededor del personaje. El render de
 * verano tiene los transparentes casi negros, así que el problema es real y
 * visible. Trabajar en {@link BufferedImage#TYPE_INT_ARGB_PRE} hace que un
 * píxel invisible aporte exactamente cero a cualquier promedio.
 *
 * <p>
 * <b>Trampa 2: reducir de golpe.</b> Una interpolación bilineal solo mira los
 * vecinos inmediatos, así que al bajar de 1500 píxeles a 440 de una vez la
 * mayoría del original ni se consulta: el detalle fino se pierde de forma
 * irregular y el borde sale dentado. Bajando a la mitad cada vez, cada paso
 * promedia de verdad todo lo que descarta.
 */
public final class GenerarAssets {

	private static final String ORIGENES = "assets/References";
	private static final String DESTINO = "src/main/resources/images";

	/** Lado de la ranura más grande de {@code MascotSlot} (220) por dos, para HiDPI. */
	private static final int LADO_OLAZ = 440;

	/** Blanco roto de la baldosa del icono. Muestreado de la hoja de presentación. */
	private static final Color CREMA_ICONO = new Color(0xFD, 0xF5, 0xE8);

	private static final int[] TAMANOS_ICONO = { 16, 24, 32, 48, 64, 128, 256 };

	/** Original → nombre con el que lo pide {@code BrandAssets}. */
	private static final Map<String, String> OLAZ = new LinkedHashMap<>();

	static {
		OLAZ.put("olaz-primavera-1-transparent.png", "olaz-primavera-bienvenida.png");
		OLAZ.put("olaz-primavera-2-transparent.png", "olaz-primavera-accion.png");
		OLAZ.put("olaz-verano-1-transparent.png", "olaz-verano-bienvenida.png");
		// El render bueno de la toalla, entregado después que los otros siete.
		OLAZ.put("IconovERANO.png", "olaz-verano-accion.png");
		OLAZ.put("olaz-otono-1-transparent.png", "olaz-otono-bienvenida.png");
		OLAZ.put("olaz-otono-2-transparent.png", "olaz-otono-accion.png");
		OLAZ.put("olaz-invierno-1-transparent.png", "olaz-invierno-bienvenida.png");
		OLAZ.put("olaz-invierno-2-transparent.png", "olaz-invierno-accion.png");
	}

	private GenerarAssets() {
	}

	public static void main(String[] args) throws IOException {

		File origenes = new File(ORIGENES);
		File destino = new File(DESTINO);

		if (!origenes.isDirectory()) {
			throw new IllegalStateException("Ejecuta desde la raíz del proyecto: no encuentro " + ORIGENES);
		}

		olaz(origenes, new File(destino, "olaz"));
		iconos(origenes, new File(destino, "brand"));
	}

	private static void olaz(File origenes, File destino) throws IOException {

		for (Map.Entry<String, String> pieza : OLAZ.entrySet()) {

			BufferedImage original = ImageIO.read(new File(origenes, pieza.getKey()));

			// Recortar el margen transparente antes de escalar: los originales son
			// lienzos con la figura flotando en medio, y sin recortar la mascota
			// aparecería pequeña y perdida dentro de su ranura.
			BufferedImage reducida = escalarACaja(recortar(original), LADO_OLAZ);

			File salida = new File(destino, pieza.getValue());
			ImageIO.write(reducida, "png", salida);

			System.out.printf("%-34s %4dx%-4d -> %3dx%-3d  %3d KB%n", pieza.getValue(), original.getWidth(),
					original.getHeight(), reducida.getWidth(), reducida.getHeight(), salida.length() / 1024);
		}
	}

	/**
	 * El icono de aplicación: el logotipo sobre un cuadrado redondeado de blanco
	 * roto.
	 *
	 * <p>
	 * Se usa la variante de la <b>"A" azul marino</b> y no la de la "A" blanca. No
	 * es preferencia: el interior del círculo del logotipo es transparente, así que
	 * la letra se apoya directamente en el fondo de la baldosa, y sobre blanco roto
	 * una letra blanca desaparece.
	 */
	private static void iconos(File origenes, File destino) throws IOException {

		BufferedImage logo = recortar(
				ImageIO.read(new File(origenes, "actihome-icon-variante1-azul-transparent.png")));

		for (int lado : TAMANOS_ICONO) {

			BufferedImage icono = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = icono.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			double radio = lado * 0.22;
			g2.setColor(CREMA_ICONO);
			g2.fill(new RoundRectangle2D.Double(0, 0, lado, lado, radio, radio));

			// Margen interior más generoso en los tamaños pequeños: ahí cada píxel de
			// aire cuenta para que la silueta siga leyéndose.
			int margen = Math.max(1, Math.round(lado * (lado <= 32 ? 0.10f : 0.13f)));
			BufferedImage escalado = escalarACaja(logo, lado - margen * 2);

			g2.drawImage(escalado, (lado - escalado.getWidth()) / 2, (lado - escalado.getHeight()) / 2, null);
			g2.dispose();

			File salida = new File(destino, "actihome-icon-" + lado + ".png");
			ImageIO.write(icono, "png", salida);

			System.out.printf("actihome-icon-%-3d %d bytes%n", lado, salida.length());
		}
	}

	/** Escala conservando la proporción, en alfa premultiplicado y por pasos. */
	private static BufferedImage escalarACaja(BufferedImage origen, int caja) {

		double escala = Math.min((double) caja / origen.getWidth(), (double) caja / origen.getHeight());
		int ancho = Math.max(1, (int) Math.round(origen.getWidth() * escala));
		int alto = Math.max(1, (int) Math.round(origen.getHeight() * escala));

		BufferedImage actual = copiar(origen, origen.getWidth(), origen.getHeight(),
				BufferedImage.TYPE_INT_ARGB_PRE, null);

		while (actual.getWidth() / 2 > ancho) {
			actual = copiar(actual, Math.max(ancho, actual.getWidth() / 2), Math.max(alto, actual.getHeight() / 2),
					BufferedImage.TYPE_INT_ARGB_PRE, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		}

		BufferedImage exacta = copiar(actual, ancho, alto, BufferedImage.TYPE_INT_ARGB_PRE,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		// De vuelta a ARGB sin premultiplicar, que es lo que espera un PNG.
		return copiar(exacta, ancho, alto, BufferedImage.TYPE_INT_ARGB, null);
	}

	private static BufferedImage copiar(BufferedImage origen, int ancho, int alto, int tipo, Object interpolacion) {

		BufferedImage destino = new BufferedImage(ancho, alto, tipo);
		Graphics2D g = destino.createGraphics();

		if (interpolacion != null) {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolacion);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}

		g.drawImage((Image) origen, 0, 0, ancho, alto, null);
		g.dispose();

		return destino;
	}

	/** El rectángulo mínimo que contiene todo lo que no es transparente. */
	private static BufferedImage recortar(BufferedImage img) {

		int minX = img.getWidth();
		int minY = img.getHeight();
		int maxX = -1;
		int maxY = -1;

		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {

				if (((img.getRGB(x, y) >>> 24) & 0xFF) > 8) {
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}

		return maxX < 0 ? img : img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
	}
}
