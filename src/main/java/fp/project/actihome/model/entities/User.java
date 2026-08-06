package fp.project.actihome.model.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "USERS")
public class User {

	public enum RoleType {
		ADMIN, CUSTOMER;
	}

	/**
	 * Idioma de la interfaz. Vive aquí, no en {@code ui.theme}, porque el modelo
	 * no puede depender de la capa de UI (regla de capas de CLAUDE.md): esta
	 * clase solo necesita saber que hay dos idiomas posibles, no cómo se pintan.
	 */
	public enum Idioma {
		ES, EN;
	}

	/**
	 * Estación preferida al iniciar sesión. Deliberadamente distinto de
	 * {@code ui.theme.Season} por la misma razón que {@link Idioma}: son los
	 * mismos cuatro nombres, pero en dos capas que no pueden depender la una de
	 * la otra. La conversión entre ambos (los nombres coinciden) vive en la UI.
	 */
	public enum EstacionPreferida {
		PRIMAVERA, VERANO, OTONO, INVIERNO;
	}

	private Long id;

	private String username;

	private String password;

	private String name;

	private String surname;

	private String locality;

	private int phoneNumber;

	private String email;

	private LocalDateTime birthDate;

	private RoleType role;

	/**
	 * Preferencias de Ajustes (Fase 7.6). El valor por defecto va en el campo, no
	 * en un constructor, para que ninguna de las llamadas ya existentes a los dos
	 * constructores de abajo tenga que cambiar: cualquier {@code User} nuevo
	 * arranca exactamente con el comportamiento de siempre (estación real por
	 * fecha, partículas activas, español) hasta que alguien pase por Ajustes.
	 */
	private EstacionPreferida defaultSeason;

	private boolean particlesEnabled = true;

	private Idioma language = Idioma.ES;

	/**
	 * Si ya ha visto la pantalla de bienvenida (Fase 7.8). Por la misma razón que
	 * {@code particlesEnabled}: el valor por defecto va en el campo, no en un
	 * constructor, así que cualquier {@code User} nuevo —recién registrado o ya
	 * existente en una base creada antes de esta fase— empieza en {@code false} y
	 * ve la bienvenida exactamente una vez.
	 */
	private boolean onboardingSeen = false;

	/**
	 * Vista con la que arranca el catálogo la primera vez que se abre en una
	 * sesión (Fase 7.11): {@code false} lista, {@code true} cuadrícula. Solo la
	 * primera vez — si la persona cambia de vista mientras usa la aplicación, ese
	 * cambio manda hasta que se cierre la aplicación o inicie sesión otra cuenta.
	 */
	private boolean defaultGridView = false;

	public User() {

	}

	public User(Long id, String username, String password, String name, String surname, String locality,
			int phoneNumber, String email, LocalDateTime birthDate, RoleType role) {
		super();
		this.id = id;
		this.username = username;
		this.password = password;
		this.name = name;
		this.surname = surname;
		this.locality = locality;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.birthDate = birthDate;
		this.role = role;
	}

	public User(String username, String password, String name, String surname, String locality, int phoneNumber,
			String email, LocalDateTime birthDate, RoleType role) {
		super();
		this.username = username;
		this.password = password;
		this.name = name;
		this.surname = surname;
		this.locality = locality;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.birthDate = birthDate;
		this.role = role;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getLocality() {
		return locality;
	}

	public void setLocality(String locality) {
		this.locality = locality;
	}

	public int getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(int phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public LocalDateTime getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(LocalDateTime birthDate) {
		this.birthDate = birthDate;
	}

	/**
	 * El rol se persiste como <b>texto</b> ("ADMIN", "CUSTOMER").
	 *
	 * <p>
	 * Sin esta anotación, JPA guarda los enums por su <em>número de orden</em>: 0
	 * para el primero, 1 para el segundo. Eso significa que reordenar la
	 * declaración del enum, o insertar un valor nuevo en medio, convierte
	 * silenciosamente a todos los administradores en clientes. La base de datos
	 * seguiría siendo válida y nadie se enteraría hasta que fuera grave.
	 *
	 * <p>
	 * Guardar texto ocupa unos bytes más y a cambio hace que el dato sea legible y
	 * a prueba de refactorizaciones del código.
	 */
	@Enumerated(EnumType.STRING)
	public RoleType getRole() {
		return role;
	}

	public void setRole(RoleType role) {
		this.role = role;
	}

	@Enumerated(EnumType.STRING)
	public EstacionPreferida getDefaultSeason() {
		return defaultSeason;
	}

	public void setDefaultSeason(EstacionPreferida defaultSeason) {
		this.defaultSeason = defaultSeason;
	}

	public boolean isParticlesEnabled() {
		return particlesEnabled;
	}

	public void setParticlesEnabled(boolean particlesEnabled) {
		this.particlesEnabled = particlesEnabled;
	}

	@Enumerated(EnumType.STRING)
	public Idioma getLanguage() {
		return language;
	}

	public void setLanguage(Idioma language) {
		this.language = language;
	}

	public boolean isOnboardingSeen() {
		return onboardingSeen;
	}

	public void setOnboardingSeen(boolean onboardingSeen) {
		this.onboardingSeen = onboardingSeen;
	}

	public boolean isDefaultGridView() {
		return defaultGridView;
	}

	public void setDefaultGridView(boolean defaultGridView) {
		this.defaultGridView = defaultGridView;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", username=" + username + ", name=" + name + ", surname=" + surname + ", locality="
				+ locality + ", phoneNumber=" + phoneNumber + ", email=" + email + ", birthDate=" + birthDate
				+ ", role=" + role + ", defaultSeason=" + defaultSeason + ", particlesEnabled=" + particlesEnabled
				+ ", language=" + language + ", onboardingSeen=" + onboardingSeen + ", defaultGridView="
				+ defaultGridView + "]";
	}

}
