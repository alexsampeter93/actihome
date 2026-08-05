package fp.project.actihome.ui.theme;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Carga las imágenes de marca empaquetadas en el jar.
 *
 * <p>
 * Un único sitio desde el que pedirlas, con caché y con ausencia elegante: si
 * un archivo falta, se devuelve {@code null} y quien lo pidió decide qué hacer,
 * en lugar de reventar la aplicación por un recurso decorativo.
 *
 * <p>
 * <b>Por qué las imágenes viven en {@code src/main/resources} y no en
 * {@code assets/}.</b> La carpeta {@code assets/} es el archivo de originales:
 * ilustraciones a 1024 y 1152 píxeles que pesan más de un mega cada una. Lo que
 * viaja dentro del jar son versiones reducidas al tamaño en que se van a
 * mostrar. La diferencia es de 29 MB a menos de 3, y la calidad en pantalla es
 * la misma: escalar una imagen enorme en cada pintado no la mejora, solo gasta
 * memoria y tiempo.
 */
public final class BrandAssets {

	/** Tamaños del icono de aplicación. Windows elige el que mejor le encaja en cada sitio. */
	private static final int[] TAMANOS_ICONO = { 16, 24, 32, 48, 64, 128, 256 };

	private static final Map<String, BufferedImage> CACHE = new HashMap<>();

	private BrandAssets() {
	}

	/**
	 * El icono de la aplicación, en todos sus tamaños.
	 *
	 * <p>
	 * Se devuelve una lista y no una sola imagen porque Windows usa tamaños
	 * distintos según el sitio: 16px en la esquina de la ventana, 32px al alternar
	 * con Alt+Tab, 48px o más en la barra de tareas. Dándole todas las versiones,
	 * cada una se ve nítida; dándole solo una, el sistema la escala y se
	 * emborrona.
	 */
	public static List<Image> iconosDeAplicacion() {

		List<Image> iconos = new ArrayList<>();

		for (int tamano : TAMANOS_ICONO) {
			BufferedImage imagen = cargar("/images/brand/actihome-icon-" + tamano + ".png");
			if (imagen != null) {
				iconos.add(imagen);
			}
		}

		return iconos;
	}

	/** El icono de aplicación al tamaño que usa la bandeja del sistema (F11). */
	public static BufferedImage iconoDeBandeja() {
		return cargar("/images/brand/actihome-icon-32.png");
	}

	/** Lockup circular "CocoBrain presenta" del splash. */
	public static BufferedImage lockupCocoBrain() {
		return cargar("/images/brand/cocobrain-presenta.png");
	}

	/** Fondo decorativo. Uso acotado: splash y, si llega a existir, onboarding. */
	public static BufferedImage fondo() {
		return cargar("/images/brand/fondo.png");
	}

	/**
	 * Las dos poses en las que está ilustrado Olaz, en cada estación.
	 *
	 * <p>
	 * No son dos dibujos intercambiables del mismo personaje: cuentan cosas
	 * distintas. En {@link #BIENVENIDA} Olaz está de pie, mirando de frente y con
	 * algo en la mano que tiene que ver con llegar a un sitio —las llaves de una
	 * casa, una maleta, una taza—; en {@link #ACCION} está haciendo algo —en bici,
	 * esquiando, tumbado en la toalla, de ruta con la mochila—. Por eso la pose la
	 * elige la pantalla y no el azar: una que pide datos acompaña mejor con
	 * alguien que te recibe, y una de explorar, con alguien que ya está de viaje.
	 */
	public enum Pose {

		BIENVENIDA("bienvenida"), ACCION("accion");

		private final String sufijo;

		Pose(String sufijo) {
			this.sufijo = sufijo;
		}
	}

	/**
	 * La foto de un alojamiento, o {@code null} si no tiene.
	 *
	 * <p>
	 * Se busca primero en {@link HousingPhotos}, la carpeta donde caen las fotos
	 * que sube un usuario real (Fase 7.9), y solo si no hay ninguna se cae al jar
	 * empaquetado — que es donde viven las de los seis alojamientos de ejemplo.
	 * Es justo la extensión que ya anticipaba este método antes de que existiera:
	 * "pasar mañana a servirlas desde disco... sin tocar la base de datos".
	 *
	 * @param nombre lo que guarda {@code Housing.image}: solo el nombre del
	 *               archivo, nunca una ruta. Que la entidad no sepa dónde viven las
	 *               imágenes es lo que permite tener dos orígenes distintos sin que
	 *               ni el modelo ni quien pinta la foto tengan que saberlo
	 */
	public static BufferedImage fotoDeAlojamiento(String nombre) {

		if (nombre == null || nombre.trim().isEmpty()) {
			return null;
		}

		BufferedImage subida = HousingPhotos.cargar(nombre.trim());

		if (subida != null) {
			return subida;
		}

		return cargar("/images/housings/" + nombre.trim());
	}

	/** Foto adjunta a una reseña (F15), o {@code null} sin foto. Mismo patrón que {@link #fotoDeAlojamiento}. */
	public static BufferedImage fotoDeResena(String nombre) {

		if (nombre == null || nombre.trim().isEmpty()) {
			return null;
		}

		return ReviewPhotos.cargar(nombre.trim());
	}

	/** Olaz en la variante de una estación y una pose. */
	public static BufferedImage olaz(Season estacion, Pose pose) {
		return cargar("/images/olaz/olaz-" + estacion.name().toLowerCase() + "-" + pose.sufijo + ".png");
	}

	private static BufferedImage cargar(String ruta) {

		if (CACHE.containsKey(ruta)) {
			return CACHE.get(ruta);
		}

		try (InputStream in = BrandAssets.class.getResourceAsStream(ruta)) {

			// Se guarda también el null: así un recurso que falta se busca una sola vez
			// en lugar de en cada repintado.
			BufferedImage imagen = in == null ? null : ImageIO.read(in);
			CACHE.put(ruta, imagen);

			if (imagen == null) {
				System.err.println("[BrandAssets] No se encontró " + ruta);
			}

			return imagen;

		} catch (IOException e) {
			System.err.println("[BrandAssets] No se pudo leer " + ruta + ": " + e.getMessage());
			CACHE.put(ruta, null);
			return null;
		}
	}
}
