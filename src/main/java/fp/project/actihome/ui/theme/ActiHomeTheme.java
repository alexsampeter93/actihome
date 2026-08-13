package fp.project.actihome.ui.theme;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLaf;
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
 * Esta clase es el pegamento entre el sistema de diseño ({@link Season},
 * {@link Theme}, {@link Typography}) y FlatLaf.
 */
public final class ActiHomeTheme {

	/** Clase de utilidad: no se instancia. */
	private ActiHomeTheme() {
	}

	/**
	 * Instala el Look and Feel, la tipografía y la paleta de la estación activa.
	 */
	public static void install() {

		// Suavizado de fuentes. Sin esto el texto se ve dentado en algunos equipos:
		// se le pide a AWT que respete la configuración de suavizado del sistema.
		System.setProperty("awt.useSystemAAFontSettings", "on");
		System.setProperty("swing.aatext", "true");

		// Las fuentes deben estar cargadas antes de fijarlas como fuente por defecto.
		Typography.register();

		// Recupera estación, idioma y partículas de la última sesión. Va antes de
		// aplicar la paleta a propósito: si se restaurara después, FlatLaf ya habría
		// derivado sus decenas de colores del acento equivocado y habría que
		// recalcularlo todo otra vez, con un parpadeo visible en el arranque.
		Preferencias.restaurar();

		// Pinta la aplicación con la estación activa: la recién restaurada si había
		// algo guardado, o la que toque según la fecha de hoy si es la primera vez.
		aplicarPaleta(Theme.estacion());

		// A partir de aquí, cualquier cambio de estación repinta la aplicación entera.
		// Ver Theme: quien quiere enterarse se apunta; el emisor no conoce a nadie.
		Theme.alCambiar(ActiHomeTheme::aplicarPaleta);

		quitarFocoAlPulsarFuera();
	}

	/**
	 * Pulsar en cualquier sitio inerte de la pantalla suelta el foco del campo
	 * que lo tuviera — el cursor de escritura deja de parpadear y el marco del
	 * buscador vuelve a su forma de reposo.
	 *
	 * <p>
	 * <b>El problema que resuelve no es de esta aplicación, es de Swing.</b> Un
	 * clic solo mueve el foco de teclado si el componente pulsado lo pide por su
	 * cuenta, y eso lo decide cada Look and Feel dentro de su propio
	 * {@code MouseListener}: un botón sí, un campo de texto sí, un
	 * {@code JPanel} no. Pulsar sobre el fondo de una tarjeta, un margen o un
	 * titular no hace absolutamente nada, así que el campo que tuviera el foco
	 * lo sigue teniendo. Con {@link fp.project.actihome.ui.components.SearchField},
	 * cuyo marco de escuadras se cierra al enfocarse, eso se ve: el buscador se
	 * queda con pinta de activo hasta que se pulsa otro control.
	 *
	 * <p>
	 * <b>Y la pregunta obvia —"¿el componente pulsado es enfocable?"— no
	 * sirve.</b> Fue el primer intento y no disparó ni una vez: en Swing
	 * {@code isFocusable()} viene a {@code true} de fábrica en casi todo,
	 * paneles incluidos (lo que impide que el tabulador se pare en ellos es la
	 * política de recorrido, no esa bandera). Preguntarlo devuelve "sí" para el
	 * fondo de cualquier tarjeta, así que la condición nunca se cumplía.
	 *
	 * <p>
	 * <b>Lo que sí se puede preguntar es el resultado.</b> Se anota quién tiene
	 * el foco al empezar el clic y se vuelve a mirar cuando el clic ya se ha
	 * repartido entero: si nadie se lo ha quedado, es que se ha pulsado algo
	 * inerte y el foco se suelta. Funciona sin saber qué componentes piden foco
	 * y cuáles no — que es justo lo que no hay forma de consultar. El
	 * {@code invokeLater} es lo que espera a que el reparto termine; los
	 * traspasos de foco dentro de una misma ventana son síncronos, así que para
	 * entonces el nuevo dueño ya está anotado.
	 *
	 * <p>
	 * Un solo oyente global y no un {@code MouseListener} por cada panel vacío:
	 * cubrirlos a mano habría significado acordarse en cada pantalla nueva, y
	 * olvidarlo en una sería indistinguible de "aquí funciona así".
	 */
	private static void quitarFocoAlPulsarFuera() {

		Toolkit.getDefaultToolkit().addAWTEventListener(evento -> {

			if (evento.getID() != MouseEvent.MOUSE_PRESSED) {
				return;
			}

			Component conElFoco = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

			if (conElFoco == null) {
				return;
			}

			Component pulsado = ((MouseEvent) evento).getComponent();

			// El clic ha caído dentro del propio componente enfocado: colocar el cursor
			// en mitad de un texto ya escrito no debe soltarlo.
			if (pulsado != null && SwingUtilities.isDescendingFrom(pulsado, conElFoco)) {
				return;
			}

			// Un desplegable abierto es un caso aparte: su lista vive en un
			// JPopupMenu y el foco se queda en el combo mientras se elige. Soltarlo
			// aquí cerraría la lista a mitad de la elección.
			if (pulsado != null && SwingUtilities.getAncestorOfClass(JPopupMenu.class, pulsado) != null) {
				return;
			}

			SwingUtilities.invokeLater(() -> {

				KeyboardFocusManager gestor = KeyboardFocusManager.getCurrentKeyboardFocusManager();

				if (gestor.getFocusOwner() == conElFoco) {
					gestor.clearFocusOwner();
				}
			});

		}, AWTEvent.MOUSE_EVENT_MASK);
	}

	/**
	 * Recalcula el Look and Feel con los colores de una estación y repinta todas las
	 * ventanas abiertas.
	 *
	 * <p>
	 * El orden de las tres operaciones no es negociable y costó entenderlo:
	 * <ol>
	 * <li><b>Variables globales primero.</b> FlatLaf no se limita a usar el acento
	 * donde se lo pides: a partir de él <em>deriva</em> decenas de colores (botón
	 * pulsado, selección de tabla, anillo de foco, borde deshabilitado...). Ese
	 * cálculo ocurre al inicializar el tema, así que las variables tienen que estar
	 * puestas antes.</li>
	 * <li><b>Reinstalar el tema.</b> Es lo que dispara ese recálculo.</li>
	 * <li><b>Volver a aplicar nuestros ajustes.</b> Reinstalar el tema
	 * <em>reinicia</em> la tabla de propiedades de Swing, así que los radios, la
	 * fuente por defecto y las barras de scroll se perderían si se pusieran
	 * antes.</li>
	 * </ol>
	 */
	static void aplicarPaleta(Season estacion) {

		Map<String, String> variables = new HashMap<>();
		variables.put("@accentColor", Season.hex(estacion.acc()));
		variables.put("@background", Season.hex(estacion.bg()));
		variables.put("@foreground", Season.hex(estacion.txt()));

		FlatLaf.setGlobalExtraDefaults(variables);
		FlatLightLaf.setup();
		aplicarAjustesGlobales();

		// Recorre las ventanas vivas y las obliga a repintarse con la tabla nueva.
		// En el primer arranque no hay ninguna y la llamada no hace nada.
		FlatLaf.updateUI();
	}

	/**
	 * Ajustes de forma comunes a las cuatro estaciones.
	 *
	 * <p>
	 * Se llaman en cada cambio de paleta porque reinstalar el tema los borra.
	 */
	private static void aplicarAjustesGlobales() {

		// Fuente base de toda la interfaz. FlatLaf usa "defaultFont" para cualquier
		// componente que no defina la suya, así que esta línea cambia la tipografía
		// de la aplicación entera.
		UIManager.put("defaultFont", Typography.sans(14f));

		// El handoff pide "estética afilada": radios de 2-4px. Los valores por defecto
		// de FlatLaf son 5-6, y los redondeos generosos de 8-12px son justo lo que da
		// el aire de plantilla SaaS genérica que queremos evitar.
		UIManager.put("Button.arc", 4);
		UIManager.put("Component.arc", 4);
		UIManager.put("CheckBox.arc", 3);
		UIManager.put("ProgressBar.arc", 4);
		UIManager.put("TextComponent.arc", 4);

		// Anillo de foco fino: el foco debe verse, pero sin engordar el control.
		UIManager.put("Component.focusWidth", 1);
		UIManager.put("Component.innerFocusWidth", 1);

		// Barras de scroll discretas, sin fondo ni botones de flecha: en una interfaz
		// editorial la barra no debe competir con el contenido.
		UIManager.put("ScrollBar.showButtons", false);
		UIManager.put("ScrollBar.thumbArc", 999);
		UIManager.put("ScrollBar.thumbInsets", new Insets(2, 4, 2, 4));
		UIManager.put("ScrollBar.width", 12);

		// Separación interna algo más generosa que la de FlatLaf: el diseño se apoya
		// en el aire como recurso principal.
		UIManager.put("Button.margin", new Insets(8, 18, 8, 18));
	}
}
