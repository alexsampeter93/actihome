package fp.project.actihome.ui.nav;

import java.awt.Image;
import java.awt.Window;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.ui.theme.BrandAssets;

/**
 * Único responsable de abrir y cerrar ventanas.
 *
 * <p>
 * <b>El problema que resuelve.</b> Hasta ahora cada pantalla navegaba por su
 * cuenta:
 *
 * <pre>
 * dispose();
 * ShowHousingsFrame destino = context.getBean(ShowHousingsFrame.class);
 * destino.setVisible(true);
 * </pre>
 *
 * Eso significa que diecisiete clases conocen a las demás, que todas guardan
 * una referencia al {@code ApplicationContext} de Spring —una ventana no
 * debería saber que Spring existe— y que la regla de "cerrar la anterior antes
 * de abrir la siguiente" está repetida diecisiete veces, con la posibilidad de
 * que a alguna se le olvide.
 *
 * <p>
 * Con el navegador, una pantalla dice a dónde quiere ir y se desentiende de
 * cómo:
 *
 * <pre>
 * navigator.ir(ShowHousingsFrame.class);
 * navigator.ir(HousingDetailsFrame.class, frame -&gt; frame.loadDetails(housing));
 * </pre>
 *
 * <p>
 * <b>Concepto: acoplamiento.</b> Dos clases están acopladas cuando cambiar una
 * obliga a tocar la otra. Antes, renombrar o dividir una pantalla obligaba a
 * revisar todas las que navegaban hacia ella. Ahora las pantallas no se
 * conocen: cada una conoce al navegador, y el navegador no conoce a ninguna en
 * concreto. Es la diferencia entre diecisiete piezas enredadas y diecisiete
 * piezas colgando de un eje.
 *
 * <p>
 * Y de paso resuelve dos bugs de raíz, sin tener que acordarse en cada
 * pantalla:
 * <ul>
 * <li><b>B13</b>: ninguna ventana declaraba qué hacer al pulsar la X, así que
 * regía el valor por defecto de Swing —ocultarla— y el proceso Java se quedaba
 * vivo en segundo plano. El navegador lo fija al mostrarla.</li>
 * <li>La ventana anterior se cierra <b>después</b> de mostrar la nueva. Al
 * revés, cerrar la última ventana visible puede terminar la aplicación antes de
 * que aparezca la siguiente.</li>
 * </ul>
 */
@Component
@Profile("!test")
public class Navigator {

	private final ApplicationContext context;

	/** La ventana que el usuario está viendo ahora mismo. */
	private JFrame visible;

	public Navigator(ApplicationContext context) {
		this.context = context;
	}

	/** Abre una pantalla y cierra la anterior. */
	public <T extends JFrame> void ir(Class<T> destino) {
		ir(destino, ventana -> {
			// Sin preparación previa.
		});
	}

	/**
	 * Abre una pantalla, dándole antes los datos que necesite.
	 *
	 * <p>
	 * La preparación se ejecuta <b>antes</b> de mostrarla, porque los frames
	 * recargan sus datos dentro de {@code setVisible} y para entonces ya deben
	 * saber, por ejemplo, de qué alojamiento hablan.
	 *
	 * @param preparar qué hacer con la ventana antes de mostrarla
	 */
	public <T extends JFrame> void ir(Class<T> destino, Consumer<T> preparar) {

		T ventana = context.getBean(destino);
		preparar.accept(ventana);
		mostrar(ventana);
	}

	private void mostrar(JFrame ventana) {

		JFrame anterior = visible;

		// Pulsar la X cierra la aplicación. Es una app de una sola ventana: ocultarla
		// y dejar el proceso vivo, que es el comportamiento por defecto de Swing, solo
		// sirve para dejar javaw.exe consumiendo memoria sin que nadie lo sepa.
		ventana.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		// El icono se pone aquí y no en cada pantalla: es el navegador quien las
		// muestra, así que es el único sitio por el que pasan todas. Si mañana se añade
		// una ventana, hereda el icono sin que nadie tenga que acordarse.
		List<Image> iconos = BrandAssets.iconosDeAplicacion();

		if (!iconos.isEmpty()) {
			ventana.setIconImages(iconos);
		}

		visible = ventana;
		ventana.setVisible(true);

		// Se cierra CUALQUIER otra ventana viva, no solo la que el navegador recuerda
		// haber mostrado.
		//
		// El motivo es que el rediseño va por fases y conviven dos estilos de
		// navegación: las pantallas nuevas usan este navegador y las que quedan por
		// rediseñar siguen haciendo "dispose(); getBean(...); setVisible(true)" por su
		// cuenta. Cuando una de esas navega, el navegador se queda apuntando a una
		// ventana ya cerrada; en el siguiente salto cerraría esa —que ya no está— en
		// lugar de la que el usuario tiene delante, y acabarían dos ventanas abiertas a
		// la vez. En una aplicación de una sola ventana eso se lee directamente como
		// que algo se ha roto.
		//
		// Preguntar por las ventanas vivas en vez de fiarse de lo apuntado hace que el
		// navegador sea correcto pase lo que pase fuera de él. Cuando no queden
		// pantallas del estilo antiguo, esto se podrá simplificar.
		//
		// El orden importa y no es negociable: **primero se muestra la nueva y después
		// se cierran las demás**. Al revés, cerrar la última ventana viva puede terminar
		// la aplicación antes de que aparezca la siguiente.
		for (Window abierta : Window.getWindows()) {

			if (abierta != ventana && abierta instanceof JFrame && abierta.isDisplayable()) {
				abierta.dispose();
			}
		}

		// Redundante con el bucle de arriba, pero explícito: la ventana que el navegador
		// sí conocía queda cerrada seguro.
		if (anterior != null && anterior != ventana) {
			anterior.dispose();
		}
	}
}
