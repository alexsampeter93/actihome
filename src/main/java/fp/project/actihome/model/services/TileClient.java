package fp.project.actihome.model.services;

import java.awt.image.BufferedImage;

import fp.project.actihome.model.exceptions.TilesUnavailableException;

/**
 * Trae una tesela suelta del mapa (F19).
 *
 * <p>
 * Interfaz por el mismo motivo que {@link WeatherClient} y
 * {@link TranslationClient}: detrás hay red, y la red no entra ni en un test ni
 * en una herramienta de medida. Es lo que permite que
 * {@code TileClientDeEjemplo} ocupe su sitio en el CI.
 *
 * <p>
 * <b>Una tesela, no un mapa.</b> Componer varias es cosa de quien pinta, no de
 * quien las trae: el cliente no sabe de qué tamaño es el hueco donde van a
 * caber. Separarlo así también hace que la caché tenga sentido — una tesela la
 * comparten todos los alojamientos de la misma zona.
 */
public interface TileClient {

	/**
	 * @param zoom   nivel de detalle; el mundo entero es 0
	 * @param columna índice horizontal, ya envuelto por {@link Teselas}
	 * @param fila    índice vertical, ya acotado por {@link Teselas}
	 * @return la imagen de 256×256, nunca {@code null}
	 * @throws TilesUnavailableException si no hay red o el proveedor falla
	 */
	BufferedImage tesela(int zoom, int columna, int fila) throws TilesUnavailableException;
}
