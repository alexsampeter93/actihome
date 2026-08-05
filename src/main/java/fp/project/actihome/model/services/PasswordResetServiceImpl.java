package fp.project.actihome.model.services;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.PasswordResetCode;
import fp.project.actihome.model.entities.PasswordResetCodeDao;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.entities.UserDao;
import fp.project.actihome.model.exceptions.EmailFailedException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.InvalidResetCodeException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;

@Service
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

	/**
	 * Alfabeto del código, <b>sin caracteres que se confundan al leerlos</b>: no
	 * hay O ni 0, ni I ni 1, ni L. Suena a detalle menor y no lo es — este código
	 * se dicta por teléfono o se copia de un correo, y una O confundida con un cero
	 * gasta uno de los cinco intentos por un fallo que no es del usuario.
	 */
	private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

	/**
	 * Ocho caracteres de ese alfabeto son 31^8, unas 850.000 millones de
	 * combinaciones. Con cinco intentos permitidos, adivinarlo no es una vía.
	 */
	private static final int LONGITUD = 8;

	private static final int MINUTOS_DE_VIDA = 15;

	private static final int INTENTOS_MAXIMOS = 5;

	/**
	 * {@code SecureRandom} y no {@code Random}. La diferencia no es de calidad
	 * estadística: {@code Random} es predecible: quien vea unos cuantos valores
	 * puede calcular su estado interno y, con él, los siguientes. Para un código
	 * que abre una cuenta, eso lo invalida por completo.
	 */
	private final SecureRandom aleatorio = new SecureRandom();

	@Autowired
	private UserDao userDao;

	@Autowired
	private PasswordResetCodeDao resetCodeDao;

	@Autowired
	private EmailSender emailSender;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Override
	public boolean puedeEnviarCorreo() {
		return emailSender.estaConfigurado();
	}

	/**
	 * {@code NOT_SUPPORTED} por lo mismo que la traducción (Fase 8.5): dentro hay
	 * una conexión a un servidor de correo, y no se mantiene abierta una
	 * transacción a través de una llamada a un sistema externo.
	 *
	 * <p>
	 * Aquí sí hay escritura que proteger —el código generado—, así que se guarda
	 * <b>antes</b>, en su propia transacción ({@link #crearCodigo}), y el envío
	 * ocurre después y fuera. El orden importa: si se enviara primero, un fallo al
	 * guardar dejaría al usuario con un código en el correo que la base no conoce.
	 * Al revés, un fallo de envío deja un código guardado que nadie usará y que
	 * caduca solo en quince minutos.
	 */
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void solicitarPorCorreo(String username) {

		Optional<User> usuario = userDao.findByUsername(username);

		// Silencio deliberado si no existe: ver el javadoc de la interfaz. Tampoco se
		// registra nada distinto, porque una traza también es una filtración para
		// quien pueda leerla.
		if (!usuario.isPresent()) {
			return;
		}

		String codigo = crearCodigo(usuario.get().getId());

		try {
			emailSender.enviar(usuario.get().getEmail(), "ActiHome - código de recuperación",
					"Tu código para cambiar la contraseña es:\n\n    " + codigo + "\n\n"
							+ "Caduca en " + MINUTOS_DE_VIDA + " minutos y solo se puede usar una vez.\n"
							+ "Si no has pedido cambiarla, ignora este mensaje: tu contraseña no ha cambiado.");

		} catch (EmailFailedException ex) {
			// No se propaga, y es coherente con no revelar si el usuario existe: si el
			// fallo de envío se contara, "ha fallado el envío" significaría "esa cuenta
			// existe y tiene correo", que es justo lo que se está protegiendo.
			// El código queda guardado y caduca solo.
		}
	}

	@Override
	public String generarCodigoParaEntregar(String username, Long adminId)
			throws InstanceNotFoundException, NotAuthorizedUserException {

		Optional<User> administrador = userDao.findById(adminId);

		if (!administrador.isPresent()) {
			throw new InstanceNotFoundException("project.entities.user", adminId);
		}

		if (administrador.get().getRole() != RoleType.ADMIN) {
			throw new NotAuthorizedUserException();
		}

		Optional<User> usuario = userDao.findByUsername(username);

		if (!usuario.isPresent()) {
			throw new InstanceNotFoundException("project.entities.user", username);
		}

		return crearCodigo(usuario.get().getId());
	}

	@Override
	public void restablecer(String username, String codigo, String nuevaContrasena) throws InvalidResetCodeException {

		Optional<User> usuario = userDao.findByUsername(username);

		if (!usuario.isPresent()) {
			throw new InvalidResetCodeException();
		}

		PasswordResetCode valido = buscarCodigoValido(usuario.get().getId(), codigo);

		valido.setUsedAt(LocalDateTime.now());
		usuario.get().setPassword(passwordEncoder.encode(nuevaContrasena));

		// Los demás códigos vivos de esta cuenta se anulan también. Si alguien pidió
		// tres porque el correo tardaba, los otros dos no deben seguir abriendo la
		// puerta después de haber cambiado ya la contraseña.
		for (PasswordResetCode otro : resetCodeDao.findByUserIdAndUsedAtIsNullOrderByExpiresAtDesc(usuario.get().getId())) {
			otro.setUsedAt(LocalDateTime.now());
		}
	}

	/**
	 * Recorre los códigos vivos buscando el que encaje, gastando un intento en cada
	 * uno.
	 *
	 * <p>
	 * Hay que comparar contra todos porque están hasheados: no se puede buscar "el
	 * código X" en la base, solo comprobar uno a uno si el hash corresponde. Es el
	 * mismo motivo por el que el login no puede buscar por contraseña.
	 */
	private PasswordResetCode buscarCodigoValido(Long userId, String codigo) throws InvalidResetCodeException {

		LocalDateTime ahora = LocalDateTime.now();

		for (PasswordResetCode candidato : resetCodeDao.findByUserIdAndUsedAtIsNullOrderByExpiresAtDesc(userId)) {

			if (candidato.getExpiresAt().isBefore(ahora) || candidato.getAttempts() >= INTENTOS_MAXIMOS) {
				continue;
			}

			// El intento se cuenta ANTES de saber si acierta. Contarlo solo al fallar
			// parece equivalente y no lo es: si el proceso se interrumpe justo después de
			// comparar, el intento se habría perdido y el límite dejaría de ser un límite.
			candidato.setAttempts(candidato.getAttempts() + 1);

			if (passwordEncoder.matches(codigo, candidato.getCodeHash())) {
				return candidato;
			}
		}

		throw new InvalidResetCodeException();
	}

	/**
	 * Genera un código nuevo, lo guarda hasheado y <b>anula los anteriores</b>.
	 *
	 * <p>
	 * Anularlos es lo que hace que "pedir otro código" signifique lo que el usuario
	 * cree que significa. Sin esto, pedir tres seguidos porque el correo tarda
	 * dejaría tres llaves activas a la vez durante quince minutos.
	 *
	 * <p>
	 * <b>Guarda con {@code save} explícito en lugar de confiar en el dirty
	 * checking</b>, y esa es la parte que hay que entender. Este método se llama
	 * desde dos sitios muy distintos: {@code generarCodigoParaEntregar}, que sí
	 * corre dentro de la transacción de clase, y {@code solicitarPorCorreo}, que es
	 * {@code NOT_SUPPORTED} y por tanto no tiene ninguna. Sin transacción no hay
	 * contexto de persistencia que detecte los cambios, así que mutar las entidades
	 * y confiar en que alguien las escriba <b>no guardaría nada</b> — en silencio.
	 *
	 * <p>
	 * Llevaba además un {@code @Transactional(REQUIRES_NEW)} que se ha retirado
	 * porque <b>no hacía nada</b>: las transacciones de Spring funcionan con un
	 * proxy alrededor del bean, y una llamada de un método a otro <em>del mismo
	 * objeto</em> no pasa por ese proxy. Una anotación que no se aplica es peor que
	 * ninguna, porque quien la lee da por hecha una garantía que no existe.
	 */
	private String crearCodigo(Long userId) {

		ArrayList<PasswordResetCode> anteriores = resetCodeDao
				.findByUserIdAndUsedAtIsNullOrderByExpiresAtDesc(userId);

		for (PasswordResetCode anterior : anteriores) {
			anterior.setUsedAt(LocalDateTime.now());
		}

		resetCodeDao.saveAll(anteriores);

		StringBuilder codigo = new StringBuilder(LONGITUD);

		for (int i = 0; i < LONGITUD; i++) {
			codigo.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
		}

		User usuario = userDao.findById(userId).orElseThrow(IllegalStateException::new);

		// El código se guarda hasheado con el mismo BCrypt que las contraseñas, así
		// que a partir de aquí ni la aplicación puede volver a leerlo. Por eso se
		// devuelve en claro ahora: es la única oportunidad de entregárselo a alguien.
		resetCodeDao.save(new PasswordResetCode(usuario, passwordEncoder.encode(codigo.toString()),
				LocalDateTime.now().plusMinutes(MINUTOS_DE_VIDA)));

		return codigo.toString();
	}
}
