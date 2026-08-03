package fp.project.actihome.ui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;

import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Píldora conmutable: los filtros del catálogo (tipo de alojamiento,
 * amenidades) y las etiquetas de una ficha.
 *
 * <p>
 * Es el único elemento del diseño con forma de píldora — radio de 50px, es
 * decir, semicírculos completos en los extremos. El contraste es deliberado:
 * todo lo demás tiene esquinas casi rectas, así que la forma redonda marca al
 * instante "esto se puede pulsar y tiene dos estados", sin necesidad de
 * explicarlo.
 *
 * <p>
 * Dos estados, según el handoff:
 * <ul>
 * <li><b>Activo</b>: relleno con el acento y texto blanco.</li>
 * <li><b>Inactivo</b>: sin relleno, contorno fino y texto secundario.</li>
 * </ul>
 *
 * <p>
 * Hereda de {@link JToggleButton} y no de {@code JButton} porque el
 * comportamiento de "queda pulsado" ya está resuelto ahí: el estado
 * seleccionado, la accesibilidad y la activación con la barra espaciadora
 * vienen de serie. Reimplementar eso a mano sería trabajo tirado y peor hecho.
 */
public class Chip extends JToggleButton {

	private static final long serialVersionUID = 1L;

	/** Solo informativa: muestra un dato, no responde al ratón. */
	private final boolean soloLectura;

	public Chip(String texto) {
		this(texto, false);
	}

	/** Chip con estado inicial activo. */
	public Chip(String texto, boolean activo) {

		super(texto, activo);
		this.soloLectura = false;
		configurar();
	}

	/**
	 * Chip meramente informativo, para mostrar las amenidades de un alojamiento en
	 * una ficha: tiene el aspecto de chip inactivo pero no se pulsa.
	 */
	public static Chip informativo(String texto) {

		Chip chip = new Chip(texto, false, true);
		chip.setEnabled(false);
		return chip;
	}

	private Chip(String texto, boolean activo, boolean soloLectura) {

		super(texto, activo);
		this.soloLectura = soloLectura;
		configurar();
	}

	private void configurar() {

		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		setOpaque(false);
		setFont(Typography.sansSemiBold(12f));
		setBorder(BorderFactory.createEmptyBorder(Space.XS, Space.MD, Space.XS, Space.MD));

		if (!soloLectura) {
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
	}

	@Override
	public Color getForeground() {

		if (isSelected()) {
			return Theme.onAccent();
		}

		return Theme.mut();
	}

	@Override
	public Dimension getPreferredSize() {

		Dimension d = super.getPreferredSize();
		// Altura mínima cómoda para el ratón, sin que el texto quede apretado.
		return new Dimension(d.width, Math.max(d.height, 30));
	}

	/**
	 * Un chip no se encoge: o cabe entero o no cabe.
	 *
	 * <p>
	 * <b>Faltaba, y es la trampa que el propio manual del proyecto advierte.</b> Un
	 * componente que declara el tamaño preferido pero no el mínimo hereda el mínimo
	 * del <i>look and feel</i>, que para un botón permite recortar el texto con
	 * puntos suspensivos. En cuanto la ventana se quedaba corta —cosa que pasa
	 * antes de lo previsto en un sistema con el escalado al 150 %— los filtros se
	 * leían "Tod…", "Ca…", "Caba…". Un filtro cuyo nombre no se lee no es un filtro.
	 *
	 * <p>
	 * Devolviendo el preferido como mínimo, el chip deja de ser el que cede espacio
	 * y el problema sube al contenedor, que es donde se puede resolver de verdad:
	 * ampliando el mínimo de la ventana.
	 */
	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int ancho = getWidth();
		int alto = getHeight();
		// El radio es el propio alto: así los extremos son semicírculos exactos sea
		// cual sea el tamaño del texto, sin tener que ajustar un número a ojo.
		int radio = alto;

		if (isSelected()) {
			g2.setColor(Theme.acc());
			g2.fillRoundRect(0, 0, ancho, alto, radio, radio);

		} else {
			if (getModel().isRollover() && !soloLectura) {
				g2.setColor(Theme.HAIRLINE);
				g2.fillRoundRect(0, 0, ancho, alto, radio, radio);
			}
			g2.setColor(Theme.FIELD_BORDER);
			g2.drawRoundRect(0, 0, ancho - 1, alto - 1, radio, radio);
		}

		g2.dispose();
		super.paintComponent(g);
	}
}
