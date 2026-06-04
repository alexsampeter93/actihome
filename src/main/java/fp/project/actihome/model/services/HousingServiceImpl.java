package fp.project.actihome.model.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.HousingDao;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.LessThanOneRoomException;
import fp.project.actihome.model.exceptions.NegativePrizeException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotTheOwnerException;

@Service
@Transactional
public class HousingServiceImpl implements HousingService {

	@Autowired
	private HousingDao housingDao;

	@Autowired
	private PermissionChecker permissionChecker;

	@Override
	public Housing uploadHousing(Long housingCode, String type, int numberOfRooms, BigDecimal pricePerNight,
			String description, boolean breakfast, boolean lunch, boolean dinner, String location, Long ownerId)
			throws DuplicateInstanceException, InstanceNotFoundException, LessThanOneRoomException,
			NegativePrizeException, NotAuthorizedUserException {

		User owner = permissionChecker.checkUser(ownerId);

		if (owner.getRole() != RoleType.ADMIN) {
			throw new NotAuthorizedUserException();
		}

		if (housingDao.existsByHousingCode(housingCode)) {
			throw new DuplicateInstanceException("project.entities.housing", housingCode);
		}

		if (numberOfRooms < 1) {
			throw new LessThanOneRoomException();
		}

		if (pricePerNight.compareTo(BigDecimal.ZERO) < 0) {
			throw new NegativePrizeException();
		}

		Housing housing = new Housing(housingCode, type, numberOfRooms, pricePerNight, description, breakfast, lunch,
				dinner, true, location, owner);
		housingDao.save(housing);

		return housing;
	}

	@Override
	public Housing findHousing(Long housingId) throws InstanceNotFoundException {

		Optional<Housing> housing = housingDao.findById(housingId);

		if (!housing.isPresent()) {
			throw new InstanceNotFoundException("project.entities.housing", housingId);
		}

		return housing.get();
	}

	@Override
	public ArrayList<Housing> showHousings() {

		return housingDao.findAllBy();
	}

	@Override
	public Housing updateHousing(Long housingId, Long ownerId, int numberOfRooms, BigDecimal pricePerNight,
			String description, boolean breakfast, boolean lunch, boolean dinner) throws InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotTheOwnerException, NotAuthorizedUserException {

		Optional<Housing> housing = housingDao.findById(housingId);
		User owner = permissionChecker.checkUser(ownerId);

		if (owner.getRole() != RoleType.ADMIN) {
			throw new NotAuthorizedUserException();
		}

		if (!housing.isPresent()) {
			throw new InstanceNotFoundException("project.entities.housing", housingId);
		}

		if (housing.get().getOwner().getId() != ownerId) {
			throw new NotTheOwnerException();
		}

		if (numberOfRooms < 1) {
			throw new LessThanOneRoomException();
		}

		if (pricePerNight.compareTo(BigDecimal.ZERO) < 0) {
			throw new NegativePrizeException();
		}

		housing.get().setDescription(description);
		housing.get().setNumberOfRooms(numberOfRooms);
		housing.get().setPricePerNight(pricePerNight);
		housing.get().setBreakfast(breakfast);
		housing.get().setLunch(lunch);
		housing.get().setDinner(dinner);

		return housing.get();
	}

	@Override
	public ArrayList<Housing> filterHousingsByType(String type) {

		return housingDao.findHousingsByType(type);
	}

	@Override
	public ArrayList<Housing> filterHousingsByMinimumRooms(int minimum) {

		return housingDao.findHousingsByNumberOfRooms(minimum);
	}

	@Override
	public void tradeHousings(Long ownerId, Long ownersHousingId, Long housingToTradeCode)
			throws InstanceNotFoundException, AlreadyReservedException {

		User owner = permissionChecker.checkUser(ownerId);
		Optional<Housing> housing = housingDao.findById(ownersHousingId);
		Optional<Housing> housingToTrade = housingDao.findByHousingCode(housingToTradeCode);

		if (!housing.isPresent() || !housingToTrade.isPresent()) {
			throw new InstanceNotFoundException("project.entities.housing", ownersHousingId);
		}
		if (!housing.get().isAvailable() || !housingToTrade.get().isAvailable()) {
			throw new AlreadyReservedException();
		}

		housing.get().setOwner(housingToTrade.get().getOwner());
		housingToTrade.get().setOwner(owner);
	}

}