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

	/**
	 * Borde de los campos de formulario.
	 *
	 * <p>
	 * <b>Ajustado en la Fase 7</b> tras medir con {@code MedirContraste}: el valor
	 * original ({@code rgba(0,0,0,.2)}) daba 1,61:1 contra una ficha blanca, muy por
	 * debajo del 3:1 que exige WCAG para el borde de un componente de interfaz —un
	 * campo vacío apenas se distinguía de la superficie que lo rodea. Subido a
	 * {@code rgba(0,0,0,.42)}, que da 3,03:1.
	 */
	public static final Color FIELD_BORDER = new Color(0, 0, 0, 107);

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

		Color base = bg();
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), 155);
	}

	private static Season estacion = Season.actual();

	/** La estación de la que se viene mientras dura la transición. */
	private static Season saliente;

	/** Cuánto ha avanzado la transición, de 0 (saliente) a 1 (entrante). */
	private static double avance = 1;

	/** La transición viva, para cancelarla si el usuario cambia otra vez a media animación. */
	private static javax.swing.Timer transicion;

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

		// La estación anterior se guarda para poder mezclar los colores durante la
		// transición. La nueva pasa a ser la activa DE INMEDIATO: lo que se difumina es
		// el color, no la lógica. Un componente que pregunta "¿qué estación es?" para
		// elegir la pose de la mascota o la frase editorial debe recibir ya la nueva —
		// una mascota a medio camino entre dos ilustraciones no existe.
		saliente = estacion;
		estacion = nueva;
		avance = 0;

		for (Consumer<Season> oyente : oyentes) {
			oyente.accept(estacion);
		}

		Animacion.cancelar(transicion);
		transicion = Animacion.animar(new javax.swing.JPanel(), 0, 1, Animacion.AMBIENTE, v -> {
			avance = v;
			repintarTodo();
		});
	}

	/**
	 * Repinta todas las ventanas visibles.
	 *
	 * <p>
	 * <b>Hace falta porque la transición no la dispara nadie desde dentro.</b> Los
	 * componentes de este sistema no guardan su color, lo piden al pintar — que es
	 * justo lo que hace que la mezcla funcione sin tocar ni una de las dieciocho
	 * pantallas—, pero eso también significa que <em>nadie sabe</em> que el color ha
	 * cambiado y por tanto nadie se repinta solo. El aviso tiene que venir de aquí.
	 *
	 * <p>
	 * Se recorren las ventanas de AWT en vez de guardar una lista propia: la lista
	 * habría que mantenerla al día y sería una segunda fuente de fugas, exactamente
	 * el problema que ya obliga a dar de baja los oyentes.
	 */
	private static void repintarTodo() {

		for (java.awt.Window ventana : java.awt.Window.getWindows()) {

			if (ventana.isVisible()) {
				ventana.repaint();
			}
		}
	}

	/**
	 * Mezcla el color de la estación saliente con el de la entrante según lo
	 * avanzada que esté la transición.
	 *
	 * <p>
	 * <b>Aquí está toda la animación de ambiente de la aplicación, en cinco
	 * líneas.</b> Como ningún componente guarda su color —lo pide en cada pintado—,
	 * basta con que estos atajos devuelvan un valor intermedio para que las
	 * dieciocho pantallas, las partículas, la cabecera y hasta el velo sobre las
	 * fotos se fundan a la vez. Es el rendimiento de haber seguido la regla "el
	 * color no se guarda, se resuelve al pintar" durante ocho fases sin excepciones.
	 */
	private static Color enTransicion(java.util.function.Function<Season, Color> token) {

		if (saliente == null || avance >= 1) {
			return token.apply(estacion);
		}

		return Animacion.mezclar(token.apply(saliente), token.apply(estacion), avance);
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
		return enTransicion(Season::acc);
	}

	/** Acento para texto sobre fondo claro. Ver {@link Season#accText()}. */
	public static Color accText() {
		return enTransicion(Season::accText);
	}

	/**
	 * Color de las partículas de la estación. Ver {@link Season#particula()}.
	 *
	 * <p>
	 * <b>Este NO se mezcla</b>, y es la única excepción. Las partículas se sustituyen
	 * en el mismo momento —un copo deja de ser un copo y pasa a ser un pétalo— así que
	 * teñir los pétalos nuevos con el lila de la nieve durante un cuarto de segundo no
	 * suaviza nada: pinta una forma con el color de otra. Lo que se funde es el
	 * ambiente; lo que se sustituye, se sustituye.
	 */
	public static Color particula() {
		return estacion.particula();
	}

	/**
	 * Texto y trazos que van <b>encima</b> del color de acento.
	 *
	 * <p>
	 * Depende de la estación y no es una constante, que es lo que era antes. El
	 * motivo es el amarillo de verano: una etiqueta blanca sobre él daría 2,4:1 —
	 * ilegible—, mientras que en oscuro da 7,3:1. En las otras tres el acento es
	 * suficientemente profundo y el blanco es lo correcto.
	 *
	 * <p>
	 * Es la regla de siempre del sistema, aplicada también aquí: <b>el color no se
	 * guarda, se resuelve al pintar</b>. Como constante, cambiar de estación no lo
	 * habría actualizado.
	 */
	public static Color onAccent() {
		return enTransicion(s -> s == Season.VERANO ? s.txt() : Color.WHITE);
	}

	public static Color bg() {
		return enTransicion(Season::bg);
	}

	public static Color hdr() {
		return enTransicion(Season::hdr);
	}

	public static Color txt() {
		return enTransicion(Season::txt);
	}

	public static Color mut() {
		return enTransicion(Season::mut);
	}

	public static Color img() {
		return enTransicion(Season::img);
	}

	public static Color imgTint() {
		return enTransicion(Season::imgTint);
	}
}
