package fp.project.actihome.ui;

import java.util.Locale;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.EstacionPreferida;
import fp.project.actihome.model.entities.User.Idioma;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.catalog.CatalogFilters;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.Rescate;
import fp.project.actihome.ui.components.Segmented;
import fp.project.actihome.ui.components.Toast;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Particulas;
import fp.project.actihome.ui.theme.Season;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Ajustes de la cuenta: estación por defecto, partículas decorativas e idioma
 * (Fase 7.6), y vista de catálogo por defecto (Fase 7.11).
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
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private JLabel superTitulo;
	private JLabel titulo;
	private JLabel etiquetaEstacion;
	private JComboBox<Season> estacion;
	private JCheckBox particulas;
	private JLabel etiquetaIdioma;
	private JComboBox<Idioma> idioma;
	private JLabel etiquetaVista;
	private Segmented vistaPorDefecto;
	private JButton guardar;
	private JButton cancelar;
	private JLabel error;

	public SettingsFrame(UserService userService, SessionManager sessionManager, Navigator navigator,
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
			actualizarTextos();
			precargar();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(760, 720);
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
				"[]" + Space.XXL + "[]" + Space.LG + "[]" + Space.LG + "[]" + Space.LG + "[]" + Space.LG + "[]"
						+ Space.LG + "[]"));
		panel.setOpaque(false);

		panel.add(cabecera());
		panel.add(campoEstacion());
		panel.add(campoParticulas());
		panel.add(campoIdioma());
		panel.add(campoVistaPorDefecto());

		error = Labels.error(" ");
		panel.add(error);

		panel.add(acciones());

		return panel;
	}

	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);

		superTitulo = Labels.capsAccent(" ");
		titulo = Labels.title(" ");

		titulos.add(superTitulo);
		titulos.add(titulo, "gaptop " + Space.XXS);

		panel.add(titulos);
		panel.add(new MascotSlot(MascotSlot.Tamano.PEQUENO, Pose.BIENVENIDA), "top, w 56!, h 56!");

		return panel;
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
		panel.add(estacion, "gaptop " + Space.XXS + ", height 38!");

		return panel;
	}

	private JPanel campoParticulas() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]", ""));
		panel.setOpaque(false);

		particulas = new JCheckBox();
		particulas.setOpaque(false);
		particulas.setFont(Typography.sans(Typography.BODY));

		panel.add(particulas);

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
		panel.add(idioma, "gaptop " + Space.XXS + ", height 38!");

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
		panel.add(vistaPorDefecto, "gaptop " + Space.XXS + ", w 220!");

		return panel;
	}

	private JPanel acciones() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]", ""));
		fila.setOpaque(false);

		// El texto se fija en actualizarTextos() en vez de aquí, igual que el resto
		// de los campos de esta pantalla.
		guardar = Buttons.primary(" ", e -> guardar());
		cancelar = Buttons.link(" ", e -> navigator.ir(ShowHousingsFrame.class));

		fila.add(guardar, "height 44!");
		fila.add(cancelar);

		return fila;
	}

	/**
	 * Vuelve a fijar todos los textos fijos de la pantalla en el idioma activo.
	 * Se llama desde {@code setVisible(true)}: ver la nota de clase sobre por qué
	 * no hace falta ningún mecanismo más elaborado.
	 */
	private void actualizarTextos() {

		superTitulo.setText(Textos.t("ajustes.superTitulo"));
		titulo.setText(Textos.t("ajustes.titulo"));
		etiquetaEstacion.setText(Textos.t("ajustes.estacion.label"));
		particulas.setText(Textos.t("ajustes.particulas.label"));
		etiquetaIdioma.setText(Textos.t("ajustes.idioma.label"));
		etiquetaVista.setText(Textos.t("ajustes.vista.label"));
		vistaPorDefecto.actualizarTextos(Textos.t("catalogo.vista.lista"), Textos.t("catalogo.vista.cuadricula"));
		guardar.setText(Textos.t("ajustes.guardar"));
		cancelar.setText(Textos.t("ajustes.cancelar"));

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
		particulas.setSelected(actual.isParticlesEnabled());
		idioma.setSelectedItem(actual.getLanguage());
		vistaPorDefecto.setActivo(actual.isDefaultGridView() ? CatalogFilters.VISTA_CUADRICULA : 0);

		error.setText(" ");
	}

	private void guardar() {

		Season estacionElegida = (Season) estacion.getSelectedItem();
		Idioma idiomaElegido = (Idioma) idioma.getSelectedItem();
		boolean particulasActivas = particulas.isSelected();

		boolean vistaCuadricula = vistaPorDefecto.getActivo() == CatalogFilters.VISTA_CUADRICULA;

		try {
			User actualizado = userService.updatePreferences(sessionManager.getLoggedInUser().getId(),
					EstacionPreferida.valueOf(estacionElegida.name()), particulasActivas, idiomaElegido,
					vistaCuadricula);

			// La sesión guarda el User en memoria: sin esto, la próxima vez que se
			// abriera esta pantalla precargaría los valores viejos.
			sessionManager.setLoggedInUser(actualizado);

			Theme.cambiarA(estacionElegida);
			Particulas.activar(particulasActivas);
			Textos.cambiarA(idiomaElegido == Idioma.EN ? Locale.ENGLISH : new Locale("es"));

			navigator.ir(ShowHousingsFrame.class, ShowHousingsFrame::olvidarVistaAplicada);
			Toast.mostrar(navigator.ventanaVisible(), Textos.t("ajustes.confirmacion.guardado"));

		} catch (InstanceNotFoundException ex) {
			error.setText(Textos.t("ajustes.error.usuarioNoExiste"));
		}
	}
}
