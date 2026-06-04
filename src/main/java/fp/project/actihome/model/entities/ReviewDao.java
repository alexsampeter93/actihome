package fp.project.actihome.model.entities;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface ReviewDao extends PagingAndSortingRepository<Review, Long> {

	boolean existsByAuthorIdAndHousingId(Long authorId, Long reviewId);

	Optional<Review> findById(Long reviewId);

	ArrayList<Review> findByHousingIdOrderByPublicationDateDesc(Long housingId);

}