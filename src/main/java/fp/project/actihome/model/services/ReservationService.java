package fp.project.actihome.model.services;

import java.time.LocalDateTime;
import java.util.ArrayList;

import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.model.exceptions.AlreadyCancelledException;
import fp.project.actihome.model.exceptions.AlreadyCheckedInException;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
import fp.project.actihome.model.exceptions.CannotCancelException;
import fp.project.actihome.model.exceptions.CannotCheckInException;
import fp.project.actihome.model.exceptions.CheckOutMustBeOneDayAfterException;
import fp.project.actihome.model.exceptions.CodeDoesNotMatchException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.MustBeTodayOrAfterException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotMyReservationException;
import fp.project.actihome.model.exceptions.WrongCreditCardNumberException;

public interface ReservationService {

	Reservation reserveHousing(Long customerId, Long housingId, String creditCardNumber, LocalDateTime checkInDate,
			LocalDateTime checkOutDate) throws WrongCreditCardNumberException, MustBeTodayOrAfterException,
			CheckOutMustBeOneDayAfterException, InstanceNotFoundException, AlreadyReservedException, NotAuthorizedUserException;

	ArrayList<Reservation> showMyReservations(Long customerId) throws InstanceNotFoundException;

	Reservation doCheckIn(Long customerId, Long reservationId, Long reservationCode)
			throws CodeDoesNotMatchException, InstanceNotFoundException, NotMyReservationException, CannotCheckInException,
			AlreadyCheckedInException;

	/**
	 * Todas las reservas de un alojamiento, pasadas y futuras.
	 *
	 * <p>
	 * Para pintar el calendario de disponibilidad al reservar: qué días están ya
	 * ocupados. No hace falta que quien pregunta sea el propietario ni esté
	 * autenticado como nada en particular —es la misma información que ya se ve,
	 * agregada, en la etiqueta "Reservada" del catálogo—, así que no lleva
	 * comprobación de permisos.
	 */
	ArrayList<Reservation> showHousingReservations(Long housingId);

	/**
	 * Cancela una reserva del cliente.
	 *
	 * <p>
	 * No se borra la fila: se marca {@code cancelled}, igual que un check-in se
	 * marca en vez de crear una reserva nueva. Así "Mis reservas" sigue pudiendo
	 * enseñar el historial completo, cancelaciones incluidas, y las dos consultas
	 * de disponibilidad de {@link fp.project.actihome.model.entities.ReservationDao}
	 * dejan de contarla.
	 */
	Reservation cancelReservation(Long customerId, Long reservationId) throws InstanceNotFoundException,
			NotMyReservationException, AlreadyCancelledException, CannotCancelException;

}