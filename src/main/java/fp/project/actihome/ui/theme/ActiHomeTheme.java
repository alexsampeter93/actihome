package fp.project.actihome.ui.theme;

import java.awt.Insets;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLightLaf;

/**
 * Punto único de instalación del aspecto visual de ActiHome.
 *
 * <p>
 * En Swing, el <em>Look and Feel</em> (L&amp;F) es la implementación que decide
 * cómo se dibuja cada control: botones, campos, tablas, barras de scroll. Java
 * trae por defecto "Metal", el aspecto gris de los años 2000. Aquí se sustituye
 * por FlatLaf, que aporta un dibujado plano y moderno, soporte de pantallas
 * HiDPI y un catálogo de propiedades para afinar el estilo.
 *
 * <p>
 * <b>Importante:</b> {@link #install()} debe llamarse <b>antes</b> de crear
 * cualquier ventana. En esta aplicación las ventanas son beans de Spring y
 * varias de ellas no son {@code @Lazy}, así que Spring las construye al
 * levantar el contexto: por eso la llamada va en {@code main()} antes de
 * {@code SpringApplication.run(...)}. Un componente ya construido conserva el
 * L&amp;F que hubiera en ese momento.
 *
 * <p>
 * Esta clase es el embrión del sistema de diseño: en la Fase 1 se le añadirán
 * las paletas estacionales y la escala tipográfica. De momento solo fija la base
 * común.
 */
public final class ActiHomeTheme {

	/** Clase de utilidad: no se instancia. */
	private ActiHomeTheme() {
	}

	/**
	 * Instala el Look and Feel y los ajustes globales de estilo.
	 */
	public static void install() {

		// Suavizado de fuentes. Sin esto el texto se ve dentado en algunos equipos:
		// se le pide a AWT que respete la configuración de suavizado del sistema.
		System.setProperty("awt.useSystemAAFontSettings", "on");
		System.setProperty("swing.aatext", "true");

		// Instala FlatLaf en su variante clara. La dirección visual del rediseño es
		// editorial sobre fondos crema, así que se parte del tema claro; el tema
		// oscuro no está en el alcance actual.
		FlatLightLaf.setup();

		// --- Ajustes globales alineados con la dirección editorial ---
		//
		// El handoff pide "estética afilada": radios de 2-4px en superficies, nada
		// de esquinas muy redondeadas (que es justo lo que da el aire de plantilla
		// SaaS genérica). Estas claves son las que FlatLaf usa para el radio de
		// cada familia de componentes; su valor por defecto es 5-6.
		UIManager.put("Button.arc", 4);
		UIManager.put("Component.arc", 4);
		UIManager.put("CheckBox.arc", 3);
		UIManager.put("ProgressBar.arc", 4);

		// Anillo de foco fino: el foco debe verse, pero sin engordar el control.
		UIManager.put("Component.focusWidth", 1);
		UIManager.put("Component.innerFocusWidth", 1);

		// Barras de scroll discretas, sin fondo ni botones de flecha: en una
		// interfaz editorial la barra no debe competir con el contenido.
		UIManager.put("ScrollBar.showButtons", false);
		UIManager.put("ScrollBar.thumbArc", 999);
		UIManager.put("ScrollBar.thumbInsets", new Insets(2, 4, 2, 4));
		UIManager.put("ScrollBar.width", 12);

		// Separación interna por defecto algo más generosa que la de FlatLaf: el
		// diseño se apoya en el aire como recurso principal.
		UIManager.put("Button.margin", new Insets(8, 18, 8, 18));
		UIManager.put("TextComponent.arc", 4);
	}
}
