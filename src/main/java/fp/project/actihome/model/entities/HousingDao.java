package fp.project.actihome.model.entities;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface HousingDao extends PagingAndSortingRepository<Housing, Long> {

	Optional<Housing> findById(Long housingId);

	Optional<Housing> findByHousingCode(Long housingCode);

	boolean existsByHousingCode(Long housingCode);

	ArrayList<Housing> findAllBy();

	@Query("Select h from Housing h where LOWER(h.type) LIKE %?1%")
	ArrayList<Housing> findHousingsByType(String type);

	@Query("Select h from Housing h where h.numberOfRooms >= ?1")
	ArrayList<Housing> findHousingsByNumberOfRooms(int minimum);
}