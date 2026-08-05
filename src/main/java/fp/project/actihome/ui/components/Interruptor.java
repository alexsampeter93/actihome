package fp.project.actihome.ui.components;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

import fp.project.actihome.ui.theme.Theme;

/**
 * Interruptor de encendido y apagado: la píldora que desliza un disco.
 *
 * <p>
 * <b>Por qué no basta con un {@code JCheckBox}.</b> Una casilla dice «marca esto
 * y luego pulsa Guardar»; un interruptor dice «esto está encendido». Son dos
 * gramáticas distintas y la diferencia importa en una pantalla de preferencias,
 * donde lo que se manipula no son datos que rellenar sino estados que activar.
 * Es lo que pide el handoff para las partículas decorativas, y es también lo que
 * hace cualquier panel de ajustes moderno.
 *
 * <p>
 * <b>Sigue a la estación sin suscribirse a nada.</b> El color se resuelve dentro
 * de {@code paintComponent} leyendo {@link Theme}, que es la convención del
 * sistema de diseño: guardarlo en un campo obligaría a darse de alta y de baja
 * como oyente, y este componente puede vivir dentro de una fila que se
 * reconstruye en cada visita a la pantalla.
 *
 * <p>
 * Es accesible con teclado ({@link Foco#activable}) porque extiende
 * {@code JComponent} y no un botón de Swing: sin eso no sería alcanzable con
 * Tab ni activable con Espacio, que es exactamente el fallo que la Fase 7
 * encontró en los otros cuatro controles dibujados a mano del proyecto.
 */
public class Interruptor extends JComponent {

	private static final long serialVersionUID = 1L;

	private static final int ANCHO = 44;
	private static final int ALTO = 24;

	/** Holgura entre el disco y el borde de la píldora. */
	private static final int MARGEN = 3;

	private boolean encendido;

	public Interruptor() {

		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setPreferredSize(new Dimension(ANCHO, ALTO));

		// Preferido y mínimo a la vez. Un componente propio que solo declara el
		// preferido anuncia un mínimo de cero, y el layout lo aplasta a nada en
		// cuanto falta sitio, sin dar ningún error (trampa de Swing documentada en
		// CLAUDE.md §6).
		setMinimumSize(new Dimension(ANCHO, ALTO));

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				setEncendido(!encendido);
			}
		});

		Foco.activable(this, () -> setEncendido(!encendido));
	}

	public boolean isEncendido() {
		return encendido;
	}

	public void setEncendido(boolean encendido) {
		this.encendido = encendido;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int alto = Math.min(getHeight(), ALTO);
		int ancho = Math.min(getWidth(), ANCHO);

		// Apagado, el carril es la misma línea tenue que separa cualquier cosa en
		// esta interfaz; encendido, el acento de la estación. Que el estado "off" no
		// tenga color propio es deliberado: lo que debe llamar la atención es lo que
		// está activo.
		g2.setColor(encendido ? Theme.acc() : Theme.HAIRLINE);
		g2.fillRoundRect(0, 0, ancho, alto, alto, alto);

		if (!encendido) {
			g2.setColor(Theme.FIELD_BORDER);
			g2.drawRoundRect(0, 0, ancho - 1, alto - 1, alto, alto);
		}

		int diametro = alto - MARGEN * 2;
		int x = encendido ? ancho - MARGEN - diametro : MARGEN;

		g2.setColor(java.awt.Color.WHITE);
		g2.fillOval(x, MARGEN, diametro, diametro);

		Foco.pintarAnillo(g2, this);

		g2.dispose();
	}
}
