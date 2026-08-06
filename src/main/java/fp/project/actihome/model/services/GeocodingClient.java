package fp.project.actihome.model.services;

import fp.project.actihome.model.exceptions.LocationNotFoundException;

/**
 * Convierte un nombre de sitio en un punto del mapa (F17).
 *
 * <p>
 * Es lo que permite que un alojamiento publicado desde la aplicación tenga
 * coordenadas <b>sin pedirle al propietario que escriba una latitud</b>. Nadie
 * sabe de memoria que su casa está en el 37,0955; todo el mundo sabe que está en
 * Sierra Nevada.
 *
 * <p>
 * Interfaz por el mismo motivo que {@link WeatherClient} y
 * {@link TranslationClient}: detrás hay red, y la red no entra en un test.
 */
public interface GeocodingClient {

	/**
	 * @param lugar texto libre tal y como lo escribió el usuario
	 * @return el mejor resultado, nunca {@code null}
	 * @throws LocationNotFoundException si no se conoce ese sitio o no se ha podido
	 *                                   preguntar
	 */
	Coordenadas localizar(String lugar) throws LocationNotFoundException;
}
