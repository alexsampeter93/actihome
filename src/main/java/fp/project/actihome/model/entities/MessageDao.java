package fp.project.actihome.model.entities;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageDao extends JpaRepository<Message, Long> {

	Optional<Message> findById(Long messageId);

	/**
	 * Todos los mensajes en los que el usuario participa, como emisor o como
	 * destinatario, del más antiguo al más nuevo.
	 *
	 * <p>
	 * No hay una consulta separada "dame mi lista de conversaciones": eso es
	 * agregación, y se calcula agrupando esta misma lista en memoria
	 * ({@code MessageServiceImpl.showConversations}), igual que ya hace el resto
	 * de la aplicación con datos derivados en vez de guardarlos aparte.
	 */
	ArrayList<Message> findBySenderIdOrRecipientIdOrderBySentDateAsc(Long senderId, Long recipientId);

}
