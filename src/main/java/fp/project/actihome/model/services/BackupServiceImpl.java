package fp.project.actihome.model.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.BackupFailedException;
import fp.project.actihome.model.exceptions.BackupNotAvailableException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;

/**
 * Copia de seguridad vía el comando {@code BACKUP TO} de H2.
 *
 * <p>
 * <b>Por qué no una copia de fichero a secas.</b> {@code actihome.mv.db} está
 * abierto todo el tiempo que la aplicación está en marcha —el pool de
 * conexiones de Hikari mantiene una conexión viva—, así que copiar el fichero
 * byte a byte con {@code Files.copy} arriesga capturar una escritura a medias.
 * {@code BACKUP TO} es el mecanismo que la propia H2 ofrece para esto: genera
 * una foto fija consistente de la base <b>en caliente</b>, sin bloquear a
 * quien esté usando la aplicación mientras se genera.
 *
 * <p>
 * <b>Solo funciona sobre H2.</b> Es sintaxis propia del motor, así que sobre
 * el perfil {@code mysql} —un MySQL de servidor de verdad, con sus propias
 * herramientas de respaldo (`mysqldump`)— la operación se rechaza con
 * {@link BackupNotAvailableException} en lugar de fallar con un error de SQL
 * que no significa nada para quien lo lea en la pantalla.
 */
@Service
@Transactional
public class BackupServiceImpl implements BackupService {

	@Autowired
	private PermissionChecker permissionChecker;

	@PersistenceContext
	private EntityManager entityManager;

	@Value("${spring.datasource.url}")
	private String urlBaseDeDatos;

	@Override
	public void exportarCopiaDeSeguridad(Long adminId, String rutaDestino) throws InstanceNotFoundException,
			NotAuthorizedUserException, BackupNotAvailableException, BackupFailedException {

		User admin = permissionChecker.checkUser(adminId);

		if (admin.getRole() != RoleType.ADMIN) {
			throw new NotAuthorizedUserException();
		}

		if (!urlBaseDeDatos.startsWith("jdbc:h2:")) {
			throw new BackupNotAvailableException();
		}

		try {
			// BACKUP TO no admite parámetros con "?": la ruta va incrustada en la
			// sentencia. Un apóstrofo literal en la ruta se escapa duplicándolo, que es
			// como H2 espera un apóstrofo dentro de una cadena SQL.
			String rutaEscapada = rutaDestino.replace("'", "''");
			entityManager.createNativeQuery("BACKUP TO '" + rutaEscapada + "'").executeUpdate();

		} catch (RuntimeException ex) {
			throw new BackupFailedException(ex);
		}
	}
}
