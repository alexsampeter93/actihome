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

import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.LessThanOneRoomException;
import fp.project.actihome.model.exceptions.NegativePrizeException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
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
 * Dar de alta un alojamiento (rol ADMIN).
 *
 * <p>
 * Los campos viven en {@link HousingForm}, compartido con
 * {@link UpdateHousingFrame}. Aquí solo está lo propio del alta: el encabezado,
 * la llamada al servicio y los mensajes de error.
 *
 * <p>
 * <b>Mensajes por excepción</b> (bug B11). El servicio distingue cinco motivos
 * de fallo y la versión anterior los capturaba todos con {@code catch (Exception)}
 * para decir siempre "Error en los datos". El caso que más importa es
 * {@link DuplicateInstanceException}: no es un error de escritura, es que ese
 * código de alojamiento ya existe, y lo único que hay que cambiar es ese campo.
 */
@Component
@Profile("!test")
@Lazy
public class UploadHousingFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final transient HousingService housingService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private HousingForm formulario;
	private JLabel error;

	public UploadHousingFrame(HousingService housingService, SessionManager sessionManager, Navigator navigator,
			HeaderPanel headerPanel) {

		this.housingService = housingService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			headerPanel.refresh();
			formulario.limpiar();
			error.setText(" ");
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

		formulario = new HousingForm(true);
		exterior.add(formulario, Layout.ancho(Layout.CONTENIDO) + ", alignx center");

		error = Labels.error(" ");
		exterior.add(error, Layout.ancho(Layout.CONTENIDO) + ", alignx center");

		exterior.add(acciones(), Layout.ancho(Layout.CONTENIDO) + ", alignx center");

		// El formulario de alojamiento es el más largo de la aplicación. En una pantalla
		// holgada cabe entero; el scroll está por si no.
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
		titulos.add(Labels.capsAccent("Nuevo alojamiento"));
		titulos.add(Labels.title("Publica tu estancia"), "gaptop " + Space.XXS);

		panel.add(titulos);
		panel.add(new MascotSlot(MascotSlot.Tamano.PEQUENO, Pose.BIENVENIDA), "top, w 56!, h 56!");

		return panel;
	}

	private JPanel acciones() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]", ""));
		fila.setOpaque(false);

		fila.add(Buttons.primary("Publicar alojamiento", e -> publicar()), "height 44!");
		fila.add(Buttons.link("Cancelar", e -> navigator.ir(ShowHousingsFrame.class)));

		return fila;
	}

	private void publicar() {

		try {
			housingService.uploadHousing(formulario.datos(), sessionManager.getLoggedInUser().getId());

			navigator.ir(ShowHousingsFrame.class);

		} catch (DatosInvalidos ex) {
			// El formulario ya sabe qué campo falla y trae el mensaje escrito.
			error.setText(ex.getMessage());

		} catch (DuplicateInstanceException ex) {
			error.setText("Ya existe un alojamiento con ese código. Usa otro.");

		} catch (LessThanOneRoomException ex) {
			error.setText("El alojamiento debe tener al menos una habitación.");

		} catch (NegativePrizeException ex) {
			error.setText("El precio por noche no puede ser negativo.");

		} catch (NotAuthorizedUserException ex) {
			error.setText("Solo los administradores pueden publicar alojamientos.");

		} catch (InstanceNotFoundException ex) {
			error.setText("Tu sesión ya no es válida. Vuelve a entrar.");
		}
	}
}
