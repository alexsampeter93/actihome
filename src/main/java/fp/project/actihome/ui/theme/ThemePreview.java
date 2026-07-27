package fp.project.actihome.ui.theme;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

/**
 * Ventana de la guía de estilo viva.
 *
 * <p>
 * Muestra {@link StyleGuidePanel} y permite cambiar de estación para ver cómo
 * responde todo el sistema. Se arranca sin Spring y sin base de datos:
 *
 * <pre>
 * .\mvnw.cmd compile exec:java
 * </pre>
 *
 * <p>
 * No forma parte de la aplicación: es una herramienta de desarrollo, y sirve
 * para tres cosas.
 * <ul>
 * <li><b>Verificar</b> que un cambio en el sistema de diseño se propaga como se
 * espera, sin arrancar la aplicación entera ni la base de datos.</li>
 * <li><b>Decidir</b> con los ojos en vez de con la imaginación: los colores
 * sobre una tabla de Markdown no se parecen a los colores en pantalla.</li>
 * <li><b>Documentar</b> qué piezas hay disponibles cuando haya que montar una
 * pantalla nueva.</li>
 * </ul>
 *
 * <p>
 * Es la única pantalla del proyecto a la que se le permite tener scroll de
 * página: la regla de "cada pantalla cabe en la ventana" rige para la
 * aplicación, no para el taller.
 */
public class ThemePreview extends JFrame {

	private static final long serialVersionUID = 1L;

	private final StyleGuidePanel guia = new StyleGuidePanel(true);

	public ThemePreview() {

		setTitle("ActiHome — Guía de estilo");
		setSize(1180, 900);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		JScrollPane scroll = new JScrollPane(guia);
		scroll.setBorder(null);
		scroll.getVerticalScrollBar().setUnitIncrement(24);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		add(scroll, BorderLayout.CENTER);

		// Al cambiar de estación se reconstruye el contenido. Reconstruir es más caro
		// que repintar, pero en una herramienta de desarrollo la simplicidad gana: así
		// no hay que mantener una lista de qué componente depende de qué token.
		Theme.alCambiar(estacion -> guia.construir());
	}

	public static void main(String[] args) {

		ActiHomeTheme.install();
		EventQueue.invokeLater(() -> new ThemePreview().setVisible(true));
	}
}
