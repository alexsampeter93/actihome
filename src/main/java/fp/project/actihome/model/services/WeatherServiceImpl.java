package fp.project.actihome.model.services;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fp.project.actihome.model.exceptions.WeatherUnavailableException;

/**
 * Previsión con caché en memoria.
 *
 * <p>
 * <b>No lleva {@code @Transactional}, y eso es intencionado</b>, no un olvido.
 * Este servicio no toca la base de datos: abrir una transacción para no usarla
 * retendría una conexión del pozo durante toda la espera de red, que es
 * exactamente el error que la Fase 8.5 sacó de {@code ReviewServiceImpl}. Es la
 * regla del proyecto: nunca se mantiene abierta una transacción a través de una
 * llamada a un sistema externo.
 *
 * <p>
 * <b>Por qué la caché es de memoria y no una tabla</b>, a diferencia de la de
 * traducciones. Una traducción es estable: "Casa con piscina" significa lo mismo
 * hoy que dentro de un año, así que guardarla en disco es puro beneficio. Una
 * previsión es lo contrario —caduca por definición— y una tabla de previsiones
 * viejas solo serviría para enseñar el tiempo de la semana pasada. Aquí la
 * caducidad no es un detalle de implementación, es la naturaleza del dato.
 *
 * <p>
 * <b>Una hora de validez</b>, que es del orden en que los modelos meteorológicos
 * se actualizan de verdad: pedirlo más a menudo devuelve lo mismo. Y cuando
 * caduca, se vuelve a consultar; no se guarda para siempre lo primero que llegó.
 *
 * <p>
 * <b>{@code ConcurrentHashMap} y no un {@code HashMap} normal</b>, porque estas
 * consultas salen de un {@code SwingWorker}: el que escribe es un hilo de fondo y
 * puede haber varios a la vez si el usuario abre dos fichas seguidas. Un
 * {@code HashMap} escrito desde dos hilos no da un error, se corrompe en
 * silencio, que es peor.
 *
 * <p>
 * <b>Un fallo NO se guarda en la caché.</b> Guardarlo convertiría un corte de red
 * de dos segundos en una hora sin previsión, cuando reintentar cuesta una
 * petición.
 */
@Service
public class WeatherServiceImpl implements WeatherService {

	/** Cuánto vale una respuesta antes de volver a preguntar. */
	private static final Duration VALIDEZ = Duration.ofHours(1);

	@Autowired
	private WeatherClient weatherClient;

	private final Map<String, Anotacion> cache = new ConcurrentHashMap<>();

	@Override
	public List<PrevisionDiaria> previsionDe(double latitud, double longitud, int dias)
			throws WeatherUnavailableException {

		String clave = claveDe(latitud, longitud, dias);
		Anotacion guardada = cache.get(clave);

		if (guardada != null && guardada.sigueValiendo()) {
			return guardada.dias();
		}

		List<PrevisionDiaria> recien = weatherClient.prevision(latitud, longitud, dias);

		cache.put(clave, new Anotacion(recien, Instant.now()));

		return recien;
	}

	/**
	 * La clave de la caché.
	 *
	 * <p>
	 * <b>Redondear a cuatro decimales es la parte que importa.</b> Un
	 * {@code double} sin redondear compara por igualdad exacta de bits, así que
	 * dos alojamientos del mismo edificio o el mismo alojamiento leído dos veces
	 * podrían generar claves distintas por un error de coma flotante en la
	 * decimoquinta cifra, y la caché no acertaría nunca. Cuatro decimales son unos
	 * once metros: por debajo de eso la previsión es idéntica.
	 *
	 * <p>
	 * {@code Locale.US} por lo mismo que en el cliente: con la configuración
	 * española el separador decimal es una coma, y aunque aquí no viaje por la
	 * red, una clave que cambia según la configuración regional del equipo es una
	 * bomba de relojería.
	 *
	 * <p>
	 * Y el número de días entra en la clave porque una petición de tres días no
	 * puede servirse desde una respuesta de tres cuando se pidieron siete.
	 */
	private String claveDe(double latitud, double longitud, int dias) {
		return String.format(Locale.US, "%.4f/%.4f/%d", latitud, longitud, dias);
	}

	/** Una respuesta guardada, con la hora a la que llegó. */
	private record Anotacion(List<PrevisionDiaria> dias, Instant cuando) {

		boolean sigueValiendo() {
			return Duration.between(cuando, Instant.now()).compareTo(VALIDEZ) < 0;
		}
	}
}
