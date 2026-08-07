package fp.project.actihome.ui.components;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;

import fp.project.actihome.model.entities.Amenity;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Los siete iconos de comodidades, dibujados con geometría.
 *
 * <h2>Por qué dibujados y no de una librería</h2>
 *
 * <p>
 * <b>No es por evitar una dependencia: técnicamente cualquiera de las librerías
 * conocidas funcionaría.</b> Son SVG y {@code flatlaf-extras} —que este proyecto
 * ya declara— los carga y los recolorea sin problema. El motivo es otro.
 *
 * <p>
 * Lucide, Heroicons, Feather y Tabler comparten la misma receta: trazo de 2
 * puntos, esquinas redondeadas, retícula de 24. No es la estética de la
 * inteligencia artificial —es anterior, viene de Tailwind y de 2020— pero sí es
 * <b>la estética que todo el mundo ha visto ya cien veces</b>, y eso es lo que
 * hace que una interfaz parezca de nadie. Este proyecto ya rechazó una
 * tipografía por exactamente esta razón (Manrope, Fase 8.2); meter aquí el
 * repertorio canónico de las plantillas sería volver a entrar por la puerta de
 * atrás.
 *
 * <p>
 * <b>Y lo importante: el problema nunca fue ser vistoso.</b> Un icono que se
 * mueve no es sospechoso por moverse; lo es cuando podría estar igual en la app
 * de un banco, en un gestor de facturas y aquí. Estos se dibujan con el grosor
 * de línea del sistema y se encienden con el acento de la estación activa, así
 * que no pueden estar en ningún otro sitio.
 *
 * <h2>Cómo están hechos</h2>
 *
 * <p>
 * <b>Todas las medidas son fracciones del lado</b>, nunca píxeles. Es la misma
 * regla que ya siguen {@link StarRating}, {@link SwapGlyph} y
 * {@link IconoDelCielo}: así el mismo código sirve a 16 puntos y a 40, y sobre
 * todo sigue siendo correcto cuando el escalado de Windows agranda la fuente y
 * el icono tiene que crecer con ella.
 *
 * <p>
 * <b>El trazo es de 1,4 puntos y no de 2.</b> Dos es el grosor de las librerías
 * citadas y se lee redondeado y amable; uno y medio es el grosor del dibujo
 * técnico y de la infografía de prensa, que es de donde viene todo lo demás en
 * este sistema —las hairlines, las versalitas, la serif de contraste alto—.
 *
 * <h2>La animación</h2>
 *
 * <p>
 * Cada icono tiene <b>una sola parte que se mueve</b>, y siempre la que explica
 * qué es: los arcos del wifi se encienden de dentro afuera, la gota de la
 * piscina cae, el coche entra en su plaza. Animar el icono entero —girarlo,
 * escalarlo— es lo que hace que un adorno parezca un adorno; animar lo que
 * significa hace que parezca que el icono <em>funciona</em>.
 *
 * <p>
 * El movimiento lo empuja quien contiene el icono, con {@link #setEncendido},
 * por la misma razón que la foto de una ficha no escucha su propio ratón: el
 * cursor está sobre la etiqueta, no sobre el dibujo de doce puntos.
 */
public class IconoDeComodidad extends JComponent {

	private static final long serialVersionUID = 1L;

	/**
	 * Grosor del trazo, como fracción del lado.
	 *
	 * <p>
	 * <b>Empezó en 0,09 y se bajó a 0,065 mirando la lámina de
	 * {@code MirarIconos}.</b> Con el grosor anterior y remates redondeados el
	 * resultado era exactamente lo que este archivo dice evitar: el aspecto de
	 * Lucide y Feather. Y no se vio en el catálogo, donde miden dieciséis puntos y
	 * todo parece igual — se vio a doscientos. Un icono dibujado a mano hay que
	 * juzgarlo grande aunque se use pequeño.
	 */
	private static final double GROSOR = 0.065;

	private final transient Amenity comodidad;

	/** Cuánto está encendido, de 0 a 1. */
	private transient double encendido;

	public IconoDeComodidad(Amenity comodidad) {

		this.comodidad = comodidad;

		int lado = lado();
		setPreferredSize(new Dimension(lado, lado));

		// Si se declara el preferido hay que declarar el mínimo, o el layout lo aplasta
		// a cero en cuanto falte sitio sin que nada avise.
		setMinimumSize(new Dimension(lado, lado));

		setToolTipText(comodidad.etiqueta());
	}

	/**
	 * El lado del icono, derivado de la línea de texto que acompaña.
	 *
	 * <p>
	 * Un icono junto a una etiqueta debe medir aproximadamente lo que mide una
	 * mayúscula de esa etiqueta, más un poco. Fijarlo en píxeles es la regla que más
	 * veces se ha incumplido en este proyecto: con el escalado del sistema al 150 %
	 * la letra crece y el icono se queda de juguete.
	 */
	private static int lado() {
		return Math.max(14, Math.round(Typography.sans(Typography.BODY_SM).getSize() * 1.45f));
	}

	/** Enciende el icono. Lo mueve quien lo contiene; ver la nota de clase. */
	public void setEncendido(double encendido) {

		this.encendido = encendido;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		double lado = Math.min(getWidth(), getHeight());
		double x = (getWidth() - lado) / 2;
		double y = (getHeight() - lado) / 2;

		g2.translate(x, y);

		// El color se resuelve al pintar y no se guarda: así el icono sigue a la
		// estación activa sin suscribirse a nada, como el resto del vocabulario visual.
		// Apagado usa el texto secundario; encendido, el acento de TEXTO —accText y no
		// acc—, porque esto es un trazo fino sobre el fondo claro y tiene los mismos
		// requisitos de contraste que una letra (ADR-008).
		g2.setColor(fp.project.actihome.ui.theme.Animacion.mezclar(Theme.mut(), Theme.accText(), encendido));

		// **Remates rectos y uniones en ángulo, no redondeados.** Es lo que separa un
		// icono de dibujo técnico de uno de plantilla: CAP_ROUND + JOIN_ROUND es la
		// combinación exacta de Lucide y Feather, y con ella estos siete salían
		// indistinguibles de aquellos por mucho que el trazado fuera propio. El resto
		// del sistema —hairlines, versalitas, radios de 2 a 4— es anguloso; los iconos
		// también deben serlo.
		g2.setStroke(new BasicStroke((float) (lado * GROSOR), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));

		switch (comodidad) {
		case WIFI:
			wifi(g2, lado);
			break;
		case POOL:
			piscina(g2, lado);
			break;
		case TV:
			television(g2, lado);
			break;
		case PARKING:
			parking(g2, lado);
			break;
		case AIR_CONDITIONING:
			aire(g2, lado);
			break;
		case BREAKFAST:
			desayuno(g2, lado);
			break;
		case PETS:
			mascotas(g2, lado);
			break;
		default:
			break;
		}

		g2.dispose();
	}

	/**
	 * Tres arcos concéntricos y un punto, que se encienden <b>de dentro afuera</b>.
	 *
	 * <p>
	 * El orden importa: es el de una señal que se propaga, y es el único que se lee
	 * como "hay cobertura". De fuera adentro parecería que la señal se apaga.
	 */
	private void wifi(Graphics2D g2, double lado) {

		double centroX = lado / 2;
		double base = lado * 0.80;

		for (int i = 0; i < 3; i++) {

			double radio = lado * (0.20 + 0.17 * i);

			// Cada arco entra con retraso sobre el anterior: es lo que produce la
			// sensación de onda que se propaga hacia fuera.
			//
			// **Los tres se dibujan SIEMPRE.** La primera versión se los saltaba mientras
			// el icono estuviera apagado, y en reposo el wifi era un punto suelto sin
			// arcos: irreconocible. Una animación puede cambiar la intensidad de una
			// pieza o moverla, pero no puede decidir si la pieza existe — el estado de
			// reposo es el que se ve el 99 % del tiempo.
			double propio = Math.max(0, Math.min(1, (encendido - i * 0.25) / 0.5));

			g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
					(float) (0.55 + 0.45 * propio)));

			g2.draw(new Arc2D.Double(centroX - radio, base - radio, radio * 2, radio * 2, 35, 110, Arc2D.OPEN));
		}

		g2.setComposite(java.awt.AlphaComposite.SrcOver);

		double punto = lado * 0.10;
		g2.fill(new Ellipse2D.Double(centroX - punto / 2, base - punto / 2, punto, punto));
	}

	/**
	 * Dos ondas de agua y una gota que <b>cae</b> al encenderse.
	 *
	 * <p>
	 * La gota no está en el icono en reposo: aparece arriba y baja hasta la
	 * superficie. Un icono en el que sobra un elemento cuando nadie lo mira es un
	 * icono peor; uno donde ese elemento <em>llega</em> cuenta algo.
	 */
	private void piscina(Graphics2D g2, double lado) {

		// La escalera, y es ella la que hace el icono. Con solo ondas esto era "agua",
		// no "piscina": lo comprobamos en la lámina de MirarIconos, donde tres líneas
		// onduladas podían ser igualmente un río, el mar o la señal de humedad. Dos
		// largueros y dos peldaños bastan para que solo se lea de una manera.
		double largueroIzq = lado * 0.54;
		double largueroDer = lado * 0.84;

		g2.draw(new Line2D.Double(largueroIzq, lado * 0.60, largueroIzq, lado * 0.28));
		g2.draw(new Line2D.Double(largueroDer, lado * 0.60, largueroDer, lado * 0.28));

		// El arco que une los dos largueros arriba, que es el asidero.
		g2.draw(new Arc2D.Double(largueroIzq, lado * 0.16, largueroDer - largueroIzq, lado * 0.24, 0, 180, Arc2D.OPEN));

		for (int i = 0; i < 2; i++) {
			double y = lado * (0.38 + 0.13 * i);
			g2.draw(new Line2D.Double(largueroIzq, y, largueroDer, y));
		}

		// El agua. **Las ondas se dibujan UNA sola vez y con el desplazamiento ya
		// aplicado.** El primer intento pintaba las ondas quietas y encima otra copia
		// desplazada, y a doscientos puntos eso no se veía como movimiento sino como
		// una impresión mal registrada, con dos trazos fantasma. Cuando algo se mueve,
		// se mueve: no se le pinta al lado dónde estaba.
		for (int i = 0; i < 2; i++) {

			double y = lado * (0.70 + 0.16 * i);
			double empuje = lado * 0.05 * encendido * (i == 0 ? 1 : -1);

			Path2D onda = new Path2D.Double();
			onda.moveTo(lado * 0.06 + empuje, y);
			onda.curveTo(lado * 0.22 + empuje, y - lado * 0.09, lado * 0.34 + empuje, y + lado * 0.09,
					lado * 0.48 + empuje, y);
			onda.curveTo(lado * 0.62 + empuje, y - lado * 0.09, lado * 0.76 + empuje, y + lado * 0.09,
					lado * 0.90 + empuje, y);

			g2.draw(onda);
		}
	}

	/** Pantalla y peana. Al encenderse aparece una línea de brillo dentro. */
	private void television(Graphics2D g2, double lado) {

		g2.draw(new RoundRectangle2D.Double(lado * 0.10, lado * 0.20, lado * 0.80, lado * 0.52, lado * 0.10,
				lado * 0.10));

		g2.draw(new Line2D.Double(lado * 0.33, lado * 0.86, lado * 0.67, lado * 0.86));
		g2.draw(new Line2D.Double(lado * 0.50, lado * 0.72, lado * 0.50, lado * 0.86));

		if (encendido > 0) {

			// **La pantalla se enciende**: se rellena de color muy rebajado, de abajo
			// arriba. Antes había un destello diagonal que barría la pantalla y en la
			// lámina se leía como un arañazo sobre el cristal, no como una imagen. Un
			// relleno que sube es inequívoco y no añade ninguna forma nueva que
			// interpretar.
			g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
					(float) (0.22 * encendido)));

			double margen = lado * GROSOR;
			double altoTotal = lado * 0.52 - 2 * margen;
			double alto = altoTotal * encendido;

			g2.fill(new java.awt.geom.Rectangle2D.Double(lado * 0.10 + margen,
					lado * 0.20 + margen + (altoTotal - alto), lado * 0.80 - 2 * margen, alto));

			g2.setComposite(java.awt.AlphaComposite.SrcOver);
		}
	}

	/**
	 * La "P" de aparcamiento dentro de un cuadrado, y un coche que entra.
	 *
	 * <p>
	 * Se dibuja la letra a mano con dos trazos en vez de escribirla con la fuente:
	 * una "P" tipográfica dentro de un icono de línea trae consigo el contraste y
	 * los remates de la familia, y a doce puntos eso se ve como una mancha.
	 */
	private void parking(Graphics2D g2, double lado) {

		g2.draw(new RoundRectangle2D.Double(lado * 0.14, lado * 0.14, lado * 0.72, lado * 0.72, lado * 0.14,
				lado * 0.14));

		// El asta y el ojo de la P.
		//
		// **La geometría del arco no es libre: sus dos extremos tienen que caer sobre
		// el asta.** Un arco de 90 a -90 grados recorre la mitad DERECHA de su elipse,
		// así que empieza y acaba en el centro horizontal de esa elipse — y por tanto
		// ese centro debe coincidir con la x del asta. La primera versión situaba la
		// elipse siete centésimas más a la derecha y el ojo de la P quedaba despegado
		// del asta, con dos muñones a la vista. A dieciséis puntos parecía una mancha;
		// a doscientos era una letra rota.
		double asta = lado * 0.40;
		double anchoOjo = lado * 0.30;

		g2.draw(new Line2D.Double(asta, lado * 0.30, asta, lado * 0.70));
		g2.draw(new Arc2D.Double(asta - anchoOjo / 2, lado * 0.30, anchoOjo, lado * 0.24, 90, -180, Arc2D.OPEN));

		if (encendido > 0) {

			// **Las marcas de la plaza**, que se dibujan de fuera adentro como si el coche
			// acabara de aparcar. Sustituyen a un punto que subía por el asta y que en la
			// lámina se veía como una errata encima de la letra.
			g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
					(float) (0.75 * encendido)));

			double avance = lado * 0.18 * encendido;

			g2.draw(new Line2D.Double(lado * 0.24, lado * 0.76, lado * 0.24 + avance, lado * 0.76));
			g2.draw(new Line2D.Double(lado * 0.76, lado * 0.76, lado * 0.76 - avance, lado * 0.76));

			g2.setComposite(java.awt.AlphaComposite.SrcOver);
		}
	}

	/**
	 * Un aparato de aire y tres chorros que <b>bajan</b> al encenderse.
	 *
	 * <p>
	 * Los tres salen escalonados, no a la vez: un flujo simultáneo se lee como tres
	 * rayas, y escalonado se lee como aire moviéndose.
	 */
	private void aire(Graphics2D g2, double lado) {

		g2.draw(new RoundRectangle2D.Double(lado * 0.10, lado * 0.16, lado * 0.80, lado * 0.34, lado * 0.09,
				lado * 0.09));

		g2.draw(new Line2D.Double(lado * 0.20, lado * 0.40, lado * 0.80, lado * 0.40));

		// Los tres chorros existen siempre: en reposo son cortos, y al encenderse se
		// alargan escalonados. Igual que en el wifi, lo que anima es la longitud, no la
		// existencia — sin ellos el icono en reposo sería una caja sin más.
		for (int i = 0; i < 3; i++) {

			double x = lado * (0.28 + 0.22 * i);
			double propio = Math.max(0, Math.min(1, (encendido - i * 0.18) / 0.6));

			g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
					(float) (0.6 + 0.4 * propio)));
			g2.draw(new Line2D.Double(x, lado * 0.58, x, lado * (0.58 + 0.12 + 0.16 * propio)));
		}

		g2.setComposite(java.awt.AlphaComposite.SrcOver);
	}

	/**
	 * Una taza con su plato, y el vapor que <b>sube</b> al encenderse.
	 *
	 * <p>
	 * Una taza es mejor símbolo de desayuno que un cruasán o unos huevos: no
	 * presupone qué se sirve, y a doce puntos una silueta redonda con asa se
	 * reconoce donde un bollo se convierte en una mancha.
	 */
	private void desayuno(Graphics2D g2, double lado) {

		g2.draw(new RoundRectangle2D.Double(lado * 0.16, lado * 0.42, lado * 0.48, lado * 0.36, lado * 0.10,
				lado * 0.14));

		// El asa.
		g2.draw(new Arc2D.Double(lado * 0.58, lado * 0.48, lado * 0.24, lado * 0.22, 90, -180, Arc2D.OPEN));

		// El plato.
		g2.draw(new Line2D.Double(lado * 0.10, lado * 0.86, lado * 0.72, lado * 0.86));

		if (encendido > 0) {

			g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
					(float) Math.min(1, encendido)));

			for (int i = 0; i < 2; i++) {

				double x = lado * (0.28 + 0.18 * i);
				double alto = lado * 0.20 * encendido;

				Path2D vapor = new Path2D.Double();
				vapor.moveTo(x, lado * 0.36);
				vapor.curveTo(x + lado * 0.06, lado * 0.36 - alto * 0.5, x - lado * 0.06, lado * 0.36 - alto * 0.7,
						x, lado * 0.36 - alto);

				g2.draw(vapor);
			}

			g2.setComposite(java.awt.AlphaComposite.SrcOver);
		}
	}

	/**
	 * Una huella: la almohadilla y cuatro dedos.
	 *
	 * <p>
	 * Al encenderse los dedos se <b>separan ligeramente</b> de la almohadilla, como
	 * una pata que se apoya. Es el movimiento más pequeño de los siete y es
	 * deliberado: una huella es una forma reconocible al instante y cualquier gesto
	 * grande la desdibujaría.
	 */
	private void mascotas(Graphics2D g2, double lado) {

		double apertura = lado * 0.05 * encendido;

		// Los cuatro dedos, en arco. Los dos centrales van más arriba que los
		// exteriores, que es lo que hace que se lea como una pata y no como una flor.
		double[][] dedos = { { 0.22, 0.40 }, { 0.40, 0.28 }, { 0.60, 0.28 }, { 0.78, 0.40 } };
		double radioDedo = lado * 0.15;

		for (double[] dedo : dedos) {

			double centroX = lado * dedo[0] + (dedo[0] < 0.5 ? -apertura : apertura);
			double centroY = lado * dedo[1] - apertura;

			g2.fill(new Ellipse2D.Double(centroX - radioDedo / 2, centroY - radioDedo / 2, radioDedo, radioDedo));
		}

		// La almohadilla.
		g2.fill(new RoundRectangle2D.Double(lado * 0.28, lado * 0.54, lado * 0.44, lado * 0.32, lado * 0.22,
				lado * 0.26));
	}
}
