package fp.project.actihome.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.ui.components.Avatar;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.SeasonGlyph;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Barra de navegación superior, común a todas las pantallas con sesión abierta.
 *
 * <p>
 * De izquierda a derecha: el glifo de la estación activa, el wordmark
 * <b>ActiHome</b> en serif con versalita, y a la derecha los destinos de
 * navegación seguidos del avatar y el nombre de usuario.
 *
 * <p>
 * <b>Por qué la barra es oscura.</b> Es el único bloque oscuro de la pantalla, y
 * eso hace tres cosas a la vez: separa la navegación del contenido sin
 * necesidad de una línea, ancla visualmente la composición por arriba, y da a la
 * estación un sitio donde manifestarse en un tono profundo —el token
 * {@code hdr}— que no se usa en ningún otro lugar.
 *
 * <p>
 * <b>Los destinos son los que existen, no los del mockup.</b> El handoff dibuja
 * tres: Catálogo, Reservas y Reseñas. En ActiHome las reseñas se consultan
 * <em>de un alojamiento concreto</em> ({@code ShowReviewsFrame} necesita saber
 * cuál), así que no hay ninguna pantalla de "todas las reseñas" a la que enlazar
 * y crearla sería inventar funcionalidad. Y "Mis reservas" solo tiene sentido
 * para un CUSTOMER: un ADMIN publica alojamientos, no los reserva. La barra
 * muestra por tanto lo que el usuario puede hacer de verdad. Un menú que lleva a
 * sitios que no existen es peor que un menú corto.
 *
 * <p>
 * Sigue siendo {@code @Scope("prototype")}: cada ventana necesita su propia
 * instancia, porque un componente de Swing solo puede estar dentro de un
 * contenedor a la vez. Si fuera singleton, abrir la segunda pantalla arrancaría
 * la cabecera de la primera.
 */
@Component
@Profile("!test")
@Lazy
@Scope("prototype")
public class HeaderPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Alto fijo. La cabecera no crece con la ventana: lo que crece es el contenido. */
	private static final int ALTO = 66;

	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;

	private final List<Destino> destinos = new ArrayList<>();
	private JPanel zonaUsuario;
	private Class<?> pantallaActual;

	public HeaderPanel(SessionManager sessionManager, Navigator navigator) {

		this.sessionManager = sessionManager;
		this.navigator = navigator;

		initUI();
	}

	/**
	 * Marca qué destino es la pantalla en la que estamos, para resaltarlo.
	 *
	 * <p>
	 * Lo llama cada pantalla al construirse. Sin esto la cabecera no tiene forma de
	 * saber dónde está: es un componente, no conoce a la ventana que lo contiene.
	 */
	public void marcarActual(Class<?> pantalla) {

		this.pantallaActual = pantalla;
		repaint();
	}

	/** Recarga lo que depende del usuario de la sesión. */
	public void refresh() {

		zonaUsuario.removeAll();
		construirZonaUsuario();
		zonaUsuario.revalidate();
		zonaUsuario.repaint();
	}

	private void initUI() {

		setLayout(new MigLayout(Space.insets(Space.LG, Space.XXXL, Space.LG, Space.XXXL),
				"[]11[]push[]" + Space.XXL + "[]", "[]"));
		setOpaque(false);
		setPreferredSize(new Dimension(0, ALTO));
		setMinimumSize(new Dimension(0, ALTO));

		add(new SeasonGlyph(16), "w 16!, h 16!");
		add(wordmark());

		JPanel navegacion = new JPanel(new MigLayout(Space.insets(0), "", "[]"));
		navegacion.setOpaque(false);

		anadirDestino(navegacion, "Catálogo", ShowHousingsFrame.class);

		if (esCliente()) {
			anadirDestino(navegacion, "Mis reservas", ShowMyReservationsFrame.class);
		}

		add(navegacion);

		zonaUsuario = new JPanel(new MigLayout(Space.insets(0), "[]9[]", "[]"));
		zonaUsuario.setOpaque(false);
		construirZonaUsuario();
		add(zonaUsuario);
	}

	private boolean esCliente() {

		User usuario = sessionManager.getLoggedInUser();
		return usuario != null && usuario.getRole() == RoleType.CUSTOMER;
	}

	/**
	 * El wordmark: serif, versalita y letras separadas.
	 *
	 * <p>
	 * Es la marca del producto, así que no pasa por {@code Labels}: allí viven los
	 * papeles genéricos del sistema (título, cuerpo, versalita) y esto es una pieza
	 * única con su propio tratamiento.
	 */
	private JLabel wordmark() {
		return Labels.brand("ActiHome", 20f);
	}

	private void anadirDestino(JPanel contenedor, String texto, Class<?> pantalla) {

		Destino destino = new Destino(texto, pantalla);
		destinos.add(destino);
		contenedor.add(destino, "gapleft " + (destinos.size() == 1 ? 0 : Space.XL));
	}

	private void construirZonaUsuario() {

		User usuario = sessionManager.getLoggedInUser();

		if (usuario == null) {
			return;
		}

		zonaUsuario.add(Avatar.contorno(usuario.getName(), usuario.getSurname(), 26), "w 26!, h 26!");

		JLabel nombre = Labels.capsOnHeader(usuario.getUsername());
		nombre.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		nombre.setToolTipText(usuario.getRole() == RoleType.ADMIN ? "Administrador" : "Cliente");
		nombre.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				menuDeUsuario().show(nombre, 0, nombre.getHeight() + Space.XS);
			}
		});

		zonaUsuario.add(nombre);
	}

	/**
	 * El menú se construye en cada apertura, no una vez.
	 *
	 * <p>
	 * Es deliberado y es la corrección del bug B2 aplicada aquí: un menú guardado
	 * como campo al que se le añaden escuchas fuera del constructor acaba
	 * acumulándolas. Construirlo al vuelo cuesta microsegundos y no deja estado que
	 * mantener.
	 */
	private JPopupMenu menuDeUsuario() {

		JPopupMenu menu = new JPopupMenu();

		JMenuItem perfil = new JMenuItem("Editar perfil");
		perfil.addActionListener(e -> navigator.ir(UpdateProfileFrame.class));

		JMenuItem contrasena = new JMenuItem("Cambiar contraseña");
		contrasena.addActionListener(e -> navigator.ir(ChangePasswordFrame.class));

		JMenuItem salir = new JMenuItem("Cerrar sesión");
		salir.addActionListener(e -> {
			sessionManager.logout();
			navigator.ir(LoginFrame.class);
		});

		menu.add(perfil);
		menu.add(contrasena);
		menu.addSeparator();
		menu.add(salir);

		return menu;
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(Theme.hdr());
		g2.fillRect(0, 0, getWidth(), getHeight());

		g2.dispose();
		super.paintComponent(g);
	}

	/** Un enlace de navegación de la barra. */
	private class Destino extends JComponent {

		private static final long serialVersionUID = 1L;

		private final transient Class<?> pantalla;
		private final String texto;
		private boolean encima;

		Destino(String texto, Class<?> pantalla) {

			this.texto = texto.toUpperCase();
			this.pantalla = pantalla;

			setFont(Typography.label(12f));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {

					// Navegar a donde ya estás cierra y reabre la ventana para nada.
					if (pantalla != pantallaActual) {
						navigator.ir(pantalla.asSubclass(javax.swing.JFrame.class));
					}
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					encima = true;
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e) {
					encima = false;
					repaint();
				}
			});
		}

		@Override
		public Dimension getPreferredSize() {

			return new Dimension(getFontMetrics(getFont()).stringWidth(texto) + 2,
					getFontMetrics(getFont()).getHeight() + 6);
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			boolean actual = pantalla == pantallaActual;

			// Sobre la barra oscura el texto es el color del fondo de página; el estado
			// inactivo se consigue bajando la opacidad, no cambiando de color, para que
			// los cuatro temas se comporten igual sin añadir tokens nuevos.
			Color base = Theme.bg();
			g2.setColor(actual || encima ? base : new Color(base.getRed(), base.getGreen(), base.getBlue(), 150));

			g2.setFont(getFont());
			g2.drawString(texto, 0, g2.getFontMetrics().getAscent());

			if (actual) {
				g2.setColor(Theme.acc());
				g2.fillRect(0, getHeight() - 2, getWidth() - 2, 2);
			}

			g2.dispose();
		}
	}
}
