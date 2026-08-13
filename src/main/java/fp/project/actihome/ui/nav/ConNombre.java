package fp.project.actihome.ui.nav;

/**
 * Una pantalla que sabe decir cómo se llama.
 *
 * <p>
 * <b>Existe para que el botón de atrás pueda decir la verdad.</b> Hasta la Fase
 * 9 cada enlace de "volver" llevaba escrito a dónde iba: el de la conversación
 * decía "← Mensajes" y siempre iba a la bandeja, viniera de donde viniera. Con
 * {@link Navigator#volver} el destino ya no es fijo —es la pantalla anterior de
 * verdad—, así que la etiqueta tampoco puede serlo: un enlace que dice
 * "Mensajes" y lleva a la ficha de un alojamiento es peor que uno impreciso.
 *
 * <p>
 * <b>Y es una interfaz y no un mapa dentro del navegador a propósito.</b> El
 * {@link Navigator} está construido sobre la idea de que no conoce ninguna
 * pantalla en concreto —ese es el acoplamiento que vino a romper—, así que una
 * tabla de {@code Class → nombre} allí dentro lo desharía justo por donde más
 * duele. Preguntándoselo a la propia pantalla, el navegador sigue sin saber que
 * existe ninguna: solo sabe que algunas contestan.
 *
 * <p>
 * Quien no la implementa no rompe nada: el enlace se queda con su texto
 * genérico de "Atrás".
 */
public interface ConNombre {

	/**
	 * El nombre de esta pantalla, <b>ya traducido</b>, tal y como debe aparecer
	 * dentro de un "← ...".
	 *
	 * <p>
	 * Traducido y no una clave, porque una pantalla puede querer decir algo que
	 * no está en el fichero de textos — la ficha de un alojamiento se llama como
	 * el alojamiento.
	 */
	String nombreDePantalla();
}
