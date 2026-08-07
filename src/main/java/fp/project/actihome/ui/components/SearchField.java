package fp.project.actihome.ui.components;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Animacion;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Buscador con lupa, que filtra mientras se escribe.
 *
 * <p>
 * <b>Sin botón de buscar, y es deliberado.</b> Un botón obliga a un gesto extra
 * y, peor, convierte la búsqueda en algo que "se lanza": el usuario escribe,
 * pulsa, espera y evalúa. Filtrando en vivo el usuario ve el efecto de cada
 * letra y puede corregir sobre la marcha, que es cómo se busca de verdad cuando
 * no se sabe exactamente qué se busca. Con un catálogo que cabe en memoria no
 * hay ninguna razón para hacerlo de otro modo.
 *
 * <p>
 * <b>Detalle de Swing que cuesta encontrar.</b> Para enterarse de cada
 * pulsación no se escucha al {@code JTextField}, se escucha a su
 * <b>documento</b>: el texto de un campo vive en un modelo aparte
 * ({@code Document}), y es ese modelo el que avisa de inserciones y borrados.
 * Escuchar teclas en el campo dejaría fuera el pegado con el ratón y el
 * deshacer.
 */
public class SearchField extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final int ALTO = 38;

	private final JTextField campo = new JTextField();

	/** Cuánto está enfocado, de 0 a 1. Lo leen el borde y la lupa. */
	private transient double enfocado;

	private transient javax.swing.Timer animacion;

	public SearchField(String marcador, Runnable alCambiar) {

		super(new MigLayout(Space.insets(0, Space.SM, 0, Space.SM), "[18!]" + Space.XS + "[grow,fill]", "[grow,fill]"));

		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder());
		setPreferredSize(new Dimension(300, ALTO));

		campo.setFont(Typography.sans(Typography.BODY_SM));
		campo.setForeground(Theme.txt());
		campo.setOpaque(false);
		campo.setBorder(BorderFactory.createEmptyBorder());

		// Texto de ayuda dentro del campo. Es una propiedad de FlatLaf: se le pide al
		// Look and Feel en vez de simularlo escribiendo y borrando texto, que es el
		// apaño clásico y el que acaba enviando "Buscar…" como si fuera una búsqueda.
		campo.putClientProperty("JTextField.placeholderText", marcador);

		campo.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				alCambiar.run();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				alCambiar.run();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				alCambiar.run();
			}
		});

		// El foco se escucha en el CAMPO, no en el panel: el panel nunca lo recibe
		// —no es focuseable— así que escucharlo ahí no habría disparado nunca. Es un
		// fallo que no da error y deja la animación muerta sin que nada lo delate.
		campo.addFocusListener(new java.awt.event.FocusAdapter() {

			@Override
			public void focusGained(java.awt.event.FocusEvent e) {
				animarHacia(1);
			}

			@Override
			public void focusLost(java.awt.event.FocusEvent e) {
				animarHacia(0);
			}
		});

		add(new Lupa(), "h 18!");
		add(campo);
	}

	public String getTexto() {
		return campo.getText().trim();
	}

	/** Cambia el texto de ayuda (idioma, Fase 7.6). */
	public void setMarcador(String marcador) {
		campo.putClientProperty("JTextField.placeholderText", marcador);
	}

	public void limpiar() {
		campo.setText("");
	}

	/**
	 * Arranca la transición de enfoque, partiendo de donde esté.
	 *
	 * <p>
	 * Se repinta el panel entero y no solo el borde: la lupa también cambia de
	 * color, y son dos dibujos distintos que tienen que moverse a la vez.
	 */
	private void animarHacia(double destino) {

		Animacion.cancelar(animacion);
		animacion = Animacion.animar(this, enfocado, destino, Animacion.CONTROL, v -> {
			enfocado = v;
			repaint();
		});
	}

	/**
	 * Cuánto está enfocado, de 0 a 1. Lo leen el borde y la lupa.
	 *
	 * <p>
	 * Lo consulta {@link Lupa}, que es una clase interna y por eso puede.
	 */
	double enfoque() {
		return enfocado;
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(Theme.SURFACE);
		g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);

		// **El borde se oscurece y engorda al enfocar, en vez de cambiar de color.**
		// La alternativa habitual —teñirlo con el acento— es lo que hace cualquier
		// plantilla, y aquí además chocaría: el acento de verano sobre un campo blanco
		// es un amarillo que casi no se ve. Un borde que gana presencia funciona en las
		// cuatro estaciones sin excepciones y es el gesto del papel, no el de la web.
		g2.setColor(Animacion.mezclar(Theme.FIELD_BORDER, Theme.txt(), enfocado * 0.55));
		g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);

		if (enfocado > 0) {

			// Un segundo trazo por dentro, cuya opacidad sube con el enfoque. Es la forma
			// de engordar la línea sin que el borde salte de uno a dos píxeles de golpe,
			// que se vería como un temblor.
			g2.setColor(new java.awt.Color(Theme.txt().getRed(), Theme.txt().getGreen(), Theme.txt().getBlue(),
					(int) Math.round(90 * enfocado)));
			g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 3, 3);
		}

		g2.dispose();
	}

	/** El icono: un círculo y un mango. Dos trazos bastan para que se lea "buscar". */
	private class Lupa extends JComponent {

		private static final long serialVersionUID = 1L;

		Lupa() {
			setPreferredSize(new Dimension(18, 18));
			setMinimumSize(new Dimension(18, 18));
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// La lupa acompaña al borde: pasa del gris secundario al color del texto
			// mientras el campo gana el foco. Es el mismo valor que mueve el borde, así
			// que los dos gestos van sincronizados por construcción y no por casualidad.
			g2.setColor(Animacion.mezclar(Theme.mut(), Theme.txt(), enfoque()));

			// Remate recto y unión en ángulo, como los siete iconos de comodidad: es lo
			// que separa el dibujo técnico del icono de plantilla, y hasta ahora esta
			// lupa era el único icono del sistema que seguía la convención contraria.
			g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));

			double lado = Math.min(getWidth(), getHeight());
			double escala = lado / 18.0;

			g2.scale(escala, escala);
			g2.draw(new Ellipse2D.Double(2, 2, 10, 10));
			g2.draw(new Line2D.Double(11.5, 11.5, 16, 16));

			g2.dispose();
		}
	}
}
