package fp.project.actihome.model.services;

import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.EstacionPreferida;
import fp.project.actihome.model.entities.User.Idioma;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.IncorrectLoginException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.WrongPasswordException;

public interface UserService {
	
	void signUp(User user) throws DuplicateInstanceException;
	
	User login(String username, String password) throws IncorrectLoginException;
	
	User loginFromId(Long id) throws InstanceNotFoundException;
	
	User updateProfile(Long id, String username, String name, String surname, String email, int phonenumber,
			String locality) throws InstanceNotFoundException, DuplicateInstanceException;
	
	void changePassword(Long userId, String oldPassword, String newPassword)
			throws InstanceNotFoundException, WrongPasswordException;

	/**
	 * Cambia el rol del usuario al contrario del que tiene y devuelve cómo queda.
	 *
	 * <p>
	 * <b>Por qué el cambio se guarda en la base de datos y no solo en la sesión.</b>
	 * La autorización de todos los servicios se resuelve con
	 * {@code permissionChecker.checkUser(id).getRole()}, es decir, leyendo el rol
	 * <em>de la base</em>. Si el cambio viviera solo en memoria, la aplicación
	 * enseñaría las opciones de administrador y el servicio las rechazaría una por
	 * una: peor que no ofrecer el cambio.
	 *
	 * <p>
	 * Es una función pensada para poder recorrer la aplicación entera con una sola
	 * cuenta —los dos roles ven cosas distintas— y esa es su justificación. En un
	 * producto real, cambiarse el rol a uno mismo sería una escalada de privilegios
	 * y tendría que autorizarlo otra persona.
	 */
	User changeRole(Long userId) throws InstanceNotFoundException;

	/**
	 * Guarda las preferencias de Ajustes (Fase 7.6, ampliada en la Fase 7.11 con
	 * la vista de catálogo). {@code defaultSeason} puede ser {@code null}:
	 * significa "sin preferencia guardada, usa la estación real de hoy".
	 */
	User updatePreferences(Long userId, EstacionPreferida defaultSeason, boolean particlesEnabled, Idioma language,
			boolean defaultGridView) throws InstanceNotFoundException;

	/**
	 * Marca la bienvenida como vista (Fase 7.8). A partir de aquí, iniciar sesión
	 * lleva directo al catálogo en vez de a la pantalla de bienvenida.
	 */
	User completeOnboarding(Long userId) throws InstanceNotFoundException;
}
