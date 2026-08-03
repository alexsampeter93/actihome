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

import fp.project.actihome.model.entities.Review;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotTheAuthorException;
import fp.project.actihome.model.exceptions.ScoreOutOfBoundsException;
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
 * Editar una reseña propia.
 *
 * <p>
 * Gemela de {@link PublishReviewFrame} y con los mismos campos —viven los dos en
 * {@link ReviewForm}—, con dos diferencias: el formulario llega <b>precargado</b>
 * con lo que ya habías escrito, y el error posible no es "ya has publicado" sino
 * {@link NotTheAuthorException}.
 *
 * <p>
 * <b>Por qué precargar importa tanto aquí.</b> Un formulario de edición vacío no
 * es neutral: si guardas sin rellenarlo todo, borras lo que había. Es
 * exactamente la familia del bug B9, que activaba las tres comidas de un
 * alojamiento al editar solo el precio. La regla que sale de aquel caso es que
 * <b>un formulario de edición carga siempre el estado real antes de dejar
 * guardar</b>.
 */
@Component
@Profile("!test")
@Lazy
public class UpdateReviewFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final transient ReviewService reviewService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;

	private Long reviewId;
	private transient Review review;

	private ReviewForm formulario;
	private JLabel tituloAlojamiento;
	private JLabel error;

	public UpdateReviewFrame(ReviewService reviewService, SessionManager sessionManager, Navigator navigator) {

		this.reviewService = reviewService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;

		initUI();
	}

	/** Prepara qué reseña se edita. La llama el {@link Navigator}. */
	public void setReviewId(Long reviewId) {
		this.reviewId = reviewId;
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

		JScrollPane scroll = new JScrollPane(exterior);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(24);
		scroll.setMinimumSize(new Dimension(0, 0));

		raiz.add(scroll, "grow");

		setContentPane(raiz);

		Foco.alPulsarEscape(this, this::volverAlDetalle);
	}

	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);

		titulos.add(Labels.capsAccent("Editar reseña"));

		tituloAlojamiento = Labels.title(" ");
		titulos.add(tituloAlojamiento, "gaptop " + Space.XXS);

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

	private void recargar() {

		if (reviewId == null) {
			return;
		}

		try {
			review = reviewService.findReview(reviewId);

		} catch (InstanceNotFoundException ex) {
			navigator.ir(ShowHousingsFrame.class);
			return;
		}

		tituloAlojamiento.setText(review.getHousing().getName());
		formulario.precargar(review);
		error.setText(" ");
	}

	private void guardar() {

		if (formulario.getTitulo().isEmpty()) {
			error.setText("La reseña necesita un título.");
			return;
		}

		try {
			reviewService.updateReview(reviewId, sessionManager.getLoggedInUser().getId(), formulario.getTitulo(),
					formulario.getCuerpo(), formulario.getUbicacion(), formulario.getServicio(), formulario.getWifi(),
					formulario.getComida(), formulario.getLimpieza());

			volverAlDetalle();

		} catch (NotTheAuthorException ex) {
			error.setText("Solo puedes editar las reseñas que has escrito tú.");

		} catch (ScoreOutOfBoundsException ex) {
			error.setText("Las valoraciones deben estar entre 0 y 5.");

		} catch (NotAuthorizedUserException ex) {
			error.setText("Solo los clientes pueden editar reseñas.");

		} catch (InstanceNotFoundException ex) {
			error.setText("La reseña ya no existe.");
		}
	}

	private void volverAlDetalle() {
		navigator.ir(ReviewDetailsFrame.class, frame -> frame.loadDetails(review));
	}
}
