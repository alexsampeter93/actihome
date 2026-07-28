package fp.project.actihome.model.exceptions;

/**
 * El nombre de usuario no existe o la contraseña no coincide.
 *
 * <p>
 * <b>Guarda el usuario, pero no la contraseña.</b> Antes llevaba las dos, y eso
 * es una fuga esperando a ocurrir: basta con que alguien registre la excepción
 * en un log, la imprima con {@code printStackTrace} o la envíe a un servicio de
 * seguimiento de errores para que una contraseña en claro acabe escrita en un
 * fichero. Un dato sensible no debe viajar dentro de un objeto que está pensado
 * para ser contado por ahí.
 *
 * <p>
 * La excepción tampoco distingue entre "ese usuario no existe" y "la contraseña
 * es otra". Distinguirlo sería más amable, y a la vez permitiría averiguar qué
 * nombres de usuario están registrados probándolos uno a uno.
 */
@SuppressWarnings("serial")
public class IncorrectLoginException extends Exception {

	private final String username;

	public IncorrectLoginException(String username) {

		super();
		this.username = username;
	}

	public String getUsername() {
		return username;
	}
}
