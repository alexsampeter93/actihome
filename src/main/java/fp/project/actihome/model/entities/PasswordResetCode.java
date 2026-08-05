package fp.project.actihome.model.entities;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Un código de un solo uso para recuperar una contraseña (Fase 8.6).
 *
 * <p>
 * <b>El código se guarda hasheado, no en claro</b>, por el mismo motivo que las
 * contraseñas: quien pueda leer esta tabla —una copia de seguridad, el fichero
 * de la base de datos— no debe poder usar lo que lee para entrar en ninguna
 * cuenta. Es un detalle que se salta constantemente en los tutoriales, con el
 * argumento de que "solo dura quince minutos"; quince minutos es tiempo de
 * sobra.
 *
 * <p>
 * <b>Tres defensas, y las tres hacen falta:</b>
 * <ul>
 * <li><b>Caduca.</b> Un código eterno es una segunda contraseña que el usuario
 * no sabe que tiene.</li>
 * <li><b>Se usa una vez.</b> Una vez cambiada la contraseña deja de valer,
 * aunque no haya caducado — si no, quien viera el correo días después seguiría
 * pudiendo entrar.</li>
 * <li><b>Cuenta los intentos.</b> Un código corto sin límite no protege de nada:
 * se prueban todos en segundos. Con cinco intentos, adivinarlo deja de ser
 * viable sin tener que alargarlo hasta hacerlo incómodo de teclear.</li>
 * </ul>
 */
@Entity
@Table(name = "PASSWORD_RESET_CODES")
public class PasswordResetCode {

	private Long id;

	private User user;

	private String codeHash;

	private LocalDateTime expiresAt;

	private int attempts;

	private LocalDateTime usedAt;

	/** Constructor sin argumentos, obligatorio para JPA. */
	public PasswordResetCode() {

	}

	public PasswordResetCode(User user, String codeHash, LocalDateTime expiresAt) {

		this.user = user;
		this.codeHash = codeHash;
		this.expiresAt = expiresAt;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "userId")
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getCodeHash() {
		return codeHash;
	}

	public void setCodeHash(String codeHash) {
		this.codeHash = codeHash;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(LocalDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}

	public LocalDateTime getUsedAt() {
		return usedAt;
	}

	public void setUsedAt(LocalDateTime usedAt) {
		this.usedAt = usedAt;
	}
}
