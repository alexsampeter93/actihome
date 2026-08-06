package fp.project.actihome.ui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;

import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Fábrica de botones del sistema de diseño.
 *
 * <p>
 * Tres niveles de énfasis, y la regla de uso importa tanto como el aspecto:
 * <ul>
 * <li><b>Primario</b> — relleno con el acento. <b>Uno por pantalla.</b> Es la
 * acción que el usuario ha venido a hacer: reservar, publicar, confirmar. Dos
 * botones primarios en la misma pantalla equivalen a ninguno, porque el ojo ya
 * no sabe dónde ir.</li>
 * <li><b>Secundario</b> — solo contorno. Acciones disponibles pero no
 * protagonistas: cancelar, volver, ver detalle.</li>
 * <li><b>Enlace</b> — texto subrayado, sin caja. Acciones terciarias dentro de
 * una lista o un pie: "Ver estancia →".</li>
 * </ul>
 *
 * <p>
 * Todos se pintan a mano en lugar de dejárselo a FlatLaf. No es capricho: el
 * diseño pide un relleno concreto, un radio de 4px y un contorno de una sola
 * línea fina, y hacerlo con propiedades sueltas del Look and Feel acabaría en
 * una lista de excepciones difícil de mantener. Además, pintando podemos leer
 * el color de la estación en cada pasada y así el botón cambia de color solo.
 */
public final class Buttons {

	private Buttons() {
	}

	/** Acción principal de la pantalla. Relleno con el acento. */
	public static JButton primary(String texto, ActionListener accion) {
		return crear(texto, Estilo.PRIMARIO, accion);
	}

	/** Acción secundaria. Solo contorno. */
	public static JButton secondary(String texto, ActionListener accion) {
		return crear(texto, Estilo.SECUNDARIO, accion);
	}

	/** Acción terciaria. Texto subrayado, sin caja. */
	public static JButton link(String texto, ActionListener accion) {
		return crear(texto, Estilo.ENLACE, accion);
	}

	/** Enlace con el color de acento, para acciones destacadas dentro de una ficha. */
	public static JButton linkAccent(String texto, ActionListener accion) {
		return crear(texto, Estilo.ENLACE_ACENTO, accion);
	}

	private static JButton crear(String texto, Estilo estilo, ActionListener accion) {

		ThemedButton boton = new ThemedButton(texto, estilo);

		if (accion != null) {
			boton.addActionListener(accion);
		}

		return boton;
	}

	private enum Estilo {
		PRIMARIO, SECUNDARIO, ENLACE, ENLACE_ACENTO
	}

	private static final class ThemedButton extends JButton {

		private static final long serialVersionUID = 1L;

		private static final int RADIO = 4;

		private final transient Estilo estilo;

		private ThemedButton(String texto, Estilo estilo) {

			super(texto);
			this.estilo = estilo;

			// Se desactiva todo el dibujado que aporta el Look and Feel: el fondo, el
			// borde y el rectángulo de foco. A partir de aquí pintamos nosotros.
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			boolean esEnlace = estilo == Estilo.ENLACE || estilo == Estilo.ENLACE_ACENTO;

			setFont(esEnlace ? Typography.sansSemiBold(Typography.BODY_SM) : Typography.sansSemiBold(13f));
			setBorder(esEnlace
					? BorderFactory.createEmptyBorder(Space.XXS, 0, Space.XXS, 0)
					: BorderFactory.createEmptyBorder(Space.SM, Space.XL, Space.SM, Space.XL));
		}

		@Override
		public Color getForeground() {

			if (estilo == null) {
				return super.getForeground();
			}

			switch (estilo) {
			case PRIMARIO:
				return Theme.onAccent();
			case ENLACE_ACENTO:
				return Theme.accText();
			default:
				return Theme.txt();
			}
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int ancho = getWidth();
			int alto = getHeight();
			boolean pulsado = getModel().isArmed() && getModel().isPressed();
			boolean encima = getModel().isRollover();

			switch (estilo) {
			case PRIMARIO:
				g2.setColor(ajustar(Theme.acc(), pulsado ? -28 : encima ? -14 : 0));
				g2.fillRoundRect(0, 0, ancho, alto, RADIO, RADIO);
				break;

			case SECUNDARIO:
				if (encima) {
					g2.setColor(Theme.HAIRLINE);
					g2.fillRoundRect(0, 0, ancho, alto, RADIO, RADIO);
				}
				g2.setColor(Theme.FIELD_BORDER);
				g2.drawRoundRect(0, 0, ancho - 1, alto - 1, RADIO, RADIO);
				break;

			default:
				// Los enlaces se subrayan con una línea a la altura de la base del texto.
				//
				// **La línea empieza donde empieza el texto, no en x=0**, y esa era la
				// diferencia que se veía fea. Un JButton centra su etiqueta, así que en cuanto
				// el layout le da más ancho del que el texto necesita —una fila con otros
				// elementos, una columna con "grow"— el texto se va al centro y el subrayado
				// se quedaba pegado al borde izquierdo: una raya suelta a la izquierda de la
				// palabra, que es justo lo que se leía como un fallo de pintado.
				int base = getBaseline(ancho, alto);
				if (base > 0) {
					int anchoTexto = getTextoAncho();
					g2.setColor(getForeground());
					g2.fillRect(Math.max(0, (ancho - anchoTexto) / 2), base + 2, anchoTexto, 1);
				}
				break;
			}

			g2.dispose();
			super.paintComponent(g);
		}

		private int getTextoAncho() {
			return getFontMetrics(getFont()).stringWidth(getText());
		}

		/**
		 * Aclara u oscurece un color. Se usa para los estados de ratón encima y
		 * pulsado, en lugar de definir tres colores por estación: así el sistema de
		 * tokens no crece y los estados salen solos en las cuatro paletas.
		 */
		private static Color ajustar(Color color, int delta) {

			return new Color(
					Math.max(0, Math.min(255, color.getRed() + delta)),
					Math.max(0, Math.min(255, color.getGreen() + delta)),
					Math.max(0, Math.min(255, color.getBlue() + delta)),
					color.getAlpha());
		}
	}
}
