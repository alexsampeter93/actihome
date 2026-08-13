package fp.project.actihome.model.services;

import java.util.List;

import fp.project.actihome.model.entities.TradeProposal;
import fp.project.actihome.model.exceptions.AlreadyProposedException;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
import fp.project.actihome.model.exceptions.CannotTradeWithSelfException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.NotTheOwnerException;
import fp.project.actihome.model.exceptions.ProposalNotPendingException;

/**
 * El acuerdo de un intercambio: proponer, aceptar, rechazar y retirar.
 *
 * <p>
 * <b>Por qué es un servicio aparte y no tres métodos más en
 * {@link HousingService}.</b> Aquel gestiona alojamientos; esto gestiona
 * <em>negociaciones sobre</em> alojamientos, que tienen su propio ciclo de vida
 * —nacen pendientes y mueren de cuatro maneras distintas— y su propia tabla. La
 * permuta en sí sigue viviendo donde vivía ({@code HousingService.tradeHousings}),
 * porque es una operación sobre alojamientos: aquí solo se decide <b>cuándo</b>
 * está permitido ejecutarla.
 *
 * <p>
 * <b>Lo que cambia respecto a antes de la Fase 9.</b> El intercambio era
 * instantáneo y unilateral: quien escribía el código se llevaba la casa del
 * otro sin que este se enterara. Ahora hay dos pasos y dos personas, y la
 * titularidad no se mueve hasta que la segunda dice que sí.
 */
public interface TradeProposalService {

	/**
	 * Propone un intercambio y lo deja pendiente de respuesta.
	 *
	 * @param proposerId          quién propone; debe ser el dueño de
	 *                            {@code offeredHousingId}
	 * @param offeredHousingId    el alojamiento propio que se ofrece
	 * @param requestedHousingCode el <b>código público</b> del que se pide a
	 *                            cambio — es lo que el otro propietario enseña,
	 *                            y lo único que se puede teclear sin conocer la
	 *                            base de datos
	 *
	 * @throws NotTheOwnerException        si lo ofrecido no es de quien propone
	 * @throws CannotTradeWithSelfException si los dos alojamientos son suyos, o
	 *                                     son el mismo
	 * @throws AlreadyReservedException    si alguno tiene una estancia en curso
	 * @throws AlreadyProposedException    si ya hay una propuesta viva entre esos
	 *                                     dos alojamientos
	 */
	TradeProposal propose(Long proposerId, Long offeredHousingId, Long requestedHousingCode)
			throws InstanceNotFoundException, NotTheOwnerException, CannotTradeWithSelfException,
			AlreadyReservedException, AlreadyProposedException;

	/**
	 * Acepta una propuesta y permuta la titularidad en ese mismo instante.
	 *
	 * <p>
	 * Solo puede aceptarla el dueño <b>actual</b> del alojamiento pedido. Se
	 * vuelve a comprobar todo lo que se comprobó al proponer —la propiedad y las
	 * estancias en curso—, porque entre la propuesta y la respuesta puede haber
	 * pasado cualquier cosa: una reserva, otra permuta. Una validación hecha al
	 * enviar no dice nada de lo que es cierto al aceptar.
	 */
	void accept(Long userId, Long proposalId) throws InstanceNotFoundException, NotTheOwnerException,
			ProposalNotPendingException, AlreadyReservedException;

	/** La rechaza quien la recibió. */
	void reject(Long userId, Long proposalId)
			throws InstanceNotFoundException, NotTheOwnerException, ProposalNotPendingException;

	/** La retira quien la envió, antes de que le contesten. */
	void withdraw(Long userId, Long proposalId)
			throws InstanceNotFoundException, NotTheOwnerException, ProposalNotPendingException;

	/** Las propuestas pendientes que alguien ha recibido, de la más nueva a la más vieja. */
	List<TradeProposal> showReceived(Long userId) throws InstanceNotFoundException;

	/** Las propuestas pendientes que alguien ha enviado, de la más nueva a la más vieja. */
	List<TradeProposal> showSent(Long userId) throws InstanceNotFoundException;
}
