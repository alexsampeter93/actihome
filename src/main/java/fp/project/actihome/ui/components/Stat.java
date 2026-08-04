package fp.project.actihome.ui.components;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Typography;

/**
 * Cifra editorial: un número grande en serif con su rótulo pequeño debajo.
 *
 * <p>
 * Es el recurso con el que el hero del catálogo cuenta el estado del catálogo
 * —"12 en catálogo · 11 disponibles · 4,3 media"— sin gastar una frase. Tres
 * cifras alineadas se leen de un vistazo; la misma información en prosa habría
 * que leerla.
 *
 * <p>
 * <b>La cifra se destaca, el rótulo se retira.</b> El número va en Spectral a
 * 26px y el rótulo en 11px con el color secundario. Esa diferencia de peso es lo
 * que hace que la vista caiga primero en el dato y solo después en qué dato es
 * —que es el orden en que uno lee de verdad una cifra.
 */
public class Stat extends JPanel {

	private static final long serialVersionUID = 1L;

	private final JLabel valor;
	private final JLabel etiqueta;

	public Stat(String valorInicial, String rotulo) {
		this(valorInicial, rotulo, false);
	}

	/**
	 * @param destacado si la cifra va en el color de acento. Reservado para una sola
	 *                  de las tres: si se destacan todas no se destaca ninguna.
	 */
	public Stat(String valorInicial, String rotulo, boolean destacado) {

		super(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XXS + "[]"));
		setOpaque(false);

		// Ojo: las etiquetas del sistema resuelven su color sobrescribiendo
		// getForeground(), así que un setForeground() posterior no tendría ningún
		// efecto. Se elige la fábrica que ya trae el color correcto y se le cambia
		// únicamente el tamaño.
		valor = destacado ? Labels.price(valorInicial) : Labels.body(valorInicial);
		valor.setFont(Typography.serif(26f));
		valor.setHorizontalAlignment(SwingConstants.RIGHT);

		etiqueta = Labels.caps(rotulo);
		etiqueta.setHorizontalAlignment(SwingConstants.RIGHT);

		add(valor);
		add(etiqueta);
	}

	/** Actualiza la cifra sin reconstruir el componente. */
	public void setValor(String nuevo) {
		valor.setText(nuevo);
	}

	/** Actualiza el rótulo sin reconstruir el componente (idioma, Fase 7.6). */
	public void setRotulo(String nuevo) {
		etiqueta.setText(nuevo);
	}
}
