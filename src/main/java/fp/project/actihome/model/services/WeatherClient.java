package fp.project.actihome.model.services;

import java.util.List;

import fp.project.actihome.model.exceptions.WeatherUnavailableException;

/**
 * Pide la previsión meteorológica de un punto del mapa (F18).
 *
 * <p>
 * <b>Es una interfaz por el mismo motivo que {@link TranslationClient}</b>, y no
 * por costumbre: detrás hay una llamada por internet, y una llamada por internet
 * no se puede usar en un test. Con esta costura, las pruebas sustituyen el
 * cliente por uno que devuelve lo que les conviene —incluida una lista vacía o
 * un fallo— y comprueban lo que de verdad le toca comprobar al servicio.
 *
 * <p>
 * Es también el punto por el que se cambiaría de proveedor sin tocar nada más.
 */
public interface WeatherClient {

	/**
	 * @param latitud  grados decimales
	 * @param longitud grados decimales
	 * @param dias     cuántos días se piden a partir de hoy
	 * @return la previsión, un elemento por día, en orden cronológico; nunca
	 *         {@code null}
	 * @throws WeatherUnavailableException si no hay red, el proveedor no responde,
	 *                                     tarda demasiado o contesta con un error
	 */
	List<PrevisionDiaria> prevision(double latitud, double longitud, int dias) throws WeatherUnavailableException;
}
