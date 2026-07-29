package fp.project.actihome.ui;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

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
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.housings.HousingForm;
import fp.project.actihome.ui.housings.HousingForm.DatosInvalidos;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;

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

	private final transient HousingService housingService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private Long housingId;
	private transient Housing housing;

	private HousingForm formulario;
	private JLabel subtitulo;
	private JLabel error;

	public UpdateHousingFrame(HousingService housingService, SessionManager sessionManager, Navigator navigator,
			HeaderPanel headerPanel) {

		this.housingService = housingService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

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
			recargar();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(1120, 800);
		setMinimumSize(new Dimension(940, 660));
		setLocationRelativeTo(null);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[grow,fill]"));

		JPanel exterior = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.XXL, Space.GIANT, Space.XXL, Space.GIANT), "[grow,fill]",
				"[]" + Space.LG + "[]" + Space.XS + "[]" + Space.MD + "[]"));
		exterior.setOpaque(false);

		exterior.add(cabecera(), Layout.ancho(Layout.CONTENIDO) + ", alignx center");

		formulario = new HousingForm(false);
		exterior.add(formulario, Layout.ancho(Layout.CONTENIDO) + ", alignx center");

		error = Labels.error(" ");
		exterior.add(error, Layout.ancho(Layout.CONTENIDO) + ", alignx center");

		exterior.add(acciones(), Layout.ancho(Layout.CONTENIDO) + ", alignx center");

		JScrollPane scroll = new JScrollPane(exterior);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(24);
		scroll.setMinimumSize(new Dimension(0, 0));

		raiz.add(headerPanel, "growx");
		raiz.add(scroll, "grow");

		setContentPane(raiz);
	}

	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);
		titulos.add(Labels.capsAccent("Editar alojamiento"));

		subtitulo = Labels.title(" ");
		titulos.add(subtitulo, "gaptop " + Space.XXS);

		panel.add(titulos);
		panel.add(new MascotSlot(MascotSlot.Tamano.PEQUENO, Pose.BIENVENIDA), "top, w 56!, h 56!");

		return panel;
	}

	private JPanel acciones() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]", ""));
		fila.setOpaque(false);

		fila.add(Buttons.primary("Guardar cambios", e -> guardar()), "height 44!");
		fila.add(Buttons.link("Cancelar", e -> volverAlDetalle()));

		return fila;
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
			housingService.updateHousing(housingId, sessionManager.getLoggedInUser().getId(),
					formulario.datosCon(housing.getHousingCode()));

			volverAlDetalle();

		} catch (DatosInvalidos ex) {
			error.setText(ex.getMessage());

		} catch (LessThanOneRoomException ex) {
			error.setText("El alojamiento debe tener al menos una habitación.");

		} catch (NegativePrizeException ex) {
			error.setText("El precio por noche no puede ser negativo.");

		} catch (NotTheOwnerException ex) {
			error.setText("Solo puedes editar los alojamientos de los que eres titular.");

		} catch (NotAuthorizedUserException ex) {
			error.setText("Solo los administradores pueden editar alojamientos.");

		} catch (InstanceNotFoundException ex) {
			error.setText("El alojamiento ya no existe.");
		}
	}

	private void volverAlDetalle() {
		navigator.ir(HousingDetailsFrame.class, frame -> frame.loadDetails(housing));
	}
}
