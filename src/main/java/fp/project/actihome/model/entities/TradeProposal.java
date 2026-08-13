package fp.project.actihome.model.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Una propuesta de intercambio: "te ofrezco mi alojamiento a cambio del tuyo".
 *
 * <p>
 * <b>Por qué existe esta entidad, que es la parte importante.</b> Hasta la Fase
 * 9 el intercambio no era un intercambio: escribías el código del alojamiento
 * de otra persona, pulsabas "Confirmar" y la titularidad cambiaba en el acto.
 * <b>El otro propietario no se enteraba, no aceptaba nada y no podía impedirlo.</b>
 * Eso no es permutar, es apropiarse — y no era un descuido pequeño, porque
 * {@code tradeHousings} ni siquiera comprobaba que quien lo pedía fuese dueño
 * del alojamiento que ofrecía: cualquiera podía permutar dos casas ajenas entre
 * sí.
 *
 * <p>
 * <b>Un acuerdo entre dos partes necesita un estado que viva entre las dos.</b>
 * Mientras la operación fuera instantánea no había nada que guardar; en cuanto
 * hay un "te lo he propuesto y todavía no me has contestado", ese hecho es un
 * dato con vida propia: alguien tiene que poder verlo, aceptarlo, rechazarlo o
 * retirarlo, y tiene que sobrevivir a que las dos personas cierren la
 * aplicación. Es justo el criterio contrario al de {@code Message}, que no crea
 * una entidad "Conversación" porque una conversación <em>se deduce</em> de los
 * mensajes. Una propuesta pendiente no se deduce de nada.
 *
 * <p>
 * <b>Se guarda quién propuso, aunque parezca redundante</b> con
 * {@code offered.getOwner()}. Lo es hoy y deja de serlo en cuanto la propuesta
 * se acepta: a partir de ahí el dueño de lo ofrecido es la otra persona, y sin
 * este campo el historial diría que la propuesta la hizo quien la recibió. Un
 * dato derivado que sobrevive a su origen miente en silencio (CLAUDE.md §7).
 */
@Entity
@Table(name = "TRADE_PROPOSALS")
public class TradeProposal {

	/**
	 * En qué punto está la propuesta.
	 *
	 * <p>
	 * Se persiste como texto y no por ordinal, como {@code User.RoleType} y por el
	 * mismo motivo: insertar mañana un estado en medio del enum no puede convertir
	 * en silencio una propuesta rechazada en una aceptada.
	 */
	public enum State {

		/** Enviada y sin contestar. Es el único estado en el que se puede actuar. */
		PENDING,

		/** Aceptada por quien la recibió: en ese momento se permutaron los dueños. */
		ACCEPTED,

		/** Rechazada por quien la recibió. */
		REJECTED,

		/** Retirada por quien la envió, antes de que le contestaran. */
		WITHDRAWN
	}

	private Long id;

	private User proposer;

	private Housing offered;

	private Housing requested;

	private State state;

	private LocalDateTime createdDate;

	private LocalDateTime resolvedDate;

	public TradeProposal() {
	}

	public TradeProposal(User proposer, Housing offered, Housing requested, LocalDateTime createdDate) {

		this.proposer = proposer;
		this.offered = offered;
		this.requested = requested;
		this.state = State.PENDING;
		this.createdDate = createdDate;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/** Quién hizo la propuesta, tal y como era en el momento de hacerla. */
	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "proposerId")
	public User getProposer() {
		return proposer;
	}

	public void setProposer(User proposer) {
		this.proposer = proposer;
	}

	/** El alojamiento que se pone encima de la mesa. */
	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "offeredHousingId")
	public Housing getOffered() {
		return offered;
	}

	public void setOffered(Housing offered) {
		this.offered = offered;
	}

	/** El alojamiento que se pide a cambio. */
	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "requestedHousingId")
	public Housing getRequested() {
		return requested;
	}

	public void setRequested(Housing requested) {
		this.requested = requested;
	}

	@Enumerated(EnumType.STRING)
	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public LocalDateTime getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(LocalDateTime createdDate) {
		this.createdDate = createdDate;
	}

	/** Cuándo se contestó, o {@code null} mientras siga pendiente. */
	public LocalDateTime getResolvedDate() {
		return resolvedDate;
	}

	public void setResolvedDate(LocalDateTime resolvedDate) {
		this.resolvedDate = resolvedDate;
	}

	@Transient
	public boolean isPending() {
		return state == State.PENDING;
	}

	@Override
	public String toString() {
		return "TradeProposal [id=" + id + ", proposer=" + proposer + ", offered=" + offered + ", requested=" + requested
				+ ", state=" + state + ", createdDate=" + createdDate + ", resolvedDate=" + resolvedDate + "]";
	}
}
