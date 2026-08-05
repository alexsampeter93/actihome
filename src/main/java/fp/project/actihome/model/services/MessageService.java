package fp.project.actihome.model.services;

import java.util.ArrayList;

import fp.project.actihome.model.entities.Message;
import fp.project.actihome.model.exceptions.CannotMessageSelfException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;

/**
 * Mensajería interna huésped ↔ propietario, siempre sobre un alojamiento
 * concreto (F10): antes de reservar, para preguntar; después, para coordinar
 * el check-in.
 */
public interface MessageService {

	Message sendMessage(Long senderId, Long recipientId, Long housingId, String body)
			throws InstanceNotFoundException, CannotMessageSelfException;

	/**
	 * Los mensajes entre {@code userId} y {@code otherUserId} sobre un
	 * alojamiento, del más antiguo al más nuevo.
	 *
	 * <p>
	 * Abrir la conversación marca como leídos los mensajes pendientes que
	 * {@code userId} tuviera de {@code otherUserId} en ese alojamiento — es el
	 * efecto esperado de "abrir la bandeja de entrada", no una consulta neutra.
	 */
	ArrayList<Message> showConversation(Long userId, Long otherUserId, Long housingId) throws InstanceNotFoundException;

	/** Una fila por cada persona con la que {@code userId} tiene mensajes, con el más reciente primero. */
	ArrayList<ConversationSummary> showConversations(Long userId) throws InstanceNotFoundException;
}
