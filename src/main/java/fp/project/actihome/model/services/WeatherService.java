package fp.project.actihome.model.services;

import java.util.List;

import fp.project.actihome.model.exceptions.WeatherUnavailableException;

/**
 * La previsión de un alojamiento, con memoria para no repetir consultas (F18).
 *
 * <p>
 * <b>Por qué hay un servicio y no se llama al cliente directamente desde la
 * pantalla.</b> Por la caché. La ficha de un alojamiento se abre, se cierra y se
 * vuelve a abrir constantemente —es el centro de la navegación del catálogo— y
 * sin memoria cada visita sería otra petición por internet para obtener
 * exactamente la misma respuesta. Eso es lento para el usuario y descortés con
 * un servicio gratuito.
 */
public interface WeatherService {

	/**
	 * @param latitud  grados decimales
	 * @param longitud grados decimales
	 * @param dias     cuántos días de previsión a partir de hoy
	 * @throws WeatherUnavailableException si no hay red o el proveedor falla
	 */
	TiempoDelSitio tiempoDe(double latitud, double longitud, int dias) throws WeatherUnavailableException;
}
