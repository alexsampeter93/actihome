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
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.NotTheOwnerException;
import fp.project.actihome.model.exceptions.TranslationNotConfiguredException;
import fp.project.actihome.model.services.ReviewService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.Rescate;
import fp.project.actihome.ui.components.ScoreBar;
import fp.project.actihome.ui.components.ScoreDisc;
import fp.project.actihome.ui.components.WrappingText;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.BrandAssets;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
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

	/** Ver la nota gemela en {@code ReviewRow.formatoFecha()}: no basta con el {@link Locale}. */
	private static DateTimeFormatter formatoFecha() {

		return Textos.idioma().getLanguage().equals("en")
				? DateTimeFormatter.ofPattern("MMMM d, yyyy", Textos.idioma())
				: DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Textos.idioma());
	}

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
		// Rescate: hasta F15 esta pantalla nunca necesitó red de seguridad, porque
		// título, cuerpo y cinco barras cabían siempre. Con la foto adjunta (hasta
		// 260px) y la respuesta del propietario, el contenido ya puede superar una
		// ventana pequeña, y sin esto las últimas barras de subNotas quedaban
		// dibujadas por debajo del borde: no cortadas, inalcanzables.
		raiz.add(Rescate.envolver(contenido), "grow");

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

		boolean conFoto = review.getImage() != null;
		boolean conRespuesta = review.getOwnerResponse() != null || esPropietarioDelAlojamiento();

		// El número de filas de "columna" depende de la reseña (¿tiene foto? ¿hay
		// respuesta del propietario, o puede haberla?), así que su spec de filas se
		// construye aquí en vez de ser una constante — no hay problema en reconstruir
		// todo el panel en cada visita, que es justo lo que ya hacía este método antes
		// de F15.
		StringBuilder filas = new StringBuilder("[]").append(Space.LG).append("[]").append(Space.MD).append("[]");

		if (conFoto) {
			filas.append(Space.SM).append("[]");
		}

		filas.append(Space.SM).append("[]").append(Space.XXL).append("[]").append(Space.XXL).append("[]");

		if (conRespuesta) {
			filas.append(Space.XXL).append("[]").append(Space.LG).append("[]");
		}

		filas.append("push[]");

		// Una sola columna centrada, y dentro todo alineado a la izquierda. Es la
		// diferencia entre un margen izquierdo recto y uno dentado: si cada bloque se
		// centrase por su cuenta según su propio ancho máximo, el cuerpo del texto
		// arrancaría más adentro que el título.
		JPanel columna = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", filas.toString()));
		columna.setOpaque(false);

		columna.add(migaDePan(), "growx");
		columna.add(cabecera(), "growx");
		columna.add(new WrappingText(review.getBody()), "growx, " + Layout.ancho(Layout.TEXTO));

		if (conFoto) {
			columna.add(foto(), "h 0:260:260, " + Layout.ancho(Layout.TEXTO));
		}

		columna.add(traduccion(), "growx, " + Layout.ancho(Layout.TEXTO));
		columna.add(Hairline.horizontal(), "growx, h 1!");
		columna.add(subNotas(), "growx, " + Layout.ancho(Layout.TEXTO));

		if (conRespuesta) {
			columna.add(Hairline.horizontal(), "growx, h 1!");
			columna.add(respuestaDelPropietario(), "growx, " + Layout.ancho(Layout.TEXTO));
		}

		columna.add(acciones(), "growx");

		contenido.add(columna, "grow, " + Layout.anchoCentrado(Layout.CONTENIDO));

		contenido.revalidate();
		contenido.repaint();
	}

	/** La foto adjunta a la reseña (F15), del mismo tamaño de caja en las cuatro estaciones. */
	private ImagePlaceholder foto() {

		ImagePlaceholder placeholder = new ImagePlaceholder();
		placeholder.setFoto(BrandAssets.fotoDeResena(review.getImage()));
		return placeholder;
	}

	private JPanel migaDePan() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XS + "[]" + Space.XS + "[]", "[]"));
		panel.setOpaque(false);

		panel.add(Buttons.link(Textos.t("detalleResena.migaDePan", review.getHousing().getName()),
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

		texto.add(Labels.muted(Textos.t("resenas.row.por", review.getAuthor().getUsername()) + " · "
				+ formatoFecha().format(review.getPublicationDate())));

		panel.add(texto, "aligny center");

		return panel;
	}

	/**
	 * Enlace "Traducir" junto a un mensaje de estado, vacío hasta que se pulsa.
	 *
	 * <p>
	 * Fase 7.7: no hay todavía proveedor de traducción elegido, así que el
	 * servicio siempre responde {@link TranslationNotConfiguredException} — lo
	 * que sí queda montado es el hueco en la pantalla y la captura de esa
	 * excepción como un error de negocio más, igual que el resto de la UI.
	 */
	private JPanel traduccion() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.SM + "[]", "[]"));
		panel.setOpaque(false);

		JLabel mensaje = Labels.muted(" ");

		panel.add(Buttons.link(Textos.t("detalleResena.traducir"), e -> {
			try {
				reviewService.translateReview(review.getId(), Textos.idioma().getLanguage());

			} catch (TranslationNotConfiguredException ex) {
				mensaje.setText(Textos.t("detalleResena.error.traduccionNoConfigurada"));

			} catch (InstanceNotFoundException ex) {
				navigator.ir(ShowHousingsFrame.class);
			}
		}));
		panel.add(mensaje);

		return panel;
	}

	private JPanel subNotas() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.SM + "[]" + Space.SM + "[]" + Space.SM + "[]" + Space.SM + "[]"));
		panel.setOpaque(false);

		panel.add(new ScoreBar(Textos.t("resenas.subnota.ubicacion"), review.getLocationScore()), "growx");
		panel.add(new ScoreBar(Textos.t("resenas.subnota.servicio"), review.getServiceScore()), "growx");
		panel.add(new ScoreBar(Textos.t("resenas.subnota.wifi"), review.getWifiScore()), "growx");
		panel.add(new ScoreBar(Textos.t("resenas.subnota.comida"), review.getFoodScore()), "growx");
		panel.add(new ScoreBar(Textos.t("resenas.subnota.limpieza"), review.getCleaningScore()), "growx");

		return panel;
	}

	/** "Actualizar reseña" solo para quien la escribió. */
	private JPanel acciones() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]push[]", "[]"));
		panel.setOpaque(false);

		if (esSuya()) {
			panel.add(Buttons.secondary(Textos.t("detalleResena.actualizar"),
					e -> navigator.ir(UpdateReviewFrame.class, frame -> frame.setReviewId(review.getId()))));
		}

		panel.add(Buttons.link(Textos.t("detalleResena.volver"),
				e -> navigator.ir(ShowReviewsFrame.class, frame -> frame.setHousingId(review.getHousing().getId()))));

		return panel;
	}

	private boolean esSuya() {

		User usuario = sessionManager.getLoggedInUser();

		return usuario != null && usuario.getId().equals(review.getAuthor().getId());
	}

	/**
	 * Respuesta pública del propietario del alojamiento (F15).
	 *
	 * <p>
	 * Al propietario se le enseña siempre el formulario para responder —incluso
	 * si todavía no hay respuesta, que es justo el caso que le interesa—; a
	 * cualquier otra persona solo se le enseña si ya hay algo que leer. Es el
	 * mismo criterio de "una acción que no te corresponde ni se enseña" que usa
	 * el resto de la aplicación (el botón de publicar alojamiento, la copia de
	 * seguridad de F12): un formulario de respuesta vacío en la pantalla de un
	 * huésped no tiene ninguna acción que hacer con él.
	 */
	private JPanel respuestaDelPropietario() {

		JPanel panel = new JPanel(
				new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XS + "[]"));
		panel.setOpaque(false);

		panel.add(Labels.caps(Textos.t("detalleResena.respuesta.titulo")));
		panel.add(esPropietarioDelAlojamiento() ? formularioDeRespuesta() : lecturaDeRespuesta());

		return panel;
	}

	private JPanel lecturaDeRespuesta() {

		JPanel panel = new JPanel(
				new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XXS + "[]"));
		panel.setOpaque(false);

		panel.add(new WrappingText(review.getOwnerResponse()));
		panel.add(Labels.muted(formatoFecha().format(review.getOwnerResponseDate())));

		return panel;
	}

	/**
	 * Precargado con lo que ya hubiera, igual que cualquier formulario de
	 * edición de esta aplicación (regla de B9): si el propietario abre esto para
	 * corregir una palabra, no debe encontrarse el campo en blanco.
	 */
	private JPanel formularioDeRespuesta() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.SM + "[]" + Space.XS + "[]"));
		panel.setOpaque(false);

		Field respuesta = Field.textArea(Textos.t("detalleResena.respuesta.campo"), 3);
		respuesta.setText(review.getOwnerResponse() == null ? "" : review.getOwnerResponse());
		panel.add(respuesta);

		JLabel errorRespuesta = Labels.error(" ");

		String textoBoton = review.getOwnerResponse() == null ? Textos.t("detalleResena.respuesta.publicar")
				: Textos.t("detalleResena.respuesta.actualizar");

		panel.add(Buttons.secondary(textoBoton, e -> {

			String texto = respuesta.getText().trim();

			if (texto.isEmpty()) {
				errorRespuesta.setText(Textos.t("detalleResena.respuesta.error.vacia"));
				return;
			}

			try {
				reviewService.respondToReview(review.getId(), sessionManager.getLoggedInUser().getId(), texto);
				recargar();

			} catch (NotTheOwnerException | NotAuthorizedUserException ex) {
				// No debería poder ocurrir —esPropietarioDelAlojamiento() ya comprueba lo
				// mismo que el servicio—, pero la regla la impone el servicio y la pantalla
				// no debe darla por hecha.
				errorRespuesta.setText(Textos.t("detalleResena.respuesta.error.noAutorizado"));

			} catch (InstanceNotFoundException ex) {
				navigator.ir(ShowHousingsFrame.class);
			}
		}));

		panel.add(errorRespuesta);

		return panel;
	}

	private boolean esPropietarioDelAlojamiento() {

		User usuario = sessionManager.getLoggedInUser();

		return usuario != null && usuario.getRole() == RoleType.ADMIN
				&& review.getHousing().getOwner().getId().equals(usuario.getId());
	}
}
