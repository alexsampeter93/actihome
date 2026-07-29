package fp.project.actihome.ui.components;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Selector de una sub-nota: etiqueta en versalita, cinco estrellas pinchables y
 * el valor elegido.
 *
 * <p>
 * <b>Sustituye a un {@code JSpinner} de 0 a 5 con paso 0,1.</b> Ese control
 * ofrece cincuenta valores posibles y obliga a pulsar una flechita hasta
 * cincuenta veces para llegar al que quieres. Nadie tiene una opinión con una
 * décima de precisión sobre el wifi de una casa rural: pedir esa exactitud no
 * recoge mejor la opinión, solo hace el formulario más pesado. Cinco estrellas
 * es un gesto y se entiende sin instrucciones.
 *
 * <p>
 * <b>Qué se pierde y por qué está bien.</b> El modelo sigue guardando un
 * {@code double}, así que las reseñas antiguas con decimales se conservan y se
 * muestran tal cual; lo que cambia es la granularidad con la que se pueden
 * <i>crear</i> desde ahora. La nota total, que calcula el servicio promediando
 * las cinco, sí sigue teniendo decimales. Es la simplificación que especifica el
 * handoff.
 *
 * <p>
 * Las estrellas se dibujan con geometría, no con el carácter "★". Un glifo
 * depende de que la fuente lo incluya —Spectral y Manrope están empaquetadas y
 * no tienen por qué—, y cuando falta, Swing lo sustituye por un recuadro vacío.
 * Un polígono se ve igual en cualquier equipo y además se puede teñir con el
 * acento de la estación activa.
 */
public class StarRating extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final int ESTRELLAS = 5;
	private static final int LADO = 26;

	private int valor;
	private final Estrellas estrellas;
	private final JLabel lectura;

	public StarRating(String etiqueta, int valorInicial) {

		// Etiqueta, estrellas y lectura en una sola fila, con la etiqueta a ancho fijo
		// para que las cinco valoraciones queden alineadas en columna. Apilar la
		// etiqueta encima —como hace Field— multiplicaría por cinco el alto del
		// formulario y dejaría los botones fuera de la ventana, que es justo lo que la
		// regla de escritorio del proyecto no permite.
		super(new MigLayout(Space.insets(0), "[100!]" + Space.MD + "[]" + Space.MD + "[]", "[]"));
		setOpaque(false);

		this.valor = acotar(valorInicial);

		add(Labels.caps(etiqueta), "aligny center");

		estrellas = new Estrellas();
		add(estrellas, "w " + (LADO * ESTRELLAS) + "!, h " + LADO + "!, aligny center");

		lectura = Labels.muted(textoLectura());
		add(lectura, "aligny center");
	}

	public int getValor() {
		return valor;
	}

	public void setValor(int nuevo) {

		this.valor = acotar(nuevo);
		lectura.setText(textoLectura());
		estrellas.repaint();
	}

	private String textoLectura() {
		return valor == 0 ? "sin valorar" : valor + " de " + ESTRELLAS;
	}

	private static int acotar(int n) {
		return Math.max(0, Math.min(ESTRELLAS, n));
	}

	/** La fila de cinco estrellas: dibujo, pulsación y previsualización al pasar. */
	private class Estrellas extends JComponent {

		private static final long serialVersionUID = 1L;

		/** Estrella sobre la que está el ratón, o 0 si está fuera. */
		private int resaltadas;

		Estrellas() {

			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setPreferredSize(new Dimension(LADO * ESTRELLAS, LADO));
			setMinimumSize(new Dimension(LADO * ESTRELLAS, LADO));

			MouseAdapter raton = new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {

					int elegida = estrellaEn(e.getX());

					// Volver a pulsar la estrella ya seleccionada pone la nota a cero. Sin
					// esto no habría forma de deshacer un clic: el control no tiene ningún
					// otro sitio donde pinchar para volver a "sin valorar".
					setValor(elegida == valor ? 0 : elegida);
				}

				@Override
				public void mouseMoved(MouseEvent e) {
					resaltadas = estrellaEn(e.getX());
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e) {
					resaltadas = 0;
					repaint();
				}
			};

			addMouseListener(raton);
			addMouseMotionListener(raton);
		}

		private int estrellaEn(int x) {
			return acotar(x / LADO + 1);
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int cuantas = resaltadas > 0 ? resaltadas : valor;

			for (int i = 0; i < ESTRELLAS; i++) {

				Path2D estrella = dibujar(i * LADO + LADO / 2.0, LADO / 2.0, LADO / 2.0 - 3);

				if (i < cuantas) {
					g2.setColor(Theme.acc());
					g2.fill(estrella);

				} else {
					g2.setColor(Theme.FIELD_BORDER);
					g2.setStroke(new BasicStroke(1.2f));
					g2.draw(estrella);
				}
			}

			g2.dispose();
		}

		/**
		 * Una estrella de cinco puntas.
		 *
		 * <p>
		 * Se recorren diez vértices alternando el radio exterior y el interior. El
		 * primero se coloca arriba restando un cuarto de vuelta, porque el ángulo cero
		 * en pantalla apunta a la derecha y una estrella con una punta hacia la derecha
		 * se ve torcida.
		 */
		private Path2D dibujar(double cx, double cy, double radio) {

			Path2D camino = new Path2D.Double();
			double interior = radio * 0.42;

			for (int i = 0; i < 10; i++) {

				double r = i % 2 == 0 ? radio : interior;
				double angulo = Math.PI * i / 5 - Math.PI / 2;

				double x = cx + r * Math.cos(angulo);
				double y = cy + r * Math.sin(angulo);

				if (i == 0) {
					camino.moveTo(x, y);
				} else {
					camino.lineTo(x, y);
				}
			}

			camino.closePath();
			return camino;
		}
	}
}
