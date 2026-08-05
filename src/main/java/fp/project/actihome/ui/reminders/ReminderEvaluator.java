package fp.project.actihome.ui.reminders;

import java.time.Duration;
import java.time.LocalDateTime;

import fp.project.actihome.model.entities.Reservation;

/**
 * Qué reservas merecen un aviso ahora mismo (F11), sin ningún AWT de por
 * medio.
 *
 * <p>
 * Separado a propósito de {@link TrayReminders}: la decisión de "¿toca avisar
 * de esto?" es una regla de negocio pequeña y se puede probar con JUnit sin
 * abrir ninguna ventana ni depender de que el entorno tenga una bandeja de
 * sistema de verdad —{@link java.awt.SystemTray#isSupported()} no siempre da
 * {@code true}, ni siquiera en Windows, según el entorno—. Lo que sí sabe
 * pintar un aviso vive en {@code TrayReminders}; lo que decide cuándo, aquí.
 */
public final class ReminderEvaluator {

	/** A partir de cuánto antes del check-in se avisa de que se acerca. */
	public static final Duration VENTANA_CHECKIN_PROXIMO = Duration.ofHours(24);

	private ReminderEvaluator() {
	}

	/**
	 * La estancia empieza dentro de la ventana de aviso, pero todavía no ha
	 * empezado. Una reserva cancelada no avisa de nada: no va a ocurrir.
	 */
	public static boolean esCheckInProximo(Reservation reserva, LocalDateTime ahora) {

		if (reserva.isCancelled()) {
			return false;
		}

		LocalDateTime checkIn = reserva.getCheckIn();

		return checkIn.isAfter(ahora) && !checkIn.isAfter(ahora.plus(VENTANA_CHECKIN_PROXIMO));
	}

	/**
	 * Ya ha llegado la fecha de entrada, la estancia no ha terminado y el
	 * check-in todavía no se ha hecho: es el momento en que
	 * {@code DoCheckInFrame} tiene sentido.
	 */
	public static boolean esListaParaCheckIn(Reservation reserva, LocalDateTime ahora) {

		if (reserva.isCancelled() || reserva.isCheckedIn()) {
			return false;
		}

		return !reserva.getCheckIn().isAfter(ahora) && reserva.getCheckOut().isAfter(ahora);
	}
}
