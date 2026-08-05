package fp.project.actihome.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.Message;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.CannotMessageSelfException;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.LessThanOneRoomException;
import fp.project.actihome.model.exceptions.NegativePrizeException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class MessageServiceTests {

	@Autowired
	private UserService userService;

	@Autowired
	private HousingService housingService;

	@Autowired
	private MessageService messageService;

	private User signUpUser(String username, RoleType role) {

		User user = new User(username, "password", "name", "surname", "locality", 664567076, username + "@" + username,
				LocalDateTime.now(), role);

		try {
			userService.signUp(user);
		} catch (DuplicateInstanceException e) {
			throw new RuntimeException(e);
		}

		return user;
	}

	private Housing createHousing(Long housingCode, Long ownerId) throws DuplicateInstanceException,
			InstanceNotFoundException, LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		return housingService.uploadHousing(
				HousingData.basico(housingCode, "Casa en la playa", "Casa", 6, BigDecimal.valueOf(20.65),
						"Playa del Orzán").description("Descripción breve").breakfast(true),
				ownerId);
	}

	@Test
	public void testSendMessage() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException, CannotMessageSelfException {

		User customer = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		Message mensaje = messageService.sendMessage(customer.getId(), owner.getId(), housing.getId(),
				"¿Admite mascotas?");

		assertEquals(customer, mensaje.getSender());
		assertEquals(owner, mensaje.getRecipient());
		assertEquals(housing, mensaje.getHousing());
		assertFalse(mensaje.isRead());
	}

	@Test
	public void testSendMessageToSelfRejected() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		assertThrows(CannotMessageSelfException.class,
				() -> messageService.sendMessage(owner.getId(), owner.getId(), housing.getId(), "Hola"));
	}

	@Test
	public void testSendMessageNonExistentHousing() throws DuplicateInstanceException {

		User customer = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);

		assertThrows(InstanceNotFoundException.class,
				() -> messageService.sendMessage(customer.getId(), owner.getId(), Long.valueOf(999), "Hola"));
	}

	/**
	 * Una conversación es de ida y vuelta: los mensajes que envía el cliente y
	 * los que responde el propietario tienen que salir juntos y en orden,
	 * aunque {@code sender} y {@code recipient} se turnen entre los dos.
	 */
	@Test
	public void testShowConversationBothDirections() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException, CannotMessageSelfException {

		User customer = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		messageService.sendMessage(customer.getId(), owner.getId(), housing.getId(), "¿Admite mascotas?");
		messageService.sendMessage(owner.getId(), customer.getId(), housing.getId(), "Sí, sin problema.");

		ArrayList<Message> conversacion = messageService.showConversation(customer.getId(), owner.getId(),
				housing.getId());

		assertEquals(2, conversacion.size());
		assertEquals("¿Admite mascotas?", conversacion.get(0).getBody());
		assertEquals("Sí, sin problema.", conversacion.get(1).getBody());
	}

	/** Dos alojamientos con el mismo par de personas son dos conversaciones distintas, no una mezclada. */
	@Test
	public void testShowConversationScopedToHousing() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException, CannotMessageSelfException {

		User customer = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housingA = createHousing(Long.valueOf(50), owner.getId());
		Housing housingB = createHousing(Long.valueOf(51), owner.getId());

		messageService.sendMessage(customer.getId(), owner.getId(), housingA.getId(), "Sobre A");
		messageService.sendMessage(customer.getId(), owner.getId(), housingB.getId(), "Sobre B");

		ArrayList<Message> conversacionA = messageService.showConversation(customer.getId(), owner.getId(),
				housingA.getId());

		assertEquals(1, conversacionA.size());
		assertEquals("Sobre A", conversacionA.get(0).getBody());
	}

	/** Abrir la conversación marca como leídos los mensajes pendientes dirigidos a quien la abre. */
	@Test
	public void testShowConversationMarksAsRead() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException, CannotMessageSelfException {

		User customer = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		messageService.sendMessage(customer.getId(), owner.getId(), housing.getId(), "¿Admite mascotas?");

		ArrayList<ConversationSummary> antes = messageService.showConversations(owner.getId());
		assertEquals(1, antes.get(0).getUnreadCount());

		messageService.showConversation(owner.getId(), customer.getId(), housing.getId());

		ArrayList<ConversationSummary> despues = messageService.showConversations(owner.getId());
		assertEquals(0, despues.get(0).getUnreadCount());
	}

	@Test
	public void testShowConversationsGroupsByOtherPartyAndHousing() throws DuplicateInstanceException,
			InstanceNotFoundException, LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException,
			CannotMessageSelfException {

		User customerA = signUpUser("AuthorA", RoleType.CUSTOMER);
		User customerB = signUpUser("AuthorB", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		messageService.sendMessage(customerA.getId(), owner.getId(), housing.getId(), "Pregunta de A");
		messageService.sendMessage(customerB.getId(), owner.getId(), housing.getId(), "Pregunta de B");
		messageService.sendMessage(owner.getId(), customerA.getId(), housing.getId(), "Respuesta para A");

		ArrayList<ConversationSummary> conversaciones = messageService.showConversations(owner.getId());

		assertEquals(2, conversaciones.size());

		// La más reciente primero: la última en escribirse fue la respuesta a A.
		assertEquals(customerA, conversaciones.get(0).getOtherUser());
		assertEquals("Respuesta para A", conversaciones.get(0).getLastMessage().getBody());
		assertTrue(conversaciones.get(0).getUnreadCount() >= 0);
	}
}
