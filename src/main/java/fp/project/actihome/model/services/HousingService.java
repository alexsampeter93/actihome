package fp.project.actihome.model.services;

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

	/**
	 * Da de alta un alojamiento a nombre de un ADMIN.
	 *
	 * <p>
	 * Los datos del alojamiento viajan en un {@link HousingData} en lugar de como
	 * diez argumentos sueltos. El motivo está explicado en esa clase; en resumen,
	 * con las comodidades del catálogo la firma anterior habría acabado con nueve
	 * booleanos consecutivos.
	 */
	Housing uploadHousing(HousingData data, Long ownerId) throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException;

	Housing findHousing(Long housingId) throws InstanceNotFoundException;

	ArrayList<Housing> showHousings();

	/**
	 * Modifica un alojamiento existente.
	 *
	 * <p>
	 * El código del alojamiento y el propietario <b>no</b> se tocan: el código es su
	 * identificador público y el propietario solo cambia mediante un intercambio.
	 * Todo lo demás se toma del {@link HousingData} recibido, así que quien llama
	 * debe enviarlo completo —partiendo de los valores actuales— y no solo los
	 * campos que cambia.
	 */
	Housing updateHousing(Long housingId, Long ownerId, HousingData data) throws InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotTheOwnerException, NotAuthorizedUserException;

	ArrayList<Housing> filterHousingsByType(String type);

	ArrayList<Housing> filterHousingsByMinimumRooms(int minimum);

	void tradeHousings(Long ownerId, Long ownersHousingId, Long housingToTradeCode)
			throws InstanceNotFoundException, AlreadyReservedException;
}
