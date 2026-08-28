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
import fp.project.actihome.model.services.TileClient;
import fp.project.actihome.model.services.WeatherService;
import fp.project.actihome.ui.components.Avatar;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Card;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.Punto;
import fp.project.actihome.ui.catalog.Destacado;
import fp.project.actihome.ui.components.CalendarioRango;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.components.InlineScore;
import fp.project.actihome.ui.components.FilaFluida;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.Rescate;
import fp.project.actihome.ui.components.WrappingText;
import fp.project.actihome.ui.housings.Galeria;
import fp.project.actihome.ui.housings.MapaDeUbicacion;
import fp.project.actihome.ui.housings.PrevisionPanel;
import fp.project.actihome.ui.nav.ConNombre;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.Contenido;
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
public class HousingDetailsFrame extends JFrame implements ConNombre {

	private static final long serialVersionUID = 1L;

	/** Ver la nota extensa en {@code ShowHousingsFrame.AIRE_LATERAL}. */
	private static final String AIRE_LATERAL = Space.MD + ":" + Space.HUGE + ":" + Space.HUGE;

	private final transient HousingService housingService;

	/** De dónde sale la previsión del {@code PrevisionPanel} (F18). */
	private final transient WeatherService weatherService;

	/** De dónde salen las teselas del {@code MapaDeUbicacion} (F19). */
	private final transient TileClient tileClient;
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
			HeaderPanel headerPanel, WeatherService weatherService, TileClient tileClient) {

		this.housingService = housingService;
		this.reviewService = reviewService;
		this.reservationService = reservationService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;
		this.weatherService = weatherService;
		this.tileClient = tileClient;

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

		Foco.alPulsarEscape(this, () -> navigator.volver(ShowHousingsFrame.class));
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

		// Traduce en segundo plano la descripcion y la cita destacada si faltan, y
		// repinta cuando lleguen. Va despues de reconstruir, no antes: la pantalla se
		// ve al instante y mejora sola.
		Contenido.precalentar(java.util.Arrays.asList(housing.getDescription()), this::reconstruir);
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
		// El aire lateral va como RANGO en la especificacion de columnas, no como
		// insets fijos. Con 44 puntos fijos a cada lado, las dos columnas de la ficha
		// no cabian en una ventana de 1024 y la de la derecha se salia por el borde,
		// donde no hay barra horizontal que la rescate. Ver la nota de AIRE_LATERAL en
		// ShowHousingsFrame: MigLayout no admite rangos en insets, si en gaps.
		contenido.setLayout(new MigLayout("wrap 1, fill, " + Space.insets(0),
				AIRE_LATERAL + "[grow,fill]" + AIRE_LATERAL,
				Space.margen(Space.XL) + "[]" + Space.aire(Space.LG) + "[grow,fill]" + Space.margen(Space.XL)));

		contenido.add(migaDePan(), "growx, " + Layout.anchoCentrado(Layout.FICHA));
		contenido.add(cuerpo(), "grow, " + Layout.anchoCentrado(Layout.FICHA));

		contenido.revalidate();
		contenido.repaint();
	}

	// ------------------------------------------------------------------
	// Miga de pan
	// ------------------------------------------------------------------

	/**
	 * El enlace de atrás, que dice a dónde va de verdad.
	 *
	 * <p>
	 * <b>Era la única migaja de pan de las cinco pantallas que la usan sin pasar
	 * por {@code Buttons.link}</b> (Fase 9): un {@code JLabel} con
	 * {@code MouseAdapter} propio, sin la flecha que sí llevan
	 * {@code ConversationFrame} o {@code DoCheckInFrame} ("Volver a mis
	 * reservas"). El código funcionaba, pero no se leía igual: un botón sin
	 * flecha ni la palabra "volver" es una migaja de pan —sirve para orientarse—,
	 * no un botón de atrás —sirve para salir—. La flecha es lo que lo dice.
	 *
	 * <p>
	 * <b>Y el texto se calcula, no está escrito.</b> A esta ficha se llega desde
	 * el catálogo, desde el buscador y desde la comparativa; un enlace que
	 * dijera siempre "Catálogo" mentiría en dos de los tres casos. Se le pregunta
	 * al {@link Navigator} de dónde se viene — se reconstruye en cada
	 * {@code recargar()}, así que la etiqueta nunca se queda vieja. El texto fijo
	 * queda de reserva para cuando no hay recorrido previo, que es exactamente lo
	 * que decía antes.
	 */
	private JPanel migaDePan() {

		String anterior = navigator.nombreDeLaAnterior();
		String etiqueta = anterior != null ? Textos.t("nav.volverA", anterior) : Textos.t("detalle.volver");

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]", ""));
		fila.setOpaque(false);
		fila.add(Buttons.link(etiqueta, e -> navigator.volver(ShowHousingsFrame.class)));

		return fila;
	}

	// ------------------------------------------------------------------
	// Cuerpo: foto + información
	// ------------------------------------------------------------------

	private JPanel cuerpo() {

		// **Solo crece la columna de la izquierda.** La derecha es texto y acciones, y
		// se queda fija en Layout.COLUMNA_DE_TEXTO: un párrafo más ancho no se lee
		// mejor, se lee peor. La izquierda es la fotografía, y ahí el espacio de más sí
		// es contenido — es la misma distinción que el sistema ya hacía entre un
		// formulario, que se acota, y una lista, que crece.
		//
		// El "300::" del rango dice que puede encogerse hasta 300 antes de rendirse, y
		// el mínimo de la derecha es 340 para que en 1024 puntos las dos quepan.
		JPanel panel = new JPanel(new MigLayout(Space.insets(0),
				"[300::,grow,fill]" + Space.MD + ":" + Space.XXXL + ":" + Space.XXXL + "[340:"
						+ Layout.COLUMNA_DE_TEXTO + ":" + Layout.COLUMNA_DE_TEXTO + ",fill]",
				"[grow,fill]"));
		panel.setOpaque(false);

		// Columna izquierda: la foto y, debajo, el tiempo que hará allí. Las dos cosas
		// contestan "cómo es"; la derecha contesta "qué ofrece y cuánto cuesta".
		//
		// En este hueco estuvo el calendario de ocupación hasta la Fase 8.11 (ver la
		// nota más abajo, donde vivía). La previsión ocupa su sitio pero NO es su
		// sustituto: el calendario prometía una interacción que no daba, y esto no
		// promete ninguna — es información y se lee. Y desaparece sola cuando no la
		// hay, que es justo lo que aquel no sabía hacer.
		JPanel izquierda = new JPanel(
				new MigLayout("wrap 1, hidemode 3, " + Space.insets(0), "[grow,fill]", "[grow,fill]" + Space.aire(Space.LG) + "[]"));
		izquierda.setOpaque(false);

		izquierda.add(galeria(), "grow");

		// **El tiempo y el mapa van uno al lado del otro, no apilados.** Apilados
		// sumaban unos 150 puntos de alto que son justo lo que sacaba la ficha de la
		// pantalla en cualquier portátil: la ficha pedía 926 puntos y un portátil de
		// 1920x1080 al 150 % da 660 útiles. Y no compiten por el ancho, porque
		// contestan la misma pregunta desde dos lados —dónde está y cómo está— así que
		// verlos juntos es incluso mejor lectura que en columna.
		//
		// "hidemode 3" sigue siendo imprescindible: los dos desaparecen sin dejar hueco
		// cuando el alojamiento no tiene coordenadas o la consulta falla, y entonces el
		// que quede se lleva el ancho entero.
		JPanel entorno = new JPanel(
				new MigLayout("hidemode 3, " + Space.insets(0), "[grow,fill]" + Space.LG + "[grow,fill]", "[]"));
		entorno.setOpaque(false);

		entorno.add(new PrevisionPanel(housing, weatherService), "growx, aligny top");
		entorno.add(new MapaDeUbicacion(housing, tileClient), "growx, aligny top");

		izquierda.add(entorno, "growx");

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

		// **El preferido se declara aquí, y es lo que de verdad decidía si la ficha
		// cabía.** La galería vive en una fila con "grow", así que en una ventana alta
		// se estira igual — pero lo que MigLayout usa para calcular cuánto necesita la
		// pantalla entera es el PREFERIDO, y el que traía de serie inflaba la columna
		// izquierda muy por encima de lo que hace falta para ver una foto.
		//
		// Es la distinción que este proyecto ya ha tenido que aprender dos veces: el
		// mínimo dice hasta dónde puede encoger y el preferido dice cuánto pide. Tocar
		// solo el mínimo no cambia nada de lo que se mide.
		galeria.setPreferredSize(new Dimension(0, 260));
		galeria.setMinimumSize(new Dimension(0, 200));

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

		panel.add(referencia(), "gapbottom " + Space.aire(Space.SM));
		panel.add(titulo(), "gapbottom " + Space.aire(Space.MD));
		panel.add(anfitrion(), "gapbottom " + Space.aire(Space.MD));

		// "wmin 0" es imprescindible aquí: un JTextArea sin ese freno reporta como
		// ancho mínimo el de su texto sin partir en líneas, que para una descripción
		// de tres frases es enorme. Sin este freno, MigLayout respeta esa demanda y dejaba
		// la columna del texto invadir la de la foto —el mismo problema, ya documentado en
		// Layout.ancho(), que en su día se llevó por delante el panel oscuro del login—.
		panel.add(descripcion(), "growx, wmin 0, gapbottom " + Space.aire(Space.MD));

		// Solo si hay alguna reseña: un hueco con comillas vacías sería peor que la
		// ausencia. Al no añadirse, el layout no le reserva sitio.
		JPanel cita = citaDestacada();

		if (cita != null) {
			panel.add(cita, "growx, wmin 0, gapbottom " + Space.aire(Space.MD));
		}

		panel.add(miniGrid(), "gapbottom " + Space.aire(Space.MD));

		// La tarjeta se acota a Layout.FORMULARIO y NO ocupa toda la columna. Con el
		// ancho entero, el botón principal medía casi 600 puntos: eso no es un botón de
		// ficha, es la llamada a la acción de una página de aterrizaje, y grita en una
		// pantalla que se sostiene sobre líneas finas y espacio. Es la misma regla del
		// sistema que ya limita los formularios — el espacio sobrante se queda como
		// margen, no se reparte entre los controles.
		//
		// La tarjeta ocupa la columna entera, y ya no hace falta acotarla aquí: la
		// columna es la que está acotada, en Layout.COLUMNA_DE_TEXTO.
		//
		// **Esto costó tres intentos fallidos y merece quedar escrito.** Primero se
		// intentó limitar la tarjeta —restricción en el componente, tope en la columna,
		// columna acotada más un panel sumidero— y las tres fracasaron de formas
		// distintas: dos la dejaron en su ancho natural y la tercera la encogió a un
		// tercio en una ventana ancha, que es como el usuario la vio. El error no
		// estaba en la sintaxis de ninguna, estaba en el sitio: **se estaba acotando el
		// contenido cuando lo que había que acotar era el contenedor.** Una vez la
		// columna tiene un ancho, la tarjeta no necesita ninguno.
		panel.add(tarjetaDeReserva(), "growx, wmin 0");

		return panel;
	}

	/**
	 * El precio, el estado y las acciones, dentro de una superficie propia.
	 *
	 * <p>
	 * <b>Antes eran tres bloques sueltos al final de la columna</b>, y ese era el
	 * problema: el precio flotaba en el aire y "Reservar" tenía exactamente el mismo
	 * peso visual que "Ver reseñas". Nada decía cuál de las tres cosas es la que se
	 * viene a hacer aquí.
	 *
	 * <p>
	 * <b>Lo que hace el marco no es decorar, es jerarquizar.</b> Es la única
	 * superficie contenida de esta pantalla —el resto es texto sobre el fondo— y por
	 * eso manda sin necesidad de gritar con tamaños ni con colores. Es el patrón de
	 * cualquier producto de viajes: el contenido se lee, la transacción se enmarca.
	 *
	 * <p>
	 * <b>La disponibilidad vive aquí y no en la rejilla de datos.</b> En la rejilla
	 * era una celda que decía "Disponible — Sí", que es relleno: la respuesta solo
	 * importa cuando estás decidiendo si reservar, y ese momento es este. Se cuenta
	 * con un punto de color más la palabra, nunca solo con el color.
	 */

	private JComponent tarjetaDeReserva() {

		Card tarjeta = new Card(new MigLayout("wrap 1, " + Space.insets(Space.MD), "[grow,fill]", ""));

		// **La cabecera de la tarjeta va en UNA fila, no en tres.** "DESDE" a la
		// izquierda y el estado a la derecha comparten renglón, y el separador
		// horizontal que había entre medias desaparece: la tarjeta ya tiene un borde
		// propio, así que una línea más dentro solo servía para partir en dos algo que
		// se lee de un vistazo. Entre esto y el ahorro de la columna izquierda, la
		// ficha baja de 926 puntos a caber en un portátil.
		JPanel cabecera = new JPanel(new MigLayout(Space.insets(0), "[]push[]", "[]"));
		cabecera.setOpaque(false);
		cabecera.add(Labels.caps(Textos.t("detalle.reserva.desde")), "aligny center");
		cabecera.add(disponibilidad(), "aligny center");

		tarjeta.add(cabecera);
		tarjeta.add(precio(), "gaptop " + Space.XXS + ", gapbottom " + Space.aire(Space.MD));

		tarjeta.add(acciones(), "growx, wmin 0");

		return tarjeta;
	}

	/** "● Disponible" o "● Reservada", con el punto tintado según cuál sea. */
	private JPanel disponibilidad() {

		boolean libre = housingService.isAvailableNow(housing.getId());

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XS + "[]", "[]"));
		fila.setOpaque(false);

		// El acento de la estación cuando está libre, y el color del texto secundario
		// cuando no. No se usa un verde ni un rojo: esta aplicación no tiene colores de
		// semáforo y meterlos aquí rompería la paleta estacional en la única pantalla
		// donde más se mira. "Ocupado" no es un error, es un hecho, y el gris lo dice
		// mejor que una alarma.
		fila.add(new Punto(libre ? Theme::acc : Theme::mut), "aligny center");
		fila.add(Labels.body(Textos.t(libre ? "catalogo.disponibilidad.disponible" : "catalogo.disponibilidad.reservada")),
				"aligny center");

		return fila;
	}

	/** "Nº 10001 —— Sierra Nevada, Granada", igual que en el catálogo. */
	private JPanel referencia() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.SM + "[]", ""));
		fila.setOpaque(false);

		fila.add(Labels.capsAccent(Textos.t("catalogo.numero") + " " + housing.getHousingCode()));
		fila.add(Labels.caps(housing.getLocation()));

		return fila;
	}

	/**
	 * El nombre del alojamiento, y <b>parte en dos líneas si hace falta</b>.
	 *
	 * <p>
	 * <b>Era un {@code JLabel} y eso lo rompía en ventanas estrechas.</b> Un JLabel
	 * no parte el texto: declara como ancho mínimo el de la frase entera, y
	 * "Casa Rural El Pinar" a cuerpo 46 pide 556 puntos. Al acotar la columna
	 * derecha, esa exigencia empujaba el titular 71 puntos fuera de una ventana de
	 * 1024 — donde no hay barra horizontal que lo rescate, así que sencillamente no
	 * se alcanzaba. Lo detectó {@code MedirResponsive} en el mismo momento de
	 * acotarla; a ojo, en una ventana normal, no se veía nada.
	 *
	 * <p>
	 * Es la regla 5 de la adaptabilidad, aplicada donde no se había aplicado: un
	 * texto que puede ocupar más de una línea nunca va en un {@code JLabel}. Y el
	 * mínimo pasa a ser <b>la palabra más larga</b>, que es el único punto por
	 * debajo del cual ya no hay reflujo posible.
	 */
	private JComponent titulo() {

		WrappingText etiqueta = new WrappingText(housing.getName());
		etiqueta.setFont(cuerpoQueCabeEnUnaLinea(housing.getName()));
		etiqueta.setForeground(Theme.txt());

		return etiqueta;
	}

	/**
	 * El mayor cuerpo de serif con el que el nombre cabe en <b>una sola línea</b>.
	 *
	 * <p>
	 * <b>Es la misma técnica que ya usa el titular del catálogo, y aquí hizo falta
	 * por un efecto secundario de haber acotado la columna.</b> Al pasar el título de
	 * {@code JLabel} a {@code WrappingText} dejó de desbordar —bien— pero empezó a
	 * partirse en dos renglones dentro de una columna de 480, y esas dos líneas
	 * cuestan cuarenta y seis puntos de alto. Un arreglo creó el siguiente.
	 *
	 * <p>
	 * <b>Un texto más pequeño es aceptable; uno cortado o una pantalla que no cabe,
	 * no.</b> Se mide con la métrica real de la fuente cargada y no estimando por
	 * número de caracteres: la serif tiene anchos muy distintos por letra y una
	 * "Villa" no ocupa lo mismo que un "Apartamento".
	 *
	 * <p>
	 * El suelo son 26 puntos. Por debajo dejaría de leerse como el título de la
	 * pantalla, y entonces es mejor que parta en dos líneas — a lo que el
	 * {@code WrappingText} vuelve solo, porque nunca dejó de saber hacerlo.
	 */
	private java.awt.Font cuerpoQueCabeEnUnaLinea(String nombre) {

		int disponible = Layout.COLUMNA_DE_TEXTO - Space.SM;

		for (float cuerpo = Typography.DETAIL_TITLE; cuerpo >= 26f; cuerpo -= 1f) {

			java.awt.Font fuente = Typography.serifMedium(cuerpo);

			if (getFontMetrics(fuente).stringWidth(nombre) <= disponible) {
				return fuente;
			}
		}

		return Typography.serifMedium(26f);
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

		WrappingText cita = new WrappingText("«" + Contenido.de(mejor.getTitle()) + "»");
		cita.setFont(Typography.serifItalic(18f));
		panel.add(cita, "growx, wmin 0");

		panel.add(Labels.caps(Textos.t("resenas.row.por", mejor.getAuthor().getUsername()) + " · "
				+ Formato.nota(mejor.getTotalScore())));

		return panel;
	}

	// Aquí vivía calendarioDeOcupacion(): un CalendarioRango en modo solo lectura
	// con los días ya cogidos. Retirado el 06-08-2026 a petición del usuario, y la
	// justificación merece quedarse escrita porque el fallo era de diseño y no de
	// código.
	//
	// **Prometía una interacción que no daba.** Es el mismo componente que la
	// pantalla de reservar, así que se ve exactamente igual que un calendario en el
	// que se elige — pero aquí no se elegía nada. El usuario lo reportó como "no
	// funciona", y tenía razón: un control que parece pulsable y no lo es está roto
	// aunque el código haga lo que dice su javadoc.
	//
	// **Y no informaba de nada la mayor parte del tiempo.** Sin reservas —el caso
	// normal en un alojamiento recién publicado— pintaba dos meses de días todos
	// iguales, sin leyenda ni título que explicara qué se estaba mirando.
	//
	// **A cambio costaba unos 280 puntos de alto**, justo lo que se estaba peleando
	// para que la ficha entrara en una ventana de 1024.
	//
	// Lo que aportaba —ver la disponibilidad antes de entrar al flujo— sigue
	// disponible a un clic, en Reservar, donde el calendario es de verdad
	// interactivo y no hay ninguna ambigüedad sobre qué hace.

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
		return new WrappingText(Contenido.de(housing.getDescription()));
	}

	/**
	 * Una sola línea con los datos clave: "3 habitaciones · Desayuno, Cena".
	 *
	 * <p>
	 * <b>Era una rejilla de dos celdas con etiqueta encima, y la etiqueta no
	 * aportaba nada.</b> "HABITACIONES / 3 habitaciones" dice dos veces lo mismo, y
	 * "PENSIÓN / Desayuno, Cena" tampoco necesita presentación: nadie lee "Desayuno,
	 * Cena" y se pregunta de qué le están hablando. Un rótulo solo hace falta cuando
	 * el valor es ambiguo sin él.
	 *
	 * <p>
	 * Los cuarenta puntos de alto que se ahorran no son un extra: son parte de lo que
	 * hace que la ficha entera quepa en un portátil sin desplazar la pantalla.
	 *
	 * <p>
	 * La celda "Disponible — Sí" se retiró al crear la tarjeta de reserva: la
	 * respuesta ya estaba en la insignia sobre la foto, y ese dato solo importa
	 * cuando estás decidiendo reservar, no mezclado con el número de habitaciones. Y
	 * el titular salió en la Fase 8.4, porque la tarjeta de anfitrión de arriba dice
	 * lo mismo con cara y con nota.
	 */
	private JComponent miniGrid() {

		String habitaciones = Formato.plural(housing.getNumberOfRooms(),
				Textos.t("palabra.habitacion.singular"), Textos.t("palabra.habitacion.plural"));

		// WrappingText y no Labels.body: es una línea que puede alargarse -"Desayuno,
		// Comida, Cena" en una columna de 480- y un JLabel no parte el texto, declara el
		// ancho entero y desborda el contenedor. Regla 5 de la adaptabilidad.
		return new WrappingText(habitaciones + " · " + resumenPension());
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
		precioLabel.setFont(Typography.sansSemiBold(Typography.PRICE_LG));
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

		// **Una columna, no una fila.** Dentro de la tarjeta el ancho es acotado y una
		// fila con "push" repartiría los tres botones por el borde, que es justo la
		// composición dispersa que la tarjeta viene a corregir. En columna, la acción
		// principal ocupa todo el ancho —que es lo que la convierte en principal, sin
		// necesidad de hacerla más grande— y las secundarias quedan debajo, centradas y
		// en jerarquía descendente.
		JPanel columna = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]"));
		columna.setOpaque(false);

		User usuario = sessionManager.getLoggedInUser();
		boolean esPropietario = usuario != null && usuario.getId().equals(housing.getOwner().getId());

		// **Los dos enlaces comparten renglón; la acción principal no.** Antes iban los
		// tres apilados, y esa tercera fila costaba 56 puntos de alto en la pantalla que
		// menos margen tiene. Juntos no pierden nada: son las dos acciones secundarias,
		// del mismo peso, y leerlas en la misma línea las agrupa mejor que apiladas
		// —donde la última parecía un tercer nivel de jerarquía que no existe—.
		//
		// FilaFluida y no una fila rígida, por la regla 1: son dos textos de ancho muy
		// variable ("Preguntar al propietario" / "Ask the owner") dentro de una columna
		// acotada, y una fila rígida exigiría la suma de ambos. Aquí, si no caben, el
		// segundo baja de línea. Y como FilaFluida coloca cada elemento a su tamaño
		// preferido, ninguno puede quedar aplastado.
		FilaFluida secundarias = new FilaFluida(Space.MD, Space.XS);

		if (usuario != null && usuario.getRole() == RoleType.CUSTOMER) {

			columna.add(Buttons.primary(Textos.t("detalle.accion.reservar"), e -> reservar()),
					"height " + Typography.altoDeBoton() + "!");
			secundarias.add(Buttons.linkAccent(Textos.t("detalle.accion.preguntar"), e -> preguntar()));

		} else if (usuario != null && usuario.getRole() == RoleType.ADMIN && esPropietario) {

			columna.add(Buttons.secondary(Textos.t("detalle.accion.actualizar"), e -> actualizar()),
					"height " + Typography.altoDeBoton() + "!");
			secundarias.add(Buttons.linkAccent(Textos.t("catalogo.row.intercambiar"), e -> intercambiar()));
		}

		secundarias.add(Buttons.link(Textos.t("detalle.accion.verResenas"), e -> verResenas()));
		columna.add(secundarias, "gaptop " + Space.aire(Space.SM));

		return columna;
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

	/**
	 * Esta pantalla se llama como el alojamiento que enseña.
	 *
	 * <p>
	 * No hay clave de textos que valga: "← Villa Aurora" orienta y "← Detalle" no
	 * dice nada. Por eso {@code ConNombre} devuelve texto ya resuelto y no una
	 * clave.
	 */
	@Override
	public String nombreDePantalla() {
		return housing != null ? housing.getName() : null;
	}
}
