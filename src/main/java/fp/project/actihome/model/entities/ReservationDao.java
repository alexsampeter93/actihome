package fp.project.actihome.model.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ReservationDao extends PagingAndSortingRepository<Reservation, Long> {

	ArrayList<Reservation> findByCustomerIdOrderByReservationDateDesc(Long customerId);

	Optional<Reservation> findById(Long reservationId);

	/**
	 * Si el alojamiento tiene ya una reserva que pisa el rango pedido.
	 *
	 * <p>
	 * Condición clásica de solapamiento de intervalos: dos rangos se cruzan si uno
	 * empieza antes de que el otro termine, en los dos sentidos. La desigualdad es
	 * estricta a propósito —{@code checkOut > ?2}, no {@code >=}— porque una
	 * reserva que termina justo cuando otra empieza no se pisa con ella: la
	 * habitación se libera esa misma mañana.
	 */
	@Query("select case when count(r) > 0 then true else false end from Reservation r "
			+ "where r.housing.id = ?1 and r.checkIn < ?3 and r.checkOut > ?2")
	boolean existsOverlappingReservation(Long housingId, LocalDateTime desde, LocalDateTime hasta);

	/** Los alojamientos con una estancia en curso justo ahora. */
	@Query("select distinct r.housing.id from Reservation r where r.checkIn < ?1 and r.checkOut > ?1")
	List<Long> findHousingIdsWithActiveStay(LocalDateTime ahora);
}
