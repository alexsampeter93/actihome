// Dao: objeto de acceso a datos (permite entrar a los datos de la base de datos)
package fp.project.actihome.model.entities;

import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface UserDao extends PagingAndSortingRepository<User, Long> {
	
	 //Comprobará si el nombre de usuario "username" existe
	boolean existsByUsername(String username);
	
	//Devolverá al usuario de nombre "username"
	Optional<User> findByUsername(String username); 

}
