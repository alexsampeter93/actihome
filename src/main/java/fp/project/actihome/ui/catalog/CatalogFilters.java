package fp.project.actihome.ui.catalog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Amenity;
import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.ui.components.Chip;
import fp.project.actihome.ui.components.FilaFluida;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.OptionLinks;
import fp.project.actihome.ui.components.SearchField;
import fp.project.actihome.ui.components.Segmented;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Space;

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

	/** Las categorías de alojamiento, en el orden del diseño. */
	private static final String[] TIPOS = { TODOS, "Casa", "Apartamento", "Villa", "Cabaña" };

	private static final String[] ORDENES = { "Mejor valorados", "Precio · menor", "Precio · mayor" };

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

	private final transient Runnable alCambiar;

	private SearchField buscador;
	private JSpinner minimoHabitaciones;
	private JLabel recuento;
	private Segmented vista;
	private OptionLinks orden;

	private Chip masFiltros;
	private JPanel comodidadesVisibles;
	private final transient java.util.Map<Amenity, Chip> chipsPorComodidad = new java.util.EnumMap<>(Amenity.class);

	private String tipo = TODOS;
	private final EnumSet<Amenity> comodidades = EnumSet.noneOf(Amenity.class);

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
		buscador = new SearchField("Buscar por nombre, ubicación o tipo", this::notificar);

		add(Hairline.horizontal(), "growx, h 1!");
		add(bandaDeClasificado(), "growx");
		add(bandaDeComodidades(), "growx");
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

		fila.add(Labels.caps("Tipo"), "aligny center");
		fila.add(chipsDeTipo(), "aligny center");

		recuento = Labels.muted("");
		fila.add(recuento, "aligny center");

		vista = new Segmented(0, indice -> notificar(), "Lista", "Cuadrícula");
		fila.add(vista, "aligny center");

		return fila;
	}

	/** Segunda fila: cuántas habitaciones, más filtros, y a la derecha el orden. */
	private JPanel filaDeAjustes() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0),
				"[]" + Space.XS + "[]" + Space.MD + "[]push[]" + Space.SM + "[]", "[]"));
		fila.setOpaque(false);

		fila.add(Labels.caps("Mín. hab."), "aligny center");

		// Un contador numérico y no un campo de texto libre: el valor solo puede ser un
		// entero positivo pequeño, y un control que impide escribir algo inválido
		// ahorra tener que explicar después por qué no vale.
		minimoHabitaciones = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
		minimoHabitaciones.addChangeListener(e -> notificar());
		fila.add(minimoHabitaciones, "w 62!, h 32!, aligny center");

		masFiltros = new Chip("Más filtros");
		masFiltros.addActionListener(e -> alternarComodidades());
		fila.add(masFiltros, "aligny center");

		fila.add(Labels.caps("Ordenar"), "aligny center");

		orden = new OptionLinks(ORDEN_PUNTUACION, indice -> notificar(), ORDENES);
		fila.add(orden, "aligny center");

		return fila;
	}

	/**
	 * Las comodidades, ocultas hasta que se piden.
	 *
	 * <p>
	 * <b>Recogerlas en lugar de quitarlas.</b> Son siete chips que ocupaban una
	 * banda permanente para un filtro que no se usa en cada visita. Detrás de "Más
	 * filtros" siguen estando a un clic, y el propio chip lleva el número de filtros
	 * activos cuando hay alguno, así que nunca quedan olvidados y filtrando en
	 * silencio.
	 */
	private JPanel bandaDeComodidades() {

		JPanel banda = new JPanel(new MigLayout("hidemode 3, " + Space.insets(Space.XXS, 0, Space.SM, 0),
				MARGEN_LATERAL + "[]" + Space.SM + "[grow,fill]" + MARGEN_LATERAL, "[]"));
		banda.setOpaque(false);
		banda.setVisible(false);

		banda.add(Labels.caps("Comodidades"), "aligny top, gaptop 6");
		banda.add(chipsDeComodidad(), "growx");

		comodidadesVisibles = banda;

		return banda;
	}

	private void alternarComodidades() {

		comodidadesVisibles.setVisible(masFiltros.isSelected());
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

		masFiltros.setText(comodidades.isEmpty() ? "Más filtros" : "Más filtros (" + comodidades.size() + ")");
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

			Chip chip = new Chip(valor, valor.equals(tipo));

			chip.addActionListener(e -> {
				tipo = valor;
				notificar();
			});

			grupo.add(chip);
			fila.add(chip);
		}

		return fila;
	}

	/** Las siete comodidades, también en fila fluida. Ver {@link #chipsDeTipo()}. */
	private JPanel chipsDeComodidad() {

		FilaFluida fila = new FilaFluida(Space.XS, Space.XS);

		for (Amenity amenity : Amenity.values()) {

			Chip chip = new Chip(amenity.etiqueta());

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

			chip.setText(amenity.etiqueta() + " (" + cuantos + ")");

			// Un chip que no encontraría nada se deja visible pero apagado: enseñar que
			// existe y que hoy está vacío informa más que esconderlo.
			chip.setEnabled(cuantos > 0 || chip.isSelected());
		});
	}

	private void notificar() {
		alCambiar.run();
	}

	// ------------------------------------------------------------------
	// Filtrado
	// ------------------------------------------------------------------

	/** Si el usuario ha pedido la vista de cuadrícula. */
	public boolean esCuadricula() {
		return vista.getActivo() == VISTA_CUADRICULA;
	}

	/** Escribe el recuento de resultados. */
	public void setResultado(int cuantos) {
		recuento.setText(Formato.plural(cuantos, "estancia", "estancias"));
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
		int minimo = (Integer) minimoHabitaciones.getValue();

		for (Housing housing : origen) {

			if (!TODOS.equals(tipo) && !tipo.equals(housing.getType())) {
				continue;
			}

			if (housing.getNumberOfRooms() < minimo) {
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
