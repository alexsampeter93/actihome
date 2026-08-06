package fp.project.actihome.model.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fp.project.actihome.model.exceptions.WeatherUnavailableException;

/**
 * Previsión meteorológica con <b>Open-Meteo</b>, el proveedor elegido en F18.
 *
 * <p>
 * <b>Por qué Open-Meteo y no otro, que es la única razón que importa aquí:
 * no pide clave de API.</b> Esta aplicación se reparte como un {@code .exe} y un
 * secreto metido dentro de un ejecutable se extrae descompilándolo — es la misma
 * regla por la que las credenciales de correo viven en variables de entorno. De
 * los servicios meteorológicos con plan gratuito, prácticamente todos
 * (OpenWeatherMap, WeatherAPI, Tomorrow.io) exigen registrarse y llevar una
 * clave encima. Con cualquiera de ellos, la función solo habría funcionado en la
 * máquina de quien la programó.
 *
 * <p>
 * Open-Meteo es además de acceso abierto para uso no comercial, sin registro y
 * sin cuota que administrar. Si algún día esta aplicación se comercializara,
 * <b>este es el punto que habría que revisar</b>, y queda anotado aquí a
 * propósito para que no se descubra tarde.
 *
 * <p>
 * <b>Los tiempos de espera no son opcionales</b>, igual que en la traducción:
 * sin ellos, una red que acepta la conexión y luego no contesta deja la petición
 * colgada indefinidamente. Aquí importa incluso más, porque nadie ha pedido esta
 * información: es la ficha la que la busca sola, así que un hilo colgado sería
 * un coste que el usuario paga sin haber solicitado nada. Ocho segundos.
 *
 * <p>
 * <b>El cliente se crea una vez y se reutiliza</b>: {@code HttpClient} mantiene
 * su propio pozo de conexiones y de hilos, y crear uno por consulta va dejando
 * hilos vivos que nadie recoge.
 *
 * <p>
 * <b>Sobre la privacidad.</b> Se envían dos números —la latitud y la longitud
 * del alojamiento, que es información pública del catálogo— y nada más: ni quién
 * consulta, ni desde dónde, ni ningún dato de la cuenta.
 */
@Component
@ConditionalOnProperty(name = "actihome.meteorologia.habilitada", havingValue = "true", matchIfMissing = true)
public class OpenMeteoWeatherClient implements WeatherClient {

	private static final String ENDPOINT = "https://api.open-meteo.com/v1/forecast";

	private static final Duration ESPERA = Duration.ofSeconds(8);

	/** Lo máximo que acepta el proveedor. Pedir más devuelve un error, no menos días. */
	private static final int MAXIMO_DE_DIAS = 16;

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(ESPERA).build();

	private final ObjectMapper json = new ObjectMapper();

	@Override
	public List<PrevisionDiaria> prevision(double latitud, double longitud, int dias)
			throws WeatherUnavailableException {

		int pedidos = Math.max(1, Math.min(dias, MAXIMO_DE_DIAS));

		try {
			HttpRequest peticion = HttpRequest.newBuilder()
					.uri(URI.create(construirUrl(latitud, longitud, pedidos)))
					.timeout(ESPERA)
					.GET()
					.build();

			HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

			if (respuesta.statusCode() != 200) {
				throw new WeatherUnavailableException();
			}

			return leerPrevision(respuesta.body());

		} catch (IOException ex) {
			// Sin red, DNS que no resuelve, conexión cortada a mitad: todos acaban aquí y
			// todos significan lo mismo de cara a la pantalla.
			throw new WeatherUnavailableException();

		} catch (InterruptedException ex) {
			// Reponer la marca de interrupción antes de salir. Tragársela deja al hilo
			// creyendo que nadie le ha pedido parar, y este método corre dentro de un
			// SwingWorker que sí se puede cancelar — al navegar a otra pantalla, por
			// ejemplo, que es un caso muy real aquí: la previsión llega sola y el usuario
			// no tiene por qué esperarla.
			Thread.currentThread().interrupt();
			throw new WeatherUnavailableException();
		}
	}

	/**
	 * Monta la dirección de la consulta.
	 *
	 * <p>
	 * <b>El {@code Locale.US} del formateo no es un adorno y es justo la clase de
	 * fallo que solo aparece ejecutando el cliente de verdad.</b> Con la
	 * configuración regional española, {@code String.format("%.4f", 37.0955)}
	 * escribe {@code 37,0955} <em>con coma</em>, y la coma es el separador de
	 * listas en esta API: el proveedor leería dos parámetros donde hay uno y
	 * devolvería un error. Es hermano del fallo de la barra vertical documentado
	 * en {@code MyMemoryTranslationClient} — un formato correcto para una persona
	 * y roto para un protocolo.
	 *
	 * <p>
	 * {@code timezone=auto} pide que los días se corten según la hora local del
	 * punto consultado y no la del servidor. Sin eso, "mañana" en Gran Canaria
	 * podría no ser el mismo día que "mañana" en la respuesta.
	 */
	private String construirUrl(double latitud, double longitud, int dias) {

		return ENDPOINT
				+ "?latitude=" + String.format(Locale.US, "%.4f", latitud)
				+ "&longitude=" + String.format(Locale.US, "%.4f", longitud)
				+ "&daily=weather_code,temperature_2m_max,temperature_2m_min"
				+ "&timezone=auto"
				+ "&forecast_days=" + dias;
	}

	/**
	 * Convierte la respuesta en una lista de días.
	 *
	 * <p>
	 * <b>Open-Meteo no devuelve una lista de objetos, devuelve objetos de
	 * listas</b>: un array de fechas, otro de máximas, otro de mínimas, todos
	 * paralelos y correlacionados por posición. Es eficiente para transmitir y
	 * frágil para leer, porque nada garantiza que los cuatro arrays midan lo
	 * mismo. Por eso se recorre hasta <b>el más corto</b> en lugar de hasta el de
	 * fechas: si el proveedor envía un array truncado, se pierden días —que es
	 * recuperable— en vez de reventar con un índice fuera de rango.
	 *
	 * <p>
	 * Se navega con {@code path} y no con {@code get} por lo mismo que en la
	 * traducción: {@code path} devuelve un nodo vacío cuando falta el campo, así
	 * que una respuesta con otra forma acaba en una lista vacía y no en un
	 * {@code NullPointerException}.
	 */
	private List<PrevisionDiaria> leerPrevision(String cuerpo) throws WeatherUnavailableException {

		try {
			JsonNode diario = json.readTree(cuerpo).path("daily");

			JsonNode fechas = diario.path("time");
			JsonNode maximas = diario.path("temperature_2m_max");
			JsonNode minimas = diario.path("temperature_2m_min");
			JsonNode codigos = diario.path("weather_code");

			int cuantos = Math.min(Math.min(fechas.size(), maximas.size()), Math.min(minimas.size(), codigos.size()));

			if (cuantos == 0) {
				throw new WeatherUnavailableException();
			}

			List<PrevisionDiaria> dias = new ArrayList<>();

			for (int i = 0; i < cuantos; i++) {

				// Un día suelto ilegible no invalida la semana entera: se salta. Enseñar seis
				// días de siete es mejor que no enseñar ninguno.
				LocalDate fecha = leerFecha(fechas.get(i).asText(""));

				if (fecha == null) {
					continue;
				}

				dias.add(new PrevisionDiaria(fecha, maximas.get(i).asDouble(), minimas.get(i).asDouble(),
						codigos.get(i).asInt(-1)));
			}

			if (dias.isEmpty()) {
				throw new WeatherUnavailableException();
			}

			return dias;

		} catch (IOException ex) {
			throw new WeatherUnavailableException();
		}
	}

	/** La fecha del proveedor, o {@code null} si no se entiende. */
	private LocalDate leerFecha(String texto) {

		try {
			return LocalDate.parse(texto);

		} catch (DateTimeParseException ex) {
			return null;
		}
	}
}
