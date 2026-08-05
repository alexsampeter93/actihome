package fp.project.actihome.model.exceptions;

/** La base de datos sabe hacer copias de seguridad, pero esta en concreto ha fallado (disco lleno, ruta sin permiso...). */
@SuppressWarnings("serial")
public class BackupFailedException extends Exception {

	public BackupFailedException(Throwable causa) {
		super(causa);
	}
}
