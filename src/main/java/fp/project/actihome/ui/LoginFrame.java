package fp.project.actihome.ui;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.exceptions.IncorrectLoginException;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.Layout;
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
 *
 * <p>
 * <b>{@code @Lazy}, y esto cierra B10 (parcialmente — ver {@code SignUpFrame},
 * su gemela en este cierre).</b> Hasta ahora era, junto a {@code SignUpFrame},
 * de las últimas pantallas sin marcar así: quedaba de la Fase 2, anterior a que
 * {@code @Lazy} se asentara como convención para las diecisiete. Sin ella,
 * Spring construía sus widgets Swing durante el arranque del contexto —en el
 * hilo principal, no en el EDT—, que es justo lo que {@code B10} lleva anotado
 * como deuda desde el principio del rediseño. Con ella, la primera construcción
 * ocurre dentro del {@code EventQueue.invokeLater(...)} de
 * {@code ActihomeApplication.main}, que es el sitio correcto.
 */
@Component
@Profile("!test")
@Lazy
public class LoginFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final UserService userService;
	private final SessionManager sessionManager;
	private final Navigator navigator;

	private Field usuario;
	private Field contrasena;
	private JLabel error;

	/**
	 * La frase editorial de la estación. Es texto, no color, así que no se resuelve
	 * sola al repintar: hay que reescribirla al volver a mostrar la pantalla, porque
	 * el usuario ha podido cambiar de estación antes de cerrar sesión.
	 */
	private JLabel fraseEstacional;

	/** Piezas de display que crecen con la ventana. Ver {@link Layout}. */
	private JLabel claimPrimera;
	private JLabel claimSegunda;
	private JLabel titulo;

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
			fraseEstacional.setText(Theme.estacion().frase());
			SwingUtilities.invokeLater(usuario::requestFocus);
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(980, 620);
		setLocationRelativeTo(null);

		// Columna izquierda fijada al 45 % con mínimo y máximo iguales, y "wmin 0" en
		// el componente. Sin ese "wmin 0", MigLayout respeta el ancho mínimo del panel
		// —que viene dado por el claim a 40px— y lo deja invadir la columna de al lado:
		// el formulario acababa dibujado por debajo del panel oscuro y cortado.
		JPanel raiz = new Page(new MigLayout("fill, " + Space.insets(0), "[45%:45%:45%][grow,fill]", "[grow,fill]"));

		raiz.add(panelDeMarca(), "grow, wmin 0");
		raiz.add(formulario(), "grow, wmin 0");

		setContentPane(raiz);

		// La tipografía de display se recalcula al redimensionar. Un titular de 38px
		// se ve rotundo en una ventana de 980 y tímido en una de 2500: la proporción
		// entre el texto y su contenedor es parte del diseño, no una consecuencia del
		// tamaño de la letra.
		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				ajustarEscalaDeDisplay();
			}
		});
	}

	/** Ajusta los titulares al ancho actual de la ventana. */
	private void ajustarEscalaDeDisplay() {

		int ancho = getWidth();

		claimPrimera.setFont(Typography.serifMedium(Layout.display(38f, ancho)));
		claimSegunda.setFont(Typography.serifMedium(Layout.display(38f, ancho)));
		titulo.setFont(Typography.serifMedium(Layout.display(Typography.SCREEN_TITLE, ancho)));

		revalidate();
		repaint();
	}

	/**
	 * Mitad izquierda: fondo oscuro, claim y mascota.
	 *
	 * <p>
	 * El contenido se limita a 560px y se centra. Sin ese tope, al maximizar la
	 * ventana el bloque se repartía por una columna de más de mil píxeles y quedaba
	 * un hueco enorme entre el claim y la mascota. <b>Que un layout crezca no
	 * significa que su contenido deba crecer con él</b>: lo que crece es el aire
	 * alrededor.
	 */
	private JPanel panelDeMarca() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, fill, " + Space.insets(Space.GIANT, Space.XXXL, Space.XXXL,
				Space.XXXL), "[grow,fill]", "[]push[]0[]" + Space.XL + "[]push[]" + Space.XXS + "[]")) {

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

		panel.add(Labels.brand("ActiHome", 22f), Layout.ancho(Layout.TEXTO));

		// Dos etiquetas en lugar de una con <html><br></html>: el renderizado HTML de
		// Swing calcula sus tamaños por su cuenta y se lleva mal con las fuentes
		// registradas en tiempo de ejecución. Dos etiquetas son más predecibles.
		// El gap negativo aprieta el interlineado. La altura de una etiqueta incluye el
		// espacio de los rasgos ascendentes y descendentes, así que dos etiquetas
		// apiladas dejan más aire del que pide una serif de display: se lee como dos
		// frases sueltas en vez de como una sola en dos líneas.
		claimPrimera = claim("Elige dónde");
		claimSegunda = claim("quieres despertar");

		panel.add(claimPrimera, Layout.ancho(Layout.TEXTO));
		panel.add(claimSegunda, Layout.ancho(Layout.TEXTO) + ", gaptop -10");

		panel.add(new MascotSlot(MascotSlot.Tamano.GRANDE, Pose.BIENVENIDA), "align left");

		fraseEstacional = Labels.editorialOnHeader(Theme.estacion().frase());
		panel.add(fraseEstacional, Layout.ancho(Layout.TEXTO));
		panel.add(Labels.capsOnHeader("By CocoBrain"), Layout.ancho(Layout.TEXTO));

		return panel;
	}

	/**
	 * Una línea del claim editorial.
	 *
	 * <p>
	 * Pasa por {@code Labels} y no por un {@code setForeground} directo: un color
	 * asignado se congela, y desde que existe el selector de estación esta pantalla
	 * puede reabrirse —al cerrar sesión— con una estación distinta de la que había
	 * cuando se construyó.
	 */
	private JLabel claim(String texto) {
		return Labels.displayOnHeader(texto, 38f);
	}

	/**
	 * Mitad derecha: el formulario.
	 *
	 * <p>
	 * <b>El formulario tiene un ancho máximo y se centra.</b> Al maximizar la
	 * ventana, sin ese tope, el campo de usuario llegaba a medir mil píxeles: un
	 * campo así es incómodo de leer y de rellenar —el ojo pierde la línea— y una
	 * caja tan larga para escribir ocho letras se percibe como un error de
	 * maquetación. La medida cómoda de un formulario ronda los 400-450px
	 * independientemente del tamaño de la pantalla.
	 */
	private JPanel formulario() {

		// Dos capas: una exterior que ocupa toda la mitad y una interior con el ancho
		// limitado. Poner el tope directamente en cada fila no funciona: la columna
		// declarada como "fill" obliga a los componentes a ocupar toda la celda y el
		// máximo se ignora. Limitando el contenedor, todo lo que lleva dentro queda
		// limitado con él.
		// Dos detalles de MigLayout que hay que acertar a la vez, y que costaron tres
		// intentos:
		//
		//   1. La columna es "[grow]" y NO "[grow,fill]". El "fill" de una columna
		//      obliga a sus componentes a ocupar todo el ancho de la celda.
		//   2. El layout NO lleva la palabra "fill". Esa restricción global hace lo
		//      mismo que la anterior pero para todo el panel, y pesa por encima de
		//      cualquier tamaño declarado en un componente.
		//
		// Con las dos fuera, la celda crece con la ventana pero el componente decide
		// su ancho: el tope manda y el espacio sobrante se queda como aire, que es
		// justo la regla que persigue esta pantalla.
		JPanel exterior = new JPanel(new MigLayout(Space.insets(Space.GIANT, Space.GIANT, Space.XXXL, Space.GIANT),
				"[grow]", "[grow]"));
		exterior.setOpaque(false);

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"push[]" + Space.XS + "[]" + Space.XXL + "[]" + Space.LG + "[]" + Space.XS + "[]" + Space.LG + "[]"
						+ Space.XXL + "[]push"));
		panel.setOpaque(false);

		panel.add(Labels.capsAccent("Acceso"));

		titulo = Labels.title("Bienvenido de nuevo");
		panel.add(titulo);

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

		exterior.add(panel, Layout.ancho(Layout.FORMULARIO) + ", aligny center, alignx left");

		return exterior;
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
