package fp.project.actihome.model.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.HousingDao;
import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.model.entities.ReservationDao;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.AlreadyCancelledException;
import fp.project.actihome.model.exceptions.AlreadyCheckedInException;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
import fp.project.actihome.model.exceptions.CannotCancelException;
import fp.project.actihome.model.exceptions.CannotCheckInException;
import fp.project.actihome.model.exceptions.CapacityExceededException;
import fp.project.actihome.model.exceptions.CheckOutMustBeOneDayAfterException;
import fp.project.actihome.model.exceptions.CodeDoesNotMatchException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.MustBeTodayOrAfterException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotMyReservationException;
import fp.project.actihome.model.exceptions.WrongCreditCardNumberException;

@Service
@Transactional
public class ReservationServiceImpl implements ReservationService {

	@Autowired
	private PermissionChecker permissionChecker;

	@Autowired
	private HousingDao housingDao;

	@Autowired
	private ReservationDao reservationDao;

	@Override
	public Reservation reserveHousing(Long customerId, Long housingId, String creditCardNumber,
			LocalDateTime checkInDate, LocalDateTime checkOutDate, int numberOfAdults, int numberOfChildren)
			throws WrongCreditCardNumberException, MustBeTodayOrAfterException, CheckOutMustBeOneDayAfterException,
			InstanceNotFoundException, AlreadyReservedException, NotAuthorizedUserException, CapacityExceededException {

		User customer = permissionChecker.checkUser(customerId);
		Optional<Housing> housing = housingDao.findById(housingId);
		long reservationCode = new Random().nextInt(1000);
		long nights = 0;
		BigDecimal totalPrice = BigDecimal.valueOf(0);
		LocalDateTime reservationDate = LocalDateTime.now();

		if (customer.getRole() != RoleType.CUSTOMER) {
			throw new NotAuthorizedUserException();
		}

		if (!housing.isPresent()) {
			throw new InstanceNotFoundException("project.housing.entities", housingId);
		}

		// Los bebés no cuentan aquí (no se piden ni se guardan): en ningún sistema
		// de reservas real ocupan una plaza del aforo.
		if (numberOfAdults + numberOfChildren > housing.get().getCapacity()) {
			throw new CapacityExceededException();
		}

		// F13: no basta con contar caracteres. Antes "123456789012345A" pasaba la
		// comprobación porque medía 16 de largo, aunque no fuera un número de tarjeta
		// de verdad.
		if (!creditCardNumber.matches("\\d{16}")) {
			throw new WrongCreditCardNumberException();
		}

		if (checkInDate.isBefore(reservationDate)) {
			throw new MustBeTodayOrAfterException();
		}

		if (checkOutDate.isBefore(checkInDate.plusDays(1))) {
			throw new CheckOutMustBeOneDayAfterException();
		}

		// La disponibilidad se comprueba aquí, no antes: un solapamiento de fechas
		// solo tiene sentido sobre un intervalo ya válido (checkIn/checkOut en el
		// orden correcto), así que este chequeo va después de los dos de arriba.
		if (reservationDao.existsOverlappingReservation(housingId, checkInDate, checkOutDate)) {
			throw new AlreadyReservedException();
		}

		nights = ChronoUnit.DAYS.between(checkInDate.toLocalDate(), checkOutDate.toLocalDate());
		totalPrice = housing.get().getPricePerNight().multiply(BigDecimal.valueOf(nights));
		Reservation reservation = new Reservation(reservationCode, checkInDate, checkOutDate, numberOfAdults,
				numberOfChildren, "Tarjeta de crédito", reservationDate, totalPrice, false, customer, housing.get());
		return reservationDao.save(reservation);
	}

	@Override
	public ArrayList<Reservation> showMyReservations(Long customerId) throws InstanceNotFoundException {

		permissionChecker.checkUserExists(customerId);

		ArrayList<Reservation> reservations = reservationDao.findByCustomerIdOrderByReservationDateDesc(customerId);
		return reservations;
	}

	@Override
	public Reservation doCheckIn(Long customerId, Long reservationId, Long reservationCode)
			throws CodeDoesNotMatchException, InstanceNotFoundException, NotMyReservationException,
			CannotCheckInException, AlreadyCheckedInException {

		User customer = permissionChecker.checkUser(customerId);
		Optional<Reservation> reservation = reservationDao.findById(reservationId);

		if (!reservation.isPresent()) {
			throw new InstanceNotFoundException("project.entities.reservation", reservationId);
		}

		if (!reservationCode.equals(reservation.get().getReservationCode())) {
			throw new CodeDoesNotMatchException();
		}

		if (!reservation.get().getCustomer().equals(customer)) {
			throw new NotMyReservationException();
		}

		if (LocalDateTime.now().isBefore(reservation.get().getCheckIn())) {
			throw new CannotCheckInException();
		}

		if (reservation.get().isCheckedIn()) {
			throw new AlreadyCheckedInException();
		}

		reservation.get().setCheckedIn(true);
		return reservation.get();
	}

	@Override
	public ArrayList<Reservation> showHousingReservations(Long housingId) {
		return reservationDao.findByHousingId(housingId);
	}

	@Override
	public java.util.Set<Long> showUnavailableHousingIds(LocalDateTime checkIn, LocalDateTime checkOut) {
		return new java.util.HashSet<>(reservationDao.findHousingIdsUnavailableBetween(checkIn, checkOut));
	}

	@Override
	public Reservation cancelReservation(Long customerId, Long reservationId) throws InstanceNotFoundException,
			NotMyReservationException, AlreadyCancelledException, CannotCancelException {

		User customer = permissionChecker.checkUser(customerId);
		Optional<Reservation> reservation = reservationDao.findById(reservationId);

		if (!reservation.isPresent()) {
			throw new InstanceNotFoundException("project.entities.reservation", reservationId);
		}

		if (!reservation.get().getCustomer().equals(customer)) {
			throw new NotMyReservationException();
		}

		if (reservation.get().isCancelled()) {
			throw new AlreadyCancelledException();
		}

		// Ni tras el check-in ni una vez empezada la estancia: en ese punto ya se ha
		// hecho uso de la reserva, o está a punto de empezar, y cancelarla no
		// deshace nada del lado del alojamiento.
		if (reservation.get().isCheckedIn() || !LocalDateTime.now().isBefore(reservation.get().getCheckIn())) {
			throw new CannotCancelException();
		}

		reservation.get().setCancelled(true);
		return reservation.get();
	}
}