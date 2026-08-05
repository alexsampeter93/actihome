package fp.project.actihome.model.services;

import fp.project.actihome.model.exceptions.BackupFailedException;
import fp.project.actihome.model.exceptions.BackupNotAvailableException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;

public interface BackupService {

	/**
	 * Genera una copia de seguridad completa de la base de datos en la ruta
	 * indicada, sin necesidad de parar la aplicación ni de que ningún otro
	 * usuario deje de trabajar mientras se hace.
	 *
	 * <p>
	 * Reservado a ADMIN: es una operación sobre toda la base de datos, no sobre
	 * los alojamientos de quien la pide.
	 */
	void exportarCopiaDeSeguridad(Long adminId, String rutaDestino) throws InstanceNotFoundException,
			NotAuthorizedUserException, BackupNotAvailableException, BackupFailedException;
}
