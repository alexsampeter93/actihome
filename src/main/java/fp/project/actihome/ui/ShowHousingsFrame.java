package fp.project.actihome.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
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
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.model.services.ReviewService;
import fp.project.actihome.ui.catalog.CatalogFilters;
import fp.project.actihome.ui.catalog.HousingCard;
import fp.project.actihome.ui.catalog.HousingRow;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.SeasonSelector;
import fp.project.actihome.ui.components.Stat;
import fp.project.actihome.ui.components.Toast;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

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
public class ShowHousingsFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	/** Tamaño del titular del hero. Ver la nota en {@code titular()}. */
	private static final float TITULAR = 40f;

	/** Por debajo de esto el titular deja de encogerse: mejor dos lineas que ilegible. */
	private static final float TITULAR_MINIMO = 26f;

	/** Columnas de la vista de cuadrícula, según el handoff. */
	private static final int COLUMNAS_CUADRICULA = 3;

	/**
	 * Cuántos alojamientos como máximo se pueden comparar a la vez (F16). Tres
	 * es el número que cabe en {@link ComparisonFrame} sin scroll horizontal
	 * —la misma cifra que columnas tiene la cuadrícula del catálogo— y más allá
	 * de eso una tabla de comparación deja de leerse de un vistazo.
	 */
	private static final int MAX_COMPARAR = 3;

	private final transient HousingService housingService;
	private final transient ReviewService reviewService;
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
	 * Los alojamientos marcados para comparar (F16), en el orden en que se
	 * marcaron. Se vacía cada vez que se recarga el catálogo: es una selección
	 * de la visita, no una preferencia que deba sobrevivir a un "Actualizar".
	 */
	private final transient Set<Long> seleccionComparar = new LinkedHashSet<>();

	private JPanel accionComparar;
	private JLabel etiquetaComparar;
	private JButton botonComparar;
	private JLabel enlaceCancelarComparar;

	private JLabel tituloPrimera;
	private JLabel tituloSegunda;
	private JLabel fraseEstacional;
	private Stat enCatalogo;
	private Stat disponibles;
	private Stat media;
	private JPanel accionAdmin;
	private JPanel heroCompleto;
	private JPanel heroCompacto;
	private JLabel resumenCompacto;
	private JLabel tituloCompacto;
	private boolean heroContraido;
	private JPanel controlesHero;

	/**
	 * Cuántas veces tiene que caber el hero completo en la ventana para desplegarse.
	 * Ver {@link #elHeroCompletoSeGanaSuSitio()}.
	 */
	private static final int PARTE_DE_VENTANA_PARA_EL_HERO = 5;

	/**
	 * Testigo de la suscripción a los cambios de estación.
	 *
	 * <p>
	 * {@link Theme#alCambiar} devuelve un objeto con el que darse de baja. Guardarlo
	 * no es opcional: un oyente que nunca se retira mantiene viva la pantalla
	 * entera aunque se cierre, y eso es una fuga de memoria de manual.
	 */
	private transient Object suscripcion;

	public ShowHousingsFrame(HousingService housingService, ReviewService reviewService, SessionManager sessionManager,
			Navigator navigator, HeaderPanel headerPanel) {

		this.housingService = housingService;
		this.reviewService = reviewService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			headerPanel.refresh();
			filtros.actualizarTextos();
			refrescarAccionAdmin();
			cargarAlojamientos();
			actualizarTextosEstacionales();
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

		// "hidemode 3" para que el hero oculto no reserve su hueco: es lo que permite
		// que al contraerse la lista gane el espacio de verdad y no quede un vacío.
		JPanel raiz = new Page(new MigLayout("wrap 1, fill, hidemode 3, " + Space.insets(0), "[grow,fill]",
				"[]0[]0[]0[]0[grow,fill]"));

		filtros = new CatalogFilters(this::aplicarFiltros);

		heroCompleto = hero();
		heroCompacto = heroCompacto();

		raiz.add(headerPanel, "growx");
		raiz.add(heroCompleto, "growx");
		raiz.add(heroCompacto, "growx");
		raiz.add(filtros, "growx");
		raiz.add(listaConBotonFlotante(), "grow");

		setContentPane(raiz);

		// Cambiar de estación no solo cambia colores: cambia también las palabras (la
		// frase editorial de la estación). Los colores se resuelven solos al repintar;
		// el texto hay que reescribirlo.
		suscripcion = Theme.alCambiar(estacion -> actualizarTextosEstacionales());

		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {

				ajustarEscalaDeDisplay();

				// El hero depende del alto de la ventana desde que se despliega solo cuando
				// se gana su sitio, así que redimensionar es tan motivo para recalcularlo
				// como desplazar la lista. Sin esto, agrandar la ventana no devolvía el
				// titular hasta que además se tocara la rueda.
				ajustarHeroAlScroll();
			}
		});
	}

	// ------------------------------------------------------------------
	// Hero
	// ------------------------------------------------------------------

	/**
	 * El hero reducido a una línea, para cuando el usuario ya está explorando.
	 *
	 * <p>
	 * <b>La idea es de Airbnb y resuelve una tensión real.</b> El titular editorial
	 * es lo que le da carácter a la aplicación, pero mientras recorres fichas no
	 * aporta nada y se lleva 140px de los 866 que hay. Con dos versiones no hay que
	 * elegir: al llegar se ve el titular completo —el impacto— y al bajar por la
	 * lista se contrae a una línea que sigue diciendo dónde estás, devolviendo el
	 * espacio al contenido.
	 *
	 * <p>
	 * Es una banda distinta y no el mismo hero encogido a propósito: cambiar tamaños
	 * de fuente y márgenes a mitad de animación produce saltos de reflujo, mientras
	 * que alternar dos paneles ya construidos es instantáneo y no recalcula nada.
	 */
	private JPanel heroCompacto() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(Space.SM, Space.HUGE, Space.SM, Space.HUGE),
				"[]" + Space.MD + "[]push[]", "[]"));
		panel.setOpaque(false);
		panel.setVisible(false);

		resumenCompacto = Labels.capsAccent("");
		panel.add(resumenCompacto, "aligny center");

		tituloCompacto = Labels.body("");
		tituloCompacto.setFont(Typography.serifMedium(19f));
		panel.add(tituloCompacto, "aligny center");

		return panel;
	}

	/**
	 * Decide si toca el hero completo o el compacto, según lo desplazada que esté la
	 * lista <b>y según lo alta que sea la ventana</b>.
	 *
	 * <p>
	 * <b>El umbral tiene histéresis a propósito</b>: se contrae al pasar de 60px y
	 * no se despliega hasta bajar de 20. Con un único umbral, quedarse justo en el
	 * límite hace que el hero parpadee entre los dos estados a cada píxel de scroll,
	 * y ese temblor es mucho peor que cualquiera de los dos estados.
	 */
	private void ajustarHeroAlScroll() {

		if (scroll == null) {
			return;
		}

		int desplazamiento = scroll.getVerticalScrollBar().getValue();
		boolean contraer = !elHeroCompletoSeGanaSuSitio()
				|| (heroContraido ? desplazamiento > 20 : desplazamiento > 60);

		if (contraer == heroContraido) {
			return;
		}

		heroContraido = contraer;

		heroCompleto.setVisible(!contraer);
		heroCompacto.setVisible(contraer);

		if (contraer) {
			actualizarHeroCompacto();
		}

		revalidate();
		repaint();
	}

	/**
	 * Si la ventana da de sí lo bastante como para que el hero completo valga lo
	 * que cuesta.
	 *
	 * <p>
	 * <b>El problema que resuelve es de primera impresión, y solo existía en
	 * ventanas bajas.</b> El hero ya se contraía al bajar por la lista desde la Fase
	 * 7.11, así que en cuanto el usuario mueve la rueda la lista se queda con el
	 * 74 % de la pantalla. Pero <b>antes de mover nada</b> —que es cuando alguien se
	 * hace una idea de qué es esta aplicación— el reparto sigue siendo el de
	 * partida, y en un portátil de 1280×660 eso deja la lista en 313px: <b>un
	 * alojamiento, y cortado</b>. En un catálogo, la primera pantalla debería
	 * enseñar catálogo.
	 *
	 * <p>
	 * <b>La regla, dicha entera: el titular no puede llevarse más de un quinto de la
	 * ventana.</b> Con el hero completo midiendo unos 165 puntos, eso significa que
	 * se despliega a partir de unos 825 de alto y arranca contraído por debajo. En
	 * un monitor de escritorio no cambia nada; en el portátil del cliente, la lista
	 * pasa de 313 a 423 puntos sin tocar una sola constante de diseño.
	 *
	 * <p>
	 * <b>No es una excepción a la identidad editorial, es la regla del proyecto
	 * aplicada a lo que toca.</b> "Cuando falta sitio, lo que cede es el aire, nunca
	 * un elemento con el que se interactúa": el hero <em>es</em> aire —una frase de
	 * estación y un titular— y la lista es el contenido. Lo que no se hace es
	 * quitarlo en las ventanas donde sí cabe, porque ahí no le quita el sitio a
	 * nadie.
	 *
	 * <p>
	 * Se mide contra el alto <b>de la ventana</b> y no contra el que le queda a la
	 * lista, y esa elección importa: el alto de la lista depende de si el hero está
	 * contraído, así que decidir con él realimenta la propia decisión y el hero
	 * oscilaría entre los dos estados. El de la ventana no depende de nada de esto.
	 */
	private boolean elHeroCompletoSeGanaSuSitio() {

		int altoDeVentana = getContentPane().getHeight();

		// Antes del primer pase de layout todavía no hay alto. Se responde que sí
		// porque el hero completo es el estado de partida y así no hay un parpadeo de
		// contraído a desplegado nada más abrirse la pantalla.
		if (altoDeVentana <= 0) {
			return true;
		}

		return heroCompleto.getPreferredSize().height * PARTE_DE_VENTANA_PARA_EL_HERO <= altoDeVentana;
	}

	private void actualizarHeroCompacto() {

		resumenCompacto.setText(Theme.estacion().nombre().toUpperCase());
		tituloCompacto.setText(Formato.plural(catalogo.size(), Textos.t("palabra.estancia.singular"),
				Textos.t("palabra.estancia.plural")) + " " + Textos.t("catalogo.compacto.paraElegir"));
	}

	private JPanel hero() {

		// Los márgenes del hero son menores que los del handoff (48/44/30). En una
		// ventana de escritorio el alto es el recurso escaso: cada píxel que se lleva
		// el hero se lo quita a la lista, que es lo único que el usuario ha venido a
		// mirar. El aire lateral se mantiene, que es el que se nota.
		JPanel panel = new JPanel(new MigLayout(Space.insets(Space.XL, 0, Space.SM, 0),
				Space.LG + ":" + Space.HUGE + ":" + Space.HUGE + "[grow]" + Space.MD + ":" + Space.XXXL + ":"
						+ Space.XXXL + "[]" + Space.LG + ":" + Space.HUGE + ":" + Space.HUGE,
				"[]"));
		panel.setOpaque(false);

		// El titular NO lleva el tope de Layout.TEXTO que tenía antes. Ese tope está
		// pensado para columnas de texto legible —una línea muy larga cansa de leer— y
		// un titular de display no es eso. Con el tope puesto y la fuente escalada a
		// ×1.35 en pantallas grandes, la frase pedía más de los 560px permitidos y se
		// quedaba en "Elige dónde quieres desperta": la última letra, cortada.
		panel.add(titular(), "growx, aligny bottom");

		controlesHero = controles();
		panel.add(controlesHero, "aligny bottom");

		return panel;
	}

	private JPanel titular() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.SM + "[]"));
		panel.setOpaque(false);

		fraseEstacional = Labels.capsAccent(Theme.estacion().etiqueta());
		panel.add(fraseEstacional);

		// En UNA línea, no en dos como el mockup.
		//
		// El handoff parte el titular porque en una web sobra alto. Aquí el alto es lo
		// que se le quita al catálogo: la segunda línea costaba sesenta píxeles, que es
		// casi una cuarta parte de una ficha. En una línea el titular funciona además
		// como cabecera de revista, que es exactamente la referencia del diseño.
		//
		// Son dos etiquetas seguidas y no una con marcado: el renderizado HTML de Swing
		// calcula sus tamaños por su cuenta y se lleva mal con las fuentes registradas
		// en tiempo de ejecución.
		//
		// El handoff pone "despertar" en cursiva, y así estuvo hasta que el usuario
		// pidió quitarla. Es una desviación consciente del diseño: una cursiva serif de
		// verdad no es la redonda inclinada, lleva las letras dibujadas aparte, y ese
		// cambio de forma en mitad de la frase se percibía como que la palabra estaba
		// en otra tipografía. El titular pierde el énfasis, pero gana uniformidad.
		// La separación entre las dos etiquetas es la de un espacio de esta fuente a
		// este cuerpo, medida — no una constante. Era Space.SM (12px), ajustado a ojo
		// con Spectral; al cambiar a Fraunces (Fase 8.2), que tiene el espacio más
		// estrecho, esos mismos 12px se leían como dos espacios seguidos en mitad del
		// titular.
		int espacio = Typography.anchoDeEspacio(Typography.serifMedium(TITULAR));

		JPanel linea = new JPanel(new MigLayout(Space.insets(0), "[]" + espacio + "[]push", "[]"));
		linea.setOpaque(false);

		tituloPrimera = Labels.hero(Textos.t("catalogo.hero.titulo1"));
		tituloPrimera.setFont(Typography.serifMedium(TITULAR));

		tituloSegunda = Labels.hero(Textos.t("catalogo.hero.titulo2"));
		tituloSegunda.setFont(Typography.serifMedium(TITULAR));

		linea.add(tituloPrimera, "aligny bottom");
		linea.add(tituloSegunda, "aligny bottom");

		panel.add(linea);

		return panel;
	}

	/**
	 * La columna derecha del hero: solo las tres cifras.
	 *
	 * <p>
	 * El selector de estación estaba aquí y ha subido a la cabecera. Dos motivos, y
	 * el segundo es el importante: recupera alto para la lista, y sobre todo deja de
	 * ser alcanzable <b>solo</b> desde esta pantalla — antes, estando en el detalle
	 * o en un formulario no había forma de cambiar de estación.
	 */
	private JPanel controles() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.MD + "[]"));
		panel.setOpaque(false);

		// El buscador ocupa exactamente el hueco que dejó el selector de estación al
		// subir a la cabecera, así que se recupera su banda de ~85px sin que el hero
		// crezca ni un píxel. Ponerlo bajo el titular, en la columna izquierda, fue el
		// primer intento y salía más caro: el hero pasaba de 183 a 231px.
		// El ancho del buscador es un rango, no un número. Con "w 340!" el hero exigía
		// 340 puntos pasara lo que pasara, y en una ventana estrecha esa exigencia se
		// traducía en que el campo se dibujaba saliéndose por la derecha. Con
		// "220:340:340" mide lo que el diseño pide mientras haya sitio y se estrecha
		// hasta un ancho todavía cómodo de escribir cuando no lo hay.
		panel.add(filtros.extraerBuscador(), "w 220:340:340, h " + Typography.altoDeControl() + "!, alignx right");
		panel.add(cifras());

		return panel;
	}

	private JPanel cifras() {

		String aire = Space.SM + ":" + Space.XL + ":" + Space.XL;

		JPanel panel = new JPanel(
				new MigLayout(Space.insets(0), "push[]" + aire + "[1!]" + aire + "[]" + aire + "[1!]" + aire + "[]", "[]"));
		panel.setOpaque(false);

		enCatalogo = new Stat("0", Textos.t("catalogo.stat.enCatalogo"));
		disponibles = new Stat("0", Textos.t("catalogo.stat.disponibles"));
		media = new Stat("—", Textos.t("catalogo.stat.media"), true);

		panel.add(enCatalogo);
		panel.add(Hairline.vertical(), "growy");
		panel.add(disponibles);
		panel.add(Hairline.vertical(), "growy");
		panel.add(media);

		return panel;
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


		lista = new JPanel(new MigLayout("wrap 1, " + Space.insets(0, Space.HUGE, Space.XXL, Space.HUGE),
				"[grow,fill]", "[]"));
		lista.setOpaque(false);

		scroll = new JScrollPane(lista);
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
		seleccionComparar.clear();
		actualizarBarraComparar();

		actualizarCifras(catalogo);
		filtros.setUbicaciones(catalogo);
		aplicarVistaPorDefecto();
		aplicarFiltros();
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
						+ Space.insets(0, Space.HUGE, Space.XXL, Space.HUGE),
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
			return "[grow,fill]";
		}

		StringBuilder columnas = new StringBuilder();

		for (int i = 0; i < COLUMNAS_CUADRICULA; i++) {
			columnas.append(i == 0 ? "" : String.valueOf(Space.XXL)).append("[grow,fill]");
		}

		return columnas.toString();
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
							seleccionComparar.contains(housing.getId()), abrir(housing),
							seleccionado -> alternarComparacion(housing, seleccionado)),
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
				seleccionComparar.contains(housing.getId()), abrir(housing), intercambiar,
				seleccionado -> alternarComparacion(housing, seleccionado));
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

		JPanel capa = new JPanel(new MigLayout("fill, " + Space.insets(0), "[grow,fill]", "[grow,fill]"));
		capa.setOpaque(false);

		accionAdmin = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.SM + "[]", "[]"));
		accionAdmin.setOpaque(false);

		accionComparar = barraComparar();

		// El botón va con "pos" —posición absoluta, fuera de la rejilla— y la lista con
		// "grow". Es importante que solo uno de los dos ocupe celda: si los dos van en
		// posición absoluta, ningún componente aporta tamaño a la rejilla y MigLayout le
		// da altura cero al contenedor entero.
		capa.add(accionAdmin, "pos null null (container.x2-" + Space.XXXL + ") (container.y2-" + Space.XL + ")");
		capa.add(accionComparar, "pos (container.x+" + Space.HUGE + ") null null (container.y2-" + Space.XL + ")");
		capa.add(zonaDeLista(), "grow");

		return capa;
	}

	/**
	 * Barra flotante que aparece al marcar dos o más alojamientos para comparar
	 * (F16), en la esquina opuesta al botón de publicar.
	 *
	 * <p>
	 * Reutiliza el mismo idioma visual que {@link Toast} —bloque sólido con
	 * {@code Theme.hdr()} y texto claro— en vez de inventar una superficie nueva:
	 * es la única pieza del sistema pensada para flotar sobre el contenido con
	 * un fondo propio.
	 */
	private JPanel barraComparar() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(Space.SM, Space.LG, Space.SM, Space.LG),
				"[]" + Space.MD + "[]" + Space.MD + "[]", "[]"));
		panel.setOpaque(true);
		panel.setBackground(Theme.hdr());
		panel.setVisible(false);

		etiquetaComparar = Labels.onHeader("");
		panel.add(etiquetaComparar, "aligny center");

		botonComparar = Buttons.primary(Textos.t("catalogo.comparar.boton"),
				e -> navigator.ir(ComparisonFrame.class, frame -> frame.loadHousings(new ArrayList<>(seleccionComparar))));
		panel.add(botonComparar, "aligny center");

		enlaceCancelarComparar = Labels.onHeader(Textos.t("catalogo.comparar.cancelar"));
		enlaceCancelarComparar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		enlaceCancelarComparar.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				seleccionComparar.clear();
				actualizarBarraComparar();
				aplicarFiltros();
			}
		});
		panel.add(enlaceCancelarComparar, "aligny center");

		return panel;
	}

	/**
	 * Marca o desmarca un alojamiento en la selección de comparar.
	 *
	 * <p>
	 * Rechazar el {@value #MAX_COMPARAR}+1 exige repintar la lista entera: el
	 * chip ya se dibujó marcado en cuanto el usuario lo pulsó —así es como
	 * responde un {@code JToggleButton}— y la única forma de devolverlo a su
	 * estado real, sin guardar una referencia al chip concreto, es reconstruir
	 * la fila desde el estado que sí es la fuente de verdad: {@link #seleccionComparar}.
	 */
	private void alternarComparacion(Housing housing, boolean seleccionado) {

		if (seleccionado) {

			if (seleccionComparar.size() >= MAX_COMPARAR) {
				Toast.mostrar(this,
						Textos.t("catalogo.comparar.limite.prefijo") + " " + MAX_COMPARAR + " "
								+ Textos.t("catalogo.comparar.limite.sufijo"));
				aplicarFiltros();
				return;
			}

			seleccionComparar.add(housing.getId());

		} else {
			seleccionComparar.remove(housing.getId());
		}

		actualizarBarraComparar();
	}

	private void actualizarBarraComparar() {

		if (accionComparar == null) {
			return;
		}

		boolean visible = seleccionComparar.size() >= 2;
		accionComparar.setVisible(visible);

		if (visible) {
			etiquetaComparar.setText(seleccionComparar.size() + " " + Textos.t("catalogo.comparar.seleccionados"));
		}
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

	private void actualizarCifras(List<Housing> alojamientos) {

		int total = alojamientos.size();
		int libres = 0;
		double suma = 0;
		int conNota = 0;

		for (Housing housing : alojamientos) {

			if (!ocupadosAhora.contains(housing.getId())) {
				libres++;
			}

			if (housing.getScore() != null) {
				suma += housing.getScore();
				conNota++;
			}
		}

		enCatalogo.setValor(String.valueOf(total));
		disponibles.setValor(String.valueOf(libres));
		media.setValor(conNota == 0 ? "—" : Formato.nota(suma / conNota));
	}

	private void actualizarTextosEstacionales() {

		fraseEstacional.setText(Theme.estacion().etiqueta());
		repaint();
	}

	/**
	 * Vuelve a fijar los textos fijos que no dependen de la estación, sino solo
	 * del idioma (Fase 7.6): el titular, las tres cifras y —si la lista está
	 * desplazada— el resumen compacto.
	 *
	 * <p>
	 * El titular puede pasar a medir distinto (el inglés no ocupa lo mismo que el
	 * español), así que se reajusta la escala después de cambiarlo — el mismo
	 * mecanismo que ya usa {@code ajustarEscalaDeDisplay()} al redimensionar la
	 * ventana.
	 */
	private void actualizarTextosFijos() {

		tituloPrimera.setText(Textos.t("catalogo.hero.titulo1"));
		tituloSegunda.setText(Textos.t("catalogo.hero.titulo2"));

		enCatalogo.setRotulo(Textos.t("catalogo.stat.enCatalogo"));
		disponibles.setRotulo(Textos.t("catalogo.stat.disponibles"));
		media.setRotulo(Textos.t("catalogo.stat.media"));

		botonComparar.setText(Textos.t("catalogo.comparar.boton"));
		enlaceCancelarComparar.setText(Textos.t("catalogo.comparar.cancelar"));
		actualizarBarraComparar();

		if (heroContraido) {
			actualizarHeroCompacto();
		}

		ajustarEscalaDeDisplay();
	}

	/** Los titulares crecen con la ventana; el cuerpo de texto nunca. */
	/**
	 * Escala el titular con el tamaño de la ventana, <b>sin dejar nunca que se
	 * corte</b>.
	 *
	 * <p>
	 * <b>Escalar a ciegas no basta, y este fue el fallo.</b> La regla del sistema
	 * dice que la tipografía de display crece en ventanas grandes
	 * ({@code Layout.display}), y así estaba: ×1.35 por encima de cierto ancho. Pero
	 * nadie comprobaba que la frase resultante cupiera, así que en un monitor de 27
	 * pulgadas el titular pedía más sitio del que tenía y se quedaba en "Elige dónde
	 * quieres <b>desperta</b>". Un texto cortado es peor que un texto pequeño.
	 *
	 * <p>
	 * Ahora el tamaño que devuelve la regla es el <em>punto de partida</em>, no la
	 * última palabra: se mide lo que ocuparía la frase con esa fuente y se va
	 * bajando hasta que entra en el sitio real que le queda al titular. La medida se
	 * hace con {@code FontMetrics}, que es lo mismo que usará Swing al dibujar, así
	 * que no hay estimaciones de por medio.
	 */
	private void ajustarEscalaDeDisplay() {

		int ancho = getWidth();

		if (ancho <= 0 || tituloPrimera == null) {
			return;
		}

		float tamano = Layout.display(TITULAR, ancho);
		int disponible = anchoParaElTitular();

		while (tamano > TITULAR_MINIMO && anchoDelTitular(tamano) > disponible) {
			tamano -= 1f;
		}

		tituloPrimera.setFont(Typography.serifMedium(tamano));
		tituloSegunda.setFont(Typography.serifMedium(tamano));

		revalidate();
		repaint();
	}

	/** Lo que mediría la frase completa dibujada con ese cuerpo. */
	private int anchoDelTitular(float tamano) {

		FontMetrics metrica = getFontMetrics(Typography.serifMedium(tamano));

		return metrica.stringWidth(tituloPrimera.getText()) + Space.SM + metrica.stringWidth(tituloSegunda.getText());
	}

	/**
	 * El ancho que le queda de verdad al titular: la ventana menos los márgenes del
	 * hero, la columna de la derecha y el hueco entre ambas.
	 */
	private int anchoParaElTitular() {

		int derecha = controlesHero == null ? 360 : controlesHero.getPreferredSize().width;

		return getWidth() - Space.HUGE * 2 - Space.XXXL - derecha;
	}

	/** Botón circular de "añadir", del handoff: sin degradados ni animaciones. */
	private static class BotonMas extends JComponent {

		private static final long serialVersionUID = 1L;

		private final transient Runnable accion;
		private boolean encima;

		BotonMas(Runnable accion) {

			this.accion = accion;

			setPreferredSize(new Dimension(44, 44));
			setMinimumSize(new Dimension(44, 44));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText(Textos.t("catalogo.publicar.tooltipCorto"));

			addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					accion.run();
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					encima = true;
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e) {
					encima = false;
					repaint();
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int lado = Math.min(getWidth(), getHeight());
			Color fondo = encima ? Theme.acc() : Theme.hdr();

			g2.setColor(fondo);
			g2.fillOval(0, 0, lado, lado);

			g2.setColor(Theme.bg());
			int centro = lado / 2;
			int brazo = lado / 6;
			g2.fillRect(centro - brazo, centro - 1, brazo * 2, 2);
			g2.fillRect(centro - 1, centro - brazo, 2, brazo * 2);

			g2.dispose();
		}
	}
}
