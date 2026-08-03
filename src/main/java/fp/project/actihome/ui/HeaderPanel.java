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
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.brand.AboutDialog;
import fp.project.actihome.ui.components.Avatar;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.SeasonGlyph;
import fp.project.actihome.ui.components.SeasonSelector;
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
	private static final int ALTO = 72;

	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final transient UserService userService;

	private final List<Destino> destinos = new ArrayList<>();
	private JPanel navegacion;
	private JPanel zonaUsuario;
	private Class<?> pantallaActual;

	public HeaderPanel(SessionManager sessionManager, Navigator navigator, UserService userService) {

		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.userService = userService;

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

	/**
	 * Recarga lo que depende del usuario de la sesión.
	 *
	 * <p>
	 * Reconstruye también los <b>destinos de navegación</b>, no solo el avatar y
	 * el nombre. Se descubrió por qué hace falta al ver una captura de esta misma
	 * fase: como los frames son singleton, la primera vez que alguien abre una
	 * pantalla queda fijado <em>para siempre</em> qué {@code HeaderPanel} usa esa
	 * pantalla —se inyecta una vez, en el constructor—. Si esa primera vez fue con
	 * un CUSTOMER conectado, "Mis reservas" quedaba en la barra aunque después se
	 * cerrara sesión y entrara un ADMIN, porque {@code initUI()} solo decidía los
	 * destinos una vez y nadie los volvía a mirar. Ahora {@code esCliente()} se
	 * vuelve a evaluar en cada visita, igual que ya hacía la zona de usuario.
	 */
	public void refresh() {

		navegacion.removeAll();
		destinos.clear();
		construirNavegacion();
		navegacion.revalidate();
		navegacion.repaint();

		zonaUsuario.removeAll();
		construirZonaUsuario();
		zonaUsuario.revalidate();
		zonaUsuario.repaint();
	}

	private void initUI() {

		// Márgenes verticales cortos (XS en vez de LG). Con 20px arriba y abajo, de los
		// 66px de la barra quedaban 26 para el contenido: justo lo que mide el wordmark
		// solo, así que al añadirle debajo la firma "By CocoBrain" la segunda línea
		// salía cortada por el borde. El aire lateral sí se mantiene, que es el que se
		// percibe en una barra.
		//
		// **Las separaciones son elásticas, y ese es el arreglo de fondo.** Antes eran
		// números fijos —40 de margen, 34 entre bloques—, así que la barra tenía un
		// ancho mínimo que era la suma de todo lo que lleva más esos huecos. En un
		// portátil con el escalado de Windows al 150 % la aplicación recibe 1280 puntos
		// lógicos de ancho, no 1920, y ese mínimo no cabía: el avatar y el nombre de
		// usuario quedaban dibujados fuera de la ventana.
		//
		// La sintaxis "16:34:34" es mínimo:preferido:máximo aplicada a un hueco. Con
		// ella la barra usa el aire que el diseño pide cuando hay sitio y lo cede —solo
		// entonces, y solo hasta un tope decente— cuando no lo hay. Es preferible a
		// esconder elementos: el aire se puede negociar, un botón no.
		setLayout(new MigLayout(Space.insets(Space.XS, 0, Space.XS, 0),
				Space.LG + ":" + Space.XXXL + ":" + Space.XXXL + "[]11[]push[]" + Space.MD + ":" + Space.XXL + ":"
						+ Space.XXL + "[]" + Space.MD + ":" + Space.XXL + ":" + Space.XXL + "[]" + Space.LG + ":"
						+ Space.XXXL + ":" + Space.XXXL,
				"[]"));
		setOpaque(false);
		setPreferredSize(new Dimension(0, ALTO));
		setMinimumSize(new Dimension(0, ALTO));

		add(new SeasonGlyph(16), "w 16!, h 16!");
		add(marca());

		navegacion = new JPanel(new MigLayout(Space.insets(0), "", "[]"));
		navegacion.setOpaque(false);
		construirNavegacion();
		add(navegacion);

		// El selector de estación vive aquí y no en el hero del catálogo. Ver la nota
		// en SeasonSelector: ahí solo era alcanzable desde una pantalla de diecisiete.
		add(new SeasonSelector(true), "aligny center");

		zonaUsuario = new JPanel(new MigLayout(Space.insets(0), "[]9[]", "[]"));
		zonaUsuario.setOpaque(false);
		construirZonaUsuario();
		add(zonaUsuario);
	}

	/**
	 * Los destinos que ve el usuario según su rol.
	 *
	 * <p>
	 * <b>"Intercambio" aparece para todo administrador, tenga o no alojamientos.</b>
	 * Antes solo existía como enlace dentro de la ficha de un alojamiento propio, y
	 * el resultado era que quien no tuviera ninguno no llegaba a saber que la
	 * pantalla existía: una función cuyas condiciones para aparecer son invisibles
	 * es, en la práctica, una función que no está. Ahora el destino está siempre y
	 * es la propia pantalla la que explica qué falta para poder usarlo.
	 */
	private void construirNavegacion() {

		anadirDestino(navegacion, "Catálogo", ShowHousingsFrame.class);

		if (esCliente()) {
			anadirDestino(navegacion, "Mis reservas", ShowMyReservationsFrame.class);
		} else {
			anadirDestino(navegacion, "Intercambio", TradeHousingsFrame.class);
		}
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

	/**
	 * El wordmark con la firma de CocoBrain debajo.
	 *
	 * <p>
	 * <b>La firma vivía en el colofón del catálogo</b>, y el colofón se eliminó para
	 * devolverle 80px a la lista. Aquí no cuesta alto: la versalita cabe bajo el
	 * wordmark dentro de los 66px que la cabecera ya ocupaba, y además gana
	 * presencia — antes solo aparecía en el catálogo y ahora firma <b>las
	 * diecisiete pantallas</b>.
	 *
	 * <p>
	 * Se escribe <b>"By CocoBrain"</b> y no "por CocoBrain": es como firma la marca.
	 */
	private JPanel marca() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[]", "[]0[]"));
		panel.setOpaque(false);

		panel.add(wordmark());
		panel.add(Labels.capsOnHeader("By CocoBrain"), "gaptop -2");

		return panel;
	}

	private void anadirDestino(JPanel contenedor, String texto, Class<?> pantalla) {

		Destino destino = new Destino(texto, pantalla);
		destinos.add(destino);

		// El hueco entre destinos también es elástico, por el mismo motivo que los de la
		// barra: es aire, y el aire es lo primero que se cede cuando falta ancho.
		contenedor.add(destino,
				destinos.size() == 1 ? "gapleft 0" : "gapleft " + Space.SM + ":" + Space.XL + ":" + Space.XL);
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

		JMenuItem rol = new JMenuItem(esCliente() ? "Cambiar a administrador" : "Cambiar a cliente");
		rol.addActionListener(e -> cambiarRol());

		JMenuItem acerca = new JMenuItem("Acerca de ActiHome");
		acerca.addActionListener(e -> AboutDialog.mostrar(SwingUtilities.getWindowAncestor(this)));

		JMenuItem salir = new JMenuItem("Cerrar sesión");
		salir.addActionListener(e -> {
			sessionManager.logout();
			navigator.ir(LoginFrame.class);
		});

		menu.add(perfil);
		menu.add(contrasena);
		menu.addSeparator();
		menu.add(rol);
		menu.addSeparator();
		menu.add(acerca);
		menu.add(salir);

		return menu;
	}

	/**
	 * Cambia el rol de la cuenta y vuelve al catálogo.
	 *
	 * <p>
	 * <b>Se vuelve al catálogo a propósito, en lugar de quedarse donde se estaba.</b>
	 * Las pantallas dependen del rol: un ADMIN no tiene "Mis reservas" y un CUSTOMER
	 * no puede editar alojamientos. Quedarse quieto podría dejar al usuario en una
	 * pantalla que su nuevo rol no debería ver. El catálogo lo ven los dos.
	 *
	 * <p>
	 * La sesión se actualiza con el usuario que devuelve el servicio, no tocando el
	 * que ya había: el rol de verdad es el que quedó guardado, y copiarlo de la
	 * respuesta evita que la interfaz y la base de datos digan cosas distintas.
	 */
	private void cambiarRol() {

		User usuario = sessionManager.getLoggedInUser();

		if (usuario == null) {
			return;
		}

		try {
			sessionManager.setLoggedInUser(userService.changeRole(usuario.getId()));
			navigator.ir(ShowHousingsFrame.class);

		} catch (InstanceNotFoundException ex) {
			// La cuenta ya no existe: lo único sensato es volver al login.
			sessionManager.logout();
			navigator.ir(LoginFrame.class);
		}
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

	/**
	 * Un enlace de navegación de la barra.
	 *
	 * <p>
	 * <b>Igual que {@code SeasonSelector.Pestana}, ya no dibuja su texto a mano.</b>
	 * La primera versión usaba {@code drawString} con hints fijados a mano y
	 * repintaba la fila entera para evitar discrepancias de redondeo entre monitores
	 * a distinto escalado — y el usuario siguió viendo temblor. La causa de fondo no
	 * eran los hints: era que este es de los pocos textos de la aplicación que
	 * <b>no</b> pasa por el motor de pintado estándar de Swing. Ahora sí: un
	 * {@code JLabel} para el texto, una barra fina aparte para el subrayado de la
	 * pantalla actual, y el color se cambia con {@code setForeground}.
	 */
	private class Destino extends JPanel {

		private static final long serialVersionUID = 1L;

		private final transient Class<?> pantalla;
		private final JLabel etiqueta;
		private final JPanel subrayado;
		private boolean encima;

		Destino(String texto, Class<?> pantalla) {

			super(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XXS + "[]"));
			this.pantalla = pantalla;

			setOpaque(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			etiqueta = new JLabel(texto.toUpperCase());
			etiqueta.setFont(Typography.label(12f));

			subrayado = new JPanel();
			subrayado.setOpaque(true);

			// Ver la nota gemela en SeasonSelector.Pestana: un JPanel recién creado
			// informa un mínimo de 10x10, muy por encima del alto real que fuerza el "h
			// 2!" de abajo.
			subrayado.setMinimumSize(new Dimension(0, 2));

			add(etiqueta);
			add(subrayado, "growx, h 2!");

			MouseAdapter interaccion = new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					navegar();
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					encima = true;
					actualizarColores();
				}

				@Override
				public void mouseExited(MouseEvent e) {
					encima = false;
					actualizarColores();
				}
			};

			addMouseListener(interaccion);
			etiqueta.addMouseListener(interaccion);

			// Sin esto no había forma de navegar por la cabecera con el teclado: ni Tab
			// llegaba hasta aquí, ni Espacio o Intro activaban nada. Es la pieza de
			// accesibilidad que faltaba en los cuatro controles que dibujan su propio
			// texto en vez de heredar de un botón (ver la nota de clase en Foco).
			Foco.activable(this, this::navegar);

			actualizarColores();
		}

		private void navegar() {

			// Navegar a donde ya estás cierra y reabre la ventana para nada.
			if (pantalla != pantallaActual) {
				navigator.ir(pantalla.asSubclass(javax.swing.JFrame.class));
			}
		}

		private void actualizarColores() {

			boolean actual = pantalla == pantallaActual;

			// Sobre la barra oscura el texto es el color del fondo de página; el estado
			// inactivo se consigue bajando la opacidad, no cambiando de color, para que
			// los cuatro temas se comporten igual sin añadir tokens nuevos.
			Color base = Theme.bg();
			etiqueta.setForeground(actual || encima ? base : new Color(base.getRed(), base.getGreen(), base.getBlue(), 150));

			// Theme.bg(), no Theme.acc(): el mismo color que ya usa el texto activo justo
			// encima, no el acento. Medido con MedirContraste (entrada 032 del diario):
			// el acento como barra sobre la cabecera oscura daba 2,2-2,4:1 en tres de las
			// cuatro estaciones —por debajo del 3:1 que exige un componente de
			// interfaz—, casi invisible. Iba a colores distintos el texto y su propio
			// subrayado, así que el arreglo además une los dos en el mismo tono.
			subrayado.setBackground(actual ? base : getBackground());
			subrayado.setOpaque(actual);
		}

		/**
		 * Se resuelve el estado en cada repintado. Ver la nota gemela en
		 * {@code SeasonSelector.Pestana.paint()}: es lo que hace que marcar el destino
		 * actual ({@code marcarActual}) o navegar entre pantallas se refleje sin
		 * necesidad de una suscripción explícita a nada.
		 */
		@Override
		public void paint(Graphics g) {

			actualizarColores();
			super.paint(g);
			Foco.pintarAnillo((Graphics2D) g, this);
		}
	}
}
