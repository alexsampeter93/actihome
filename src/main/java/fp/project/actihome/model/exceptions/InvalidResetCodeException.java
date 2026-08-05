package fp.project.actihome.model.exceptions;

/**
 * El código de recuperación no sirve: no coincide, ha caducado, ya se usó o se
 * han agotado los cinco intentos.
 *
 * <p>
 * <b>Una sola excepción para las cuatro causas, y aquí no es solo comodidad.</b>
 * Distinguirlas le diría a quien esté probando códigos cuál de sus intentos iba
 * por buen camino: "ha caducado" confirma que el código era correcto, y "código
 * incorrecto" confirma que la cuenta existe. Al usuario legítimo le sirve el
 * mismo mensaje —pide otro código— y a quien no lo es no se le regala nada.
 *
 * <p>
 * Es <em>checked</em>, como el resto de excepciones de negocio del proyecto, y
 * eso tiene además una consecuencia útil: Spring solo revierte la transacción
 * ante excepciones no comprobadas, así que el contador de intentos que se
 * incrementa antes de lanzarla <b>se guarda</b>. Con una excepción de tiempo de
 * ejecución se perdería, y el límite de cinco intentos dejaría de existir.
 */
@SuppressWarnings("serial")
public class InvalidResetCodeException extends Exception {

}
