package fp.project.actihome.model.services;

/**
 * La aritmética de un mapa de teselas (F19).
 *
 * <p>
 * <b>Qué es una tesela.</b> Un mapa web no se sirve como una imagen gigante:
 * se sirve troceado en cuadrados de 256×256 píxeles llamados <i>teselas</i>. El
 * mundo entero es una sola tesela en el nivel de zoom 0; en el nivel 1 son
 * cuatro (2×2); en el 2, dieciséis. En general, <b>2^zoom por lado</b>. Cada
 * tesela se pide por su fila y su columna, y el cliente compone las que necesita.
 *
 * <p>
 * Esta clase traduce <b>latitud y longitud</b> —que es lo que guarda un
 * alojamiento— a <b>fila y columna de tesela</b>, que es lo que entiende el
 * servidor.
 *
 * <p>
 * <b>Por qué la fórmula de la latitud es rara y la de la longitud no.</b> La
 * proyección que usan todos los mapas web se llama <i>Web Mercator</i>, y su
 * propiedad es que conserva los ángulos: un cruce de calles se ve en ángulo recto
 * en el mapa igual que en el terreno. Eso se paga estirando las distancias hacia
 * los polos —por eso Groenlandia parece del tamaño de África sin serlo— y ese
 * estiramiento es el logaritmo que aparece abajo. La longitud no lo necesita
 * porque los meridianos sí están igual de separados en todas partes.
 *
 * <p>
 * <b>Devuelve un decimal a propósito</b>, no un entero. La parte entera dice qué
 * tesela es y <b>la fracción dice en qué punto dentro de ella</b> cae la
 * coordenada, que es justo lo que hace falta para centrar el mapa y colocar el
 * marcador. Redondear aquí obligaría a volver a calcularlo fuera.
 *
 * <p>
 * <b>Está en la capa de servicios y sin dependencias, así que se puede probar
 * sin red y sin ventana</b> — que es lo contrario de lo que pasaría si esta
 * cuenta viviera dentro del componente que pinta.
 */
public final class Teselas {

	/** Lado de una tesela, en píxeles. Es un estándar de hecho en todos los proveedores. */
	public static final int LADO = 256;

	/**
	 * Web Mercator no llega a los polos.
	 *
	 * <p>
	 * A 90° la fórmula diverge: la tangente se va a infinito y el resultado deja de
	 * ser un número. El corte convencional está en ±85,0511°, que es la latitud a
	 * la que el mapa sale exactamente cuadrado. No es una limitación de este
	 * código, es de la proyección — y por eso los mapas web no enseñan la Antártida
	 * entera.
	 */
	private static final double LATITUD_MAXIMA = 85.0511;

	private Teselas() {
	}

	/** Cuántas teselas hay por lado en este zoom: 2^zoom. */
	public static int porLado(int zoom) {
		return 1 << zoom;
	}

	/**
	 * Columna de tesela, con decimales, para una longitud dada.
	 *
	 * <p>
	 * Es una regla de tres: la longitud va de -180 a 180 y las columnas de 0 a
	 * 2^zoom.
	 */
	public static double columna(double longitud, int zoom) {
		return (longitud + 180.0) / 360.0 * porLado(zoom);
	}

	/**
	 * Fila de tesela, con decimales, para una latitud dada.
	 *
	 * <p>
	 * Aquí está el logaritmo de Mercator. La latitud se acota antes de entrar en la
	 * fórmula: sin ese tope, un valor de 90° daría infinito y a partir de ahí todo
	 * lo que se calcule con él es basura silenciosa.
	 */
	public static double fila(double latitud, int zoom) {

		double acotada = Math.max(-LATITUD_MAXIMA, Math.min(LATITUD_MAXIMA, latitud));
		double radianes = Math.toRadians(acotada);

		return (1 - Math.log(Math.tan(radianes) + 1 / Math.cos(radianes)) / Math.PI) / 2 * porLado(zoom);
	}

	/**
	 * Deja un índice de tesela dentro del mapa.
	 *
	 * <p>
	 * <b>Los dos ejes se tratan distinto, y no es un descuido.</b> En horizontal el
	 * mundo <b>da la vuelta</b> —después del meridiano 180 viene el -180— así que
	 * se envuelve con un módulo. En vertical no: por encima del polo no hay nada,
	 * así que se recorta. Pedir una tesela fuera de rango devuelve un error del
	 * servidor, que en pantalla se vería como un hueco gris sin explicación.
	 */
	public static int envolverColumna(int columna, int zoom) {

		int total = porLado(zoom);
		int dentro = columna % total;

		return dentro < 0 ? dentro + total : dentro;
	}

	/** Ver la nota de {@link #envolverColumna}: en vertical se recorta, no se envuelve. */
	public static int acotarFila(int fila, int zoom) {
		return Math.max(0, Math.min(porLado(zoom) - 1, fila));
	}

	/** Si esa fila existe en este zoom. Las de fuera no se piden. */
	public static boolean filaValida(int fila, int zoom) {
		return fila >= 0 && fila < porLado(zoom);
	}
}
