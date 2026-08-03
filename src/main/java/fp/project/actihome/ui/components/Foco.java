package fp.project.actihome.ui.components;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;

import fp.project.actihome.ui.theme.Theme;

/**
 * Hace que un control pintado a mano sea alcanzable y activable con teclado.
 *
 * <p>
 * <b>El fallo que corrige, y por qué se repetía cuatro veces.</b> Los enlaces de
 * la cabecera ({@code HeaderPanel.Destino}), las pestañas de estación
 * ({@code SeasonSelector.Pestana}), el orden del catálogo
 * ({@code OptionLinks.Opcion}) y el conmutador de vista
 * ({@code Segmented.Segmento}) tienen algo en común: ninguno extiende
 * {@code AbstractButton}, así que ninguno hereda de Swing la accesibilidad de
 * teclado que trae de serie un botón — alcanzarlo con Tab, activarlo con
 * Espacio o Intro, ver dónde está el foco. Solo escuchan al ratón. Medido con
 * un recorrido manual: con solo teclado, <b>no hay forma de cambiar de
 * estación ni de navegar por la cabecera</b>, que es justo la función más
 * repetida de toda la aplicación.
 *
 * <p>
 * La alternativa habría sido convertir los cuatro en {@link javax.swing.JToggleButton}
 * o {@link javax.swing.AbstractButton}, como ya hace {@link Chip} — pero los
 * cuatro tienen su propia lógica de pintado y de grupo (excluyente, en fila,
 * con subrayado) que no encaja con la forma en que {@code AbstractButton}
 * espera pintarse. Esta clase aísla justo la parte que faltaba —el teclado— sin
 * tocar cómo se pintan.
 *
 * <p>
 * <b>Uso:</b> se llama una vez, en el constructor del control, con la misma
 * acción que ya dispara el clic de ratón; y se pinta el anillo al final de
 * {@code paint()} o {@code paintComponent()}, después de todo lo demás.
 */
public final class Foco {

	/** Client property: si el foco actual se ganó pulsando Tab. */
	private static final String FOCO_POR_TECLADO = "foco.porTeclado";

	/**
	 * Si el último Tab/ratón visto en toda la aplicación fue una tecla Tab.
	 *
	 * <p>
	 * <b>Por qué hace falta un vigía global y no basta con mirar el propio
	 * evento.</b> El foco de un componente puede cambiar por tres vías distintas:
	 * pulsar Tab, hacer clic, o que Swing lo asigne <em>solo</em> —cuando una
	 * ventana se muestra por primera vez, el gestor de foco elige un componente
	 * por defecto sin que medie ni tecla ni clic—. Esa tercera vía es la que
	 * dejaba el anillo dibujado nada más entrar en el catálogo recién iniciada la
	 * sesión: {@code CATÁLOGO} de la cabecera es el primer componente enfocable
	 * de la ventana, así que se lo quedaba por defecto. Preguntar "¿fue un clic?"
	 * no lo detecta, porque no fue ni lo uno ni lo otro. La pregunta que sí lo
	 * cubre es la contraria: "¿fue de verdad Tab?", con un no por defecto.
	 */
	private static volatile boolean ultimoFueTab;

	static {
		Toolkit.getDefaultToolkit().addAWTEventListener(evento -> {

			if (evento instanceof KeyEvent && evento.getID() == KeyEvent.KEY_PRESSED
					&& ((KeyEvent) evento).getKeyCode() == KeyEvent.VK_TAB) {
				ultimoFueTab = true;

			} else if (evento.getID() == java.awt.event.MouseEvent.MOUSE_PRESSED) {
				ultimoFueTab = false;
			}

		}, AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
	}

	private Foco() {
	}

	/**
	 * Hace focuseable el componente y liga Espacio e Intro a la acción, igual que
	 * hace Swing de serie con cualquier botón.
	 */
	public static void activable(JComponent componente, Runnable accion) {

		componente.setFocusable(true);

		componente.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "activar");
		componente.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "activar");

		componente.getActionMap().put("activar", new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				accion.run();
			}
		});

		// Sin esto el anillo no se repintaría al llegar o irse el foco con Tab: nada
		// más dispara un repintado en ese momento. Y se anota aquí, al ganar el
		// foco, si en ese instante el vigía dice que vino de Tab: es el único
		// momento en que la pregunta tiene sentido, porque el vigía sigue
		// actualizándose después con cualquier otra tecla o clic que pase por la
		// aplicación.
		componente.addFocusListener(new FocusAdapter() {

			@Override
			public void focusGained(FocusEvent e) {
				componente.putClientProperty(FOCO_POR_TECLADO, ultimoFueTab);
				componente.repaint();
			}

			@Override
			public void focusLost(FocusEvent e) {
				componente.repaint();
			}
		});
	}

	/**
	 * Liga la tecla Escape, en toda la ventana, a la misma acción que ya dispara el
	 * botón "Cancelar" o "Volver" de la pantalla.
	 *
	 * <p>
	 * <b>Antes no existía en ninguna de las diecisiete pantallas.</b> Un formulario
	 * se puede abandonar con el ratón —cabecera o botón explícito, regla del
	 * proyecto desde la Fase 6— pero no con el teclado, y Escape es el gesto que
	 * cualquiera prueba primero para salir de algo sin querer completarlo.
	 *
	 * <p>
	 * Se registra con {@code WHEN_IN_FOCUSED_WINDOW}, no {@code WHEN_FOCUSED}:
	 * tiene que funcionar sea cual sea el campo con el foco en ese momento —el
	 * cursor escribiendo en "Nombre", un chip, el botón mismo—, no solo cuando el
	 * foco está exactamente en un componente concreto.
	 *
	 * <p>
	 * <b>Qué pantallas no lo llevan, y por qué es intencional.</b> El catálogo, mis
	 * reservas y el login no tienen "acción a medias" de la que salir: son
	 * destinos, no pasos de un formulario. Añadir un Escape ahí no tendría a dónde
	 * llevar que no fuera arbitrario.
	 *
	 * <p>
	 * Tipado como {@code RootPaneContainer} —la interfaz común a {@code JFrame} y
	 * {@code JDialog}— en vez de {@code JFrame} a secas, para que sirva también en
	 * los diálogos modales (Fase 7.5.3, {@code Confirmacion}) sin duplicar esta
	 * misma línea.
	 */
	public static void alPulsarEscape(RootPaneContainer ventana, Runnable accion) {

		ventana.getRootPane().registerKeyboardAction(e -> accion.run(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	/**
	 * Dibuja el anillo de foco, si el componente lo tiene ahora mismo.
	 *
	 * <p>
	 * Un rectángulo redondeado en el acento, apenas por dentro del borde del
	 * componente. Es el mismo lenguaje visual que ya usa FlatLaf para los
	 * controles estándar —un anillo de color, no un cambio de fondo—, así que un
	 * control pintado a mano con el foco encima no se lee como una pieza distinta
	 * del resto del formulario.
	 */
	public static void pintarAnillo(Graphics2D g2destino, JComponent componente) {

		if (!componente.isFocusOwner()) {
			return;
		}

		if (!Boolean.TRUE.equals(componente.getClientProperty(FOCO_POR_TECLADO))) {
			return;
		}

		Graphics2D g2 = (Graphics2D) g2destino.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke(new BasicStroke(2f));
		g2.setColor(Theme.acc());
		g2.drawRoundRect(1, 1, componente.getWidth() - 3, componente.getHeight() - 3, 6, 6);
		g2.dispose();
	}
}
