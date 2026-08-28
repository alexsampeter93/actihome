package fp.project.actihome.ui;

import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.ScrollPaneConstants;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Amenity;
import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.model.services.ReservationService;
import fp.project.actihome.model.services.ReviewService;
import fp.project.actihome.ui.catalog.BarraComparar;
import fp.project.actihome.ui.catalog.BotonMas;
import fp.project.actihome.ui.catalog.CatalogFilters;
import fp.project.actihome.ui.catalog.HeroCatalogo;
import fp.project.actihome.ui.catalog.HousingCard;
import fp.project.actihome.ui.catalog.HousingRow;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Capa;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.nav.ConNombre;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.components.Rescate;

/**
 * Catálogo de alojamientos: la pantalla principal tras iniciar sesión.
 *
 * <p>
 * Tres bandas de alto fijo y la lista, que es la única que estira:
 *
 * <ol>
 * <li><b>Cabecera</b> oscura, con el selector de estación.</li>
 * <li><b>Hero</b>: a la izquierda la frase de la estación y el titular; a la
 * derecha el buscador y las tres cifras del catálogo. Se <b>contrae a una línea
 * al bajar por la lista</b>.</li>
 * <li><b>Filtros</b>: dos filas bajas, con las comodidades recogidas tras "Más
 * filtros".</li>
 * <li><b>La lista</b>, la única con scroll, con el botón de publicar flotando
 * encima.</li>
 * </ol>
 *
 * <p>
 * <b>Ese reparto es la regla de escritorio del proyecto.</b> Una web se recorre
 * con la rueda y puede permitirse crecer hacia abajo sin fin; una aplicación de
 * escritorio no, porque su ventana tiene un tamaño y el usuario espera ver la
 * pantalla entera. Aquí el marco está siempre a la vista y lo que se desplaza es
 * únicamente el contenido.
 *
 * <p>
 * <b>El reparto se midió, no se estimó</b>, y el resultado era malo: el cromo se
 * llevaba el 59 % de la pantalla y la lista el 41 %, o sea que en vista de lista
 * se veía una ficha y pico. En una aplicación cuyo trabajo es enseñar
 * alojamientos, eso está al revés. Cuatro cambios lo corrigen sin tocar la
 * identidad editorial:
 *
 * <ul>
 * <li>El <b>selector de estación</b> sube a la cabecera. Además de recuperar
 * alto, arregla que solo fuera alcanzable desde esta pantalla de diecisiete.</li>
 * <li>El <b>buscador</b> ocupa el hueco que dejó el selector, así que su banda
 * de ~85px desaparece sin que el hero crezca.</li>
 * <li>Las <b>comodidades</b> se recogen tras "Más filtros", que lleva el número
 * de filtros activos para que nunca queden filtrando en silencio.</li>
 * <li>El <b>colofón</b> desaparece: el botón de publicar ya era flotante y no
 * necesitaba una banda de 80px propia.</li>
 * </ul>
 *
 * <p>
 * Resultado medido: la lista pasa de 351 a 512px —del 41 % al 59 %— y a 614px
 * (74 %) con el hero contraído.
 *
 * <p>
 * <b>Las cifras del hero se calculan sobre el catálogo completo</b>, no sobre lo
 * que quede tras filtrar. Son el estado del catálogo, no del resultado: si
 * bajaran al filtrar dejarían de ser un dato y pasarían a ser un eco del propio
 * filtro.
 */
@Component
@Profile("!test")
@Lazy
public class ShowHousingsFrame extends JFrame implements ConNombre {

	private static final long serialVersionUID = 1L;

	/**
	 * El aire a izquierda y derecha de la lista, <b>declarado como rango</b>.
	 *
	 * <p>
	 * Antes eran 44 puntos fijos a cada lado, metidos en los {@code insets} del
	 * panel. Ochenta y ocho puntos que la lista no podía recuperar jamás, y en una
	 * ventana de 1024 eso era exactamente la diferencia entre que una fila cupiera o
	 * se saliera por la derecha — donde no hay barra de desplazamiento que valga.
	 *
	 * <p>
	 * <b>Va en la especificación de columnas y no en los insets, y no es
	 * indiferente:</b> MigLayout <b>no admite</b> {@code min:pref:max} dentro de
	 * {@code insets} —revienta con "Malformed UnitValue"— pero sí en los gaps de
	 * las columnas. Es la regla número 2 de la adaptabilidad del proyecto aplicada
	 * donde el gestor de layout deja aplicarla: el aire se negocia, un botón no.
	 */
	private static final String AIRE_LATERAL = Space.MD + ":" + Space.HUGE + ":" + Space.HUGE;

	/** Columnas de la vista de cuadrícula, según el handoff. */
	private static final int COLUMNAS_CUADRICULA = 3;

	private final transient HousingService housingService;
	private final transient ReviewService reviewService;
	private final transient ReservationService reservationService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private JPanel lista;
	private JScrollPane scroll;
	private CatalogFilters filtros;

	/**
	 * El catálogo completo, cargado una vez al abrir la pantalla.
	 *
	 * <p>
	 * Los filtros trabajan sobre esta lista en memoria en lugar de volver a la base
	 * de datos. Con un buscador que filtra en vivo, la alternativa sería una consulta
	 * por cada tecla pulsada.
	 */
	private transient List<Housing> catalogo = Collections.emptyList();

	/**
	 * Reseñas por alojamiento, contadas una sola vez por visita.
	 *
	 * <p>
	 * Sin esta caché, cada cambio de filtro volvería a pedir las reseñas de cada
	 * alojamiento que sobreviva al filtro — y eso ocurre en cada pulsación del
	 * buscador.
	 */
	private final transient Map<Long, Integer> resenasPorAlojamiento = new HashMap<>();

	/**
	 * Los alojamientos con una estancia en curso ahora mismo, calculados una sola
	 * vez por recarga del catálogo. Mismo criterio que {@link #resenasPorAlojamiento}:
	 * evita una consulta por fila cuando lo que cambia es solo el filtro, no la
	 * base de datos.
	 */
	private transient Set<Long> ocupadosAhora = Collections.emptySet();

	/**
	 * A quién ya se le aplicó su vista de catálogo por defecto en esta sesión
	 * (Fase 7.11). Sin esto, cada visita al catálogo —tras ver un detalle, tras
	 * reservar— volvería a imponer la vista guardada en Ajustes y borraría
	 * cualquier cambio manual hecho mientras tanto. Se aplica una sola vez por
	 * cuenta: al cambiar de usuario (cerrar sesión y entrar con otro) vuelve a
	 * aplicarse, porque el id ya no coincide.
	 */
	private Long usuarioDeLaVistaAplicada;

	/**
	 * La selección de comparar (F16) y su barra flotante, en una sola pieza.
	 *
	 * <p>
	 * Vivía aquí como un {@code Set}, cuatro campos de widget y tres métodos que
	 * los mantenían sincronizados. Ahora es {@link BarraComparar}: la selección y
	 * lo que la anuncia son la misma cosa, y tenerlas separadas solo permitía
	 * tocar una y olvidar la otra. Se vacía en cada recarga del catálogo — es una
	 * selección de la visita, no una preferencia que deba sobrevivir.
	 */
	private BarraComparar comparar;

	/**
	 * La cabecera editorial, en sus dos versiones. Ver {@link HeroCatalogo}.
	 *
	 * <p>
	 * Eran dos bandas sueltas en la raíz, trece métodos y catorce campos. Esta
	 * pantalla ya no sabe que hay dos: solo le dice cuánto se ha desplazado la
	 * lista y de cuánta ventana dispone.
	 */
	private HeroCatalogo hero;

	private JPanel accionAdmin;

	/**
	 * Testigo de la suscripción a los cambios de estación.
	 *
	 * <p>
	 * {@link Theme#alCambiar} devuelve un objeto con el que darse de baja. Guardarlo
	 * no es opcional: un oyente que nunca se retira mantiene viva la pantalla
	 * entera aunque se cierre, y eso es una fuga de memoria de manual.
	 */
	private transient Object suscripcion;

	public ShowHousingsFrame(HousingService housingService, ReviewService reviewService,
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
	 * Deja pendiente lo que se pidió en el buscador de destino, fechas y
	 * huéspedes (Fase 9), para aplicarlo en cuanto el catálogo esté cargado.
	 *
	 * <p>
	 * <b>No se aplica aquí mismo.</b> Este método se llama desde
	 * {@code navigator.ir(ShowHousingsFrame.class, frame -> frame.aplicarBusqueda(...))},
	 * es decir, <em>antes</em> de que {@code setVisible(true)} recargue
	 * {@link #catalogo} y reconstruya {@link #filtros}. Escribir directamente en
	 * los controles del filtro en este punto se perdería en cuanto
	 * {@code cargarAlojamientos()} los reinicie.
	 *
	 * @param destino    texto libre de destino, o vacío para no filtrar por él
	 * @param entrada    fecha de entrada, o {@code null} si el buscador no la pidió
	 * @param salida     fecha de salida, o {@code null}
	 * @param huespedes  adultos + niños; los bebés no cuentan para el aforo
	 * @param conMascota si se pidió que el alojamiento admita mascotas
	 */
	public void aplicarBusqueda(String destino, java.time.LocalDate entrada, java.time.LocalDate salida,
			int huespedes, boolean conMascota) {

		busquedaPendiente = new BusquedaPendiente(destino, entrada, salida, huespedes, conMascota);
	}

	/**
	 * Lo que trae el buscador de destino, en un solo objeto para no repartir
	 * cinco campos sueltos que solo tienen sentido juntos y solo hasta la
	 * próxima carga.
	 */
	private record BusquedaPendiente(String destino, java.time.LocalDate entrada, java.time.LocalDate salida,
			int huespedes, boolean conMascota) {
	}

	private transient BusquedaPendiente busquedaPendiente;

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			headerPanel.refresh();
			filtros.actualizarTextos();
			refrescarAccionAdmin();
			cargarAlojamientos();
			hero.actualizarTextosEstacionales();
			actualizarTextosFijos();
			volverArriba();
		}

		super.setVisible(visible);
	}

	@Override
	public void dispose() {

		Theme.olvidar(suscripcion);
		super.dispose();
	}

	private void initUI() {

		setTitle("ActiHome");

		// El catálogo pide más ventana que el login: tiene una cabecera, un hero, tres
		// bandas de filtros y una lista, y todo eso tiene un ancho mínimo real. El
		// mínimo no es un capricho, es el ancho por debajo del cual los chips de
		// comodidad empiezan a salirse.
		setSize(1400, 900);
		setLocationRelativeTo(null);

		headerPanel.marcarActual(ShowHousingsFrame.class);

		// El hero se cuenta como UNA fila y no como dos, porque las dos bandas viven
		// ahora dentro de HeroCatalogo. El "hidemode 3" que hacía falta aquí para que
		// la banda oculta no reservara su hueco se ha ido con ellas, que es donde
		// tiene sentido.
		JPanel raiz = new Page(
				new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[]0[]0[grow,fill]"));

		filtros = new CatalogFilters(this::aplicarFiltros);

		hero = new HeroCatalogo(filtros.extraerBuscador());

		raiz.add(headerPanel, "growx");
		raiz.add(hero, "growx");
		raiz.add(filtros, "growx");
		raiz.add(listaConBotonFlotante(), "grow");

		setContentPane(raiz);

		// Cambiar de estación no solo cambia colores: cambia también las palabras (la
		// frase editorial de la estación). Los colores se resuelven solos al repintar;
		// el texto hay que reescribirlo.
		suscripcion = Theme.alCambiar(estacion -> hero.actualizarTextosEstacionales());

		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {

				hero.ajustarEscalaDeDisplay(getWidth());

				// El hero depende del alto de la ventana desde que se despliega solo cuando
				// se gana su sitio, así que redimensionar es tan motivo para recalcularlo
				// como desplazar la lista. Sin esto, agrandar la ventana no devolvía el
				// titular hasta que además se tocara la rueda.
				ajustarHeroAlScroll();
			}
		});
	}

	/**
	 * Le pasa al hero lo único que él no puede saber: cuánto se ha desplazado la
	 * lista y cuánta ventana hay.
	 *
	 * <p>
	 * Si el hero cambia de banda hay que revalidar <b>la ventana</b> y no solo él:
	 * el reparto vertical entre hero, filtros y lista lo decide la raíz.
	 */
	private void ajustarHeroAlScroll() {

		if (scroll == null || hero == null) {
			return;
		}

		if (hero.ajustarAlScroll(scroll.getVerticalScrollBar().getValue(), getContentPane().getHeight())) {
			revalidate();
			repaint();
		}
	}


	// ------------------------------------------------------------------
	// Lista
	// ------------------------------------------------------------------

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


		lista = new JPanel(new MigLayout("wrap 1, " + Space.insets(0, 0, Space.XXL, 0),
				AIRE_LATERAL + "[grow,fill]" + AIRE_LATERAL, "[]"));
		lista.setOpaque(false);

		// Rescate y no un JScrollPane crudo. La diferencia esta en el Scrollable que
		// Rescate envuelve: sin el, el contenido conserva su ancho preferido en vez de
		// seguir el del visor, y con la barra horizontal desactivada lo que se sale por
		// la derecha NO SE PUEDE ALCANZAR NUNCA, por mucho que se agrande la ventana.
		scroll = Rescate.envolver(lista);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		// Los dos bordes, no solo uno: JScrollPane tiene un borde propio y otro para el
		// viewport, y FlatLaf pone una línea en el segundo. Quitando solo el primero
		// queda una raya vertical pegada al contenido que parece un fallo de pintado.
		scroll.setBorder(null);
		scroll.setViewportBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		// El salto por defecto de Swing es de un píxel por muesca de rueda, que en una
		// lista de filas de doscientos píxeles se percibe como que el scroll no funciona.
		scroll.getVerticalScrollBar().setUnitIncrement(24);

		// Escuchar el desplazamiento es lo que permite contraer el hero. Se registra una
		// sola vez, aqui en la construccion: hacerlo en setVisible sobre un frame
		// singleton acumularia una escucha por visita (bug B2).
		scroll.getVerticalScrollBar().addAdjustmentListener(e -> ajustarHeroAlScroll());

		// Mínimo cero: cuando la ventana se queda corta, el espacio se lo tiene que
		// quitar la lista —que para eso tiene scroll— y no la cabecera, el hero o los
		// filtros. Sin esto, el reparto castiga a quien no ha declarado su mínimo, que
		// es exactamente cómo se aplastaron las pestañas del selector de estación.
		scroll.setMinimumSize(new Dimension(0, 0));

		return scroll;
	}

	/** Recarga el catálogo desde la base de datos. Solo al abrir la pantalla. */
	private void cargarAlojamientos() {

		catalogo = housingService.showHousings();
		resenasPorAlojamiento.clear();
		ocupadosAhora = new HashSet<>(housingService.currentlyOccupiedHousingIds());
		comparar.limpiar();

		hero.actualizarCifras(catalogo, ocupadosAhora);
		filtros.setUbicaciones(catalogo);
		aplicarVistaPorDefecto();
		aplicarBusquedaPendiente();
		aplicarFiltros();
	}

	/**
	 * Traslada al filtro lo que trajo el buscador de destino, si lo trajo, y lo
	 * olvida a continuación.
	 *
	 * <p>
	 * <b>Se olvida siempre, incluso si no había nada que aplicar.</b> Sin eso,
	 * volver al catálogo por la cabecera después de una búsqueda repetiría esos
	 * mismos criterios en cada visita posterior de la sesión, que es justo el
	 * fallo que ya se corrigió una vez para la vista por defecto
	 * ({@link #usuarioDeLaVistaAplicada}).
	 */
	private void aplicarBusquedaPendiente() {

		BusquedaPendiente busqueda = busquedaPendiente;
		busquedaPendiente = null;

		if (busqueda == null) {
			return;
		}

		if (busqueda.destino() != null && !busqueda.destino().isBlank()) {
			filtros.extraerBuscador().setTexto(busqueda.destino());
		}

		if (busqueda.huespedes() > 1) {
			filtros.setHuespedesMinimos(busqueda.huespedes());
		}

		if (busqueda.conMascota()) {
			filtros.preseleccionarComodidad(Amenity.PETS);
		}

		if (busqueda.entrada() != null && busqueda.salida() != null) {

			java.time.LocalDateTime checkIn = busqueda.entrada().atStartOfDay();
			java.time.LocalDateTime checkOut = busqueda.salida().atStartOfDay();

			filtros.setDisponibilidad(busqueda.entrada(), busqueda.salida(),
					reservationService.showUnavailableHousingIds(checkIn, checkOut));
		}
	}

	/**
	 * Hace que la próxima carga vuelva a imponer la vista por defecto guardada,
	 * aunque ya se hubiera aplicado antes en esta sesión.
	 *
	 * <p>
	 * Lo llama {@code SettingsFrame} justo antes de volver aquí tras guardar un
	 * cambio en esa preferencia: sin esto, cambiar la vista por defecto y pulsar
	 * "Guardar cambios" no se notaría hasta la próxima vez que se iniciara
	 * sesión, porque {@link #aplicarVistaPorDefecto()} ya habría marcado a este
	 * usuario como atendido en una visita anterior de la misma sesión.
	 */
	public void olvidarVistaAplicada() {
		usuarioDeLaVistaAplicada = null;
	}

	/** Ver la nota de {@link #usuarioDeLaVistaAplicada}. */
	private void aplicarVistaPorDefecto() {

		User actual = sessionManager.getLoggedInUser();

		if (actual == null || actual.getId().equals(usuarioDeLaVistaAplicada)) {
			return;
		}

		filtros.setVista(actual.isDefaultGridView() ? CatalogFilters.VISTA_CUADRICULA : 0);
		usuarioDeLaVistaAplicada = actual.getId();
	}

	/**
	 * Repinta el contenido con los filtros vigentes.
	 *
	 * <p>
	 * Se llama en cada cambio de filtro, incluida cada tecla del buscador. Por eso no
	 * toca la base de datos: trabaja sobre {@link #catalogo}, que ya está en memoria.
	 */
	private void aplicarFiltros() {

		if (filtros == null) {
			return;
		}

		List<Housing> resultado = filtros.aplicar(catalogo);
		filtros.setResultado(resultado.size());
		filtros.setRecuentosPorComodidad(catalogo);

		lista.removeAll();

		// El layout se rehace porque la cuadrícula necesita tres columnas y la lista
		// una. Cambiar el gestor de layout en caliente es legítimo en Swing; lo que hay
		// que recordar es revalidar después, o los componentes se quedan colocados según
		// el layout anterior.
		lista.setLayout(new MigLayout(
				"wrap " + (filtros.esCuadricula() ? COLUMNAS_CUADRICULA : 1) + ", "
						+ Space.insets(0, 0, Space.XXL, 0),
				columnasDe(filtros.esCuadricula()), "[]"));

		if (resultado.isEmpty()) {
			lista.add(estadoVacio(catalogo.isEmpty()),
					"growx" + (filtros.esCuadricula() ? ", span " + COLUMNAS_CUADRICULA : ""));

		} else if (filtros.esCuadricula()) {
			pintarCuadricula(resultado);

		} else {
			pintarLista(resultado);
		}

		lista.revalidate();
		lista.repaint();
	}

	private String columnasDe(boolean cuadricula) {

		if (!cuadricula) {
			return AIRE_LATERAL + "[grow,fill]" + AIRE_LATERAL;
		}

		StringBuilder columnas = new StringBuilder(AIRE_LATERAL);

		for (int i = 0; i < COLUMNAS_CUADRICULA; i++) {
			columnas.append(i == 0 ? "" : String.valueOf(Space.XXL)).append("[grow,fill]");
		}

		return columnas.append(AIRE_LATERAL).toString();
	}

	private void pintarLista(List<Housing> alojamientos) {

		boolean primera = true;

		for (Housing housing : alojamientos) {

			if (!primera) {
				lista.add(Hairline.horizontal(), "growx, h 1!");
			}

			lista.add(fila(housing), "growx");
			primera = false;
		}
	}

	private void pintarCuadricula(List<Housing> alojamientos) {

		for (Housing housing : alojamientos) {
			lista.add(
					new HousingCard(housing, contarResenas(housing), !ocupadosAhora.contains(housing.getId()),
							comparar.estaSeleccionado(housing.getId()), abrir(housing),
							seleccionado -> comparar.alternar(this, housing, seleccionado)),
					"growx, aligny top, gapbottom " + Space.LG);
		}
	}

	private Runnable abrir(Housing housing) {
		return () -> navigator.ir(HousingDetailsFrame.class, frame -> frame.loadDetails(housing));
	}

	private HousingRow fila(Housing housing) {

		// El intercambio es cosa del ADMIN propietario del alojamiento, igual que en el
		// detalle. Se decide aquí y no dentro de la fila: la fila pinta lo que le den,
		// no consulta la sesión. Un componente visual que sabe quién ha iniciado sesión
		// es un componente que ya no se puede reutilizar ni probar por separado.
		// El id va en la navegación y no se omite: sin él, la pantalla de intercambio
		// no sabe qué alojamiento estás ofreciendo y abre vacía. Se coló al escribir
		// esta fila en la Fase 3c, cuando la pantalla de destino todavía era la vieja y
		// no se notaba.
		Runnable intercambiar = puedeIntercambiar(housing)
				? () -> navigator.ir(TradeHousingsFrame.class, frame -> frame.setHousingId(housing.getId()))
				: null;

		return new HousingRow(housing, contarResenas(housing), !ocupadosAhora.contains(housing.getId()),
				comparar.estaSeleccionado(housing.getId()), abrir(housing), intercambiar,
				seleccionado -> comparar.alternar(this, housing, seleccionado));
	}

	private boolean puedeIntercambiar(Housing housing) {

		User usuario = sessionManager.getLoggedInUser();

		return usuario != null && usuario.getRole() == RoleType.ADMIN
				&& housing.getOwner().getId().equals(usuario.getId());
	}

	/**
	 * Cuántas reseñas tiene un alojamiento.
	 *
	 * <p>
	 * Se pide al servicio la lista y se mide, en lugar de contar en la base de
	 * datos. Es una consulta por fila —lo que en jerga se llama el problema "N+1"— y
	 * con un catálogo de seis alojamientos es irrelevante. Se deja así a propósito:
	 * contar en la base exigiría un método nuevo en el DAO, y la capa de interfaz no
	 * habla con los DAOs. Cuando el catálogo crezca, la solución correcta será un
	 * método de servicio que devuelva los recuentos de una vez, no saltarse la capa.
	 */
	private int contarResenas(Housing housing) {

		return resenasPorAlojamiento.computeIfAbsent(housing.getId(), id -> {

			// InstanceNotFoundException y no Exception (Fase 8.3): es la única que
			// declara showHousingReviews, y el genérico se tragaba además cualquier
			// fallo de programación — que aquí se vería como una ficha diciendo "0
			// reseñas" teniendo varias, sin ninguna traza que lo delatara.
			try {
				return reviewService.showHousingReviews(id).size();

			} catch (InstanceNotFoundException ex) {
				return 0;
			}
		});
	}

	/**
	 * Pantalla vacía, con Olaz y un texto que explica <b>por qué</b> está vacía.
	 *
	 * <p>
	 * Los dos casos se dicen distinto a propósito. Que el catálogo esté vacío y que
	 * tus filtros no encuentren nada son situaciones opuestas: en la primera no hay
	 * nada que hacer, en la segunda basta con soltar un chip. Un único mensaje
	 * genérico —"No hay resultados"— dejaría al usuario sin saber en cuál de las dos
	 * está, que es lo único que necesita saber.
	 *
	 * <p>
	 * Es también uno de los sitios donde Olaz aparece <b>en grande</b>: la regla del
	 * proyecto es tamaño según el vacío, y esto es literalmente el vacío.
	 */
	private JPanel estadoVacio(boolean catalogoVacio) {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.HUGE, 0, Space.HUGE, 0), "[grow,fill]",
				"[]" + Space.LG + "[]" + Space.XS + "[]"));
		panel.setOpaque(false);

		panel.add(centrar(new MascotSlot(MascotSlot.Tamano.MEDIANO, Pose.ACCION)));

		if (catalogoVacio) {
			panel.add(centrar(Labels.title(Textos.t("catalogo.vacio.catalogoVacio.titulo"))));
			panel.add(centrar(Labels.muted(Textos.t("catalogo.vacio.catalogoVacio.cuerpo"))));

		} else {
			panel.add(centrar(Labels.title(Textos.t("catalogo.vacio.sinResultados.titulo"))));
			panel.add(centrar(Labels.muted(Textos.t("catalogo.vacio.sinResultados.cuerpo"))));
		}

		return panel;
	}

	private JPanel centrar(JComponent componente) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "push[]push", ""));
		fila.setOpaque(false);
		fila.add(componente);
		return fila;
	}

	// ------------------------------------------------------------------
	// Colofón
	// ------------------------------------------------------------------

	/**
	 * La lista, con el botón de publicar flotando encima en la esquina.
	 *
	 * <p>
	 * <b>Antes había una banda de colofón de 80px</b> con la firma "por CocoBrain" y
	 * este botón. Ochenta píxeles fijos de los que la lista solo tiene unos
	 * seiscientos, y para un botón que ya estaba diseñado como flotante: no
	 * necesitaba banda propia. La firma de CocoBrain sigue en el icono, el splash y
	 * el diálogo "Acerca de", que son tres de sus cuatro sitios previstos.
	 *
	 * <p>
	 * <b>El orden en que se añaden importa y es al revés de lo intuitivo.</b> Swing
	 * pinta los hijos del último índice al primero, así que en una superposición el
	 * que se añade <em>primero</em> queda <em>encima</em>. El botón va antes que la
	 * lista o quedaría debajo de ella.
	 */
	private JPanel listaConBotonFlotante() {

		Capa capa = new Capa(new MigLayout("fill, " + Space.insets(0), "[grow,fill]", "[grow,fill]"));

		accionAdmin = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.SM + "[]", "[]"));
		accionAdmin.setOpaque(false);

		comparar = new BarraComparar(this::aplicarFiltros,
				ids -> navigator.ir(ComparisonFrame.class, frame -> frame.loadHousings(ids)));

		// El botón va con "pos" —posición absoluta, fuera de la rejilla— y la lista con
		// "grow". Es importante que solo uno de los dos ocupe celda: si los dos van en
		// posición absoluta, ningún componente aporta tamaño a la rejilla y MigLayout le
		// da altura cero al contenedor entero.
		capa.add(accionAdmin, "pos null null (container.x2-" + Space.XXXL + ") (container.y2-" + Space.XL + ")");
		capa.add(comparar, "pos (container.x+" + Space.HUGE + ") null null (container.y2-" + Space.XL + ")");
		capa.add(zonaDeLista(), "grow");

		return capa;
	}


	/**
	 * El acceso a publicar alojamiento, solo para ADMIN.
	 *
	 * <p>
	 * Se reconstruye en cada visita en lugar de solo mostrarse u ocultarse. El
	 * motivo es el bug B2: esta pantalla es un singleton de Spring, así que añadir
	 * el botón —y su escucha— en cada {@code setVisible} sin limpiar antes dejaría
	 * dos escuchas la segunda vez y tres la tercera.
	 */
	private void refrescarAccionAdmin() {

		accionAdmin.removeAll();

		User usuario = sessionManager.getLoggedInUser();

		if (usuario != null && usuario.getRole() == RoleType.ADMIN) {

			// Solo el botón, sin el rótulo "Registrar alojamiento" que lo acompañaba.
			// Cuando esto vivía en una banda propia el texto tenía sentido; flotando sobre
			// la lista se superponía al contenido de la ficha de abajo y se leía como un
			// error de maquetación. La explicación pasa al tooltip, que es donde va la
			// ayuda de un botón que ya se entiende por su icono y su posición.
			BotonMas boton = new BotonMas(() -> navigator.ir(UploadHousingFrame.class));
			boton.setToolTipText(Textos.t("catalogo.publicar.tooltip"));
			accionAdmin.add(boton, "w 52!, h 52!");
		}

		accionAdmin.revalidate();
		accionAdmin.repaint();
	}

	// ------------------------------------------------------------------
	// Refrescos
	// ------------------------------------------------------------------

	/**
	 * Reescribe lo que depende solo del idioma (Fase 7.6).
	 *
	 * <p>
	 * <b>Lo que queda aquí es el reparto, no el trabajo.</b> Cada pieza sabe cuáles
	 * son sus textos; esta pantalla solo sabe en qué orden hay que avisarlas. El
	 * reajuste de escala va el último y a propósito: el titular puede pasar a medir
	 * distinto —el inglés no ocupa lo mismo que el español— así que no tiene
	 * sentido medirlo antes de haberlo cambiado.
	 */
	private void actualizarTextosFijos() {

		hero.actualizarTextosFijos();
		comparar.actualizarTextos();

		hero.ajustarEscalaDeDisplay(getWidth());
	}

	/** El nombre con el que la enseña el enlace de atrás de otra pantalla. */
	@Override
	public String nombreDePantalla() {
		return Textos.t("header.nav.catalogo");
	}

}
