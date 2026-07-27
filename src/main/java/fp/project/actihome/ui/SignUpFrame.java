package fp.project.actihome.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Chip;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Alta de usuario.
 *
 * <p>
 * Ocho campos en <b>dos columnas</b>, según el handoff. Es la decisión de
 * maquetación que hace que la pantalla quepa sin scroll: en una sola columna,
 * ocho campos más el rol más el botón se irían por debajo del borde inferior, y
 * la regla de escritorio del proyecto dice que cada pantalla cabe en su
 * ventana.
 *
 * <p>
 * El rol se elige con dos chips en vez de con un desplegable. Un
 * {@code JComboBox} con dos opciones esconde la mitad de la información hasta
 * que se despliega; dos chips enseñan las dos alternativas a la vez y se
 * seleccionan con un solo clic en lugar de dos.
 */
@Component
@Profile("!test")
public class SignUpFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final UserService userService;
	private final Navigator navigator;

	private Field usuario;
	private Field contrasena;
	private Field nombre;
	private Field apellido;
	private Field localidad;
	private Field telefono;
	private Field correo;
	private JSpinner nacimiento;
	private Chip rolCliente;
	private Chip rolAdmin;
	private JLabel error;

	public SignUpFrame(UserService userService, Navigator navigator) {

		this.userService = userService;
		this.navigator = navigator;

		initUI();
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			limpiar();
			SwingUtilities.invokeLater(usuario::requestFocus);
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");

		// El alto sale de medir el contenido, no de un número redondo: cabecera, ocho
		// campos en dos columnas, la fila de rol y el pie. Con 680 el pie se quedaba
		// fuera de la ventana, que es justo lo que la regla de escritorio prohíbe.
		setSize(980, 870);
		setMinimumSize(new Dimension(900, 840));
		setLocationRelativeTo(null);

		// Tres filas: cabecera, campos y pie. El "push" antes del pie lo empuja abajo,
		// así el botón principal queda siempre anclado al mismo sitio aunque la ventana
		// se agrande.
		// Ojo con la sintaxis: "push" es una separación que crece, y no admite llevar
		// pegado un número. Concatenar Space.LG detrás produjo "push20", que MigLayout
		// rechaza en tiempo de ejecución (las restricciones son cadenas: el compilador
		// no las revisa, el fallo aparece al construir la ventana).
		JPanel raiz = new JPanel(new MigLayout("wrap 1, fill, " + Space.insets(Space.XXXL, Space.GIANT, Space.XXL,
				Space.GIANT), "[grow,fill]", "[]" + Space.XL + "[]push[]"));
		raiz.setBackground(Theme.bg());

		raiz.add(cabecera(), "growx");
		raiz.add(campos(), "growx");
		raiz.add(pie(), "growx");

		setContentPane(raiz);
	}

	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);
		titulos.add(Labels.capsAccent("Crear cuenta"));
		titulos.add(Labels.title("Empieza a viajar con ActiHome"), "gaptop " + Space.XXS);
		titulos.add(Labels.muted("Necesitamos unos pocos datos. Podrás cambiarlos después desde tu perfil."),
				"gaptop " + Space.XS);
		panel.add(titulos);

		panel.add(new MascotSlot(MascotSlot.Tamano.MEDIANO), "top");

		return panel;
	}

	private JPanel campos() {

		// gapy separa las filas. Sin él, cada etiqueta quedaba más cerca del campo de
		// arriba que del suyo propio, y eso invierte la agrupación: el ojo agrupa por
		// proximidad, así que una etiqueta pegada al campo anterior parece pertenecerle
		// a él. Es un detalle de dos píxeles que cambia cómo se lee el formulario.
		JPanel panel = new JPanel(new MigLayout("wrap 2, gapy " + Space.XL + ", " + Space.insets(0),
				"[grow,fill]" + Space.XXL + "[grow,fill]", ""));
		panel.setOpaque(false);

		usuario = Field.text("Nombre de usuario");
		contrasena = Field.password("Contraseña");
		nombre = Field.text("Nombre");
		apellido = Field.text("Apellido");
		localidad = Field.text("Localidad");
		telefono = Field.text("Teléfono");
		correo = Field.text("Correo electrónico");

		panel.add(usuario);
		panel.add(contrasena);
		panel.add(nombre);
		panel.add(apellido);
		panel.add(localidad);
		panel.add(telefono);
		panel.add(correo);
		panel.add(fechaDeNacimiento());

		panel.add(rol(), "span 2, growx, gaptop " + Space.SM);

		return panel;
	}

	private JPanel fechaDeNacimiento() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panel.setOpaque(false);

		nacimiento = new JSpinner(new SpinnerDateModel());
		nacimiento.setEditor(new JSpinner.DateEditor(nacimiento, "dd/MM/yyyy"));
		nacimiento.setFont(Typography.sans(Typography.BODY));

		// Arranca treinta años atrás en vez de en la fecha de hoy. Un campo de fecha de
		// nacimiento que empieza en hoy obliga a todo el mundo a retroceder décadas a
		// mano; empezar en un valor plausible ahorra ese trabajo a casi todos.
		nacimiento.setValue(Date.from(LocalDate.now().minusYears(30).atStartOfDay(ZoneId.systemDefault()).toInstant()));

		panel.add(Labels.caps("Fecha de nacimiento"));
		panel.add(nacimiento, "gaptop " + Space.XXS + ", height 38!");

		return panel;
	}

	private JPanel rol() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.MD + "[]" + Space.SM + "[]push[]", ""));
		panel.setOpaque(false);

		panel.add(Labels.caps("Quiero"));

		rolCliente = new Chip("Reservar alojamientos", true);
		rolAdmin = new Chip("Publicar alojamientos");

		// Selección única hecha a mano en lugar de con un ButtonGroup: el grupo de
		// Swing permite quedarse sin ninguno seleccionado al pulsar el activo, y aquí
		// siempre tiene que haber un rol elegido.
		rolCliente.addActionListener(e -> seleccionarRol(true));
		rolAdmin.addActionListener(e -> seleccionarRol(false));

		panel.add(rolCliente);
		panel.add(rolAdmin);

		panel.add(Labels.muted("Podrás publicar alojamientos si eliges la segunda opción."));

		return panel;
	}

	private void seleccionarRol(boolean cliente) {

		rolCliente.setSelected(cliente);
		rolAdmin.setSelected(!cliente);
	}

	private JPanel pie() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panel.setOpaque(false);

		error = Labels.error(" ");
		panel.add(error);

		JPanel acciones = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]push[]", ""));
		acciones.setOpaque(false);
		acciones.add(Buttons.primary("Crear cuenta", e -> registrar()), "height 44!");
		acciones.add(Buttons.secondary("Cancelar", e -> navigator.ir(LoginFrame.class)), "height 44!");
		acciones.add(enlaceALogin());

		panel.add(acciones, "gaptop " + Space.XS);

		return panel;
	}

	private JPanel enlaceALogin() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XXS + "[]", ""));
		fila.setOpaque(false);

		fila.add(Labels.muted("¿Ya tienes cuenta?"));

		JLabel enlace = Labels.body("Inicia sesión");
		enlace.setFont(Typography.sansSemiBold(Typography.BODY_SM));
		enlace.setForeground(Theme.acc());
		enlace.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		enlace.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				navigator.ir(LoginFrame.class);
			}
		});
		fila.add(enlace);

		return fila;
	}

	private void limpiar() {

		usuario.setText("");
		contrasena.setText("");
		nombre.setText("");
		apellido.setText("");
		localidad.setText("");
		telefono.setText("");
		correo.setText("");
		seleccionarRol(true);
		error.setText(" ");
	}

	private void registrar() {

		String faltante = primerCampoVacio();

		if (faltante != null) {
			error.setText("Falta rellenar: " + faltante + ".");
			return;
		}

		int numero;
		try {
			numero = Integer.parseInt(telefono.getText().trim());
		} catch (NumberFormatException ex) {
			// Antes esto reventaba con una excepción sin capturar y la ventana se
			// quedaba muerta sin decir nada.
			error.setText("El teléfono debe ser un número, sin espacios ni guiones.");
			telefono.requestFocus();
			return;
		}

		try {
			User nuevo = new User(usuario.getText().trim(), contrasena.getText(), nombre.getText().trim(),
					apellido.getText().trim(), localidad.getText().trim(), numero, correo.getText().trim(),
					fechaSeleccionada(), rolAdmin.isSelected() ? RoleType.ADMIN : RoleType.CUSTOMER);

			userService.signUp(nuevo);
			navigator.ir(LoginFrame.class);

		} catch (DuplicateInstanceException ex) {
			error.setText("Ese nombre de usuario ya está cogido. Prueba con otro.");
			usuario.requestFocus();

		} catch (DateTimeParseException ex) {
			error.setText("La fecha de nacimiento no es válida.");
		}
	}

	/** Devuelve el nombre del primer campo obligatorio sin rellenar, o null. */
	private String primerCampoVacio() {

		if (usuario.getText().trim().isEmpty()) {
			return "el nombre de usuario";
		}
		if (contrasena.getText().isEmpty()) {
			return "la contraseña";
		}
		if (nombre.getText().trim().isEmpty()) {
			return "el nombre";
		}
		if (apellido.getText().trim().isEmpty()) {
			return "el apellido";
		}
		if (localidad.getText().trim().isEmpty()) {
			return "la localidad";
		}
		if (telefono.getText().trim().isEmpty()) {
			return "el teléfono";
		}
		if (correo.getText().trim().isEmpty()) {
			return "el correo electrónico";
		}

		return null;
	}

	private LocalDateTime fechaSeleccionada() {

		Date valor = (Date) nacimiento.getValue();
		LocalDate fecha = valor.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		return fecha.atStartOfDay();
	}
}
