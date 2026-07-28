package fp.project.actihome.ui.components;

import java.awt.Graphics;
import java.awt.LayoutManager;

import javax.swing.JPanel;

import fp.project.actihome.ui.theme.Theme;

/**
 * El lienzo de una pantalla: un panel cuyo fondo es siempre el de la estación
 * activa.
 *
 * <p>
 * <b>Por qué no basta con {@code setBackground(Theme.bg())}.</b> Ese método
 * <em>guarda</em> el color, y lo guarda una sola vez, cuando se construye la
 * pantalla. Como los frames son beans singleton de Spring, se construyen una vez
 * y viven toda la sesión: si después el usuario cambia de estación, la barra
 * oscura, el acento, los textos y hasta Olaz cambian —porque todos resuelven su
 * color al pintar— pero el fondo de página se queda con el de la estación que
 * hubiera en el arranque.
 *
 * <p>
 * El fallo apareció exactamente así: en las capturas de invierno todo era lila
 * salvo el fondo, que seguía siendo el crema de primavera. Y es de los peores de
 * detectar, porque el fondo es lo último que uno mira.
 *
 * <p>
 * La regla del sistema, otra vez: <b>no se guarda un color, se resuelve al
 * pintar.</b> Aquí eso significa rellenar en {@code paintComponent} en lugar de
 * confiar en la propiedad {@code background}.
 */
public class Page extends JPanel {

	private static final long serialVersionUID = 1L;

	public Page(LayoutManager layout) {

		super(layout);

		// Opaco sí —queremos que pinte fondo—, pero el color no sale de la propiedad
		// background sino de paintComponent.
		setOpaque(true);
	}

	@Override
	protected void paintComponent(Graphics g) {

		g.setColor(Theme.bg());
		g.fillRect(0, 0, getWidth(), getHeight());
	}
}
