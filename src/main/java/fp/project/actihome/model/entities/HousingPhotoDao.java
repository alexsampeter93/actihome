package fp.project.actihome.model.entities;

import java.util.ArrayList;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HousingPhotoDao extends JpaRepository<HousingPhoto, Long> {

	/**
	 * Las fotos de galería de un alojamiento, en el orden en que se enseñan.
	 *
	 * <p>
	 * El orden va en la consulta y no lo pone la pantalla: una galería que sale
	 * distinta cada vez que se abre se lee como ruido, y confiar en el orden en
	 * que la base de datos devuelva las filas es confiar en algo que SQL no
	 * garantiza sin un {@code ORDER BY}.
	 */
	ArrayList<HousingPhoto> findByHousingIdOrderByPositionAsc(Long housingId);

	/** Cuántas tiene ya, para saber en qué posición entra la siguiente. */
	int countByHousingId(Long housingId);
}
