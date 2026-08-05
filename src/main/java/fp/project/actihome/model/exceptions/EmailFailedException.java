package fp.project.actihome.model.exceptions;

/**
 * No se ha podido enviar el correo: sin configuración, servidor caído,
 * credenciales rechazadas o dirección mal formada.
 *
 * <p>
 * Agrupa todas esas causas por lo mismo que {@code TranslationFailedException}
 * agrupa las suyas: acaban todas en la misma alternativa —pedirle el código a un
 * administrador— así que separarlas solo multiplicaría los {@code catch} sin
 * cambiar nada de lo que ve el usuario.
 */
@SuppressWarnings("serial")
public class EmailFailedException extends Exception {

}
