package fp.project.actihome.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.EstacionPreferida;
import fp.project.actihome.model.entities.User.Idioma;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.IncorrectLoginException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.WrongPasswordException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class UserServiceTests {

	@Autowired
	private UserService userService;

	private User createUser(String username) {

		return new User(username, "password", "name", "surname", "locality", 664567076, username + "@" + username,
				LocalDateTime.now(), RoleType.ADMIN);
	}

	@Test
	public void testSignUpAndLoginFromId() throws DuplicateInstanceException, InstanceNotFoundException {

		User user = createUser("Sampi");

		userService.signUp(user);

		User loggedIn = userService.loginFromId(user.getId());

		assertEquals(user, loggedIn);
	}

	@Test
	public void testSignUpWithUsedUsername() throws DuplicateInstanceException {

		User user1 = createUser("Sampi");
		User user2 = createUser("Sampi");

		userService.signUp(user1);
		assertThrows(DuplicateInstanceException.class, () -> userService.signUp(user2));

	}

	@Test
	public void testLogin() throws DuplicateInstanceException, IncorrectLoginException {

		User user = createUser("Sampi");

		userService.signUp(user);

		User loggedInUser = userService.login("Sampi", "password");

		assertEquals(user, loggedInUser);
	}

	@Test
	public void testLoginWithWrongCredentials() throws DuplicateInstanceException, IncorrectLoginException {

		User user = createUser("Sampi");

		userService.signUp(user);

		assertThrows(IncorrectLoginException.class, () -> userService.login("Sampi", "pass"));

	}

	@Test
	public void testUpdateProfile() throws DuplicateInstanceException, InstanceNotFoundException {

		User user = createUser("Sampi");

		userService.signUp(user);

		User updatedUser = userService.updateProfile(user.getId(), "Sampi1999", "Alejandro", "Sampedro",
				"alejsamcalo" + "@" + "gmail.com", 654992100, "Toledo");

		assertEquals(updatedUser.getUsername(), "Sampi1999");
		assertEquals(updatedUser.getName(), "Alejandro");
		assertEquals(updatedUser.getSurname(), "Sampedro");
		assertEquals(updatedUser.getEmail(), "alejsamcalo" + "@" + "gmail.com");
		assertEquals(updatedUser.getPhoneNumber(), 654992100);
		assertEquals(updatedUser.getLocality(), "Toledo");

	}

	@Test
	public void testUpdateProfileUserNotFound() {

		assertThrows(InstanceNotFoundException.class, () -> userService.updateProfile(Long.valueOf(20), "Sampi1999",
				"Aleiandro", "Sampedro", "alejsamcalo" + "@" + "gmail.com", 654992100, "Toledo"));
	}

	@Test
	public void testUpdateProfileWithUsedUsername() throws DuplicateInstanceException {

		User user1 = createUser("Sampi1");
		User user2 = createUser("Sampi1999");

		userService.signUp(user1);
		userService.signUp(user2);

		assertThrows(DuplicateInstanceException.class, () -> userService.updateProfile(user1.getId(), "Sampi1999",
				"Aleiandro", "Sampedro", "alejsamcalo" + "@" + "gmail.com", 654992100, "Toledo"));
	}

	@Test
	public void testChangePasswordUserNotFound() {

		assertThrows(InstanceNotFoundException.class,
				() -> userService.changePassword(Long.valueOf(21), "password", "b"));
	}

	@Test
	public void testChangePasswordOldPasswordDoesntMatch() throws DuplicateInstanceException {

		User user = createUser("Sampi");

		userService.signUp(user);

		assertThrows(WrongPasswordException.class, () -> userService.changePassword(user.getId(), "wrong", "b"));

	}

	@Test
	public void testChangeRoleAlternaEntreLosDosRoles()
			throws DuplicateInstanceException, InstanceNotFoundException {

		User user = createUser("Sampi");
		userService.signUp(user);

		RoleType inicial = user.getRole();

		User cambiado = userService.changeRole(user.getId());
		assertNotEquals(inicial, cambiado.getRole());

		// Y vuelve: es un interruptor, no un camino de ida.
		User devuelto = userService.changeRole(user.getId());
		assertEquals(inicial, devuelto.getRole());
	}

	@Test
	public void testChangeRoleSeGuardaDeVerdad() throws DuplicateInstanceException, InstanceNotFoundException {

		User user = createUser("Sampi");
		userService.signUp(user);

		RoleType inicial = user.getRole();
		userService.changeRole(user.getId());

		// Se vuelve a leer desde el servicio, no se mira el objeto que devolvió el
		// cambio. Es lo que de verdad importa: la autorización de los demás servicios
		// consulta la base de datos, así que un cambio que solo viviera en memoria
		// dejaría la interfaz y los permisos diciendo cosas distintas.
		assertNotEquals(inicial, userService.loginFromId(user.getId()).getRole());
	}

	@Test
	public void testChangeRoleUserNotFound() {

		assertThrows(InstanceNotFoundException.class, () -> userService.changeRole(Long.valueOf(9999)));
	}

	@Test
	public void testUpdatePreferencesNuevaCuenta() throws DuplicateInstanceException {

		User user = createUser("Sampi");
		userService.signUp(user);

		// Una cuenta recién creada, antes de pasar por Ajustes, se comporta como
		// siempre: sin estación guardada, con partículas activas y en español.
		assertEquals(null, user.getDefaultSeason());
		assertEquals(true, user.isParticlesEnabled());
		assertEquals(Idioma.ES, user.getLanguage());
	}

	@Test
	public void testUpdatePreferences() throws DuplicateInstanceException, InstanceNotFoundException {

		User user = createUser("Sampi");
		userService.signUp(user);

		User actualizado = userService.updatePreferences(user.getId(), EstacionPreferida.INVIERNO, false, Idioma.EN,
				true);

		assertEquals(EstacionPreferida.INVIERNO, actualizado.getDefaultSeason());
		assertEquals(false, actualizado.isParticlesEnabled());
		assertEquals(Idioma.EN, actualizado.getLanguage());
		assertEquals(true, actualizado.isDefaultGridView());

		// Se vuelve a leer desde el servicio, no se mira el objeto devuelto: es lo
		// que de verdad importa, que quedó guardado y no solo en memoria.
		User releido = userService.loginFromId(user.getId());
		assertEquals(EstacionPreferida.INVIERNO, releido.getDefaultSeason());
		assertEquals(false, releido.isParticlesEnabled());
		assertEquals(Idioma.EN, releido.getLanguage());
		assertEquals(true, releido.isDefaultGridView());
	}

	@Test
	public void testUpdatePreferencesSinEstacionGuardada()
			throws DuplicateInstanceException, InstanceNotFoundException {

		User user = createUser("Sampi");
		userService.signUp(user);

		// defaultSeason nulo es un valor válido y distinto de "no lo toques": significa
		// "sin preferencia guardada, usa la estación real de hoy".
		User actualizado = userService.updatePreferences(user.getId(), null, true, Idioma.ES, false);

		assertEquals(null, actualizado.getDefaultSeason());
	}

	@Test
	public void testUpdatePreferencesUserNotFound() {

		assertThrows(InstanceNotFoundException.class, () -> userService.updatePreferences(Long.valueOf(9999),
				EstacionPreferida.VERANO, true, Idioma.ES, false));
	}

	@Test
	public void testCompleteOnboarding() throws DuplicateInstanceException, InstanceNotFoundException {

		User user = createUser("Sampi");
		userService.signUp(user);

		// Antes de completarla, cualquier cuenta nueva empieza sin haberla visto.
		assertEquals(false, user.isOnboardingSeen());

		User actualizado = userService.completeOnboarding(user.getId());
		assertEquals(true, actualizado.isOnboardingSeen());

		// Releído desde el servicio y no desde el objeto en memoria: lo que importa
		// es que quedó guardado, no solo devuelto.
		User releido = userService.loginFromId(user.getId());
		assertEquals(true, releido.isOnboardingSeen());
	}

	@Test
	public void testCompleteOnboardingUserNotFound() {

		assertThrows(InstanceNotFoundException.class, () -> userService.completeOnboarding(Long.valueOf(9999)));
	}
}