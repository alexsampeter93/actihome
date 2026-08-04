package fp.project.actihome.ui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Control segmentado: dos o tres opciones excluyentes en una sola caja.
 *
 * <p>
 * Es el conmutador de vista del catálogo (Lista / Cuadrícula). Frente a un menú
 * desplegable tiene la ventaja de que <b>las opciones están a la vista</b>: se
 * ve cuántas hay y cuál está activa sin tener que abrir nada, y cambiar cuesta
 * un clic en lugar de dos. La contrapartida es que ocupa espacio, así que solo
 * compensa con pocas opciones y etiquetas cortas — que es justo este caso.
 *
 * <p>
 * La opción activa va rellena con el acento, según el handoff. El conjunto se
 * enmarca con una línea fina para que se lea como <em>un</em> control con dos
 * mitades y no como dos botones sueltos que casualmente están juntos.
 */
public class Segmented extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final int ALTO = 34;

	private final List<Segmento> segmentos = new ArrayList<>();
	private final transient Consumer<Integer> alCambiar;
	private int activo;

	/**
	 * @param alCambiar recibe el índice de la opción elegida
	 */
	public Segmented(int inicial, Consumer<Integer> alCambiar, String... opciones) {

		super(new MigLayout(Space.insets(0), "", "[grow,fill]"));

		this.activo = inicial;
		this.alCambiar = alCambiar;
		setOpaque(false);
		setPreferredSize(new Dimension(0, ALTO));

		for (int i = 0; i < opciones.length; i++) {

			final int indice = i;
			Segmento segmento = new Segmento(opciones[i], indice);

			segmento.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {

					// Volver a pulsar la opción activa no es un cambio: avisar de todos modos
					// obligaría a recargar la lista entera para dejarla exactamente igual. Esa
					// comprobación vive en setActivo.
					setActivo(indice);
				}
			});

			// Sin esto no había forma de cambiar de vista con el teclado. Ver la nota de
			// clase en Foco.
			Foco.activable(segmento, () -> setActivo(indice));

			segmentos.add(segmento);
			add(segmento, "h " + ALTO + "!");
		}
	}

	public int getActivo() {
		return activo;
	}

	/**
	 * Cambia el texto de cada opción, en el mismo orden en que se pasaron al
	 * constructor (idioma, Fase 7.6). El ancho de cada segmento depende del
	 * texto, así que hace falta revalidar después de cambiarlo.
	 */
	public void actualizarTextos(String... nuevas) {

		for (int i = 0; i < segmentos.size() && i < nuevas.length; i++) {
			segmentos.get(i).setTexto(nuevas[i]);
		}

		revalidate();
		repaint();
	}

	/**
	 * Selecciona una opción por código, avisando igual que si se hubiera pulsado.
	 *
	 * <p>
	 * Lo necesita cualquiera que quiera dejar el control en un estado concreto sin
	 * simular un clic: restaurar una preferencia guardada, o la herramienta de
	 * capturas, que necesita fotografiar las dos vistas del catálogo.
	 */
	public void setActivo(int indice) {

		if (indice < 0 || indice >= segmentos.size() || indice == activo) {
			return;
		}

		activo = indice;
		repaint();
		alCambiar.accept(indice);
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(Theme.FIELD_BORDER);
		g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);

		g2.dispose();
	}

	/** Una de las opciones. */
	private class Segmento extends JComponent {

		private static final long serialVersionUID = 1L;

		private String texto;
		private final int indice;

		Segmento(String texto, int indice) {

			this.texto = texto.toUpperCase();
			this.indice = indice;

			setFont(Typography.label(11f));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}

		void setTexto(String texto) {
			this.texto = texto.toUpperCase();
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(getFontMetrics(getFont()).stringWidth(texto) + Space.XL * 2, ALTO);
		}

		/** Ver la nota de {@code SeasonSelector.Pestana}: sin mínimo, se aplasta. */
		@Override
		public Dimension getMinimumSize() {
			return getPreferredSize();
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			boolean esActivo = indice == activo;
			Color tinta;

			if (esActivo) {
				g2.setColor(Theme.acc());
				g2.fillRect(0, 0, getWidth(), getHeight());
				tinta = Theme.onAccent();
			} else {
				tinta = Theme.mut();
			}

			g2.setColor(tinta);
			g2.setFont(getFont());

			int ancho = g2.getFontMetrics().stringWidth(texto);
			int base = (getHeight() + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;

			g2.drawString(texto, (getWidth() - ancho) / 2, base);

			g2.dispose();

			Foco.pintarAnillo((Graphics2D) g, this);
		}
	}
}
