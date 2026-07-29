package fp.project.actihome.model;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Repara los acentos que quedaron corrompidos en bases de datos creadas antes de
 * que se fijara la codificación de los scripts de arranque.
 *
 * <p>
 * <b>De dónde viene el daño.</b> Spring leía {@code data.sql} con el juego de
 * caracteres del sistema —Windows-1252 en un Windows en español— cuando el
 * archivo está en UTF-8. En UTF-8 una "á" ocupa dos bytes, y leídos de uno en
 * uno como Windows-1252 se convierten en dos caracteres: "Ã¡". Así se sembraron
 * "MÃ¡laga", "CabaÃ±a del Bosque" o "Loft Barrio GÃ³tico".
 *
 * <p>
 * La lectura ya está corregida ({@code sql-script-encoding: UTF-8}), pero eso
 * solo arregla las bases <b>nuevas</b>: las inserciones de {@code data.sql} van
 * protegidas por {@code WHERE NOT EXISTS}, así que en una base que ya existía
 * las filas dañadas no se vuelven a tocar nunca. Es exactamente el mismo caso
 * que las contraseñas en texto plano de la Fase 3d, y se resuelve igual: con una
 * reparación que se puede ejecutar en cada arranque sin hacer nada la segunda
 * vez.
 *
 * <p>
 * <b>Por qué en Java y no con una lista de {@code UPDATE}.</b> El daño no está
 * en un campo, está en todos los textos de cuatro tablas —nombres, ubicaciones,
 * descripciones, títulos y cuerpos de reseña—, y escribir a mano cada cadena
 * corrompida sería largo y frágil. Aquí se deshace la conversión, que es
 * reversible: se vuelven a tomar los caracteres como los bytes que eran y se
 * releen como UTF-8.
 *
 * <p>
 * <b>Y por qué es seguro.</b> Se repara únicamente si se cumplen las tres
 * condiciones de {@link #reparar}: que el texto quepa en un solo byte por
 * carácter, que al releerlo en UTF-8 no aparezca ningún carácter de sustitución,
 * y que el resultado sea distinto del original. Un texto correcto que el usuario
 * haya escrito —"A Coruña"— falla la primera condición y no se toca.
 */
@Component
@Order(0)
public class ReparacionDeAcentos implements ApplicationRunner {

	/** Tabla, clave primaria y columnas de texto que hay que revisar. */
	private static final String[][] OBJETIVOS = {
			{ "USERS", "id", "name", "surname", "locality", "email" },
			{ "HOUSINGS", "id", "name", "type", "description", "location" },
			{ "REVIEWS", "id", "title", "body" },
			{ "RESERVATIONS", "id", "paymentMethod" } };

	private final DataSource dataSource;

	public ReparacionDeAcentos(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public void run(ApplicationArguments args) {

		int reparados = 0;

		try (Connection conexion = dataSource.getConnection()) {

			for (String[] objetivo : OBJETIVOS) {
				reparados += repararTabla(conexion, objetivo);
			}

		} catch (SQLException ex) {
			// Esto es una reparación de datos heredados, no una función de la
			// aplicación: si falla, la aplicación tiene que abrir igual. Se avisa por
			// consola y se sigue.
			System.err.println("[ReparacionDeAcentos] No se pudo revisar la base de datos: " + ex.getMessage());
			return;
		}

		if (reparados > 0) {
			System.out.println("[ReparacionDeAcentos] Textos corregidos: " + reparados);
		}
	}

	private int repararTabla(Connection conexion, String[] objetivo) throws SQLException {

		String tabla = objetivo[0];
		String clave = objetivo[1];
		int reparados = 0;

		for (int i = 2; i < objetivo.length; i++) {

			String columna = objetivo[i];
			String consulta = "SELECT " + clave + ", " + columna + " FROM " + tabla + " WHERE " + columna
					+ " IS NOT NULL";

			try (Statement lectura = conexion.createStatement(); ResultSet filas = lectura.executeQuery(consulta)) {

				while (filas.next()) {

					String actual = filas.getString(2);
					String arreglado = reparar(actual);

					if (arreglado != null) {
						escribir(conexion, tabla, columna, clave, filas.getLong(1), arreglado);
						reparados++;
					}
				}
			}
		}

		return reparados;
	}

	private void escribir(Connection conexion, String tabla, String columna, String clave, long id, String valor)
			throws SQLException {

		try (PreparedStatement sentencia = conexion
				.prepareStatement("UPDATE " + tabla + " SET " + columna + " = ? WHERE " + clave + " = ?")) {

			sentencia.setString(1, valor);
			sentencia.setLong(2, id);
			sentencia.executeUpdate();
		}
	}

	/**
	 * Deshace la conversión errónea, o devuelve {@code null} si el texto ya está
	 * bien.
	 *
	 * <p>
	 * Tres comprobaciones, y las tres hacen falta:
	 *
	 * <ol>
	 * <li><b>Todos los caracteres deben caber en un byte.</b> Un texto corrompido
	 * solo contiene caracteres del repertorio Windows-1252, porque así se leyó. Si
	 * hay alguno por encima de 255 —una "ñ" de verdad ya es 241, pero una "€" o un
	 * carácter asiático no cabrían— es que el texto está bien y no hay nada que
	 * deshacer.</li>
	 * <li><b>Al releer en UTF-8 no puede salir el carácter de sustitución</b>
	 * (U+FFFD). Si sale, es que la secuencia de bytes no era UTF-8 válido, o sea
	 * que el texto no venía de esta conversión.</li>
	 * <li><b>El resultado tiene que ser distinto.</b> Un texto sin acentos —"Casa
	 * Rural El Pinar"— pasa las dos primeras y sale idéntico; devolver
	 * {@code null} evita escribir en la base miles de filas que no cambian.</li>
	 * </ol>
	 */
	static String reparar(String texto) {

		if (texto.isEmpty()) {
			return null;
		}

		for (int i = 0; i < texto.length(); i++) {
			if (texto.charAt(i) > 0xFF) {
				return null;
			}
		}

		String candidato = new String(texto.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);

		if (candidato.indexOf('�') >= 0 || candidato.equals(texto)) {
			return null;
		}

		return candidato;
	}
}
