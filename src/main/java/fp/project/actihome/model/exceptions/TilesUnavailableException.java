package fp.project.actihome.model.exceptions;

/**
 * No se ha podido obtener una tesela del mapa: sin red, el proveedor no
 * contesta, tarda demasiado o devuelve algo que no es una imagen.
 *
 * <p>
 * Una sola excepción para todas esas causas, por lo mismo que
 * {@link WeatherUnavailableException}. Y con la misma consecuencia en pantalla:
 * <b>el mapa no es algo que el usuario haya pedido</b>, es un bloque informativo
 * que la ficha intenta por su cuenta, así que cuando falla el bloque desaparece
 * y no se enseña ningún error.
 */
@SuppressWarnings("serial")
public class TilesUnavailableException extends Exception {

}
