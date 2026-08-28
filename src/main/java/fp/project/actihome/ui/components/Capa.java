package fp.project.actihome.ui.components;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

/**
 * Un panel que <b>apila sus hijos a propósito</b>: el botón flotante sobre la
 * lista del catálogo, el velo sobre una miniatura de la galería.
 *
 * <p>
 * Es un {@code JPanel} normal y transparente; lo único que añade es
 * {@link Superpuesto}, la marca con la que {@code MedirResponsive} sabe que los
 * solapes de dentro son intencionados y no un reparto que se ha ido de las
 * manos. Sin ella, el detector no tiene forma de distinguir el velo estacional
 * sobre una foto —que está bien— de un "ACTIHOME" pisado por un "BUSCAR" —que
 * no—: en píxeles son exactamente lo mismo.
 *
 * <p>
 * <b>Y el orden importa, al revés de lo intuitivo.</b> Swing pinta los hijos
 * del último índice al primero, así que <b>lo que se añade primero queda
 * encima</b>. Es al contrario que en HTML.
 *
 * <p>
 * Otra cosa a tener en cuenta al apilar con MigLayout: que <b>solo uno</b> de
 * los hijos ocupe celda. Si todos van en posición absoluta ({@code pos}),
 * ninguno aporta tamaño a la rejilla y el contenedor entero queda con altura
 * cero.
 */
public class Capa extends JPanel implements Superpuesto {

	private static final long serialVersionUID = 1L;

	public Capa(MigLayout layout) {

		super(layout);
		setOpaque(false);
	}
}
