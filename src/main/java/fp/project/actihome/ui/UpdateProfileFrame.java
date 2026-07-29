package fp.project.actihome.ui;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;

/**
 * Editar los datos de la cuenta.
 *
 * <p>
 * <b>Tenía dos problemas de fondo, y ninguno era de maquetación.</b>
 *
 * <p>
 * El primero: <b>era un callejón sin salida</b>. No tenía cabecera ni botón de
 * cancelar, y solo navegaba a otro sitio cuando el guardado salía bien. Si
 * entrabas por curiosidad y no querías cambiar nada, la única salida era cerrar
 * la aplicación. Se arregla con las dos piezas que ya usa el resto: el
 * {@link HeaderPanel}, que siempre ofrece volver al catálogo, y un "Cancelar"
 * explícito. La regla que sale de aquí: <b>una pantalla nunca puede depender de
 * que la operación salga bien para poder abandonarla</b>.
 *
 * <p>
 * El segundo: <b>el teléfono no se precargaba</b> —la línea que lo hacía estaba
 * comentada— así que el campo aparecía vacío. Al guardar,
 * {@code Integer.parseInt("")} lanzaba {@code NumberFormatException}, que caía
 * en el {@code catch (Exception)} genérico y enseñaba "El nombre de usuario ya
 * existe": un mensaje que no tenía nada que ver, sobre un campo que el usuario
 * ni había tocado. Es la misma familia del bug B9 —formulario de edición que no
 * carga el estado real— agravada por el diagnóstico equivocado que provoca
 * capturar todas las excepciones juntas.
 */
@Component
@Profile("!test")
@Lazy
public class UpdateProfileFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final transient UserService userService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private Field usuario;
	private Field nombre;
	private Field apellido;
	private Field localidad;
	private Field telefono;
	private Field correo;
	private JLabel error;

	public UpdateProfileFrame(UserService userService, SessionManager sessionManager, Navigator navigator,
			HeaderPanel headerPanel) {

		this.userService = userService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			headerPanel.refresh();
			precargar();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(820, 760);
		setMinimumSize(new Dimension(700, 680));
		setLocationRelativeTo(null);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[grow,fill]"));

		JPanel exterior = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.GIANT), "[grow]", "[grow]"));
		exterior.setOpaque(false);
		exterior.add(formulario(), Layout.ancho(Layout.FORMULARIO) + ", aligny center, alignx center");

		raiz.add(headerPanel, "growx");
		raiz.add(exterior, "grow");

		setContentPane(raiz);
	}

	private JPanel formulario() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.XXL + "[]" + Space.MD + "[]" + Space.MD + "[]" + Space.MD + "[]" + Space.LG + "[]"
						+ Space.LG + "[]"));
		panel.setOpaque(false);

		panel.add(cabecera());

		usuario = Field.text("Nombre de usuario");
		nombre = Field.text("Nombre");
		apellido = Field.text("Apellido");
		localidad = Field.text("Localidad");
		telefono = Field.text("Teléfono");
		correo = Field.text("Correo electrónico");

		// Los campos cortos van de dos en dos. Con los seis a ancho completo el
		// formulario no cabía en la ventana y los botones quedaban bajo el pliegue, que
		// es justo lo que la regla de escritorio del proyecto no permite. Emparejarlos
		// además agrupa lo que se lee junto: nombre con apellido, dónde vives con cómo
		// localizarte.
		panel.add(usuario);
		panel.add(dosColumnas(nombre, apellido));
		panel.add(dosColumnas(localidad, telefono));
		panel.add(correo);

		error = Labels.error(" ");
		panel.add(error);

		panel.add(acciones());

		return panel;
	}

	private JPanel dosColumnas(Field izquierda, Field derecha) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]" + Space.MD + "[grow,fill]", ""));
		fila.setOpaque(false);
		fila.add(izquierda);
		fila.add(derecha);

		return fila;
	}

	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);
		titulos.add(Labels.capsAccent("Tu cuenta"));
		titulos.add(Labels.title("Editar perfil"), "gaptop " + Space.XXS);

		panel.add(titulos);
		panel.add(new MascotSlot(MascotSlot.Tamano.PEQUENO, Pose.BIENVENIDA), "top, w 56!, h 56!");

		return panel;
	}

	private JPanel acciones() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]", ""));
		fila.setOpaque(false);

		fila.add(Buttons.primary("Guardar cambios", e -> guardar()), "height 44!");
		fila.add(Buttons.link("Cancelar", e -> navigator.ir(ShowHousingsFrame.class)));

		return fila;
	}

	/** Vuelca en el formulario los datos que hay ahora mismo en la sesión. */
	private void precargar() {

		User actual = sessionManager.getLoggedInUser();

		if (actual == null) {
			navigator.ir(LoginFrame.class);
			return;
		}

		usuario.setText(actual.getUsername());
		nombre.setText(actual.getName());
		apellido.setText(actual.getSurname());
		localidad.setText(actual.getLocality());
		// El campo que faltaba: sin esto se guardaba con el teléfono vacío.
		telefono.setText(String.valueOf(actual.getPhoneNumber()));
		correo.setText(actual.getEmail());

		error.setText(" ");
	}

	private void guardar() {

		if (usuario.getText().trim().isEmpty()) {
			error.setText("El nombre de usuario no puede quedar vacío.");
			return;
		}

		int numeroDeTelefono;

		try {
			// Se valida aquí y no se deja caer en el catch de abajo: un teléfono mal
			// escrito no es un fallo del servicio, y mezclarlo con las excepciones de
			// negocio es lo que producía el mensaje equivocado.
			numeroDeTelefono = Integer.parseInt(telefono.getText().trim());

		} catch (NumberFormatException ex) {
			error.setText("El teléfono debe ser un número, sin espacios ni prefijo.");
			return;
		}

		try {
			User actualizado = userService.updateProfile(sessionManager.getLoggedInUser().getId(),
					usuario.getText().trim(), nombre.getText().trim(), apellido.getText().trim(),
					correo.getText().trim(), numeroDeTelefono, localidad.getText().trim());

			// La sesión guarda el User en memoria: sin esto, la cabecera seguiría
			// enseñando el nombre viejo hasta volver a entrar.
			sessionManager.setLoggedInUser(actualizado);

			navigator.ir(ShowHousingsFrame.class);

		} catch (DuplicateInstanceException ex) {
			error.setText("Ya hay alguien con ese nombre de usuario. Prueba con otro.");

		} catch (InstanceNotFoundException ex) {
			error.setText("Tu sesión ya no es válida. Vuelve a entrar.");
		}
	}
}
