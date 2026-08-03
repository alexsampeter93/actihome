package fp.project.actihome.ui;

import java.awt.Dimension;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.ScrollPaneConstants;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.Review;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.model.services.ReviewService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.reviews.ReviewRow;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Typography;

/**
 * Las reseñas de un alojamiento.
 *
 * <p>
 * Cabecera con la nota media grande y la lista debajo, con scroll propio: la
 * regla de escritorio del proyecto, igual que el catálogo y "mis reservas".
 *
 * <p>
 * <b>Sustituye a una {@code JTable} de cuatro columnas</b> (id, autor, título,
 * nota). Una tabla ordena datos homogéneos y cortos; una reseña es un texto con
 * autoría, fecha y cinco sub-notas, y meterla en celdas obligaba a esconder casi
 * todo. El listado editorial enseña de cada reseña lo que permite decidir si
 * abrirla, que es justo para lo que sirve un listado.
 */
@Component
@Profile("!test")
@Lazy
public class ShowReviewsFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final transient ReviewService reviewService;
	private final transient HousingService housingService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private Long housingId;
	private transient Housing housing;

	private JPanel titular;
	private JPanel lista;
	private JScrollPane scroll;

	public ShowReviewsFrame(ReviewService reviewService, HousingService housingService, SessionManager sessionManager,
			Navigator navigator, HeaderPanel headerPanel) {

		this.reviewService = reviewService;
		this.housingService = housingService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	/** Prepara de qué alojamiento son las reseñas. La llama el {@link Navigator}. */
	public void setHousingId(Long housingId) {
		this.housingId = housingId;
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			headerPanel.refresh();
			recargar();
				volverArriba();
	}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(1040, 800);
		setLocationRelativeTo(null);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[]0[grow,fill]"));

		titular = new JPanel();
		titular.setOpaque(false);

		raiz.add(headerPanel, "growx");
		raiz.add(titular, "growx");
		raiz.add(zonaDeLista(), "grow");

		setContentPane(raiz);

		// Al detalle del alojamiento, no al catálogo: es el paso inmediatamente
		// anterior en la miga de pan, y Escape es "un paso atrás", no "ir al inicio".
		Foco.alPulsarEscape(this, () -> navigator.ir(HousingDetailsFrame.class, frame -> frame.loadDetails(housing)));
	}

	/**
	 * Devuelve la lista al principio.
	 *
	 * <p>
	 * <b>Hace falta porque los frames son singleton.</b> El {@code JScrollPane} es
	 * el mismo objeto en cada visita y conserva su posición, así que al volver a
	 * esta pantalla la lista aparecía desplazada desde la vez anterior —con la
	 * primera fila cortada por arriba— sin que el usuario hubiera tocado la rueda.
	 * Se leía como un fallo de maquetación y era memoria de estado.
	 *
	 * <p>
	 * Va dentro de {@code invokeLater} porque en el momento de llamarlo la lista
	 * acaba de reconstruirse y todavía no se ha distribuido: poner el scroll a cero
	 * antes de que el layout calcule el alto no serviría de nada.
	 */
	private void volverArriba() {

		if (scroll != null) {
			SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
		}
	}

	private JScrollPane zonaDeLista() {


		lista = new JPanel(
				new MigLayout("wrap 1, " + Space.insets(0, Space.HUGE, Space.XXL, Space.HUGE), "[grow,fill]", "[]"));
		lista.setOpaque(false);

		scroll = new JScrollPane(lista);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setBorder(null);
		scroll.setViewportBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.getVerticalScrollBar().setUnitIncrement(24);

		// Mínimo cero para que sea la lista la que ceda espacio cuando la ventana se
		// queda corta, y no la cabecera.
		scroll.setMinimumSize(new Dimension(0, 0));

		return scroll;
	}

	private void recargar() {

		if (housingId == null) {
			return;
		}

		List<Review> resenas;

		try {
			housing = housingService.findHousing(housingId);
			resenas = reviewService.showHousingReviews(housingId);

		} catch (InstanceNotFoundException ex) {
			// El alojamiento ya no existe. No hay pantalla que enseñar, así que se vuelve
			// al catálogo en lugar de dejar una lista vacía sin explicación.
			navigator.ir(ShowHousingsFrame.class);
			return;
		}

		reconstruirTitular(resenas);
		reconstruirLista(resenas);
	}

	// ------------------------------------------------------------------
	// Cabecera
	// ------------------------------------------------------------------

	private void reconstruirTitular(List<Review> resenas) {

		titular.removeAll();
		titular.setLayout(new MigLayout("wrap 1, " + Space.insets(Space.XL, Space.HUGE, Space.LG, Space.HUGE),
				"[grow,fill]", "[]" + Space.XS + "[]" + Space.MD + "[]"));

		titular.add(migaDePan(), "growx");
		titular.add(Labels.title("Reseñas · " + housing.getName()));
		titular.add(notaMedia(resenas), "growx");

		titular.revalidate();
		titular.repaint();
	}

	private JPanel migaDePan() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XS + "[]" + Space.XS + "[]", "[]"));
		panel.setOpaque(false);

		panel.add(Buttons.link("Catálogo", e -> navigator.ir(ShowHousingsFrame.class)));
		panel.add(Labels.muted("›"));
		panel.add(Buttons.link(housing.getName(),
				e -> navigator.ir(HousingDetailsFrame.class, frame -> frame.loadDetails(housing))));

		return panel;
	}

	/**
	 * La media del alojamiento, en grande, con el número de reseñas al lado.
	 *
	 * <p>
	 * La media se lee de {@code housing.getScore()} y no se calcula aquí: el
	 * servicio ya la recalcula y la reescribe cada vez que se publica o se
	 * actualiza una reseña. Recalcularla también en la pantalla sería tener la
	 * misma regla en dos sitios, y el día que cambiara solo se corregiría uno.
	 */
	private JPanel notaMedia(List<Review> resenas) {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.MD + "[]" + Space.XL + "[]push[]", "[]"));
		panel.setOpaque(false);

		JLabel media = Labels.price(Formato.nota(housing.getScore()));
		media.setFont(Typography.serifMedium(40f));
		panel.add(media, "aligny center");

		panel.add(Labels.muted(resenas.isEmpty() ? "sin reseñas"
				: Formato.plural(resenas.size(), "reseña", "reseñas")), "aligny center");

		if (esCliente() && !yaOpino(resenas)) {
			panel.add(Buttons.primary("Publicar reseña", e -> publicar()), "aligny center");
		}

		return panel;
	}

	/** Solo un CUSTOMER publica reseñas; el servicio impone la misma regla. */
	private boolean esCliente() {

		User usuario = sessionManager.getLoggedInUser();

		return usuario != null && usuario.getRole() == RoleType.CUSTOMER;
	}

	/**
	 * Si el usuario ya tiene una reseña de este alojamiento.
	 *
	 * <p>
	 * El servicio impide publicar dos veces ({@code AlreadyPublishedException}), y
	 * ofrecer un botón que siempre va a fallar es peor que no ofrecerlo. Esto
	 * <b>no</b> es duplicar la regla de negocio: no se consulta nada de más, se mira
	 * la lista que la pantalla acaba de cargar para pintarse. La validación de
	 * verdad la sigue haciendo el servicio, y {@code PublishReviewFrame} sigue
	 * teniendo su mensaje por si se llegara ahí por otro camino.
	 */
	private boolean yaOpino(List<Review> resenas) {

		User usuario = sessionManager.getLoggedInUser();

		return usuario != null
				&& resenas.stream().anyMatch(r -> r.getAuthor().getId().equals(usuario.getId()));
	}

	// ------------------------------------------------------------------
	// Lista
	// ------------------------------------------------------------------

	private void reconstruirLista(List<Review> resenas) {

		lista.removeAll();

		if (resenas.isEmpty()) {
			lista.add(estadoVacio(), "growx");

		} else {
			boolean primera = true;

			for (Review resena : resenas) {

				if (!primera) {
					lista.add(Hairline.horizontal(), "growx, h 1!");
				}

				lista.add(fila(resena), "growx, " + Layout.ancho(Layout.CONTENIDO));
				primera = false;
			}
		}

		lista.revalidate();
		lista.repaint();
	}

	private ReviewRow fila(Review resena) {

		return new ReviewRow(resena,
				() -> navigator.ir(ReviewDetailsFrame.class, frame -> frame.loadDetails(resena)));
	}

	private JPanel estadoVacio() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.HUGE, 0, Space.HUGE, 0), "[grow,fill]",
				"[]" + Space.LG + "[]" + Space.XS + "[]"));
		panel.setOpaque(false);

		panel.add(centrar(new MascotSlot(MascotSlot.Tamano.MEDIANO, Pose.ACCION)));
		panel.add(centrar(Labels.title("Todavía no hay reseñas")));
		panel.add(centrar(Labels.muted(esCliente() ? "Si te has alojado aquí, cuéntalo: serás la primera persona."
				: "Cuando alguien se aloje y opine, aparecerá aquí.")));

		return panel;
	}

	private JPanel centrar(JComponent componente) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "push[]push", ""));
		fila.setOpaque(false);
		fila.add(componente);
		return fila;
	}

	private void publicar() {
		navigator.ir(PublishReviewFrame.class, frame -> frame.setHousingId(housingId));
	}
}
