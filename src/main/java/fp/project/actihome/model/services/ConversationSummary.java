package fp.project.actihome.model.services;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.Message;
import fp.project.actihome.model.entities.User;

/**
 * Una fila de la bandeja de entrada (F10): el alojamiento del que se habla,
 * con quién, el último mensaje y cuántos quedan sin leer.
 *
 * <p>
 * No es una entidad — no hay tabla "conversaciones", ver la nota de
 * {@link Message}—, es la forma en la que {@code MessageServiceImpl} agrupa
 * en memoria los mensajes de un usuario antes de devolverlos a la pantalla.
 */
public class ConversationSummary {

	private final Housing housing;
	private final User otherUser;
	private Message lastMessage;
	private int unreadCount;

	public ConversationSummary(Housing housing, User otherUser) {
		this.housing = housing;
		this.otherUser = otherUser;
	}

	public Housing getHousing() {
		return housing;
	}

	public User getOtherUser() {
		return otherUser;
	}

	public Message getLastMessage() {
		return lastMessage;
	}

	public void setLastMessage(Message lastMessage) {
		this.lastMessage = lastMessage;
	}

	public int getUnreadCount() {
		return unreadCount;
	}

	public void incrementarNoLeidos() {
		unreadCount++;
	}
}
