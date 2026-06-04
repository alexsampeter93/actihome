package fp.project.actihome.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.User;
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
}