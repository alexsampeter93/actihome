package fp.project.actihome.model.services;

import java.util.ArrayList;

import fp.project.actihome.model.entities.Review;
import fp.project.actihome.model.exceptions.AlreadyPublishedException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.MustHaveStayedException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotTheAuthorException;
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
}