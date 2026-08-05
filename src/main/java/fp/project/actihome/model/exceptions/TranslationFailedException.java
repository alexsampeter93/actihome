package fp.project.actihome.model.exceptions;

/**
 * La traducción no se ha podido completar: sin red, el proveedor no contesta,
 * tarda demasiado o devuelve algo que no se entiende.
 *
 * <p>
 * <b>Es una sola excepción para todas esas causas a propósito.</b> Distinguir
 * "no hay red" de "el servidor devolvió un 503" tiene sentido en un registro
 * técnico, pero no en la pantalla: al usuario solo le sirve saber que ahora
 * mismo no se puede traducir y que la reseña original sigue ahí. Multiplicar
 * excepciones que acaban todas en el mismo mensaje solo multiplica los
 * {@code catch}.
 */
@SuppressWarnings("serial")
public class TranslationFailedException extends Exception {

}
