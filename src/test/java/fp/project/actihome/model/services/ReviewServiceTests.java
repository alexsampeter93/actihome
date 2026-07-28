package fp.project.actihome.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.Review;
import fp.project.actihome.model.entities.ReviewDao;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.AlreadyPublishedException;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.LessThanOneRoomException;
import fp.project.actihome.model.exceptions.NegativePrizeException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotTheAuthorException;
import fp.project.actihome.model.exceptions.ScoreOutOfBoundsException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class ReviewServiceTests {

	@Autowired
	private UserService userService;

	@Autowired
	private HousingService housingService;

	@Autowired
	private ReviewService reviewService;

	@Autowired
	private ReviewDao reviewDao;

	private User signUpUser(String username, RoleType role) {

		User user = new User(username, "password", "name", "surname", "locality", 664567076, username + "@" + username,
				LocalDateTime.now(), role);

		try {
			userService.signUp(user);
		} catch (DuplicateInstanceException e) {
			throw new RuntimeException(e);
		}

		return user;
	}

	private Housing createHousing(Long housingCode, Long ownerId) throws DuplicateInstanceException,
			InstanceNotFoundException, LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		return housingService.uploadHousing(
				HousingData.basico(housingCode, "Casa en la playa", "Casa", 6, BigDecimal.valueOf(20.65),
						"Playa del Orzán").description("Descripción breve").breakfast(true),
				ownerId);
	}

	@Test
	public void testPublishReview()
			throws DuplicateInstanceException, InstanceNotFoundException, LessThanOneRoomException,
			NegativePrizeException, NotAuthorizedUserException, AlreadyPublishedException, ScoreOutOfBoundsException {

		User author = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());
		Review review = reviewService.publishReview(author.getId(), housing.getId(), "Título", "Cuerpo", 3.0, 3.6, 5,
				4.2, 4.1);
		Review publishedReview = reviewDao.findById(review.getId()).get();
		double totalScore = (3.0 + 3.6 + 5 + 4.2 + 4.1) / 5;

		assertEquals(review.getTitle(), publishedReview.getTitle());
		assertEquals(review.getBody(), publishedReview.getBody());
		assertEquals(review.getAuthor(), publishedReview.getAuthor());
		assertEquals(review.getHousing(), publishedReview.getHousing());
		assertEquals(review.getLocationScore(), publishedReview.getLocationScore());
		assertEquals(review.getServiceScore(), publishedReview.getServiceScore());
		assertEquals(review.getWifiScore(), publishedReview.getWifiScore());
		assertEquals(review.getFoodScore(), publishedReview.getFoodScore());
		assertEquals(review.getCleaningScore(), publishedReview.getCleaningScore());
		assertEquals(publishedReview.getTotalScore(), totalScore);

	}

	@Test
	public void testPublishReviewSameHousingAndAuthor()
			throws DuplicateInstanceException, InstanceNotFoundException, LessThanOneRoomException,
			NegativePrizeException, NotAuthorizedUserException, AlreadyPublishedException, ScoreOutOfBoundsException {

		User author = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		reviewService.publishReview(author.getId(), housing.getId(), "Título", "Cuerpo", 3.5, 3.5, 3.5, 3.5, 3.5);

		assertThrows(AlreadyPublishedException.class, () -> reviewService.publishReview(author.getId(), housing.getId(),
				"Título", "Cuerpo", 3.5, 3.5, 3.5, 3.5, 3.5));

	}

	@Test
	public void testPublishReviewOutOfBoundsScores() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User author = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		assertThrows(ScoreOutOfBoundsException.class, () -> reviewService.publishReview(author.getId(), housing.getId(),
				"Título", "Cuerpo", -1, 3.5, 3.5, 3.5, 3.5));
		assertThrows(ScoreOutOfBoundsException.class, () -> reviewService.publishReview(author.getId(), housing.getId(),
				"Título", "Cuerpo", 3.5, 5.5, 3.5, 3.5, 3.5));
		assertThrows(ScoreOutOfBoundsException.class, () -> reviewService.publishReview(author.getId(), housing.getId(),
				"Título", "Cuerpo", 3.5, 3.5, -1, 3.5, 3.5));
		assertThrows(ScoreOutOfBoundsException.class, () -> reviewService.publishReview(author.getId(), housing.getId(),
				"Título", "Cuerpo", 3.5, 3.5, 3.5, 6, 3.5));
		assertThrows(ScoreOutOfBoundsException.class, () -> reviewService.publishReview(author.getId(), housing.getId(),
				"Título", "Cuerpo", 3.5, 3.5, 3.5, 3.5, -1));
	}

	@Test
	public void testPublishReviewWithNoAuthorization() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException {

		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		assertThrows(NotAuthorizedUserException.class, () -> reviewService.publishReview(owner.getId(), housing.getId(),
				"Título", "Cuerpo", 3.5, 3.5, 3.5, 3.5, 4));

	}

	@Test
	public void testPublishReviewHousingNotFound() {

		User author = signUpUser("Author", RoleType.CUSTOMER);

		assertThrows(InstanceNotFoundException.class, () -> reviewService.publishReview(author.getId(),
				Long.valueOf(60), "Título", "Cuerpo", 3.5, 3.5, 3.5, 3.5, 4));
	}

	@Test
	public void testFindReview() throws DuplicateInstanceException, InstanceNotFoundException, LessThanOneRoomException,
			NegativePrizeException, NotAuthorizedUserException, AlreadyPublishedException, ScoreOutOfBoundsException {

		User author = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		Review review = reviewService.publishReview(author.getId(), housing.getId(), "Título", "Cuerpo", 3.5, 3.5, 3.5,
				3.5, 3.5);

		Review publishedReview = reviewService.findReview(review.getId());

		assertEquals(review, publishedReview);
	}

	@Test
	public void testFindNonExistentReview() {

		assertThrows(InstanceNotFoundException.class, () -> reviewService.findReview(Long.valueOf(60)));
	}

	@Test
	public void testShowHousingReviews()
			throws DuplicateInstanceException, InstanceNotFoundException, LessThanOneRoomException,
			NegativePrizeException, NotAuthorizedUserException, AlreadyPublishedException, ScoreOutOfBoundsException {

		User author1 = signUpUser("Author1", RoleType.CUSTOMER);
		User author2 = signUpUser("Author2", RoleType.CUSTOMER);
		User author3 = signUpUser("Author3", RoleType.CUSTOMER);
		User author4 = signUpUser("Author4", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		Review review1 = reviewService.publishReview(author1.getId(), housing.getId(), "Título", "Cuerpo", 3.5, 3.8,
				3.5, 3.5, 3.5);
		Review review2 = reviewService.publishReview(author2.getId(), housing.getId(), "Título", "Cuerpo", 3.5, 3.5,
				3.5, 3.3, 3.5);
		Review review3 = reviewService.publishReview(author3.getId(), housing.getId(), "Título", "Cuerpo", 3.5, 3.5,
				3.5, 3.5, 4.5);
		Review review4 = reviewService.publishReview(author4.getId(), housing.getId(), "Título", "Cuerpo", 1.5, 3.5,
				3.5, 2.5, 3.5);

		review1.setPublicationDate(LocalDateTime.now().minusHours(4));
		review2.setPublicationDate(LocalDateTime.now().minusHours(3));
		review3.setPublicationDate(LocalDateTime.now().minusHours(2));
		review4.setPublicationDate(LocalDateTime.now().minusHours(1));
		double housingScore = (review1.getTotalScore() + review2.getTotalScore() + review3.getTotalScore()
				+ review4.getTotalScore()) / 4;

		ArrayList<Review> reviews = reviewService.showHousingReviews(housing.getId());

		assertEquals(reviews.get(0), review4);
		assertEquals(reviews.get(1), review3);
		assertEquals(reviews.get(2), review2);
		assertEquals(reviews.get(3), review1);

		// Comparación con tolerancia, no exacta. Dos números decimales que "deberían"
		// ser iguales casi nunca lo son bit a bit: el orden de las sumas cambia el
		// último dígito, y cada base de datos redondea a su manera al guardarlos y
		// leerlos. Este test fallaba con 3,4050000000000002 frente a 3,405.
		//
		// La regla: los decimales nunca se comparan con igualdad, siempre con un
		// margen. (Y el dinero no se guarda en decimales, sino en BigDecimal, que es
		// lo que ya hace esta aplicación con los precios.)
		assertEquals(housingScore, housing.getScore(), 0.0001);
	}

	@Test
	public void testShowNonExistentHousingReviews() {

		assertThrows(InstanceNotFoundException.class, () -> reviewService.showHousingReviews(Long.valueOf(40)));
	}

	@Test
	public void testUpdateReview() throws DuplicateInstanceException, InstanceNotFoundException,
			LessThanOneRoomException, NegativePrizeException, NotAuthorizedUserException, AlreadyPublishedException,
			ScoreOutOfBoundsException, NotTheAuthorException {

		User author = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		Review review = reviewService.publishReview(author.getId(), housing.getId(), "Título", "Cuerpo", 3.5, 3.5, 3.5,
				3.5, 3.5);

		Review updatedReview = reviewService.updateReview(review.getId(), author.getId(), "Nuevo título",
				"Nuevo cuerpo", 5, 4, 3, 2, 1);

		double totalScore = (5 + 4 + 3 + 2 + 1) / 5;
		double housingScore = totalScore;

		assertEquals(updatedReview.getTitle(), "Nuevo título");
		assertEquals(updatedReview.getBody(), "Nuevo cuerpo");
		assertEquals(updatedReview.getLocationScore(), 5);
		assertEquals(updatedReview.getServiceScore(), 4);
		assertEquals(updatedReview.getWifiScore(), 3);
		assertEquals(updatedReview.getFoodScore(), 2);
		assertEquals(updatedReview.getCleaningScore(), 1);
		assertEquals(updatedReview.getTotalScore(), totalScore);
		assertEquals(housing.getScore(), housingScore);

	}

	@Test
	public void testUpdateReviewWithNoAuthorization()
			throws DuplicateInstanceException, InstanceNotFoundException, LessThanOneRoomException,
			NegativePrizeException, NotAuthorizedUserException, AlreadyPublishedException, ScoreOutOfBoundsException {

		User author = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		Review review = reviewService.publishReview(author.getId(), housing.getId(), "Título", "Cuerpo", 3.5, 3.5, 3.5,
				3.5, 3.5);

		author.setRole(RoleType.ADMIN);

		assertThrows(NotAuthorizedUserException.class, () -> reviewService.updateReview(review.getId(), author.getId(),
				"Nuevo título", "Nuevo cuerpo", 5, 4, 3, 2, 1));

	}

	@Test
	public void testUpdateAnotherReview()
			throws DuplicateInstanceException, InstanceNotFoundException, LessThanOneRoomException,
			NegativePrizeException, NotAuthorizedUserException, AlreadyPublishedException, ScoreOutOfBoundsException {

		User author = signUpUser("Author", RoleType.CUSTOMER);
		User author2 = signUpUser("Author2", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		Review review = reviewService.publishReview(author.getId(), housing.getId(), "Título", "Cuerpo", 3.5, 3.5, 3.5,
				3.5, 3.5);

		assertThrows(NotTheAuthorException.class, () -> reviewService.updateReview(review.getId(), author2.getId(),
				"Nuevo título", "Nuevo cuerpo", 5, 4, 3, 2, 1));
	}

	@Test
	public void testUpdateReviewOutOfBoundsScores()
			throws InstanceNotFoundException, AlreadyPublishedException, ScoreOutOfBoundsException,
			NotAuthorizedUserException, DuplicateInstanceException, LessThanOneRoomException, NegativePrizeException {

		User author = signUpUser("Author", RoleType.CUSTOMER);
		User owner = signUpUser("Owner", RoleType.ADMIN);
		Housing housing = createHousing(Long.valueOf(50), owner.getId());

		Review review = reviewService.publishReview(author.getId(), housing.getId(), "Título", "Cuerpo", 3.5, 3.5, 3.5,
				3.5, 3.5);

		assertThrows(ScoreOutOfBoundsException.class, () -> reviewService.updateReview(review.getId(), author.getId(),
				"Nuevo título", "Nuevo cuerpo", -1, 4, 3, 2, 1));
		assertThrows(ScoreOutOfBoundsException.class, () -> reviewService.updateReview(review.getId(), author.getId(),
				"Nuevo título", "Nuevo cuerpo", 5, 6, 3, 2, 1));
		assertThrows(ScoreOutOfBoundsException.class, () -> reviewService.updateReview(review.getId(), author.getId(),
				"Nuevo título", "Nuevo cuerpo", 5, 4, -3, 2, 1));
		assertThrows(ScoreOutOfBoundsException.class, () -> reviewService.updateReview(review.getId(), author.getId(),
				"Nuevo título", "Nuevo cuerpo", 5, 4, 3, 8, 1));
		assertThrows(ScoreOutOfBoundsException.class, () -> reviewService.updateReview(review.getId(), author.getId(),
				"Nuevo título", "Nuevo cuerpo", 5, 4, 3, 2, -1));
	}

	@Test
	public void testUpdateNonExistentReview() {

		User author = signUpUser("Author", RoleType.CUSTOMER);

		assertThrows(InstanceNotFoundException.class, () -> reviewService.updateReview(Long.valueOf(50), author.getId(),
				"Nuevo título", "Nuevo cuerpo", 5, 4, 3, 4, 1));

	}
}