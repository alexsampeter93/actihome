package fp.project.actihome.ui;

import java.io.File;
import java.time.LocalDate;
import java.util.Locale;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.EstacionPreferida;
import fp.project.actihome.model.entities.User.Idioma;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.BackupFailedException;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.BackupNotAvailableException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.services.BackupService;
import fp.project.actihome.model.services.PasswordResetService;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.catalog.CatalogFilters;
import fp.project.actihome.ui.components.Avatar;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Card;
import fp.project.actihome.ui.components.Columnas;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.Interruptor;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.Rescate;
import fp.project.actihome.ui.components.Segmented;
import fp.project.actihome.ui.components.Toast;
import fp.project.actihome.ui.components.WrappingText;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Particulas;
import fp.project.actihome.ui.theme.Preferencias;
import fp.project.actihome.ui.theme.Season;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Ajustes de la cuenta: estación por defecto, partículas decorativas e idioma
 * (Fase 7.6), vista de catálogo por defecto (Fase 7.11) y, solo para ADMIN,
 * copia de seguridad de la base de datos (F12).
 *
 * <p>
 * <b>Es la pantalla piloto del idioma.</b> Todo su texto pasa por
 * {@link Textos#t(String)} en vez de ir escrito a mano, como prueba de que el
 * mecanismo funciona de punta a punta antes de extenderlo al resto de la
 * aplicación (ver CLAUDE.md §7, tabla de pantallas pendientes). Como los
 * frames son singleton y {@code initUI()} solo se ejecuta una vez en toda la
 * sesión, los textos que pueden cambiar de idioma se guardan como campos y se
 * vuelven a fijar en {@link #actualizarTextos()}, llamado desde
 * {@code setVisible(true)} — el mismo gancho que ya usa cada pantalla para
 * recargar sus datos.
 *
 * <p>
 * Los cuatro cambios se notan <b>en caliente</b> al guardar, sin esperar a la
 * próxima sesión: estación, partículas e idioma son las mismas tres líneas que
 * {@code LoginFrame} ejecuta al entrar, y la vista de catálogo se nota en
 * cuanto se vuelve al catálogo justo después de guardar —
 * {@code ShowHousingsFrame.olvidarVistaAplicada()}, llamado antes de navegar,
 * es lo que hace que se vuelva a aplicar aunque ya se hubiera aplicado antes
 * en esta sesión.
 */
@Component
@Profile("!test")
@Lazy
public class SettingsFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final transient UserService userService;
	private final transient BackupService backupService;
	private final transient PasswordResetService passwordResetService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	// Cabecera de identidad
	private JPanel avatar;
	private JLabel etiquetaBienvenida;
	private WrappingText descripcionBienvenida;
	private JButton botonBienvenida;
	private JLabel nombreCompleto;
	private JLabel identidad;
	private JButton cambiarRol;

	// Tarjeta de datos personales
	private JLabel superTituloDatos;
	private Field usuario;
	private Field nombre;
	private Field apellido;
	private Field correo;
	private Field telefono;
	private Field localidad;

	// Tarjeta de preferencias
	private JLabel superTituloPreferencias;

	/**
	 * Lo que necesita una tarjeta de ajustes para leerse bien.
	 *
	 * <p>
	 * Sale de la más exigente de las tres: la de datos personales, que lleva sus
	 * seis campos a dos columnas dentro y su relleno de tarjeta a los lados. Por
	 * debajo de esto, "Nombre" y "Apellido" quedan en dos cajas de cien puntos.
	 */
	private static final int ANCHO_COMODO_DE_TARJETA = 420;
	private JLabel etiquetaEstacion;
	private JComboBox<Season> estacion;
	private JLabel etiquetaParticulas;
	private Interruptor particulas;
	private JLabel etiquetaIdioma;
	private JComboBox<Idioma> idioma;
	private JLabel etiquetaVista;
	private Segmented vistaPorDefecto;
	private JButton guardar;
	private JButton cancelar;
	private JButton enlaceContrasena;
	private JButton enlaceCerrarSesion;
	private JLabel error;

	public SettingsFrame(UserService userService, BackupService backupService,
			PasswordResetService passwordResetService, SessionManager sessionManager,
			Navigator navigator, HeaderPanel headerPanel) {

		this.userService = userService;
		this.backupService = backupService;
		this.passwordResetService = passwordResetService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			headerPanel.refresh();
			actualizarTextos();
			precargar();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(1100, 880);
		setLocationRelativeTo(null);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[grow,fill]"));

		JPanel exterior = new JPanel(new MigLayout("wrap 1, " + Space.insetsLaterales(Space.GIANT, Space.GIANT),
				"[grow]", Space.margen(Space.GIANT) + "[grow]" + Space.margen(Space.GIANT)));
		exterior.setOpaque(false);
		// **Más ancho que Layout.CONTENIDO, y es la excepción que el propio sistema
		// prevé.** La regla dice que el espacio sobrante se queda como margen porque un
		// campo de texto muy ancho se lee peor; pero aquí no hay un bloque que se
		// ensanche, hay tres tarjetas que se reparten el ancho, y con 940 puntos cada
		// una se queda en 300 — un campo de nombre de 230 puntos dentro de una tarjeta
		// a dos columnas. Es el mismo caso que las listas y las rejillas: cuando el
		// ancho de más se convierte en más columnas y no en columnas más largas, sí se
		// aprovecha.
		exterior.add(formulario(), Layout.ancho(1240) + ", aligny center, alignx center");

		raiz.add(headerPanel, "growx");
		raiz.add(Rescate.envolver(exterior), "grow");

		setContentPane(raiz);

		Foco.alPulsarEscape(this, () -> navigator.volver(ShowHousingsFrame.class));
	}

	/**
	 * Cabecera de identidad y, debajo, las dos tarjetas temáticas en paralelo.
	 *
	 * <p>
	 * <b>Dos columnas y no una lista larga.</b> Con perfil y preferencias juntos
	 * son once controles, y apilados no caben en la ventana — lo que la regla de
	 * escritorio del proyecto no permite—. Repartidos, cabe entero y el corte tiene
	 * sentido propio: a la izquierda <em>quién eres</em>, a la derecha <em>cómo
	 * quieres ver la aplicación</em>.
	 */
	private JPanel formulario() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.aire(Space.XL) + "[]" + Space.aire(Space.XL) + "[]" + Space.aire(Space.LG) + "[]"));
		panel.setOpaque(false);

		panel.add(cabeceraDeIdentidad());

		// **Tres tarjetas, y hasta ahora eran dos.** El corte anterior —"quién eres" a
		// la izquierda, "cómo ves la aplicación" a la derecha— dejaba la segunda con
		// seis bloques y 963 puntos de alto frente a los 385 de la primera, y en dos
		// columnas manda la más alta: la pantalla pedía 1394 puntos en una ventana que
		// da 672.
		//
		// La tercera columna no es un troceado para que quepa, es un grupo que ya
		// estaba ahí sin nombre: la copia de seguridad y el código de recuperación
		// **no son preferencias de nadie**, son tareas de administración de la
		// instalación —lo decía el propio comentario del código— y por eso solo las ve
		// un ADMIN. Separadas, se lee para quién es cada cosa antes de leer qué hace.
		// Tres tarjetas donde quepan tres, dos donde quepan dos: el reparto lo decide
		// Columnas a partir del ancho, no una rejilla escrita a mano. Con tres fijas,
		// una ventana de 1024 dejaba cada tarjeta en 300 puntos y sus campos y botones
		// se dibujaban fuera.
		Columnas columnas = new Columnas(ANCHO_COMODO_DE_TARJETA, Space.LG);

		columnas.add(tarjetaDatosPersonales());
		columnas.add(tarjetaPreferencias());

		panel.add(columnas);

		error = Labels.error(" ");
		panel.add(error);

		panel.add(acciones());

		return panel;
	}

	/**
	 * Avatar grande, nombre real y, debajo, usuario · rol · localidad. A la derecha
	 * el cambio de rol.
	 *
	 * <p>
	 * Es la pieza que convierte dos formularios en <b>una cuenta</b>: antes, editar
	 * el perfil y cambiar los ajustes eran dos pantallas sin nada en común, y en
	 * ninguna de las dos aparecía en ningún sitio de quién eran los datos que se
	 * estaban tocando.
	 */
	private JPanel cabeceraDeIdentidad() {

		JPanel panel = new JPanel(
				new MigLayout(Space.insets(0), "[]" + Space.MD + "[grow,fill]push[]", "[]"));
		panel.setOpaque(false);

		avatar = new JPanel(new MigLayout(Space.insets(0), "[]", "[]"));
		avatar.setOpaque(false);
		panel.add(avatar, "aligny center");

		JPanel textos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XXS + "[]"));
		textos.setOpaque(false);

		nombreCompleto = Labels.title(" ");
		identidad = Labels.muted(" ");

		textos.add(nombreCompleto);
		textos.add(identidad);
		panel.add(textos, "aligny center");

		cambiarRol = Buttons.secondary(" ", e -> cambiarRol());
		panel.add(cambiarRol, "aligny center");

		return panel;
	}

	/** Tarjeta izquierda: los datos de la persona. */
	private Card tarjetaDatosPersonales() {

		// **Tres columnas dentro de la tarjeta, no dos.** Son seis campos cortos —un
		// nombre, un teléfono, una localidad—, así que en dos eran tres filas de campo
		// y en tres son dos. Un campo no cede alto (su caja lleva un `height !` medido
		// de la fuente, que es lo que impide que el texto salga cortado con el escalado
		// al 150 %), de modo que esa fila de menos son 91 puntos que no hay que
		// quitarle a nada más.
		Card tarjeta = new Card(new MigLayout("wrap 3, " + Space.insets(Space.LG),
				"[grow,fill]" + Space.MD + "[grow,fill]" + Space.MD + "[grow,fill]", ""));

		superTituloDatos = Labels.capsAccent(" ");
		tarjeta.add(superTituloDatos, "span 3, gapbottom " + Space.aire(Space.MD));

		usuario = Field.text(" ");
		nombre = Field.text(" ");
		apellido = Field.text(" ");
		correo = Field.text(" ");
		telefono = Field.text(" ");
		localidad = Field.text(" ");

		tarjeta.add(usuario, "gapbottom " + Space.aire(Space.MD));
		tarjeta.add(nombre, "gapbottom " + Space.aire(Space.MD));
		tarjeta.add(apellido, "gapbottom " + Space.aire(Space.MD));
		tarjeta.add(correo);
		tarjeta.add(telefono);
		tarjeta.add(localidad);

		return tarjeta;
	}

	/** Tarjeta central: cómo se ve y se comporta la aplicación. */
	private Card tarjetaPreferencias() {

		// Las cuatro preferencias en dos columnas, por lo mismo que los seis campos de
		// la tarjeta de al lado: son controles cortos —dos desplegables, un selector de
		// dos opciones y un interruptor— y ninguno gana nada con el ancho entero,
		// mientras que apilados los cuatro esta tarjeta era la más alta de la pantalla
		// y la que decidía si Ajustes cabía o no.
		Card tarjeta = new Card(new MigLayout("wrap 2, hidemode 3, " + Space.insets(Space.LG),
				"[grow,fill]" + Space.MD + "[grow,fill]", ""));

		superTituloPreferencias = Labels.capsAccent(" ");
		tarjeta.add(superTituloPreferencias, "span 2, gapbottom " + Space.aire(Space.MD));

		tarjeta.add(campoEstacion(), "gapbottom " + Space.aire(Space.MD));
		tarjeta.add(campoIdioma(), "gapbottom " + Space.aire(Space.MD));
		tarjeta.add(campoVistaPorDefecto());
		tarjeta.add(campoParticulas());
		tarjeta.add(campoBienvenida(), "span 2, gaptop " + Space.aire(Space.MD));

		return tarjeta;
	}

	private JPanel campoEstacion() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panel.setOpaque(false);

		etiquetaEstacion = Labels.caps(" ");

		estacion = new JComboBox<>(Season.values());
		estacion.setFont(Typography.sans(Typography.BODY));
		estacion.setRenderer(new DefaultListCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
					boolean isSelected, boolean cellHasFocus) {

				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setText(((Season) value).nombre());
				return this;
			}
		});

		panel.add(etiquetaEstacion);
		panel.add(estacion, "gaptop " + Space.XXS + ", height " + Typography.altoDeControl() + "!");

		return panel;
	}

	/**
	 * Las partículas, con un interruptor de verdad y no una casilla.
	 *
	 * <p>
	 * Lo pide el handoff y la razón es de gramática: una casilla dice «marca esto y
	 * luego pulsa Guardar», un interruptor dice «esto está encendido». En una
	 * pantalla de preferencias lo que se manipula no son datos que rellenar sino
	 * estados que activar. La etiqueta va a la izquierda y el control a la derecha,
	 * que es la disposición de una fila de ajuste — al revés que un campo de
	 * formulario, donde el rótulo va encima.
	 */
	private JPanel campoParticulas() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", "[]"));
		panel.setOpaque(false);

		etiquetaParticulas = Labels.caps(" ");
		particulas = new Interruptor();

		panel.add(etiquetaParticulas, "aligny center");
		panel.add(particulas, "aligny center");

		return panel;
	}

	private JPanel campoIdioma() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panel.setOpaque(false);

		etiquetaIdioma = Labels.caps(" ");

		idioma = new JComboBox<>(Idioma.values());
		idioma.setFont(Typography.sans(Typography.BODY));
		idioma.setRenderer(new DefaultListCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
					boolean isSelected, boolean cellHasFocus) {

				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setText(Textos.t(value == Idioma.EN ? "ajustes.idioma.en" : "ajustes.idioma.es"));
				return this;
			}
		});

		panel.add(etiquetaIdioma);
		panel.add(idioma, "gaptop " + Space.XXS + ", height " + Typography.altoDeControl() + "!");

		return panel;
	}

	/**
	 * Con qué vista arranca el catálogo la primera vez que se abre en la sesión
	 * (Fase 7.11). Reutiliza {@link Segmented}, el mismo control con el que ya se
	 * cambia de vista dentro del propio catálogo, para que elegir aquí se sienta
	 * como el mismo gesto.
	 */
	private JPanel campoVistaPorDefecto() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panel.setOpaque(false);

		etiquetaVista = Labels.caps(" ");

		// El texto real se fija en actualizarTextos(): aquí solo hace falta que las
		// dos opciones existan, en el mismo orden que CatalogFilters.VISTA_CUADRICULA
		// espera (0 lista, 1 cuadrícula).
		vistaPorDefecto = new Segmented(0, indice -> {
			// Sin acción: es un ajuste que se guarda al pulsar "Guardar cambios", no un
			// filtro que se aplique al vuelo como en el catálogo.
		}, " ", " ");

		panel.add(etiquetaVista);
		// Sin ancho fijo: el "w 220!" que habia aqui era otro tamano dependiente del
		// texto escrito a mano, y era la otra mitad del recuadro que sobresalia. Con
		// 220 puntos reservados y dos etiquetas que miden menos, el borde del control
		// se dibujaba mas ancho que sus propios botones. Ahora mide lo que mide.
		panel.add(vistaPorDefecto, "gaptop " + Space.XXS + ", left");

		return panel;
	}



	/**
	 * "Ver la bienvenida otra vez": vuelve a abrir la pantalla de presentación.
	 *
	 * <p>
	 * <b>La bienvenida se enseña una sola vez y hasta ahora no había forma de
	 * volver a verla.</b> Es lo razonable por defecto —nadie quiere el tutorial
	 * cada vez que entra— pero convertirlo en irreversible es otra cosa: quien la
	 * pasa deprisa el primer día, o quien enseña la aplicación a otra persona, se
	 * queda sin ella para siempre. Un contenido que explica cómo funciona algo
	 * tiene que poder volver a leerse.
	 *
	 * <p>
	 * No hace falta desmarcar la bandera {@code onboardingSeen}: esa decide si la
	 * bienvenida se enseña <em>sola</em> al entrar, y aquí no se está entrando, se
	 * está pidiendo. Tocarla haría que la próxima sesión la volviera a enseñar sin
	 * que nadie lo hubiera pedido — que es justo lo contrario de lo que quiere
	 * quien pulsa este botón.
	 */
	private JPanel campoBienvenida() {

		// **El botón va al lado del texto, no debajo, y ocupa la fila entera.** Los
		// otros cuatro campos de esta tarjeta son controles cortos que caben a dos
		// columnas; éste es el único con una explicación de dos líneas, y apilado
		// —etiqueta, texto, botón— gastaba noventa puntos de alto dejando además la
		// media fila de al lado vacía. Con la explicación a la izquierda y el botón a
		// la derecha son cuarenta y pocos, que es exactamente lo que le faltaba a
		// Ajustes para caber en un portátil de 1280×680 sin barra de rescate.
		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]" + Space.LG + "[]", ""));
		panel.setOpaque(false);

		JPanel texto = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		texto.setOpaque(false);

		etiquetaBienvenida = Labels.caps(" ");
		texto.add(etiquetaBienvenida);

		descripcionBienvenida = WrappingText.muted(" ");
		texto.add(descripcionBienvenida, "growx, wmin 0, gaptop " + Space.XXS);

		panel.add(texto, "aligny center");

		botonBienvenida = Buttons.secondary(" ",
				e -> navigator.ir(OnboardingFrame.class, pantalla -> pantalla.setVolverAlSalir(true)));
		panel.add(botonBienvenida, "aligny center, height " + Typography.altoDeControl() + "!");

		return panel;
	}

	private JPanel acciones() {

		JPanel fila = new JPanel(
				new MigLayout(Space.insets(0), "[]" + Space.LG + "[]push[]" + Space.XL + "[]", ""));
		fila.setOpaque(false);

		// El texto se fija en actualizarTextos() en vez de aquí, igual que el resto
		// de los campos de esta pantalla.
		guardar = Buttons.primary(" ", e -> guardar());
		cancelar = Buttons.link(" ", e -> navigator.volver(ShowHousingsFrame.class));

		// A la derecha, separadas de los botones de guardar, las dos acciones que no
		// son "guardar cambios" sino salir de la cuenta por otro lado. El handoff las
		// quiere como texto simple: no compiten con la acción principal y dejan de
		// necesitar el menú desplegable para llegar a ellas.
		enlaceContrasena = Buttons.link(" ", e -> navigator.ir(ChangePasswordFrame.class));
		enlaceCerrarSesion = Buttons.link(" ", e -> cerrarSesion());

		fila.add(guardar, "height " + Typography.altoDeBoton() + "!");
		fila.add(cancelar);
		fila.add(enlaceContrasena);
		fila.add(enlaceCerrarSesion);

		return fila;
	}

	private void cerrarSesion() {

		sessionManager.logout();
		navigator.ir(LoginFrame.class);
	}

	/**
	 * Cambia entre cliente y administrador.
	 *
	 * <p>
	 * El cambio se <b>persiste</b>, no se toca solo la sesión, y eso no es un
	 * detalle: {@code PermissionChecker} recarga el usuario de la base de datos en
	 * cada llamada, así que un rol cambiado únicamente en memoria no cambiaría
	 * nada de lo que el servicio autoriza.
	 */
	private void cambiarRol() {

		User actual = sessionManager.getLoggedInUser();

		if (actual == null) {
			navigator.ir(LoginFrame.class);
			return;
		}

		try {
			sessionManager.setLoggedInUser(userService.changeRole(actual.getId()));
			navigator.ir(ShowHousingsFrame.class);

		} catch (InstanceNotFoundException ex) {
			sessionManager.logout();
			navigator.ir(LoginFrame.class);
		}
	}

	/**
	 * Vuelve a fijar todos los textos fijos de la pantalla en el idioma activo.
	 * Se llama desde {@code setVisible(true)}: ver la nota de clase sobre por qué
	 * no hace falta ningún mecanismo más elaborado.
	 */
	private void actualizarTextos() {

		superTituloDatos.setText(Textos.t("ajustes.datosPersonales"));
		superTituloPreferencias.setText(Textos.t("ajustes.preferencias"));

		usuario.setEtiqueta(Textos.t("login.usuario"));
		nombre.setEtiqueta(Textos.t("registro.nombre"));
		apellido.setEtiqueta(Textos.t("registro.apellido"));
		correo.setEtiqueta(Textos.t("registro.correo"));
		telefono.setEtiqueta(Textos.t("registro.telefono"));
		localidad.setEtiqueta(Textos.t("registro.localidad"));

		etiquetaEstacion.setText(Textos.t("ajustes.estacion.label"));
		etiquetaParticulas.setText(Textos.t("ajustes.particulas.label"));
		etiquetaIdioma.setText(Textos.t("ajustes.idioma.label"));
		etiquetaVista.setText(Textos.t("ajustes.vista.label"));
		etiquetaBienvenida.setText(Textos.t("ajustes.bienvenida.label"));
		descripcionBienvenida.setText(Textos.t("ajustes.bienvenida.descripcion"));
		botonBienvenida.setText(Textos.t("ajustes.bienvenida.boton"));
		vistaPorDefecto.actualizarTextos(Textos.t("catalogo.vista.lista"), Textos.t("catalogo.vista.cuadricula"));
		guardar.setText(Textos.t("ajustes.guardar"));
		cancelar.setText(Textos.t("ajustes.cancelar"));
		enlaceContrasena.setText(Textos.t("header.menu.contrasena"));
		enlaceCerrarSesion.setText(Textos.t("header.menu.cerrarSesion"));

		// Fuerza a los desplegables a repintar su selección actual con el renderer,
		// que es quien traduce los nombres de estación e idioma.
		estacion.repaint();
		idioma.repaint();
	}

	/** Vuelca en el formulario las preferencias que hay ahora mismo en la sesión. */
	private void precargar() {

		User actual = sessionManager.getLoggedInUser();

		if (actual == null) {
			navigator.ir(LoginFrame.class);
			return;
		}

		// Sin preferencia guardada (defaultSeason nulo), se preselecciona la
		// estación activa ahora mismo, casi siempre la real de hoy. Guardar sin
		// tocar el desplegable fija esa estación como preferencia explícita a
		// partir de ahora, que es justo lo que se espera de este botón.
		Season estacionInicial = actual.getDefaultSeason() != null
				? Season.valueOf(actual.getDefaultSeason().name())
				: Theme.estacion();

		estacion.setSelectedItem(estacionInicial);
		particulas.setEncendido(actual.isParticlesEnabled());
		idioma.setSelectedItem(actual.getLanguage());
		vistaPorDefecto.setActivo(actual.isDefaultGridView() ? CatalogFilters.VISTA_CUADRICULA : 0);

		usuario.setText(actual.getUsername());
		nombre.setText(actual.getName());
		apellido.setText(actual.getSurname());
		correo.setText(actual.getEmail());
		telefono.setText(String.valueOf(actual.getPhoneNumber()));
		localidad.setText(actual.getLocality());

		// El avatar se reconstruye en cada visita en lugar de guardarse: su color y
		// sus iniciales dependen del nombre, que esta misma pantalla puede acabar de
		// cambiar.
		avatar.removeAll();
		avatar.add(Avatar.relleno(actual.getName(), actual.getSurname(), 64), "w 64!, h 64!");

		nombreCompleto.setText(actual.getName() + " " + actual.getSurname());
		identidad.setText("@" + actual.getUsername() + " · "
				+ Textos.t(actual.getRole() == RoleType.ADMIN ? "header.usuario.tooltip.admin"
						: "header.usuario.tooltip.cliente")
				+ (actual.getLocality() == null || actual.getLocality().isEmpty() ? ""
						: " · " + actual.getLocality()));

		cambiarRol.setText(Textos.t(actual.getRole() == RoleType.ADMIN ? "header.menu.rol.aCliente"
				: "header.menu.rol.aAdmin"));

		error.setText(" ");
	}

	/**
	 * Guarda de una vez el perfil y las preferencias.
	 *
	 * <p>
	 * <b>Son dos llamadas al servicio y un solo botón</b>, y el orden importa:
	 * primero el perfil, que es el que puede fallar por un nombre de usuario ya
	 * ocupado. Si fallara después de haber guardado las preferencias, el usuario
	 * vería un error habiendo cambiado ya media pantalla. Las preferencias no
	 * pueden fallar por conflicto con nadie, así que van segundas.
	 *
	 * <p>
	 * No es una transacción: si la segunda llamada fallara —solo puede hacerlo
	 * porque la cuenta haya dejado de existir entre una y otra— quedaría el perfil
	 * guardado y las preferencias no. Se acepta a conciencia: montar una
	 * transacción de aplicación para dos escrituras sobre la misma fila, en una
	 * aplicación de escritorio de un solo usuario, sería más maquinaria que
	 * problema.
	 */
	private void guardar() {

		Season estacionElegida = (Season) estacion.getSelectedItem();
		Idioma idiomaElegido = (Idioma) idioma.getSelectedItem();
		boolean particulasActivas = particulas.isEncendido();

		boolean vistaCuadricula = vistaPorDefecto.getActivo() == CatalogFilters.VISTA_CUADRICULA;

		if (usuario.getText().trim().isEmpty()) {
			error.setText(Textos.t("perfil.error.usuarioVacio"));
			return;
		}

		int numeroDeTelefono;

		try {
			// Se valida aquí y no se deja caer en el catch de abajo: un teléfono mal
			// escrito no es un fallo del servicio, y mezclarlo con las excepciones de
			// negocio produce mensajes de error que hablan de otra cosa.
			numeroDeTelefono = Integer.parseInt(telefono.getText().trim());

		} catch (NumberFormatException ex) {
			error.setText(Textos.t("perfil.error.telefonoInvalido"));
			return;
		}

		try {
			userService.updateProfile(sessionManager.getLoggedInUser().getId(), usuario.getText().trim(),
					nombre.getText().trim(), apellido.getText().trim(), correo.getText().trim(), numeroDeTelefono,
					localidad.getText().trim());

			User actualizado = userService.updatePreferences(sessionManager.getLoggedInUser().getId(),
					EstacionPreferida.valueOf(estacionElegida.name()), particulasActivas, idiomaElegido,
					vistaCuadricula);

			// La sesión guarda el User en memoria: sin esto, la próxima vez que se
			// abriera esta pantalla precargaría los valores viejos.
			sessionManager.setLoggedInUser(actualizado);

			Theme.cambiarA(estacionElegida);
			Particulas.activar(particulasActivas);
			Textos.cambiarA(idiomaElegido == Idioma.EN ? Locale.ENGLISH : new Locale("es"));

			// Deja copia en disco de lo que se acaba de aplicar (Fase 8.1). Sin esto,
			// elegir "invierno" aquí se notaba en toda la aplicación menos en la
			// pantalla que se ve al abrirla, que es donde más canta.
			Preferencias.recordar();

			navigator.ir(ShowHousingsFrame.class, ShowHousingsFrame::olvidarVistaAplicada);
			Toast.mostrar(navigator.ventanaVisible(), Textos.t("ajustes.confirmacion.guardado"));

		} catch (DuplicateInstanceException ex) {
			error.setText(Textos.t("perfil.error.usuarioOcupado"));

		} catch (InstanceNotFoundException ex) {
			error.setText(Textos.t("ajustes.error.usuarioNoExiste"));
		}
	}
}
