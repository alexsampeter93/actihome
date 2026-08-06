package fp.project.actihome.model.services;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fp.project.actihome.model.exceptions.LocationNotFoundException;

/**
 * Geocodificación con el buscador de <b>Open-Meteo</b> (F17).
 *
 * <p>
 * <b>Mismo proveedor que la previsión y por la misma razón: no pide clave.</b>
 * El geocodificador de referencia sería Nominatim, de OpenStreetMap, pero su
 * política de uso exige identificar la aplicación con un contacto real y limita
 * a una consulta por segundo — condiciones razonables para un servicio gratuito
 * y difíciles de garantizar en un ejecutable que se reparte sin saber quién lo
 * usa. Aprovechar el buscador que ya viene con el proveedor meteorológico evita
 * añadir una segunda dependencia con sus propias reglas.
 *
 * <p>
 * <b>Sobre la ubicación tal y como la escribe la gente.</b> Este buscador espera
 * un nombre de población, no una dirección postal, así que "Sierra Nevada,
 * Granada" lo resuelve y "Calle Mayor 3, 2º B" no. Por eso se consulta
 * <b>primero el texto completo y luego solo la última parte</b> tras la coma: en
 * este catálogo la ubicación se escribe como "sitio, provincia", y cuando el
 * sitio concreto no está en el índice, la provincia casi siempre sí. Es una
 * aproximación, y para lo que se usa —el tiempo en la zona— basta de sobra.
 */
@Component
public class OpenMeteoGeocodingClient implements GeocodingClient {

	private static final String ENDPOINT = "https://geocoding-api.open-meteo.com/v1/search";

	private static final Duration ESPERA = Duration.ofSeconds(8);

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(ESPERA).build();

	private final ObjectMapper json = new ObjectMapper();

	@Override
	public Coordenadas localizar(String lugar) throws LocationNotFoundException {

		if (lugar == null || lugar.trim().isEmpty()) {
			throw new LocationNotFoundException();
		}

		for (String intento : intentos(lugar)) {

			Coordenadas encontrado = buscar(intento);

			if (encontrado != null) {
				return encontrado;
			}
		}

		throw new LocationNotFoundException();
	}

	/**
	 * Qué textos se prueban, en orden de más preciso a más general.
	 *
	 * <p>
	 * "Sierra Nevada, Granada" se prueba entero y, si no aparece, como "Granada".
	 * Nunca al revés: empezar por lo general daría siempre un resultado y sería el
	 * equivocado.
	 */
	private List<String> intentos(String lugar) {

		List<String> textos = new ArrayList<>();
		String limpio = lugar.trim();

		textos.add(limpio);

		int coma = limpio.lastIndexOf(',');

		if (coma >= 0 && coma < limpio.length() - 1) {

			String cola = limpio.substring(coma + 1).trim();

			if (!cola.isEmpty()) {
				textos.add(cola);
			}
		}

		return textos;
	}

	/** Un intento suelto. Devuelve {@code null} si ese texto no da resultado. */
	private Coordenadas buscar(String texto) throws LocationNotFoundException {

		try {
			HttpRequest peticion = HttpRequest.newBuilder()
					.uri(URI.create(ENDPOINT + "?name=" + codificar(texto) + "&count=1&language=es&format=json"))
					.timeout(ESPERA)
					.GET()
					.build();

			HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

			if (respuesta.statusCode() != 200) {
				throw new LocationNotFoundException();
			}

			return leerPrimero(respuesta.body());

		} catch (IOException ex) {
			throw new LocationNotFoundException();

		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new LocationNotFoundException();
		}
	}

	/**
	 * El primer resultado, o {@code null} si no hay ninguno.
	 *
	 * <p>
	 * <b>Cuando no encuentra nada, este proveedor omite el campo {@code results}
	 * por completo</b> en lugar de devolver una lista vacía. Con {@code path} eso
	 * da un nodo vacío y {@code size()} contesta cero, así que el caso se trata
	 * solo; con {@code get} habría que comprobar el nulo a mano.
	 */
	private Coordenadas leerPrimero(String cuerpo) throws LocationNotFoundException {

		try {
			JsonNode resultados = json.readTree(cuerpo).path("results");

			if (resultados.size() == 0) {
				return null;
			}

			JsonNode primero = resultados.get(0);

			if (!primero.hasNonNull("latitude") || !primero.hasNonNull("longitude")) {
				return null;
			}

			return new Coordenadas(primero.path("latitude").asDouble(), primero.path("longitude").asDouble(),
					etiquetaDe(primero));

		} catch (IOException ex) {
			throw new LocationNotFoundException();
		}
	}

	/**
	 * "Granada, Andalucía, España" a partir del resultado.
	 *
	 * <p>
	 * Se compone con las partes que vengan y saltándose las que falten, porque no
	 * todos los sitios tienen región: un país pequeño devuelve {@code name} y
	 * {@code country} y nada más. Concatenar a ciegas dejaría comas sueltas.
	 */
	private String etiquetaDe(JsonNode resultado) {

		List<String> partes = new ArrayList<>();

		for (String campo : new String[] { "name", "admin1", "country" }) {

			String valor = resultado.path(campo).asText("").trim();

			if (!valor.isEmpty()) {
				partes.add(valor);
			}
		}

		return String.join(", ", partes);
	}

	private String codificar(String texto) {

		// URLEncoder codifica para formularios, donde el espacio es "+". En una cadena
		// de consulta "%20" es lo correcto y no depende de la buena voluntad del
		// servidor. Misma nota que en MyMemoryTranslationClient.
		return URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
	}
}
