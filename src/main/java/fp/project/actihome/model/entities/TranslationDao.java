package fp.project.actihome.model.entities;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface TranslationDao extends PagingAndSortingRepository<Translation, Long> {

	Optional<Translation> findBySourceHashAndTargetLanguage(String sourceHash, String targetLanguage);

	/**
	 * Varias de golpe, para poder resolver una pantalla entera con una consulta.
	 *
	 * <p>
	 * Sin esto, pintar el catálogo en inglés sería una consulta por cada
	 * descripción — el mismo N+1 que el proyecto ya acepta a conciencia para
	 * contar reseñas, pero que aquí no hay motivo para repetir: los textos se
	 * conocen todos antes de empezar a pintar.
	 */
	List<Translation> findBySourceHashInAndTargetLanguage(List<String> sourceHashes, String targetLanguage);
}
