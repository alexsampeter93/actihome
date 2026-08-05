package fp.project.actihome.model.services;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fp.project.actihome.model.exceptions.TranslationFailedException;

/**
 * Traducción con <b>MyMemory</b>, el proveedor elegido en la Fase 8.5.
 *
 * <p>
 * <b>Por qué MyMemory y no otro.</b> Se buscaba algo que funcionara sin pedirle
 * nada al usuario de la aplicación: sin clave de API que registrar, sin tarjeta
 * y sin nada que instalar. MyMemory cumple los tres —su endpoint anónimo da
 * 5.000 palabras al día— y es el único de los tres candidatos que lo hace.
 * <b>DeepL</b> retiró su plan gratuito permanente en 2026, así que dejaría la
 * función muerta el día que caducara la prueba. <b>LibreTranslate</b> es
 * excelente pero autoalojado: exigiría tener Docker corriendo en la máquina de
 * quien abre el {@code .exe}, que es justo lo contrario de lo que esta
 * aplicación promete.
 *
 * <p>
 * <b>Los tiempos de espera no son opcionales.</b> Sin ellos, una red que acepta
 * la conexión y luego no contesta deja la petición colgada indefinidamente. Con
 * el {@code SwingWorker} de la pantalla eso no congela la interfaz, pero sí deja
 * un "Traduciendo…" que no termina nunca, que para el usuario es lo mismo que
 * estar roto. Diez segundos es tiempo de sobra para traducir un párrafo y poco
 * para esperar mirando.
 *
 * <p>
 * <b>El cliente se crea una vez y se reutiliza.</b> {@code HttpClient} mantiene
 * su propio pozo de conexiones y de hilos; crear uno por traducción funciona,
 * pero va dejando hilos vivos que nadie recoge.
 *
 * <p>
 * <b>Sobre la privacidad, que aquí sí importa.</b> Traducir significa
 * <em>enviar el texto de la reseña a un servidor de un tercero</em>. Es
 * información pública —cualquiera que abra la reseña la lee— así que no hay
 * fuga de datos privados, pero conviene tenerlo escrito: no se envía nunca
 * quién la escribió, ni de qué alojamiento es, ni nada de la cuenta. Solo el
 * título y el cuerpo, por separado.
 */
@Component
public class MyMemoryTranslationClient implements TranslationClient {

	private static final String ENDPOINT = "https://api.mymemory.translated.net/get";

	private static final Duration ESPERA = Duration.ofSeconds(10);

	/**
	 * MyMemory corta las peticiones muy largas. El cuerpo de una reseña son 500
	 * caracteres como mucho (lo impone la columna), así que el recorte no debería
	 * llegar a aplicarse nunca; está por si esa columna crece algún día sin que
	 * nadie se acuerde de este archivo.
	 */
	private static final int MAXIMO = 500;

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(ESPERA).build();

	private final ObjectMapper json = new ObjectMapper();

	@Override
	public String traducir(String texto, String desde, String hasta) throws TranslationFailedException {

		if (texto == null || texto.trim().isEmpty()) {
			return texto;
		}

		if (desde.equals(hasta)) {
			return texto;
		}

		String recortado = texto.length() > MAXIMO ? texto.substring(0, MAXIMO) : texto;

		try {
			// La barra vertical que separa los dos idiomas va como "%7C" y no como "|".
			// La documentación de MyMemory la escribe literal y curl la acepta sin
			// rechistar, pero el "|" es un carácter ILEGAL en una URI según el RFC 3986
			// y java.net.URI sí lo aplica: URI.create revienta con
			// "Illegal character in query". Es la clase de fallo que no aparece
			// probando la API con curl, solo ejecutando el cliente de verdad.
			HttpRequest peticion = HttpRequest.newBuilder()
					.uri(URI.create(ENDPOINT + "?q=" + codificar(recortado) + "&langpair=" + desde + "%7C" + hasta))
					.timeout(ESPERA)
					.GET()
					.build();

			HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

			if (respuesta.statusCode() != 200) {
				throw new TranslationFailedException();
			}

			return leerTraduccion(respuesta.body());

		} catch (IOException ex) {
			// Sin red, DNS que no resuelve, conexión cortada a mitad: todos acaban aquí y
			// todos significan lo mismo de cara al usuario.
			throw new TranslationFailedException();

		} catch (InterruptedException ex) {
			// Reponer la marca de interrupción antes de salir. Tragársela deja al hilo
			// creyendo que nadie le ha pedido parar, y este método corre dentro de un
			// SwingWorker que sí se puede cancelar.
			Thread.currentThread().interrupt();
			throw new TranslationFailedException();
		}
	}

	/**
	 * Saca el texto traducido de la respuesta.
	 *
	 * <p>
	 * Se navega con {@code path} y no con {@code get}: {@code path} devuelve un
	 * nodo vacío cuando falta el campo, mientras que {@code get} devuelve
	 * {@code null} y obliga a comprobar cada nivel. Con una respuesta de un tercero
	 * —que puede cambiar de forma sin avisar— la diferencia es entre un mensaje de
	 * error y un {@code NullPointerException}.
	 */
	private String leerTraduccion(String cuerpo) throws TranslationFailedException {

		try {
			JsonNode raiz = json.readTree(cuerpo);
			String traducido = raiz.path("responseData").path("translatedText").asText("");

			if (traducido.isEmpty()) {
				throw new TranslationFailedException();
			}

			return traducido;

		} catch (IOException ex) {
			throw new TranslationFailedException();
		}
	}

	private String codificar(String texto) {

		// URLEncoder codifica para formularios, donde el espacio es "+". En una cadena
		// de consulta eso lo entienden casi todos los servidores, pero "%20" es lo
		// correcto y no depende de la buena voluntad de nadie.
		return URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
	}
}
