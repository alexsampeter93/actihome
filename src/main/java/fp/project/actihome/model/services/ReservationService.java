package fp.project.actihome.model.services;

import java.time.LocalDateTime;
import java.util.ArrayList;

import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.model.exceptions.AlreadyCancelledException;
import fp.project.actihome.model.exceptions.AlreadyCheckedInException;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
import fp.project.actihome.model.exceptions.CannotCancelException;
import fp.project.actihome.model.exceptions.CapacityExceededException;
import fp.project.actihome.model.exceptions.CannotCheckInException;
import fp.project.actihome.model.exceptions.CheckOutMustBeOneDayAfterException;
import fp.project.actihome.model.exceptions.CodeDoesNotMatchException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.MustBeTodayOrAfterException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotMyReservationException;
import fp.project.actihome.model.exceptions.WrongCreditCardNumberException;

public interface ReservationService {

	/**
	 * @param numberOfAdults   cuántos adultos viajan, al menos uno
	 * @param numberOfChildren cuántos niños viajan además, puede ser cero. Los
	 *                         bebés no se piden aquí: no cuentan para el aforo
	 *                         del alojamiento (ver {@link Reservation#getNumberOfChildren()})
	 */
	Reservation reserveHousing(Long customerId, Long housingId, String creditCardNumber, LocalDateTime checkInDate,
			LocalDateTime checkOutDate, int numberOfAdults, int numberOfChildren) throws WrongCreditCardNumberException,
			MustBeTodayOrAfterException, CheckOutMustBeOneDayAfterException, InstanceNotFoundException,
			AlreadyReservedException, NotAuthorizedUserException, CapacityExceededException;

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
	 * Los ids de los alojamientos que ya tienen una reserva activa que pisa el
	 * rango pedido (Fase 9), para el buscador de destino, fechas y huéspedes.
	 *
	 * <p>
	 * Sin comprobación de permisos, por el mismo motivo que
	 * {@link #showHousingReservations}: es información agregada de
	 * disponibilidad, no el detalle de ninguna reserva.
	 */
	java.util.Set<Long> showUnavailableHousingIds(LocalDateTime checkIn, LocalDateTime checkOut);

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