package fp.project.actihome.ui.components;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Campo de formulario con su etiqueta encima.
 *
 * <p>
 * <b>Por qué la etiqueta va encima y no al lado.</b> Los formularios actuales
 * de la aplicación usan {@code GridLayout(10, 2)}: etiqueta a la izquierda,
 * campo a la derecha. Ese patrón tiene dos problemas conocidos: obliga a
 * reservar una columna del ancho de la etiqueta más larga —desperdiciando
 * espacio en todas las demás filas— y hace que el ojo salte en zigzag al
 * rellenar el formulario. Con la etiqueta encima, la lectura es una sola
 * columna vertical y el campo puede ocupar todo el ancho disponible. Es lo que
 * marca el handoff y además es lo que mide mejor en los estudios de usabilidad
 * de formularios.
 *
 * <p>
 * La etiqueta va en versalita, que es el recurso tipográfico que sostiene el
 * carácter editorial del diseño.
 *
 * <p>
 * Este componente <b>envuelve</b> el campo en vez de heredar de él: expone
 * {@link #getText()} y {@link #setText(String)} para el uso normal y
 * {@link #getInput()} para cuando haga falta el control de Swing en crudo
 * (poner el foco, escuchar eventos de teclado). Componer en lugar de heredar
 * evita arrastrar los cincuenta métodos de {@code JTextField} en una clase que
 * solo necesita dos.
 */
public class Field extends JPanel {

	private static final long serialVersionUID = 1L;

	private final JTextComponent input;

	private Field(String etiqueta, JTextComponent input) {
		this(etiqueta, input, 38);
	}

	private Field(String etiqueta, JTextComponent input, int alto) {

		super(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));

		this.input = input;
		setOpaque(false);

		input.setFont(Typography.sans(Typography.BODY));
		input.setForeground(Theme.txt());

		add(Labels.caps(etiqueta));

		// Un JTextArea no trae barras de desplazamiento propias: hay que envolverlo.
		// El resto de campos son de una línea y no las necesitan.
		JComponent visible = input instanceof JTextArea ? new JScrollPane(input) : (JComponent) input;

		add(visible, "gaptop " + Space.XXS + ", height " + alto + "!");
	}

	/** Campo de texto normal. */
	public static Field text(String etiqueta) {
		return new Field(etiqueta, new JTextField());
	}

	/** Campo de texto con contenido inicial. */
	public static Field text(String etiqueta, String valor) {

		Field campo = new Field(etiqueta, new JTextField());
		campo.setText(valor);
		return campo;
	}

	/** Campo de contraseña: oculta lo que se escribe. */
	public static Field password(String etiqueta) {
		return new Field(etiqueta, new JPasswordField());
	}

	/**
	 * Campo de varias líneas, para texto largo.
	 *
	 * <p>
	 * Va dentro de un {@code JScrollPane} porque un {@code JTextArea} suelto crece
	 * hacia abajo sin límite según se escribe, y eso rompería la regla de escritorio
	 * del proyecto: la pantalla dejaría de caber en la ventana en cuanto alguien
	 * escribiera un párrafo. Con el scroll, el alto lo fija el formulario y el texto
	 * se desplaza dentro.
	 *
	 * <p>
	 * El ajuste de palabra va activado por el mismo motivo que en
	 * {@link WrappingText}: sin él, una frase larga se convierte en una única línea
	 * con barra de desplazamiento horizontal, que es una forma pésima de escribir.
	 */
	/**
	 * Campo de varias líneas.
	 *
	 * <p>
	 * <b>El alto se pide en líneas, no en píxeles</b>, y esa es la diferencia que
	 * importa. Antes recibía un número de píxeles fijo (96), ajustado mirando la
	 * pantalla de quien lo escribió. En un sistema con el escalado al 150 % la
	 * fuente mide vez y media pero la caja seguía midiendo 96, así que la última
	 * línea de texto quedaba cortada por la mitad: el cuadro no crecía con su
	 * contenido.
	 *
	 * <p>
	 * Calculándolo desde la altura real de una línea de la fuente ya cargada, la
	 * caja mide siempre lo mismo <em>en líneas de texto</em>, que es la unidad en la
	 * que uno piensa cuando decide cuánto debe caber.
	 *
	 * @param lineas cuántas líneas de texto deben verse sin desplazar
	 */
	public static Field textArea(String etiqueta, int lineas) {

		JTextArea area = new JTextArea();
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setBorder(BorderFactory.createEmptyBorder(Space.XS, Space.XS, Space.XS, Space.XS));
		area.setFont(Typography.sans(Typography.BODY));

		int altoDeLinea = area.getFontMetrics(area.getFont()).getHeight();

		return new Field(etiqueta, area, lineas * altoDeLinea + Space.XS * 2 + 2);
	}

	public String getText() {

		if (input instanceof JPasswordField) {
			return new String(((JPasswordField) input).getPassword());
		}

		return input.getText();
	}

	public void setText(String valor) {
		input.setText(valor);
	}

	/** El control de Swing subyacente, para poner el foco o escuchar eventos. */
	public JTextComponent getInput() {
		return input;
	}

	/**
	 * Ejecuta una acción al pulsar Enter dentro del campo.
	 *
	 * <p>
	 * Obligar a soltar el teclado y coger el ratón para enviar un formulario de dos
	 * campos es de las molestias más innecesarias que puede tener una interfaz.
	 * {@code JPasswordField} hereda de {@code JTextField}, así que la comprobación
	 * cubre los dos tipos de campo.
	 */
	public void onEnter(Runnable accion) {

		if (input instanceof JTextField) {
			((JTextField) input).addActionListener(e -> accion.run());
		}
	}

	@Override
	public void requestFocus() {
		input.requestFocus();
	}
}
