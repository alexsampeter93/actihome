package fp.project.actihome.ui.components;

import java.awt.Color;

import javax.swing.JTextArea;

import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Un párrafo de cuerpo que se ajusta a su ancho, en lugar de una sola línea.
 *
 * <p>
 * {@code Labels.body} devuelve un {@code JLabel}, y un {@code JLabel} no
 * envuelve el texto: una descripción de tres frases saldría como una sola línea
 * cortada por el borde de la ventana. Para texto largo hace falta un
 * {@code JTextArea} configurado para comportarse como una etiqueta —sin caja,
 * sin cursor, sin poder editarse— y con el ajuste de palabra activado.
 *
 * <p>
 * Sigue la misma regla que el resto del sistema: el color no se guarda, se
 * resuelve en cada pintado sobrescribiendo {@code getForeground()}, así que el
 * párrafo cambia de color solo al cambiar de estación.
 *
 * <p>
 * A diferencia de las etiquetas de {@code Labels}, aquí no hace falta guardar
 * el color en un campo propio ni protegerse de un {@code null} en el
 * constructor del padre: {@link Theme#txt()} lee un campo <b>estático</b> de
 * {@code Theme}, que existe desde que la clase se carga y no depende de que
 * esta instancia termine de construirse.
 */
public class WrappingText extends JTextArea {

	private static final long serialVersionUID = 1L;

	/** Si este párrafo es texto principal o una aclaración secundaria. */
	private final boolean secundario;

	public WrappingText(String texto) {
		this(texto, false);
	}

	private WrappingText(String texto, boolean secundario) {

		super(texto);

		this.secundario = secundario;

		setEditable(false);
		setFocusable(false);
		setOpaque(false);
		setLineWrap(true);
		setWrapStyleWord(true);
		setBorder(null);
		setFont(Typography.sans(secundario ? Typography.BODY_SM : Typography.BODY));
	}

	/**
	 * Párrafo secundario: el equivalente de {@code Labels.muted} para texto que
	 * ocupa más de una línea.
	 *
	 * <p>
	 * <b>Existe porque faltaba, y su ausencia era un fallo latente.</b> Las
	 * descripciones de ayuda bajo un control —"exporta una copia completa de la base
	 * de datos a un fichero…"— se venían escribiendo con {@code Labels.muted}, que
	 * devuelve un {@code JLabel} y <b>no parte el texto en líneas</b>. Un JLabel de
	 * cien caracteres declara un ancho preferido de cien caracteres, y MigLayout no
	 * lo encoge: desborda el contenedor. En una ventana ancha no se nota; en una de
	 * 1024 puntos empuja la tarjeta entera fuera de la pantalla.
	 *
	 * <p>
	 * Es la misma familia de fallo que {@code FilaFluida} resuelve para una fila de
	 * elementos, aplicada a un párrafo: el problema nunca es que el texto se vea
	 * apretado, es que algo acaba dibujado donde no se puede alcanzar.
	 */
	public static WrappingText muted(String texto) {
		return new WrappingText(texto, true);
	}

	@Override
	public Color getForeground() {

		// Guarda contra el null implícito del arranque: getForeground() se llama desde
		// el constructor de JTextArea, antes de que 'secundario' tenga valor. Un
		// boolean no puede ser null, pero sí es false en ese momento, así que el color
		// inicial sería el principal aunque el párrafo fuera secundario. Da igual: el
		// primer pintado real ocurre mucho después, con el campo ya asignado.
		return secundario ? Theme.mut() : Theme.txt();
	}
}
