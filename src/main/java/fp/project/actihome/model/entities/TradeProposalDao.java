package fp.project.actihome.model.entities;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import fp.project.actihome.model.entities.TradeProposal.State;

public interface TradeProposalDao extends JpaRepository<TradeProposal, Long> {

	Optional<TradeProposal> findById(Long proposalId);

	/**
	 * Las propuestas que le han hecho a alguien, de la más nueva a la más vieja.
	 *
	 * <p>
	 * <b>Se pregunta por el dueño del alojamiento pedido, no por un
	 * "destinatario" guardado.</b> Es a propósito y es lo correcto: si mientras
	 * la propuesta estaba pendiente ese alojamiento cambió de manos —por otra
	 * permuta aceptada antes—, quien tiene que contestar es el dueño de ahora,
	 * no el de entonces. Guardar un destinatario habría sido un dato derivado
	 * capaz de sobrevivir a su origen, que es la trampa que este proyecto ya ha
	 * pisado varias veces.
	 */
	List<TradeProposal> findByRequestedOwnerIdAndStateOrderByCreatedDateDesc(Long ownerId, State state);

	/** Las que ha hecho alguien, de la más nueva a la más vieja. */
	List<TradeProposal> findByProposerIdAndStateOrderByCreatedDateDesc(Long proposerId, State state);

	/**
	 * Si ya hay una propuesta viva entre esos dos alojamientos concretos.
	 *
	 * <p>
	 * Evita que pulsar dos veces "Enviar propuesta" deje al otro con dos avisos
	 * idénticos que además, al aceptar el primero, dejarían el segundo apuntando
	 * a un alojamiento que ya no es de quien lo ofreció.
	 */
	boolean existsByOfferedIdAndRequestedIdAndState(Long offeredId, Long requestedId, State state);

	/**
	 * Todas las propuestas vivas en las que participa alguno de esos
	 * alojamientos, sea del lado que sea.
	 *
	 * <p>
	 * Para cerrarlas cuando una de ellas se acepta: en cuanto dos alojamientos
	 * cambian de dueño, cualquier otra propuesta que los mencione está hablando
	 * de un reparto que ya no existe. Dejarlas pendientes sería guardar promesas
	 * que nadie puede cumplir.
	 *
	 * <p>
	 * <b>Escrita como consulta y no derivada del nombre</b>, que es lo que hace
	 * el resto de este DAO. El nombre derivado que expresa esto mismo sería
	 * {@code findByStateAndOfferedIdInOrStateAndRequestedIdIn} — un método de
	 * cuatro parámetros donde dos son el mismo estado repetido, porque la sintaxis
	 * de nombres no sabe agrupar con paréntesis. Cuando el nombre cuesta más de
	 * leer que la consulta que representa, el nombre ha dejado de ser la
	 * abreviatura de nada.
	 */
	@Query("select p from TradeProposal p "
			+ "where p.state = ?1 and (p.offered.id in ?2 or p.requested.id in ?2)")
	List<TradeProposal> findByStateAndHousingIn(State state, Collection<Long> housingIds);

}
