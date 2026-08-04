package fp.project.actihome.ui;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.WrongPasswordException;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.Rescate;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;

/**
 * Cambiar la contraseña.
 *
 * <p>
 * Tenía el mismo callejón sin salida que {@link UpdateProfileFrame}: sin
 * cabecera ni cancelar, solo se salía guardando con éxito. Ahora lleva las dos
 * cosas.
 *
 * <p>
 * <b>Y tenía un bucle que no era un bucle.</b> La comprobación de que la
 * repetición coincide estaba escrita como
 * {@code while (!newPassword.equals(repeated)) { ...; return; }}: un
 * {@code while} cuyo cuerpo termina siempre en {@code return} se ejecuta como
 * mucho una vez, así que era un {@code if} disfrazado. Funcionaba, pero decía
 * algo falso sobre la intención del código —invita a leer "esto se repite hasta
 * que acierte", que no es lo que pasa— y eso es exactamente lo que hace que
 * alguien lo cambie mal más adelante.
 */
@Component
@Profile("!test")
@Lazy
public class ChangePasswordFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final transient UserService userService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private Field actual;
	private Field nueva;
	private Field repetida;
	private JLabel error;
	private JLabel superTitulo;
	private JLabel titulo;
	private JButton botonCambiar;
	private JButton botonCancelar;

	public ChangePasswordFrame(UserService userService, SessionManager sessionManager, Navigator navigator,
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
			actualizarTextosFijos();
			limpiar();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(820, 700);
		setLocationRelativeTo(null);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[grow,fill]"));

		JPanel exterior = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.GIANT), "[grow]", "[grow]"));
		exterior.setOpaque(false);
		exterior.add(formulario(), Layout.ancho(Layout.FORMULARIO) + ", aligny center, alignx center");

		raiz.add(headerPanel, "growx");
		raiz.add(Rescate.envolver(exterior), "grow");

		setContentPane(raiz);

		Foco.alPulsarEscape(this, () -> navigator.ir(ShowHousingsFrame.class));
	}

	private JPanel formulario() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.XXL + "[]" + Space.MD + "[]" + Space.MD + "[]" + Space.LG + "[]" + Space.LG + "[]"));
		panel.setOpaque(false);

		panel.add(cabecera());

		actual = Field.password(Textos.t("contrasena.actual"));
		nueva = Field.password(Textos.t("contrasena.nueva"));
		repetida = Field.password(Textos.t("contrasena.repite"));

		panel.add(actual);
		panel.add(nueva);
		panel.add(repetida);

		error = Labels.error(" ");
		panel.add(error);

		panel.add(acciones());

		// Enter en el último campo envía: llegar hasta aquí escribiendo y tener que
		// soltar el teclado para pulsar un botón es una molestia gratuita.
		repetida.onEnter(this::cambiar);

		return panel;
	}

	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);
		superTitulo = Labels.capsAccent(Textos.t("ajustes.superTitulo"));
		titulos.add(superTitulo);
		titulo = Labels.title(Textos.t("contrasena.titulo"));
		titulos.add(titulo, "gaptop " + Space.XXS);

		panel.add(titulos);
		panel.add(new MascotSlot(MascotSlot.Tamano.PEQUENO, Pose.BIENVENIDA), "top, w 56!, h 56!");

		return panel;
	}

	private JPanel acciones() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]", ""));
		fila.setOpaque(false);

		botonCambiar = Buttons.primary(Textos.t("contrasena.titulo"), e -> cambiar());
		fila.add(botonCambiar, "height 44!");
		botonCancelar = Buttons.link(Textos.t("ajustes.cancelar"), e -> navigator.ir(ShowHousingsFrame.class));
		fila.add(botonCancelar);

		return fila;
	}

	private void actualizarTextosFijos() {

		superTitulo.setText(Textos.t("ajustes.superTitulo"));
		titulo.setText(Textos.t("contrasena.titulo"));
		actual.setEtiqueta(Textos.t("contrasena.actual"));
		nueva.setEtiqueta(Textos.t("contrasena.nueva"));
		repetida.setEtiqueta(Textos.t("contrasena.repite"));
		botonCambiar.setText(Textos.t("contrasena.titulo"));
		botonCancelar.setText(Textos.t("ajustes.cancelar"));
	}

	/**
	 * Vacía los campos al abrir.
	 *
	 * <p>
	 * El frame es singleton, así que sin esto la contraseña escrita en una visita
	 * anterior seguiría en pantalla la siguiente vez que se abriera. Con datos
	 * normales sería solo desorden; con contraseñas es dejarlas a la vista de quien
	 * se siente después delante del ordenador.
	 */
	private void limpiar() {

		actual.setText("");
		nueva.setText("");
		repetida.setText("");
		error.setText(" ");
	}

	private void cambiar() {

		if (nueva.getText().isEmpty()) {
			error.setText(Textos.t("contrasena.error.nuevaVacia"));
			return;
		}

		if (!nueva.getText().equals(repetida.getText())) {
			error.setText(Textos.t("contrasena.error.noCoinciden"));
			repetida.requestFocus();
			return;
		}

		try {
			userService.changePassword(sessionManager.getLoggedInUser().getId(), actual.getText(), nueva.getText());

			navigator.ir(ShowHousingsFrame.class);

		} catch (WrongPasswordException ex) {
			error.setText(Textos.t("contrasena.error.actualIncorrecta"));
			actual.requestFocus();

		} catch (InstanceNotFoundException ex) {
			error.setText(Textos.t("alojamientoForm.error.sesionNoValida"));
		}
	}
}
