package fp.project.actihome.ui;

import java.awt.Dimension;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Review;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.ReviewService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.ScoreBar;
import fp.project.actihome.ui.components.ScoreDisc;
import fp.project.actihome.ui.components.WrappingText;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Typography;

/**
 * Una reseña completa.
 *
 * <p>
 * Título y disco de nota arriba, autoría, el cuerpo entero —aquí no se recorta,
 * a diferencia del listado— y las cinco sub-notas como barras.
 *
 * <p>
 * <b>Por qué barras y no cifras.</b> Cinco números obligan a leerlos y
 * compararlos mentalmente uno a uno; cinco barras alineadas se comparan de un
 * vistazo, porque la longitud se percibe sin tener que interpretarla. Es el
 * mismo dato codificado de una forma que cuesta menos leer, y es exactamente el
 * trabajo de una interfaz. En el listado, en cambio, van como cifras: allí lo
 * que se compara son reseñas entre sí, y las barras ocuparían el alto de cada
 * fila.
 */
@Component
@Profile("!test")
@Lazy
public class ReviewDetailsFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy",
			new Locale("es", "ES"));

	private final transient ReviewService reviewService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private Long reviewId;
	private transient Review review;

	private JPanel contenido;

	public ReviewDetailsFrame(ReviewService reviewService, SessionManager sessionManager, Navigator navigator,
			HeaderPanel headerPanel) {

		this.reviewService = reviewService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	/**
	 * Prepara qué reseña mostrar. La llama el {@link Navigator}.
	 *
	 * <p>
	 * Guarda solo el identificador y recarga en cada apertura: si vienes de
	 * editarla, el objeto que traía el listado en memoria ya está desactualizado.
	 */
	public void loadDetails(Review review) {
		this.reviewId = review.getId();
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
		setSize(1000, 780);
		setLocationRelativeTo(null);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[grow,fill]"));

		contenido = new JPanel();
		contenido.setOpaque(false);

		raiz.add(headerPanel, "growx");
		raiz.add(contenido, "grow");

		setContentPane(raiz);

		Foco.alPulsarEscape(this,
				() -> navigator.ir(ShowReviewsFrame.class, frame -> frame.setHousingId(review.getHousing().getId())));
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

		reconstruir();
	}

	private void reconstruir() {

		contenido.removeAll();
		contenido.setLayout(new MigLayout("fill, " + Space.insets(Space.XL, Space.HUGE, Space.XL, Space.HUGE),
				"[grow,fill]", "[grow,fill]"));

		// Una sola columna centrada, y dentro todo alineado a la izquierda. Es la
		// diferencia entre un margen izquierdo recto y uno dentado: si cada bloque se
		// centrase por su cuenta según su propio ancho máximo, el cuerpo del texto
		// arrancaría más adentro que el título.
		JPanel columna = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.LG + "[]" + Space.MD + "[]" + Space.XXL + "[]" + Space.XXL + "[]push[]"));
		columna.setOpaque(false);

		columna.add(migaDePan(), "growx");
		columna.add(cabecera(), "growx");
		columna.add(new WrappingText(review.getBody()), "growx, " + Layout.ancho(Layout.TEXTO));
		columna.add(Hairline.horizontal(), "growx, h 1!");
		columna.add(subNotas(), "growx, " + Layout.ancho(Layout.TEXTO));
		columna.add(acciones(), "growx");

		contenido.add(columna, "grow, " + Layout.anchoCentrado(Layout.CONTENIDO));

		contenido.revalidate();
		contenido.repaint();
	}

	private JPanel migaDePan() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XS + "[]" + Space.XS + "[]", "[]"));
		panel.setOpaque(false);

		panel.add(Buttons.link("Reseñas de " + review.getHousing().getName(),
				e -> navigator.ir(ShowReviewsFrame.class, frame -> frame.setHousingId(review.getHousing().getId()))));
		panel.add(Labels.muted("›"));
		panel.add(Labels.muted(review.getTitle()));

		return panel;
	}

	/** Disco de nota grande a la izquierda, título y autoría a la derecha. */
	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XL + "[grow,fill]", "[]"));
		panel.setOpaque(false);

		panel.add(new ScoreDisc(review.getTotalScore(), ScoreDisc.Tamano.GRANDE), "w 64!, h 64!, aligny center");

		JPanel texto = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XXS + "[]"));
		texto.setOpaque(false);

		JLabel titulo = Labels.title(review.getTitle());
		titulo.setFont(Typography.serifMedium(28f));
		texto.add(titulo);

		texto.add(Labels.muted("por " + review.getAuthor().getUsername() + " · "
				+ FECHA.format(review.getPublicationDate())));

		panel.add(texto, "aligny center");

		return panel;
	}

	private JPanel subNotas() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.SM + "[]" + Space.SM + "[]" + Space.SM + "[]" + Space.SM + "[]"));
		panel.setOpaque(false);

		panel.add(new ScoreBar("Ubicación", review.getLocationScore()), "growx");
		panel.add(new ScoreBar("Servicio", review.getServiceScore()), "growx");
		panel.add(new ScoreBar("Wifi", review.getWifiScore()), "growx");
		panel.add(new ScoreBar("Comida", review.getFoodScore()), "growx");
		panel.add(new ScoreBar("Limpieza", review.getCleaningScore()), "growx");

		return panel;
	}

	/** "Actualizar reseña" solo para quien la escribió. */
	private JPanel acciones() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]push[]", "[]"));
		panel.setOpaque(false);

		if (esSuya()) {
			panel.add(Buttons.secondary("Actualizar reseña",
					e -> navigator.ir(UpdateReviewFrame.class, frame -> frame.setReviewId(review.getId()))));
		}

		panel.add(Buttons.link("Volver a las reseñas →",
				e -> navigator.ir(ShowReviewsFrame.class, frame -> frame.setHousingId(review.getHousing().getId()))));

		return panel;
	}

	private boolean esSuya() {

		User usuario = sessionManager.getLoggedInUser();

		return usuario != null && usuario.getId().equals(review.getAuthor().getId());
	}
}
