package fp.project.actihome.ui.components;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

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

	private final boolean conBorde;

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

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(Theme.SURFACE);
		g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIO, RADIO);

		if (conBorde) {
			g2.setColor(Theme.HAIRLINE);
			g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIO, RADIO);
		}

		g2.dispose();
		super.paintComponent(g);
	}
}
