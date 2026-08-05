package fp.project.actihome.model.entities;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Un mensaje dentro de una conversación huésped ↔ propietario, siempre sobre
 * un alojamiento concreto (F10).
 *
 * <p>
 * <b>No hay una entidad "Conversación" aparte.</b> Una conversación es, por
 * definición, todos los mensajes que comparten el mismo alojamiento y el
 * mismo par de participantes — un dato derivado, no uno que haga falta
 * guardar. {@code MessageServiceImpl} lo calcula agrupando mensajes en
 * memoria, el mismo criterio que ya usa el resto de la aplicación para no
 * multiplicar tablas por cada agregación (ver {@code Housing.score}, que
 * tampoco guarda su propio histórico).
 *
 * <p>
 * <b>{@code readDate} en vez de un booleano.</b> Con un {@code boolean read}
 * bastaría para saber si ya se leyó, pero no cuándo — y "read" además es
 * palabra reservada en varios dialectos SQL. Un {@code LocalDateTime} nulo
 * significa "todavía sin leer" y no nulo guarda el instante exacto, sin
 * ambigüedad y sin arriesgar la portabilidad entre H2 y MySQL que pide
 * CLAUDE.md.
 */
@Entity
@Table(name = "MESSAGES")
public class Message {

	private Long id;

	private User sender;

	private User recipient;

	private Housing housing;

	private String body;

	private LocalDateTime sentDate;

	private LocalDateTime readDate;

	public Message() {
	}

	public Message(User sender, User recipient, Housing housing, String body, LocalDateTime sentDate) {

		this.sender = sender;
		this.recipient = recipient;
		this.housing = housing;
		this.body = body;
		this.sentDate = sentDate;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "senderId")
	public User getSender() {
		return sender;
	}

	public void setSender(User sender) {
		this.sender = sender;
	}

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "recipientId")
	public User getRecipient() {
		return recipient;
	}

	public void setRecipient(User recipient) {
		this.recipient = recipient;
	}

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "housingId")
	public Housing getHousing() {
		return housing;
	}

	public void setHousing(Housing housing) {
		this.housing = housing;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public LocalDateTime getSentDate() {
		return sentDate;
	}

	public void setSentDate(LocalDateTime sentDate) {
		this.sentDate = sentDate;
	}

	/** Cuándo lo leyó el destinatario, o {@code null} si todavía no lo ha hecho. */
	public LocalDateTime getReadDate() {
		return readDate;
	}

	public void setReadDate(LocalDateTime readDate) {
		this.readDate = readDate;
	}

	@Transient
	public boolean isRead() {
		return readDate != null;
	}

	@Override
	public String toString() {
		return "Message [id=" + id + ", sender=" + sender + ", recipient=" + recipient + ", housing=" + housing
				+ ", body=" + body + ", sentDate=" + sentDate + ", readDate=" + readDate + "]";
	}
}
