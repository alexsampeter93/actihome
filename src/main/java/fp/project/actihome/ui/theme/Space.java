package fp.project.actihome.ui.theme;

import java.awt.Insets;

/**
 * Escala de espaciado del sistema de diseño.
 *
 * <p>
 * El espaciado es lo que más delata a una interfaz hecha a ojo. Cuando cada
 * pantalla elige sus márgenes por intuición aparecen separaciones de 13, 17 y
 * 22 píxeles que nadie sabe justificar, y el conjunto se percibe descuidado sin
 * que se pueda señalar por qué. Una escala cerrada elimina esa decisión: no se
 * elige un número, se elige un <em>peldaño</em>.
 *
 * <p>
 * Los valores son los del handoff. La escala no es lineal: crece a saltos cada
 * vez mayores, porque la diferencia entre 4 y 8 píxeles se percibe igual que la
 * que hay entre 40 y 48.
 *
 * <p>
 * Regla práctica: si necesitas un espaciado que no está aquí, casi siempre es
 * señal de que el problema es otro (una jerarquía mal resuelta). Antes de
 * añadir un peldaño, mira el diseño otra vez.
 */
public final class Space {

	/** 4 — separación mínima: entre una etiqueta y su campo. */
	public static final int XXS = 4;

	/** 8 — elementos muy relacionados: icono y texto. */
	public static final int XS = 8;

	/** 12 — dentro de un grupo: chips de una misma fila. */
	public static final int SM = 12;

	/** 16 — relleno estándar de una ficha pequeña. */
	public static final int MD = 16;

	/** 20 — relleno de superficies y separación entre campos de formulario. */
	public static final int LG = 20;

	/** 24 — separación entre bloques dentro de una sección. */
	public static final int XL = 24;

	/** 34 — relleno amplio de superficies y separación entre secciones. */
	public static final int XXL = 34;

	/** 40 — margen lateral de pantalla. */
	public static final int XXXL = 40;

	/** 44 — separación entre las dos columnas de la vista de lista. */
	public static final int HUGE = 44;

	/** 48 — respiración del bloque hero. */
	public static final int GIANT = 48;

	/** 60 — separación máxima entre grandes bloques editoriales. */
	public static final int MAX = 60;

	/**
	 * Cuánto del aire pedido se puede llegar a ceder. Por debajo de esto dos bloques
	 * empiezan a leerse como uno solo, que es el punto en el que ceder deja de ser
	 * gratis.
	 */
	private static final float CESION = 0.4f;

	private Space() {
	}

	/**
	 * El mismo peldaño, declarado como <b>rango negociable</b> para MigLayout
	 * ({@code "16:40:40"}).
	 *
	 * <p>
	 * <b>Es la regla número 2 del proyecto, que llevaba desde la Fase 7 aplicándose
	 * solo en horizontal.</b> Allí está escrita así: <em>«Las separaciones se
	 * declaran como rango min:pref:max, no como número. El aire se negocia; un botón
	 * no.»</em> Se escribió para el ancho —insets laterales, huecos entre columnas—
	 * porque el ancho era donde se veía el fallo, y el eje vertical se quedó con
	 * números fijos.
	 *
	 * <p>
	 * Y con números fijos <b>el aire es lo único que no cede</b>, así que cuando la
	 * ventana se queda corta lo que sobra no es la separación entre bloques: es el
	 * botón de guardar, que se va por debajo del borde. Exactamente al revés de lo
	 * que debería. Un formulario apretado sigue siendo usable; uno cuyo botón no se
	 * alcanza, no.
	 *
	 * <p>
	 * <b>Un rango no encoge nada por su cuenta.</b> Solo dice que <em>se puede</em>.
	 * En una ventana holgada el aire es el del diseño, punto por punto; se cobra
	 * únicamente cuando alguien reparte menos alto del preferido, y entonces
	 * MigLayout lo reparte entre lo que sí puede ceder. Un {@code height 44!} no
	 * cede, y por eso los botones siguen midiendo lo que miden.
	 *
	 * @param pref el peldaño de la escala que se quiere cuando hay sitio
	 */
	public static String aire(int pref) {
		return Math.max(XXS, Math.round(pref * CESION)) + ":" + pref + ":" + pref;
	}

	/**
	 * Como {@link #aire(int)}, pero para el <b>margen de página</b>, que puede ceder
	 * más.
	 *
	 * <p>
	 * La diferencia no es de grado, es de qué hay al otro lado. Un hueco entre dos
	 * bloques deja de hacer su trabajo mucho antes de llegar a cero: por debajo de
	 * cierto punto los dos bloques se leen como uno y el formulario pierde su
	 * agrupación. <b>Un margen no separa dos cosas, separa una cosa del borde de la
	 * ventana</b>, y ahí lo único que se pierde al apretarlo es holgura.
	 *
	 * <p>
	 * Por eso este cede hasta {@link #SM} y aquel solo el 40 %. Es lo que hace que,
	 * cuando falta sitio, lo primero que se estreche sea el marco y no la separación
	 * entre el campo de contraseña y el de repetirla.
	 */
	public static String margen(int pref) {
		return Math.min(SM, pref) + ":" + pref + ":" + pref;
	}

	/**
	 * Solo los márgenes laterales; arriba y abajo, cero.
	 *
	 * <p>
	 * <b>Existe porque los insets de MigLayout no admiten rangos.</b> Son cuatro
	 * {@code UnitValue} sueltos, no {@code BoundSize}, así que
	 * {@code "insets 16:40:40 …"} no entra en su gramática y un margen superior
	 * declarado ahí es innegociable por construcción.
	 *
	 * <p>
	 * La vuelta es declarar el margen vertical donde sí se admite un rango: el
	 * <b>hueco anterior a la primera fila y posterior a la última</b> de la
	 * especificación de filas, que es exactamente el mismo espacio visto desde
	 * dentro. De ahí el reparto: los laterales por aquí, los verticales como
	 * {@link #aire(int)} en los extremos del string de filas.
	 *
	 * <pre>
	 * new MigLayout("wrap 1, fill, " + Space.insetsLaterales(Space.GIANT, Space.GIANT),
	 *         "[grow,fill]",
	 *         Space.aire(Space.XXXL) + "[]" + Space.aire(Space.XL) + "[]" + Space.aire(Space.XXL));
	 * </pre>
	 */
	public static String insetsLaterales(int derecha, int izquierda) {
		return insets(0, derecha, 0, izquierda);
	}

	/** Márgenes iguales por los cuatro lados. */
	public static Insets all(int n) {
		return new Insets(n, n, n, n);
	}

	/** Márgenes verticales y horizontales. */
	public static Insets symmetric(int vertical, int horizontal) {
		return new Insets(vertical, horizontal, vertical, horizontal);
	}

	/**
	 * Cadena de márgenes en el formato que espera MigLayout
	 * ({@code "insets 20 40 20 40"}).
	 */
	public static String insets(int arriba, int derecha, int abajo, int izquierda) {
		return "insets " + arriba + " " + izquierda + " " + abajo + " " + derecha;
	}

	/** Márgenes iguales, en formato MigLayout. */
	public static String insets(int n) {
		return "insets " + n;
	}

}
