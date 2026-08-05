package fp.project.actihome.model.services;

import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.InvalidResetCodeException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;

/**
 * Recuperar una contraseña olvidada (Fase 8.6), por dos caminos.
 *
 * <p>
 * <b>Los dos caminos generan el mismo tipo de código; solo cambia cómo llega a
 * su dueño.</b> Con servidor de correo configurado, la aplicación lo envía; sin
 * él, un administrador lo genera y lo entrega. Eso hace que el segundo no sea un
 * apaño de emergencia sino una vía completa — la única, de hecho, que funciona
 * en un ejecutable repartido, donde no puede haber credenciales de correo
 * (ver {@link BrevoEmailSender}).
 */
public interface PasswordResetService {

	/**
	 * Si esta instalación puede enviar correo.
	 *
	 * <p>
	 * La pantalla lo pregunta <b>antes</b> de pedir nada, para poder decir desde el
	 * principio qué va a pasar. Prometer un correo que nunca va a llegar deja al
	 * usuario esperando; decirle de entrada que pida el código a un administrador,
	 * no.
	 */
	boolean puedeEnviarCorreo();

	/**
	 * Genera un código y lo envía por correo al usuario.
	 *
	 * <p>
	 * <b>No dice si el usuario existe.</b> Termina igual de bien con un nombre real
	 * que con uno inventado, y es deliberado: contestar "ese usuario no existe"
	 * convierte esta pantalla en un comprobador de cuentas registradas para
	 * cualquiera que quiera probar nombres. Es la misma razón por la que el login
	 * no distingue entre contraseña incorrecta y usuario inexistente.
	 */
	void solicitarPorCorreo(String username);

	/**
	 * Genera un código para un usuario y <b>lo devuelve en claro</b>, para que un
	 * administrador se lo entregue.
	 *
	 * <p>
	 * Es el único punto de todo el sistema donde un código sale sin cifrar, y por
	 * eso es el único que exige rol ADMIN y sí distingue si el usuario existe: aquí
	 * quien pregunta ya está identificado y necesita saber si se ha equivocado de
	 * nombre. El razonamiento de no revelarlo en {@link #solicitarPorCorreo} no
	 * aplica a alguien que ya ha iniciado sesión como administrador.
	 */
	String generarCodigoParaEntregar(String username, Long adminId)
			throws InstanceNotFoundException, NotAuthorizedUserException;

	/**
	 * Cambia la contraseña si el código es correcto, no ha caducado, no se ha
	 * usado y no se han agotado los intentos.
	 *
	 * @throws InvalidResetCodeException si falla cualquiera de esas cuatro cosas.
	 *                                   Una sola excepción para las cuatro: al
	 *                                   usuario le sirve el mismo mensaje, y
	 *                                   distinguirlas le diría a quien esté
	 *                                   probando códigos cuál de sus intentos iba
	 *                                   por buen camino
	 */
	void restablecer(String username, String codigo, String nuevaContrasena) throws InvalidResetCodeException;
}
