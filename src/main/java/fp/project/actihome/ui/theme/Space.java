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

	private Space() {
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
