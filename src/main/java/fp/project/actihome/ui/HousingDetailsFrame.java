package fp.project.actihome.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.HousingPhoto;
import fp.project.actihome.model.entities.Review;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.model.services.ReservationService;
import fp.project.actihome.model.services.ReviewService;
import fp.project.actihome.ui.components.Avatar;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.catalog.Destacado;
import fp.project.actihome.ui.components.CalendarioRango;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.components.InlineScore;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.Rescate;
import fp.project.actihome.ui.components.WrappingText;
import fp.project.actihome.ui.housings.Galeria;
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
 * Dos columnas: a la izquierda la foto y el calendario de ocupación, a la
 * derecha los datos y las acciones. Es la pantalla que decide si alguien
 * reserva, así que el orden de la columna derecha va de lo que sitúa a lo que
 * convence: referencia, título, anfitrión, descripción, cita destacada, datos
 * clave, precio y por último la acción.
 *
 * <p>
 * <b>Tres piezas de la Fase 8.4</b>, las tres pensadas para el mismo momento —
 * el de decidir:
 *
 * <ul>
 * <li><b>Tarjeta de anfitrión</b> junto al título, con avatar. En un alojamiento
 * de particular, quién te recibe es parte de lo que se decide; antes el
 * propietario era una celda más de la rejilla de datos, entre las habitaciones y
 * la pensión.</li>
 * <li><b>Cita de la mejor reseña</b>, en cursiva. Una media dice cuánto gustó;
 * una cita dice <em>qué</em> gustó, que es lo que aquí se está averiguando.</li>
 * <li><b>Calendario de ocupación</b> en modo solo lectura, bajo la foto. Saber
 * qué días están cogidos es información del alojamiento, igual que el precio, y
 * tenerla antes de entrar en el flujo de reserva evita el viaje de ida y vuelta
 * de descubrir que las fechas que querías no estaban libres.</li>
 * </ul>
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
 * <b>La galería llegó al final, y el orden importa.</b> Durante seis fases esta
 * pantalla enseñó una sola fotografía a todo lo ancho, con la nota de que
 * dibujar tres miniaturas vacías con un "+6" encima sería mentir sobre cuántas
 * fotos existen. Lo que faltaba no era maquetación sino modelo: {@code Housing}
 * tenía un único campo {@code image}. Con la tabla {@code HOUSING_PHOTOS} ya
 * hay varias fotos de verdad, y {@link Galeria} se adapta a cuántas haya —una
 * sola sigue ocupando todo el ancho, exactamente como antes—. La regla que
 * sostuvo la espera sigue en pie: la interfaz no promete contenido que no
 * existe.
 */
@Component
@Profile("!test")
@Lazy
public class HousingDetailsFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final transient HousingService housingService;
	private final transient ReviewService reviewService;

	/** Solo para pintar la ocupación del calendario: aquí no se reserva nada. */
	private final transient ReservationService reservationService;

	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private Long housingId;
	private transient Housing housing;

	private JPanel contenido;

	public HousingDetailsFrame(HousingService housingService, ReviewService reviewService,
			ReservationService reservationService, SessionManager sessionManager, Navigator navigator,
			HeaderPanel headerPanel) {

		this.housingService = housingService;
		this.reviewService = reviewService;
		this.reservationService = reservationService;
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

		// Columna izquierda: la foto y, debajo, el calendario de ocupación. Los dos
		// son "cómo es y cuándo está libre"; la derecha es "qué ofrece y cuánto
		// cuesta". El calendario no cabía en la columna derecha sin empujar el precio
		// y el botón de reservar fuera de la ventana, y eso la regla de escritorio del
		// proyecto no lo permite.
		JPanel izquierda = new JPanel(
				new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[grow,fill]" + Space.LG + "[]"));
		izquierda.setOpaque(false);

		izquierda.add(galeria(), "grow");
		izquierda.add(calendarioDeOcupacion());

		panel.add(izquierda, "grow");
		panel.add(informacion(), "aligny top");

		return panel;
	}

	/**
	 * La galería: la foto principal y las de {@code HOUSING_PHOTOS} detrás.
	 *
	 * <p>
	 * La lista se monta aquí y no dentro de {@link Galeria} porque ese componente
	 * vive en el vocabulario visual y no debe conocer el modelo de negocio — la
	 * misma razón por la que {@code ImagePlaceholder} recibe un nombre de archivo
	 * y no una entidad. Aquí ya tenemos el alojamiento delante.
	 */
	private JComponent galeria() {

		boolean disponible = housingService.isAvailableNow(housing.getId());

		List<String> archivos = new ArrayList<>();
		archivos.add(housing.getImage());

		for (HousingPhoto foto : housingService.showHousingPhotos(housing.getId())) {
			archivos.add(foto.getImage());
		}

		Galeria galeria = new Galeria(Textos.tipoDeAlojamiento(housing.getType()),
				disponible ? Textos.t("catalogo.disponibilidad.disponible")
						: Textos.t("catalogo.disponibilidad.reservada"),
				disponible, Destacado.de(housing), archivos);

		galeria.setMinimumSize(new Dimension(0, 320));

		return galeria;
	}

	private JPanel informacion() {

		// Las separaciones van en cada componente ("gapbottom") y no en la lista de
		// filas del layout. Es un cambio de la Fase 8.4 y tiene motivo: la cita
		// destacada solo se añade si hay reseñas, y una lista de filas escrita a mano
		// deja de corresponderse con los componentes en cuanto uno es condicional —el
		// resultado son separaciones desplazadas una posición, que es un fallo
		// silencioso y difícil de leer en el código.
		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]push[]"));
		panel.setOpaque(false);

		panel.add(referencia(), "gapbottom " + Space.SM);
		panel.add(titulo(), "gapbottom " + Space.MD);
		panel.add(anfitrion(), "gapbottom " + Space.LG);

		// "wmin 0" es imprescindible aquí: un JTextArea sin ese freno reporta como
		// ancho mínimo el de su texto sin partir en líneas, que para una descripción
		// de tres frases es enorme. Sin este freno, MigLayout respeta esa demanda y dejaba
		// la columna del texto invadir la de la foto —el mismo problema, ya documentado en
		// Layout.ancho(), que en su día se llevó por delante el panel oscuro del login—.
		panel.add(descripcion(), "growx, wmin 0, gapbottom " + Space.LG);

		// Solo si hay alguna reseña: un hueco con comillas vacías sería peor que la
		// ausencia. Al no añadirse, el layout no le reserva sitio.
		JPanel cita = citaDestacada();

		if (cita != null) {
			panel.add(cita, "growx, wmin 0, gapbottom " + Space.XL);
		}

		panel.add(miniGrid(), "gapbottom " + Space.XXL);
		panel.add(precio(), "gapbottom " + Space.SM);
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

	/**
	 * La tarjeta de anfitrión: avatar, quién es, la nota y el enlace a las reseñas
	 * (Fase 8.4).
	 *
	 * <p>
	 * Antes aquí solo estaba la nota y el enlace, y el propietario aparecía como
	 * una celda más de la rejilla de datos, entre las habitaciones y la pensión. El
	 * cambio no es de maquetación: <b>en un alojamiento de particular, quién te
	 * recibe es parte de lo que se decide al reservar</b>, no un atributo del
	 * inmueble. Subirlo junto al título y darle cara es lo que separa esta pantalla
	 * de la ficha de un hotel.
	 */
	private JPanel anfitrion() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0),
				"[]" + Space.SM + "[]" + Space.LG + "[]" + Space.MD + "[]", "[]"));
		fila.setOpaque(false);

		User propietario = housing.getOwner();

		fila.add(Avatar.relleno(propietario.getName(), propietario.getSurname(), 30), "w 30!, h 30!, aligny center");
		fila.add(Labels.body(Textos.t("detalle.anfitrion", propietario.getUsername())), "aligny center");
		fila.add(new InlineScore(housing.getScore(), 24f, 90), "aligny center");
		fila.add(enlaceAResenas(), "aligny center");

		return fila;
	}

	/**
	 * La reseña mejor valorada, en cursiva y con su autoría.
	 *
	 * <p>
	 * Una cita concreta convence más que una media: "4,2" dice cuánto gustó, pero
	 * "Silencio y buen desayuno" dice <em>qué</em> gustó, que es lo que alguien
	 * está intentando averiguar en esta pantalla.
	 *
	 * <p>
	 * Se elige la de <b>mejor nota</b> y no la más reciente. Las dos opciones son
	 * defendibles y esta es más honesta de lo que parece: lo que se está enseñando
	 * es la mejor cara del alojamiento, y quien quiera el resto tiene el enlace a
	 * las reseñas justo encima — donde además hay un histograma que enseña de un
	 * vistazo si esa reseña es representativa o un caso aislado.
	 *
	 * <p>
	 * Devuelve {@code null} si no hay ninguna reseña. Quien llama no pinta nada
	 * entonces: un hueco con comillas vacías sería peor que la ausencia.
	 */
	private JPanel citaDestacada() {

		List<Review> resenas;

		try {
			resenas = reviewService.showHousingReviews(housing.getId());

		} catch (InstanceNotFoundException ex) {
			return null;
		}

		Review mejor = null;

		for (Review resena : resenas) {
			if (mejor == null || resena.getTotalScore() > mejor.getTotalScore()) {
				mejor = resena;
			}
		}

		if (mejor == null) {
			return null;
		}

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XS + "[]"));
		panel.setOpaque(false);

		WrappingText cita = new WrappingText("«" + mejor.getTitle() + "»");
		cita.setFont(Typography.serifItalic(18f));
		panel.add(cita, "growx, wmin 0");

		panel.add(Labels.caps(Textos.t("resenas.row.por", mejor.getAuthor().getUsername()) + " · "
				+ Formato.nota(mejor.getTotalScore())));

		return panel;
	}

	/**
	 * Mini-calendario informativo con los días ya cogidos.
	 *
	 * <p>
	 * Es el mismo {@link CalendarioRango} de la pantalla de reservar, en modo solo
	 * lectura. Reutilizarlo en vez de escribir un calendario más pequeño evita que
	 * dos componentes puedan discrepar sobre qué día está ocupado, que es
	 * exactamente la clase de incoherencia que el usuario detecta y no perdona.
	 */
	private CalendarioRango calendarioDeOcupacion() {

		CalendarioRango calendario = new CalendarioRango(() -> {
			// Sin acción: en esta pantalla el calendario informa, no selecciona.
		});

		calendario.setOcupacion(reservationService.showHousingReservations(housing.getId()));
		calendario.soloLectura();

		return calendario;
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

	/**
	 * Cuántas reseñas tiene el alojamiento, o cero si ha dejado de existir.
	 *
	 * <p>
	 * Se captura {@code InstanceNotFoundException} y no {@code Exception} (Fase
	 * 8.3): es la única excepción que declara {@code showHousingReviews}, así que
	 * para el caso previsto el comportamiento es idéntico — pero el genérico se
	 * tragaba además cualquier fallo de programación, que aquí se manifestaba como
	 * un alojamiento que dice tener cero reseñas teniendo varias, sin ninguna
	 * traza. B11 daba esto por cerrado y no lo estaba.
	 */
	private int contarResenas() {

		try {
			return reviewService.showHousingReviews(housing.getId()).size();

		} catch (InstanceNotFoundException ex) {
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

		// El titular estaba aquí como cuarta celda y salió en la Fase 8.4: la tarjeta
		// de anfitrión de arriba dice lo mismo, con cara y con nota. Repetir un dato a
		// dos centímetros de sí mismo no informa el doble, hace dudar de si son dos
		// datos distintos.
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
			izquierda.add(Buttons.primary(Textos.t("detalle.accion.reservar"), e -> reservar()), "height " + Typography.altoDeBoton() + "!");
			izquierda.add(Buttons.linkAccent(Textos.t("detalle.accion.preguntar"), e -> preguntar()),
					"gapleft " + Space.XL);

		} else if (usuario != null && usuario.getRole() == RoleType.ADMIN && esPropietario) {
			izquierda.add(Buttons.secondary(Textos.t("detalle.accion.actualizar"), e -> actualizar()), "height " + Typography.altoDeBoton() + "!");
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

	/** F10: preguntar al propietario antes de reservar, sin tener que abrir la bandeja de mensajes primero. */
	private void preguntar() {
		navigator.ir(ConversationFrame.class, frame -> frame.setConversacion(housing.getOwner(), housing));
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
