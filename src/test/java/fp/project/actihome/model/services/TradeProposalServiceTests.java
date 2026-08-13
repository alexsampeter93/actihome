package fp.project.actihome.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.model.entities.TradeProposal;
import fp.project.actihome.model.entities.TradeProposal.State;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.AlreadyProposedException;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
import fp.project.actihome.model.exceptions.CannotTradeWithSelfException;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.NotTheOwnerException;
import fp.project.actihome.model.exceptions.ProposalNotPendingException;

/**
 * El acuerdo de un intercambio (Fase 9).
 *
 * <p>
 * <b>El test que da sentido a toda la clase es
 * {@link #testProposeDoesNotChangeOwnershipYet}.</b> Antes de esta fase, la
 * operación equivalente cambiaba la titularidad en el acto y sin preguntarle
 * nada al otro propietario; lo que estos tests fijan es justamente lo
 * contrario, que es una regla de negocio y no un detalle de pantalla: nadie
 * pierde su alojamiento sin decir que sí.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class TradeProposalServiceTests {

	@Autowired
	private UserService userService;

	@Autowired
	private HousingService housingService;

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private TradeProposalService tradeProposalService;

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
	 * Un código de alojamiento distinto en cada llamada.
	 *
	 * <p>
	 * Nada de números escritos a mano: {@code data.sql} siembra diez alojamientos
	 * y varios tests conviven en la misma base, así que un código fijo choca con
	 * el del test de al lado en cuanto alguien añade uno. El contador local hace
	 * imposible esa colisión sin que ningún test tenga que saber qué códigos usan
	 * los demás.
	 */
	private long siguienteCodigo = 90000;

	private Housing publicar(User owner) throws Exception {

		return housingService.uploadHousing(HousingData
				.basico(siguienteCodigo++, "Casa " + siguienteCodigo, "Casa", 4, BigDecimal.valueOf(50), "Ronda")
				.description("Descripcion breve"), owner.getId());
	}

	// ------------------------------------------------------------------
	// Proponer
	// ------------------------------------------------------------------

	/**
	 * <b>La regla central.</b> Proponer deja constancia y no mueve nada: los dos
	 * alojamientos siguen siendo de quien eran.
	 */
	@Test
	public void testProposeDoesNotChangeOwnershipYet() throws Exception {

		User ana = signUpUser("TradeAna", RoleType.ADMIN);
		User beto = signUpUser("TradeBeto", RoleType.ADMIN);

		Housing deAna = publicar(ana);
		Housing deBeto = publicar(beto);

		TradeProposal propuesta = tradeProposalService.propose(ana.getId(), deAna.getId(), deBeto.getHousingCode());

		assertEquals(State.PENDING, propuesta.getState());
		assertEquals(ana, deAna.getOwner());
		assertEquals(beto, deBeto.getOwner());
	}

	/** No se puede ofrecer lo que no es tuyo. */
	@Test
	public void testProposeWhatIsNotYours() throws Exception {

		User ana = signUpUser("TradeAna", RoleType.ADMIN);
		User beto = signUpUser("TradeBeto", RoleType.ADMIN);
		User carla = signUpUser("TradeCarla", RoleType.ADMIN);

		Housing deAna = publicar(ana);
		Housing deBeto = publicar(beto);

		assertThrows(NotTheOwnerException.class,
				() -> tradeProposalService.propose(carla.getId(), deAna.getId(), deBeto.getHousingCode()));
	}

	@Test
	public void testProposeToYourself() throws Exception {

		User ana = signUpUser("TradeAna", RoleType.ADMIN);

		Housing uno = publicar(ana);
		Housing otro = publicar(ana);

		assertThrows(CannotTradeWithSelfException.class,
				() -> tradeProposalService.propose(ana.getId(), uno.getId(), otro.getHousingCode()));
	}

	@Test
	public void testProposeTwiceForTheSamePair() throws Exception {

		User ana = signUpUser("TradeAna", RoleType.ADMIN);
		User beto = signUpUser("TradeBeto", RoleType.ADMIN);

		Housing deAna = publicar(ana);
		Housing deBeto = publicar(beto);

		tradeProposalService.propose(ana.getId(), deAna.getId(), deBeto.getHousingCode());

		assertThrows(AlreadyProposedException.class,
				() -> tradeProposalService.propose(ana.getId(), deAna.getId(), deBeto.getHousingCode()));
	}

	// ------------------------------------------------------------------
	// Aceptar
	// ------------------------------------------------------------------

	@Test
	public void testAcceptSwapsOwners() throws Exception {

		User ana = signUpUser("TradeAna", RoleType.ADMIN);
		User beto = signUpUser("TradeBeto", RoleType.ADMIN);

		Housing deAna = publicar(ana);
		Housing deBeto = publicar(beto);

		TradeProposal propuesta = tradeProposalService.propose(ana.getId(), deAna.getId(), deBeto.getHousingCode());

		tradeProposalService.accept(beto.getId(), propuesta.getId());

		assertEquals(beto, deAna.getOwner());
		assertEquals(ana, deBeto.getOwner());
		assertEquals(State.ACCEPTED, propuesta.getState());
	}

	/**
	 * <b>Solo acepta quien la recibe.</b> Es el mismo agujero de antes visto por
	 * la otra cara: si aceptar valiera para cualquiera, quien propone podría
	 * aceptarse a sí mismo y estaríamos donde estábamos.
	 */
	@Test
	public void testProposerCannotAcceptTheirOwnProposal() throws Exception {

		User ana = signUpUser("TradeAna", RoleType.ADMIN);
		User beto = signUpUser("TradeBeto", RoleType.ADMIN);

		Housing deAna = publicar(ana);
		Housing deBeto = publicar(beto);

		TradeProposal propuesta = tradeProposalService.propose(ana.getId(), deAna.getId(), deBeto.getHousingCode());

		assertThrows(NotTheOwnerException.class,
				() -> tradeProposalService.accept(ana.getId(), propuesta.getId()));

		assertEquals(ana, deAna.getOwner());
		assertEquals(beto, deBeto.getOwner());
	}

	@Test
	public void testStrangerCannotAccept() throws Exception {

		User ana = signUpUser("TradeAna", RoleType.ADMIN);
		User beto = signUpUser("TradeBeto", RoleType.ADMIN);
		User carla = signUpUser("TradeCarla", RoleType.ADMIN);

		Housing deAna = publicar(ana);
		Housing deBeto = publicar(beto);

		TradeProposal propuesta = tradeProposalService.propose(ana.getId(), deAna.getId(), deBeto.getHousingCode());

		assertThrows(NotTheOwnerException.class,
				() -> tradeProposalService.accept(carla.getId(), propuesta.getId()));
	}

	/**
	 * Una estancia en curso bloquea la aceptación aunque no bloqueara la
	 * propuesta: entre las dos cosas puede pasar cualquier cosa, y validar solo al
	 * enviar habría dejado ejecutar un acuerdo sobre un reparto ya caducado.
	 */
	@Test
	public void testAcceptBlockedByActiveStay() throws Exception {

		User ana = signUpUser("TradeAna", RoleType.ADMIN);
		User beto = signUpUser("TradeBeto", RoleType.ADMIN);
		User cliente = signUpUser("TradeCliente", RoleType.CUSTOMER);

		Housing deAna = publicar(ana);
		Housing deBeto = publicar(beto);

		TradeProposal propuesta = tradeProposalService.propose(ana.getId(), deAna.getId(), deBeto.getHousingCode());

		LocalDateTime entrada = LocalDate.now().plusDays(7).atTime(10, 30);
		Reservation reserva = reservationService.reserveHousing(cliente.getId(), deAna.getId(), "1234567890123456",
				entrada, entrada.plusDays(4), 1, 0);

		// El mismo truco que usan los tests de check-in: se mueve el check-in al
		// pasado para simular que la estancia ya ha empezado.
		reserva.setCheckIn(LocalDateTime.now().minusHours(1));

		assertThrows(AlreadyReservedException.class,
				() -> tradeProposalService.accept(beto.getId(), propuesta.getId()));

		assertEquals(ana, deAna.getOwner());
	}

	/**
	 * Aceptar una propuesta cierra las demás que hablaban de los mismos
	 * alojamientos: ya no describen un reparto posible.
	 */
	@Test
	public void testAcceptClosesTheOtherProposalsAboutTheSameHousings() throws Exception {

		User ana = signUpUser("TradeAna", RoleType.ADMIN);
		User beto = signUpUser("TradeBeto", RoleType.ADMIN);
		User carla = signUpUser("TradeCarla", RoleType.ADMIN);

		Housing deAna = publicar(ana);
		Housing deBeto = publicar(beto);
		Housing deCarla = publicar(carla);

		TradeProposal deAnaABeto = tradeProposalService.propose(ana.getId(), deAna.getId(), deBeto.getHousingCode());
		TradeProposal deCarlaABeto = tradeProposalService.propose(carla.getId(), deCarla.getId(),
				deBeto.getHousingCode());

		tradeProposalService.accept(beto.getId(), deAnaABeto.getId());

		assertEquals(State.REJECTED, deCarlaABeto.getState());
		assertTrue(tradeProposalService.showReceived(ana.getId()).isEmpty());
	}

	// ------------------------------------------------------------------
	// Rechazar, retirar y listar
	// ------------------------------------------------------------------

	@Test
	public void testRejectKeepsEverythingWhereItWas() throws Exception {

		User ana = signUpUser("TradeAna", RoleType.ADMIN);
		User beto = signUpUser("TradeBeto", RoleType.ADMIN);

		Housing deAna = publicar(ana);
		Housing deBeto = publicar(beto);

		TradeProposal propuesta = tradeProposalService.propose(ana.getId(), deAna.getId(), deBeto.getHousingCode());

		tradeProposalService.reject(beto.getId(), propuesta.getId());

		assertEquals(State.REJECTED, propuesta.getState());
		assertEquals(ana, deAna.getOwner());
		assertEquals(beto, deBeto.getOwner());
	}

	@Test
	public void testOnlyTheProposerCanWithdraw() throws Exception {

		User ana = signUpUser("TradeAna", RoleType.ADMIN);
		User beto = signUpUser("TradeBeto", RoleType.ADMIN);

		Housing deAna = publicar(ana);
		Housing deBeto = publicar(beto);

		TradeProposal propuesta = tradeProposalService.propose(ana.getId(), deAna.getId(), deBeto.getHousingCode());

		assertThrows(NotTheOwnerException.class,
				() -> tradeProposalService.withdraw(beto.getId(), propuesta.getId()));

		tradeProposalService.withdraw(ana.getId(), propuesta.getId());

		assertEquals(State.WITHDRAWN, propuesta.getState());
	}

	@Test
	public void testCannotAnswerTwice() throws Exception {

		User ana = signUpUser("TradeAna", RoleType.ADMIN);
		User beto = signUpUser("TradeBeto", RoleType.ADMIN);

		Housing deAna = publicar(ana);
		Housing deBeto = publicar(beto);

		TradeProposal propuesta = tradeProposalService.propose(ana.getId(), deAna.getId(), deBeto.getHousingCode());

		tradeProposalService.reject(beto.getId(), propuesta.getId());

		assertThrows(ProposalNotPendingException.class,
				() -> tradeProposalService.accept(beto.getId(), propuesta.getId()));
	}

	/**
	 * Cada uno ve su lado de la misma propuesta, y solo el suyo.
	 *
	 * <p>
	 * Se comprueba por identidad de la propuesta y no por el tamaño de la lista:
	 * el tamaño depende de lo que hayan sembrado otros tests y {@code data.sql},
	 * y esa es una de las trampas que ya rompió la suite entera una vez.
	 */
	@Test
	public void testEachSideSeesItsOwn() throws Exception {

		User ana = signUpUser("TradeAna", RoleType.ADMIN);
		User beto = signUpUser("TradeBeto", RoleType.ADMIN);

		Housing deAna = publicar(ana);
		Housing deBeto = publicar(beto);

		TradeProposal propuesta = tradeProposalService.propose(ana.getId(), deAna.getId(), deBeto.getHousingCode());

		List<TradeProposal> recibidasPorBeto = tradeProposalService.showReceived(beto.getId());
		List<TradeProposal> enviadasPorAna = tradeProposalService.showSent(ana.getId());

		assertTrue(recibidasPorBeto.contains(propuesta));
		assertTrue(enviadasPorAna.contains(propuesta));
		assertTrue(tradeProposalService.showReceived(ana.getId()).isEmpty());
		assertTrue(tradeProposalService.showSent(beto.getId()).isEmpty());
	}

	@Test
	public void testProposeForAHousingThatDoesNotExist() throws Exception {

		User ana = signUpUser("TradeAna", RoleType.ADMIN);
		Housing deAna = publicar(ana);

		assertThrows(InstanceNotFoundException.class,
				() -> tradeProposalService.propose(ana.getId(), deAna.getId(), 999999L));
	}
}
