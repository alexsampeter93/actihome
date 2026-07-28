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

		add(new Lupa(), "h 18!");
		add(campo);
	}

	public String getTexto() {
		return campo.getText().trim();
	}

	public void limpiar() {
		campo.setText("");
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(Theme.SURFACE);
		g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);

		g2.setColor(Theme.FIELD_BORDER);
		g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);

		g2.dispose();
	}

	/** El icono: un círculo y un mango. Dos trazos bastan para que se lea "buscar". */
	private static class Lupa extends JComponent {

		private static final long serialVersionUID = 1L;

		Lupa() {
			setPreferredSize(new Dimension(18, 18));
			setMinimumSize(new Dimension(18, 18));
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2.setColor(Theme.mut());
			g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

			double lado = Math.min(getWidth(), getHeight());
			double escala = lado / 18.0;

			g2.scale(escala, escala);
			g2.draw(new Ellipse2D.Double(2, 2, 10, 10));
			g2.draw(new Line2D.Double(11.5, 11.5, 16, 16));

			g2.dispose();
		}
	}
}
