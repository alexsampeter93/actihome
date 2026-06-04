package fp.project.actihome.model.entities;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface ReservationDao extends PagingAndSortingRepository<Reservation, Long> {

	ArrayList<Reservation> findByCustomerIdOrderByReservationDateDesc(Long customerId);

	Optional<Reservation> findById(Long reservationId);
}
