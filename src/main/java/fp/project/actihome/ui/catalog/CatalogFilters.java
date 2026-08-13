package fp.project.actihome.ui.catalog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Amenity;
import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Chip;
import fp.project.actihome.ui.components.Contador;
import fp.project.actihome.ui.components.IconoDeTipo;
import fp.project.actihome.ui.components.FilaFluida;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.OptionLinks;
import fp.project.actihome.ui.components.SearchField;
import fp.project.actihome.ui.components.Segmented;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Typography;

/**
 * Los filtros del catálogo: el estado y los dos controles que lo manejan.
 *
 * <p>
 * <b>El estado vive aquí, no en la pantalla.</b> La pantalla pregunta "dame los
 * alojamientos que pasan el filtro" ({@link #aplicar(List)}) y pinta el
 * resultado; no sabe qué chips hay pulsados ni en qué orden se ordena. Esa
 * separación es lo que permite que añadir un filtro nuevo mañana no toque una
 * línea del catálogo.
 *
 * <p>
 * <b>Tres bandas, pero apretadas.</b> El primer intento las agrupó en dos para
 * ganar altura, y no cupieron: siete chips de comodidad, cinco de tipo, tres
 * opciones de orden y sus rótulos suman más ancho del que tiene la ventana, así
 * que el contenido se salía por la derecha y se comía el selector de estación.
 * Es la lección de siempre en escritorio —<b>el ancho tampoco es infinito</b>— y
 * la solución fue repartir en tres bandas bajas en lugar de dos altas, y
 * recuperar el alto perdido en el hero y el colofón, que sí sobraban.
 *
 * <p>
 * El reparto sigue una lógica: primero lo que <em>busca</em> (texto, mínimo de
 * habitaciones, recuento y vista), luego lo que <em>clasifica</em> (tipo y
 * orden) y por último lo que <em>acota</em> (comodidades).
 *
 * <p>
 * <b>Todos los filtros se combinan con Y.</b> Elegir "Villa" y marcar "Piscina"
 * muestra las villas con piscina, no la suma de ambas cosas. Es lo que espera
 * cualquiera que haya usado un buscador de alojamientos, y conviene decirlo
 * porque la alternativa —cada filtro ampliando el resultado— también existe en
 * otros contextos y produce una experiencia desconcertante.
 */
public class CatalogFilters extends JPanel {

	private static final long serialVersionUID = 1L;

	/** El valor del chip de tipo que no filtra nada. */
	public static final String TODOS = "Todos";

	/**
	 * El valor del desplegable de ciudad que no filtra nada.
	 *
	 * <p>
	 * A diferencia de {@link #TODOS}, la ciudad no es una categoría cerrada como
	 * el tipo: se lee de {@code Housing.location}, texto libre. El desplegable se
	 * rellena en tiempo de ejecución con las ubicaciones que de verdad hay en el
	 * catálogo ({@link #setUbicaciones(List)}), así que este centinela es el
	 * único valor fijo de la lista.
	 */
	private static final String TODAS_LAS_CIUDADES = "__todas__";

	/**
	 * Las categorías de alojamiento, en el orden del diseño.
	 *
	 * <p>
	 * <b>Son el valor real de {@code Housing.type}, no solo texto de chip.</b> Se
	 * guardan y se comparan tal cual, siempre en español: lo único que cambia con
	 * el idioma es la etiqueta que se muestra, vía {@link Textos#tipoDeAlojamiento}.
	 * Ver la nota de esa clase.
	 */
	private static final String[] TIPOS = { TODOS, "Casa", "Apartamento", "Villa", "Cabaña" };

	private static final int ORDEN_PUNTUACION = 0;
	private static final int ORDEN_PRECIO_ASC = 1;

	/**
	 * El margen lateral de las bandas, como rango en vez de como número.
	 *
	 * <p>
	 * La sintaxis {@code "20:44:44"} es mínimo:preferido:máximo. El aire de 44 puntos
	 * es el que pide el diseño y el que se ve mientras haya sitio; cuando la ventana
	 * viene estrecha —un portátil con el escalado al 150 % recibe 1280 puntos
	 * lógicos— el margen cede hasta 20 antes de que nada del contenido se salga. El
	 * aire se negocia; un filtro dibujado fuera de la ventana, no.
	 */
	private static final String MARGEN_LATERAL = Space.LG + ":" + Space.HUGE + ":" + Space.HUGE;

	/** Índice de la vista de cuadrícula en el conmutador. */
	public static final int VISTA_CUADRICULA = 1;

	/**
	 * La forma de los controles de "Más filtros" (Fase 9): precio, huéspedes y
	 * ciudad.
	 *
	 * <p>
	 * <b>Cápsula, y del mismo alto que todo lo demás.</b> El primer intento fue
	 * bajarlos seis puntos y matarles un poco las esquinas: quedaron enanos
	 * respecto a la banda de arriba, el precio máximo salía cortado dentro de su
	 * propia celda y el rectángulo seguía siendo un rectángulo. Lo que
	 * desentonaba no era el tamaño sino la silueta — cajas rectas rodeadas de
	 * chips, que son cápsulas. Ahora comparten forma con sus vecinos y altura
	 * con el resto de la aplicación.
	 *
	 * <p>
	 * El 999 del radio no es un número mágico: Java2D recorta el arco al lado
	 * menor de la figura, así que cualquier valor grande significa "todo lo
	 * redondo que se pueda". Es el mismo truco con el que {@code ActiHomeTheme}
	 * redondea el pulgar de las barras de scroll.
	 */
	private static final int RADIO_PILDORA = 999;
	private static final int ALTO_CONTROL = Typography.altoDeControlCompacto();

	private final transient Runnable alCambiar;

	private SearchField buscador;
	private JLabel etiquetaTipo;
	private Contador minimoHabitaciones;
	private JLabel etiquetaMinHab;
	private JLabel etiquetaOrdenar;
	private JLabel etiquetaComodidades;
	private JLabel recuento;
	private Segmented vista;
	private OptionLinks orden;

	private Chip masFiltros;
	private JPanel avanzadoVisible;
	private final transient java.util.Map<Amenity, Chip> chipsPorComodidad = new java.util.EnumMap<>(Amenity.class);
	private final transient java.util.Map<String, Chip> chipsPorTipo = new java.util.LinkedHashMap<>();

	private JLabel etiquetaPrecio;
	private Contador precioMinimo;
	private Contador precioMaximo;
	private JLabel etiquetaCiudad;
	private JComboBox<String> ciudad;
	private boolean actualizandoCiudades;

	private JLabel etiquetaHuespedes;
	private Contador huespedesMinimos;

	private JLabel etiquetaFechas;
	private JLabel resumenFechas;
	private javax.swing.JButton quitarFechas;

	private String tipo = TODOS;
	private final EnumSet<Amenity> comodidades = EnumSet.noneOf(Amenity.class);

	/**
	 * El rango de fechas que llega del buscador de destino (Fase 9), o
	 * {@code null} si no se ha pedido ninguno.
	 *
	 * <p>
	 * No lo elige nadie desde aquí —este panel no lleva calendario propio—, solo
	 * se aplica y se puede quitar. Por eso vive junto a {@link #idsNoDisponibles},
	 * que es lo que de verdad filtra: las fechas son lo que se enseña, el
	 * conjunto de ids es lo que se comprueba.
	 */
	private LocalDate fechaEntrada;
	private LocalDate fechaSalida;
	private transient Set<Long> idsNoDisponibles = Collections.emptySet();

	public CatalogFilters(Runnable alCambiar) {

		// El "hidemode 3" va aquí, en el layout que CONTIENE la banda de comodidades, no
		// en el layout interno de esa banda. Es un despiste fácil y silencioso: puesto
		// dentro, la banda oculta no dibuja nada pero su fila sigue reservada en el
		// padre, así que se ocultaba sin recuperar el espacio. Quien decide si un hueco
		// existe es siempre el contenedor, no el contenido.
		super(new MigLayout("wrap 1, hidemode 3, " + Space.insets(0), "[grow,fill]", "[]0[]0[]"));

		this.alCambiar = alCambiar;
		setOpaque(false);

		// Se construye el buscador aunque no se añada aquí: se lo lleva el hero con
		// extraerBuscador(). El estado sigue viviendo en esta clase, que es quien filtra.
		buscador = new SearchField(Textos.t("catalogo.buscador.placeholder"), this::notificar);

		add(Hairline.horizontal(), "growx, h 1!");
		add(bandaDeClasificado(), "growx");
		add(bandaAvanzada(), "growx");
	}

	/**
	 * Entrega el buscador para que lo coloque el hero.
	 *
	 * <p>
	 * <b>Por qué se cede el componente en lugar de duplicarlo.</b> El buscador vivía
	 * en una banda propia de esta clase, que costaba unos 85px de alto. Moverlo al
	 * hero recupera ese espacio, pero el <em>estado</em> —lo que hay escrito— tiene
	 * que seguir aquí, porque es esta clase la que filtra. Así que se comparte el
	 * mismo objeto: lo pinta el hero, lo lee el filtro. Un campo en el hero con su
	 * propia variable y un {@code addChangeListener} para sincronizarlo habría sido
	 * el mismo dato en dos sitios, que es como se desincronizan las cosas.
	 */
	public SearchField extraerBuscador() {
		return buscador;
	}

	// ------------------------------------------------------------------
	// Construcción
	// ------------------------------------------------------------------

	/**
	 * La única banda visible: tipo, mínimo de habitaciones, orden, vista y el
	 * interruptor de "Más filtros".
	 *
	 * <p>
	 * Antes eran tres bandas apiladas que sumaban 186px — tanto como el titular
	 * entero. Ahora es una sola: el buscador se ha ido al hero y las comodidades
	 * están recogidas.
	 */
	private JPanel bandaDeClasificado() {

		JPanel banda = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.XS, 0, Space.XS, 0),
				MARGEN_LATERAL + "[grow,fill]" + MARGEN_LATERAL, "[]" + Space.XS + "[]"));
		banda.setOpaque(false);

		banda.add(filaDeTipo(), "growx");
		banda.add(filaDeAjustes(), "growx");

		return banda;
	}

	/** Primera fila: qué tipo de alojamiento, y a la derecha el recuento y la vista. */
	private JPanel filaDeTipo() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.SM + "[]push[]" + Space.LG + "[]", "[]"));
		fila.setOpaque(false);

		etiquetaTipo = Labels.caps(Textos.t("catalogo.filtro.tipo"));
		fila.add(etiquetaTipo, "aligny center");
		fila.add(chipsDeTipo(), "aligny center");

		recuento = Labels.muted("");
		fila.add(recuento, "aligny center");

		vista = new Segmented(0, indice -> notificar(), Textos.t("catalogo.vista.lista"),
				Textos.t("catalogo.vista.cuadricula"));
		fila.add(vista, "aligny center");

		return fila;
	}

	/** Segunda fila: cuántas habitaciones, más filtros, y a la derecha el orden. */
	private JPanel filaDeAjustes() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0),
				"[]" + Space.XS + "[]" + Space.MD + "[]push[]" + Space.SM + "[]", "[]"));
		fila.setOpaque(false);

		etiquetaMinHab = Labels.caps(Textos.t("catalogo.filtro.minHab"));
		fila.add(etiquetaMinHab, "aligny center");

		// Un contador numérico y no un campo de texto libre: el valor solo puede ser un
		// entero positivo pequeño, y un control que impide escribir algo inválido
		// ahorra tener que explicar después por qué no vale.
		minimoHabitaciones = new Contador(1, 1, 20, 1, this::notificar);
		fila.add(minimoHabitaciones, "h " + Typography.altoDeControlCompacto() + "!, aligny center");

		masFiltros = new Chip(Textos.t("catalogo.filtro.masFiltros"));
		masFiltros.addActionListener(e -> alternarComodidades());
		fila.add(masFiltros, "aligny center");

		etiquetaOrdenar = Labels.caps(Textos.t("catalogo.filtro.ordenar"));
		fila.add(etiquetaOrdenar, "aligny center");

		orden = new OptionLinks(ORDEN_PUNTUACION, indice -> notificar(), ordenesTraducidos());
		fila.add(orden, "aligny center");

		return fila;
	}

	/**
	 * Comodidades, precio y ciudad — ocultos hasta que se piden.
	 *
	 * <p>
	 * <b>Recogerlos en lugar de quitarlos.</b> Empezó siendo solo las siete
	 * comodidades, que ocupaban una banda permanente para un filtro que no se usa
	 * en cada visita; precio y ciudad (Fase 7.10) se sumaron al mismo cajón por
	 * el mismo motivo, no porque sean comodidades. Detrás de "Más filtros" siguen
	 * estando a un clic, y el propio chip lleva el número de filtros activos
	 * cuando hay alguno, así que nunca quedan olvidados filtrando en silencio.
	 */
	private JPanel bandaAvanzada() {

		JPanel banda = new JPanel(new MigLayout("wrap 1, hidemode 3, " + Space.insets(Space.XXS, 0, Space.SM, 0),
				MARGEN_LATERAL + "[grow,fill]" + MARGEN_LATERAL, "[]" + Space.XS + "[]"));
		banda.setOpaque(false);
		banda.setVisible(false);

		banda.add(filaDePrecioYCiudad(), "growx");
		banda.add(filaDeHuespedesYFechas(), "growx");

		JPanel filaComodidades = new JPanel(
				new MigLayout(Space.insets(0), "[]" + Space.SM + "[grow,fill]", "[]"));
		filaComodidades.setOpaque(false);
		etiquetaComodidades = Labels.caps(Textos.t("catalogo.filtro.comodidades"));
		filaComodidades.add(etiquetaComodidades, "aligny top, gaptop 6");
		filaComodidades.add(chipsDeComodidad(), "growx");
		banda.add(filaComodidades, "growx");

		avanzadoVisible = banda;

		return banda;
	}

	/** Precio mínimo y máximo por noche, y la ciudad — los dos filtros de la Fase 7.10. */
	private JPanel filaDePrecioYCiudad() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0),
				"[]" + Space.XS + "[]" + Space.XXS + "[]" + Space.XXS + "[]" + Space.MD + "[]" + Space.XS + "[grow,fill]",
				"[]"));
		fila.setOpaque(false);

		etiquetaPrecio = Labels.caps(Textos.t("catalogo.filtro.precio"));
		fila.add(etiquetaPrecio, "aligny center");

		// Rango 0-2000 con el valor de partida en cada extremo: así el filtro no
		// esconde nada hasta que el usuario mueve un control. Un valor por defecto a
		// mitad de camino habría ocultado en silencio cualquier alojamiento que se
		// añadiera por encima de ese número, exactamente el fallo de datos que este
		// proyecto ya ha aprendido a evitar (ver CLAUDE.md §3).
		precioMinimo = new Contador(0, 0, 2000, 25, () -> {
			refrescarEtiquetaDeMasFiltros();
			notificar();
		}, true);
		fila.add(precioMinimo, "h " + ALTO_CONTROL + "!, aligny center");

		fila.add(Labels.muted("–"), "aligny center");

		precioMaximo = new Contador(2000, 0, 2000, 25, () -> {
			refrescarEtiquetaDeMasFiltros();
			notificar();
		}, true);
		fila.add(precioMaximo, "h " + ALTO_CONTROL + "!, aligny center");

		etiquetaCiudad = Labels.caps(Textos.t("catalogo.filtro.ciudad"));
		fila.add(etiquetaCiudad, "aligny center");

		ciudad = new JComboBox<>(new String[] { TODAS_LAS_CIUDADES });

		// Redondeado y fino, como el resto de "Más filtros" (Fase 9): un
		// desplegable de Swing crudo trae el marco grueso y recto de siempre, que
		// desentonaba junto a los contadores ya afinados. FlatLaf expone el radio
		// como propiedad de cliente en vez de por subclase, así que no hace falta
		// pintar el combo a mano para conseguirlo.
		ciudad.putClientProperty("JComponent.arc", RADIO_PILDORA);
		ciudad.setRenderer(new DefaultListCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
					boolean isSelected, boolean cellHasFocus) {

				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setText(TODAS_LAS_CIUDADES.equals(value) ? Textos.t("catalogo.filtro.todasCiudades") : (String) value);
				return this;
			}
		});
		ciudad.addActionListener(e -> {
			if (!actualizandoCiudades) {
				refrescarEtiquetaDeMasFiltros();
				notificar();
			}
		});
		fila.add(ciudad, "height " + ALTO_CONTROL + "!, aligny center, growx");

		return fila;
	}

	/**
	 * Huéspedes mínimos y, si llega del buscador (Fase 9), el rango de fechas
	 * aplicado.
	 *
	 * <p>
	 * <b>Las fechas no se eligen aquí.</b> Este panel no lleva un calendario
	 * propio —añadir uno duplicaría {@code CalendarioRango} solo para poder
	 * cambiar de opinión sobre una fecha ya elegida en el buscador—, así que el
	 * rango solo se aplica desde fuera ({@link #setDisponibilidad}) y aquí solo
	 * se enseña y se puede quitar. Es la misma idea que un filtro de precio con
	 * un botón de "restablecer": no todo control necesita poder construir su
	 * propio valor, algunos solo necesitan poder soltarlo.
	 */
	private JPanel filaDeHuespedesYFechas() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0),
				"[]" + Space.XS + "[]" + Space.MD + "[]" + Space.XS + "[]" + Space.XXS + "[]", "[]"));
		fila.setOpaque(false);

		etiquetaHuespedes = Labels.caps(Textos.t("catalogo.filtro.huespedes"));
		fila.add(etiquetaHuespedes, "aligny center");

		huespedesMinimos = new Contador(1, 1, 20, 1, () -> {
			refrescarEtiquetaDeMasFiltros();
			notificar();
		}, true);
		fila.add(huespedesMinimos, "h " + ALTO_CONTROL + "!, aligny center");

		etiquetaFechas = Labels.caps(Textos.t("catalogo.filtro.fechas"));
		fila.add(etiquetaFechas, "aligny center");

		resumenFechas = Labels.muted(Textos.t("catalogo.filtro.fechas.ninguna"));
		fila.add(resumenFechas, "aligny center");

		quitarFechas = Buttons.link(Textos.t("catalogo.filtro.fechas.quitar"), e -> limpiarDisponibilidad());
		quitarFechas.setVisible(false);
		fila.add(quitarFechas, "aligny center");

		return fila;
	}

	/**
	 * Aplica el rango de fechas del buscador de destino y el conjunto de
	 * alojamientos que ya no están libres en él.
	 *
	 * <p>
	 * <b>Quien calcula {@code noDisponibles} es el servicio, no este filtro.</b>
	 * Este panel no conoce ningún {@code ReservationService} —trabajaría sobre el
	 * catálogo en memoria, y la disponibilidad no vive en {@code Housing}—, así
	 * que recibe ya resuelta la pregunta "¿quién está libre?" y se limita a
	 * excluir a quien no lo está.
	 */
	public void setDisponibilidad(LocalDate entrada, LocalDate salida, Set<Long> noDisponibles) {

		this.fechaEntrada = entrada;
		this.fechaSalida = salida;
		this.idsNoDisponibles = noDisponibles;

		resumenFechas.setText(Formato.rangoDeFechas(entrada, salida));
		quitarFechas.setVisible(true);
	}

	private void limpiarDisponibilidad() {

		fechaEntrada = null;
		fechaSalida = null;
		idsNoDisponibles = Collections.emptySet();

		resumenFechas.setText(Textos.t("catalogo.filtro.fechas.ninguna"));
		quitarFechas.setVisible(false);

		notificar();
	}

	private boolean fechasActivas() {
		return fechaEntrada != null && fechaSalida != null;
	}

	private boolean huespedesActivo() {
		return huespedesMinimos.getValor() > 1;
	}

	/**
	 * Marca una comodidad como ya elegida, sin que el usuario haya tocado el
	 * chip (Fase 9): la usa el buscador de destino para preseleccionar
	 * "Mascotas" cuando se pide desde allí. Reutiliza el mismo chip que ya
	 * filtra el catálogo en lugar de sumar un segundo campo booleano — pedir
	 * mascota en el buscador y marcar la comodidad en el catálogo son la misma
	 * pregunta.
	 */
	public void preseleccionarComodidad(Amenity amenity) {

		Chip chip = chipsPorComodidad.get(amenity);

		if (chip != null && !chip.isSelected()) {
			chip.setSelected(true);
			comodidades.add(amenity);
			refrescarEtiquetaDeMasFiltros();
		}
	}

	/**
	 * Fija el mínimo de huéspedes sin que el usuario haya tocado el contador
	 * (Fase 9): lo usa el buscador de destino para trasladar "adultos + niños"
	 * al catálogo.
	 */
	public void setHuespedesMinimos(int minimo) {
		huespedesMinimos.setValor(minimo);
		refrescarEtiquetaDeMasFiltros();
	}

	/**
	 * Rellena el desplegable de ciudad con las que de verdad hay en el catálogo.
	 *
	 * <p>
	 * Se llama desde fuera, igual que {@link #setRecuentosPorComodidad(List)}, en
	 * cuanto se conoce el catálogo: el desplegable no puede rellenarse en el
	 * constructor porque en ese momento todavía no hay ningún alojamiento cargado.
	 * Conserva la selección si la ciudad elegida sigue existiendo; si no —el
	 * catálogo ha cambiado y esa ciudad ya no aparece—, vuelve a "todas" en vez de
	 * dejar un filtro apuntando a algo que ya no está.
	 */
	public void setUbicaciones(List<Housing> catalogo) {

		Object seleccionActual = ciudad.getSelectedItem();

		TreeSet<String> ordenadas = new TreeSet<>();
		for (Housing housing : catalogo) {
			if (housing.getLocation() != null && !housing.getLocation().trim().isEmpty()) {
				ordenadas.add(housing.getLocation());
			}
		}

		DefaultComboBoxModel<String> modelo = new DefaultComboBoxModel<>();
		modelo.addElement(TODAS_LAS_CIUDADES);
		ordenadas.forEach(modelo::addElement);

		// Reconstruir el modelo dispara por sí solo un cambio de selección (al vacío
		// nuevo modelo, Swing selecciona su primer elemento). Sin esta guarda, ese
		// evento llamaría a notificar() y, si quien llama a este método lo hace desde
		// dentro de aplicarFiltros(), se reentraría en el mismo filtrado a medio
		// terminar. La guarda deja pasar solo los cambios que hace la persona que usa
		// el desplegable, no los que provoca este propio método.
		actualizandoCiudades = true;
		try {
			ciudad.setModel(modelo);
			ciudad.setSelectedItem(ordenadas.contains(seleccionActual) ? seleccionActual : TODAS_LAS_CIUDADES);
		} finally {
			actualizandoCiudades = false;
		}
	}

	private void alternarComodidades() {

		avanzadoVisible.setVisible(masFiltros.isSelected());
		revalidate();
		repaint();
	}

	/**
	 * Actualiza el chip de "Más filtros" con cuántas comodidades hay marcadas.
	 *
	 * <p>
	 * Es lo que evita el peor estado de un filtro plegable: dejarlo cerrado con algo
	 * marcado dentro y no entender por qué faltan resultados.
	 */
	private void refrescarEtiquetaDeMasFiltros() {

		int activos = comodidades.size() + (precioActivo() ? 1 : 0) + (ciudadActiva() ? 1 : 0)
				+ (huespedesActivo() ? 1 : 0) + (fechasActivas() ? 1 : 0);

		String base = Textos.t("catalogo.filtro.masFiltros");
		masFiltros.setText(activos == 0 ? base : base + " (" + activos + ")");
	}

	private boolean precioActivo() {

		int minimo = precioMinimo.getValor();
		int maximo = precioMaximo.getValor();

		return minimo > 0 || maximo < 2000;
	}

	private boolean ciudadActiva() {
		return !TODAS_LAS_CIUDADES.equals(ciudad.getSelectedItem());
	}

	/** Los tres rótulos de orden, ya traducidos, en el orden de {@link #ORDEN_PUNTUACION} etc. */
	private String[] ordenesTraducidos() {
		return new String[] { Textos.t("catalogo.orden.mejorValorados"), Textos.t("catalogo.orden.precioMenor"),
				Textos.t("catalogo.orden.precioMayor") };
	}

	/**
	 * Los chips de tipo, en una fila que se dobla si no caben.
	 *
	 * <p>
	 * <b>Era una fila normal, y ahí estaba media avería.</b> Cinco chips en una fila
	 * rígida exigen la suma de sus cinco anchos, y esa suma crece con el escalado del
	 * sistema. En un portátil al 150 % —donde la aplicación recibe 1280 puntos
	 * lógicos, no 1920— la exigencia no se podía cumplir y MigLayout hacía lo que
	 * hace en ese caso: desbordar, dejando "Cabaña" dibujado fuera de la ventana.
	 * Con {@link FilaFluida} el mínimo pasa a ser el chip más ancho y los demás bajan
	 * a una segunda línea.
	 */
	private JPanel chipsDeTipo() {

		FilaFluida fila = new FilaFluida(Space.XS, Space.XS);

		// ButtonGroup impone la exclusividad: al marcar uno, desmarca el anterior. Es
		// el comportamiento estándar de Swing para opciones excluyentes y no hay motivo
		// para reimplementarlo con banderas a mano.
		ButtonGroup grupo = new ButtonGroup();

		for (String valor : TIPOS) {

			Chip chip = new Chip(Textos.tipoDeAlojamiento(valor), valor.equals(tipo));

			// El icono solo lo llevan los cuatro tipos concretos, no el "Todos": ese no
			// es un tipo de alojamiento, es la ausencia de filtro, y darle un dibujo lo
			// pondría al mismo nivel que los demás. Un icono debe representar algo; la
			// opción de no filtrar no representa nada que se pueda dibujar.
			if (!TODOS.equals(valor)) {
				chip.setIcon(new IconoDeTipo(valor));
				chip.setIconTextGap(Space.XS);
			}

			chip.addActionListener(e -> {
				tipo = valor;
				notificar();
			});

			grupo.add(chip);
			fila.add(chip);
			chipsPorTipo.put(valor, chip);
		}

		return fila;
	}

	/** Las siete comodidades, también en fila fluida. Ver {@link #chipsDeTipo()}. */
	private JPanel chipsDeComodidad() {

		FilaFluida fila = new FilaFluida(Space.XS, Space.XS);

		for (Amenity amenity : Amenity.values()) {

			Chip chip = new Chip(Textos.etiquetaDe(amenity));

			chip.addActionListener(e -> {

				if (chip.isSelected()) {
					comodidades.add(amenity);
				} else {
					comodidades.remove(amenity);
				}

				refrescarEtiquetaDeMasFiltros();
				notificar();
			});

			chipsPorComodidad.put(amenity, chip);
			fila.add(chip);
		}

		return fila;
	}

	/**
	 * Escribe en cada chip cuántos alojamientos tiene esa comodidad.
	 *
	 * <p>
	 * <b>Es la única idea que este catálogo le copia a Booking, y merece la pena.</b>
	 * Un filtro que dice de antemano "Piscina (3)" te ahorra pulsarlo para descubrir
	 * que no hay nada, y evita el callejón de ir marcando hasta quedarse en cero
	 * resultados sin saber cuál sobra.
	 *
	 * <p>
	 * El recuento se calcula sobre el <b>catálogo completo</b> y no sobre el
	 * resultado filtrado, y la diferencia importa: si se calculara sobre el
	 * resultado, marcar un chip pondría a cero todos los demás y dejarían de servir
	 * para nada. Lo que el número responde es "cuántos hay con esto", no "cuántos
	 * quedarían".
	 */
	public void setRecuentosPorComodidad(List<Housing> catalogo) {

		chipsPorComodidad.forEach((amenity, chip) -> {

			long cuantos = catalogo.stream().filter(amenity::presenteEn).count();

			chip.setText(Textos.etiquetaDe(amenity) + " (" + cuantos + ")");

			// Un chip que no encontraría nada se deja visible pero apagado: enseñar que
			// existe y que hoy está vacío informa más que esconderlo.
			chip.setEnabled(cuantos > 0 || chip.isSelected());
		});
	}

	private void notificar() {
		alCambiar.run();
	}

	/**
	 * Vuelve a fijar todos los textos fijos en el idioma activo (Fase 7.6).
	 *
	 * <p>
	 * Los recuentos (chip de "Más filtros", comodidades, resultados) no hace
	 * falta tocarlos aquí: los recalcula {@code aplicarFiltros()}, que
	 * {@code ShowHousingsFrame} ya llama en cada {@code setVisible(true)}.
	 */
	public void actualizarTextos() {

		buscador.setMarcador(Textos.t("catalogo.buscador.placeholder"));
		etiquetaTipo.setText(Textos.t("catalogo.filtro.tipo"));
		etiquetaMinHab.setText(Textos.t("catalogo.filtro.minHab"));
		etiquetaOrdenar.setText(Textos.t("catalogo.filtro.ordenar"));
		etiquetaComodidades.setText(Textos.t("catalogo.filtro.comodidades"));
		etiquetaPrecio.setText(Textos.t("catalogo.filtro.precio"));
		etiquetaCiudad.setText(Textos.t("catalogo.filtro.ciudad"));
		ciudad.repaint();
		etiquetaHuespedes.setText(Textos.t("catalogo.filtro.huespedes"));
		etiquetaFechas.setText(Textos.t("catalogo.filtro.fechas"));
		resumenFechas.setText(fechasActivas() ? Formato.rangoDeFechas(fechaEntrada, fechaSalida)
				: Textos.t("catalogo.filtro.fechas.ninguna"));
		quitarFechas.setText(Textos.t("catalogo.filtro.fechas.quitar"));

		vista.actualizarTextos(Textos.t("catalogo.vista.lista"), Textos.t("catalogo.vista.cuadricula"));
		orden.actualizarTextos(ordenesTraducidos());

		chipsPorTipo.forEach((valor, chip) -> chip.setText(Textos.tipoDeAlojamiento(valor)));
		refrescarEtiquetaDeMasFiltros();
	}

	// ------------------------------------------------------------------
	// Filtrado
	// ------------------------------------------------------------------

	/** Si el usuario ha pedido la vista de cuadrícula. */
	public boolean esCuadricula() {
		return vista.getActivo() == VISTA_CUADRICULA;
	}

	/**
	 * Fija la vista por código, sin que el usuario haya tocado el conmutador.
	 *
	 * <p>
	 * Para restaurar la preferencia guardada en Ajustes (Fase 7.11) al abrir el
	 * catálogo por primera vez en la sesión. {@link Segmented#setActivo} ya
	 * avisa como si fuera un clic real, así que quien llame a esto no necesita
	 * disparar el filtrado aparte.
	 */
	public void setVista(int indice) {
		vista.setActivo(indice);
	}

	/** Escribe el recuento de resultados. */
	public void setResultado(int cuantos) {
		recuento.setText(
				Formato.plural(cuantos, Textos.t("palabra.estancia.singular"), Textos.t("palabra.estancia.plural")));
	}

	/**
	 * Filtra y ordena el catálogo según lo que haya seleccionado el usuario.
	 *
	 * <p>
	 * <b>Se filtra en memoria, y es la decisión correcta aquí.</b> El servicio tiene
	 * {@code filterHousingsByType} y {@code filterHousingsByMinimumRooms}, y el plan
	 * era conectarlos. Al llegar a este punto hay dos motivos para no hacerlo:
	 *
	 * <ul>
	 * <li>Cada uno consulta por <em>su</em> criterio y devuelve <em>su</em> lista, así
	 * que <b>no se pueden combinar</b>, que es exactamente lo que pide el diseño
	 * ("todos los filtros funcionales y combinables entre sí"). Cruzar sus resultados
	 * a mano sería más código y más frágil.</li>
	 * <li>El buscador filtra en vivo. Ir a la base de datos en cada tecla pulsada
	 * para reordenar seis filas que ya están cargadas en memoria es trabajo tirado, y
	 * además introduce una latencia justo donde más se nota.</li>
	 * </ul>
	 *
	 * <p>
	 * Los dos métodos de servicio siguen existiendo, probados y disponibles: no se
	 * borra nada. Y cuando el catálogo crezca lo bastante como para que cargarlo
	 * entero deje de ser razonable, la solución no será ninguna de las dos, sino un
	 * único método de servicio que reciba todos los criterios y los traduzca a una
	 * sola consulta.
	 */
	public List<Housing> aplicar(List<Housing> origen) {

		List<Housing> resultado = new ArrayList<>();
		String texto = buscador.getTexto().toLowerCase(Locale.ROOT);
		int minimoHab = minimoHabitaciones.getValor();
		BigDecimal precioMin = BigDecimal.valueOf(precioMinimo.getValor());
		BigDecimal precioMax = BigDecimal.valueOf(precioMaximo.getValor());
		String ciudadElegida = (String) ciudad.getSelectedItem();

		for (Housing housing : origen) {

			if (!TODOS.equals(tipo) && !tipo.equals(housing.getType())) {
				continue;
			}

			if (housing.getNumberOfRooms() < minimoHab) {
				continue;
			}

			if (housing.getPricePerNight().compareTo(precioMin) < 0
					|| housing.getPricePerNight().compareTo(precioMax) > 0) {
				continue;
			}

			if (!TODAS_LAS_CIUDADES.equals(ciudadElegida) && !ciudadElegida.equals(housing.getLocation())) {
				continue;
			}

			if (housing.getCapacity() < huespedesMinimos.getValor()) {
				continue;
			}

			if (idsNoDisponibles.contains(housing.getId())) {
				continue;
			}

			if (!tieneTodasLasComodidades(housing)) {
				continue;
			}

			if (!coincideConLaBusqueda(housing, texto)) {
				continue;
			}

			resultado.add(housing);
		}

		resultado.sort(comparador());

		return resultado;
	}

	private boolean tieneTodasLasComodidades(Housing housing) {

		for (Amenity amenity : comodidades) {
			if (!amenity.presenteEn(housing)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Busca en nombre, ubicación y tipo, sin distinguir mayúsculas.
	 *
	 * <p>
	 * Se busca por "contiene" y no por "empieza por": quien escribe "granada" espera
	 * encontrar "Sierra Nevada, Granada". Un buscador que solo mira el principio de
	 * la cadena obliga a saber cómo empieza el dato, que es precisamente lo que uno
	 * no sabe cuando busca.
	 */
	private boolean coincideConLaBusqueda(Housing housing, String texto) {

		if (texto.isEmpty()) {
			return true;
		}

		return contiene(housing.getName(), texto) || contiene(housing.getLocation(), texto)
				|| contiene(housing.getType(), texto);
	}

	private boolean contiene(String campo, String texto) {
		return campo != null && campo.toLowerCase(Locale.ROOT).contains(texto);
	}

	private Comparator<Housing> comparador() {

		if (orden.getActivo() == ORDEN_PUNTUACION) {

			// Los alojamientos sin nota van al final y no al principio. Un null tratado
			// como cero los pondría los últimos en "mejor valorados", que por casualidad
			// es correcto, pero los pondría los primeros en cuanto se invirtiera el
			// orden. Mejor decir explícitamente dónde va lo que no tiene dato.
			return Comparator.comparing(Housing::getScore,
					Comparator.nullsLast(Comparator.reverseOrder()));
		}

		Comparator<Housing> porPrecio = Comparator.comparing(Housing::getPricePerNight);

		return orden.getActivo() == ORDEN_PRECIO_ASC ? porPrecio : porPrecio.reversed();
	}
}
