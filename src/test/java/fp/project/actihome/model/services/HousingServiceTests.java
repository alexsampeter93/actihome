package fp.project.actihome.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.Amenity;
import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
import fp.project.actihome.model.exceptions.CheckOutMustBeOneDayAfterException;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.LessThanOneRoomException;
import fp.project.actihome.model.exceptions.MustBeTodayOrAfterException;
import fp.project.actihome.model.exceptions.NegativePrizeException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotTheOwnerException;
import fp.project.actihome.model.exceptions.WrongCreditCardNumberException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class HousingServiceTests {

	@Autowired
	private UserService userService;

	@Autowired
	private HousingService housingService;

	@Autowired
	private ReservationService reservationService;

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

	/**
	 * Un alojamiento de prueba con valores razonables.
	 *
	 * <p>
	 * Cada test parte de aquí y cambia solo lo que le interesa
	 * ({@code datos().numberOfRooms(0)}), en lugar de repetir diez argumentos. Antes
	 * la llamada al servicio ocupaba tres líneas en cada test y lo que cambiaba
	 * respecto al test de al lado quedaba escondido en medio.
	 */
	private HousingData datos() {

		return HousingData.basico(null, "Casa en la playa", "Casa", 6, BigDecimal.valueOf(20.65), "Playa del Orzán")
				.description("Descripción breve")
				.breakfast(true)
				.dinner(true);
	}

	private HousingData datos(long housingCode) {

		return datos().housingCode(housingCode);
	}

	/**
	 * Se queda solo con los alojamientos de un propietario.
	 *
	 * <p>
	 * Varios tests comprobaban posiciones absolutas de la lista devuelta
	 * ({@code assertEquals(lista.get(0), miPrimerAlojamiento)}), lo que da por hecho
	 * que la base de datos está vacía. No lo está: {@code data.sql} siembra varios
	 * alojamientos en cada arranque, así que esas posiciones eran de los sembrados.
	 *
	 * <p>
	 * Filtrar por propietario conserva lo que el test quería comprobar —el orden en
	 * que el servicio devuelve los alojamientos— sin depender de qué más haya en la
	 * tabla. Un test debe verificar su propio efecto, no el estado del mundo.
	 */
	private List<Housing> soloDe(List<Housing> housings, User owner) {

		return housings.stream()
				.filter(housing -> housing.getOwner().getId().equals(owner.getId()))
				.collect(Collectors.toList());
	}

	@Test
	public void testUploadHousing() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		Housing housing = housingService.uploadHousing(datos(24019), owner.getId());

		assertEquals(housing.getHousingCode(), Long.valueOf(24019));
		assertEquals(housing.getName(), "Casa en la playa");
		assertEquals(housing.getType(), "Casa");
		assertEquals(housing.getNumberOfRooms(), 6);
		assertEquals(housing.getPricePerNight(), BigDecimal.valueOf(20.65));
		assertEquals(housing.getDescription(), "Descripción breve");
		assertTrue(housing.isBreakfast());
		assertFalse(housing.isLunch());
		assertTrue(housing.isDinner());
		assertTrue(housingService.isAvailableNow(housing.getId()));
		assertEquals(housing.getLocation(), "Playa del Orzán");
		assertEquals(housing.getOwner(), owner);
	}

	/**
	 * Las comodidades del catálogo (Fase 3a) se guardan y se leen.
	 *
	 * <p>
	 * {@code amenities(...)} recibe las que están presentes y apaga todas las demás,
	 * así que este test comprueba las dos mitades: que las nombradas quedan a true y
	 * que las no nombradas quedan a false. Un setter que solo enciende deja basura de
	 * la edición anterior.
	 */
	@Test
	public void testUploadHousingWithAmenities() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		Housing housing = housingService
				.uploadHousing(datos(24019).amenities(Amenity.WIFI, Amenity.PARKING, Amenity.PETS), owner.getId());

		assertTrue(housing.isWifi());
		assertTrue(housing.isParking());
		assertTrue(housing.isPets());

		assertFalse(housing.isPool());
		assertFalse(housing.isTv());
		assertFalse(housing.isAirConditioning());

		assertEquals(Amenity.de(housing).size(), 4); // las tres anteriores más el desayuno
		assertTrue(Amenity.BREAKFAST.presenteEn(housing));
	}

	@Test
	public void testUploadAlreadyUploadedHousing() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		housingService.uploadHousing(datos(24019), owner.getId());

		assertThrows(DuplicateInstanceException.class,
				() -> housingService.uploadHousing(datos(24019), owner.getId()));
	}

	@Test
	public void testUploadHousingWithZeroRooms() {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		assertThrows(LessThanOneRoomException.class,
				() -> housingService.uploadHousing(datos(24019).numberOfRooms(0), owner.getId()));
	}

	@Test
	public void testUploadHousingWithNegativePrize() {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		assertThrows(NegativePrizeException.class,
				() -> housingService.uploadHousing(datos(24019).pricePerNight(BigDecimal.valueOf(-1)), owner.getId()));
	}

	@Test
	public void testUploadHousingWithNoAuthorization() {

		User owner = signUpUser("Owner", RoleType.CUSTOMER);

		assertThrows(NotAuthorizedUserException.class,
				() -> housingService.uploadHousing(datos(24019), owner.getId()));
	}

	@Test
	public void testFindHousing() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		Housing housing = housingService.uploadHousing(datos(24019), owner.getId());

		Housing registeredHousing = housingService.findHousing(housing.getId());

		assertEquals(housing, registeredHousing);
	}

	@Test
	public void testFindNonExistentHousing() {

		assertThrows(InstanceNotFoundException.class, () -> housingService.findHousing(Long.valueOf(205)));
	}

	@Test
	public void testUpdateHousing() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException, NotTheOwnerException {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		Housing housing = housingService.uploadHousing(datos(24019), owner.getId());

		Housing updatedHousing = housingService.updateHousing(housing.getId(), owner.getId(),
				datos().numberOfRooms(3).pricePerNight(BigDecimal.valueOf(10.64)).description("Nueva descripción"));

		assertEquals(updatedHousing.getNumberOfRooms(), 3);
		assertEquals(updatedHousing.getPricePerNight(), BigDecimal.valueOf(10.64));
		assertEquals(updatedHousing.getDescription(), "Nueva descripción");
	}

	/**
	 * La edición escribe exactamente lo que recibe. Regresión del bug B9.
	 *
	 * <p>
	 * El formulario de edición llamaba al servicio con {@code (..., true, true,
	 * true)} para la pensión, porque no mostraba esos campos y había que poner
	 * <i>algo</i> en esas tres posiciones. Corregir una errata en la descripción
	 * activaba de paso desayuno, comida y cena.
	 *
	 * <p>
	 * Este test fija la regla del servicio: si los datos dicen que no hay comidas, no
	 * hay comidas. El resto de la defensa está en la interfaz, que ahora precarga los
	 * valores reales antes de enviarlos.
	 */
	@Test
	public void testUpdateHousingWritesExactlyWhatItReceives()
			throws DuplicateInstanceException, InstanceNotFoundException, LessThanOneRoomException,
			NegativePrizeException, NotAuthorizedUserException, NotTheOwnerException {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		Housing housing = housingService.uploadHousing(datos(24019).amenities(Amenity.WIFI, Amenity.TV), owner.getId());

		assertTrue(housing.isBreakfast());
		assertTrue(housing.isWifi());

		Housing updated = housingService.updateHousing(housing.getId(), owner.getId(),
				datos().breakfast(false).dinner(false).amenities(Amenity.POOL));

		assertFalse(updated.isBreakfast());
		assertFalse(updated.isLunch());
		assertFalse(updated.isDinner());

		assertTrue(updated.isPool());
		assertFalse(updated.isWifi());
		assertFalse(updated.isTv());
	}

	@Test
	public void testUpdateAnotherHousing() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner1 = signUpUser("Owner1", RoleType.ADMIN);
		User owner2 = signUpUser("Owner2", RoleType.ADMIN);

		Housing housing = housingService.uploadHousing(datos(24019), owner1.getId());

		assertThrows(NotTheOwnerException.class,
				() -> housingService.updateHousing(housing.getId(), owner2.getId(), datos()));
	}

	@Test
	public void testUpdateHousingWithZeroRooms() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		Housing housing = housingService.uploadHousing(datos(24019), owner.getId());

		assertThrows(LessThanOneRoomException.class,
				() -> housingService.updateHousing(housing.getId(), owner.getId(), datos().numberOfRooms(0)));
	}

	@Test
	public void testUpdateHousingWithNegativePrize() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		Housing housing = housingService.uploadHousing(datos(24019), owner.getId());

		assertThrows(NegativePrizeException.class, () -> housingService.updateHousing(housing.getId(), owner.getId(),
				datos().pricePerNight(BigDecimal.valueOf(-1))));
	}

	@Test
	public void testUpdateNonExistentHousing() {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		assertThrows(InstanceNotFoundException.class,
				() -> housingService.updateHousing(Long.valueOf(603), owner.getId(), datos()));
	}

	@Test
	public void testUpdateHousingWithNoAuthorization() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		Housing housing = housingService.uploadHousing(datos(24019), owner.getId());

		owner.setRole(RoleType.CUSTOMER);

		assertThrows(NotAuthorizedUserException.class,
				() -> housingService.updateHousing(housing.getId(), owner.getId(), datos()));
	}

	@Test
	public void testShowHousings() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);
		ArrayList<Housing> housingsList = new ArrayList<Housing>();

		Housing housing1 = housingService.uploadHousing(datos(4), owner.getId());
		Housing housing2 = housingService.uploadHousing(datos(7), owner.getId());
		Housing housing3 = housingService.uploadHousing(datos(29), owner.getId());
		Housing housing4 = housingService.uploadHousing(datos(243), owner.getId());
		Housing housing5 = housingService.uploadHousing(datos(21), owner.getId());

		housingsList = housingService.showHousings();

		assertEquals(Arrays.asList(housing1, housing2, housing3, housing4, housing5), soloDe(housingsList, owner));
	}

	@Test
	public void testFilterHousingsByType() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);
		ArrayList<Housing> housingsList = new ArrayList<Housing>();

		Housing housing1 = housingService.uploadHousing(datos(4), owner.getId());
		Housing housing2 = housingService.uploadHousing(datos(7), owner.getId());
		housingService.uploadHousing(datos(29).type("Chalet"), owner.getId());
		Housing housing4 = housingService.uploadHousing(datos(243), owner.getId());
		Housing housing5 = housingService.uploadHousing(datos(21), owner.getId());

		housingsList = housingService.filterHousingsByType("Casa");

		// El "Chalet" queda fuera del filtro; los otros cuatro entran.
		assertEquals(Arrays.asList(housing1, housing2, housing4, housing5), soloDe(housingsList, owner));
	}

	@Test
	public void testFilterHousingsByMinimumRooms() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);
		ArrayList<Housing> housingsList = new ArrayList<Housing>();

		housingService.uploadHousing(datos(4).numberOfRooms(2), owner.getId());
		housingService.uploadHousing(datos(7).numberOfRooms(1), owner.getId());
		Housing housing3 = housingService.uploadHousing(datos(29).numberOfRooms(6), owner.getId());
		housingService.uploadHousing(datos(243).numberOfRooms(5), owner.getId());
		Housing housing5 = housingService.uploadHousing(datos(21).numberOfRooms(10), owner.getId());

		housingsList = housingService.filterHousingsByMinimumRooms(6);

		// Solo los de 6 y 10 habitaciones superan el mínimo.
		assertEquals(Arrays.asList(housing3, housing5), soloDe(housingsList, owner));
	}

	@Test
	public void testTradeHousings() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException, AlreadyReservedException {

		User owner1 = signUpUser("Owner1", RoleType.ADMIN);
		User owner2 = signUpUser("Owner2", RoleType.ADMIN);

		Housing housing1 = housingService.uploadHousing(datos(24019), owner1.getId());

		Housing housing2 = housingService.uploadHousing(
				datos(24030).name("Casa en la playa 2").description("Descripción breve 2").location("Playa del Orzán 2"),
				owner2.getId());

		housingService.tradeHousings(owner1.getId(), housing1.getId(), housing2.getHousingCode());
		assertEquals(owner1, housing2.getOwner());
		assertEquals(owner2, housing1.getOwner());
	}

	/**
	 * Demuestra que el bug B5 ya no existe: reservar un alojamiento para el mes
	 * que viene ya no lo bloquea para intercambio hoy mismo. Antes de la Fase
	 * 7.5, cualquier reserva futura ponía {@code available} a {@code false} para
	 * siempre.
	 */
	@Test
	public void testTradeAllowedForFutureReservation() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException, AlreadyReservedException,
			WrongCreditCardNumberException, MustBeTodayOrAfterException, CheckOutMustBeOneDayAfterException {

		User owner1 = signUpUser("Owner1", RoleType.ADMIN);
		User owner2 = signUpUser("Owner2", RoleType.ADMIN);
		User customer = signUpUser("TradeCustomer", RoleType.CUSTOMER);

		Housing housing1 = housingService.uploadHousing(datos(24019), owner1.getId());
		Housing housing2 = housingService.uploadHousing(
				datos(24030).name("Casa en la playa 2").description("Descripción breve 2").location("Playa del Orzán 2"),
				owner2.getId());

		LocalDateTime entrada = LocalDate.now().plusDays(7).atTime(10, 30);
		reservationService.reserveHousing(customer.getId(), housing1.getId(), "1234567890123456", entrada,
				entrada.plusDays(4));

		housingService.tradeHousings(owner1.getId(), housing1.getId(), housing2.getHousingCode());
		assertEquals(owner1, housing2.getOwner());
		assertEquals(owner2, housing1.getOwner());
	}

	/**
	 * Un alojamiento con una estancia en curso justo ahora no se puede
	 * intercambiar. Usa el mismo truco que {@code testDoCheckIn} para simular que
	 * la estancia ya ha empezado: mover {@code checkIn} al pasado tras crear la
	 * reserva con fechas válidas.
	 */
	@Test
	public void testTradeBlockedDuringActiveStay() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException, AlreadyReservedException,
			WrongCreditCardNumberException, MustBeTodayOrAfterException, CheckOutMustBeOneDayAfterException {

		User owner1 = signUpUser("Owner1", RoleType.ADMIN);
		User owner2 = signUpUser("Owner2", RoleType.ADMIN);
		User customer = signUpUser("TradeCustomer", RoleType.CUSTOMER);

		Housing housing1 = housingService.uploadHousing(datos(24019), owner1.getId());
		Housing housing2 = housingService.uploadHousing(
				datos(24030).name("Casa en la playa 2").description("Descripción breve 2").location("Playa del Orzán 2"),
				owner2.getId());

		LocalDateTime entrada = LocalDate.now().plusDays(7).atTime(10, 30);
		Reservation reservation = reservationService.reserveHousing(customer.getId(), housing1.getId(),
				"1234567890123456", entrada, entrada.plusDays(4));
		reservation.setCheckIn(LocalDateTime.now().minusHours(1));

		assertThrows(AlreadyReservedException.class,
				() -> housingService.tradeHousings(owner1.getId(), housing1.getId(), housing2.getHousingCode()));
	}
}
