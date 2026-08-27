package fp.project.actihome.ui.dev;

import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import fp.project.actihome.ActihomeApplication;
import fp.project.actihome.model.services.HousingService;

/**
 * Comprueba que una base de datos que YA EXISTE sobrevive a un cambio de
 * esquema, ejecutándolo sobre una copia en vez de sobre la de verdad.
 *
 * <pre>
 * copy %USERPROFILE%\.actihome\actihome.mv.db %TEMP%\copia.mv.db
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.ProbarMigracion" ^
 *     "-Dexec.args=jdbc:h2:file:C:/Users/tu-usuario/AppData/Local/Temp/copia;DB_CLOSE_DELAY=-1;MODE=MySQL"
 * </pre>
 *
 * <p>
 * <b>Por qué hace falta una herramienta para esto.</b> Los tests corren siempre
 * contra una base <em>vacía</em>, así que verifican el camino de la instalación
 * nueva y <b>nunca el de la que ya está</b> — que es justo el que rompe la
 * aplicación de alguien. Este proyecto ya se ha llevado ese susto dos veces con
 * el `migracion-h2.sql` que Flyway vino a sustituir: el fallo no aparece en el
 * ordenador donde se programó, porque allí la base se borró y se volvió a crear.
 *
 * <p>
 * Lo que hay que leer en la salida es <b>el tipo de la fila 1</b>. Si dice
 * {@code BASELINE}, Flyway ha dado la base por hecha en la versión 1 y no ha
 * tocado el esquema: correcto. Si dijera {@code SQL}, sería que ha ejecutado
 * V1 sobre una base que ya tenía tablas — y entonces o la ruta apunta a una base
 * vacía por error, o la configuración de baseline no está haciendo su trabajo.
 *
 * <p>
 * <b>Sobre una copia, y no sobre la base real.</b> Una migración se prueba una
 * vez y si sale mal deja la base a medias; hacerlo sobre el fichero de trabajo
 * significaría descubrir el fallo cuando ya no hay a dónde volver.
 */
public final class ProbarMigracion {

	private ProbarMigracion() {
	}

	public static void main(String[] args) {

		String url = args.length > 0 ? args[0] : "";

		SpringApplication app = new SpringApplication(ActihomeApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext c = app.run("--spring.main.headless=true",
				"--spring.datasource.url=" + url, "--spring.datasource.username=actihome",
				"--spring.datasource.password=", "--spring.profiles.active=test")) {

			JdbcTemplate jdbc = c.getBean(JdbcTemplate.class);

			System.out.println();
			System.out.println("=== historial de Flyway en la base migrada ===");

			List<String> filas = jdbc.query(
					"select \"installed_rank\", \"version\", \"description\", \"type\", \"success\" from \"flyway_schema_history\" order by \"installed_rank\"",
					(rs, i) -> String.format("%2d  %-8s %-22s %-10s %s", rs.getInt(1), rs.getString(2) == null ? "-"
							: rs.getString(2), rs.getString(3), rs.getString(4), rs.getBoolean(5) ? "ok" : "FALLO"));

			filas.forEach(System.out::println);

			HousingService alojamientos = c.getBean(HousingService.class);

			System.out.println();
			System.out.println("alojamientos que siguen ahi: " + alojamientos.showHousings().size());
			System.out.println("usuarios: " + jdbc.queryForObject("select count(*) from USERS", Integer.class));
			System.out.println("reservas: " + jdbc.queryForObject("select count(*) from RESERVATIONS", Integer.class));
			System.out.println("propuestas: " + jdbc.queryForObject("select count(*) from TRADE_PROPOSALS", Integer.class));
		}

		System.exit(0);
	}
}
