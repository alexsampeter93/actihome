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

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Chip;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.FilaFluida;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.Rescate;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
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
 *
 * <p>
 * <b>{@code @Lazy}, y con esto se cierra B10.</b> Junto a {@code LoginFrame},
 * era de las últimas dos pantallas —de diecisiete— sin esta anotación: ambas
 * son de la Fase 2, anterior a que {@code @Lazy} se asentara como convención.
 * Sin ella, Spring construía sus widgets Swing en el hilo principal durante el
 * arranque del contexto, en vez de en el EDT. Ver la nota gemela en
 * {@code LoginFrame}.
 */
@Component
@Profile("!test")
@Lazy
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

	private JLabel superTitulo;
	private JLabel titulo;
	private JLabel subtitulo;
	private JLabel etiquetaNacimiento;
	private JLabel etiquetaQuiero;
	private JLabel textoRolInfo;
	private JButton botonCrearCuenta;
	private JButton botonCancelar;
	private JLabel etiquetaYaTienesCuenta;
	private JLabel enlaceIniciarSesion;

	public SignUpFrame(UserService userService, Navigator navigator) {

		this.userService = userService;
		this.navigator = navigator;

		initUI();
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			actualizarTextosFijos();
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
		setLocationRelativeTo(null);

		// Tres filas: cabecera, campos y pie. El "push" antes del pie lo empuja abajo,
		// así el botón principal queda siempre anclado al mismo sitio aunque la ventana
		// se agrande.
		// Ojo con la sintaxis: "push" es una separación que crece, y no admite llevar
		// pegado un número. Concatenar Space.LG detrás produjo "push20", que MigLayout
		// rechaza en tiempo de ejecución (las restricciones son cadenas: el compilador
		// no las revisa, el fallo aparece al construir la ventana).
		// El contenido se limita a 940px y se centra. Al maximizar la ventana, sin ese
		// tope, los ocho campos se estiraban hasta más de mil píxeles cada uno: un
		// campo de texto tan largo para escribir un nombre se percibe como un error de
		// maquetación, y el ojo pierde la línea al recorrerlo.
		JPanel raiz = new Page(new MigLayout("fill, " + Space.insets(0), "[grow,fill]", "[grow,fill]"));

		JPanel cuerpo = new JPanel(new MigLayout("wrap 1, fill, " + Space.insets(Space.XXXL, Space.GIANT, Space.XXL,
				Space.GIANT), "[grow,fill]", "[]" + Space.XL + "[]push[]"));
		cuerpo.setOpaque(false);

		cuerpo.add(cabecera(), "growx, " + Layout.anchoCentrado(Layout.CONTENIDO));
		cuerpo.add(campos(), "growx, " + Layout.anchoCentrado(Layout.CONTENIDO));
		cuerpo.add(pie(), "growx, " + Layout.anchoCentrado(Layout.CONTENIDO));

		// El registro es el formulario más alto de la aplicación: ocho campos en dos
		// columnas, la fila de rol y el pie. En una ventana holgada cabe entero y esta
		// barra no llega a aparecer; en un portátil con el escalado al 150 % es lo que
		// impide que el botón "Crear cuenta" quede por debajo del borde inferior.
		raiz.add(Rescate.envolver(cuerpo), "grow");

		setContentPane(raiz);

		Foco.alPulsarEscape(this, () -> navigator.ir(LoginFrame.class));
	}

	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);
		superTitulo = Labels.capsAccent(Textos.t("registro.crearCuenta"));
		titulos.add(superTitulo);
		titulo = Labels.title(Textos.t("registro.titulo"));
		titulos.add(titulo, "gaptop " + Space.XXS);
		subtitulo = Labels.muted(Textos.t("registro.subtitulo"));
		titulos.add(subtitulo, "gaptop " + Space.XS);
		panel.add(titulos);

		panel.add(new MascotSlot(MascotSlot.Tamano.MEDIANO, Pose.BIENVENIDA), "top");

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

		usuario = Field.text(Textos.t("login.usuario"));
		contrasena = Field.password(Textos.t("login.contrasena"));
		nombre = Field.text(Textos.t("registro.nombre"));
		apellido = Field.text(Textos.t("registro.apellido"));
		localidad = Field.text(Textos.t("registro.localidad"));
		telefono = Field.text(Textos.t("registro.telefono"));
		correo = Field.text(Textos.t("registro.correo"));

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

		etiquetaNacimiento = Labels.caps(Textos.t("registro.fechaNacimiento"));
		panel.add(etiquetaNacimiento);
		panel.add(nacimiento, "gaptop " + Space.XXS + ", height " + Typography.altoDeControl() + "!");

		return panel;
	}

	/**
	 * "Quiero" + los dos chips en una {@link FilaFluida}, y la frase de ayuda en
	 * su propia línea debajo.
	 *
	 * <p>
	 * Antes los cuatro compartían una sola fila rígida con la frase empujada al
	 * extremo derecho ({@code push}). En español cabía; en inglés
	 * ("You'll be able to list properties if you choose the second option.", más
	 * larga que su equivalente español) la fila entera no cabía y MigLayout, sin
	 * más sitio que ceder, aplastaba también la etiqueta "I want to" y los
	 * chips —el mismo síntoma, ya documentado en el proyecto, de una fila de
	 * ancho variable que no es {@link FilaFluida}, aquí destapado por un idioma
	 * más largo en vez de por el escalado de Windows—. Separando la frase en su
	 * propia línea, ninguna de las dos compite por el mismo ancho.
	 */
	private JPanel rol() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XS + "[]"));
		panel.setOpaque(false);

		etiquetaQuiero = Labels.caps(Textos.t("registro.quiero"));

		rolCliente = new Chip(Textos.t("registro.rol.cliente"), true);
		rolAdmin = new Chip(Textos.t("registro.rol.admin"));

		// Selección única hecha a mano en lugar de con un ButtonGroup: el grupo de
		// Swing permite quedarse sin ninguno seleccionado al pulsar el activo, y aquí
		// siempre tiene que haber un rol elegido.
		rolCliente.addActionListener(e -> seleccionarRol(true));
		rolAdmin.addActionListener(e -> seleccionarRol(false));

		FilaFluida fila = new FilaFluida(Space.MD, Space.XS);
		fila.add(etiquetaQuiero);
		fila.add(rolCliente);
		fila.add(rolAdmin);
		panel.add(fila);

		textoRolInfo = Labels.muted(Textos.t("registro.rol.info"));
		panel.add(textoRolInfo);

		return panel;
	}

	private void seleccionarRol(boolean cliente) {

		rolCliente.setSelected(cliente);
		rolAdmin.setSelected(!cliente);
	}

	/**
	 * Botones y enlace en dos líneas, no en una.
	 *
	 * <p>
	 * Antes compartían una sola fila rígida con el enlace empujado al extremo
	 * ({@code push}): "Create account" + "Cancel" + "Already have an account?
	 * Sign in" en inglés no cabían en el ancho que sí bastaba en español, y sin
	 * sitio que ceder MigLayout aplastaba los tres, botones incluidos —el
	 * "Create account" recortado no era el enlace, era el propio botón—. Los
	 * botones necesitan su alto fijo de 44px (la constante visual de toda la
	 * aplicación), así que aquí no vale {@link FilaFluida}, que mide por el alto
	 * natural de cada elemento: la solución es que el enlace baje a su propia
	 * línea en vez de competir por el mismo ancho.
	 */
	private JPanel pie() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panel.setOpaque(false);

		error = Labels.error(" ");
		panel.add(error);

		JPanel acciones = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]", ""));
		acciones.setOpaque(false);
		botonCrearCuenta = Buttons.primary(Textos.t("registro.crearCuenta"), e -> registrar());
		acciones.add(botonCrearCuenta, "height " + Typography.altoDeBoton() + "!");
		botonCancelar = Buttons.secondary(Textos.t("ajustes.cancelar"), e -> navigator.ir(LoginFrame.class));
		acciones.add(botonCancelar, "height " + Typography.altoDeBoton() + "!");

		panel.add(acciones, "gaptop " + Space.XS);
		panel.add(enlaceALogin(), "gaptop " + Space.SM);

		return panel;
	}

	private JPanel enlaceALogin() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XXS + "[]", ""));
		fila.setOpaque(false);

		etiquetaYaTienesCuenta = Labels.muted(Textos.t("registro.yaTienesCuenta"));
		fila.add(etiquetaYaTienesCuenta);

		JLabel enlace = Labels.body(Textos.t("registro.iniciaSesion"));
		enlace.setFont(Typography.sansSemiBold(Typography.BODY_SM));
		// accText(), no acc(): ver la nota gemela en LoginFrame.enlaceARegistro().
		enlace.setForeground(Theme.accText());
		enlace.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		enlace.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				navigator.ir(LoginFrame.class);
			}
		});
		fila.add(enlace);

		enlaceIniciarSesion = enlace;
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
			error.setText(Textos.t("registro.error.faltaRellenar", faltante));
			return;
		}

		int numero;
		try {
			numero = Integer.parseInt(telefono.getText().trim());
		} catch (NumberFormatException ex) {
			// Antes esto reventaba con una excepción sin capturar y la ventana se
			// quedaba muerta sin decir nada.
			error.setText(Textos.t("registro.error.telefonoInvalido"));
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
			error.setText(Textos.t("registro.error.usuarioOcupado"));
			usuario.requestFocus();

		} catch (DateTimeParseException ex) {
			error.setText(Textos.t("registro.error.fechaInvalida"));
		}
	}

	private void actualizarTextosFijos() {

		superTitulo.setText(Textos.t("registro.crearCuenta"));
		titulo.setText(Textos.t("registro.titulo"));
		subtitulo.setText(Textos.t("registro.subtitulo"));
		usuario.setEtiqueta(Textos.t("login.usuario"));
		contrasena.setEtiqueta(Textos.t("login.contrasena"));
		nombre.setEtiqueta(Textos.t("registro.nombre"));
		apellido.setEtiqueta(Textos.t("registro.apellido"));
		localidad.setEtiqueta(Textos.t("registro.localidad"));
		telefono.setEtiqueta(Textos.t("registro.telefono"));
		correo.setEtiqueta(Textos.t("registro.correo"));
		etiquetaNacimiento.setText(Textos.t("registro.fechaNacimiento"));
		etiquetaQuiero.setText(Textos.t("registro.quiero"));
		rolCliente.setText(Textos.t("registro.rol.cliente"));
		rolAdmin.setText(Textos.t("registro.rol.admin"));
		textoRolInfo.setText(Textos.t("registro.rol.info"));
		botonCrearCuenta.setText(Textos.t("registro.crearCuenta"));
		botonCancelar.setText(Textos.t("ajustes.cancelar"));
		etiquetaYaTienesCuenta.setText(Textos.t("registro.yaTienesCuenta"));
		enlaceIniciarSesion.setText(Textos.t("registro.iniciaSesion"));
	}

	/** Devuelve el nombre del primer campo obligatorio sin rellenar, o null. */
	private String primerCampoVacio() {

		if (usuario.getText().trim().isEmpty()) {
			return Textos.t("registro.campo.usuario");
		}
		if (contrasena.getText().isEmpty()) {
			return Textos.t("registro.campo.contrasena");
		}
		if (nombre.getText().trim().isEmpty()) {
			return Textos.t("registro.campo.nombre");
		}
		if (apellido.getText().trim().isEmpty()) {
			return Textos.t("registro.campo.apellido");
		}
		if (localidad.getText().trim().isEmpty()) {
			return Textos.t("registro.campo.localidad");
		}
		if (telefono.getText().trim().isEmpty()) {
			return Textos.t("registro.campo.telefono");
		}
		if (correo.getText().trim().isEmpty()) {
			return Textos.t("registro.campo.correo");
		}

		return null;
	}

	private LocalDateTime fechaSeleccionada() {

		Date valor = (Date) nacimiento.getValue();
		LocalDate fecha = valor.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		return fecha.atStartOfDay();
	}
}
