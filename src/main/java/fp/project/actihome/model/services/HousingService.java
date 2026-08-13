package fp.project.actihome.model.services;

import java.util.ArrayList;
import java.util.Set;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.HousingPhoto;
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
	 * Los intercambios abiertos por otros propietarios (Fase 8.4).
	 *
	 * <p>
	 * Alimenta la lista de "intercambios abiertos ahora mismo" de la pantalla de
	 * intercambio, que es lo que la saca del estado vacio. Excluye los propios: un
	 * tablon de anuncios en el que aparecen tus propios anuncios no informa de
	 * nada.
	 */
	ArrayList<Housing> showOpenExchanges(Long ownerId);

	/**
	 * Las fotos de galería de un alojamiento, en su orden, sin incluir la
	 * principal (Fase 8.4).
	 *
	 * <p>
	 * Devuelve lista vacía si no tiene ninguna, que es el caso de cualquier
	 * alojamiento publicado sin subir fotos extra. La pantalla no necesita
	 * distinguir "sin galería" de "galería vacía": las dos se pintan igual.
	 */
	ArrayList<HousingPhoto> showHousingPhotos(Long housingId);

	/**
	 * Añade una foto al final de la galería.
	 *
	 * <p>
	 * Solo el propietario, igual que editar. La posición la decide el servicio a
	 * partir de cuántas hay ya: dejársela al que llama sería invitar a que dos
	 * pantallas escribieran la misma.
	 */
	HousingPhoto addHousingPhoto(Long housingId, Long ownerId, String image)
			throws InstanceNotFoundException, NotTheOwnerException, NotAuthorizedUserException;

	/**
	 * Quita una foto de la galería y <b>recoloca las siguientes</b>.
	 *
	 * <p>
	 * Sin recolocar, borrar la segunda de cuatro dejaría las posiciones 1, 3 y 4:
	 * la galería seguiría viéndose bien —el orden relativo no cambia— pero cada
	 * borrado abriría un hueco, y la posición dejaría de significar "la enésima"
	 * para significar "un número que solo sirve para ordenar". Es de esas cosas
	 * que no dan problemas hasta que alguien escribe la función de reordenar.
	 */
	void removeHousingPhoto(Long photoId, Long ownerId)
			throws InstanceNotFoundException, NotTheOwnerException, NotAuthorizedUserException;

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

	/**
	 * Permuta la titularidad de dos alojamientos.
	 *
	 * <p>
	 * <b>Esto es el mecanismo, no la operación de negocio.</b> Un intercambio
	 * necesita el consentimiento de las dos partes, y ese acuerdo lo gestiona
	 * {@link TradeProposalService}: aquí solo se ejecuta lo ya acordado. Llamar a
	 * este método directamente desde una pantalla sería saltarse la conformidad
	 * del otro propietario, que es exactamente el fallo que tenía la aplicación
	 * antes de la Fase 9.
	 *
	 * <p>
	 * <b>Comprueba que quien lo pide es dueño de lo que ofrece</b>, cosa que no
	 * hacía. Sin esa comprobación, cualquier usuario podía permutar dos
	 * alojamientos ajenos entre sí; el método se llamaba desde un solo sitio que
	 * pasaba siempre un alojamiento propio, así que el agujero no se veía —y "no
	 * se ve" no es lo mismo que "no está".
	 */
	void tradeHousings(Long ownerId, Long ownersHousingId, Long housingToTradeCode)
			throws InstanceNotFoundException, AlreadyReservedException, NotTheOwnerException;

	/**
	 * Si el alojamiento tiene una estancia en curso justo ahora.
	 *
	 * <p>
	 * Sustituye al antiguo campo guardado {@code Housing.available} (Fase 7.5,
	 * bug B5): no es un dato que se lea, es una pregunta que se calcula sobre las
	 * reservas activas. Para una pantalla que muestra un único alojamiento
	 * (detalle, ficha de intercambio).
	 */
	boolean isAvailableNow(Long housingId);

	/**
	 * Los ids de los alojamientos con una estancia en curso justo ahora.
	 *
	 * <p>
	 * Para el catálogo: una sola consulta por cada vez que se recarga el listado,
	 * en vez de una por fila. Mismo criterio ya aceptado en
	 * {@code ShowHousingsFrame.contarResenas} para el recuento de reseñas.
	 */
	Set<Long> currentlyOccupiedHousingIds();
}
