package fp.project.actihome.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.model.services.ReviewService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.components.InlineScore;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.Rescate;
import fp.project.actihome.ui.components.WrappingText;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Detalle de un alojamiento.
 *
 * <p>
 * Dos columnas: la foto a la izquierda, los datos y las acciones a la derecha.
 * Es la pantalla que decide si alguien reserva, así que el orden de la columna
 * derecha va de lo que sitúa a lo que convence: referencia, título,
 * puntuación, descripción, datos clave, precio y por último la acción.
 *
 * <p>
 * <b>Reescrita entera</b>, no retocada: la versión anterior tenía dos bugs de
 * los que están en la tabla de deuda del proyecto. <b>B1</b>, que mostraba el
 * valor del desayuno en la etiqueta de la cena —copiar y pegar tres bloques
 * casi iguales invita a olvidarse de cambiar uno—, no puede reaparecer porque
 * {@link #resumenPension()} comprueba cada comida una sola vez, en su propio
 * método. <b>B2</b>, que acumulaba <i>listeners</i> en cada visita porque
 * {@code refreshActions()} llamaba a {@code addActionListener} dentro de
 * {@code setVisible}, tampoco puede reaparecer: los botones se construyen una
 * sola vez por reconstrucción, con su acción ya puesta; lo único que decide el
 * rol es cuáles se añaden.
 *
 * <p>
 * <b>Por qué no hay galería de miniaturas</b>, aunque el handoff dibuja una. No
 * es una simplificación de maquetación: el modelo {@code Housing} solo tiene un
 * campo {@code image}, no una lista. No hay ningún alojamiento con más de una
 * foto que enseñar, así que dibujar tres miniaturas vacías con un "+6" encima
 * sería mentir sobre cuántas fotos existen. El día que se admitan varias fotos
 * por alojamiento —una ampliación de modelo real, no de esta pantalla— la
 * galería tiene sentido; hasta entonces, una fotografía grande es lo honesto.
 */
@Component
@Profile("!test")
@Lazy
public class HousingDetailsFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final transient HousingService housingService;
	private final transient ReviewService reviewService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private Long housingId;
	private transient Housing housing;

	private JPanel contenido;

	public HousingDetailsFrame(HousingService housingService, ReviewService reviewService,
			SessionManager sessionManager, Navigator navigator, HeaderPanel headerPanel) {

		this.housingService = housingService;
		this.reviewService = reviewService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	/**
	 * Prepara qué alojamiento mostrar. La llama el {@link Navigator} antes de
	 * enseñar la ventana.
	 *
	 * <p>
	 * Solo guarda el identificador. Los datos se recargan siempre desde el
	 * servicio en {@link #setVisible}: si vienes de editar el alojamiento o de que
	 * otra persona lo haya intercambiado, el objeto que trae el catálogo en
	 * memoria puede estar desactualizado.
	 */
	public void loadDetails(Housing housing) {
		this.housingId = housing.getId();
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
		setSize(1180, 820);
		setLocationRelativeTo(null);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[grow,fill]"));

		contenido = new JPanel();
		contenido.setOpaque(false);

		raiz.add(headerPanel, "growx");
		raiz.add(Rescate.envolver(contenido), "grow");

		setContentPane(raiz);

		Foco.alPulsarEscape(this, () -> navigator.ir(ShowHousingsFrame.class));
	}

	/** Recarga el alojamiento desde el servicio y reconstruye la pantalla. */
	private void recargar() {

		if (housingId == null) {
			return;
		}

		try {
			housing = housingService.findHousing(housingId);

		} catch (InstanceNotFoundException ex) {
			// No debería ocurrir: no hay forma de borrar un alojamiento desde la
			// aplicación. Si pasara —una base de datos tocada a mano, por ejemplo— lo
			// razonable es volver al catálogo en vez de enseñar una pantalla vacía.
			navigator.ir(ShowHousingsFrame.class);
			return;
		}

		reconstruir();
	}

	/**
	 * Reconstruye todo el contenido variable de la pantalla.
	 *
	 * <p>
	 * Se rehace entero en cada visita en lugar de actualizar campo a campo. Con
	 * una decena de piezas de información y tres roles distintos de botonera,
	 * mantener referencias a cada etiqueta para actualizarla a mano sería más
	 * código y más frágil que reconstruir: es la misma decisión que ya toma
	 * {@code ShowHousingsFrame} al reaplicar sus filtros.
	 */
	private void reconstruir() {

		contenido.removeAll();
		contenido.setLayout(new MigLayout("wrap 1, fill, " + Space.insets(Space.XL, Space.HUGE, Space.XL, Space.HUGE),
				"[grow,fill]", "[]" + Space.LG + "[grow,fill]"));

		contenido.add(migaDePan(), "growx, " + Layout.anchoCentrado(Layout.CONTENIDO));
		contenido.add(cuerpo(), "grow, " + Layout.anchoCentrado(Layout.CONTENIDO));

		contenido.revalidate();
		contenido.repaint();
	}

	// ------------------------------------------------------------------
	// Miga de pan
	// ------------------------------------------------------------------

	private JPanel migaDePan() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XXS + "[]" + Space.XXS + "[]", ""));
		fila.setOpaque(false);

		JLabel catalogo = Labels.body(Textos.t("header.nav.catalogo"));
		catalogo.setFont(Typography.sans(Typography.BODY_SM));
		catalogo.setForeground(Theme.mut());
		catalogo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		catalogo.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				navigator.ir(ShowHousingsFrame.class);
			}
		});

		JLabel separador = Labels.muted("›");

		JLabel nombre = Labels.body(housing.getName());
		nombre.setFont(Typography.sansSemiBold(Typography.BODY_SM));

		fila.add(catalogo);
		fila.add(separador);
		fila.add(nombre);

		return fila;
	}

	// ------------------------------------------------------------------
	// Cuerpo: foto + información
	// ------------------------------------------------------------------

	private JPanel cuerpo() {

		JPanel panel = new JPanel(
				new MigLayout(Space.insets(0), "[grow,fill]" + Space.XXXL + "[grow,fill]", "[grow,fill]"));
		panel.setOpaque(false);

		panel.add(foto(), "grow");
		panel.add(informacion(), "aligny top");

		return panel;
	}

	private ImagePlaceholder foto() {

		boolean disponible = housingService.isAvailableNow(housing.getId());
		ImagePlaceholder placeholder = new ImagePlaceholder(Textos.tipoDeAlojamiento(housing.getType()),
				disponible ? Textos.t("catalogo.disponibilidad.disponible") : Textos.t("catalogo.disponibilidad.reservada"),
				disponible, housing.getImage());
		placeholder.setMinimumSize(new Dimension(0, 320));
		return placeholder;
	}

	private JPanel informacion() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.SM + "[]" + Space.MD + "[]" + Space.LG + "[]" + Space.XXL + "[]" + Space.XXL + "[]"
						+ Space.SM + "[]push[]"));
		panel.setOpaque(false);

		panel.add(referencia());
		panel.add(titulo());
		panel.add(valoracion());

		// "wmin 0" es imprescindible aquí: un JTextArea sin ese freno reporta como
		// ancho mínimo el de su texto sin partir en líneas, que para una descripción
		// de tres frases es enorme. Sin este freno, MigLayout respeta esa demanda y dejaba
		// la columna del texto invadir la de la foto —el mismo problema, ya documentado en
		// Layout.ancho(), que en su día se llevó por delante el panel oscuro del login—.
		panel.add(descripcion(), "growx, wmin 0");

		panel.add(miniGrid());
		panel.add(precio());
		panel.add(acciones());

		return panel;
	}

	/** "Nº 10001 —— Sierra Nevada, Granada", igual que en el catálogo. */
	private JPanel referencia() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.SM + "[]", ""));
		fila.setOpaque(false);

		fila.add(Labels.capsAccent(Textos.t("catalogo.numero") + " " + housing.getHousingCode()));
		fila.add(Labels.caps(housing.getLocation()));

		return fila;
	}

	private JLabel titulo() {

		JLabel etiqueta = Labels.cardTitle(housing.getName());
		etiqueta.setFont(Typography.serifMedium(Typography.DETAIL_TITLE));
		return etiqueta;
	}

	private JPanel valoracion() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.MD + "[]", ""));
		fila.setOpaque(false);

		fila.add(new InlineScore(housing.getScore(), 24f, 90));
		fila.add(enlaceAResenas());

		return fila;
	}

	private JLabel enlaceAResenas() {

		int cuantas = contarResenas();
		String texto = cuantas == 0 ? Textos.t("detalle.resenas.primero")
				: Formato.plural(cuantas, Textos.t("palabra.resena.singular"), Textos.t("palabra.resena.plural"));

		JLabel enlace = Labels.muted(texto);
		enlace.setFont(Typography.sansSemiBold(Typography.BODY_SM));
		enlace.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		enlace.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				verResenas();
			}
		});

		return enlace;
	}

	private int contarResenas() {

		try {
			return reviewService.showHousingReviews(housing.getId()).size();

		} catch (Exception ex) {
			return 0;
		}
	}

	private WrappingText descripcion() {
		return new WrappingText(housing.getDescription());
	}

	/** Rejilla 2×2: habitaciones, disponibilidad, pensión y titular. */
	private JPanel miniGrid() {

		JPanel panel = new JPanel(
				new MigLayout("wrap 2, gapy " + Space.LG, "[grow,fill]" + Space.XXL + "[grow,fill]", ""));
		panel.setOpaque(false);

		panel.add(celda(Textos.t("detalle.grid.habitaciones"), Formato.plural(housing.getNumberOfRooms(),
				Textos.t("palabra.habitacion.singular"), Textos.t("palabra.habitacion.plural"))));
		panel.add(celda(Textos.t("catalogo.disponibilidad.disponible"),
				housingService.isAvailableNow(housing.getId()) ? Textos.t("detalle.grid.si")
						: Textos.t("detalle.grid.noReservado")));
		panel.add(celda(Textos.t("catalogo.row.pension"), resumenPension()));
		panel.add(celda(Textos.t("detalle.grid.titular"), housing.getOwner().getUsername()));

		return panel;
	}

	private JPanel celda(String etiqueta, String valor) {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XXS + "[]"));
		panel.setOpaque(false);

		panel.add(Labels.caps(etiqueta));
		panel.add(Labels.body(valor));

		return panel;
	}

	/**
	 * "Desayuno, Cena" o "Sin comidas incluidas". Corrige de raíz el bug B1: la
	 * versión anterior comprobaba {@code isBreakfast()} para rellenar también la
	 * etiqueta de la cena. Aquí cada comida se lee de su propio método, una sola
	 * vez, así que no hay condición que copiar mal.
	 */
	private String resumenPension() {

		List<String> incluidas = new ArrayList<>();

		if (housing.isBreakfast()) {
			incluidas.add(Textos.t("catalogo.row.desayuno"));
		}
		if (housing.isLunch()) {
			incluidas.add(Textos.t("catalogo.row.comida"));
		}
		if (housing.isDinner()) {
			incluidas.add(Textos.t("catalogo.row.cena"));
		}

		return incluidas.isEmpty() ? Textos.t("detalle.pension.sinComidas") : String.join(", ", incluidas);
	}

	private JPanel precio() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XS + "[]", ""));
		fila.setOpaque(false);

		JLabel precioLabel = Labels.price(Formato.precioCorto(housing.getPricePerNight()));
		precioLabel.setFont(Typography.serif(Typography.PRICE_LG));
		fila.add(precioLabel, "aligny bottom");

		fila.add(Labels.muted(Textos.t("catalogo.card.porNoche")), "aligny bottom, gapbottom 5");

		return fila;
	}

	/**
	 * La botonera, construida una sola vez por reconstrucción y con la
	 * visibilidad decidida por el rol de quien mira. No hay ningún
	 * {@code addActionListener} fuera de aquí: cada botón se crea con su acción ya
	 * puesta, así que no hay manera de que una segunda visita le añada una
	 * escucha de más (bug B2).
	 */
	private JPanel acciones() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]push[]", "[]"));
		fila.setOpaque(false);

		JPanel izquierda = new JPanel(new MigLayout(Space.insets(0), "", "[]"));
		izquierda.setOpaque(false);

		User usuario = sessionManager.getLoggedInUser();
		boolean esPropietario = usuario != null && usuario.getId().equals(housing.getOwner().getId());

		if (usuario != null && usuario.getRole() == RoleType.CUSTOMER) {
			izquierda.add(Buttons.primary(Textos.t("detalle.accion.reservar"), e -> reservar()), "height 44!");

		} else if (usuario != null && usuario.getRole() == RoleType.ADMIN && esPropietario) {
			izquierda.add(Buttons.secondary(Textos.t("detalle.accion.actualizar"), e -> actualizar()), "height 44!");
			izquierda.add(Buttons.linkAccent(Textos.t("catalogo.row.intercambiar"), e -> intercambiar()),
					"gapleft " + Space.XL);
		}

		fila.add(izquierda);
		fila.add(Buttons.link(Textos.t("detalle.accion.verResenas"), e -> verResenas()));

		return fila;
	}

	private void reservar() {
		navigator.ir(ReserveHousingFrame.class, frame -> frame.setHousingId(housingId));
	}

	private void actualizar() {
		navigator.ir(UpdateHousingFrame.class, frame -> frame.setHousingId(housingId));
	}

	private void intercambiar() {
		navigator.ir(TradeHousingsFrame.class, frame -> frame.setHousingId(housingId));
	}

	private void verResenas() {
		navigator.ir(ShowReviewsFrame.class, frame -> frame.setHousingId(housingId));
	}
}
