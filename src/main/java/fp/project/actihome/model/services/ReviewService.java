package fp.project.actihome.model.services;

import java.util.ArrayList;

import fp.project.actihome.model.entities.Review;
import fp.project.actihome.model.exceptions.AlreadyPublishedException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.MustHaveStayedException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotTheAuthorException;
import fp.project.actihome.model.exceptions.ScoreOutOfBoundsException;
import fp.project.actihome.model.exceptions.TranslationNotConfiguredException;

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

	/**
	 * Traduce el título y el cuerpo de una reseña al idioma indicado.
	 *
	 * <p>
	 * Seam pendiente de la Fase 7.7 (04-08-2026): el usuario pidió dejar la
	 * infraestructura lista sin decidir todavía con qué proveedor se traduce, así
	 * que hoy valida que la reseña exista y siempre lanza
	 * {@link TranslationNotConfiguredException}. Devuelve {@code void} a propósito
	 * en vez de comprometerse ya a una forma de resultado (¿un texto suelto? ¿se
	 * persiste en la reseña?): esa forma la decide el proveedor elegido, no esta
	 * pantalla vacía.
	 */
	void translateReview(Long reviewId, String targetLanguage)
			throws InstanceNotFoundException, TranslationNotConfiguredException;
}