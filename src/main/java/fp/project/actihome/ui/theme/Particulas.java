package fp.project.actihome.ui.theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.Random;

/**
 * La capa estacional de partículas: lo que flota despacio por el fondo de todas
 * las pantallas.
 *
 * <p>
 * <b>Qué cae en cada estación</b>, y en ningún caso lluvia —descartada por
 * decisión de producto, porque la lluvia sobre un catálogo de vacaciones dice
 * justo lo contrario de lo que se quiere vender—:
 *
 * <ul>
 * <li><b>Primavera</b>: vilanos y pétalos, mezclados. Son las dos cosas que
 * flotan en primavera y tienen movimientos distintos —el vilano se queda en el
 * aire y va a la deriva, el pétalo cae girando—, así que la mezcla se lee como
 * aire de primavera y no como un mismo elemento repetido.</li>
 * <li><b>Verano</b>: motas de luz, muy tenues, subiendo apenas. Es el polvo en
 * suspensión de un contraluz de tarde.</li>
 * <li><b>Otoño</b>: hojas, cayendo con giro.</li>
 * <li><b>Invierno</b>: nieve.</li>
 * </ul>
 *
 * <p>
 * <b>Densas arriba, casi invisibles abajo.</b> Son setenta piezas, que es
 * bastante, pero la atenuación por altura hace que el efecto se concentre en la
 * franja del titular —donde no hay nada que leer con detalle— y se apague sobre
 * el contenido. Así se nota de verdad sin llegar a competir con el texto.
 *
 * <p>
 * <b>El color sale de {@link Season#particula()} y no del acento.</b> Un pétalo
 * de cerezo es rosa, no verde; una hoja de otoño es más viva que el terracota de
 * la interfaz; y la nieve blanca sobre el fondo lila de invierno daba 1,18:1 de
 * contraste, o sea, era literalmente invisible.
 *
 * <p>
 * Esta clase es solo el <em>modelo</em> —posiciones, movimiento y dibujo de una
 * pieza—. Quién la anima y cuándo se repinta es cosa de
 * {@code ui.components.Page}, que es donde vive el ciclo de vida.
 */
public final class Particulas {

	/** Cuántas piezas hay a la vez. */
	public static final int CUANTAS = 70;

	/**
	 * Si las partículas se animan. Apagable desde Ajustes (Fase 7.6); por defecto
	 * activas, que es el comportamiento de siempre.
	 */
	private static boolean activas = true;

	/** Si las partículas están activas ahora mismo. */
	public static boolean activas() {
		return activas;
	}

	/** Activa o desactiva las partículas para toda la aplicación. */
	public static void activar(boolean valor) {
		activas = valor;
	}

	/**
	 * Cuánto se atenúan según bajan por la pantalla.
	 *
	 * <p>
	 * Arriba se ven bien —es la zona de la cabecera y el titular, donde no hay
	 * texto que leer con detalle— y según bajan se apagan hasta casi desaparecer
	 * sobre el contenido. La curva no es lineal sino cuadrática, así que la caída es
	 * rápida en el primer tercio y luego se aplana: eso concentra el efecto arriba
	 * en lugar de repartirlo.
	 */
	private static float atenuacionPorAltura(float y, int alto) {

		if (alto <= 0) {
			return 1f;
		}

		// Nada sobre la barra de navegación. No es un ajuste estético: al pintar las
		// partículas por delante del contenido, una hoja cruzando el selector de
		// estación convertía "VERANO" en "-VERANO", y eso se lee como una errata de la
		// aplicación, no como decoración. La franja de entrada evita además que las
		// piezas aparezcan de golpe al asomar por arriba.
		if (y < FIN_CABECERA) {
			return 0f;
		}

		if (y < FIN_ENTRADA) {
			return (y - FIN_CABECERA) / (FIN_ENTRADA - FIN_CABECERA);
		}

		float t = Math.max(0f, Math.min(1f, (y - FIN_ENTRADA) / Math.max(1, alto - FIN_ENTRADA)));
		float restante = 1f - t;

		return MINIMO_ABAJO + (1f - MINIMO_ABAJO) * restante * restante;
	}

	/** Alto de la cabecera, por debajo del cual no se pinta nada. */
	private static final float FIN_CABECERA = 68f;

	/** Dónde alcanzan su intensidad máxima, ya en la zona del titular. */
	private static final float FIN_ENTRADA = 150f;

	/** Lo que queda de opacidad al pie de la pantalla. */
	private static final float MINIMO_ABAJO = 0.12f;

	private Particulas() {
	}

	/** Una pieza flotando: dónde está, cómo se mueve y cómo se dibuja. */
	public static class Pieza {

		private final Random azar;

		private float x;
		private float y;
		private float tamano;
		private float velocidad;

		/** Amplitud y fase del vaivén lateral. */
		private float vaiven;
		private float fase;

		private float giro;
		private float velocidadDeGiro;
		private float opacidad;

		/** Opacidad ya atenuada por la altura, recalculada en cada pintado. */
		private float efectiva;

		/** Para primavera, que mezcla dos tipos. */
		private boolean esPetalo;

		public Pieza(Random azar, int ancho, int alto) {

			this.azar = azar;
			reiniciar(ancho, alto, true);
		}

		/**
		 * Recoloca la pieza arriba del todo, con propiedades nuevas.
		 *
		 * @param repartida si puede aparecer a cualquier altura. Cierto solo al
		 *                  arrancar: si no, las primeras piezas entrarían todas juntas
		 *                  por el borde superior, como un telón, en vez de estar ya
		 *                  repartidas por la pantalla
		 */
		public void reiniciar(int ancho, int alto, boolean repartida) {

			x = azar.nextFloat() * Math.max(1, ancho);
			y = repartida ? azar.nextFloat() * Math.max(1, alto) : -20 - azar.nextFloat() * 60;

			tamano = 4 + azar.nextFloat() * 8;
			velocidad = 0.12f + azar.nextFloat() * 0.30f;
			vaiven = 0.3f + azar.nextFloat() * 0.9f;
			fase = azar.nextFloat() * (float) Math.PI * 2;
			giro = azar.nextFloat() * (float) Math.PI * 2;
			velocidadDeGiro = (azar.nextFloat() - 0.5f) * 0.03f;
			opacidad = 0.34f + azar.nextFloat() * 0.30f;
			esPetalo = azar.nextBoolean();
		}

		/** Avanza un fotograma. Devuelve la zona que hay que repintar. */
		public Rectangle avanzar(int ancho, int alto) {

			Rectangle antes = limites();

			fase += 0.02f;
			x += (float) Math.sin(fase) * vaiven;
			giro += velocidadDeGiro;

			// El verano sube en vez de caer: el polvo en un contraluz asciende.
			y += Theme.estacion() == Season.VERANO ? -velocidad : velocidad;

			boolean fuera = Theme.estacion() == Season.VERANO ? y < -30 : y > alto + 30;

			if (fuera || x < -60 || x > ancho + 60) {
				reiniciar(ancho, alto, false);

				if (Theme.estacion() == Season.VERANO) {
					y = alto + 20;
				}
			}

			return antes.union(limites());
		}

		/**
		 * La caja que ocupa, con holgura.
		 *
		 * <p>
		 * Se repinta solo esto y no la pantalla entera. Es la diferencia entre pedirle
		 * a Swing dieciséis rectángulos diminutos por fotograma o pedirle que vuelva a
		 * dibujar el catálogo completo sesenta veces por segundo, que es lo que haría
		 * que la lista se moviera a trompicones al desplazarla.
		 */
		public Rectangle limites() {

			int lado = (int) Math.ceil(tamano * 2.4);
			return new Rectangle((int) x - lado, (int) y - lado, lado * 2, lado * 2);
		}

		public void pintar(Graphics2D g2, int altoDeLaPantalla) {

			Season estacion = Theme.estacion();

			// La opacidad efectiva se calcula en cada pintado, no se guarda: depende de
			// dónde esté la pieza ahora mismo, y la pieza se mueve.
			efectiva = opacidad * atenuacionPorAltura(y, altoDeLaPantalla);

			AffineTransform original = g2.getTransform();
			g2.translate(x, y);

			switch (estacion) {

				case PRIMAVERA:
					if (esPetalo) {
						g2.rotate(giro);
						flor(g2);
					} else {
						vilano(g2);
					}
					break;

				case VERANO:
					mota(g2);
					break;

				case OTONO:
					g2.rotate(giro);
					hoja(g2);
					break;

				case INVIERNO:
				default:
					copo(g2);
					break;
			}

			g2.setTransform(original);
		}

		/**
		 * Flor de cerezo: cinco pétalos redondos alrededor de un corazón.
		 *
		 * <p>
		 * <b>Cinco y no más, y sin contorno.</b> Una flor dibujada con detalle —nervios,
		 * borde, degradado— a doce píxeles no se lee como una flor, se lee como una
		 * mancha sucia; y repetida setenta veces satura el fondo. Aquí basta la
		 * silueta: cinco círculos solapados dan la forma reconocible al tamaño mínimo
		 * en el que una flor sigue siendo una flor.
		 *
		 * <p>
		 * El corazón va un poco más opaco que los pétalos. Es lo que evita que el
		 * conjunto se lea como cinco burbujas sueltas en lugar de como una sola pieza.
		 */
		private void flor(Graphics2D g2) {

			float alfa = Math.min(0.9f, efectiva * 1.6f);
			float radio = tamano * 0.34f;
			float centro = tamano * 0.30f;

			g2.setColor(tinta(Theme.particula(), alfa));

			for (int i = 0; i < 5; i++) {

				double angulo = i * Math.PI * 2 / 5;
				float cx = (float) Math.cos(angulo) * centro;
				float cy = (float) Math.sin(angulo) * centro;

				g2.fill(new Ellipse2D.Float(cx - radio, cy - radio, radio * 2, radio * 2));
			}

			g2.setColor(tinta(Theme.particula(), Math.min(1f, alfa * 1.35f)));
			g2.fill(new Ellipse2D.Float(-radio * 0.42f, -radio * 0.42f, radio * 0.84f, radio * 0.84f));
		}

		/**
		 * Vilano: el corazón y las hebras que salen de él.
		 *
		 * <p>
		 * Las hebras son lo que lo distingue de un copo de nieve, y por eso son finas
		 * y desiguales: un asterisco perfecto se lee como un símbolo, no como algo
		 * vivo.
		 */
		private void vilano(Graphics2D g2) {

			g2.setColor(tinta(Theme.particula(), efectiva * 0.95f));

			for (int i = 0; i < 7; i++) {

				double angulo = giro + i * Math.PI * 2 / 7;
				float largo = tamano * (0.75f + (i % 3) * 0.12f);

				g2.drawLine(0, 0, (int) Math.round(Math.cos(angulo) * largo),
						(int) Math.round(Math.sin(angulo) * largo));
			}

			g2.setColor(tinta(Theme.particula(), efectiva));
			g2.fill(new Ellipse2D.Float(-1.2f, -1.2f, 2.4f, 2.4f));
		}

		/** Mota de luz: un punto muy tenue, sin forma reconocible. */
		private void mota(Graphics2D g2) {

			g2.setColor(tinta(Theme.particula(), Math.min(0.9f, efectiva * 1.5f)));
			g2.fill(new Ellipse2D.Float(-tamano * 0.3f, -tamano * 0.3f, tamano * 0.6f, tamano * 0.6f));
		}

		/** Hoja: elipse con nervio, más ancha por un lado. */
		private void hoja(Graphics2D g2) {

			g2.setColor(tinta(Theme.particula(), Math.min(0.9f, efectiva * 1.35f)));
			g2.fill(new Ellipse2D.Float(-tamano * 0.6f, -tamano * 0.34f, tamano * 1.2f, tamano * 0.68f));

			g2.setColor(tinta(Theme.hdr(), efectiva * 0.5f));
			g2.drawLine((int) (-tamano * 0.5f), 0, (int) (tamano * 0.5f), 0);
		}

		/**
		 * Copo de nieve: seis brazos con dos ramitas cada uno.
		 *
		 * <p>
		 * <b>Seis es el número correcto y no un capricho de dibujo.</b> Los cristales
		 * de hielo crecen con simetría hexagonal, así que un copo de cinco o de ocho
		 * puntas se percibe como una estrella genérica. Las ramitas laterales son lo
		 * que lo separa de un asterisco.
		 *
		 * <p>
		 * Va con trazo redondeado y no relleno: a este tamaño una silueta maciza se
		 * convertiría en un hexágono borroso, mientras que las líneas conservan la
		 * estructura. El grosor se calcula a partir del tamaño de la pieza para que los
		 * copos grandes no salgan de alambre.
		 */
		private void copo(Graphics2D g2) {

			float alfa = Math.min(0.85f, efectiva * 1.9f);
			float brazo = tamano * 0.5f;

			g2.setColor(tinta(Theme.particula(), alfa));
			g2.setStroke(new BasicStroke(Math.max(0.9f, tamano * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

			Path2D copo = new Path2D.Float();

			for (int i = 0; i < 6; i++) {

				double angulo = giro + i * Math.PI / 3;
				float px = (float) Math.cos(angulo) * brazo;
				float py = (float) Math.sin(angulo) * brazo;

				copo.moveTo(0, 0);
				copo.lineTo(px, py);

				// Las dos ramitas salen a dos tercios del brazo, abiertas 40 grados.
				float bx = px * 0.62f;
				float by = py * 0.62f;
				float rama = brazo * 0.34f;

				for (int lado = -1; lado <= 1; lado += 2) {

					double abierto = angulo + lado * Math.PI / 4.5;

					copo.moveTo(bx, by);
					copo.lineTo(bx + (float) Math.cos(abierto) * rama, by + (float) Math.sin(abierto) * rama);
				}
			}

			g2.draw(copo);
		}

		private static Color tinta(Color base, float alfa) {

			int a = Math.max(0, Math.min(255, Math.round(alfa * 255)));
			return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
		}
	}
}
