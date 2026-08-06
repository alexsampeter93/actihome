package fp.project.actihome.ui.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Barra de una sub-nota de reseña: ubicación, servicio, wifi, comida o
 * limpieza.
 *
 * <p>
 * Tres partes en una fila: etiqueta en versalita, barra rellena en proporción a
 * la nota sobre cinco, y el valor en serif a la derecha.
 *
 * <p>
 * Sustituye a lo que hay hoy, que es un par etiqueta-valor con el número en
 * crudo ("Calificación de wifi: 4.0"). La diferencia no es decorativa: cinco
 * barras alineadas se comparan de un vistazo, mientras que cinco números
 * obligan a leerlos y compararlos mentalmente uno a uno. **Codificar un dato en
 * una longitud es más rápido de leer que codificarlo en una cifra**, y ese es
 * justamente el trabajo de una interfaz.
 */
public class ScoreBar extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final double MAXIMO = 5.0;

	public ScoreBar(String etiqueta, double nota) {

		super(new MigLayout(Space.insets(0), "[110!]" + Space.MD + "[grow,fill]" + Space.MD + "[40!]", ""));
		setOpaque(false);

		add(Labels.caps(etiqueta));
		add(new Barra(nota), "height 6!");

		javax.swing.JLabel valor = Labels.body(String.format("%.1f", nota).replace('.', ','));
		valor.setFont(Typography.serifMedium(15f));
		add(valor);
	}

	/** El carril y su relleno. */
	private static class Barra extends JPanel {

		private static final long serialVersionUID = 1L;

		private final double nota;

		Barra(double nota) {

			this.nota = nota;
			setOpaque(false);
			setPreferredSize(new Dimension(100, 6));

			// **El mínimo se declara, y es la regla del proyecto que esta clase se
			// saltaba.** Definir solo el preferido deja que el mínimo lo invente el Look
			// and Feel —FlatLaf devolvía 10×10— y eso hace que un carril de 6 puntos de
			// alto se considere "aplastado" siempre, porque su alto real nunca llega a
			// los 10 que nadie pidió. Aquí no se rompía nada en pantalla: lo que se
			// rompía era la comprobación, que informaba de un fallo inexistente en todas
			// las medidas. Un detector que da falsos positivos deja de leerse, y
			// entonces tampoco avisa de los verdaderos.
			setMinimumSize(new Dimension(10, 6));
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int ancho = getWidth();
			int alto = getHeight();

			// Carril completo: da la referencia de "sobre cuánto". Sin él, una barra al
			// 60 % y otra al 100 % no se distinguen de dos barras de distinto largo.
			g2.setColor(Theme.HAIRLINE);
			g2.fillRoundRect(0, 0, ancho, alto, alto, alto);

			int relleno = (int) Math.round(ancho * Math.max(0, Math.min(MAXIMO, nota)) / MAXIMO);
			g2.setColor(Theme.acc());
			g2.fillRoundRect(0, 0, relleno, alto, alto, alto);

			g2.dispose();
		}
	}
}
