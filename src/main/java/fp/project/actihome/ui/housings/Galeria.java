package fp.project.actihome.ui.housings;

import java.awt.Dimension;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.components.Capa;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Typography;

/**
 * La galería de fotos del detalle: una grande y dos apiladas al lado.
 *
 * <p>
 * <b>Por qué esta composición y no una tira de miniaturas.</b> Una tira trata a
 * las cuatro fotos como iguales, y no lo son: la principal es la que decide si
 * alguien sigue leyendo. Con una grande y dos pequeñas, la jerarquía la pone el
 * tamaño y no hace falta explicarla.
 *
 * <p>
 * <b>Se adapta a cuántas fotos hay de verdad</b>, y esa es la parte que importa:
 * <ul>
 * <li>Solo la principal — una foto a todo el ancho, como estaba antes de que
 * existiera esta clase.</li>
 * <li>Una extra — dos fotos, la principal más grande.</li>
 * <li>Dos o más — la composición completa, y si sobran, la última lleva encima
 * un velo con "+N fotos".</li>
 * </ul>
 * Nunca se pintan huecos vacíos para completar la cuadrícula. Enseñar tres
 * marcos grises con un "+6" encima sería mentir sobre cuántas fotos existen, y
 * es exactamente lo que esta pantalla evitó hacer durante seis fases.
 *
 * <p>
 * Al pulsar una foto pequeña se intercambia con la grande. Es la interacción
 * más barata que convierte la galería en algo que se puede recorrer sin abrir
 * un visor a pantalla completa, que sería otra pantalla que mantener.
 */
public class Galeria extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Cuántas caben en la composición además de la grande. */
	private static final int SECUNDARIAS = 2;

	private final transient List<String> fotos = new ArrayList<>();
	private final transient String tipo;
	private final transient String estado;
	private final boolean disponible;
	private final transient String destacado;

	/** Por debajo de esto una foto de alojamiento deja de enseñar nada util. */
	private static final int MINIMO_UTIL = 260;

	private int principal;

	/**
	 * @param tipo       etiqueta de la esquina superior izquierda ("Casa")
	 * @param estado     etiqueta inferior izquierda ("Disponible")
	 * @param destacado  distintivo estacional, o {@code null}
	 * @param principales el archivo de la foto principal seguido de los de galería;
	 *                    los nulos se descartan
	 */
	public Galeria(String tipo, String estado, boolean disponible, String destacado, List<String> principales) {

		super(new MigLayout(Space.insets(0), "[grow,fill]" + Space.XS + "[]", "[grow,fill]"));
		setOpaque(false);

		// **La galería puede encogerse, y esta línea es la que se lo permite.** Sin un
		// mínimo declarado lo hereda de sus fotos, y entre la foto grande y la columna
		// de miniaturas exigía más de seiscientos puntos. En la ficha de alojamiento
		// eso significaba que la galería y la columna de información no cabían juntas
		// en una ventana de 1024, y la de la derecha se salía por el borde — donde no
		// hay barra de desplazamiento que la rescate.
		//
		// El número no es un tamaño de diseño, es un SUELO: por debajo de 260 puntos
		// una foto de alojamiento deja de enseñar nada útil. Por encima manda el
		// preferido, que sigue siendo el de siempre, así que en una ventana normal no
		// cambia nada. Es la regla de siempre: cuando falta sitio cede el aire, y aquí
		// cede la imagen antes que el precio y el botón de reservar.
		setMinimumSize(new Dimension(MINIMO_UTIL, MINIMO_UTIL * 2 / 3));

		this.tipo = tipo;
		this.estado = estado;
		this.disponible = disponible;
		this.destacado = destacado;

		for (String foto : principales) {
			if (foto != null && !foto.trim().isEmpty()) {
				fotos.add(foto);
			}
		}

		reconstruir();
	}

	private void reconstruir() {

		removeAll();

		if (fotos.isEmpty()) {
			// Sin ninguna foto: el marcador tintado de siempre, a todo el ancho.
			add(marco(null, true), "grow, span 2");

		} else if (fotos.size() == 1) {
			add(marco(fotos.get(0), true), "grow, span 2");

		} else {
			add(marco(fotos.get(principal), true), "grow");
			add(columnaSecundaria(), "growy, w 30%");
		}

		revalidate();
		repaint();
	}

	/** Las dos miniaturas de la derecha, con el contador si sobran fotos. */
	private JPanel columnaSecundaria() {

		JPanel columna = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[grow,fill]" + Space.XS + "[grow,fill]"));
		columna.setOpaque(false);

		// Las secundarias son todas menos la que está en grande, en orden.
		List<Integer> otras = new ArrayList<>();

		for (int i = 0; i < fotos.size(); i++) {
			if (i != principal) {
				otras.add(i);
			}
		}

		int visibles = Math.min(SECUNDARIAS, otras.size());

		for (int i = 0; i < visibles; i++) {

			int indice = otras.get(i);
			boolean esLaUltima = i == visibles - 1;
			int ocultas = otras.size() - visibles;

			JComponent miniatura = marco(fotos.get(indice), false);

			if (esLaUltima && ocultas > 0) {
				columna.add(conContador(miniatura, ocultas, indice), "grow");
			} else {
				columna.add(pinchable(miniatura, indice), "grow");
			}
		}

		return columna;
	}

	private ImagePlaceholder marco(String archivo, boolean conEtiquetas) {

		ImagePlaceholder imagen = conEtiquetas ? new ImagePlaceholder(tipo, estado, disponible, archivo)
				: new ImagePlaceholder(null, null, disponible, archivo);

		if (conEtiquetas) {
			imagen.setDestacado(destacado);
		}

		// Mínimo cero para que la galería pueda encogerse con la ventana: sin esto,
		// tres ImagePlaceholder con su preferido de 240x150 fijan un suelo que en una
		// ventana estrecha empuja la columna de datos fuera de la pantalla.
		imagen.setMinimumSize(new java.awt.Dimension(0, 0));

		return imagen;
	}

	private JComponent pinchable(JComponent componente, int indice) {

		componente.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		componente.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				principal = indice;
				reconstruir();
			}
		});

		return componente;
	}

	/**
	 * Miniatura con el velo y el "+N fotos" encima.
	 *
	 * <p>
	 * El contador se dibuja en una capa aparte y no dentro de
	 * {@code ImagePlaceholder}: aquel componente ya tiene tres ranuras de etiqueta
	 * y añadirle una cuarta solo para este caso lo convertiría en un cajón de
	 * sastre. Aquí es una superposición, que es lo que de verdad es.
	 */
	private JComponent conContador(JComponent miniatura, int ocultas, int indice) {

		// En Swing los hijos se pintan del último índice al primero, así que lo que se
		// añade PRIMERO queda ENCIMA. Es al revés que en HTML, y por eso el velo va
		// antes que la foto.
		Capa capa = new Capa(new MigLayout(Space.insets(0), "[grow,fill]", "[grow,fill]"));

		capa.add(new Velo(ocultas), "pos 0 0 container.x2 container.y2");
		capa.add(miniatura, "pos 0 0 container.x2 container.y2");

		return pinchable(capa, indice);
	}

	/** Rectángulo oscuro translúcido con el recuento centrado. */
	private static class Velo extends JComponent {

		private static final long serialVersionUID = 1L;

		private final int ocultas;

		Velo(int ocultas) {
			this.ocultas = ocultas;
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2.setColor(new Color(0, 0, 0, 115));
			g2.fillRect(0, 0, getWidth(), getHeight());

			g2.setColor(Color.WHITE);
			g2.setFont(Typography.label(11f));

			String texto = "+" + ocultas;
			int ancho = g2.getFontMetrics().stringWidth(texto);

			g2.drawString(texto, (getWidth() - ancho) / 2, getHeight() / 2 + 4);

			g2.dispose();
		}
	}
}
