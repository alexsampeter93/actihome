package fp.project.actihome.ui.components;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.theme.Animacion;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;

/**
 * Superficie blanca sobre la que se apoya el contenido.
 *
 * <p>
 * Es la pieza más repetida del diseño y la que más define el carácter. Dos
 * decisiones deliberadas, ambas contrarias a lo que hace la mayoría de las
 * interfaces actuales:
 *
 * <ul>
 * <li><b>Sin sombra.</b> La separación con el fondo se consigue con una línea
 * de un píxel muy tenue (<em>hairline</em>), no con una sombra difusa. La
 * sombra suave es el recurso más característico de la estética de plantilla que
 * el handoff pide evitar; la línea fina es el recurso de la maquetación
 * editorial impresa, que es la referencia de este proyecto.</li>
 * <li><b>Radio pequeño</b> (4px). Las esquinas muy redondeadas suavizan y
 * amabilizan; este diseño busca lo contrario, precisión.</li>
 * </ul>
 *
 * <p>
 * Como el resto de componentes, lee el color en cada pintado, así que sigue a
 * la estación activa sin suscribirse a nada.
 */
public class Card extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final int RADIO = 4;

	/** Cuánto sube una tarjeta interactiva al pasar el ratón, en puntos. */
	private static final int SUBIDA = 2;

	/**
	 * La hairline cuando la tarjeta está elevada.
	 *
	 * <p>
	 * Es el mismo negro de {@link Theme#HAIRLINE} con más opacidad —del 12 % al
	 * 28 %— y no un color distinto. Sustituirlo por un tono de acento habría metido
	 * color donde el sistema no lo pide; lo que cambia al levantar una hoja de papel
	 * no es el color de su canto, es cuánto se ve.
	 */
	private static final java.awt.Color HAIRLINE_ELEVADA = new java.awt.Color(0, 0, 0, 71);

	private final boolean conBorde;

	/** Cuánto está elevada, de 0 a 1. Ver {@link #interactiva()}. */
	private transient double elevacion;

	private transient javax.swing.Timer animacion;

	/** Tarjeta con el relleno estándar y disposición en columna. */
	public Card() {
		this(new MigLayout("wrap 1, " + Space.insets(Space.XXL), "[grow,fill]", ""), true);
	}

	/** Tarjeta con una disposición concreta. */
	public Card(LayoutManager layout) {
		this(layout, true);
	}

	/**
	 * @param conBorde si es {@code false} la superficie es blanca pero sin
	 *                 hairline: útil cuando la tarjeta va dentro de otra o cuando
	 *                 la separación ya la da un separador propio
	 */
	public Card(LayoutManager layout, boolean conBorde) {

		super(layout);
		this.conBorde = conBorde;

		// El panel no se rellena por el sistema: lo pintamos nosotros para poder
		// redondear las esquinas y aplicar antialiasing.
		setOpaque(false);
	}

	/**
	 * Hace que la tarjeta reaccione al ratón: se eleva.
	 *
	 * <p>
	 * <b>Es opcional a propósito y no el comportamiento por omisión.</b> Una tarjeta
	 * que se mueve está diciendo "soy pulsable", y en esta aplicación muchas no lo
	 * son —la de reserva del detalle, la del anfitrión, los bloques de ajustes—.
	 * Animar todas convertiría la pista en ruido y, peor, prometería un clic que no
	 * existe. Solo la piden las fichas del catálogo, que sí llevan a algún sitio.
	 *
	 * <p>
	 * <b>Elevar sin sombra.</b> El recurso natural sería una sombra difusa que crece,
	 * y está descartado: es <em>la</em> firma de la estética de plantilla que este
	 * sistema evita, y contradice la regla que sostiene toda la tarjeta —separación
	 * por línea fina, no por bruma—. La elevación se consigue con dos gestos que
	 * vienen del papel y no de la pantalla: la tarjeta <b>sube dos puntos</b> y su
	 * hairline <b>se oscurece</b>, como una hoja levantada de una pila que deja ver
	 * su propio canto.
	 */
	public Card interactiva() {

		setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

		addMouseListener(new java.awt.event.MouseAdapter() {

			@Override
			public void mouseEntered(java.awt.event.MouseEvent e) {
				animarHacia(1);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e) {
				animarHacia(0);
			}
		});

		return this;
	}

	/**
	 * Arranca la elevación hacia el destino, partiendo de donde esté ahora.
	 *
	 * <p>
	 * Partir del valor actual y no de cero es lo que evita el tirón cuando el ratón
	 * entra y sale deprisa: la animación de vuelta continúa la de ida en lugar de
	 * saltar al final para empezar de nuevo.
	 */
	private void animarHacia(double destino) {

		Animacion.cancelar(animacion);
		animacion = Animacion.animar(this, elevacion, destino, Animacion.CONTROL, v -> {

			elevacion = v;

			// Se repinta el PADRE y no solo la tarjeta. Al subir dos puntos queda al
			// descubierto la franja que ocupaba antes por abajo, y esa franja ya no es
			// suya: si no se repinta lo de debajo, la tarjeta deja un rastro blanco
			// pegado al borde inferior. Es el mismo cuidado que ya obligó a repintar la
			// fila entera en las pestañas de la cabecera.
			if (getParent() != null) {
				getParent().repaint(getX(), getY(), getWidth(), getHeight() + SUBIDA);
			}
		});
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// El desplazamiento es NEGATIVO en el eje Y: sube. El alto se recorta lo mismo
		// para que la tarjeta no invada por arriba la que tiene encima.
		int subida = (int) Math.round(SUBIDA * elevacion);
		int alto = getHeight() - subida;

		g2.setColor(Theme.SURFACE);
		g2.fillRoundRect(0, -subida, getWidth(), alto + subida, RADIO, RADIO);

		if (conBorde) {
			g2.setColor(Animacion.mezclar(Theme.HAIRLINE, HAIRLINE_ELEVADA, elevacion));
			g2.drawRoundRect(0, -subida, getWidth() - 1, alto + subida - 1, RADIO, RADIO);
		}

		g2.dispose();
		super.paintComponent(g);
	}
}
