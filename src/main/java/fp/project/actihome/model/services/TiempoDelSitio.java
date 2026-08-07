package fp.project.actihome.model.services;

import java.util.List;

/**
 * Todo el tiempo de un sitio: cómo está ahora y cómo estará los próximos días.
 *
 * <p>
 * <b>Van juntos en un solo objeto porque se piden en una sola petición</b>, y
 * esa decisión tiene su razonamiento. Open-Meteo devuelve el bloque
 * {@code current} y el bloque {@code daily} en la misma respuesta si se le piden
 * los dos, así que separarlos en dos métodos habría significado dos viajes por
 * la red para llenar una única pantalla.
 *
 * <p>
 * La contrapartida es que la caché tiene que caducar al ritmo del dato más
 * volátil —el instantáneo— y por tanto la previsión diaria se vuelve a pedir más
 * a menudo de lo que cambia. <b>Sale a cuenta igualmente:</b> con una caché
 * combinada de 15 minutos son 4 peticiones por hora; con dos cachés separadas
 * serían 4 del tiempo actual más 1 de la previsión, o sea 5. Menos tráfico, y un
 * mecanismo en vez de dos.
 *
 * @param ahora cómo está en este momento
 * @param dias  la previsión, un elemento por día, en orden cronológico
 */
public record TiempoDelSitio(TiempoAhora ahora, List<PrevisionDiaria> dias) {

}
