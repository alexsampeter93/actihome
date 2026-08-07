package fp.project.actihome.ui.components;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;

/**
 * Envuelve el cuerpo de una pantalla en un scroll que <b>solo aparece si hace
 * falta</b>.
 *
 * <p>
 * <b>Esto no contradice la regla de escritorio del proyecto, la sostiene.</b> La
 * regla dice que una pantalla debe caber en la ventana y que el scroll vive
 * dentro de las listas, no en la página. Sigue siendo así: las pantallas se
 * diseñan para caber, y en una ventana de tamaño razonable esta barra no aparece
 * nunca. Lo que resuelve es el caso que la regla no cubría — <b>qué pasa cuando
 * la ventana es más pequeña que cualquier diseño razonable</b>.
 *
 * <p>
 * <b>Y antes de sacar la barra, aprieta.</b> Hasta que se midieron las dieciocho
 * pantallas contra un portátil de verdad, aquí solo había dos estados: cabía, o
 * salía barra. Faltando quince puntos salía barra, y una pantalla de formulario
 * que se recorre con la rueda es exactamente lo que la regla de escritorio
 * prohíbe. Ahora hay tres, y el orden importa: primero se cede el aire que se
 * declaró negociable ({@link fp.project.actihome.ui.theme.Space#aire}), y solo
 * cuando ya no queda aire que ceder aparece la barra. Ver
 * {@link Elastico#getScrollableTracksViewportHeight()}.
 *
 * <p>
 * Y ese caso no es hipotético: un portátil de 1920×1080 con el escalado de
 * Windows al 150 % le entrega a la aplicación <b>1280×720 puntos lógicos</b>, de
 * los que la barra de tareas y el marco de la ventana se llevan otros sesenta.
 * Con 660 puntos de alto, un formulario de siete campos no cabe por mucho que se
 * apriete. Sin esta red, el botón de guardar quedaba dibujado por debajo del
 * borde inferior: no cortado, <b>inalcanzable</b>. Es el peor fallo posible en un
 * formulario, porque deja la pantalla sin salida.
 *
 * <p>
 * La alternativa era impedir que la ventana se hiciera tan pequeña. No sirve: el
 * usuario no está encogiendo la ventana, es que su pantalla mide eso.
 *
 * <p>
 * <b>Nunca en horizontal.</b> Una barra horizontal en una pantalla de formulario
 * es señal de que el contenido no se ha adaptado al ancho, y taparlo con una
 * barra sería esconder el fallo en lugar de arreglarlo. El ancho se resuelve
 * reflujo mediante ({@link FilaFluida}), no desplazando.
 */
public final class Rescate {

	/**
	 * El salto por muesca de rueda. El de Swing por defecto es de un punto, que en un
	 * formulario se percibe como que la rueda no hace nada.
	 */
	private static final int SALTO_DE_RUEDA = 24;

	/**
	 * Marca que distingue un scroll de rescate de uno de lista.
	 *
	 * <p>
	 * Existe para {@code MedirPantallas}. La regla del proyecto no es «no haya
	 * scrolls» —los de las listas son correctos y necesarios— sino <b>que esta barra
	 * concreta no llegue a aparecer</b>, y desde fuera las dos son el mismo
	 * {@code JScrollPane}. Sin una marca explícita la herramienta tendría que
	 * adivinarlo por la forma del árbol, que es como se coló el punto ciego de
	 * {@code MedirResponsive} durante dos fases.
	 */
	public static final String MARCA = "actihome.rescate";


	private Rescate() {
	}

	/** Envuelve un componente en el scroll de rescate. */
	public static JScrollPane envolver(JComponent contenido) {

		JScrollPane scroll = new JScrollPane(new Elastico(contenido));

		scroll.putClientProperty(MARCA, Boolean.TRUE);

		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);

		// Los dos bordes, no solo uno: JScrollPane tiene el suyo y otro para el
		// viewport, y FlatLaf pinta una línea en el segundo. Quitando solo el primero
		// queda una raya pegada al contenido que parece un fallo de pintado.
		scroll.setBorder(null);
		scroll.setViewportBorder(null);

		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.getVerticalScrollBar().setUnitIncrement(SALTO_DE_RUEDA);

		// Mínimo cero, y es imprescindible. Cuando la ventana se queda corta el espacio
		// se lo tiene que quitar quien tiene barra de desplazamiento, no la cabecera ni
		// el titular. Sin esto, el reparto castiga a quien no ha declarado su mínimo y
		// se aplasta justo lo que no debe.
		scroll.setMinimumSize(new Dimension(0, 0));

		return scroll;
	}

	/**
	 * El contenido, que <b>ocupa toda la ventana cuando cabe</b> y solo se comporta
	 * como contenido desplazable cuando no.
	 *
	 * <p>
	 * <b>Sin esta pieza el rescate estropearía las quince pantallas que hoy están
	 * bien.</b> Un {@code JScrollPane} le da a lo que lleva dentro exactamente su
	 * tamaño preferido, ni más ni menos. Eso significa que un formulario centrado
	 * verticalmente en una ventana grande dejaría de estarlo: se pegaría al borde
	 * superior con todo el hueco debajo, porque el panel ya no recibe el alto de la
	 * ventana sino el suyo propio.
	 *
	 * <p>
	 * {@code Scrollable} es la interfaz con la que Swing pregunta justo eso — "¿te
	 * estiro hasta el tamaño de la ventana o te dejo a tu aire?"— y aquí la respuesta
	 * es <b>condicional</b>: estírame si hay sitio, déjame a mi tamaño si no lo hay.
	 * Con eso el rescate es invisible mientras no haga falta, que es la única forma
	 * de que una red de seguridad no se convierta en una molestia.
	 *
	 * <p>
	 * En anchura la respuesta es siempre que sí, y por eso no hay barra horizontal:
	 * el contenido recibe el ancho de la ventana y se adapta a él.
	 */
	private static class Elastico extends JPanel implements Scrollable {

		private static final long serialVersionUID = 1L;

		Elastico(JComponent contenido) {

			super(new BorderLayout());
			setOpaque(false);
			add(contenido, BorderLayout.CENTER);
		}

		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int direccion) {
			return SALTO_DE_RUEDA;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int direccion) {
			return visible.height;
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		/**
		 * <b>Se compara con el mínimo, no con el preferido, y ese cambio es el que
		 * convierte el rescate en última opción de verdad.</b>
		 *
		 * <p>
		 * Mientras se comparaba con el preferido había solo dos estados: o sobraba
		 * sitio, o salía la barra. Faltando un punto salía la barra. Y eso hacía que
		 * una pantalla que solo necesitaba apretar el aire treinta píxeles se
		 * recorriera con la rueda como una página web, que es justo lo que la regla de
		 * escritorio del proyecto prohíbe.
		 *
		 * <p>
		 * Con el mínimo hay tres, y en este orden:
		 *
		 * <ol>
		 * <li><b>Sobra sitio</b> — el contenido se estira y se centra, como siempre.</li>
		 * <li><b>Falta, pero cabe apretando</b> — el contenido recibe el alto de la
		 * ventana y MigLayout se lo quita a lo que declaró que podía cederlo
		 * ({@link fp.project.actihome.ui.theme.Space#aire}). Sin barra.</li>
		 * <li><b>No cabe ni apretando</b> — barra, que es el caso para el que se
		 * escribió esta clase.</li>
		 * </ol>
		 *
		 * <p>
		 * <b>Depende por completo de que cada componente diga la verdad sobre su
		 * mínimo.</b> Uno que declare el preferido y se calle el mínimo hereda el cero
		 * de Swing, y entonces esta pieza lo aplastará en vez de sacar la barra —que
		 * es peor, porque un contenido machacado no se ve como un fallo de tamaño—. Es
		 * la misma regla que ya está escrita en el manual: <em>si defines el preferido,
		 * define el mínimo</em>; lo que cambia es que ahora se cobra.
		 */
		@Override
		public boolean getScrollableTracksViewportHeight() {

			Container padre = getParent();

			return padre instanceof JViewport && padre.getHeight() >= getMinimumSize().height;
		}

		/**
		 * <b>Nunca por debajo del mínimo, diga lo que diga quien nos coloca.</b>
		 *
		 * <p>
		 * {@code getScrollableTracksViewportHeight()} contesta con el mínimo que hay en
		 * ese instante, y el visor nos da su alto después. Entre las dos cosas el mínimo
		 * puede haber crecido —lo calculan sumando los de los hijos, y los que reflúyen
		 * contestan según el ancho que tengan ahora mismo—, y entonces MigLayout se
		 * encuentra con menos alto del que necesita y hace lo único que puede: repartir
		 * el déficit. Aplasta. Así salieron un botón de 19 puntos donde su mínimo era 24
		 * y un precio de 23 donde era 28: <b>por debajo de su mínimo, que es la única
		 * medida que un componente no debería tener que defender</b>.
		 *
		 * <p>
		 * Acotando aquí, ese descuadre se paga donde no duele: sobra contenido por
		 * abajo, que en el peor caso es margen de página, y el siguiente pase de layout
		 * ya ve el mínimo bueno y saca la barra. Lo que no pasa nunca es que un control
		 * se dibuje más pequeño de lo que dijo que necesitaba.
		 */
		@Override
		public void setBounds(int x, int y, int ancho, int alto) {
			super.setBounds(x, y, ancho, Math.max(alto, getMinimumSize().height));
		}
	}
}
