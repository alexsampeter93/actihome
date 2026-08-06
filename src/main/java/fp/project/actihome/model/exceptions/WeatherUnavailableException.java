package fp.project.actihome.model.exceptions;

/**
 * No se ha podido obtener la previsión: sin red, el proveedor no contesta, tarda
 * demasiado o devuelve algo que no se entiende.
 *
 * <p>
 * Una sola excepción para todas esas causas, por lo mismo que
 * {@link TranslationFailedException}: al usuario solo le sirve saber que ahora
 * mismo no hay previsión, y la ficha del alojamiento sigue completa sin ella.
 *
 * <p>
 * <b>Y aquí la consecuencia es aún más clara que en la traducción</b>: esto no
 * es una operación que el usuario haya pedido, es un adorno informativo que la
 * pantalla intenta por su cuenta. Así que no se enseña ningún mensaje de error —
 * el bloque sencillamente no aparece. Un aviso rojo por no poder decir el tiempo
 * sería una interrupción por algo que nadie pidió.
 */
@SuppressWarnings("serial")
public class WeatherUnavailableException extends Exception {

}
