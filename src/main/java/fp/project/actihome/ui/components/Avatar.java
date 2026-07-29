package fp.project.actihome.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

import javax.swing.JComponent;

import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Círculo con las iniciales del usuario.
 *
 * <p>
 * Dos variantes, y la diferencia entre ellas es el fondo sobre el que se posan:
 * <ul>
 * <li><b>Contorno</b> — un aro fino de acento con las iniciales en el color del
 * fondo de página. Es la de la cabecera oscura: sobre una barra ya oscura, un
 * círculo relleno sería una mancha más y competiría con el wordmark.</li>
 * <li><b>Relleno</b> — disco de acento con las iniciales en blanco. Para la
 * ficha de perfil, donde el avatar sí es protagonista.</li>
 * </ul>
 *
 * <p>
 * <b>Por qué iniciales y no una foto.</b> No hay flujo de subida de imágenes de
 * usuario y no está previsto: inventarlo sería inventar funcionalidad. Las
 * iniciales en serif resuelven lo mismo —identificar de un vistazo la sesión
 * activa— con lo que ya hay en el modelo, y encajan mejor con la dirección
 * editorial que un icono genérico de persona gris.
 */
public class Avatar extends JComponent {

	private static final long serialVersionUID = 1L;

	private final String iniciales;
	private final boolean relleno;

	private Avatar(String iniciales, int lado, boolean relleno) {

		this.iniciales = iniciales;
		this.relleno = relleno;

		setPreferredSize(new Dimension(lado, lado));
		setMinimumSize(new Dimension(lado, lado));
		setMaximumSize(new Dimension(lado, lado));
	}

	/** Aro de acento con las iniciales claras. Para la cabecera oscura. */
	public static Avatar contorno(String nombre, String apellido, int lado) {
		return new Avatar(iniciales(nombre, apellido), lado, false);
	}

	/** Disco de acento con las iniciales en blanco. Para el perfil. */
	public static Avatar relleno(String nombre, String apellido, int lado) {
		return new Avatar(iniciales(nombre, apellido), lado, true);
	}

	/**
	 * Primera letra del nombre y del apellido, en mayúscula.
	 *
	 * <p>
	 * Tolera nulos y cadenas vacías a propósito. Un avatar es decoración de apoyo:
	 * que reviente porque un usuario tiene el apellido en blanco sería el clásico
	 * fallo desproporcionado, una excepción en toda la pantalla por un adorno.
	 */
	static String iniciales(String nombre, String apellido) {

		StringBuilder sb = new StringBuilder();

		if (nombre != null && !nombre.trim().isEmpty()) {
			sb.append(Character.toUpperCase(nombre.trim().charAt(0)));
		}

		if (apellido != null && !apellido.trim().isEmpty()) {
			sb.append(Character.toUpperCase(apellido.trim().charAt(0)));
		}

		return sb.length() == 0 ? "·" : sb.toString();
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int lado = Math.min(getWidth(), getHeight());
		Color tinta;

		if (relleno) {
			g2.setColor(Theme.acc());
			g2.fill(new Ellipse2D.Double(0, 0, lado, lado));
			tinta = Theme.onAccent();

		} else {
			g2.setColor(Theme.acc());
			g2.draw(new Ellipse2D.Double(0.5, 0.5, lado - 1.0, lado - 1.0));
			tinta = Theme.bg();
		}

		// La tipografía se deriva del tamaño del círculo en lugar de fijarse: el mismo
		// componente sirve para los 26px de la cabecera y los 88px del perfil sin tener
		// que mantener dos escalas en paralelo.
		g2.setFont(Typography.serifMedium(Math.max(9f, lado * 0.42f)));
		g2.setColor(tinta);

		int ancho = g2.getFontMetrics().stringWidth(iniciales);
		int base = (lado + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;

		g2.drawString(iniciales, (lado - ancho) / 2, base);

		g2.dispose();
	}
}
