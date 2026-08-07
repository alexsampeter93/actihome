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

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.Review;
import fp.project.actihome.model.exceptions.AlreadyPublishedException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.MustHaveStayedException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotTheAuthorException;
import fp.project.actihome.model.exceptions.ScoreOutOfBoundsException;
import fp.project.actihome.model.services.HousingService;
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
 * Publicar una reseña sobre un alojamiento.
 *
 * <p>
 * Los campos viven en {@link ReviewForm}, compartido con
 * {@link UpdateReviewFrame}. Aquí solo está lo propio de publicar: el
 * encabezado, la llamada al servicio y los mensajes de error.
 *
 * <p>
 * <b>Mensajes por excepción, no un genérico</b> (bug B11). Publicar puede
 * fallar por cinco motivos distintos y el servicio los distingue con cinco
 * excepciones; la versión anterior los capturaba todos con
 * {@code catch (Exception)} y decía siempre "Error en los datos", que no ayuda a
 * arreglar nada. El caso que más importa es
 * {@link AlreadyPublishedException}: no es un error de escritura, es que ya
 * habías opinado sobre esta casa, y lo que toca entonces es editar la reseña que
 * ya existe. {@link MustHaveStayedException} (Fase 7.5.4) es el más nuevo:
 * antes cualquier CUSTOMER podía puntuar cualquier alojamiento sin haberlo
 * pisado.
 */
@Component
@Profile("!test")
@Lazy
public class PublishReviewFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	/**
	 * El tope de ancho del formulario: las dos columnas de {@code ReviewForm} más su
	 * separación. Con Layout.FORMULARIO (440) no cabían dos y {@code Columnas} las
	 * apilaba siempre, que es justo lo que se venía a evitar.
	 */
	private static final int ANCHO_DEL_FORMULARIO = 780;


	private final transient ReviewService reviewService;
	private final transient HousingService housingService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;

	private Long housingId;
	private transient Housing housing;

	private ReviewForm formulario;
	private JLabel superTitulo;
	private JLabel tituloAlojamiento;
	private JLabel error;
	private JButton botonPublicar;
	private JButton botonCancelar;

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

		// La ventana es alta y el formulario también: si la pantalla del usuario es
		// pequeña, el scroll lo absorbe aquí en lugar de recortar los botones.
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

		Foco.alPulsarEscape(this, this::volverAlListado);
	}

	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);

		superTitulo = Labels.capsAccent(Textos.t("resenas.publicar"));
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

		botonPublicar = Buttons.primary(Textos.t("resenas.publicar"), e -> publicar());
		fila.add(botonPublicar, "height " + Typography.altoDeBoton() + "!");
		botonCancelar = Buttons.link(Textos.t("ajustes.cancelar"), e -> volverAlListado());
		fila.add(botonCancelar);

		return fila;
	}

	private void actualizarTextosFijos() {

		superTitulo.setText(Textos.t("resenas.publicar"));
		formulario.actualizarTextos();
		botonPublicar.setText(Textos.t("resenas.publicar"));
		botonCancelar.setText(Textos.t("ajustes.cancelar"));
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
			error.setText(Textos.t("resenaForm.error.sinTitulo"));
			return;
		}

		try {
			Review publicada = reviewService.publishReview(sessionManager.getLoggedInUser().getId(), housingId,
					formulario.getTitulo(), formulario.getCuerpo(), formulario.getUbicacion(), formulario.getServicio(),
					formulario.getWifi(), formulario.getComida(), formulario.getLimpieza());

			guardarFotoSiHaceFalta(publicada);
			volverAlListado();

		} catch (AlreadyPublishedException ex) {
			error.setText(Textos.t("resenaForm.error.yaPublicada"));

		} catch (MustHaveStayedException ex) {
			error.setText(Textos.t("resenaForm.error.debesHaberteAlojado"));

		} catch (ScoreOutOfBoundsException ex) {
			// Con el selector de estrellas no debería poder ocurrir —el control solo
			// produce valores de 0 a 5—, pero la regla la impone el servicio y la
			// pantalla no debe dar por hecho que la conoce.
			error.setText(Textos.t("resenaForm.error.notasFueraDeRango"));

		} catch (NotAuthorizedUserException ex) {
			error.setText(Textos.t("resenaForm.error.soloClientesPublican"));

		} catch (InstanceNotFoundException ex) {
			error.setText(Textos.t("resenaForm.error.alojamientoNoDisponible"));
		}
	}

	/**
	 * Guarda la foto elegida, si la hay, y la asocia a la reseña ya publicada.
	 *
	 * <p>
	 * Va después de publicar, nunca antes: el nombre del archivo (F15, ver
	 * {@link ReviewPhotos}) no depende del id de la reseña, pero asociarla sí
	 * necesita que la reseña ya exista. Un fallo aquí no deshace la publicación
	 * —ya está hecha, y es lo importante— así que se avisa con un
	 * {@link Toast} en vez de bloquear la pantalla con un error.
	 */
	private void guardarFotoSiHaceFalta(Review review) {

		if (formulario.getFotoElegida() == null) {
			return;
		}

		try {
			String nombre = ReviewPhotos.nombreNuevo();
			ReviewPhotos.guardar(nombre, formulario.getFotoElegida());
			reviewService.setReviewImage(review.getId(), sessionManager.getLoggedInUser().getId(), nombre);

		} catch (IOException | NotAuthorizedUserException | InstanceNotFoundException | NotTheAuthorException ex) {
			Toast.mostrar(this, Textos.t("resenaForm.foto.error.noSeGuarda"));
		}
	}

	private void volverAlListado() {
		navigator.ir(ShowReviewsFrame.class, frame -> frame.setHousingId(housingId));
	}
}
