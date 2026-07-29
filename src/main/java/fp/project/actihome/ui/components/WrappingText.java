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

	public WrappingText(String texto) {

		super(texto);

		setEditable(false);
		setFocusable(false);
		setOpaque(false);
		setLineWrap(true);
		setWrapStyleWord(true);
		setBorder(null);
		setFont(Typography.sans(Typography.BODY));
	}

	@Override
	public Color getForeground() {
		return Theme.txt();
	}
}
