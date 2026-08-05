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

import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.LessThanOneRoomException;
import fp.project.actihome.model.exceptions.NegativePrizeException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
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
	private JLabel superTitulo;
	private JLabel titulo;
	private JButton botonPublicar;
	private JButton botonCancelar;

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
			actualizarTextosFijos();
			formulario.limpiar();
			error.setText(" ");
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(1120, 800);
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
		// holgada cabe entero; el scroll de rescate está por si no.
		raiz.add(headerPanel, "growx");
		raiz.add(Rescate.envolver(exterior), "grow");

		setContentPane(raiz);

		Foco.alPulsarEscape(this, () -> navigator.ir(ShowHousingsFrame.class));
	}

	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);
		superTitulo = Labels.capsAccent(Textos.t("alojamientoAlta.superTitulo"));
		titulos.add(superTitulo);
		titulo = Labels.title(Textos.t("alojamientoAlta.titulo"));
		titulos.add(titulo, "gaptop " + Space.XXS);

		panel.add(titulos);
		panel.add(new MascotSlot(MascotSlot.Tamano.PEQUENO, Pose.BIENVENIDA), "top, w 56!, h 56!");

		return panel;
	}

	private JPanel acciones() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]", ""));
		fila.setOpaque(false);

		botonPublicar = Buttons.primary(Textos.t("alojamientoAlta.publicar"), e -> publicar());
		fila.add(botonPublicar, "height " + Typography.altoDeBoton() + "!");
		botonCancelar = Buttons.link(Textos.t("ajustes.cancelar"), e -> navigator.ir(ShowHousingsFrame.class));
		fila.add(botonCancelar);

		return fila;
	}

	private void actualizarTextosFijos() {

		superTitulo.setText(Textos.t("alojamientoAlta.superTitulo"));
		titulo.setText(Textos.t("alojamientoAlta.titulo"));
		formulario.actualizarTextos();
		botonPublicar.setText(Textos.t("alojamientoAlta.publicar"));
		botonCancelar.setText(Textos.t("ajustes.cancelar"));
	}

	private void publicar() {

		try {
			HousingData datos = formulario.datos();

			// Después de leer los datos y antes de enviarlos: si el resto del
			// formulario no fuera válido, datos() ya habría lanzado DatosInvalidos sin
			// gastar tiempo reduciendo una foto que no se iba a usar.
			formulario.guardarFotoSiHaceFalta(datos.getHousingCode());

			housingService.uploadHousing(datos, sessionManager.getLoggedInUser().getId());

			navigator.ir(ShowHousingsFrame.class);

		} catch (DatosInvalidos ex) {
			// El formulario ya sabe qué campo falla y trae el mensaje escrito.
			error.setText(ex.getMessage());

		} catch (IOException ex) {
			error.setText(Textos.t("alojamientoForm.foto.error.noSeGuarda"));

		} catch (DuplicateInstanceException ex) {
			error.setText(Textos.t("alojamientoForm.error.codigoDuplicado"));

		} catch (LessThanOneRoomException ex) {
			error.setText(Textos.t("alojamientoForm.error.sinHabitaciones"));

		} catch (NegativePrizeException ex) {
			error.setText(Textos.t("alojamientoForm.error.precioNegativo"));

		} catch (NotAuthorizedUserException ex) {
			error.setText(Textos.t("alojamientoAlta.error.soloAdmin"));

		} catch (InstanceNotFoundException ex) {
			error.setText(Textos.t("alojamientoForm.error.sesionNoValida"));
		}
	}
}
