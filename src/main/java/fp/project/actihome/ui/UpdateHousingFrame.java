package fp.project.actihome.ui;

import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.LessThanOneRoomException;
import fp.project.actihome.model.exceptions.NegativePrizeException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotTheOwnerException;
import fp.project.actihome.model.services.GeocodingClient;
import fp.project.actihome.model.services.HousingData;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.Rescate;
import fp.project.actihome.ui.housings.HousingForm;
import fp.project.actihome.ui.housings.HousingForm.DatosInvalidos;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Typography;

/**
 * Editar un alojamiento propio.
 *
 * <p>
 * Gemela de {@link UploadHousingFrame} y con los mismos campos —viven los dos en
 * {@link HousingForm}— salvo el código, que aquí no se pide porque es el
 * identificador público del alojamiento y {@code updateHousing} no lo modifica.
 *
 * <p>
 * <b>Aquí nació el bug B9</b>, el peor que ha tenido este proyecto: el
 * formulario no editaba las comidas pero llamaba al servicio pasando
 * {@code true} en las tres, así que cambiar solo el precio activaba desayuno,
 * comida y cena. Ya está corregido desde la Fase 3a por partida doble —el
 * servicio recibe un {@code HousingData} con cada valor nombrado y el formulario
 * precarga los valores reales—, y esta reescritura mantiene las dos defensas:
 * los datos se recargan <b>desde el servicio</b> en cada apertura, y
 * {@code HousingForm.datos()} devuelve siempre el objeto completo.
 */
@Component
@Profile("!test")
@Lazy
public class UpdateHousingFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	/**
	 * El tope de ancho del formulario, y no es Layout.CONTENIDO.
	 *
	 * <p>
	 * Es la excepción que el propio sistema prevé: la regla de acotar existe porque
	 * un campo de texto muy ancho se lee peor, y aquí el ancho de más no alarga
	 * ningún campo — lo convierte en <b>otra columna</b>, que es el mismo caso que
	 * las listas y las rejillas. Con los 940 de CONTENIDO, las tres columnas del
	 * formulario nunca cabían y {@code Columnas} lo dejaba siempre en dos, con lo
	 * que el alto volvía a irse de la ventana.
	 */
	private static final int ANCHO_DEL_FORMULARIO = 1180;

	private final transient HousingService housingService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	/** Buscador de coordenadas para el botón "Localizar" del formulario (F17). */
	private final transient GeocodingClient geocodingClient;

	private Long housingId;
	private transient Housing housing;

	private HousingForm formulario;
	private JLabel superTitulo;
	private JLabel subtitulo;
	private JLabel error;
	private JButton botonGuardar;
	private JButton botonCancelar;

	public UpdateHousingFrame(HousingService housingService, SessionManager sessionManager, Navigator navigator,
			HeaderPanel headerPanel, GeocodingClient geocodingClient) {

		this.housingService = housingService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;
		this.geocodingClient = geocodingClient;

		initUI();
	}

	/** Prepara qué alojamiento se edita. La llama el {@link Navigator}. */
	public void setHousingId(Long housingId) {
		this.housingId = housingId;
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			headerPanel.refresh();
			actualizarTextosFijos();
			recargar();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(1300, 840);
		setLocationRelativeTo(null);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[grow,fill]"));

		JPanel exterior = new JPanel(new MigLayout("wrap 1, " + Space.insetsLaterales(Space.GIANT, Space.GIANT),
				"[grow,fill]", Space.margen(Space.XXL) + "[]" + Space.aire(Space.LG) + "[]" + Space.aire(Space.XS) + "[]"
						+ Space.aire(Space.MD) + "[]" + Space.margen(Space.XXL)));
		exterior.setOpaque(false);

		exterior.add(cabecera(), Layout.ancho(Layout.CONTENIDO) + ", alignx center");

		formulario = new HousingForm(false, geocodingClient);
		exterior.add(formulario, Layout.ancho(ANCHO_DEL_FORMULARIO) + ", alignx center");

		error = Labels.error(" ");
		exterior.add(error, Layout.ancho(Layout.CONTENIDO) + ", alignx center");

		exterior.add(acciones(), Layout.ancho(Layout.CONTENIDO) + ", alignx center");

		raiz.add(headerPanel, "growx");
		raiz.add(Rescate.envolver(exterior), "grow");

		setContentPane(raiz);

		Foco.alPulsarEscape(this, this::volverAlDetalle);
	}

	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);
		superTitulo = Labels.capsAccent(Textos.t("alojamientoEditar.titulo"));
		titulos.add(superTitulo);

		subtitulo = Labels.title(" ");
		titulos.add(subtitulo, "gaptop " + Space.XXS);

		panel.add(titulos);
		panel.add(new MascotSlot(MascotSlot.Tamano.PEQUENO, Pose.BIENVENIDA), "top, w 56!, h 56!");

		return panel;
	}

	private JPanel acciones() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]", ""));
		fila.setOpaque(false);

		botonGuardar = Buttons.primary(Textos.t("ajustes.guardar"), e -> guardar());
		fila.add(botonGuardar, "height " + Typography.altoDeBoton() + "!");
		botonCancelar = Buttons.link(Textos.t("ajustes.cancelar"), e -> volverAlDetalle());
		fila.add(botonCancelar);

		return fila;
	}

	private void actualizarTextosFijos() {

		superTitulo.setText(Textos.t("alojamientoEditar.titulo"));
		formulario.actualizarTextos();
		botonGuardar.setText(Textos.t("ajustes.guardar"));
		botonCancelar.setText(Textos.t("ajustes.cancelar"));
	}

	/**
	 * Recarga el alojamiento desde el servicio, no desde el objeto que traía el
	 * catálogo: si acaba de cambiar de dueño en un intercambio, el que hay en
	 * memoria ya está desactualizado.
	 */
	private void recargar() {

		if (housingId == null) {
			return;
		}

		try {
			housing = housingService.findHousing(housingId);

		} catch (InstanceNotFoundException ex) {
			navigator.ir(ShowHousingsFrame.class);
			return;
		}

		subtitulo.setText(housing.getName());
		formulario.precargar(housing);
		error.setText(" ");
	}

	private void guardar() {

		try {
			HousingData datos = formulario.datosCon(housing.getHousingCode());
			formulario.guardarFotoSiHaceFalta(housing.getHousingCode());

			housingService.updateHousing(housingId, sessionManager.getLoggedInUser().getId(), datos);

			formulario.guardarFotosDeGaleria(housingId, housing.getHousingCode(),
					sessionManager.getLoggedInUser().getId(), housingService);

			volverAlDetalle();

		} catch (DatosInvalidos ex) {
			error.setText(ex.getMessage());

		} catch (IOException ex) {
			error.setText(Textos.t("alojamientoForm.foto.error.noSeGuarda"));

		} catch (LessThanOneRoomException ex) {
			error.setText(Textos.t("alojamientoForm.error.sinHabitaciones"));

		} catch (NegativePrizeException ex) {
			error.setText(Textos.t("alojamientoForm.error.precioNegativo"));

		} catch (NotTheOwnerException ex) {
			error.setText(Textos.t("alojamientoEditar.error.noEresTitular"));

		} catch (NotAuthorizedUserException ex) {
			error.setText(Textos.t("alojamientoEditar.error.soloAdmin"));

		} catch (InstanceNotFoundException ex) {
			error.setText(Textos.t("alojamientoEditar.error.yaNoExiste"));
		}
	}

	private void volverAlDetalle() {
		navigator.ir(HousingDetailsFrame.class, frame -> frame.loadDetails(housing));
	}
}
