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
import fp.project.actihome.model.exceptions.AlreadyPublishedException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.ScoreOutOfBoundsException;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.model.services.ReviewService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.reviews.ReviewForm;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;

/**
 * Publicar una reseña sobre un alojamiento.
 *
 * <p>
 * Los campos viven en {@link ReviewForm}, compartido con
 * {@link UpdateReviewFrame}. Aquí solo está lo propio de publicar: el
 * encabezado, la llamada al servicio y los mensajes de error.
 *
 * <p>
 * <b>Mensajes por excepción, no un genérico</b> (bug B11). Publicar puede
 * fallar por cuatro motivos distintos y el servicio los distingue con cuatro
 * excepciones; la versión anterior los capturaba todos con
 * {@code catch (Exception)} y decía siempre "Error en los datos", que no ayuda a
 * arreglar nada. El caso que más importa es
 * {@link AlreadyPublishedException}: no es un error de escritura, es que ya
 * habías opinado sobre esta casa, y lo que toca entonces es editar la reseña que
 * ya existe.
 */
@Component
@Profile("!test")
@Lazy
public class PublishReviewFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final transient ReviewService reviewService;
	private final transient HousingService housingService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;

	private Long housingId;
	private transient Housing housing;

	private ReviewForm formulario;
	private JLabel tituloAlojamiento;
	private JLabel error;

	public PublishReviewFrame(ReviewService reviewService, HousingService housingService,
			SessionManager sessionManager, Navigator navigator) {

		this.reviewService = reviewService;
		this.housingService = housingService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;

		initUI();
	}

	/** Prepara sobre qué alojamiento se opina. La llama el {@link Navigator}. */
	public void setHousingId(Long housingId) {
		this.housingId = housingId;
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			recargar();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(760, 800);
		setLocationRelativeTo(null);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[grow,fill]"));

		JPanel exterior = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.GIANT), "[grow,fill]",
				"[]" + Space.XL + "[]" + Space.LG + "[]" + Space.XL + "[]"));
		exterior.setOpaque(false);

		exterior.add(cabecera(), Layout.ancho(Layout.FORMULARIO) + ", alignx center");

		formulario = new ReviewForm();
		exterior.add(formulario, Layout.ancho(Layout.FORMULARIO) + ", alignx center");

		error = Labels.error(" ");
		exterior.add(error, Layout.ancho(Layout.FORMULARIO) + ", alignx center");

		exterior.add(acciones(), Layout.ancho(Layout.FORMULARIO) + ", alignx center");

		// La ventana es alta y el formulario también: si la pantalla del usuario es
		// pequeña, el scroll lo absorbe aquí en lugar de recortar los botones.
		JScrollPane scroll = new JScrollPane(exterior);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(24);
		scroll.setMinimumSize(new Dimension(0, 0));

		raiz.add(scroll, "grow");

		setContentPane(raiz);

		Foco.alPulsarEscape(this, this::volverAlListado);
	}

	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);

		titulos.add(Labels.capsAccent("Publicar reseña"));

		tituloAlojamiento = Labels.title(" ");
		titulos.add(tituloAlojamiento, "gaptop " + Space.XXS);

		panel.add(titulos);
		panel.add(new MascotSlot(MascotSlot.Tamano.PEQUENO, Pose.BIENVENIDA), "top, w 56!, h 56!");

		return panel;
	}

	private JPanel acciones() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]", ""));
		fila.setOpaque(false);

		fila.add(Buttons.primary("Publicar reseña", e -> publicar()), "height 44!");
		fila.add(Buttons.link("Cancelar", e -> volverAlListado()));

		return fila;
	}

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

		tituloAlojamiento.setText(housing.getName());
		error.setText(" ");
	}

	private void publicar() {

		if (formulario.getTitulo().isEmpty()) {
			error.setText("Ponle un título a tu reseña.");
			return;
		}

		try {
			reviewService.publishReview(sessionManager.getLoggedInUser().getId(), housingId, formulario.getTitulo(),
					formulario.getCuerpo(), formulario.getUbicacion(), formulario.getServicio(), formulario.getWifi(),
					formulario.getComida(), formulario.getLimpieza());

			volverAlListado();

		} catch (AlreadyPublishedException ex) {
			error.setText("Ya has publicado una reseña de este alojamiento. Puedes editarla desde el listado.");

		} catch (ScoreOutOfBoundsException ex) {
			// Con el selector de estrellas no debería poder ocurrir —el control solo
			// produce valores de 0 a 5—, pero la regla la impone el servicio y la
			// pantalla no debe dar por hecho que la conoce.
			error.setText("Las valoraciones deben estar entre 0 y 5.");

		} catch (NotAuthorizedUserException ex) {
			error.setText("Solo los clientes pueden publicar reseñas.");

		} catch (InstanceNotFoundException ex) {
			error.setText("El alojamiento ya no está disponible.");
		}
	}

	private void volverAlListado() {
		navigator.ir(ShowReviewsFrame.class, frame -> frame.setHousingId(housingId));
	}
}
