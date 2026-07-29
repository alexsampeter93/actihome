package fp.project.actihome.ui.components;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.LayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.Timer;

import fp.project.actihome.ui.theme.Particulas;
import fp.project.actihome.ui.theme.Theme;

/**
 * El lienzo de una pantalla: un panel cuyo fondo es siempre el de la estación
 * activa.
 *
 * <p>
 * <b>Por qué no basta con {@code setBackground(Theme.bg())}.</b> Ese método
 * <em>guarda</em> el color, y lo guarda una sola vez, cuando se construye la
 * pantalla. Como los frames son beans singleton de Spring, se construyen una vez
 * y viven toda la sesión: si después el usuario cambia de estación, la barra
 * oscura, el acento, los textos y hasta Olaz cambian —porque todos resuelven su
 * color al pintar— pero el fondo de página se queda con el de la estación que
 * hubiera en el arranque.
 *
 * <p>
 * El fallo apareció exactamente así: en las capturas de invierno todo era lila
 * salvo el fondo, que seguía siendo el crema de primavera. Y es de los peores de
 * detectar, porque el fondo es lo último que uno mira.
 *
 * <p>
 * La regla del sistema, otra vez: <b>no se guarda un color, se resuelve al
 * pintar.</b> Aquí eso significa rellenar en {@code paintComponent} en lugar de
 * confiar en la propiedad {@code background}.
 */
public class Page extends JPanel {

	private static final long serialVersionUID = 1L;

	/** 40 ms ≈ 25 fotogramas por segundo. Para algo que flota despacio, sobra. */
	private static final int MILIS_POR_FOTOGRAMA = 40;

	private final transient List<Particulas.Pieza> piezas = new ArrayList<>();
	private final transient Random azar = new Random();

	private transient Timer animacion;

	public Page(LayoutManager layout) {

		super(layout);

		// Opaco sí —queremos que pinte fondo—, pero el color no sale de la propiedad
		// background sino de paintComponent.
		setOpaque(true);
	}

	/**
	 * Arranca la animación cuando la pantalla entra de verdad en la ventana.
	 *
	 * <p>
	 * <b>Por qué aquí y no en el constructor.</b> Los frames son singleton de
	 * Spring: se construyen una vez y viven toda la sesión, pero solo uno está
	 * visible. Un temporizador arrancado en el constructor seguiría latiendo en las
	 * dieciséis pantallas ocultas, repintando ventanas que nadie ve.
	 * {@code addNotify} y {@code removeNotify} son los dos únicos puntos que Swing
	 * garantiza al entrar y salir de la jerarquía visible, así que son el sitio
	 * correcto para cualquier recurso que deba vivir solo mientras se ve.
	 */
	@Override
	public void addNotify() {

		super.addNotify();

		if (animacion == null) {
			animacion = new Timer(MILIS_POR_FOTOGRAMA, e -> avanzar());
			animacion.start();
		}
	}

	@Override
	public void removeNotify() {

		if (animacion != null) {
			animacion.stop();
			animacion = null;
		}

		piezas.clear();
		super.removeNotify();
	}

	/**
	 * Mueve las partículas y pide repintar <b>solo</b> lo que se ha movido.
	 *
	 * <p>
	 * Un {@code repaint()} a secas marcaría sucia la pantalla entera veinticinco
	 * veces por segundo, y con ella todas las filas del catálogo. Repintar la caja
	 * de cada pieza deja el área sucia en unos pocos cientos de píxeles.
	 */
	/** Crea las piezas que falten. Es seguro llamarlo tantas veces como haga falta. */
	private void sembrar() {

		if (getWidth() <= 0 || getHeight() <= 0) {
			return;
		}

		while (piezas.size() < Particulas.CUANTAS) {
			piezas.add(new Particulas.Pieza(azar, getWidth(), getHeight()));
		}
	}

	private void avanzar() {

		if (!isShowing() || getWidth() <= 0) {
			return;
		}

		sembrar();

		for (Particulas.Pieza pieza : piezas) {

			Rectangle sucio = pieza.avanzar(getWidth(), getHeight());
			repaint(sucio.x, sucio.y, sucio.width, sucio.height);
		}
	}

	@Override
	protected void paintComponent(Graphics g) {

		g.setColor(Theme.bg());
		g.fillRect(0, 0, getWidth(), getHeight());
	}

	/**
	 * Pinta las partículas <b>por encima</b> del contenido.
	 *
	 * <p>
	 * <b>Por qué aquí y no en {@code paintComponent}.</b> Ese método pinta solo el
	 * fondo del panel, y Swing dibuja los hijos <em>después</em>, así que las
	 * partículas quedaban tapadas por todo lo opaco: las tarjetas del catálogo, la
	 * cabecera, las listas. El efecto solo asomaba en los márgenes, y en pantallas
	 * llenas no se veía en absoluto. {@code paint} es el que ordena las tres fases
	 * —fondo, hijos y borde—, así que dibujar tras llamar a {@code super} deja las
	 * partículas delante de todo.
	 *
	 * <p>
	 * Ponerlas delante obliga a que sean discretas de verdad, y de ahí la
	 * atenuación por altura de {@link Particulas}: arriba, sobre la cabecera y el
	 * titular, se ven; abajo, sobre el texto que hay que leer, se apagan casi del
	 * todo.
	 */
	@Override
	public void paint(Graphics g) {

		super.paint(g);

		// Se siembra también aquí y no solo en el temporizador. Sin esto, el primer
		// fotograma sale sin partículas —el temporizador todavía no ha llegado a
		// disparar— y en una captura hecha nada más mostrar la ventana la estación
		// aparecía vacía. En la aplicación real duraba 40 ms y no se notaba; en la
		// herramienta de capturas era el resultado.
		sembrar();

		if (piezas.isEmpty()) {
			return;
		}

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		for (Particulas.Pieza pieza : piezas) {
			pieza.pintar(g2, getHeight());
		}

		g2.dispose();
	}
}
