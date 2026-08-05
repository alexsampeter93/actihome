package fp.project.actihome.model.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.BackupFailedException;
import fp.project.actihome.model.exceptions.BackupNotAvailableException;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;

/**
 * <b>Por qué este test arranca su propio contexto de Spring, con otra URL de
 * base de datos.</b> El perfil {@code test} normal usa H2 <i>en memoria</i>
 * ({@code jdbc:h2:mem:...}), y H2 rechaza el comando {@code BACKUP TO} ahí con
 * "la base de datos no es persistente" — es una limitación real del propio
 * motor, no un fallo de {@link BackupServiceImpl}: no hay ningún fichero del
 * que hacer una foto fija. Para probar el camino que sí tiene éxito hace falta
 * una H2 <b>de fichero</b>, así que esta clase sobrescribe la URL con
 * {@code properties} apuntando a un fichero temporal único
 * ({@code random.uuid}), lo que le cuesta a Spring un contexto de test aparte
 * —unos segundos más al arrancar la suite— pero deja probado el camino real.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:file:${java.io.tmpdir}/actihome-backup-test-${random.uuid};DB_CLOSE_DELAY=-1;MODE=MySQL")
@Transactional
@ActiveProfiles("test")
public class BackupServiceTests {

	@Autowired
	private BackupService backupService;

	@Autowired
	private UserService userService;

	private User createUser(String username, RoleType role) throws DuplicateInstanceException {

		User user = new User(username, "password", "name", "surname", "locality", 664567076,
				username + "@" + username, LocalDateTime.now(), role);
		userService.signUp(user);
		return user;
	}

	@Test
	public void testExportarCopiaDeSeguridadComoAdmin(@TempDir File carpeta) throws DuplicateInstanceException,
			InstanceNotFoundException, NotAuthorizedUserException, BackupNotAvailableException, BackupFailedException {

		User admin = createUser("BackupAdmin", RoleType.ADMIN);
		File destino = new File(carpeta, "copia.zip");

		backupService.exportarCopiaDeSeguridad(admin.getId(), destino.getAbsolutePath());

		// BACKUP TO genera un .zip real con el contenido de la base: comprobar que
		// existe y que pesa algo es suficiente para saber que el comando corrió de
		// verdad y no que simplemente no lanzó una excepción.
		assertTrue(destino.exists());
		assertTrue(destino.length() > 0);
	}

	@Test
	public void testExportarCopiaDeSeguridadRechazaCliente(@TempDir File carpeta) throws DuplicateInstanceException {

		User cliente = createUser("BackupCliente", RoleType.CUSTOMER);
		File destino = new File(carpeta, "copia.zip");

		assertThrows(NotAuthorizedUserException.class,
				() -> backupService.exportarCopiaDeSeguridad(cliente.getId(), destino.getAbsolutePath()));
	}

	@Test
	public void testExportarCopiaDeSeguridadUsuarioInexistente(@TempDir File carpeta) {

		File destino = new File(carpeta, "copia.zip");

		assertThrows(InstanceNotFoundException.class,
				() -> backupService.exportarCopiaDeSeguridad(-1L, destino.getAbsolutePath()));
	}

	/** El fichero de base de datos temporal que se crea en {@code java.io.tmpdir} no lo limpia nadie más. */
	@AfterAll
	public static void limpiarBaseDeDatosTemporal() {

		File tmp = new File(System.getProperty("java.io.tmpdir"));
		File[] restos = tmp.listFiles((dir, nombre) -> nombre.startsWith("actihome-backup-test-"));

		if (restos != null) {
			for (File resto : restos) {
				resto.delete();
			}
		}
	}
}
