package fp.project.actihome.model.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.HousingDao;
import fp.project.actihome.model.entities.TradeProposal;
import fp.project.actihome.model.entities.TradeProposal.State;
import fp.project.actihome.model.entities.TradeProposalDao;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.exceptions.AlreadyProposedException;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
import fp.project.actihome.model.exceptions.CannotTradeWithSelfException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.NotTheOwnerException;
import fp.project.actihome.model.exceptions.ProposalNotPendingException;

@Service
@Transactional
public class TradeProposalServiceImpl implements TradeProposalService {

	@Autowired
	private TradeProposalDao tradeProposalDao;

	@Autowired
	private HousingDao housingDao;

	@Autowired
	private HousingService housingService;

	@Autowired
	private PermissionChecker permissionChecker;

	@Override
	public TradeProposal propose(Long proposerId, Long offeredHousingId, Long requestedHousingCode)
			throws InstanceNotFoundException, NotTheOwnerException, CannotTradeWithSelfException,
			AlreadyReservedException, AlreadyProposedException {

		User proposer = permissionChecker.checkUser(proposerId);

		Housing offered = housingDao.findById(offeredHousingId)
				.orElseThrow(() -> new InstanceNotFoundException("project.entities.housing", offeredHousingId));

		Housing requested = housingDao.findByHousingCode(requestedHousingCode)
				.orElseThrow(() -> new InstanceNotFoundException("project.entities.housing", requestedHousingCode));

		if (!offered.getOwner().getId().equals(proposer.getId())) {
			throw new NotTheOwnerException();
		}

		// Los dos casos van juntos porque para el usuario son el mismo error:
		// intentar cambiarse algo consigo mismo. Ofrecer una casa a cambio de otra
		// que también es tuya no permuta nada, solo gasta una propuesta.
		if (offered.getId().equals(requested.getId())
				|| requested.getOwner().getId().equals(proposer.getId())) {
			throw new CannotTradeWithSelfException();
		}

		comprobarQueNingunoEstaOcupado(offered, requested);

		if (tradeProposalDao.existsByOfferedIdAndRequestedIdAndState(offered.getId(), requested.getId(),
				State.PENDING)) {
			throw new AlreadyProposedException();
		}

		TradeProposal propuesta = new TradeProposal(proposer, offered, requested, LocalDateTime.now());

		// save() explícito y no dirty checking: la entidad es nueva, así que no hay
		// nada "sucio" que detectar — el dirty checking actualiza filas que ya
		// existen, no las crea.
		tradeProposalDao.save(propuesta);

		return propuesta;
	}

	@Override
	public void accept(Long userId, Long proposalId) throws InstanceNotFoundException, NotTheOwnerException,
			ProposalNotPendingException, AlreadyReservedException {

		User user = permissionChecker.checkUser(userId);
		TradeProposal propuesta = pendiente(proposalId);

		Housing offered = propuesta.getOffered();
		Housing requested = propuesta.getRequested();

		// Contesta el dueño ACTUAL de lo pedido, no el que lo fuera al proponerse.
		if (!requested.getOwner().getId().equals(user.getId())) {
			throw new NotTheOwnerException();
		}

		// **Se revalida todo, y no es paranoia.** Entre proponer y aceptar puede
		// haber pasado un día: alguien pudo reservar cualquiera de los dos, o el que
		// se ofrecía pudo cambiar de dueño al aceptarse otra propuesta antes que
		// esta. Comprobarlo solo al enviar habría dejado que se ejecutara un acuerdo
		// sobre un reparto que ya no existe.
		if (!offered.getOwner().getId().equals(propuesta.getProposer().getId())) {
			throw new NotTheOwnerException();
		}

		comprobarQueNingunoEstaOcupado(offered, requested);

		housingService.tradeHousings(propuesta.getProposer().getId(), offered.getId(), requested.getHousingCode());

		resolver(propuesta, State.ACCEPTED);

		// Las demás propuestas vivas que mencionen a cualquiera de los dos hablan de
		// un reparto que acaba de dejar de ser cierto. Se cierran como rechazadas en
		// lugar de dejarlas pendientes: una propuesta que ya no se puede cumplir y
		// sigue apareciendo en la bandeja es peor que ninguna.
		cerrarLasQueQuedanEnPie(offered, requested, propuesta);
	}

	@Override
	public void reject(Long userId, Long proposalId)
			throws InstanceNotFoundException, NotTheOwnerException, ProposalNotPendingException {

		User user = permissionChecker.checkUser(userId);
		TradeProposal propuesta = pendiente(proposalId);

		if (!propuesta.getRequested().getOwner().getId().equals(user.getId())) {
			throw new NotTheOwnerException();
		}

		resolver(propuesta, State.REJECTED);
	}

	@Override
	public void withdraw(Long userId, Long proposalId)
			throws InstanceNotFoundException, NotTheOwnerException, ProposalNotPendingException {

		User user = permissionChecker.checkUser(userId);
		TradeProposal propuesta = pendiente(proposalId);

		if (!propuesta.getProposer().getId().equals(user.getId())) {
			throw new NotTheOwnerException();
		}

		resolver(propuesta, State.WITHDRAWN);
	}

	@Override
	@Transactional(readOnly = true)
	public List<TradeProposal> showReceived(Long userId) throws InstanceNotFoundException {

		User user = permissionChecker.checkUser(userId);

		return tradeProposalDao.findByRequestedOwnerIdAndStateOrderByCreatedDateDesc(user.getId(), State.PENDING);
	}

	@Override
	@Transactional(readOnly = true)
	public List<TradeProposal> showSent(Long userId) throws InstanceNotFoundException {

		User user = permissionChecker.checkUser(userId);

		return tradeProposalDao.findByProposerIdAndStateOrderByCreatedDateDesc(user.getId(), State.PENDING);
	}

	// ------------------------------------------------------------------
	// Auxiliares
	// ------------------------------------------------------------------

	private TradeProposal pendiente(Long proposalId)
			throws InstanceNotFoundException, ProposalNotPendingException {

		Optional<TradeProposal> propuesta = tradeProposalDao.findById(proposalId);

		if (propuesta.isEmpty()) {
			throw new InstanceNotFoundException("project.entities.tradeProposal", proposalId);
		}

		if (!propuesta.get().isPending()) {
			throw new ProposalNotPendingException();
		}

		return propuesta.get();
	}

	/**
	 * Ningún alojamiento con gente dentro cambia de dueño.
	 *
	 * <p>
	 * Se apoya en {@code isAvailableNow} en vez de repetir la consulta: la regla
	 * —"solo bloquea una estancia <em>en curso</em>, no una reserva futura"— ya se
	 * decidió al cerrar el bug B5 y tenerla escrita en dos sitios sería tenerla
	 * escrita en uno y medio.
	 */
	private void comprobarQueNingunoEstaOcupado(Housing offered, Housing requested) throws AlreadyReservedException {

		if (!housingService.isAvailableNow(offered.getId()) || !housingService.isAvailableNow(requested.getId())) {
			throw new AlreadyReservedException();
		}
	}

	private void resolver(TradeProposal propuesta, State estado) {

		propuesta.setState(estado);
		propuesta.setResolvedDate(LocalDateTime.now());

		tradeProposalDao.save(propuesta);
	}

	private void cerrarLasQueQuedanEnPie(Housing offered, Housing requested, TradeProposal yaResuelta) {

		List<TradeProposal> afectadas = tradeProposalDao.findByStateAndHousingIn(State.PENDING,
				List.of(offered.getId(), requested.getId()));

		for (TradeProposal otra : afectadas) {

			if (!otra.getId().equals(yaResuelta.getId())) {
				resolver(otra, State.REJECTED);
			}
		}
	}
}
