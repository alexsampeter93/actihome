package fp.project.actihome.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import fp.project.actihome.model.entities.HousingPhoto;
import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
import fp.project.actihome.model.exceptions.CapacityExceededException;
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

	/**
	 * Las coordenadas se guardan y se pueden quitar (F17).
	 *
	 * <p>
	 * <b>La segunda mitad es la que importa.</b> Un alojamiento localizado tiene
	 * que poder dejar de estarlo: si el propietario cambia la ubicación de Granada
	 * a Bilbao, el formulario borra las coordenadas viejas y envía nulos, y si el
	 * servicio los ignorase "porque son nulos" el alojamiento diría Bilbao y su
	 * previsión sería la de Granada. Un dato derivado que sobrevive a su origen es
	 * peor que no tener el dato, y este test fija que no puede pasar.
	 */
	@Test
	public void testUpdateHousingGuardaYBorraLasCoordenadas()
			throws DuplicateInstanceException, InstanceNotFoundException, LessThanOneRoomException,
			NegativePrizeException, NotAuthorizedUserException, NotTheOwnerException {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		Housing housing = housingService.uploadHousing(datos(24019).coordenadas(37.0955, -3.3987), owner.getId());

		assertTrue(housing.estaLocalizado());
		assertEquals(37.0955, housing.getLatitude());
		assertEquals(-3.3987, housing.getLongitude());

		Housing sinLocalizar = housingService.updateHousing(housing.getId(), owner.getId(),
				datos().coordenadas(null, null));

		assertFalse(sinLocalizar.estaLocalizado());
	}

	/**
	 * Media coordenada no localiza nada, así que no se guarda ninguna.
	 *
	 * <p>
	 * {@code HousingData.coordenadas} recibe las dos juntas justamente para que no
	 * exista un objeto con latitud y sin longitud. Sin esta regla,
	 * {@code estaLocalizado()} diría que no —comprueba las dos— pero la base
	 * quedaría con una columna a medias, que es un dato que no significa nada y que
	 * alguien acabaría leyendo suelto algún día.
	 */
	@Test
	public void testUnaCoordenadaSolaNoSeGuarda() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		Housing housing = housingService.uploadHousing(datos(24019).coordenadas(37.0955, null), owner.getId());

		assertNull(housing.getLatitude());
		assertNull(housing.getLongitude());
		assertFalse(housing.estaLocalizado());
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
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException, AlreadyReservedException,
			NotTheOwnerException {

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
			WrongCreditCardNumberException, MustBeTodayOrAfterException, CheckOutMustBeOneDayAfterException,
			CapacityExceededException, NotTheOwnerException {

		User owner1 = signUpUser("Owner1", RoleType.ADMIN);
		User owner2 = signUpUser("Owner2", RoleType.ADMIN);
		User customer = signUpUser("TradeCustomer", RoleType.CUSTOMER);

		Housing housing1 = housingService.uploadHousing(datos(24019), owner1.getId());
		Housing housing2 = housingService.uploadHousing(
				datos(24030).name("Casa en la playa 2").description("Descripción breve 2").location("Playa del Orzán 2"),
				owner2.getId());

		LocalDateTime entrada = LocalDate.now().plusDays(7).atTime(10, 30);
		reservationService.reserveHousing(customer.getId(), housing1.getId(), "1234567890123456", entrada,
				entrada.plusDays(4), 1, 0);

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
			WrongCreditCardNumberException, MustBeTodayOrAfterException, CheckOutMustBeOneDayAfterException,
			CapacityExceededException {

		User owner1 = signUpUser("Owner1", RoleType.ADMIN);
		User owner2 = signUpUser("Owner2", RoleType.ADMIN);
		User customer = signUpUser("TradeCustomer", RoleType.CUSTOMER);

		Housing housing1 = housingService.uploadHousing(datos(24019), owner1.getId());
		Housing housing2 = housingService.uploadHousing(
				datos(24030).name("Casa en la playa 2").description("Descripción breve 2").location("Playa del Orzán 2"),
				owner2.getId());

		LocalDateTime entrada = LocalDate.now().plusDays(7).atTime(10, 30);
		Reservation reservation = reservationService.reserveHousing(customer.getId(), housing1.getId(),
				"1234567890123456", entrada, entrada.plusDays(4), 1, 0);
		reservation.setCheckIn(LocalDateTime.now().minusHours(1));

		assertThrows(AlreadyReservedException.class,
				() -> housingService.tradeHousings(owner1.getId(), housing1.getId(), housing2.getHousingCode()));
	}

	/**
	 * El tablón de intercambios abiertos enseña los de otros y <b>nunca los
	 * tuyos</b> (Fase 8.4).
	 *
	 * <p>
	 * Es la única regla de negocio que aporta el intercambio abierto, y merece un
	 * test porque el fallo sería silencioso: la lista se vería igual de llena, solo
	 * que ofreciéndote permutar contigo mismo. Se comprueban las dos direcciones
	 * —que el ajeno aparece y el propio no— porque una consulta que devolviera
	 * siempre la lista vacía pasaría la mitad de la comprobación.
	 */
	@Test
	public void testShowOpenExchangesExcludesYourOwn() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User propio = signUpUser("DuenoPropio", RoleType.ADMIN);
		User ajeno = signUpUser("DuenoAjeno", RoleType.ADMIN);

		Housing mio = housingService.uploadHousing(datos(31001).openToExchange(true).exchangeWanted("una cabaña"),
				propio.getId());
		Housing suyo = housingService.uploadHousing(datos(31002).name("Casa ajena").description("Descripción ajena")
				.location("Otra ciudad").openToExchange(true).exchangeWanted("un ático"), ajeno.getId());

		List<Housing> abiertos = housingService.showOpenExchanges(propio.getId());

		assertTrue(abiertos.stream().anyMatch(h -> h.getId().equals(suyo.getId())));
		assertFalse(abiertos.stream().anyMatch(h -> h.getId().equals(mio.getId())));
	}

	/**
	 * Quitar una foto del medio <b>recoloca</b> las siguientes (Fase 8.4).
	 *
	 * <p>
	 * Es la única parte de la galería con lógica de verdad, y la que fallaría en
	 * silencio: sin recolocar, la galería seguiría viéndose bien —el orden relativo
	 * no cambia— pero las posiciones quedarían 1, 3, 4, y cada borrado abriría otro
	 * hueco. No da problemas hasta que alguien escribe la función de reordenar, que
	 * es justo cuando ya nadie recuerda por qué hay huecos.
	 */
	@Test
	public void testRemovingAPhotoRenumbersTheRest() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException, NotTheOwnerException {

		User owner = signUpUser("DuenoGaleria", RoleType.ADMIN);
		Housing housing = housingService.uploadHousing(datos(32001), owner.getId());

		housingService.addHousingPhoto(housing.getId(), owner.getId(), "a.jpg");
		HousingPhoto segunda = housingService.addHousingPhoto(housing.getId(), owner.getId(), "b.jpg");
		housingService.addHousingPhoto(housing.getId(), owner.getId(), "c.jpg");

		housingService.removeHousingPhoto(segunda.getId(), owner.getId());

		List<HousingPhoto> quedan = housingService.showHousingPhotos(housing.getId());

		assertEquals(2, quedan.size());
		assertEquals("a.jpg", quedan.get(0).getImage());
		assertEquals(1, quedan.get(0).getPosition());
		assertEquals("c.jpg", quedan.get(1).getImage());
		assertEquals(2, quedan.get(1).getPosition());
	}

	/** Solo el propietario toca su galería. */
	@Test
	public void testOnlyTheOwnerCanAddPhotos() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("DuenoLegitimo", RoleType.ADMIN);
		User intruso = signUpUser("Intruso", RoleType.ADMIN);

		Housing housing = housingService.uploadHousing(datos(32002), owner.getId());

		assertThrows(NotTheOwnerException.class,
				() -> housingService.addHousingPhoto(housing.getId(), intruso.getId(), "robada.jpg"));
	}

	/** Un alojamiento que no se ofrece no sale en el tablón, aunque sea de otro. */
	@Test
	public void testShowOpenExchangesIgnoresClosedOnes() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User yo = signUpUser("Curioso", RoleType.ADMIN);
		User otro = signUpUser("Cerrado", RoleType.ADMIN);

		Housing cerrado = housingService.uploadHousing(datos(31003), otro.getId());

		assertFalse(housingService.showOpenExchanges(yo.getId()).stream()
				.anyMatch(h -> h.getId().equals(cerrado.getId())));
	}
}
