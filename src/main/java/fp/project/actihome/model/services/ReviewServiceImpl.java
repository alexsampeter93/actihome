package fp.project.actihome.model.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.HousingDao;
import fp.project.actihome.model.entities.ReservationDao;
import fp.project.actihome.model.entities.Review;
import fp.project.actihome.model.entities.ReviewDao;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.AlreadyPublishedException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.MustHaveStayedException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotTheAuthorException;
import fp.project.actihome.model.exceptions.NotTheOwnerException;
import fp.project.actihome.model.exceptions.ScoreOutOfBoundsException;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

	@Autowired
	private PermissionChecker permissionChecker;

	@Autowired
	private ReviewDao reviewDao;

	@Autowired
	private HousingDao housingDao;

	@Autowired
	private ReservationDao reservationDao;

	@Override
	public Review publishReview(Long authorId, Long housingId, String title, String body, double locationScore,
			double serviceScore, double wifiScore, double foodScore, double cleaningScore)
			throws InstanceNotFoundException, AlreadyPublishedException, ScoreOutOfBoundsException,
			NotAuthorizedUserException, MustHaveStayedException {

		User author = permissionChecker.checkUser(authorId);
		Optional<Housing> housing = housingDao.findById(housingId);

		if (author.getRole() != RoleType.CUSTOMER) {
			throw new NotAuthorizedUserException();
		}

		if (!housing.isPresent()) {
			throw new InstanceNotFoundException("project.entities.housing", housingId);
		}

		// Fase 7.5.4: antes, cualquier CUSTOMER puntuaba cualquier alojamiento sin
		// haberlo pisado. Se exige una reserva propia, no cancelada, cuya salida ya
		// haya pasado — no basta con haber reservado, la estancia tiene que haberse
		// completado de verdad.
		if (!reservationDao.existsByCustomerIdAndHousingIdAndCancelledFalseAndCheckOutBefore(authorId, housingId,
				LocalDateTime.now())) {
			throw new MustHaveStayedException();
		}

		if ((locationScore < 0 || locationScore > 5) || (serviceScore < 0 || serviceScore > 5)
				|| (wifiScore < 0 || wifiScore > 5) || (foodScore < 0 || foodScore > 5)
				|| (cleaningScore < 0 || cleaningScore > 5)) {
			throw new ScoreOutOfBoundsException();
		}

		if (reviewDao.existsByAuthorIdAndHousingId(authorId, housingId)) {
			throw new AlreadyPublishedException();
		}
		
		double totalScore = (locationScore + serviceScore + wifiScore + foodScore + cleaningScore) / 5;
		Review newReview = new Review(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore,
				LocalDateTime.now(), author, housing.get());

		reviewDao.save(newReview);
		ArrayList<Review> reviews = reviewDao.findByHousingIdOrderByPublicationDateDesc(housingId);
		double score = 0.0;
		for (Review review : reviews) {
			score = score + review.getTotalScore();
		}
		double averageScore = score / reviews.size();
		housing.get().setScore(averageScore);

		return newReview;
	}
	

	@Override
	public Review findReview(Long reviewId) throws InstanceNotFoundException {

		Optional<Review> review = reviewDao.findById(reviewId);

		if (!review.isPresent()) {
			throw new InstanceNotFoundException("project.entities.review", reviewId);
		}

		return review.get();
	}

	@Override
	public ArrayList<Review> showHousingReviews(Long housingId) throws InstanceNotFoundException {

		Optional<Housing> housing = housingDao.findById(housingId);

		if (!housing.isPresent()) {
			throw new InstanceNotFoundException("project.entities.housing", housingId);
		}

		return reviewDao.findByHousingIdOrderByPublicationDateDesc(housingId);
	}

	@Override
	public Review updateReview(Long reviewId, Long authorId, String title, String body, double locationScore,
			double serviceScore, double wifiScore, double foodScore, double cleaningScore)
			throws InstanceNotFoundException, ScoreOutOfBoundsException, NotAuthorizedUserException,
			NotTheAuthorException {

		User author = permissionChecker.checkUser(authorId);
		Optional<Review> reviewToUpdate = reviewDao.findById(reviewId);

		if (author.getRole() != RoleType.CUSTOMER) {
			throw new NotAuthorizedUserException();
		}

		if (! reviewToUpdate.isPresent()) {
			throw new InstanceNotFoundException("project.entities.review", reviewId);
		}

		if ((locationScore < 0 || locationScore > 5) || (serviceScore < 0 || serviceScore > 5)
				|| (wifiScore < 0 || wifiScore > 5) || (foodScore < 0 || foodScore > 5)
				|| (cleaningScore < 0 || cleaningScore > 5)) {
			throw new ScoreOutOfBoundsException();
		}

		// equals y no != (B4, cerrado en la auditoría de la Fase 8.3). Comparar dos
		// entidades con != pregunta si son *el mismo objeto en memoria*, no si son la
		// misma fila. Aquí funcionaba de casualidad: dentro de una transacción, el
		// contexto de persistencia de JPA garantiza una única instancia por fila, así
		// que las dos referencias coinciden. Deja de funcionar en cuanto una de las dos
		// entidades venga de fuera de esa transacción — de una sesión anterior, de una
		// caché, o de un futuro cliente que hable con una API. Y el fallo no avisa: no
		// lanza nada, simplemente rechaza a un autor que sí lo es.
		if (!reviewToUpdate.get().getAuthor().getId().equals(author.getId())) {
			throw new NotTheAuthorException();
		}
		
		double totalScore = (locationScore + serviceScore + wifiScore + foodScore + cleaningScore) / 5;

		reviewToUpdate.get().setBody(body);
		reviewToUpdate.get().setTitle(title);
		reviewToUpdate.get().setLocationScore(locationScore);
		reviewToUpdate.get().setServiceScore(serviceScore);
		reviewToUpdate.get().setWifiScore(wifiScore);
		reviewToUpdate.get().setFoodScore(foodScore);
		reviewToUpdate.get().setCleaningScore(cleaningScore);
		reviewToUpdate.get().setTotalScore(totalScore);
		
		ArrayList<Review> reviews = reviewDao.findByHousingIdOrderByPublicationDateDesc(reviewToUpdate.get().getHousing().getId());
		double score = 0.0;
		for (Review review : reviews) {
			score = score + review.getTotalScore();
		}
		double averageScore = score / reviews.size();
		reviewToUpdate.get().getHousing().setScore(averageScore);

		return reviewToUpdate.get();
	}

	@Override
	public Review setReviewImage(Long reviewId, Long authorId, String image)
			throws InstanceNotFoundException, NotAuthorizedUserException, NotTheAuthorException {

		User author = permissionChecker.checkUser(authorId);
		Optional<Review> review = reviewDao.findById(reviewId);

		if (author.getRole() != RoleType.CUSTOMER) {
			throw new NotAuthorizedUserException();
		}

		if (!review.isPresent()) {
			throw new InstanceNotFoundException("project.entities.review", reviewId);
		}

		if (!review.get().getAuthor().getId().equals(authorId)) {
			throw new NotTheAuthorException();
		}

		review.get().setImage(image);

		return review.get();
	}

	@Override
	public Review respondToReview(Long reviewId, Long ownerId, String response)
			throws InstanceNotFoundException, NotAuthorizedUserException, NotTheOwnerException {

		User owner = permissionChecker.checkUser(ownerId);
		Optional<Review> review = reviewDao.findById(reviewId);

		if (owner.getRole() != RoleType.ADMIN) {
			throw new NotAuthorizedUserException();
		}

		if (!review.isPresent()) {
			throw new InstanceNotFoundException("project.entities.review", reviewId);
		}

		if (!review.get().getHousing().getOwner().getId().equals(ownerId)) {
			throw new NotTheOwnerException();
		}

		review.get().setOwnerResponse(response);
		review.get().setOwnerResponseDate(LocalDateTime.now());

		return review.get();
	}
}