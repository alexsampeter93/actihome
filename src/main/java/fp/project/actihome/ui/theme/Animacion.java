package fp.project.actihome.ui.theme;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Timer;

/**
 * Interpola un número de 0 a 1 a lo largo de un tiempo y repinta mientras dura.
 *
 * <p>
 * <b>Es la pieza que separa "funciona" de "está cuidado".</b> Hasta ahora todo
 * en esta aplicación cambiaba de golpe: pasabas el ratón por un botón y el
 * color saltaba, cambiabas de estación y las diecisiete pantallas se
 * repintaban de otro color en un fotograma. Nada de eso es un fallo, y aun así
 * es lo primero que distingue un producto por el que se paga de uno que
 * simplemente hace su trabajo. El ojo lee un salto como un parpadeo y una
 * transición como una respuesta.
 *
 * <p>
 * <b>Un temporizador por animación, y muere al terminar.</b> La alternativa
 * —un reloj global permanente— late aunque no haya nada que animar, y en una
 * aplicación de dieciocho ventanas singleton eso significa consumir tiempo de
 * CPU por diecisiete pantallas ocultas. Es exactamente la trampa que ya
 * documenta {@code Particulas}, y la solución es la misma: que exista solo
 * mientras hay algo que mover.
 *
 * <p>
 * <b>Y una sola animación viva por componente.</b> Quien la arranca guarda el
 * testigo y lo cancela antes de empezar otra; si no, pasar el ratón por encima
 * y quitarlo deprisa deja dos temporizadores peleándose por el mismo valor y
 * el botón vibra. Ver {@link #cancelar(Timer)}.
 *
 * <p>
 * <b>La curva no es lineal a propósito.</b> Un movimiento a velocidad
 * constante se percibe mecánico porque nada en el mundo físico arranca y para
 * de golpe. {@link #suavizar(double)} acelera al principio y frena al final,
 * que es lo que hace que 160 milisegundos se lean como una respuesta y no como
 * un salto lento.
 */
public final class Animacion {

	/** Cada cuánto se recalcula, en milisegundos. 60 pasos por segundo. */
	private static final int PASO = 16;

	/** Duración de un cambio de estado de un control (ratón encima, pulsado). */
	public static final int CONTROL = 160;

	/** Duración de un cambio de ambiente (la estación entera). */
	public static final int AMBIENTE = 260;

	/** Ver {@link #desactivarParaHerramientas()}. */
	private static boolean activa = true;

	private Animacion() {
	}

	/**
	 * Apaga toda animación de la aplicación: cada transición salta directa a su
	 * valor final.
	 *
	 * <p>
	 * <b>Lo llaman las herramientas de desarrollo, y es imprescindible.</b>
	 * {@code ScreenSnapshots} cambia de estación y pinta acto seguido;
	 * {@code MedirResponsive} construye las pantallas y recorre el árbol. Con las
	 * transiciones vivas, las dos leerían <em>un fotograma intermedio</em>: una
	 * captura saldría con el color a medio camino entre dos estaciones y el detector
	 * mediría un botón a mitad de su animación. El resultado dependería del
	 * cronómetro, y un resultado que depende del cronómetro no sirve para comparar
	 * dos versiones ni para decidir si algo está roto.
	 *
	 * <p>
	 * <b>Un único interruptor global y no un parámetro por llamada.</b> Es
	 * deliberado: así una animación que se añada mañana en cualquier componente
	 * queda cubierta sin que nadie tenga que acordarse de nada. La alternativa
	 * —desactivar caso por caso— es la que garantiza que dentro de tres fases
	 * alguien introduzca una animación nueva y vuelva a hacer intermitente el CI. Ya
	 * pasó tres veces en este proyecto por motivos parecidos.
	 */
	public static void desactivarParaHerramientas() {
		activa = false;
	}

	/**
	 * Lleva un valor de 0 a 1 en el tiempo dado, repintando el componente en cada
	 * paso.
	 *
	 * @param desde  valor de partida, normalmente el que la animación anterior dejó
	 *               a medias — arrancar siempre de 0 es lo que hace que un control
	 *               "salte hacia atrás" cuando el ratón entra y sale deprisa
	 * @param hasta  valor de destino
	 * @param aplica recibe el valor de cada paso; es quien guarda el resultado donde
	 *               lo vaya a leer el pintado
	 * @return el temporizador, para poder cancelarlo si empieza otra animación
	 */
	public static Timer animar(Component componente, double desde, double hasta, int duracion,
			java.util.function.DoubleConsumer aplica) {

		if (!activa) {

			aplica.accept(hasta);
			componente.repaint();

			return null;
		}

		long inicio = System.currentTimeMillis();

		Timer reloj = new Timer(PASO, null);

		reloj.addActionListener(e -> {

			double avance = Math.min(1.0, (System.currentTimeMillis() - inicio) / (double) duracion);
			aplica.accept(desde + (hasta - desde) * suavizar(avance));

			componente.repaint();

			if (avance >= 1.0) {
				reloj.stop();
			}
		});

		reloj.setInitialDelay(0);
		reloj.start();

		return reloj;
	}

	/** Para un temporizador si existe y sigue vivo. Tolera {@code null}. */
	public static void cancelar(Timer reloj) {

		if (reloj != null && reloj.isRunning()) {
			reloj.stop();
		}
	}

	/**
	 * Curva de aceleración y frenado ("ease in-out cúbica").
	 *
	 * <p>
	 * En la primera mitad va acelerando y en la segunda frenando, con el mismo
	 * perfil invertido. Es la curva más usada en interfaz precisamente porque no se
	 * nota: lo que se nota es su ausencia.
	 */
	public static double suavizar(double avance) {

		if (avance < 0.5) {
			return 4 * avance * avance * avance;
		}

		double resto = -2 * avance + 2;

		return 1 - resto * resto * resto / 2;
	}

	/**
	 * Mezcla dos colores.
	 *
	 * <p>
	 * Se interpolan también los canales alfa, que es lo que permite animar una
	 * <em>hairline</em> del 12 % al 28 % de negro sin cambiar de color.
	 */
	public static Color mezclar(Color desde, Color hasta, double avance) {

		double t = Math.max(0, Math.min(1, avance));

		return new Color(interpolar(desde.getRed(), hasta.getRed(), t),
				interpolar(desde.getGreen(), hasta.getGreen(), t),
				interpolar(desde.getBlue(), hasta.getBlue(), t),
				interpolar(desde.getAlpha(), hasta.getAlpha(), t));
	}

	private static int interpolar(int desde, int hasta, double avance) {
		return (int) Math.round(desde + (hasta - desde) * avance);
	}
}
