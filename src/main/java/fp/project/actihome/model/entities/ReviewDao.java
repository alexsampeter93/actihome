package fp.project.actihome.model.entities;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewDao extends JpaRepository<Review, Long> {

	boolean existsByAuthorIdAndHousingId(Long authorId, Long reviewId);

	Optional<Review> findById(Long reviewId);

	ArrayList<Review> findByHousingIdOrderByPublicationDateDesc(Long housingId);

}