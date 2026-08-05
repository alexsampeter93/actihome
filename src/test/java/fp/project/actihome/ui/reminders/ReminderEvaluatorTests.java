package fp.project.actihome.ui.reminders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import fp.project.actihome.model.entities.Reservation;

/**
 * Lógica pura de F11, sin Spring y sin AWT: rápido de correr y no depende de
 * que el entorno tenga una bandeja de sistema de verdad.
 *
 * <p>
 * Las fechas son siempre relativas a {@code LocalDateTime.now()}, nunca fijas
 * (regla del proyecto, CLAUDE.md §2): un check-in "hace tres horas" no debe
 * caducar el día que alguien vuelva a ejecutar la suite.
 */
public class ReminderEvaluatorTests {

	/** Sin id: el constructor de nueve argumentos (reservationCode, no id) basta, porque el evaluador nunca lo toca. */
	private Reservation reserva(LocalDateTime checkIn, LocalDateTime checkOut, boolean checkedIn) {

		Reservation reserva = new Reservation(Long.valueOf(1), checkIn, checkOut, "Tarjeta de crédito",
				LocalDateTime.now().minusDays(1), BigDecimal.valueOf(100), checkedIn, null, null);
		reserva.setCancelled(false);

		return reserva;
	}

	@Test
	public void testCheckInProximoDentroDeLaVentana() {

		LocalDateTime ahora = LocalDateTime.now();
		Reservation reserva = reserva(ahora.plusHours(5), ahora.plusDays(3), false);

		assertTrue(ReminderEvaluator.esCheckInProximo(reserva, ahora));
	}

	@Test
	public void testCheckInProximoFueraDeLaVentana() {

		LocalDateTime ahora = LocalDateTime.now();
		Reservation reserva = reserva(ahora.plusDays(5), ahora.plusDays(8), false);

		assertFalse(ReminderEvaluator.esCheckInProximo(reserva, ahora));
	}

	@Test
	public void testCheckInProximoYaEmpezadaNoAvisa() {

		LocalDateTime ahora = LocalDateTime.now();
		Reservation reserva = reserva(ahora.minusHours(2), ahora.plusDays(2), false);

		assertFalse(ReminderEvaluator.esCheckInProximo(reserva, ahora));
	}

	@Test
	public void testCheckInProximoCanceladaNoAvisa() {

		LocalDateTime ahora = LocalDateTime.now();
		Reservation reserva = reserva(ahora.plusHours(5), ahora.plusDays(3), false);
		reserva.setCancelled(true);

		assertFalse(ReminderEvaluator.esCheckInProximo(reserva, ahora));
	}

	@Test
	public void testListaParaCheckIn() {

		LocalDateTime ahora = LocalDateTime.now();
		Reservation reserva = reserva(ahora.minusHours(3), ahora.plusDays(2), false);

		assertTrue(ReminderEvaluator.esListaParaCheckIn(reserva, ahora));
	}

	@Test
	public void testListaParaCheckInYaHechoNoAvisa() {

		LocalDateTime ahora = LocalDateTime.now();
		Reservation reserva = reserva(ahora.minusHours(3), ahora.plusDays(2), true);

		assertFalse(ReminderEvaluator.esListaParaCheckIn(reserva, ahora));
	}

	@Test
	public void testListaParaCheckInEstanciaYaTerminadaNoAvisa() {

		LocalDateTime ahora = LocalDateTime.now();
		Reservation reserva = reserva(ahora.minusDays(5), ahora.minusDays(1), false);

		assertFalse(ReminderEvaluator.esListaParaCheckIn(reserva, ahora));
	}

	@Test
	public void testListaParaCheckInCanceladaNoAvisa() {

		LocalDateTime ahora = LocalDateTime.now();
		Reservation reserva = reserva(ahora.minusHours(3), ahora.plusDays(2), false);
		reserva.setCancelled(true);

		assertFalse(ReminderEvaluator.esListaParaCheckIn(reserva, ahora));
	}
}
