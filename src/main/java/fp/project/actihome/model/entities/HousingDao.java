package fp.project.actihome.model.entities;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HousingDao extends JpaRepository<Housing, Long> {

	Optional<Housing> findById(Long housingId);

	Optional<Housing> findByHousingCode(Long housingCode);

	boolean existsByHousingCode(Long housingCode);

	ArrayList<Housing> findAllBy();

	/**
	 * Los alojamientos ofrecidos para permutar por alguien que no seas tú.
	 *
	 * <p>
	 * El filtro del propietario va en la consulta y no en Java a propósito: es una
	 * condición de negocio —"los de otros"— y dejarla fuera significaría traerse
	 * también los tuyos para descartarlos después, con el riesgo de que alguna
	 * pantalla se olvide de descartarlos y te ofrezca intercambiar contigo mismo.
	 */
	ArrayList<Housing> findByOpenToExchangeTrueAndOwnerIdNot(Long ownerId);

	/**
	 * Búsqueda por tipo, sin distinguir mayúsculas.
	 *
	 * <p>
	 * La consulta ponía en minúsculas <b>la columna pero no el parámetro</b>, así
	 * que buscar "Casa" comparaba "casa en la playa" contra "%Casa%" y no
	 * encontraba nada. Funcionaba solo porque MySQL compara texto sin distinguir
	 * mayúsculas por defecto; al pasar a H2, que sí distingue, el fallo salió a la
	 * luz.
	 *
	 * <p>
	 * Es un buen ejemplo de por qué conviene no apoyarse en el comportamiento
	 * particular de un motor: lo que parece que funciona puede estar funcionando
	 * por accidente.
	 */
	@Query("select h from Housing h where lower(h.type) like lower(concat('%', ?1, '%'))")
	ArrayList<Housing> findHousingsByType(String type);

	@Query("Select h from Housing h where h.numberOfRooms >= ?1")
	ArrayList<Housing> findHousingsByNumberOfRooms(int minimum);
}