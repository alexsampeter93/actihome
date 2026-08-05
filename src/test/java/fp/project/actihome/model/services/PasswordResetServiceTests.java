package fp.project.actihome.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.PasswordResetCode;
import fp.project.actihome.model.entities.PasswordResetCodeDao;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.IncorrectLoginException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.InvalidResetCodeException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;

/**
 * La recuperación de contraseña (Fase 8.6).
 *
 * <p>
 * Se prueba el camino del <b>código entregado por un administrador</b>, no el
 * del correo: {@code generarCodigoParaEntregar} devuelve el código en claro, así
 * que es el único por el que un test puede obtener uno. El del correo comparte
 * exactamente la misma generación y la misma validación —solo cambia cómo llega
 * el código a su dueño—, así que probar este cubre la lógica de los dos.
 * {@code EmailSender} va sustituido por un doble de todas formas, para que la
 * suite no dependa de que haya un servidor de correo.
 *
 * <p>
 * Lo que se comprueba es <b>lo que protege</b>: que el código no se guarda en
 * claro, que se usa una sola vez, que pedir otro anula el anterior y que los
 * intentos se agotan. Nada de eso se ve funcionando en la pantalla —una
 * recuperación normal parece igual de bien con o sin estas defensas—, y esa es
 * justamente la razón de escribirles un test.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class PasswordResetServiceTests {

	@Autowired
	private UserService userService;

	@Autowired
	private PasswordResetService passwordResetService;

	@Autowired
	private PasswordResetCodeDao resetCodeDao;

	@MockBean
	private EmailSender emailSender;

	private User signUpUser(String username, RoleType role) {

		User user = new User(username, "1234", "name", "surname", "locality", 664567076, username + "@ejemplo.test",
				LocalDateTime.now(), role);

		try {
			userService.signUp(user);
		} catch (DuplicateInstanceException e) {
			throw new RuntimeException(e);
		}

		return user;
	}

	@Test
	public void testCodigoValidoCambiaLaContrasena()
			throws InstanceNotFoundException, NotAuthorizedUserException, InvalidResetCodeException,
			IncorrectLoginException {

		User admin = signUpUser("AdminReset", RoleType.ADMIN);
		signUpUser("Olvidadizo", RoleType.CUSTOMER);

		String codigo = passwordResetService.generarCodigoParaEntregar("Olvidadizo", admin.getId());

		passwordResetService.restablecer("Olvidadizo", codigo, "nuevaClave");

		// La prueba de verdad no es que no lance, es que la contraseña nueva sirve para
		// entrar. Un método que dijera "hecho" sin cambiar nada pasaría cualquier
		// comprobación más floja que esta.
		assertEquals("Olvidadizo", userService.login("Olvidadizo", "nuevaClave").getUsername());
	}

	/**
	 * El código <b>no se guarda en claro</b>.
	 *
	 * <p>
	 * Es la comprobación que más fácil sería no escribir, porque el flujo funciona
	 * igual de bien guardándolo en claro. Lo que protege es a quien tenga acceso a
	 * una copia de seguridad de la base: sin esto, cualquier código vivo que
	 * encontrara ahí le abriría la cuenta.
	 */
	@Test
	public void testElCodigoSeGuardaHasheado() throws InstanceNotFoundException, NotAuthorizedUserException {

		User admin = signUpUser("AdminReset", RoleType.ADMIN);
		User olvidadizo = signUpUser("Olvidadizo", RoleType.CUSTOMER);

		String codigo = passwordResetService.generarCodigoParaEntregar("Olvidadizo", admin.getId());

		PasswordResetCode guardado = resetCodeDao
				.findByUserIdAndUsedAtIsNullOrderByExpiresAtDesc(olvidadizo.getId()).get(0);

		assertNotEquals(codigo, guardado.getCodeHash());
		assertTrue(guardado.getCodeHash().startsWith("$2a$"));
	}

	/** Un código usado no vuelve a valer, aunque no haya caducado. */
	@Test
	public void testUnCodigoSoloSirveUnaVez()
			throws InstanceNotFoundException, NotAuthorizedUserException, InvalidResetCodeException {

		User admin = signUpUser("AdminReset", RoleType.ADMIN);
		signUpUser("Olvidadizo", RoleType.CUSTOMER);

		String codigo = passwordResetService.generarCodigoParaEntregar("Olvidadizo", admin.getId());
		passwordResetService.restablecer("Olvidadizo", codigo, "primera");

		assertThrows(InvalidResetCodeException.class,
				() -> passwordResetService.restablecer("Olvidadizo", codigo, "segunda"));
	}

	/**
	 * Pedir un código nuevo <b>anula el anterior</b>.
	 *
	 * <p>
	 * Sin esto, quien pide tres códigos seguidos porque el correo tarda acabaría
	 * con tres llaves activas a la vez durante quince minutos. Es lo que hace que
	 * "pedir otro" signifique lo que el usuario cree que significa.
	 */
	@Test
	public void testPedirOtroCodigoAnulaElAnterior()
			throws InstanceNotFoundException, NotAuthorizedUserException, InvalidResetCodeException {

		User admin = signUpUser("AdminReset", RoleType.ADMIN);
		signUpUser("Olvidadizo", RoleType.CUSTOMER);

		String primero = passwordResetService.generarCodigoParaEntregar("Olvidadizo", admin.getId());
		String segundo = passwordResetService.generarCodigoParaEntregar("Olvidadizo", admin.getId());

		assertThrows(InvalidResetCodeException.class,
				() -> passwordResetService.restablecer("Olvidadizo", primero, "conElViejo"));

		// Y el nuevo sí funciona: si el test solo comprobara que el viejo falla, una
		// implementación que anulara los dos también pasaría.
		passwordResetService.restablecer("Olvidadizo", segundo, "conElNuevo");
	}

	/**
	 * A los cinco intentos fallidos, el código deja de servir <b>aunque luego se
	 * acierte</b>.
	 *
	 * <p>
	 * El contador se guarda a pesar de que cada intento fallido lanza una excepción,
	 * y eso depende de un detalle de Spring que conviene tener fijado por un test:
	 * solo revierte la transacción ante excepciones NO comprobadas.
	 * {@code InvalidResetCodeException} es comprobada, así que el incremento se
	 * confirma. Con una excepción de tiempo de ejecución, el límite no existiría.
	 */
	@Test
	public void testSeAgotanLosIntentos() throws InstanceNotFoundException, NotAuthorizedUserException {

		User admin = signUpUser("AdminReset", RoleType.ADMIN);
		signUpUser("Olvidadizo", RoleType.CUSTOMER);

		String codigo = passwordResetService.generarCodigoParaEntregar("Olvidadizo", admin.getId());

		for (int i = 0; i < 5; i++) {
			assertThrows(InvalidResetCodeException.class,
					() -> passwordResetService.restablecer("Olvidadizo", "NOESELBUENO", "loQueSea"));
		}

		assertThrows(InvalidResetCodeException.class,
				() -> passwordResetService.restablecer("Olvidadizo", codigo, "yaEsTarde"));
	}

	/** Solo un ADMIN puede generar códigos para otros. */
	@Test
	public void testSoloAdminGeneraCodigos() {

		User cliente = signUpUser("ClienteCurioso", RoleType.CUSTOMER);
		signUpUser("Olvidadizo", RoleType.CUSTOMER);

		assertThrows(NotAuthorizedUserException.class,
				() -> passwordResetService.generarCodigoParaEntregar("Olvidadizo", cliente.getId()));
	}

	/**
	 * Pedir por correo un usuario que no existe <b>no falla</b>.
	 *
	 * <p>
	 * Terminar igual de bien con un nombre real que con uno inventado es lo que
	 * impide usar esta pantalla como comprobador de cuentas registradas. Un test
	 * que solo mirara el camino feliz dejaría pasar una versión que lanzara
	 * {@code InstanceNotFoundException} y delatara qué nombres existen.
	 */
	@Test
	public void testSolicitarPorCorreoNoRevelaSiElUsuarioExiste() {

		passwordResetService.solicitarPorCorreo("NoExisteNadieAsi");
	}

	@Test
	public void testPuedeEnviarCorreoDelegaEnElEnviador() {

		assertFalse(passwordResetService.puedeEnviarCorreo());
	}
}
