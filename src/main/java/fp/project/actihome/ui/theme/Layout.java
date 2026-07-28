package fp.project.actihome.ui.theme;

/**
 * Reglas de comportamiento del layout cuando la ventana crece.
 *
 * <p>
 * <b>El problema.</b> Un layout que se limita a repartir el espacio disponible
 * funciona al tamaño para el que se diseñó y se deshace en cuanto la ventana se
 * agranda: los campos de un formulario llegan a medir mil píxeles, la
 * tipografía se queda pequeña dentro de bloques enormes y aparecen huecos que
 * nadie ha decidido. Se vio al maximizar el login.
 *
 * <p>
 * <b>La regla.</b> Lo que crece es el <em>aire</em>, no el contenido. Cada
 * bloque tiene un ancho máximo y se queda ahí; el espacio que sobra se reparte
 * alrededor. Es la misma idea que usa cualquier revista: la mancha de texto
 * tiene una medida cómoda de lectura y los márgenes absorben el resto.
 *
 * <p>
 * <b>La excepción.</b> Las listas y las rejillas sí crecen, porque en ellas el
 * espacio de más se traduce en más contenido visible, que es exactamente lo que
 * el usuario quiere al agrandar la ventana. Una lista de alojamientos con
 * márgenes gigantes y tres filas visibles sería absurda.
 *
 * <p>
 * <b>Y la tipografía de display sube un escalón</b> en ventanas grandes. Un
 * titular de 38px que se ve rotundo en 980px de ancho se queda tímido en 2500:
 * la proporción entre el texto y su contenedor es parte del diseño, no un
 * accidente del tamaño de la letra.
 */
public final class Layout {

	/**
	 * Ancho máximo de un formulario. Por encima de esta medida un campo de texto
	 * deja de ser cómodo: el ojo pierde la línea al recorrerlo y una caja larguísima
	 * para escribir ocho letras se lee como un error de maquetación.
	 */
	public static final int FORMULARIO = 440;

	/** Ancho máximo de un bloque de texto editorial (claim, descripción, intro). */
	public static final int TEXTO = 560;

	/** Ancho máximo de una pantalla de contenido denso, como el registro. */
	public static final int CONTENIDO = 940;

	/** A partir de este ancho de ventana se considera "pantalla grande". */
	private static final int UMBRAL_GRANDE = 1500;

	/** Y a partir de este, "muy grande" (monitores de 2K y 4K maximizados). */
	private static final int UMBRAL_ENORME = 2200;

	private Layout() {
	}

	/**
	 * Escala un tamaño de fuente de display según el ancho de la ventana.
	 *
	 * <p>
	 * Solo se aplica a titulares y cifras grandes. El cuerpo de texto <b>no</b> se
	 * escala: su tamaño depende de la distancia de lectura, que no cambia porque la
	 * ventana sea más ancha. Agrandar el cuerpo junto con la ventana es un error
	 * frecuente y hace que el texto parezca gritado.
	 *
	 * @param base   tamaño de la escala tipográfica ({@link Typography#HERO}, etc.)
	 * @param ancho  ancho actual de la ventana en píxeles
	 */
	public static float display(float base, int ancho) {

		if (ancho >= UMBRAL_ENORME) {
			return base * 1.35f;
		}

		if (ancho >= UMBRAL_GRANDE) {
			return base * 1.18f;
		}

		return base;
	}

	/** Si la ventana da para tratar el contenido con holgura. */
	public static boolean esGrande(int ancho) {
		return ancho >= UMBRAL_GRANDE;
	}

	/**
	 * Restricción de MigLayout para un bloque con ancho máximo.
	 *
	 * <p>
	 * El {@code wmin 0} es imprescindible y no evidente: sin él, MigLayout respeta
	 * el ancho <em>mínimo</em> del componente —que en un texto grande puede ser
	 * enorme— y le deja invadir la columna vecina. Fue lo que hizo que el panel
	 * oscuro del login se comiera parte del formulario.
	 */
	public static String ancho(int maximo) {
		// Sintaxis mínimo:preferido:máximo. Con "wmax" a secas MigLayout lo ignoraba
		// cuando la celda crecía; declarando los tres valores de una vez, el máximo se
		// respeta y el mínimo de 0 permite encoger en ventanas estrechas.
		return "w 0:" + maximo + ":" + maximo;
	}

	/** Bloque con ancho máximo y centrado en el espacio sobrante. */
	public static String anchoCentrado(int maximo) {
		return ancho(maximo) + ", alignx center";
	}
}
