package fp.project.actihome.ui;

import java.awt.Dimension;
import java.io.IOException;

import javax.swing.JButton;
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
import fp.project.actihome.ui.components.Toast;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.reviews.ReviewForm;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.ReviewPhotos;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Typography;
import fp.project.actihome.ui.components.Rescate;

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

	/**
	 * El tope de ancho del formulario: las dos columnas de {@code ReviewForm} más su
	 * separación. Con Layout.FORMULARIO (440) no cabían dos y {@code Columnas} las
	 * apilaba siempre, que es justo lo que se venía a evitar.
	 */
	private static final int ANCHO_DEL_FORMULARIO = 780;


	private final transient ReviewService reviewService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;

	private Long reviewId;
	private transient Review review;

	private ReviewForm formulario;
	private JLabel superTitulo;
	private JLabel tituloAlojamiento;
	private JLabel error;
	private JButton botonGuardar;
	private JButton botonCancelar;

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
			actualizarTextosFijos();
			recargar();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(1000, 800);
		setLocationRelativeTo(null);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[grow,fill]"));

		JPanel exterior = new JPanel(new MigLayout("wrap 1, " + Space.insetsLaterales(Space.GIANT, Space.GIANT),
				"[grow,fill]", Space.margen(Space.GIANT) + "[]" + Space.aire(Space.XL) + "[]" + Space.aire(Space.LG)
						+ "[]" + Space.aire(Space.XL) + "[]" + Space.margen(Space.GIANT)));
		exterior.setOpaque(false);

		exterior.add(cabecera(), Layout.ancho(Layout.FORMULARIO) + ", alignx center");

		formulario = new ReviewForm();
		exterior.add(formulario, Layout.ancho(ANCHO_DEL_FORMULARIO) + ", alignx center");

		error = Labels.error(" ");
		exterior.add(error, Layout.ancho(Layout.FORMULARIO) + ", alignx center");

		exterior.add(acciones(), Layout.ancho(Layout.FORMULARIO) + ", alignx center");

		// Rescate y no un JScrollPane crudo. La diferencia esta en el Scrollable que
		// Rescate envuelve: sin el, el contenido conserva su ancho preferido en vez de
		// seguir el del visor, y con la barra horizontal desactivada lo que se sale por
		// la derecha NO SE PUEDE ALCANZAR NUNCA, por mucho que se agrande la ventana.
		JScrollPane scroll = Rescate.envolver(exterior);
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

		superTitulo = Labels.capsAccent(Textos.t("resenaEditar.titulo"));
		titulos.add(superTitulo);

		tituloAlojamiento = Labels.title(" ");
		titulos.add(tituloAlojamiento, "gaptop " + Space.XXS);

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

		superTitulo.setText(Textos.t("resenaEditar.titulo"));
		formulario.actualizarTextos();
		botonGuardar.setText(Textos.t("ajustes.guardar"));
		botonCancelar.setText(Textos.t("ajustes.cancelar"));
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
			error.setText(Textos.t("resenaEditar.error.sinTitulo"));
			return;
		}

		try {
			Review actualizada = reviewService.updateReview(reviewId, sessionManager.getLoggedInUser().getId(),
					formulario.getTitulo(), formulario.getCuerpo(), formulario.getUbicacion(), formulario.getServicio(),
					formulario.getWifi(), formulario.getComida(), formulario.getLimpieza());

			guardarFotoSiHaceFalta(actualizada);
			volverAlDetalle();

		} catch (NotTheAuthorException ex) {
			error.setText(Textos.t("resenaEditar.error.noEresAutor"));

		} catch (ScoreOutOfBoundsException ex) {
			error.setText(Textos.t("resenaForm.error.notasFueraDeRango"));

		} catch (NotAuthorizedUserException ex) {
			error.setText(Textos.t("resenaEditar.error.soloClientesEditan"));

		} catch (InstanceNotFoundException ex) {
			error.setText(Textos.t("resenaEditar.error.yaNoExiste"));
		}
	}

	/**
	 * Guarda, reemplaza o quita la foto de la reseña, según lo que haya tocado
	 * el formulario (F15). Ver la nota gemela en
	 * {@code PublishReviewFrame.guardarFotoSiHaceFalta}: un fallo aquí no
	 * deshace el resto de la edición, ya guardada, así que se avisa con un
	 * {@link Toast} en vez de bloquear la pantalla.
	 */
	private void guardarFotoSiHaceFalta(Review actualizada) {

		try {
			if (formulario.getFotoElegida() != null) {

				String nombre = ReviewPhotos.nombreNuevo();
				ReviewPhotos.guardar(nombre, formulario.getFotoElegida());
				reviewService.setReviewImage(actualizada.getId(), sessionManager.getLoggedInUser().getId(), nombre);

			} else if (formulario.getImagenExistente() == null && actualizada.getImage() != null) {
				// Tenía foto y se pulsó "Quitar": la reseña se queda sin ninguna.
				reviewService.setReviewImage(actualizada.getId(), sessionManager.getLoggedInUser().getId(), null);
			}

		} catch (IOException | NotAuthorizedUserException | InstanceNotFoundException | NotTheAuthorException ex) {
			Toast.mostrar(this, Textos.t("resenaForm.foto.error.noSeGuarda"));
		}
	}

	private void volverAlDetalle() {
		navigator.volver(ReviewDetailsFrame.class, frame -> frame.loadDetails(review));
	}
}
