package fp.project.actihome.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.model.entities.ReservationDao;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.AlreadyCheckedInException;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
import fp.project.actihome.model.exceptions.CannotCheckInException;
import fp.project.actihome.model.exceptions.CheckOutMustBeOneDayAfterException;
import fp.project.actihome.model.exceptions.CodeDoesNotMatchException;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.LessThanOneRoomException;
import fp.project.actihome.model.exceptions.MustBeTodayOrAfterException;
import fp.project.actihome.model.exceptions.NegativePrizeException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotMyReservationException;
import fp.project.actihome.model.exceptions.WrongCreditCardNumberException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class ReservationServiceTests {

	@Autowired
	private UserService userService;

	@Autowired
	private HousingService housingService;

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private ReservationDao reservationDao;

	private User signUpUser(String username, RoleType role) {

		User user = new User(username, "password", "name", "surname", "locality", 664567076, username + "@" + username,
				LocalDateTime.now(), role);

		try {
			userService.signUp(user);
		} catch (DuplicateInstanceException e) {
			throw new RuntimeException(e);
		}

		return user;
	}

	private Housing createHousing(Long housingCode, Long ownerId) throws DuplicateInstanceException,
			InstanceNotFoundException, LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		return housingService.uploadHousing(
				HousingData.basico(housingCode, "Casa en la playa", "Casa", 6, BigDecimal.valueOf(20.65),
						"Playa del Orzán").description("Descripción breve").breakfast(true),
				ownerId);
	}

	// ------------------------------------------------------------------
	// Fechas de prueba
	//
	// Estas fechas eran constantes escritas a mano (LocalDateTime.of(2026, 2, 9...)).
	// Funcionaron mientras esa fecha estuvo en el futuro y empezaron a fallar en
	// cuanto pasó: reserveHousing rechaza cualquier entrada anterior a hoy, así que
	// lanzaba MustBeTodayOrAfterException antes de llegar a la regla que el test
	// quería comprobar.
	//
	// La regla general: un test nunca debe depender del calendario. Si necesita una
	// fecha futura, la calcula desde hoy.
	//
	// Se usa LocalDate.now() y no LocalDateTime.now() a propósito: así la diferencia
	// entre entrada y salida es exactamente de cuatro días naturales, sin que influya
	// la hora a la que se ejecute el test.
	// ------------------------------------------------------------------

	/** Entrada válida: dentro de una semana. */
	private LocalDateTime entrada() {
		return LocalDate.now().plusDays(7).atTime(10, 30);
	}

	/** Salida válida: cuatro noches después de la entrada. */
	private LocalDateTime salida() {
		return LocalDate.now().plusDays(11).atTime(10, 30);
	}

	/** Entrada que ya pasó, para comprobar la regla de "hoy o después". */
	private LocalDateTime entradaEnElPasado() {
		return LocalDate.now().minusDays(1).atTime(10, 30);
	}

	@Test
	public void testReserveHousing()
			throws DuplicateInstanceException, InstanceNotFoundException, LessThanOneRoomException,
			NegativePrizeException, NotAuthorizedUserException, WrongCreditCardNumberException,
			MustBeTodayOrAfterException, CheckOutMustBeOneDayAfterException, AlreadyReservedException {

		User customer = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		Reservation reservation = reservationService.reserveHousing(customer.getId(), housing.getId(),
				"1234567890123456", entrada(), salida());

		Reservation myReservation = reservationDao.findById(reservation.getId()).get();

		BigDecimal totalPrice = housing.getPricePerNight().multiply(BigDecimal.valueOf(4));

		assertEquals(reservation.getCheckIn(), myReservation.getCheckIn());
		assertEquals(reservation.getCheckOut(), myReservation.getCheckOut());
		assertEquals(reservation.getPaymentMethod(), myReservation.getPaymentMethod());
		assertEquals(reservation.getReservationDate(), myReservation.getReservationDate());
		assertEquals(reservation.getReservationCode(), myReservation.getReservationCode());
		assertEquals(reservation.getTotalPrice(), totalPrice);
		assertTrue(!reservation.isCheckedIn());

	}

	@Test
	public void testReserveNonExistentHousing() {

		User customer = signUpUser("Author", RoleType.CUSTOMER);

		assertThrows(InstanceNotFoundException.class,
				() -> reservationService.reserveHousing(customer.getId(), Long.valueOf(125), "1234567890123456",
						entrada(), salida()));
	}

	@Test
	public void testReserveHousingCheckInBeforeToday() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User customer = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		assertThrows(MustBeTodayOrAfterException.class,
				() -> reservationService.reserveHousing(customer.getId(), housing.getId(), "1234567890123456",
						entradaEnElPasado(), salida()));
	}

	@Test
	public void testReserveHousingNotValidCheckOut() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User customer = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		assertThrows(CheckOutMustBeOneDayAfterException.class,
				() -> reservationService.reserveHousing(customer.getId(), housing.getId(), "1234567890123456",
						entrada(), entrada().plusHours(2)));
	}

	@Test
	public void testReserveHousingWrongCreditCard() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User customer = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		assertThrows(WrongCreditCardNumberException.class,
				() -> reservationService.reserveHousing(customer.getId(), housing.getId(), "123456789012346",
						entrada(), salida()));
	}

	@Test
	public void testReserveUnavailableHousing() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User customer = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());
		housing.setAvailable(false);

		assertThrows(AlreadyReservedException.class,
				() -> reservationService.reserveHousing(customer.getId(), housing.getId(), "1234567890123456",
						entrada(), salida()));
	}

	@Test
	public void testReserveHousingWithoutAuthorization() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());
		housing.setAvailable(false);

		assertThrows(NotAuthorizedUserException.class,
				() -> reservationService.reserveHousing(owner.getId(), housing.getId(), "1234567890123456",
						entrada(), salida()));
	}

	@Test
	public void testShowMyReservations()
			throws DuplicateInstanceException, InstanceNotFoundException, LessThanOneRoomException,
			NegativePrizeException, NotAuthorizedUserException, WrongCreditCardNumberException,
			MustBeTodayOrAfterException, CheckOutMustBeOneDayAfterException, AlreadyReservedException {

		User customer = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing1 = createHousing(Long.valueOf(50), owner.getId());
		Housing housing2 = createHousing(Long.valueOf(51), owner.getId());
		Housing housing3 = createHousing(Long.valueOf(52), owner.getId());
		Housing housing4 = createHousing(Long.valueOf(53), owner.getId());
		Housing housing5 = createHousing(Long.valueOf(54), owner.getId());

		Reservation reservation1 = reservationService.reserveHousing(customer.getId(), housing1.getId(),
				"1234567890123456", entrada(), salida());
		Reservation reservation2 = reservationService.reserveHousing(customer.getId(), housing2.getId(),
				"1234567890123456", entrada().plusMonths(1), salida().plusMonths(1));
		Reservation reservation3 = reservationService.reserveHousing(customer.getId(), housing3.getId(),
				"1234567890123456", entrada().plusMonths(2), salida().plusMonths(2));
		Reservation reservation4 = reservationService.reserveHousing(customer.getId(), housing4.getId(),
				"1234567890123456", entrada().plusMonths(3), salida().plusMonths(3));
		Reservation reservation5 = reservationService.reserveHousing(customer.getId(), housing5.getId(),
				"1234567890123456", entrada().plusMonths(4), salida().plusMonths(4));

		reservation1.setReservationDate(LocalDateTime.now().plusDays(1));
		reservation2.setReservationDate(LocalDateTime.now().plusDays(2));
		reservation3.setReservationDate(LocalDateTime.now().plusDays(3));
		reservation4.setReservationDate(LocalDateTime.now().plusDays(4));
		reservation5.setReservationDate(LocalDateTime.now().plusDays(5));

		ArrayList<Reservation> myReservations = reservationService.showMyReservations(customer.getId());

		assertEquals(myReservations.get(0), reservation5);
		assertEquals(myReservations.get(1), reservation4);
		assertEquals(myReservations.get(2), reservation3);
		assertEquals(myReservations.get(3), reservation2);
		assertEquals(myReservations.get(4), reservation1);
	}

	@Test
	public void testShowMyReservationsNonExistentCustomer() throws DuplicateInstanceException,
			InstanceNotFoundException, LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		assertThrows(InstanceNotFoundException.class,
				() -> reservationService.reserveHousing(Long.valueOf(321), housing.getId(), "1234567890123456",
						entrada(), salida()));
	}

	@Test
	public void testDoCheckIn() throws DuplicateInstanceException, InstanceNotFoundException, LessThanOneRoomException,
			NegativePrizeException, NotAuthorizedUserException, WrongCreditCardNumberException,
			MustBeTodayOrAfterException, CheckOutMustBeOneDayAfterException, AlreadyReservedException,
			CodeDoesNotMatchException, NotMyReservationException, CannotCheckInException, AlreadyCheckedInException {

		User customer = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		Reservation reservation = reservationService.reserveHousing(customer.getId(), housing.getId(),
				"1234567890123456", entrada(), salida());

		reservation.setCheckIn(LocalDateTime.now().minusHours(1));
		Reservation checkedInReservation = reservationService.doCkeckIn(customer.getId(), reservation.getId(),
				reservation.getReservationCode());

		assertTrue(checkedInReservation.isCheckedIn());
	}

	@Test
	public void testDoCheckInNotCheckInDate() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException,
			WrongCreditCardNumberException, MustBeTodayOrAfterException, CheckOutMustBeOneDayAfterException,
			AlreadyReservedException, CodeDoesNotMatchException {

		User customer = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		Reservation reservation = reservationService.reserveHousing(customer.getId(), housing.getId(),
				"1234567890123456", entrada(), salida());

		assertThrows(CannotCheckInException.class, () -> reservationService.doCkeckIn(customer.getId(),
				reservation.getId(), reservation.getReservationCode()));
	}

	@Test
	public void testDoCheckInAlreadyChecked() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException,
			WrongCreditCardNumberException, MustBeTodayOrAfterException, CheckOutMustBeOneDayAfterException,
			AlreadyReservedException, CodeDoesNotMatchException {

		User customer = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		Reservation reservation = reservationService.reserveHousing(customer.getId(), housing.getId(),
				"1234567890123456", entrada(), salida());
		reservation.setCheckedIn(true);
		reservation.setCheckIn(LocalDateTime.now().minusHours(1));

		assertThrows(AlreadyCheckedInException.class, () -> reservationService.doCkeckIn(customer.getId(),
				reservation.getId(), reservation.getReservationCode()));
	}

	@Test
	public void testDoCheckInWrongCode()
			throws DuplicateInstanceException, InstanceNotFoundException, LessThanOneRoomException,
			NegativePrizeException, NotAuthorizedUserException, WrongCreditCardNumberException,
			MustBeTodayOrAfterException, CheckOutMustBeOneDayAfterException, AlreadyReservedException {

		User customer = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		Reservation reservation = reservationService.reserveHousing(customer.getId(), housing.getId(),
				"1234567890123456", entrada(), salida());
		reservation.setCheckedIn(true);

		assertThrows(CodeDoesNotMatchException.class,
				() -> reservationService.doCkeckIn(customer.getId(), reservation.getId(), new Random().nextLong()));
	}

	@Test
	public void testDoCheckInNotMyReservation()
			throws DuplicateInstanceException, InstanceNotFoundException, LessThanOneRoomException,
			NegativePrizeException, NotAuthorizedUserException, WrongCreditCardNumberException,
			MustBeTodayOrAfterException, CheckOutMustBeOneDayAfterException, AlreadyReservedException {

		User customer1 = signUpUser("Customer1", RoleType.CUSTOMER);
		User customer2 = signUpUser("Customer2", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		Reservation reservation = reservationService.reserveHousing(customer1.getId(), housing.getId(),
				"1234567890123456", entrada(), salida());
		reservation.setCheckedIn(true);

		assertThrows(NotMyReservationException.class, () -> reservationService.doCkeckIn(customer2.getId(),
				reservation.getId(), reservation.getReservationCode()));
	}

	@Test
	public void testDoCheckInNonExistentReservation()
			throws DuplicateInstanceException, InstanceNotFoundException, LessThanOneRoomException,
			NegativePrizeException, NotAuthorizedUserException, WrongCreditCardNumberException,
			MustBeTodayOrAfterException, CheckOutMustBeOneDayAfterException, AlreadyReservedException {

		User customer = signUpUser("Customer2", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());
		Reservation reservation = reservationService.reserveHousing(customer.getId(), housing.getId(),
				"1234567890123456", entrada(), salida());
		reservation.setCheckedIn(true);

		assertThrows(InstanceNotFoundException.class, () -> reservationService.doCkeckIn(customer.getId(),
				Long.valueOf(285), reservation.getReservationCode()));

	}
}