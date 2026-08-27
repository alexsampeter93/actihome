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

	/**
	 * Ancho máximo de una ficha con imagen protagonista (el detalle de un
	 * alojamiento).
	 *
	 * <p>
	 * <b>Por qué esta es más ancha que {@link #CONTENIDO} y no incumple la regla.</b>
	 * El tope de 940 protege la <em>lectura</em>: una línea de texto demasiado larga
	 * hace que el ojo pierda el renglón al volver. Una fotografía no tiene ese
	 * problema — igual que las listas y las rejillas, que el sistema ya deja crecer,
	 * porque ahí el espacio de más significa más contenido visible y no líneas más
	 * difíciles de leer.
	 *
	 * <p>
	 * La ficha es las dos cosas a la vez, así que el tope no puede ser único: crece
	 * hasta aquí, pero <b>solo crece la columna de la foto</b>. La del texto se queda
	 * fija en {@link #COLUMNA_DE_TEXTO}. Con un único tope pasaba lo que el usuario
	 * reportó el 07-08-2026: en un monitor ancho la ficha entera quedaba encerrada en
	 * el centro con 640 puntos vacíos a cada lado, con la foto pequeña y la tarjeta de
	 * reserva encogida.
	 */
	public static final int FICHA = 1440;

	/**
	 * Ancho de la columna de texto y acciones de una ficha.
	 *
	 * <p>
	 * Es fijo a propósito y no un máximo: es la columna que <b>no</b> crece cuando
	 * la ventana lo hace, y por eso puede llevar dentro la tarjeta de reserva sin que
	 * haya que acotarla otra vez. Algo más ancho que un formulario, porque además del
	 * texto sostiene una rejilla de dos datos.
	 */
	public static final int COLUMNA_DE_TEXTO = 480;

	/** A partir de este ancho de ventana se considera "pantalla grande". */
	private static final int UMBRAL_GRANDE = 1500;

	/** Y a partir de este, "muy grande" (monitores de 2K y 4K maximizados). */
	private static final int UMBRAL_ENORME = 2200;

	/**
	 * Por debajo de este ancho la pantalla va justa y hay que soltar lastre.
	 *
	 * <p>
	 * El número no es una preferencia estética, sale de una máquina concreta: un
	 * portátil de 1920×1080 con el escalado de Windows al 150 % le da a la
	 * aplicación <b>1280 puntos lógicos</b> de ancho. Ese es el caso que hay que
	 * cubrir, así que el umbral queda por encima.
	 */
	private static final int UMBRAL_COMPACTO = 1320;

	/**
	 * El tamaño de ventana más pequeño que la aplicación se compromete a servir bien.
	 *
	 * <p>
	 * <b>Hay uno solo, y ese es el cambio.</b> Antes cada pantalla declaraba el suyo
	 * —1180×760 el catálogo, 900×840 el registro, 680×720 la reserva— con números
	 * ajustados mirando capturas en una máquina concreta. Eso tenía dos consecuencias
	 * malas a la vez: la aplicación cambiaba de tamaño mínimo al navegar, y en un
	 * portátil con el escalado de Windows al 150 % —donde la ventana dispone de
	 * 1280×660 puntos lógicos— varios de esos mínimos <b>no cabían en la pantalla</b>,
	 * así que la ventana no se dejaba encoger hasta un tamaño en el que se viera bien.
	 *
	 * <p>
	 * Ahora el suelo es del sistema y se verifica automáticamente:
	 * {@code MedirResponsive} comprueba que las dieciséis pantallas caben aquí sin
	 * que nada se salga. Lo que hay por debajo lo cubre el scroll de rescate.
	 *
	 * <p>
	 * El {@code Navigator} lo acota además al escritorio real, porque un mínimo mayor
	 * que la pantalla deja una ventana que no se puede ni colocar.
	 */
	public static final java.awt.Dimension MINIMO_DE_VENTANA = new java.awt.Dimension(1024, 600);

	/**
	 * El tamano con el que la aplicacion abre su primera ventana, y por tanto el
	 * de toda la sesion mientras el usuario no lo cambie.
	 *
	 * <p>
	 * <b>Existe para que el tamano de la sesion deje de decidirlo la pantalla de
	 * login.</b> Cada pantalla declara su propio {@code setSize} —del 720x680 del
	 * check-in al 1400x900 del catalogo— y el navegador hacia crecer la ventana
	 * hasta el tamano de diseno de cada destino. El resultado es el que reporto
	 * el usuario: <b>la ventana solo podia crecer</b>, a saltos, segun que
	 * pantallas visitaras y en que orden. Medido en un recorrido real: 1024x620
	 * al entrar, 1300x940 al pasar por Buscar, 1400x940 al ir al catalogo. Una
	 * aplicacion de escritorio no cambia de tamano sola al cambiar de seccion.
	 *
	 * <p>
	 * Con un tamano de apertura propio de la <em>aplicacion</em>, el navegador no
	 * necesita adivinar nada: abre aqui y a partir de ahi el tamano es del
	 * usuario. Esta elegido para que quepan las pantallas mas altas, y el
	 * navegador lo acota al escritorio real — en un portatil con el escalado al
	 * 150 % manda el escritorio.
	 */
	public static final java.awt.Dimension TAMANO_DE_SESION = new java.awt.Dimension(1320, 920);

	private Layout() {
	}

	/**
	 * Cuánto sitio hay, en las tres únicas categorías que la interfaz distingue.
	 *
	 * <p>
	 * <b>Tener tres regímenes en vez de un número suelto es lo que hace que esto sea
	 * un sistema.</b> Si cada pantalla compara el ancho con su propia constante, al
	 * final hay quince umbrales distintos y la interfaz cambia de forma a saltos
	 * incoherentes: la cabecera se simplifica a 1200 y el hero a 1150, así que entre
	 * medias queda un estado que nadie ha diseñado. Con tres peldaños compartidos,
	 * todas las pantallas cambian a la vez y el resultado se puede mirar entero.
	 */
	public enum Regimen {

		/** Cabe lo imprescindible: se sueltan firmas, rótulos y cifras decorativas. */
		COMPACTO,

		/** El diseño completo, sin holguras. */
		MEDIO,

		/** Sobra sitio: la tipografía de display sube un escalón. */
		AMPLIO
	}

	/** En qué régimen está una ventana de este ancho. */
	public static Regimen regimen(int ancho) {

		if (ancho < UMBRAL_COMPACTO) {
			return Regimen.COMPACTO;
		}

		return ancho < UMBRAL_GRANDE ? Regimen.MEDIO : Regimen.AMPLIO;
	}

	/**
	 * Si hay que aligerar la pantalla.
	 *
	 * <p>
	 * Lo que se suelta en compacto es siempre <b>decoración o repetición</b>, nunca
	 * una acción ni un dato que no esté en otro sitio. Esconder un botón porque la
	 * ventana es pequeña sería cambiar un fallo visible por uno invisible.
	 */
	public static boolean esCompacto(int ancho) {
		return regimen(ancho) == Regimen.COMPACTO;
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
