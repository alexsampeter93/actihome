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

	private static final int ALTO = 46;

	/**
	 * Lo que se separa el papel del borde del componente.
	 *
	 * <p>
	 * <b>Es la diferencia entre unas escuadras que se ven y unas que no.</b> Con la
	 * superficie blanca ocupando el componente entero, el rectángulo ya estaba
	 * dibujado —lo dibujaba el propio papel contra el fondo crema— y las escuadras no
	 * añadían nada: enfocar el campo no cambiaba nada perceptible. Dejándoles un
	 * hueco por fuera, las marcas de corte quedan donde van de verdad en un pliego,
	 * <b>fuera del área impresa</b>, y al enfocar se cierran alrededor del papel.
	 *
	 * <p>
	 * Es la misma idea que el filete grabado del botón primario, del otro lado del
	 * borde: allí se abre hacia fuera, aquí se cierra hacia dentro.
	 */
	private static final int HUECO = 5;

	private final JTextField campo = new JTextField();

	/** Cuánto está enfocado, de 0 a 1. Lo leen el borde y la lupa. */
	private transient double enfocado;

	private transient javax.swing.Timer animacion;

	public SearchField(String marcador, Runnable alCambiar) {

		super(new MigLayout(Space.insets(0, Space.MD, 0, Space.MD), "[18!]" + Space.XS + "[grow,fill]", "[grow,fill]"));

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
	 * Fuerza el estado de enfoque, para poder retratarlo.
	 *
	 * <p>
	 * Existe para {@code MirarControles} por la misma razón que
	 * {@code SettingsFrame.generarCodigoPara}: el estado que hay que juzgar no se
	 * alcanza dibujando el componente, hace falta que alguien lo enfoque, y una
	 * lámina pintada fuera de pantalla no tiene foco de teclado. Sin esto, la única
	 * forma de revisar el enfoque sería abrir la aplicación y hacer clic — que es
	 * justo el bucle lento que las herramientas del proyecto existen para evitar.
	 */
	public void mostrarEnfocado(boolean si) {
		animarHacia(si ? 1 : 0);
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

	/**
	 * Superficie, escuadras de imprenta y un filete de base que se abre desde el
	 * centro.
	 *
	 * <p>
	 * <b>Lo que fallaba no era el borde, era la forma.</b> Un rectángulo de esquinas
	 * redondeadas con una línea alrededor es <em>la</em> forma genérica: la traen
	 * por defecto todos los frameworks, sale en todas las plantillas y no dice nada
	 * de quién la ha hecho. Se podía afinar el color y el grosor cuanto se quisiera y
	 * seguiría siendo un campo de formulario cualquiera.
	 *
	 * <p>
	 * Ahora son tres capas con tres papeles distintos:
	 *
	 * <ol>
	 * <li><b>La superficie blanca</b>, con radio 2 en vez de 4 — el mismo radio
	 * mínimo que el resto del sistema, lo justo para que la esquina no corte.</li>
	 * <li><b>Las escuadras</b> de {@link Escuadras}, que en reposo son cuatro
	 * ángulos y al enfocar se cierran hasta completar el marco. Es el mismo gesto
	 * del botón secundario, y esa repetición es lo que lo convierte en vocabulario:
	 * en esta aplicación, unas escuadras que se cierran significan "esto responde".</li>
	 * <li><b>El filete de base</b>, que crece <b>desde el centro hacia los dos
	 * lados</b>. Un subrayado que crece de izquierda a derecha ya lo usan los
	 * enlaces y significa "se lee en esta dirección"; aquí no se lee nada, se
	 * escribe, y un trazo que se abre simétricamente dice justamente eso: el campo
	 * se abre.</li>
	 * </ol>
	 *
	 * <p>
	 * <b>Nada de teñir con el acento.</b> Es lo que haría cualquier plantilla y aquí
	 * además no funcionaría: el acento de verano sobre blanco es un amarillo que casi
	 * no se ve. Lo que gana el campo al enfocarse es <em>presencia</em> —más línea,
	 * más contraste— y eso se comporta igual en las cuatro estaciones.
	 */
	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		int ancho = getWidth();
		int alto = getHeight();

		// El papel: la superficie del campo, separada del borde del componente. Ese
		// hueco es lo que deja sitio a las escuadras por fuera.
		g2.setColor(Theme.SURFACE);
		g2.fillRoundRect(HUECO, HUECO, ancho - 2 * HUECO, alto - 2 * HUECO, 2, 2);

		g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));

		g2.setColor(Animacion.mezclar(Theme.FIELD_BORDER, Theme.txt(), enfocado * 0.45));
		g2.drawRoundRect(HUECO, HUECO, ancho - 2 * HUECO - 1, alto - 2 * HUECO - 1, 2, 2);

		// Remate recto y unión en ángulo: una escuadra con las puntas redondeadas deja
		// de parecer una marca de corte y pasa a parecer un borde mal terminado.
		g2.setColor(Animacion.mezclar(Theme.FIELD_BORDER, Theme.txt(), 0.15 + enfocado * 0.6));

		Escuadras.pintar(g2, 0.5, 0.5, ancho - 1.0, alto - 1.0, enfocado);

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
