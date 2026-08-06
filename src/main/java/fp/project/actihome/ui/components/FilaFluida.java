package fp.project.actihome.ui.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Una fila de elementos que <b>se dobla en varias líneas</b> cuando no caben.
 *
 * <p>
 * <b>El problema que resuelve, y por qué era el fallo de fondo de toda la
 * aplicación.</b> Los chips de filtro, los destinos de la barra de navegación o
 * los botones de un formulario se colocaban en una fila de MigLayout sin
 * posibilidad de salto. Una fila así tiene un ancho mínimo que es la
 * <em>suma</em> de todo lo que lleva dentro, y ese número no se puede negociar:
 * si la ventana es más estrecha, MigLayout no encoge los elementos —hace lo
 * correcto, no los recorta— sino que <b>desborda el contenedor</b> y los deja
 * dibujados fuera de la ventana. El usuario no ve un texto apretado; ve que el
 * último chip no está.
 *
 * <p>
 * Y ese ancho mínimo crece con el escalado del sistema, porque las fuentes miden
 * más. De ahí venía que la aplicación se viera bien en un monitor sin escalar y
 * se rompiera en un portátil al 150 %: no es que el portátil sea pequeño, es que
 * a la aplicación le llegan <b>1280 puntos lógicos en vez de 1920</b>, y una fila
 * que exige 1400 no cabe de ninguna manera.
 *
 * <p>
 * Con esta fila el ancho mínimo deja de ser la suma y pasa a ser <b>el elemento
 * más ancho</b>. Es la diferencia entre exigir 1400 puntos y exigir 160: por
 * debajo de eso ya no hay nada que hacer, pero por encima siempre cabe, doblando
 * en cuantas líneas hagan falta.
 *
 * <p>
 * <b>La parte delicada: el alto depende del ancho.</b> Swing pregunta "¿cuánto
 * necesitas?" sin decir "¿de cuánto ancho dispones?", y aquí la respuesta
 * depende de la pregunta que no se hace — con 1400 puntos esto es una línea, con
 * 700 son dos y el doble de alto. Se resuelve mirando el ancho que ya tiene el
 * contenedor, y revalidando cuando ese ancho cambia el número de líneas. Se
 * revalida <b>solo</b> si el número de líneas cambia: hacerlo en cada píxel de
 * arrastre sería un bucle de layout que dejaría la ventana temblando.
 */
public class FilaFluida extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Cuántas líneas ocupaba la última vez, para no revalidar sin motivo. */
	private int lineasAnteriores = -1;

	/** Fila con la separación estándar entre elementos de un grupo. */
	public FilaFluida(int separacionHorizontal, int separacionVertical) {

		setOpaque(false);
		setLayout(new Flujo(separacionHorizontal, separacionVertical));

		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				revalidarSiCambiaElNumeroDeLineas();
			}
		});
	}

	private void revalidarSiCambiaElNumeroDeLineas() {

		int lineas = ((Flujo) getLayout()).lineasCon(this, anchoUtil());

		if (lineas == lineasAnteriores) {
			return;
		}

		lineasAnteriores = lineas;

		// invokeLater: estamos dentro de un pase de layout, y pedir una revalidación en
		// mitad de él la descarta. Encolarla la deja para el siguiente ciclo, que es
		// cuando ya se puede atender.
		SwingUtilities.invokeLater(() -> {
			revalidate();
			repaint();
		});
	}

	/**
	 * El ancho del que se dispone de verdad.
	 *
	 * <p>
	 * Si el contenedor todavía no tiene ancho —Swing pregunta el tamaño preferido
	 * antes de colocar nada—, se busca el primer antepasado que ya lo tenga. Sin ese
	 * rastreo hacia arriba, la primera medición se hace "sin límite" y devuelve una
	 * sola línea larguísima; la ventana se abriría con el alto de una línea y solo se
	 * corregiría al primer redimensionado, que es un parpadeo perfectamente evitable.
	 */
	private int anchoUtil() {

		Container actual = this;
		int descontar = 0;

		while (actual != null) {

			if (actual.getWidth() > 0) {
				return Math.max(0, actual.getWidth() - descontar);
			}

			Insets margenes = actual.getInsets();
			descontar += margenes.left + margenes.right;
			actual = actual.getParent();
		}

		return 0;
	}

	/** Coloca los elementos en cuantas líneas hagan falta. */
	private class Flujo implements LayoutManager {

		private final int gapH;
		private final int gapV;

		Flujo(int gapH, int gapV) {
			this.gapH = gapH;
			this.gapV = gapV;
		}

		@Override
		public Dimension preferredLayoutSize(Container padre) {
			return recorrer(padre, anchoUtil(), false);
		}

		/**
		 * El mínimo es el elemento más ancho, no la suma.
		 *
		 * <p>
		 * Es toda la idea de esta clase expresada en un método: por debajo del elemento
		 * más ancho no hay reflujo que valga —una etiqueta de 160 puntos necesita 160
		 * puntos—, pero por encima siempre se puede repartir en más líneas. Devolver la
		 * suma, que es lo que hace una fila normal, es lo que propagaba un mínimo
		 * imposible hasta la ventana entera.
		 *
		 * <p>
		 * <b>El alto que se devuelve es el del ancho actual, no el del ancho mínimo</b>,
		 * y la diferencia se vio a la primera. Devolviendo el alto de estar plegada a
		 * una columna —cinco chips, cinco líneas— MigLayout reservaba ese alto
		 * <em>siempre</em>, también en una ventana ancha donde los chips caben en una
		 * línea: la banda de filtros del catálogo pasó de 114 a 280 puntos y el rótulo
		 * "Tipo" quedó flotando ochenta puntos por debajo de sus propios chips. El
		 * mínimo de ancho y el alto son dos preguntas distintas, y contestar las dos con
		 * la misma medición mezcla un tamaño hipotético con el real.
		 */
		@Override
		public Dimension minimumLayoutSize(Container padre) {

			Insets margenes = padre.getInsets();
			int masAncho = 0;

			for (Component hijo : padre.getComponents()) {

				if (hijo.isVisible()) {
					masAncho = Math.max(masAncho, hijo.getPreferredSize().width);
				}
			}

			return new Dimension(masAncho + margenes.left + margenes.right, preferredLayoutSize(padre).height);
		}

		@Override
		public void layoutContainer(Container padre) {
			recorrer(padre, padre.getWidth(), true);
		}

		/**
		 * Alinea verticalmente los elementos de una linea por su CENTRO.
		 *
		 * <p>
		 * Sin esto, cada hijo se coloca en el borde superior de la linea, y como no
		 * todos miden lo mismo -una etiqueta y un boton de enlace, que lleva su propio
		 * relleno vertical- sus textos quedan a alturas distintas. En el pie del login
		 * se veia: "¿Has olvidado la contraseña?" caia unos pixeles por debajo de
		 * "Registrate", y dos textos de la misma frase a distinta altura se leen como
		 * un descuido.
		 */
		private void centrarEnLaLinea(List<Component> linea, int y, int altoDeLinea, int margenSuperior) {

			for (Component hijo : linea) {

				int alto = hijo.getHeight();
				hijo.setLocation(hijo.getX(), margenSuperior + y + (altoDeLinea - alto) / 2);
			}
		}

		int lineasCon(Container padre, int ancho) {

			int lineas = 1;
			int x = 0;
			boolean primero = true;

			for (Component hijo : padre.getComponents()) {

				if (!hijo.isVisible()) {
					continue;
				}

				int w = hijo.getPreferredSize().width;

				if (!primero && x + gapH + w > disponible(padre, ancho)) {
					lineas++;
					x = w;
				} else {
					x += primero ? w : gapH + w;
				}

				primero = false;
			}

			return lineas;
		}

		private int disponible(Container padre, int ancho) {

			Insets margenes = padre.getInsets();
			int util = ancho - margenes.left - margenes.right;

			// Sin ancho conocido se mide "a una línea": es lo que pide el contenido si
			// nadie lo limita, y es la respuesta correcta cuando aún no hay contenedor.
			return util > 0 ? util : Integer.MAX_VALUE;
		}

		/**
		 * Recorre los elementos repartiéndolos en líneas.
		 *
		 * <p>
		 * El mismo recorrido sirve para medir y para colocar, y eso <b>no es un
		 * ahorro de líneas sino una garantía</b>: dos recorridos separados acaban
		 * discrepando en algún caso raro, y entonces el contenedor reserva un alto y
		 * dibuja otro.
		 *
		 * @param colocar si además de medir hay que fijar la posición de cada elemento
		 */
		private Dimension recorrer(Container padre, int ancho, boolean colocar) {

			Insets margenes = padre.getInsets();
			int util = disponible(padre, ancho);

			List<Component> enEstaLinea = new ArrayList<>();

			int x = 0;
			int y = 0;
			int altoDeLinea = 0;
			int anchoMaximo = 0;
			boolean primero = true;

			for (Component hijo : padre.getComponents()) {

				if (!hijo.isVisible()) {
					continue;
				}

				Dimension tamano = hijo.getPreferredSize();

				if (!primero && x + gapH + tamano.width > util) {

					// Se cierra la linea anterior antes de empezar la siguiente.
					if (colocar) {
						centrarEnLaLinea(enEstaLinea, y, altoDeLinea, margenes.top);
						enEstaLinea.clear();
					}

					y += altoDeLinea + gapV;
					x = 0;
					altoDeLinea = 0;
					primero = true;
				}

				if (!primero) {
					x += gapH;
				}

				if (colocar) {
					// Se anota y se coloca despues: el centrado vertical necesita saber el alto
					// de la linea ENTERA, y eso no se sabe hasta haberla recorrido toda.
					enEstaLinea.add(hijo);
					hijo.setBounds(margenes.left + x, margenes.top + y, tamano.width, tamano.height);
				}

				x += tamano.width;
				altoDeLinea = Math.max(altoDeLinea, tamano.height);
				anchoMaximo = Math.max(anchoMaximo, x);
				primero = false;
			}

			if (colocar) {
				centrarEnLaLinea(enEstaLinea, y, altoDeLinea, margenes.top);
			}

			return new Dimension(anchoMaximo + margenes.left + margenes.right,
					y + altoDeLinea + margenes.top + margenes.bottom);
		}

		@Override
		public void addLayoutComponent(String nombre, Component componente) {
			// Sin restricciones por elemento: el orden de adición es toda la información
			// que necesita una fila que fluye.
		}

		@Override
		public void removeLayoutComponent(Component componente) {
			// Nada que soltar.
		}
	}
}
