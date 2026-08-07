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
 * Bloques en <b>tantas columnas como quepan</b>, y menos cuando no quepan.
 *
 * <p>
 * <b>Es la hermana vertical de {@link FilaFluida}, y nace del mismo fallo visto
 * desde el otro lado.</b> Aquella resolvió que una fila de elementos no exigiera
 * la suma de sus anchos; esta resuelve que un formulario no exija la suma de
 * <em>sus alturas</em>. Repartir un formulario largo en varias columnas es la
 * única forma de que quepa en el alto de un portátil —el alto de dos columnas es
 * el de la más alta, no la suma—, pero un número de columnas escrito a mano
 * cambia el problema de sitio en vez de resolverlo: tres columnas que caben
 * holgadas en 1280 puntos se convierten, en 1024, en tres columnas de 300 con los
 * chips dibujados fuera.
 *
 * <p>
 * Y eso pasó de verdad. Al repartir en tres columnas el alta de alojamiento y los
 * ajustes, el alto dejó de desbordarse y <b>aparecieron 44 componentes fuera del
 * área visible a 1024 puntos de ancho</b>: el arreglo de un eje era el fallo del
 * otro. La conclusión no es que las columnas estuvieran mal, es que <b>cuántas
 * columnas hay no es una propiedad del formulario, es una función del ancho</b>.
 *
 * <h2>Cómo decide</h2>
 *
 * <p>
 * Cada bloque declara un <b>ancho cómodo</b>: por debajo de él deja de leerse
 * bien, y ese suelo es lo que se respeta antes que el número de columnas. Se van
 * colocando bloques en la fila mientras quepan a ese ancho; el que no quepa abre
 * una fila nueva. Así, el mismo formulario sale en tres columnas en un monitor,
 * en dos en un portátil y en una en una ventana estrecha, sin que nadie escriba
 * ningún umbral.
 *
 * <p>
 * <b>El ancho cómodo no sustituye al preferido, lo acompaña:</b> un bloque que
 * pide más —el calendario de dos meses de la pantalla de reservar, que son 608
 * puntos de celdas y no se negocian— se coloca por lo que pide. El cómodo es un
 * suelo, no una talla única.
 *
 * <h2>Lo que hace que el rescate siga funcionando</h2>
 *
 * <p>
 * El alto <b>mínimo</b> de cada fila es el mayor de los mínimos de sus bloques, no
 * el de los preferidos. Es lo que permite que {@link Rescate} apriete el aire
 * antes de sacar la barra: si aquí se devolviera el preferido como mínimo, todo lo
 * que los bloques hubieran declarado como negociable dejaría de serlo al pasar por
 * este contenedor.
 */
public class Columnas extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Cuántas filas ocupaba la última vez, para no revalidar sin motivo. */
	private int filasAnteriores = -1;

	/**
	 * @param anchoComodo el ancho por debajo del cual un bloque deja de leerse bien.
	 *                    Es el que decide cuántas columnas caben
	 * @param separacion  el hueco entre columnas y entre filas
	 */
	public Columnas(int anchoComodo, int separacion) {

		setOpaque(false);
		setLayout(new Reparto(anchoComodo, separacion));

		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				revalidarSiCambiaElNumeroDeFilas();
			}
		});
	}

	private void revalidarSiCambiaElNumeroDeFilas() {

		int filas = ((Reparto) getLayout()).repartir(this, anchoUtil()).size();

		if (filas == filasAnteriores) {
			return;
		}

		filasAnteriores = filas;

		// invokeLater por lo mismo que en FilaFluida: revalidar en mitad de un pase de
		// layout se descarta, encolarlo lo deja para el siguiente ciclo.
		SwingUtilities.invokeLater(() -> {
			revalidate();
			repaint();
		});
	}

	/**
	 * El ancho del que se dispone de verdad, buscándolo hacia arriba si este
	 * contenedor aún no tiene. Misma necesidad y misma solución que en
	 * {@link FilaFluida}: sin este rastreo, la primera medición se hace sin límite y
	 * devuelve siempre una sola fila.
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

	/** Reparte los bloques en filas de tantas columnas como quepan. */
	private class Reparto implements LayoutManager {

		private final int anchoComodo;
		private final int separacion;

		Reparto(int anchoComodo, int separacion) {
			this.anchoComodo = anchoComodo;
			this.separacion = separacion;
		}

		/**
		 * Lo que un bloque necesita de verdad: su suelo cómodo, o su mínimo si es mayor.
		 *
		 * <p>
		 * <b>El mínimo, no el preferido, y el primer intento se equivocó justo aquí.</b>
		 * El ancho preferido de una columna de formulario está inflado por su contenido
		 * —una fila de chips pide de preferido el ancho de todos ellos sin doblar— así
		 * que empaquetar por él daba dos columnas donde cabían tres, y el formulario de
		 * alojamiento subió de 787 puntos de alto a 1198. El mínimo es la pregunta
		 * correcta porque es la única que tiene una respuesta honesta: por debajo de él
		 * el bloque se rompe, por encima se apaña.
		 *
		 * <p>
		 * Y por eso el calendario de la pantalla de reservar sigue funcionando sin decir
		 * nada especial: su mínimo son los 608 puntos de sus dos meses de celdas, y este
		 * método los respeta por encima de cualquier suelo.
		 */
		private int anchoDe(Component hijo) {
			return Math.max(anchoComodo, hijo.getMinimumSize().width);
		}

		/** Los bloques visibles agrupados en filas, según el ancho disponible. */
		private List<List<Component>> repartir(Container padre, int ancho) {

			Insets margenes = padre.getInsets();
			int util = ancho - margenes.left - margenes.right;

			List<List<Component>> filas = new ArrayList<>();
			List<Component> actual = new ArrayList<>();
			int ocupado = 0;

			for (Component hijo : padre.getComponents()) {

				if (!hijo.isVisible()) {
					continue;
				}

				int necesita = anchoDe(hijo);
				int conSeparacion = actual.isEmpty() ? necesita : separacion + necesita;

				// Sin ancho conocido todavía, todo va en una fila: es lo que pide el
				// contenido cuando nadie lo limita, y la respuesta correcta antes de que
				// exista contenedor.
				if (!actual.isEmpty() && util > 0 && ocupado + conSeparacion > util) {

					filas.add(actual);
					actual = new ArrayList<>();
					ocupado = necesita;

				} else {
					ocupado += conSeparacion;
				}

				actual.add(hijo);
			}

			if (!actual.isEmpty()) {
				filas.add(actual);
			}

			return filas;
		}

		@Override
		public Dimension preferredLayoutSize(Container padre) {

			Insets margenes = padre.getInsets();
			List<List<Component>> filas = repartir(padre, anchoUtil());

			int alto = 0;
			int ancho = 0;

			for (List<Component> fila : filas) {

				int altoDeFila = 0;
				int anchoDeFila = 0;

				for (Component hijo : fila) {
					altoDeFila = Math.max(altoDeFila, hijo.getPreferredSize().height);
					anchoDeFila += anchoDe(hijo) + separacion;
				}

				alto += altoDeFila + separacion;
				ancho = Math.max(ancho, anchoDeFila - separacion);
			}

			return new Dimension(ancho + margenes.left + margenes.right,
					Math.max(0, alto - separacion) + margenes.top + margenes.bottom);
		}

		/**
		 * El mínimo de ancho es <b>un solo bloque</b>, no la fila entera: por debajo del
		 * más ancho no hay reparto posible, por encima siempre se puede plegar a menos
		 * columnas. Es la misma idea que en {@link FilaFluida}, con bloques en lugar de
		 * con chips.
		 *
		 * <p>
		 * El de alto, en cambio, se calcula sobre el reparto actual y suma <b>los
		 * mínimos</b> de cada fila. Devolver aquí los preferidos anularía todo el aire
		 * que los bloques hayan declarado negociable, y con él la capacidad de
		 * {@link Rescate} de apretar antes de sacar la barra.
		 */
		@Override
		public Dimension minimumLayoutSize(Container padre) {

			Insets margenes = padre.getInsets();
			List<List<Component>> filas = repartir(padre, anchoUtil());

			int alto = 0;
			int ancho = 0;

			for (List<Component> fila : filas) {

				int altoDeFila = 0;

				for (Component hijo : fila) {
					altoDeFila = Math.max(altoDeFila, hijo.getMinimumSize().height);
					ancho = Math.max(ancho, hijo.getMinimumSize().width);
				}

				alto += altoDeFila + separacion;
			}

			return new Dimension(ancho + margenes.left + margenes.right,
					Math.max(0, alto - separacion) + margenes.top + margenes.bottom);
		}

		@Override
		public void layoutContainer(Container padre) {

			Insets margenes = padre.getInsets();
			int util = padre.getWidth() - margenes.left - margenes.right;

			List<List<Component>> filas = repartir(padre, padre.getWidth());

			int y = margenes.top;

			for (List<Component> fila : filas) {

				// **El sobrante se reparte a partes iguales, no en proporción al ancho que
				// pidió cada uno.** Repartirlo en proporción haría que la columna que ya era
				// la más ancha creciera más, y en una ventana grande las tres columnas de un
				// formulario acabarían con anchos visiblemente distintos sin que nada lo
				// justifique. A partes iguales, la rejilla se lee como una rejilla.
				int pedido = 0;

				for (Component hijo : fila) {
					pedido += anchoDe(hijo);
				}

				int sobrante = util - pedido - separacion * (fila.size() - 1);
				int extra = sobrante > 0 ? sobrante / fila.size() : 0;

				int x = margenes.left;
				int altoDeFila = 0;

				for (Component hijo : fila) {
					altoDeFila = Math.max(altoDeFila, hijo.getPreferredSize().height);
				}

				for (Component hijo : fila) {

					int w = anchoDe(hijo) + extra;

					// Todos los bloques de una fila reciben el mismo alto. En tarjetas es lo
					// que hace que sus bordes inferiores coincidan —tres marcos acabando a
					// distinta altura se leen como un fallo de maquetación— y en columnas de
					// formulario no cambia nada, porque su contenido se coloca desde arriba.
					hijo.setBounds(x, y, w, altoDeFila);
					x += w + separacion;
				}

				y += altoDeFila + separacion;
			}
		}

		@Override
		public void addLayoutComponent(String nombre, Component componente) {
			// Sin restricciones por bloque: el orden de adición es toda la información
			// que hace falta.
		}

		@Override
		public void removeLayoutComponent(Component componente) {
			// Nada que soltar.
		}
	}
}
