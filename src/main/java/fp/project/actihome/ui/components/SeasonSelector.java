package fp.project.actihome.ui.components;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.theme.Season;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Selector de estación: la pieza que reambienta la aplicación entera.
 *
 * <p>
 * Cuatro pestañas en versalita —Primavera, Verano, Otoño, Invierno— bajo la
 * etiqueta "Viajar en". La activa va en el color de acento y subrayada; las
 * demás en el color secundario.
 *
 * <p>
 * <b>Es el control más importante del rediseño</b> y no porque haga mucho, sino
 * por lo que provoca: un clic cambia los siete colores del sistema, la
 * ilustración de Olaz y el glifo de la cabecera, de golpe y en toda la
 * aplicación. Es el argumento visual de todo el proyecto reducido a un gesto.
 *
 * <p>
 * <b>Cómo se propaga el cambio.</b> Este componente no conoce a nadie: se limita
 * a llamar a {@link Theme#cambiarA(Season)}. A partir de ahí, quien se haya
 * apuntado se entera —incluido el tema de FlatLaf, que recalcula la paleta de
 * todos los controles estándar— y los componentes propios, que resuelven su
 * color en cada pintado, salen repintados con el color nuevo sin haberse
 * suscrito a nada. Es el patrón observador aplicado con cabeza: <b>el emisor no
 * sabe cuántos receptores hay ni quiénes son</b>.
 *
 * <p>
 * Sin subrayado en las inactivas no bastaría con el color: el subrayado da un
 * segundo canal —posición y peso, no solo tinta— para que la estación activa se
 * reconozca también sin distinguir bien los colores.
 */
public class SeasonSelector extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Separación entre pestañas, del handoff. */
	private static final int GAP = 22;

	public SeasonSelector() {
		this(false);
	}

	/**
	 * @param sobreCabecera versión compacta para la barra de navegación oscura: sin
	 *                      el rótulo "Viajar en" y con los colores del fondo oscuro.
	 *                      Vive ahí desde que se comprobó que el selector solo
	 *                      existía en el catálogo, así que estando en el detalle o en
	 *                      un formulario <b>no había forma de cambiar de estación</b>
	 *                      — siendo la característica más distintiva de la
	 *                      aplicación. Subirlo a la cabecera lo hace alcanzable desde
	 *                      las diecisiete pantallas y de paso devuelve al catálogo el
	 *                      alto que ocupaba
	 */
	public SeasonSelector(boolean sobreCabecera) {

		super(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				sobreCabecera ? "[]" : "[]" + Space.SM + "[]"));
		setOpaque(false);

		if (!sobreCabecera) {
			add(alinearDerecha(Labels.caps("Viajar en")));
		}

		int gap = sobreCabecera ? Space.MD : GAP;

		JPanel pestanas = new JPanel(new MigLayout(Space.insets(0), "push[]" + gap + "[]" + gap + "[]" + gap + "[]", ""));
		pestanas.setOpaque(false);

		for (Season estacion : Season.values()) {
			pestanas.add(new Pestana(estacion, sobreCabecera));
		}

		add(pestanas);
	}

	/**
	 * El bloque del hero está alineado a la derecha, y una etiqueta dentro de una
	 * celda "fill" se dibuja pegada a la izquierda por defecto.
	 */
	private static JPanel alinearDerecha(JComponent componente) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "push[]", ""));
		fila.setOpaque(false);
		fila.add(componente);
		return fila;
	}

	/** Una de las cuatro pestañas. */
	private static class Pestana extends JComponent {

		private static final long serialVersionUID = 1L;

		private static final int GROSOR_SUBRAYADO = 2;
		private static final int AIRE_BAJO_TEXTO = 7;

		private final transient Season estacion;
		private final boolean sobreCabecera;
		private boolean encima;

		Pestana(Season estacion, boolean sobreCabecera) {

			this.estacion = estacion;
			this.sobreCabecera = sobreCabecera;
			setFont(Typography.label(sobreCabecera ? 10f : 12f));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText(estacion.etiqueta());

			addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					Theme.cambiarA(estacion);
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					encima = true;
					repintarFila();
				}

				@Override
				public void mouseExited(MouseEvent e) {
					encima = false;
					repintarFila();
				}
			});
		}

		/**
		 * Repinta la fila de las cuatro pestañas, no solo esta.
		 *
		 * <p>
		 * Ver la nota gemela en {@code HeaderPanel.Destino.repintarFila()}: repintar
		 * solo el componente bajo el ratón deja el redondeo del rectángulo sucio a
		 * merced de en qué escalado de monitor esté en ese instante la ventana, y
		 * puede no coincidir con el del último reparto completo. Repintando el
		 * contenedor de las cuatro pestañas a la vez, el rectángulo es siempre el
		 * mismo.
		 */
		private void repintarFila() {

			Container fila = getParent();
			(fila != null ? fila : this).repaint();
		}

		private boolean esActiva() {
			return Theme.estacion() == estacion;
		}

		@Override
		public Dimension getPreferredSize() {

			int ancho = getFontMetrics(getFont()).stringWidth(texto());
			int alto = getFontMetrics(getFont()).getHeight() + AIRE_BAJO_TEXTO + GROSOR_SUBRAYADO;

			return new Dimension(ancho + 2, alto);
		}

		/**
		 * El mínimo es el preferido: este componente no se puede encoger.
		 *
		 * <p>
		 * <b>Y omitirlo cuesta caro.</b> Un {@code JComponent} que solo declara el
		 * tamaño preferido informa de un mínimo de <b>cero</b>, porque la
		 * implementación por defecto devuelve el tamaño actual —que antes del primer
		 * reparto es 0×0—. Mientras sobra sitio no se nota nada; en cuanto la ventana
		 * se queda corta, el gestor de layout reparte quitándole espacio a quien dice
		 * poder cederlo, y estas pestañas se aplastaban a dos píxeles: quedaba
		 * únicamente el subrayado de la estación activa, flotando sin texto.
		 *
		 * <p>
		 * Es un fallo que no da error y que solo aparece a partir de cierto tamaño de
		 * ventana. La regla, para todo componente propio: <b>si defines el preferido,
		 * define también el mínimo.</b>
		 */
		@Override
		public Dimension getMinimumSize() {
			return getPreferredSize();
		}

		private String texto() {
			return estacion.nombre().toUpperCase();
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			Typography.hintsDeTextoEstable(g2);

			boolean activa = esActiva();
			// Sobre la cabecera oscura no valen ni acc() ni mut(): el primero puede ser el
			// amarillo de verano, que contra el marrón oscuro pierde fuerza, y el segundo
			// está pensado para fondo claro y ahí queda casi ilegible. Se usa el blanco y
			// su versión atenuada, que es lo que ya hace el resto de la barra.
			Color tinta;

			if (sobreCabecera) {
				tinta = activa || encima ? Color.WHITE : Theme.mutSobreOscuro();
			} else {
				tinta = activa ? Theme.acc() : encima ? Theme.txt() : Theme.mut();
			}

			g2.setFont(getFont());
			g2.setColor(tinta);

			int base = g2.getFontMetrics().getAscent();
			g2.drawString(texto(), 0, base);

			if (activa) {
				g2.setColor(Theme.acc());
				g2.fillRect(0, getHeight() - GROSOR_SUBRAYADO, getWidth(), GROSOR_SUBRAYADO);
			}

			g2.dispose();
		}
	}
}
