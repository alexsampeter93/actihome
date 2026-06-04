package fp.project.actihome.model.services;

import java.math.BigDecimal;
import java.util.ArrayList;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.LessThanOneRoomException;
import fp.project.actihome.model.exceptions.NegativePrizeException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotTheOwnerException;

public interface HousingService {

	Housing uploadHousing(Long housingCode, String type, int numberOfRooms, BigDecimal pricePerNight,
			String description, boolean breakfast, boolean lunch, boolean dinner, String location, Long ownerId)
			throws DuplicateInstanceException, InstanceNotFoundException, LessThanOneRoomException,
			NegativePrizeException, NotAuthorizedUserException;

	Housing findHousing(Long housingId) throws InstanceNotFoundException;

	ArrayList<Housing> showHousings();

	Housing updateHousing(Long housingId, Long ownerId, int numberOfRooms, BigDecimal pricePerNight, String description,
			boolean breakfast, boolean lunch, boolean dinner) throws InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotTheOwnerException, NotAuthorizedUserException;

	ArrayList<Housing> filterHousingsByType(String type);

	ArrayList<Housing> filterHousingsByMinimumRooms(int minimum);

	void tradeHousings(Long ownerId, Long ownersHousingId, Long housingToTradeCode)
			throws InstanceNotFoundException, AlreadyReservedException;
}