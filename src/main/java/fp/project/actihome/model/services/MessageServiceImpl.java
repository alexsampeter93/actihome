package fp.project.actihome.model.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.HousingDao;
import fp.project.actihome.model.entities.Message;
import fp.project.actihome.model.entities.MessageDao;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.exceptions.CannotMessageSelfException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;

@Service
@Transactional
public class MessageServiceImpl implements MessageService {

	@Autowired
	private PermissionChecker permissionChecker;

	@Autowired
	private MessageDao messageDao;

	@Autowired
	private HousingDao housingDao;

	@Override
	public Message sendMessage(Long senderId, Long recipientId, Long housingId, String body)
			throws InstanceNotFoundException, CannotMessageSelfException {

		User sender = permissionChecker.checkUser(senderId);
		User recipient = permissionChecker.checkUser(recipientId);

		if (senderId.equals(recipientId)) {
			throw new CannotMessageSelfException();
		}

		Optional<Housing> housing = housingDao.findById(housingId);

		if (!housing.isPresent()) {
			throw new InstanceNotFoundException("project.entities.housing", housingId);
		}

		Message message = new Message(sender, recipient, housing.get(), body, LocalDateTime.now());

		return messageDao.save(message);
	}

	@Override
	public ArrayList<Message> showConversation(Long userId, Long otherUserId, Long housingId)
			throws InstanceNotFoundException {

		permissionChecker.checkUserExists(userId);

		ArrayList<Message> propios = messageDao.findBySenderIdOrRecipientIdOrderBySentDateAsc(userId, userId);
		ArrayList<Message> conversacion = new ArrayList<>();

		for (Message mensaje : propios) {

			boolean esDeEstaConversacion = mensaje.getHousing().getId().equals(housingId)
					&& (mensaje.getSender().getId().equals(otherUserId)
							|| mensaje.getRecipient().getId().equals(otherUserId));

			if (!esDeEstaConversacion) {
				continue;
			}

			// Abrir la conversación es leerla: los que llegaron dirigidos a userId y
			// seguían sin marca se marcan aquí mismo, no en un método aparte que alguien
			// tendría que acordarse de llamar.
			if (mensaje.getRecipient().getId().equals(userId) && !mensaje.isRead()) {
				mensaje.setReadDate(LocalDateTime.now());
			}

			conversacion.add(mensaje);
		}

		return conversacion;
	}

	@Override
	public ArrayList<ConversationSummary> showConversations(Long userId) throws InstanceNotFoundException {

		permissionChecker.checkUserExists(userId);

		ArrayList<Message> propios = messageDao.findBySenderIdOrRecipientIdOrderBySentDateAsc(userId, userId);

		// LinkedHashMap, no HashMap: con varias conversaciones sin leer nada aún,
		// conviene que el orden de entrada sea reproducible antes de ordenar por
		// fecha — mismo motivo que ya documenta PlatformPanelFrame.cargar().
		LinkedHashMap<String, ConversationSummary> agrupados = new LinkedHashMap<>();

		for (Message mensaje : propios) {

			User otro = mensaje.getSender().getId().equals(userId) ? mensaje.getRecipient() : mensaje.getSender();
			String clave = mensaje.getHousing().getId() + "-" + otro.getId();

			ConversationSummary resumen = agrupados.computeIfAbsent(clave,
					k -> new ConversationSummary(mensaje.getHousing(), otro));

			// Los mensajes llegan en orden ascendente de fecha, así que sobrescribir en
			// cada vuelta deja, al final del recorrido, el más reciente de verdad.
			resumen.setLastMessage(mensaje);

			if (mensaje.getRecipient().getId().equals(userId) && !mensaje.isRead()) {
				resumen.incrementarNoLeidos();
			}
		}

		ArrayList<ConversationSummary> resultado = new ArrayList<>(agrupados.values());
		resultado.sort(
				Comparator.comparing((ConversationSummary c) -> c.getLastMessage().getSentDate()).reversed());

		return resultado;
	}
}
