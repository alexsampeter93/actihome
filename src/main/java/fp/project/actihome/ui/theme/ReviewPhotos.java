package fp.project.actihome.ui.theme;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.imageio.ImageIO;

/**
 * Fotos adjuntas a una reseña, subidas por un usuario real (F15).
 *
 * <p>
 * Mismo mecanismo que {@link HousingPhotos}, con una diferencia obligada por
 * el modelo: un alojamiento tiene un {@code housingCode} de negocio que existe
 * <b>antes</b> de guardarse, así que su foto puede llamarse
 * {@code "20001.jpg"}. Una reseña no tiene ningún identificador propio hasta
 * que {@code ReviewDao.save} le asigna uno autogenerado, y para entonces la
 * foto ya se habría tenido que guardar. En vez de encadenar "guarda la reseña,
 * entérate de su id, guarda la foto, y si algo falla en medio deja una reseña
 * sin foto o una foto sin reseña", el nombre de archivo se genera aquí mismo
 * —un UUID— en el momento en que el usuario elige la foto, sin depender de
 * nada que la base de datos no haya decidido todavía.
 */
public final class ReviewPhotos {

	/** Mismo lado que {@link HousingPhotos}: una reseña no necesita una foto más grande que una ficha. */
	private static final int LADO_FOTO = 1200;

	private static final File CARPETA = new File(System.getProperty("user.home"), ".actihome/images/reviews");

	private ReviewPhotos() {
	}

	/** Un nombre de archivo nuevo, sin colisión posible con ningún otro ya guardado. */
	public static String nombreNuevo() {
		return UUID.randomUUID() + ".jpg";
	}

	/**
	 * Reduce y guarda una foto elegida en el selector de archivos.
	 *
	 * @param nombreArchivo el nombre con el que se va a guardar y con el que
	 *                       {@code Review.image} la va a buscar después —
	 *                       normalmente el que devuelve {@link #nombreNuevo()}
	 * @param origen         el archivo que eligió el usuario
	 * @throws IOException si no se puede leer como imagen o no se puede escribir
	 */
	public static void guardar(String nombreArchivo, File origen) throws IOException {

		BufferedImage leida = ImageIO.read(origen);

		if (leida == null) {
			throw new IOException("El archivo no se reconoce como una imagen: " + origen.getName());
		}

		BufferedImage reducida = ImageScaling.escalarACaja(leida, LADO_FOTO);

		// A RGB sin alfa: mismo motivo que en HousingPhotos — JPEG no tiene canal de
		// transparencia y pasarle ARGB tiñe la foto de rosa.
		BufferedImage sinAlfa = ImageScaling.copiar(reducida, reducida.getWidth(), reducida.getHeight(),
				BufferedImage.TYPE_INT_RGB, null);

		CARPETA.mkdirs();
		ImageIO.write(sinAlfa, "jpg", new File(CARPETA, nombreArchivo));
	}

	/** La foto ya guardada con ese nombre, o {@code null} si no hay ninguna. Sin caché: ver la nota gemela en {@link HousingPhotos#cargar}. */
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
