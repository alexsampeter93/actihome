package fp.project.actihome.ui.theme;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * El algoritmo de reducción de imágenes del proyecto, en un único sitio.
 *
 * <p>
 * Extraído de {@code ui.dev.GenerarAssets} en la Fase 7.9, cuando dejó de ser
 * exclusivo de esa herramienta de desarrollo: la subida real de fotos de
 * alojamiento ({@link HousingPhotos}) necesita reducir en tiempo de ejecución
 * exactamente lo mismo que {@code GenerarAssets} ya reducía en desarrollo, y
 * copiar el algoritmo en dos sitios habría significado mantener dos veces las
 * dos trampas que documenta abajo.
 *
 * <p>
 * <b>Trampa 1: el alfa premultiplicado.</b> Un PNG guarda un color también en
 * los píxeles <em>invisibles</em>, y nada obliga a que ese color sea
 * significativo. Al reducir, la interpolación promedia píxeles vecinos: si
 * mezcla uno opaco del borde de la figura con uno invisible de color oscuro,
 * el resultado es un píxel semitransparente oscuro, y repetido por todo el
 * contorno eso es una orla sucia. Trabajar en
 * {@link BufferedImage#TYPE_INT_ARGB_PRE} hace que un píxel invisible aporte
 * exactamente cero a cualquier promedio.
 *
 * <p>
 * <b>Trampa 2: reducir de golpe.</b> Una interpolación bilineal solo mira los
 * vecinos inmediatos, así que bajar de una imagen grande a una pequeña de una
 * sola vez consulta apenas una fracción del original: el detalle fino se
 * pierde de forma irregular y el borde sale dentado. Bajando a la mitad cada
 * vez, cada paso promedia de verdad todo lo que descarta.
 */
public final class ImageScaling {

	private ImageScaling() {
	}

	/** Escala conservando la proporción, en alfa premultiplicado y por pasos. */
	public static BufferedImage escalarACaja(BufferedImage origen, int caja) {

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

	/** Copia a un tamaño y un tipo de imagen concretos, con la interpolación dada (o ninguna). */
	public static BufferedImage copiar(BufferedImage origen, int ancho, int alto, int tipo, Object interpolacion) {

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
}
