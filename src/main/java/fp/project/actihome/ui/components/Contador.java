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
import javax.swing.JPanel;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.theme.Animacion;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Un contador de números pequeños: valor al centro, menos a un lado y más al
 * otro.
 *
 * <p>
 * <b>Sustituye al {@code JSpinner} del sistema, y el motivo no es estético
 * sino de coherencia.</b> Un {@code JSpinner} trae dos triangulitos apilados de
 * seis puntos de alto, dibujados por el Look and Feel con su propio color y su
 * propio grosor. En una pantalla que se sostiene sobre líneas de un píxel,
 * versalitas y una serif de contraste alto, ese control es el único elemento
 * que no está hablando el mismo idioma — y se nota precisamente porque todo lo
 * demás sí.
 *
 * <p>
 * <b>Y hay una razón de uso además de la visual.</b> Las flechas de un
 * {@code JSpinner} miden unos seis por seis puntos cada una: acertar en ellas
 * exige puntería, y son la mitad de las veces el motivo por el que alguien
 * acaba escribiendo el número a mano. Aquí cada lado ocupa un cuadrado del alto
 * completo del control, que es varias veces más superficie para el mismo gesto.
 *
 * <p>
 * <b>Los signos son un menos y un más, no dos triángulos.</b> Un triángulo
 * arriba y otro abajo dicen "sube y baja por una lista"; un menos y un más
 * dicen "quita uno y añade uno", que es lo que de verdad hace este control
 * cuando lo que se cuenta son habitaciones o euros.
 *
 * <p>
 * Las medidas son fracciones del alto, no píxeles: con el escalado del sistema
 * al 150 % el control crece y los signos crecen con él.
 */
public class Contador extends JPanel {

	private static final long serialVersionUID = 1L;

	private final int minimo;
	private final int maximo;
	private final int paso;

	private int valor;

	private final transient Runnable alCambiar;

	private final Signo menos;
	private final Signo mas;
	private final Valor lectura;

	/**
	 * @param paso      cuánto suma o resta cada pulsación. Para un precio son 10 o
	 *                  25; para habitaciones, 1. Un contador de euros que subiera de
	 *                  uno en uno obligaría a cien clics para recorrer su rango
	 * @param alCambiar se llama en cada cambio de valor, no al soltar: el catálogo
	 *                  filtra mientras se toca, igual que el buscador
	 */
	public Contador(int inicial, int minimo, int maximo, int paso, Runnable alCambiar) {

		super(new MigLayout(Space.insets(0), "[]0[grow,fill]0[]", "[grow,fill]"));

		this.minimo = minimo;
		this.maximo = maximo;
		this.paso = paso;
		this.valor = Math.max(minimo, Math.min(maximo, inicial));
		this.alCambiar = alCambiar;

		setOpaque(false);

		menos = new Signo(false, () -> cambiar(-paso));
		lectura = new Valor();
		mas = new Signo(true, () -> cambiar(paso));

		add(menos);
		add(lectura);
		add(mas);

		refrescarHabilitados();
	}

	public int getValor() {
		return valor;
	}

	public void setValor(int nuevo) {

		valor = Math.max(minimo, Math.min(maximo, nuevo));

		lectura.repaint();
		refrescarHabilitados();
	}

	private void cambiar(int delta) {

		int nuevo = Math.max(minimo, Math.min(maximo, valor + delta));

		if (nuevo == valor) {
			return;
		}

		valor = nuevo;

		lectura.repaint();
		refrescarHabilitados();

		if (alCambiar != null) {
			alCambiar.run();
		}
	}

	/**
	 * Apaga el signo que ya no puede hacer nada.
	 *
	 * <p>
	 * Es la diferencia entre un control honesto y uno que finge: si el mínimo es 1 y
	 * el valor es 1, el menos no va a hacer nada, y decirlo antes de que lo pulsen
	 * ahorra el clic que no pasa nada y la duda de si está roto.
	 */
	private void refrescarHabilitados() {

		menos.setActivo(valor - paso >= minimo);
		mas.setActivo(valor + paso <= maximo);
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(Theme.SURFACE);
		g2.fillRect(0, 0, getWidth(), getHeight());

		g2.setColor(Theme.FIELD_BORDER);
		g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

		g2.dispose();
		super.paintComponent(g);
	}

	/** El número, centrado, con la tipografía del sistema. */
	private class Valor extends JComponent {

		private static final long serialVersionUID = 1L;

		Valor() {

			int alto = Typography.altoDeControlCompacto();

			// Ancho para tres cifras: un precio de 275 tiene que caber sin que el control
			// cambie de tamaño al escribirse, que es lo que hace que una fila "baile".
			setPreferredSize(new Dimension(alto, alto));
			setMinimumSize(new Dimension(alto, alto));
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			g2.setFont(Typography.sansSemiBold(Typography.BODY_SM));
			g2.setColor(Theme.txt());

			String texto = String.valueOf(valor);
			java.awt.FontMetrics metrica = g2.getFontMetrics();

			int x = (getWidth() - metrica.stringWidth(texto)) / 2;
			int y = (getHeight() - metrica.getHeight()) / 2 + metrica.getAscent();

			g2.drawString(texto, x, y);

			g2.dispose();
		}
	}

	/**
	 * Un lado del contador: el signo dibujado dentro de un cuadrado pulsable.
	 *
	 * <p>
	 * Se ilumina al pasar el ratón con la misma transición de 160 ms que los
	 * botones, en lugar de encenderse de golpe. Es lo que hace que todos los
	 * controles de la aplicación se sientan de la misma familia.
	 */
	private class Signo extends JComponent {

		private static final long serialVersionUID = 1L;

		private final boolean suma;
		private final transient Runnable accion;

		private boolean activo = true;
		private transient double encendido;
		private transient Timer animacion;

		Signo(boolean suma, Runnable accion) {

			this.suma = suma;
			this.accion = accion;

			int alto = Typography.altoDeControlCompacto();

			setPreferredSize(new Dimension(alto, alto));
			setMinimumSize(new Dimension(alto, alto));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					if (activo) {
						accion.run();
					}
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					animarHacia(activo ? 1 : 0);
				}

				@Override
				public void mouseExited(MouseEvent e) {
					animarHacia(0);
				}
			});

			// Alcanzable con el teclado, como exige la regla del proyecto para cualquier
			// control que no extienda un botón de Swing.
			Foco.activable(this, () -> {
				if (activo) {
					accion.run();
				}
			});
		}

		void setActivo(boolean activo) {

			this.activo = activo;

			if (!activo) {
				encendido = 0;
			}

			repaint();
		}

		private void animarHacia(double destino) {

			Animacion.cancelar(animacion);
			animacion = Animacion.animar(this, encendido, destino, Animacion.CONTROL, v -> encendido = v);
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int ancho = getWidth();
			int alto = getHeight();

			if (encendido > 0) {
				g2.setColor(new java.awt.Color(Theme.acc().getRed(), Theme.acc().getGreen(), Theme.acc().getBlue(),
						(int) Math.round(38 * encendido)));
				g2.fillRect(0, 0, ancho, alto);
			}

			// El separador vertical entre el signo y el número. Uno solo por lado, del
			// mismo tono que el borde del control: es la misma hairline, no un color
			// nuevo.
			g2.setColor(Theme.FIELD_BORDER);
			int x = suma ? 0 : ancho - 1;
			g2.drawLine(x, 0, x, alto - 1);

			// El signo. Un tercio del lado, para que respire dentro de su cuadrado.
			double brazo = Math.min(ancho, alto) * 0.30;
			double centroX = ancho / 2.0;
			double centroY = alto / 2.0;

			g2.setColor(activo ? Animacion.mezclar(Theme.txt(), Theme.accText(), encendido)
					: new java.awt.Color(0, 0, 0, 60));

			g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));

			Path2D signo = new Path2D.Double();
			signo.moveTo(centroX - brazo, centroY);
			signo.lineTo(centroX + brazo, centroY);

			if (suma) {
				signo.moveTo(centroX, centroY - brazo);
				signo.lineTo(centroX, centroY + brazo);
			}

			g2.draw(signo);

			Foco.pintarAnillo(g2, this);

			g2.dispose();
		}
	}
}
