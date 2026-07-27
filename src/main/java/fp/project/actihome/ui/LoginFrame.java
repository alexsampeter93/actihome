package fp.project.actihome.ui;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.exceptions.IncorrectLoginException;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Pantalla de entrada.
 *
 * <p>
 * Dos mitades: a la izquierda el panel de marca, con el claim editorial y Olaz
 * en grande; a la derecha el formulario sobre fondo claro. Es una composición
 * clásica de pantalla de acceso y aquí encaja especialmente bien, porque
 * resuelve el problema de dónde poner la mascota: el login es una pantalla con
 * dos campos y mucho aire, justo el sitio donde Olaz puede ocupar espacio sin
 * competir con nada.
 *
 * <p>
 * Cabe entera en la ventana, sin scroll, según la regla de escritorio del
 * proyecto.
 */
@Component
@Profile("!test")
public class LoginFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final UserService userService;
	private final SessionManager sessionManager;
	private final Navigator navigator;

	private Field usuario;
	private Field contrasena;
	private JLabel error;

	public LoginFrame(UserService userService, SessionManager sessionManager, Navigator navigator) {

		this.userService = userService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;

		initUI();
	}

	/**
	 * Al mostrarse solo se <b>limpia el estado</b>. La interfaz se construyó una
	 * vez, en el constructor.
	 *
	 * <p>
	 * Esta separación es la que evita el bug B2: las pantallas antiguas registraban
	 * sus {@code ActionListener} dentro de {@code setVisible}, y como los frames son
	 * beans singleton de Spring, la segunda visita dejaba dos escuchas sobre el
	 * mismo botón, la tercera tres, y un clic disparaba la acción varias veces.
	 * <b>Construir va en el constructor; refrescar va en {@code setVisible}.</b>
	 */
	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			usuario.setText("");
			contrasena.setText("");
			error.setText(" ");
			SwingUtilities.invokeLater(usuario::requestFocus);
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(980, 620);
		setMinimumSize(new java.awt.Dimension(860, 560));
		setLocationRelativeTo(null);

		// Columna izquierda fijada al 45 % con mínimo y máximo iguales, y "wmin 0" en
		// el componente. Sin ese "wmin 0", MigLayout respeta el ancho mínimo del panel
		// —que viene dado por el claim a 40px— y lo deja invadir la columna de al lado:
		// el formulario acababa dibujado por debajo del panel oscuro y cortado.
		JPanel raiz = new JPanel(new MigLayout("fill, " + Space.insets(0), "[45%:45%:45%][grow,fill]", "[grow,fill]"));
		raiz.setBackground(Theme.bg());

		raiz.add(panelDeMarca(), "grow, wmin 0");
		raiz.add(formulario(), "grow, wmin 0");

		setContentPane(raiz);
	}

	/** Mitad izquierda: fondo oscuro, claim y mascota. */
	private JPanel panelDeMarca() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, fill, " + Space.insets(Space.GIANT, Space.XXXL, Space.XXXL,
				Space.XXXL), "[grow,fill]", "[]" + Space.XL + "[]0[]push[]" + Space.MD + "[]")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {

				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				g2.setColor(Theme.hdr());
				g2.fillRect(0, 0, getWidth(), getHeight());

				// Un velo del acento sobre el fondo oscuro, muy tenue: es lo que hace que
				// este panel cambie de carácter con la estación en lugar de ser siempre el
				// mismo rectángulo negro.
				g2.setColor(Theme.imgTint());
				g2.fillRect(0, 0, getWidth(), getHeight());

				g2.dispose();
				super.paintComponent(g);
			}
		};
		panel.setOpaque(false);

		JLabel marca = new JLabel("ActiHome");
		marca.setFont(Typography.serifMedium(30f));
		marca.setForeground(Theme.bg());
		panel.add(marca);

		// Dos etiquetas en lugar de una con <html><br></html>: el renderizado HTML de
		// Swing calcula sus tamaños por su cuenta y se lleva mal con las fuentes
		// registradas en tiempo de ejecución. Dos etiquetas son más predecibles.
		// El gap negativo aprieta el interlineado. La altura de una etiqueta incluye el
		// espacio de los rasgos ascendentes y descendentes, así que dos etiquetas
		// apiladas dejan más aire del que pide una serif de display: se lee como dos
		// frases sueltas en vez de como una sola en dos líneas.
		panel.add(claim("Elige dónde"), "wmin 0");
		panel.add(claim("quieres despertar"), "wmin 0, gaptop -10");

		panel.add(new MascotSlot(MascotSlot.Tamano.GRANDE), "align center");
		panel.add(Labels.editorialOnHeader(Theme.estacion().frase()), "wmin 0");
		panel.add(Labels.capsOnHeader("por CocoBrain"), "wmin 0");

		return panel;
	}

	/** Una línea del claim editorial. */
	private JLabel claim(String texto) {

		JLabel etiqueta = new JLabel(texto);
		etiqueta.setFont(Typography.serifMedium(38f));
		etiqueta.setForeground(Theme.bg());
		return etiqueta;
	}

	/** Mitad derecha: el formulario. */
	private JPanel formulario() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.GIANT, Space.GIANT, Space.XXXL,
				Space.GIANT), "[grow,fill]", "push[]" + Space.XS + "[]" + Space.XXL + "[]" + Space.LG + "[]"
						+ Space.XS + "[]" + Space.LG + "[]" + Space.XXL + "[]push"));
		panel.setOpaque(false);

		panel.add(Labels.capsAccent("Acceso"));
		panel.add(Labels.title("Bienvenido de nuevo"));

		usuario = Field.text("Nombre de usuario");
		panel.add(usuario);

		contrasena = Field.password("Contraseña");
		panel.add(contrasena);

		// Se reserva el hueco del error desde el principio, con un espacio en blanco.
		// Si el mensaje apareciera de la nada, el formulario entero daría un salto al
		// fallar el login, y ese salto es justo cuando el usuario está mirando.
		error = Labels.error(" ");
		panel.add(error);

		panel.add(Buttons.primary("Entrar", e -> entrar()), "growx, height 44!");

		panel.add(enlaceARegistro());

		// Enter envía el formulario desde cualquiera de los dos campos.
		usuario.onEnter(this::entrar);
		contrasena.onEnter(this::entrar);

		return panel;
	}

	private JPanel enlaceARegistro() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XXS + "[]", ""));
		fila.setOpaque(false);

		fila.add(Labels.muted("¿Es tu primera vez aquí?"));

		JLabel enlace = Labels.body("Regístrate");
		enlace.setFont(Typography.sansSemiBold(Typography.BODY_SM));
		enlace.setForeground(Theme.acc());
		enlace.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		enlace.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				navigator.ir(SignUpFrame.class);
			}
		});
		fila.add(enlace);

		return fila;
	}

	private void entrar() {

		String nombre = usuario.getText().trim();
		String clave = contrasena.getText();

		if (nombre.isEmpty() || clave.isEmpty()) {
			error.setText("Escribe tu usuario y tu contraseña.");
			return;
		}

		try {
			sessionManager.login(userService.login(nombre, clave));
			navigator.ir(ShowHousingsFrame.class);

		} catch (IncorrectLoginException ex) {

			// Se captura la excepción concreta y no un Exception genérico, y el mensaje
			// no distingue entre "ese usuario no existe" y "la contraseña no es esa":
			// decirlo revelaría qué nombres de usuario están registrados.
			error.setText("Usuario o contraseña incorrectos.");
			contrasena.setText("");
			contrasena.requestFocus();
		}
	}
}
