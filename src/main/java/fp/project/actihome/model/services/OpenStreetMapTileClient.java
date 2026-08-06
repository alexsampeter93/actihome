package fp.project.actihome.model.services;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.exceptions.TilesUnavailableException;

/**
 * Teselas de <b>OpenStreetMap</b> (F19).
 *
 * <p>
 * <b>Por qué OpenStreetMap y no un servicio de mapas estáticos.</b> Por lo
 * mismo que Open-Meteo en la previsión: <b>no pide clave de API</b>, y esta
 * aplicación se reparte como un {@code .exe} del que cualquier secreto se
 * extrae descompilándolo. Los proveedores que sirven "una URL → una imagen ya
 * compuesta" —que sería más cómodo— piden clave todos.
 *
 * <p>
 * El precio de esa comodidad que no tenemos es que aquí llegan cuadrados de
 * 256×256 y **componerlos es cosa nuestra**. Es la parte que hace
 * {@code MapaDeUbicacion}.
 *
 * <hr>
 *
 * <h2>La política de uso, que aquí no es letra pequeña</h2>
 *
 * OpenStreetMap no cobra ni pide registro, pero <b>sus servidores van con
 * donaciones y tienen capacidad limitada</b>. Su política de uso es explícita, y
 * saltársela hace que te bloqueen sin avisar — con razón. Esta clase la cumple
 * en tres puntos, y los tres son código, no buenas intenciones:
 *
 * <ol>
 * <li><b>Se identifica.</b> La cabecera {@code User-Agent} dice qué aplicación
 * es y dónde vive su código. Un cliente anónimo es lo primero que se corta.</li>
 * <li><b>No descarga por lotes.</b> Solo se piden las teselas que se van a
 * dibujar <em>ahora</em>. Guardar zonas "para usar sin conexión" está prohibido
 * expresamente, y este código no tiene forma de hacerlo aunque se quisiera: no
 * existe ningún método que reciba un área.</li>
 * <li><b>Cachea lo que ya trajo.</b> Volver a abrir la misma ficha no vuelve a
 * pedir nada. Es lo que la política pide y además es lo que hace que el mapa se
 * vea al instante la segunda vez.</li>
 * </ol>
 *
 * <p>
 * <b>Y hay una cuarta obligación que no se cumple aquí sino en pantalla:</b> la
 * atribución. Los datos son de los colaboradores de OpenStreetMap y hay que
 * decirlo donde se vea el mapa. La pinta {@code MapaDeUbicacion}, porque es una
 * obligación de lo que se enseña, no de lo que se descarga.
 *
 * <p>
 * <b>Si esto llegara a tener uso real, este es el punto a revisar</b>, y queda
 * anotado a propósito para que no se descubra tarde: a partir de cierto volumen
 * lo correcto es autoalojar las teselas o pagar a un proveedor. Lo que no vale
 * es crecer sobre infraestructura donada.
 */
@Component
@ConditionalOnProperty(name = "actihome.mapa.habilitado", havingValue = "true", matchIfMissing = true)
public class OpenStreetMapTileClient implements TileClient {

	private static final Logger log = LoggerFactory.getLogger(OpenStreetMapTileClient.class);

	private static final String ENDPOINT = "https://tile.openstreetmap.org";

	private static final Duration ESPERA = Duration.ofSeconds(8);

	/**
	 * Cuánto vale una tesela guardada en disco antes de volver a pedirla.
	 *
	 * <p>
	 * El mapa cambia —abren calles, se editan nombres— pero no de un día para
	 * otro, y guardar para siempre significaría que una corrección en el mapa no
	 * llega nunca. Treinta días es el orden de magnitud que recomienda la propia
	 * política.
	 */
	private static final Duration VALIDEZ = Duration.ofDays(30);

	/**
	 * Cómo se identifica la aplicación.
	 *
	 * <p>
	 * <b>Esto es obligatorio, no cortesía.</b> La política exige un
	 * {@code User-Agent} que identifique la aplicación y permita contactar con
	 * quien la hizo; los clientes anónimos —o los que se hacen pasar por un
	 * navegador— son los primeros en bloquearse. Java pone por defecto algo tan
	 * poco informativo como {@code Java-http-client/17}, que es justo lo que no
	 * sirve.
	 */
	private static final String IDENTIFICACION = "ActiHome/1.0 (aplicacion de escritorio; "
			+ "https://github.com/alexsampeter93/actihome)";

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(ESPERA).build();

	/**
	 * Caché en memoria, además de la de disco.
	 *
	 * <p>
	 * La de disco evita la red; esta evita además leer y decodificar el PNG en cada
	 * repintado. {@code ConcurrentHashMap} porque quien escribe es un
	 * {@code SwingWorker} y puede haber varios a la vez.
	 */
	private final Map<String, BufferedImage> enMemoria = new ConcurrentHashMap<>();

	@Override
	public BufferedImage tesela(int zoom, int columna, int fila) throws TilesUnavailableException {

		String clave = zoom + "/" + columna + "/" + fila;
		BufferedImage guardada = enMemoria.get(clave);

		if (guardada != null) {
			return guardada;
		}

		BufferedImage deDisco = leerDeDisco(clave);

		if (deDisco != null) {
			enMemoria.put(clave, deDisco);
			return deDisco;
		}

		BufferedImage descargada = descargar(clave);

		enMemoria.put(clave, descargada);
		escribirEnDisco(clave, descargada);

		return descargada;
	}

	private BufferedImage descargar(String clave) throws TilesUnavailableException {

		try {
			HttpRequest peticion = HttpRequest.newBuilder()
					.uri(URI.create(ENDPOINT + "/" + clave + ".png"))
					.header("User-Agent", IDENTIFICACION)
					.timeout(ESPERA)
					.GET()
					.build();

			HttpResponse<byte[]> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofByteArray());

			if (respuesta.statusCode() != 200) {
				throw new TilesUnavailableException();
			}

			BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(respuesta.body()));

			// ImageIO devuelve null —no lanza— cuando los bytes no son una imagen que
			// sepa leer. Un error del proveedor servido como HTML entraría por aquí y sin
			// esta comprobación acabaría siendo un NullPointerException tres capas más
			// arriba, que no dice nada de lo que pasó.
			if (imagen == null) {
				throw new TilesUnavailableException();
			}

			return imagen;

		} catch (IOException ex) {
			throw new TilesUnavailableException();

		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new TilesUnavailableException();
		}
	}

	// ------------------------------------------------------------------
	// Caché en disco
	// ------------------------------------------------------------------

	/**
	 * Dónde se guardan.
	 *
	 * <p>
	 * En {@code ~/.actihome/}, junto a la base de datos y a las fotos subidas, por
	 * la misma razón que aquellas: <b>no pueden ir dentro del jar</b>, que ya está
	 * construido cuando alguien abre una ficha. Es lo único que esta aplicación
	 * escribe en tiempo de ejecución.
	 */
	private Path carpeta() {
		return Paths.get(System.getProperty("user.home"), ".actihome", "teselas");
	}

	private Path ficheroDe(String clave) {
		return carpeta().resolve(clave.replace('/', '-') + ".png");
	}

	/** La tesela guardada, o {@code null} si no está o ya caducó. */
	private BufferedImage leerDeDisco(String clave) {

		Path fichero = ficheroDe(clave);

		try {
			if (!Files.exists(fichero)) {
				return null;
			}

			Instant escrita = Files.getLastModifiedTime(fichero).toInstant();

			if (Duration.between(escrita, Instant.now()).compareTo(VALIDEZ) > 0) {
				return null;
			}

			return ImageIO.read(fichero.toFile());

		} catch (IOException ex) {
			// Una caché ilegible no es un fallo del que haya que enterar a nadie: se pide
			// otra vez y ya está. Se registra en el log porque si pasa siempre, sí lo es.
			log.debug("No se ha podido leer la tesela {} de la cache", clave, ex);
			return null;
		}
	}

	private void escribirEnDisco(String clave, BufferedImage imagen) {

		try {
			Files.createDirectories(carpeta());
			ImageIO.write(imagen, "png", ficheroDe(clave).toFile());

		} catch (IOException ex) {
			// Que no se pueda escribir la caché no impide enseñar el mapa: solo hace que
			// la próxima vez haya que volver a pedirlo. No se propaga a propósito.
			log.debug("No se ha podido guardar la tesela {} en la cache", clave, ex);
		}
	}
}
