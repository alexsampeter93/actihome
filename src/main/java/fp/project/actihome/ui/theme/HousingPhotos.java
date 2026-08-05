package fp.project.actihome.ui.theme;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Fotos de alojamiento subidas por un usuario real, en tiempo de ejecución.
 *
 * <p>
 * Fase 7.9: hasta ahora ningún alojamiento publicado desde la aplicación
 * podía tener foto — solo los seis de ejemplo, empaquetados en el jar por
 * {@code ui.dev.GenerarAssets}. Un archivo elegido en el formulario después
 * de que la aplicación ya esté instalada no puede vivir dentro de un jar ya
 * construido, así que se guarda donde ya vive lo único que esta aplicación
 * escribe en tiempo de ejecución: {@code ${user.home}/.actihome/} (el mismo
 * sitio que la base de datos H2, ver {@code application.yaml}).
 *
 * <p>
 * El nombre de archivo sigue el mismo formato que ya usan los seis ejemplos
 * (el código del alojamiento más la extensión, p.ej. {@code "20001.jpg"}), y
 * es justo lo que {@code Housing.image} guarda: el resto de la aplicación —
 * {@code ui.components.ImagePlaceholder} incluido, vía
 * {@link BrandAssets#fotoDeAlojamiento} — no distingue una foto de ejemplo de
 * una subida de verdad.
 */
public final class HousingPhotos {

	/** Mismo lado que usa {@code GenerarAssets} para los seis ejemplos: una foto real no debe verse ni pesar distinto. */
	private static final int LADO_FOTO = 1200;

	private static final File CARPETA = new File(System.getProperty("user.home"), ".actihome/images/housings");

	private HousingPhotos() {
	}

	/**
	 * Reduce y guarda una foto elegida en el selector de archivos.
	 *
	 * @param nombreArchivo el nombre con el que se va a guardar y con el que
	 *                       {@code Housing.image} la va a buscar después
	 * @param origen         el archivo que eligió el usuario
	 * @throws IOException si no se puede leer como imagen o no se puede escribir
	 */
	public static void guardar(String nombreArchivo, File origen) throws IOException {

		BufferedImage leida = ImageIO.read(origen);

		if (leida == null) {
			throw new IOException("El archivo no se reconoce como una imagen: " + origen.getName());
		}

		BufferedImage reducida = ImageScaling.escalarACaja(leida, LADO_FOTO);

		// A RGB sin alfa: mismo motivo que en GenerarAssets.fotos — JPEG no tiene
		// canal de transparencia y pasarle ARGB tiñe la foto de rosa.
		BufferedImage sinAlfa = ImageScaling.copiar(reducida, reducida.getWidth(), reducida.getHeight(),
				BufferedImage.TYPE_INT_RGB, null);

		CARPETA.mkdirs();
		ImageIO.write(sinAlfa, "jpg", new File(CARPETA, nombreArchivo));
	}

	/**
	 * La foto ya guardada con ese nombre, o {@code null} si no hay ninguna en la
	 * carpeta del usuario.
	 *
	 * <p>
	 * <b>Sin caché, a propósito</b> — a diferencia de {@link BrandAssets#cargar},
	 * que sí cachea porque lee del jar y el jar no cambia en caliente. Una foto de
	 * aquí sí puede cambiar en cualquier momento de la sesión (editar un
	 * alojamiento y reemplazarla), y cachearla obligaría a inventar cómo
	 * invalidar esa caché exactamente cuando toca. Es un archivo local: leerlo de
	 * más no cuesta lo bastante como para complicar el diseño.
	 */
	public static BufferedImage cargar(String nombreArchivo) {

		if (nombreArchivo == null) {
			return null;
		}

		File archivo = new File(CARPETA, nombreArchivo);

		if (!archivo.isFile()) {
			return null;
		}

		try {
			return ImageIO.read(archivo);

		} catch (IOException ex) {
			return null;
		}
	}
}
