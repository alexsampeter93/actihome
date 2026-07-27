package fp.project.actihome.ui.theme;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Estado visual de la aplicación: qué estación está activa y quién quiere
 * enterarse cuando cambia.
 *
 * <p>
 * <b>El problema que resuelve.</b> Cuando el usuario pulsa "Invierno" en el
 * catálogo, tienen que repintarse el catálogo, la cabecera, la mascota y
 * cualquier otra ventana abierta. La solución ingenua sería que el selector
 * conociera todas las pantallas y las fuera avisando una a una — con lo que
 * añadir una pantalla nueva obligaría a tocar el selector, y tendríamos
 * diecisiete clases enredadas entre sí.
 *
 * <p>
 * <b>El patrón observador.</b> En su lugar se invierte la relación: quien
 * quiera enterarse se apunta a una lista ({@link #alCambiar(Consumer)}), y
 * {@code Theme} recorre esa lista cuando hay novedad. {@code Theme} no sabe
 * quién le escucha ni le importa. Es el mismo mecanismo de los
 * {@code ActionListener} de Swing, y la razón de que se pueda añadir una
 * pantalla sin tocar nada de lo ya escrito.
 *
 * <p>
 * <b>Por qué es estático.</b> Lo habitual en este proyecto sería un bean de
 * Spring, pero el tema debe estar disponible antes de que arranque el contexto
 * (el Look and Feel se instala en {@code main()}) y también fuera de él, como
 * en {@link ThemePreview}. Aquí el estado global está justificado: hay una sola
 * aplicación, una sola ventana visible a la vez y una sola estación activa.
 *
 * <p>
 * <b>Cuidado con las fugas.</b> Cada oyente registrado es una referencia viva:
 * una ventana que se apunta y no se da de baja nunca podrá ser liberada por el
 * recolector de basura. Toda pantalla que llame a {@link #alCambiar(Consumer)}
 * debe guardar el testigo devuelto y llamar a {@link #olvidar(Object)} al
 * cerrarse.
 */
public final class Theme {

	// --- Neutros, comunes a las cuatro estaciones ---

	/** Superficie de fichas y formularios. */
	public static final Color SURFACE = Color.WHITE;

	/** Línea de separación finísima. En este diseño sustituye a las sombras difusas. */
	public static final Color HAIRLINE = new Color(0, 0, 0, 31);

	/** Borde de los campos de formulario. */
	public static final Color FIELD_BORDER = new Color(0, 0, 0, 51);

	/** Texto sobre el color de acento. */
	public static final Color ON_ACCENT = Color.WHITE;

	/**
	 * Rojo de error, común a las cuatro estaciones.
	 *
	 * <p>
	 * No es un token estacional a propósito. El color de un error <b>no debe
	 * cambiar</b> con el tema: es la única señal de la interfaz que el usuario debe
	 * reconocer al instante y siempre igual. Un error que en primavera es verdoso y
	 * en invierno azulado deja de leerse como error.
	 *
	 * <p>
	 * Es un rojo apagado y terroso, no un rojo puro de alerta: encaja con la
	 * dirección editorial y evita el aire de formulario web roto.
	 */
	public static final Color DANGER = new Color(0xA33A2E);

	/**
	 * Texto secundario <b>sobre fondo oscuro</b> (la barra de cabecera).
	 *
	 * <p>
	 * No vale usar {@code mut()} ahí: está pensado para texto secundario sobre el
	 * fondo claro, y sobre la cabecera oscura queda casi ilegible. Se detectó
	 * mirando la primera captura de la guía de estilo, no leyendo el código: es la
	 * clase de fallo que solo se ve con los ojos.
	 *
	 * <p>
	 * Se deriva del fondo de página bajándole la opacidad, así que acompaña a la
	 * estación activa sin necesidad de un token nuevo por estación.
	 */
	public static Color mutSobreOscuro() {

		Color base = estacion.bg();
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), 155);
	}

	private static Season estacion = Season.actual();

	/**
	 * Lista de oyentes. Se usa {@link CopyOnWriteArrayList} porque un oyente puede
	 * darse de baja mientras se está recorriendo la lista para avisarle, y una
	 * lista normal lanzaría {@code ConcurrentModificationException}.
	 */
	private static final List<Consumer<Season>> oyentes = new CopyOnWriteArrayList<>();

	private Theme() {
	}

	/** La estación activa ahora mismo. */
	public static Season estacion() {
		return estacion;
	}

	/**
	 * Cambia la estación activa y avisa a todos los oyentes. Si la estación ya era
	 * esa, no hace nada: repintar sin motivo es trabajo tirado y provoca parpadeos.
	 */
	public static void cambiarA(Season nueva) {

		if (nueva == null || nueva == estacion) {
			return;
		}

		estacion = nueva;

		for (Consumer<Season> oyente : oyentes) {
			oyente.accept(estacion);
		}
	}

	/**
	 * Registra a alguien interesado en los cambios de estación.
	 *
	 * @return un testigo con el que darse de baja mediante {@link #olvidar(Object)}
	 */
	public static Object alCambiar(Consumer<Season> oyente) {

		oyentes.add(oyente);
		return oyente;
	}

	/** Da de baja a un oyente registrado. Debe llamarse al cerrar una pantalla. */
	public static void olvidar(Object testigo) {
		oyentes.remove(testigo);
	}

	/** Cuántos oyentes hay registrados. Sirve para detectar fugas en pruebas. */
	static int numeroDeOyentes() {
		return oyentes.size();
	}

	// --- Atajos de lectura, para no escribir Theme.estacion().acc() por todas partes ---

	public static Color acc() {
		return estacion.acc();
	}

	public static Color bg() {
		return estacion.bg();
	}

	public static Color hdr() {
		return estacion.hdr();
	}

	public static Color txt() {
		return estacion.txt();
	}

	public static Color mut() {
		return estacion.mut();
	}

	public static Color img() {
		return estacion.img();
	}

	public static Color imgTint() {
		return estacion.imgTint();
	}
}
