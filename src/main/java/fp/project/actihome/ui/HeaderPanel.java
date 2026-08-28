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
import fp.project.actihome.ui.theme.Textos;
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
		// **El tercer hueco tras "push" cedía hasta MD (16) y no más abajo, y hasta
		// la Fase 9 eso bastaba.** Con tres destinos de navegación el hueco nunca
		// llegaba a necesitar más: "BUSCAR" fue el cuarto texto fijo que la barra
		// tuvo que sumar sin poder encoger, y a 1024 de ancho el nombre de usuario
		// empezaba a salirse por 14 puntos. El aire tenía más margen que ceder —XS
		// en vez de MD entre navegación, selector y usuario, XXS en vez de SM entre
		// destinos— y no se le había pedido.
		setLayout(new MigLayout(Space.insets(Space.XS, 0, Space.XS, 0),
				Space.LG + ":" + Space.XXXL + ":" + Space.XXXL + "[]11[]push[]" + Space.XS + ":" + Space.XXL + ":"
						+ Space.XXL + "[]" + Space.XS + ":" + Space.XXL + ":" + Space.XXL + "[]" + Space.LG + ":"
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

		anadirDestino(navegacion, Textos.t("header.nav.buscar"), SearchHousingsFrame.class);
		anadirDestino(navegacion, Textos.t("header.nav.catalogo"), ShowHousingsFrame.class);
		anadirDestino(navegacion, Textos.t("header.nav.mensajes"), MessagesFrame.class);

		if (esCliente()) {
			anadirDestino(navegacion, Textos.t("header.nav.misReservas"), ShowMyReservationsFrame.class);
		} else {
			anadirDestino(navegacion, Textos.t("header.nav.intercambio"), TradeHousingsFrame.class);
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
		// barra: es aire, y el aire es lo primero que se cede cuando falta ancho. El
		// mínimo es XXS y no SM desde que la barra pasó de tres destinos a cuatro
		// (Fase 9): con cuatro textos fijos que no pueden encoger, el hueco entre
		// ellos es lo único que puede ceder algo más.
		contenedor.add(destino,
				destinos.size() == 1 ? "gapleft 0" : "gapleft " + Space.XXS + ":" + Space.XL + ":" + Space.XL);
	}

	private void construirZonaUsuario() {

		User usuario = sessionManager.getLoggedInUser();

		if (usuario == null) {
			return;
		}

		zonaUsuario.add(Avatar.contorno(usuario.getName(), usuario.getSurname(), 26), "w 26!, h 26!");

		JLabel nombre = Labels.capsOnHeader(usuario.getUsername());
		nombre.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		nombre.setToolTipText(usuario.getRole() == RoleType.ADMIN ? Textos.t("header.usuario.tooltip.admin")
				: Textos.t("header.usuario.tooltip.cliente"));
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
	/**
	 * Avatar, nombre de usuario y rol, en la parte de arriba del menú.
	 *
	 * <p>
	 * El avatar es {@link Avatar#relleno}, el disco de acento con las iniciales en
	 * blanco — la variante pensada para fondos claros, que es lo que hay dentro de
	 * un {@code JPopupMenu}. El de la barra de navegación usa {@link
	 * Avatar#contorno} porque ahí el fondo es la cabecera oscura; son casos
	 * distintos del mismo componente, no una inconsistencia.
	 */
	private JPanel cabeceraDelMenu(User usuario) {

		JPanel panel = new JPanel(
				new MigLayout(Space.insets(Space.SM, Space.MD, Space.SM, Space.MD), "[]" + Space.SM + "[grow,fill]", "[]"));
		panel.setOpaque(false);

		panel.add(Avatar.relleno(usuario.getName(), usuario.getSurname(), 34), "w 34!, h 34!");

		JPanel texto = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]"));
		texto.setOpaque(false);
		texto.add(Labels.body(usuario.getUsername()));
		texto.add(Labels.muted(usuario.getRole() == RoleType.ADMIN ? Textos.t("header.usuario.tooltip.admin")
				: Textos.t("header.usuario.tooltip.cliente")));

		panel.add(texto);

		return panel;
	}

	private JPopupMenu menuDeUsuario() {

		JPopupMenu menu = new JPopupMenu();

		// Cabecera del menú (Fase 7.11): antes el menú era una lista desnuda de
		// acciones sin decir de quién. Es un JPanel normal, no un JMenuItem — Swing
		// permite añadir cualquier componente a un JPopupMenu, y uno sin
		// ActionListener no cierra el menú ni se pinta como opción clicable, que es
		// justo lo que hace falta para una cabecera puramente informativa.
		User usuarioActual = sessionManager.getLoggedInUser();

		if (usuarioActual != null) {
			menu.add(cabeceraDelMenu(usuarioActual));
			menu.addSeparator();
		}

		// **Una sola entrada, y antes eran dos.** "Editar perfil" y "Ajustes"
		// apuntaban las dos a esta misma pantalla desde que la Fase 8.4 las unificó,
		// con el argumento de que son dos formas de pensar lo mismo ("quiero cambiar
		// mi correo" / "quiero cambiar la estación") y que quitar una obligaría a
		// media gente a buscar su tarea bajo un nombre que no es el que tiene en la
		// cabeza.
		//
		// El argumento era bueno y la solución mala: dos entradas de menú que llevan
		// al mismo sitio se leen como un fallo, no como una comodidad — quien pulsa
		// la segunda cree que se ha equivocado. La forma correcta de atender los dos
		// modelos mentales es **que la entrada lleve las dos palabras**, que además
		// es como se llama la pantalla de verdad.
		JMenuItem perfilYAjustes = new JMenuItem(Textos.t("header.menu.perfilYAjustes"));
		perfilYAjustes.addActionListener(e -> navigator.ir(SettingsFrame.class));

		JMenuItem contrasena = new JMenuItem(Textos.t("header.menu.contrasena"));
		contrasena.addActionListener(e -> navigator.ir(ChangePasswordFrame.class));


		// Solo para ADMIN (Fase 7.9): es quien puede tener alojamientos propios que
		// mostrar aquí. Mismo criterio de visibilidad que ya usa "Cambiar rol", justo
		// debajo.
		JMenuItem panelPropietario = null;
		JMenuItem panelPlataforma = null;
		JMenuItem administracion = null;

		if (!esCliente()) {
			panelPropietario = new JMenuItem(Textos.t("header.menu.panelPropietario"));
			panelPropietario.addActionListener(e -> navigator.ir(OwnerPanelFrame.class));

			// Copia de seguridad y códigos de recuperación. Estaban dentro de Ajustes y
			// ahí no eran ajustes de nadie: son operaciones sobre la instalación, no
			// sobre la cuenta de quien las ejecuta. Ver la nota de clase de
			// AdministrationFrame.
			administracion = new JMenuItem(Textos.t("admin.menu"));
			administracion.addActionListener(e -> navigator.ir(AdministrationFrame.class));

			// F14: panel agregado de toda la plataforma, no solo de los alojamientos
			// propios — ver la nota de clase de PlatformPanelFrame sobre por qué es
			// accesible a cualquier ADMIN y no a un "superadmin" que hoy no existe.
			panelPlataforma = new JMenuItem(Textos.t("header.menu.panelPlataforma"));
			panelPlataforma.addActionListener(e -> navigator.ir(PlatformPanelFrame.class));
		}

		JMenuItem rol = new JMenuItem(esCliente() ? Textos.t("header.menu.rol.aAdmin") : Textos.t("header.menu.rol.aCliente"));
		rol.addActionListener(e -> cambiarRol());

		JMenuItem acerca = new JMenuItem(Textos.t("header.menu.acercaDe"));
		acerca.addActionListener(e -> AboutDialog.mostrar(SwingUtilities.getWindowAncestor(this)));

		JMenuItem salir = new JMenuItem(Textos.t("header.menu.cerrarSesion"));
		salir.addActionListener(e -> {
			sessionManager.logout();
			navigator.ir(LoginFrame.class);
		});

		menu.add(perfilYAjustes);
		menu.add(contrasena);

		if (panelPropietario != null) {
			menu.add(panelPropietario);
			menu.add(panelPlataforma);
			menu.add(administracion);
		}

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
