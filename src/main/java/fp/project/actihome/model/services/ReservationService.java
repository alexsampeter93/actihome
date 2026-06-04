package fp.project.actihome.model.services;

import java.time.LocalDateTime;
import java.util.ArrayList;

import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.model.exceptions.AlreadyCheckedInException;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
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

	Reservation doCkeckIn(Long customerId, Long reservationId, Long reservationCode)
			throws CodeDoesNotMatchException, InstanceNotFoundException, NotMyReservationException, CannotCheckInException,
			AlreadyCheckedInException; 

}