package fp.project.actihome.model.services;

import java.util.ArrayList;

import fp.project.actihome.model.entities.Review;
import fp.project.actihome.model.exceptions.AlreadyPublishedException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.MustHaveStayedException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotTheAuthorException;
import fp.project.actihome.model.exceptions.NotTheOwnerException;
import fp.project.actihome.model.exceptions.ScoreOutOfBoundsException;

public interface ReviewService {

	Review publishReview(Long authorId, Long housingId, String title, String body, double locationScore,
			double serviceScore, double wifiScore, double foodScore, double cleaningScore)
			throws InstanceNotFoundException, AlreadyPublishedException, ScoreOutOfBoundsException,
			NotAuthorizedUserException, MustHaveStayedException;

	Review findReview(Long reviewId) throws InstanceNotFoundException;

	ArrayList<Review> showHousingReviews(Long housingId) throws InstanceNotFoundException;

	Review updateReview(Long reviewId, Long authorId, String title, String body, double locationScore,
			double serviceScore, double wifiScore, double foodScore, double cleaningScore)
			throws InstanceNotFoundException, ScoreOutOfBoundsException, NotAuthorizedUserException,
			NotTheAuthorException;

	// La traducción vivía aquí desde la Fase 7.7 y salió en la 8.5, a
	// TranslationService. El motivo, con detalle, está en el javadoc de esa
	// interfaz: leer de la base de datos y llamar por internet no pueden convivir
	// en la misma transacción, y cuando dos cosas no pueden convivir en una
	// transacción es que no son la misma operación.

	/**
	 * Adjunta o quita la foto de una reseña (F15).
	 *
	 * <p>
	 * Va separado de {@link #publishReview} y {@link #updateReview} por el mismo
	 * motivo que la foto de un alojamiento va separada de
	 * {@code HousingService.uploadHousing}: el nombre del archivo no se conoce
	 * hasta que la foto se ha guardado, y guardarla es cosa de la capa de
	 * interfaz, no del servicio.
	 */
	Review setReviewImage(Long reviewId, Long authorId, String image)
			throws InstanceNotFoundException, NotAuthorizedUserException, NotTheAuthorException;

	/**
	 * Respuesta pública del propietario del alojamiento a una reseña (F15).
	 *
	 * <p>
	 * Solo el propietario del alojamiento reseñado puede responder — no
	 * cualquier ADMIN—, y responder de nuevo sustituye la respuesta anterior en
	 * vez de acumularlas: es una respuesta pública, no una conversación.
	 */
	Review respondToReview(Long reviewId, Long ownerId, String response)
			throws InstanceNotFoundException, NotAuthorizedUserException, NotTheOwnerException;
}