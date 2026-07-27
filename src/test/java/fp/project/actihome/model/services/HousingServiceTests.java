package fp.project.actihome.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.LessThanOneRoomException;
import fp.project.actihome.model.exceptions.NegativePrizeException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotTheOwnerException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class HousingServiceTests {

	@Autowired
	private UserService userService;

	@Autowired
	private HousingService housingService;

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
	 * Se queda solo con los alojamientos de un propietario.
	 *
	 * <p>
	 * Varios tests comprobaban posiciones absolutas de la lista devuelta
	 * ({@code assertEquals(lista.get(0), miPrimerAlojamiento)}), lo que da por hecho
	 * que la base de datos está vacía. No lo está: {@code data.sql} siembra cinco
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

		Housing housing = housingService.uploadHousing(Long.valueOf(24019), "Casa en la playa", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner.getId());

		assertEquals(housing.getHousingCode(), Long.valueOf(24019));
		assertEquals(housing.getType(), "Casa en la playa");
		assertEquals(housing.getNumberOfRooms(), 6);
		assertEquals(housing.getPricePerNight(), BigDecimal.valueOf(20.65));
		assertEquals(housing.getDescription(), "Descripción breve");
		assertTrue(housing.isBreakfast());
		assertTrue(!housing.isLunch());
		assertTrue(housing.isDinner());
		assertTrue(housing.isAvailable());
		assertEquals(housing.getLocation(), "Playa del Orzán");
		assertEquals(housing.getOwner(), owner);

	}

	@Test
	public void testUploadAlreadyUploadedHousing() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		housingService.uploadHousing(Long.valueOf(24019), "Casa en la playa", 6, BigDecimal.valueOf(20.65),
				"Descripción breve", true, false, true, "Playa del Orzán", owner.getId());

		assertThrows(DuplicateInstanceException.class,
				() -> housingService.uploadHousing(Long.valueOf(24019), "Casa en la playa", 6,
						BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán",
						owner.getId()));
	}

	@Test
	public void testUploadHousingWithZeroRooms() {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		assertThrows(LessThanOneRoomException.class,
				() -> housingService.uploadHousing(Long.valueOf(24019), "Casa en la playa", 0,
						BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán",
						owner.getId()));
	}

	@Test
	public void testUploadHousingWithNegativePrize() {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		assertThrows(NegativePrizeException.class,
				() -> housingService.uploadHousing(Long.valueOf(24019), "Casa en la playa", 2, BigDecimal.valueOf(-1),
						"Descripción breve", true, false, true, "Playa del Orzán", owner.getId()));
	}

	@Test
	public void testUploadHousingWithNoAuthorization() {

		User owner = signUpUser("Owner", RoleType.CUSTOMER);

		assertThrows(NotAuthorizedUserException.class,
				() -> housingService.uploadHousing(Long.valueOf(24019), "Casa en la playa", 7,
						BigDecimal.valueOf(20.60), "Descripción breve", true, false, true, "Playa del Orzán",
						owner.getId()));

	}

	@Test
	public void testFindHousing() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		Housing housing = housingService.uploadHousing(Long.valueOf(24019), "Casa en la playa", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner.getId());

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

		Housing housing = housingService.uploadHousing(Long.valueOf(24019), "Casa en la playa", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner.getId());

		Housing updatedHousing = housingService.updateHousing(housing.getId(), owner.getId(), 3,
				BigDecimal.valueOf(10.64), "Nueva descripción", true, false, true);

		assertEquals(updatedHousing.getNumberOfRooms(), 3);
		assertEquals(updatedHousing.getPricePerNight(), BigDecimal.valueOf(10.64));
		assertEquals(updatedHousing.getDescription(), "Nueva descripción");

	}

	@Test
	public void testUpdateAnotherHousing() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner1 = signUpUser("Owner1", RoleType.ADMIN);
		User owner2 = signUpUser("Owner2", RoleType.ADMIN);

		Housing housing = housingService.uploadHousing(Long.valueOf(24019), "Casa en la playa", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner1.getId());

		assertThrows(NotTheOwnerException.class, () -> housingService.updateHousing(housing.getId(), owner2.getId(), 3,
				BigDecimal.valueOf(10.64), "Nueva descripción", true, false, true));
	}

	@Test
	public void testUpdateHousingWithZeroRooms() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		Housing housing = housingService.uploadHousing(Long.valueOf(24019), "Casa en la playa", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner.getId());

		assertThrows(LessThanOneRoomException.class, () -> housingService.updateHousing(housing.getId(), owner.getId(),
				0, BigDecimal.valueOf(10.64), "Nueva descripción", true, false, true));
	}

	@Test
	public void testUpdateHousingWithNegativePrize() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		Housing housing = housingService.uploadHousing(Long.valueOf(24019), "Casa en la playa", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner.getId());

		assertThrows(NegativePrizeException.class, () -> housingService.updateHousing(housing.getId(), owner.getId(), 4,
				BigDecimal.valueOf(-1), "Nueva descripción", true, false, true));
	}

	@Test
	public void testUpdateNonExistentHousing() {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		assertThrows(InstanceNotFoundException.class, () -> housingService.updateHousing(Long.valueOf(603),
				owner.getId(), 4, BigDecimal.valueOf(1), "Nueva descripción", true, false, true));
	}

	@Test
	public void testUpdateHousingWithNoAuthorization() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);

		Housing housing = housingService.uploadHousing(Long.valueOf(24019), "Casa en la playa", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner.getId());

		owner.setRole(RoleType.CUSTOMER);

		assertThrows(NotAuthorizedUserException.class, () -> housingService.updateHousing(housing.getId(),
				owner.getId(), 4, BigDecimal.valueOf(1), "Nueva descripción", true, false, true));
	}

	@Test
	public void testShowHousings() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);
		ArrayList<Housing> housingsList = new ArrayList<Housing>();

		Housing housing1 = housingService.uploadHousing(Long.valueOf(4), "Casa en la playa", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner.getId());
		Housing housing2 = housingService.uploadHousing(Long.valueOf(7), "Casa en la playa", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner.getId());
		Housing housing3 = housingService.uploadHousing(Long.valueOf(29), "Casa en la playa", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner.getId());
		Housing housing4 = housingService.uploadHousing(Long.valueOf(243), "Casa en la playa", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner.getId());
		Housing housing5 = housingService.uploadHousing(Long.valueOf(21), "Casa en la playa", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner.getId());

		housingsList = housingService.showHousings();

		assertEquals(Arrays.asList(housing1, housing2, housing3, housing4, housing5), soloDe(housingsList, owner));
	}

	@Test
	public void testFilterHousingsByType() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);
		ArrayList<Housing> housingsList = new ArrayList<Housing>();

		Housing housing1 = housingService.uploadHousing(Long.valueOf(4), "Casa en la playa", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner.getId());
		Housing housing2 = housingService.uploadHousing(Long.valueOf(7), "Casa en la playa", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner.getId());
		housingService.uploadHousing(Long.valueOf(29), "Chalet", 6, BigDecimal.valueOf(20.65), "Descripción breve",
				true, false, true, "Playa del Orzán", owner.getId());
		Housing housing4 = housingService.uploadHousing(Long.valueOf(243), "Casa en la playa", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner.getId());
		Housing housing5 = housingService.uploadHousing(Long.valueOf(21), "Casa con piscina", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner.getId());

		housingsList = housingService.filterHousingsByType("Casa");

		// El "Chalet" queda fuera del filtro; los otros cuatro entran.
		assertEquals(Arrays.asList(housing1, housing2, housing4, housing5), soloDe(housingsList, owner));
	}

	@Test
	public void testFilterHousingsByMinimumRooms() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);
		ArrayList<Housing> housingsList = new ArrayList<Housing>();

		housingService.uploadHousing(Long.valueOf(4), "Casa en la playa", 2, BigDecimal.valueOf(20.65),
				"Descripción breve", true, false, true, "Playa del Orzán", owner.getId());
		housingService.uploadHousing(Long.valueOf(7), "Casa en la playa", 1, BigDecimal.valueOf(20.65),
				"Descripción breve", true, false, true, "Playa del Orzán", owner.getId());
		Housing housing3 = housingService.uploadHousing(Long.valueOf(29), "Chalet", 6, BigDecimal.valueOf(20.65),
				"Descripción breve", true, false, true, "Playa del Orzán", owner.getId());
		housingService.uploadHousing(Long.valueOf(243), "Casa en la playa", 5, BigDecimal.valueOf(20.65),
				"Descripción breve", true, false, true, "Playa del Orzán", owner.getId());
		Housing housing5 = housingService.uploadHousing(Long.valueOf(21), "Casa con piscina", 10,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner.getId());

		housingsList = housingService.filterHousingsByMinimumRooms(6);

		// Solo los de 6 y 10 habitaciones superan el mínimo.
		assertEquals(Arrays.asList(housing3, housing5), soloDe(housingsList, owner));
	}

	@Test
	public void testTradeHousings() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException, AlreadyReservedException {

		User owner1 = signUpUser("Owner1", RoleType.ADMIN);
		User owner2 = signUpUser("Owner2", RoleType.ADMIN);

		Housing housing1 = housingService.uploadHousing(Long.valueOf(24019), "Casa en la playa", 6,
				BigDecimal.valueOf(20.65), "Descripción breve", true, false, true, "Playa del Orzán", owner1.getId());

		Housing housing2 = housingService.uploadHousing(Long.valueOf(24030), "Casa en la playa 2", 6,
				BigDecimal.valueOf(20.65), "Descripción breve 2", true, false, true, "Playa del Orzán 2",
				owner2.getId());

		housingService.tradeHousings(owner1.getId(), housing1.getId(), housing2.getHousingCode());
		assertEquals(owner1, housing2.getOwner());
		assertEquals(owner2, housing1.getOwner());
	}
}