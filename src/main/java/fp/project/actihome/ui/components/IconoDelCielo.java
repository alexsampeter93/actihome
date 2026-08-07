package fp.project.actihome.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

import javax.swing.JComponent;

import fp.project.actihome.model.services.CieloWmo;
import fp.project.actihome.ui.theme.Theme;

/**
 * El estado del cielo, dibujado (F18).
 *
 * <p>
 * <b>Se dibuja y no se escribe, y esa es una regla del proyecto, no una
 * preferencia.</b> De las fuentes empaquetadas solo se puede dar por hecho el
 * repertorio latino básico, y ni Fraunces ni Archivo tienen símbolos
 * meteorológicos. Cuando a Swing le falta un glifo <b>no avisa</b>: pinta un
 * rectángulo vacío, que junto a un texto correcto se lee como un icono raro y no
 * como un fallo. Así se publicaron el {@code ⇄} de "Intercambiar" y el
 * {@code ✓} de "Check-in realizado", los dos corregidos después. Aquí ni
 * siquiera hacía falta comprobarlo con {@code MedirGlifos}: los emoji de sol y
 * nube no están en ninguna fuente de texto.
 *
 * <p>
 * Es la tercera pieza dibujada del vocabulario, después de {@link StarRating} y
 * {@code SwapGlyph}.
 *
 * <p>
 * <b>Los colores salen de la estación activa</b>, como todo lo demás: el sol es
 * el acento, las nubes y la lluvia van en el gris secundario. No se guardan en
 * un campo, se resuelven en cada pintado, así que el icono cambia de color al
 * cambiar de estación sin estar suscrito a nada.
 *
 * <p>
 * El tamaño se declara con preferido <b>y mínimo</b>: un componente propio que
 * define solo el preferido declara un mínimo de cero y el layout lo aplasta en
 * cuanto falta sitio. Es la trampa de Swing que no da error.
 */
public class IconoDelCielo extends JComponent {

	private static final long serialVersionUID = 1L;

	private final transient CieloWmo cielo;
	private final int lado;

	/**
	 * Si en ese punto del mapa es de día.
	 *
	 * <p>
	 * Cuando es de noche, el disco del sol se sustituye por una luna. <b>Solo
	 * cambia el astro</b>: la nube, la lluvia, la nieve y el rayo se dibujan igual,
	 * porque llover de noche se parece bastante a llover de día.
	 */
	private final boolean esDeDia;

	/** Icono de un día de previsión, donde no se sabe si será de día o de noche. */
	public IconoDelCielo(CieloWmo cielo, int lado) {
		this(cielo, lado, true);
	}

	public IconoDelCielo(CieloWmo cielo, int lado, boolean esDeDia) {

		this.cielo = cielo;
		this.lado = lado;
		this.esDeDia = esDeDia;

		setOpaque(false);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(lado, lado);
	}

	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	@Override
	protected void paintComponent(Graphics g) {

		if (cielo == null || cielo == CieloWmo.DESCONOCIDO) {
			return;
		}

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// El trazo escala con el icono. Un grosor fijo se ve grueso a 16 puntos y
		// anémico a 40, y este componente se usa en los dos tamaños.
		g2.setStroke(new BasicStroke(Math.max(1f, lado / 14f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		switch (cielo) {

		case DESPEJADO:
			astro(g2, 0.5, 0.48, 0.30);
			break;

		case NUBLADO:
			astro(g2, 0.36, 0.38, 0.18);
			nube(g2, 0.62);
			break;

		case NIEBLA:
			niebla(g2);
			break;

		case LLUVIA:
			nube(g2, 0.48);
			gotas(g2);
			break;

		case NIEVE:
			nube(g2, 0.48);
			copos(g2);
			break;

		case TORMENTA:
			nube(g2, 0.48);
			rayo(g2);
			break;

		default:
			break;
		}

		g2.dispose();
	}

	/**
	 * Un disco con rayos.
	 *
	 * <p>
	 * Todas las medidas son fracciones del lado y no píxeles: así el mismo código
	 * dibuja el icono de la tira compacta y el de la ficha grande, y ninguno se
	 * deforma. Es la misma idea que "ningún tamaño que dependa de texto puede ser
	 * una constante", aplicada a un dibujo.
	 */
	/** El sol o la luna, según la hora que sea allí. */
	private void astro(Graphics2D g2, double cx, double cy, double radio) {

		if (esDeDia) {
			sol(g2, cx, cy, radio);
		} else {
			luna(g2, cx, cy, radio);
		}
	}

	/**
	 * Una luna en cuarto creciente.
	 *
	 * <p>
	 * <b>Se dibuja recortando un disco de otro</b>, que es como se hace una luna
	 * sin curvas a mano: se rellena el círculo entero y encima se pinta otro
	 * desplazado, del color del fondo. El truco es que ese segundo disco tiene que
	 * ir del color de la página, no transparente — {@code Graphics2D} pinta encima,
	 * no borra.
	 *
	 * <p>
	 * Por eso el color de recorte sale de {@link Theme#bg()}: si el icono se
	 * colocara algún día sobre una superficie blanca en vez de sobre el fondo de
	 * página, este es el único punto que habría que revisar. Queda anotado.
	 */
	private void luna(Graphics2D g2, double cx, double cy, double radio) {

		double x = cx * lado;
		double y = cy * lado;
		double r = radio * lado * 0.62;

		g2.setColor(Theme.accText());
		g2.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));

		g2.setColor(Theme.bg());
		g2.fill(new Ellipse2D.Double(x - r * 1.45, y - r * 1.25, r * 2, r * 2));
	}

	private void sol(Graphics2D g2, double cx, double cy, double radio) {

		g2.setColor(Theme.acc());

		double x = cx * lado;
		double y = cy * lado;
		double r = radio * lado;

		g2.fill(new Ellipse2D.Double(x - r * 0.62, y - r * 0.62, r * 1.24, r * 1.24));

		for (int i = 0; i < 8; i++) {

			double angulo = Math.PI * i / 4;
			double dentro = r * 0.85;
			double fuera = r * 1.15;

			g2.drawLine((int) Math.round(x + Math.cos(angulo) * dentro), (int) Math.round(y + Math.sin(angulo) * dentro),
					(int) Math.round(x + Math.cos(angulo) * fuera), (int) Math.round(y + Math.sin(angulo) * fuera));
		}
	}

	/** Tres discos solapados y una base recta: la silueta de nube de toda la vida. */
	private void nube(Graphics2D g2, double baseY) {

		g2.setColor(Theme.mut());

		double y = baseY * lado;
		double alto = lado * 0.30;

		g2.fill(new Ellipse2D.Double(lado * 0.16, y - alto * 0.75, alto * 1.15, alto * 1.15));
		g2.fill(new Ellipse2D.Double(lado * 0.38, y - alto * 1.10, alto * 1.45, alto * 1.45));
		g2.fill(new Ellipse2D.Double(lado * 0.60, y - alto * 0.70, alto * 1.05, alto * 1.05));
		g2.fillRect((int) (lado * 0.20), (int) (y - alto * 0.10), (int) (lado * 0.58), (int) (alto * 0.55));
	}

	private void gotas(Graphics2D g2) {

		g2.setColor(Theme.accText());

		for (int i = 0; i < 3; i++) {

			int x = (int) (lado * (0.30 + i * 0.18));

			g2.drawLine(x, (int) (lado * 0.70), (int) (x - lado * 0.05), (int) (lado * 0.88));
		}
	}

	private void copos(Graphics2D g2) {

		g2.setColor(Theme.mut());

		double r = lado * 0.05;

		for (int i = 0; i < 3; i++) {

			double x = lado * (0.28 + i * 0.19);

			g2.fill(new Ellipse2D.Double(x - r, lado * 0.76 - r, r * 2, r * 2));
		}
	}

	/**
	 * El rayo de la tormenta.
	 *
	 * <p>
	 * <b>Va en {@code accText()} y no en {@code acc()}, y esto es el ADR-008 otra
	 * vez.</b> La primera versión usaba {@code acc()}, el acento de relleno, y en
	 * verano eso es el amarillo {@code #E0AC1B}: sobre el crema de la página el
	 * rayo quedaba casi invisible. {@code acc} solo tiene que contrastar con la
	 * etiqueta que lleva encima; esto se dibuja <em>sobre el fondo</em>, así que su
	 * requisito es el del texto, y para eso está {@code accText}.
	 *
	 * <p>
	 * El disco del sol sí se queda en {@code acc()}: es una superficie rellena, que
	 * es exactamente el papel de ese token.
	 *
	 * <p>
	 * <b>Y {@code MedirContraste} no lo habría cazado</b>: mide combinaciones de
	 * color declaradas, no lo que un componente decide pintar. Otra vez la misma
	 * lección — una comprobación automática solo protege de la clase de fallo que
	 * sabe buscar.
	 */
	private void rayo(Graphics2D g2) {

		g2.setColor(Theme.accText());

		int[] xs = { (int) (lado * 0.50), (int) (lado * 0.38), (int) (lado * 0.48), (int) (lado * 0.36),
				(int) (lado * 0.58), (int) (lado * 0.47) };
		int[] ys = { (int) (lado * 0.62), (int) (lado * 0.82), (int) (lado * 0.82), (int) (lado * 0.98),
				(int) (lado * 0.74), (int) (lado * 0.74) };

		g2.fillPolygon(xs, ys, xs.length);
	}

	/** Tres bandas horizontales: es como se representa la niebla en cualquier parte. */
	private void niebla(Graphics2D g2) {

		g2.setColor(Theme.mut());

		for (int i = 0; i < 3; i++) {

			int y = (int) (lado * (0.36 + i * 0.16));
			int margen = (int) (lado * (i == 1 ? 0.12 : 0.20));

			g2.drawLine(margen, y, lado - margen, y);
		}
	}

	/** El color base, útil para quien quiera un fondo coherente detrás. */
	public static Color tinta() {
		return Theme.mut();
	}
}
