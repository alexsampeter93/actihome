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

	/** Índice de la vista de cuadrícula en el conmutador. */
	public static final int VISTA_CUADRICULA = 1;

	private final transient Runnable alCambiar;

	private SearchField buscador;
	private JSpinner minimoHabitaciones;
	private JLabel recuento;
	private Segmented vista;
	private OptionLinks orden;

	private String tipo = TODOS;
	private final EnumSet<Amenity> comodidades = EnumSet.noneOf(Amenity.class);

	public CatalogFilters(Runnable alCambiar) {

		super(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]0[]0[]0[]0[]0[]"));

		this.alCambiar = alCambiar;
		setOpaque(false);

		add(Hairline.horizontal(), "growx, h 1!");
		add(bandaDeBusqueda(), "growx");
		add(Hairline.horizontal(), "growx, h 1!");
		add(bandaDeClasificado(), "growx");
		add(bandaDeComodidades(), "growx");
	}

	// ------------------------------------------------------------------
	// Construcción
	// ------------------------------------------------------------------

	private JPanel bandaDeBusqueda() {

		JPanel banda = new JPanel(new MigLayout(Space.insets(Space.SM, Space.HUGE, Space.SM, Space.HUGE),
				"[]" + Space.XL + "[]" + Space.XS + "[]push[]" + Space.XL + "[]", "[]"));
		banda.setOpaque(false);

		buscador = new SearchField("Buscar por nombre, ubicación o tipo", this::notificar);
		banda.add(buscador, "w 320!, h 38!");

		banda.add(Labels.caps("Mín. hab."), "aligny center");

		// Un contador numérico y no un campo de texto libre: el valor solo puede ser un
		// entero positivo pequeño, y un control que impide escribir algo inválido
		// ahorra tener que explicar después por qué no vale.
		minimoHabitaciones = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
		minimoHabitaciones.addChangeListener(e -> notificar());
		banda.add(minimoHabitaciones, "w 68!, h 34!, aligny center");

		recuento = Labels.muted("");
		banda.add(recuento, "aligny center");

		vista = new Segmented(0, indice -> notificar(), "Lista", "Cuadrícula");
		banda.add(vista, "aligny center");

		return banda;
	}

	private JPanel bandaDeClasificado() {

		JPanel banda = new JPanel(new MigLayout(Space.insets(Space.SM, Space.HUGE, Space.XXS, Space.HUGE),
				"[]" + Space.SM + "[]push[]" + Space.SM + "[]", "[]"));
		banda.setOpaque(false);

		banda.add(Labels.caps("Tipo"), "aligny center");
		banda.add(chipsDeTipo(), "aligny center");

		banda.add(Labels.caps("Ordenar"), "aligny center");

		orden = new OptionLinks(ORDEN_PUNTUACION, indice -> notificar(), ORDENES);
		banda.add(orden, "aligny center");

		return banda;
	}

	private JPanel bandaDeComodidades() {

		JPanel banda = new JPanel(new MigLayout(Space.insets(Space.XXS, Space.HUGE, Space.SM, Space.HUGE),
				"[]" + Space.SM + "[]push", "[]"));
		banda.setOpaque(false);

		banda.add(Labels.caps("Filtrar"), "aligny center");
		banda.add(chipsDeComodidad(), "aligny center");

		return banda;
	}

	private JPanel chipsDeTipo() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "", "[]"));
		fila.setOpaque(false);

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
			fila.add(chip, "gapright " + Space.XS);
		}

		return fila;
	}

	private JPanel chipsDeComodidad() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "", "[]"));
		fila.setOpaque(false);

		for (Amenity amenity : Amenity.values()) {

			Chip chip = new Chip(amenity.etiqueta());

			chip.addActionListener(e -> {

				if (chip.isSelected()) {
					comodidades.add(amenity);
				} else {
					comodidades.remove(amenity);
				}

				notificar();
			});

			fila.add(chip, "gapright " + Space.XS);
		}

		return fila;
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
