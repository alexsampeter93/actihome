package fp.project.actihome.model.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.HousingDao;
import fp.project.actihome.model.entities.ReservationDao;
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
	private ReservationDao reservationDao;

	@Autowired
	private PermissionChecker permissionChecker;

	@Override
	public Housing uploadHousing(HousingData data, Long ownerId) throws DuplicateInstanceException,
			InstanceNotFoundException, LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = permissionChecker.checkUser(ownerId);

		if (owner.getRole() != RoleType.ADMIN) {
			throw new NotAuthorizedUserException();
		}

		if (housingDao.existsByHousingCode(data.getHousingCode())) {
			throw new DuplicateInstanceException("project.entities.housing", data.getHousingCode());
		}

		comprobarDatos(data);

		Housing housing = new Housing();
		housing.setHousingCode(data.getHousingCode());
		housing.setOwner(owner);
		copiarDatos(data, housing);

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
	public Housing updateHousing(Long housingId, Long ownerId, HousingData data) throws InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotTheOwnerException, NotAuthorizedUserException {

		Optional<Housing> housing = housingDao.findById(housingId);
		User owner = permissionChecker.checkUser(ownerId);

		if (owner.getRole() != RoleType.ADMIN) {
			throw new NotAuthorizedUserException();
		}

		if (!housing.isPresent()) {
			throw new InstanceNotFoundException("project.entities.housing", housingId);
		}

		// B3: antes comparaba con "!=", que en dos objetos Long compara identidad de
		// referencia y no valor. Funcionaba de milagro: por debajo de 128 Java reutiliza
		// las instancias de Long en una caché, así que con ids pequeños daba el
		// resultado correcto y con ids grandes habría rechazado al propietario legítimo.
		if (!housing.get().getOwner().getId().equals(ownerId)) {
			throw new NotTheOwnerException();
		}

		comprobarDatos(data);

		copiarDatos(data, housing.get());

		// No se llama a save: dentro de la transacción, Hibernate detecta por su cuenta
		// que la entidad cargada ha cambiado y escribe los cambios al confirmar. Es el
		// "dirty checking" en el que ya se apoyaba el método original.
		return housing.get();
	}

	/** Reglas de negocio comunes al alta y a la edición. */
	private void comprobarDatos(HousingData data) throws LessThanOneRoomException, NegativePrizeException {

		if (data.getNumberOfRooms() < 1) {
			throw new LessThanOneRoomException();
		}

		if (data.getPricePerNight().compareTo(BigDecimal.ZERO) < 0) {
			throw new NegativePrizeException();
		}
	}

	/**
	 * Vuelca los campos editables sobre la entidad.
	 *
	 * <p>
	 * Deliberadamente <b>no</b> copia el código del alojamiento, el propietario ni
	 * la puntuación: los tres los gobierna el propio servicio y no un formulario
	 * (la disponibilidad ya ni siquiera es un campo que copiar, desde la Fase
	 * 7.5). Tenerlo en un solo sitio evita la otra mitad del bug B9, que era que
	 * el alta y la edición escribían conjuntos de campos distintos.
	 */
	private void copiarDatos(HousingData data, Housing housing) {

		housing.setName(data.getName());
		housing.setType(data.getType());
		housing.setNumberOfRooms(data.getNumberOfRooms());
		housing.setPricePerNight(data.getPricePerNight());
		housing.setDescription(data.getDescription());
		housing.setLocation(data.getLocation());
		housing.setImage(data.getImage());

		housing.setBreakfast(data.isBreakfast());
		housing.setLunch(data.isLunch());
		housing.setDinner(data.isDinner());

		housing.setPool(data.isPool());
		housing.setWifi(data.isWifi());
		housing.setTv(data.isTv());
		housing.setParking(data.isParking());
		housing.setAirConditioning(data.isAirConditioning());
		housing.setPets(data.isPets());

		housing.setIdealSeason(data.getIdealSeason());
		housing.setOpenToExchange(data.isOpenToExchange());
		housing.setExchangeWanted(data.getExchangeWanted());
	}

	@Override
	public ArrayList<Housing> showOpenExchanges(Long ownerId) {

		return housingDao.findByOpenToExchangeTrueAndOwnerIdNot(ownerId);
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
		// No se usa isAvailableNow aquí: hace la pregunta contraria ("¿está libre?") y
		// negarla dos veces solo confunde. estaOcupadoAhora dice lo que de verdad
		// bloquea el intercambio.
		if (estaOcupadoAhora(housing.get().getId()) || estaOcupadoAhora(housingToTrade.get().getId())) {
			throw new AlreadyReservedException();
		}

		housing.get().setOwner(housingToTrade.get().getOwner());
		housingToTrade.get().setOwner(owner);
	}

	@Override
	public boolean isAvailableNow(Long housingId) {
		return !estaOcupadoAhora(housingId);
	}

	private boolean estaOcupadoAhora(Long housingId) {

		LocalDateTime ahora = LocalDateTime.now();
		return reservationDao.existsOverlappingReservation(housingId, ahora, ahora);
	}

	@Override
	public Set<Long> currentlyOccupiedHousingIds() {
		return new HashSet<>(reservationDao.findHousingIdsWithActiveStay(LocalDateTime.now()));
	}

}