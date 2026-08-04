package fp.project.actihome.ui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Grupo de opciones excluyentes en forma de texto subrayado.
 *
 * <p>
 * Es el control de ordenación del catálogo (Mejor valorados · Precio menor ·
 * Precio mayor). Frente al {@link Segmented}, este no dibuja caja: la opción
 * activa se marca con un subrayado de acento y nada más.
 *
 * <p>
 * <b>Cuándo usar uno y cuándo el otro.</b> El segmentado tiene más peso visual y
 * sirve para decisiones que cambian <em>qué</em> se está viendo —la vista de
 * lista o la de cuadrícula—. Estas opciones cambian solo <em>en qué orden</em>,
 * que es una decisión menor, y por eso van con el tratamiento más ligero. Que
 * dos controles hagan lo mismo con distinto énfasis no es una incoherencia: es
 * jerarquía.
 *
 * <p>
 * Como en el selector de estación, la opción activa se marca con color
 * <b>y</b> subrayado. Dos canales en lugar de uno: quien no distinga bien los
 * colores sigue viendo cuál está elegida.
 *
 * <p>
 * <b>Segunda versión de {@code Opcion}: ya no dibuja su texto a mano.</b> La
 * primera usaba {@code drawString} con {@code Theme.acc()} como color de
 * texto —el mismo patrón, y los mismos dos fallos, que tenían
 * {@code SeasonSelector.Pestana} y {@code HeaderPanel.Destino} antes de
 * corregirse—: el texto pintado a mano no pasa por el motor de repintado
 * estándar de Swing, lo que abría la puerta al mismo temblor al pasar el
 * ratón, y {@code Theme.acc()} usado como color de texto en vez de
 * {@code Theme.accText()} daba 1,88:1 en verano —muy por debajo del 4,5 que
 * exige un texto de 11px—, detectado con {@code MedirContraste} (entrada 032
 * del diario). Ahora es un {@code JLabel} de verdad más una barra aparte para
 * el subrayado, igual que las otras dos.
 */
public class OptionLinks extends JPanel {

	private static final long serialVersionUID = 1L;

	private int activo;
	private final transient Consumer<Integer> alCambiar;

	public OptionLinks(int inicial, Consumer<Integer> alCambiar, String... opciones) {

		super(new MigLayout(Space.insets(0), "", "[]"));

		this.activo = inicial;
		this.alCambiar = alCambiar;
		setOpaque(false);

		for (int i = 0; i < opciones.length; i++) {

			final int indice = i;
			Opcion opcion = new Opcion(opciones[i], indice);

			// Sin esto no había forma de elegir el orden con el teclado. Ver la nota de
			// clase en Foco.
			Foco.activable(opcion, () -> seleccionar(indice));

			add(opcion, "gapleft " + (i == 0 ? 0 : Space.MD));
		}
	}

	public int getActivo() {
		return activo;
	}

	/**
	 * Cambia el texto de cada opción, en el mismo orden en que se pasaron al
	 * constructor (idioma, Fase 7.6). No toca cuál está activa.
	 */
	public void actualizarTextos(String... nuevas) {

		java.awt.Component[] hijos = getComponents();

		for (int i = 0; i < hijos.length && i < nuevas.length; i++) {
			((Opcion) hijos[i]).setTexto(nuevas[i]);
		}
	}

	private void seleccionar(int indice) {

		if (indice != activo) {
			activo = indice;
			repaint();
			alCambiar.accept(indice);
		}
	}

	private class Opcion extends JPanel {

		private static final long serialVersionUID = 1L;

		private static final int GROSOR = 2;

		private final int indice;
		private final JLabel etiqueta;
		private final JPanel subrayado;
		private boolean encima;

		Opcion(String texto, int indice) {

			super(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XXS + "[]"));
			this.indice = indice;

			setOpaque(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			etiqueta = new JLabel(texto.toUpperCase());
			etiqueta.setFont(Typography.label(11f));

			subrayado = new JPanel();
			subrayado.setOpaque(true);
			// Ver la nota gemela en SeasonSelector.Pestana: un JPanel recién creado
			// informa un mínimo de 10x10, muy por encima del alto real que fuerza el "h
			// GROSOR!" de abajo.
			subrayado.setMinimumSize(new java.awt.Dimension(0, GROSOR));

			add(etiqueta);
			add(subrayado, "growx, h " + GROSOR + "!");

			MouseAdapter interaccion = new MouseAdapter() {

				// El clic se escuchaba solo en el panel (this), pero la etiqueta lo cubre
				// casi entero: al ser un JLabel hijo, es él quien recibe el evento y el
				// clic nunca llegaba al panel. Por eso "Ordenar" no respondía. Ver la nota
				// gemela en HeaderPanel.Destino, que ya registraba el mismo listener en
				// los dos sitios.
				@Override
				public void mouseClicked(MouseEvent e) {
					seleccionar(indice);
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					encima = true;
					actualizarColores();
				}

				@Override
				public void mouseExited(MouseEvent e) {
					encima = false;
					actualizarColores();
				}
			};

			addMouseListener(interaccion);
			etiqueta.addMouseListener(interaccion);

			actualizarColores();
		}

		void setTexto(String texto) {
			etiqueta.setText(texto.toUpperCase());
		}

		private void actualizarColores() {

			boolean esActivo = indice == activo;
			Color tinta = esActivo ? Theme.accText() : encima ? Theme.txt() : Theme.mut();

			etiqueta.setForeground(tinta);
			subrayado.setBackground(esActivo ? Theme.accText() : getBackground());
			subrayado.setOpaque(esActivo);
		}

		/**
		 * Se resuelve el estado en cada repintado, no solo al hacer clic o al pasar el
		 * ratón. Ver la nota gemela en {@code SeasonSelector.Pestana.paint()}.
		 */
		@Override
		public void paint(java.awt.Graphics g) {

			actualizarColores();
			super.paint(g);
			Foco.pintarAnillo((java.awt.Graphics2D) g, this);
		}
	}
}
