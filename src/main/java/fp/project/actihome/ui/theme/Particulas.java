package fp.project.actihome.ui.theme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
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
 * <b>Cómo se mantiene discreta.</b> Es decoración de fondo y compite
 * directamente con el contenido, así que todo está calibrado para que no se
 * note salvo que uno se pare a mirar: pocas piezas, tamaños pequeños, opacidad
 * muy baja y velocidades lentas. La regla que se aplicó al ajustarlo: <b>si al
 * leer una ficha del catálogo la partícula te distrae, sobra</b>.
 *
 * <p>
 * Esta clase es solo el <em>modelo</em> —posiciones, movimiento y dibujo de una
 * pieza—. Quién la anima y cuándo se repinta es cosa de
 * {@code ui.components.Page}, que es donde vive el ciclo de vida.
 */
public final class Particulas {

	/** Cuántas piezas hay a la vez. Bajo a propósito: es fondo, no es el contenido. */
	public static final int CUANTAS = 16;

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

			tamano = 4 + azar.nextFloat() * 7;
			velocidad = 0.12f + azar.nextFloat() * 0.30f;
			vaiven = 0.3f + azar.nextFloat() * 0.9f;
			fase = azar.nextFloat() * (float) Math.PI * 2;
			giro = azar.nextFloat() * (float) Math.PI * 2;
			velocidadDeGiro = (azar.nextFloat() - 0.5f) * 0.03f;
			opacidad = 0.16f + azar.nextFloat() * 0.20f;
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

		public void pintar(Graphics2D g2) {

			Season estacion = Theme.estacion();

			AffineTransform original = g2.getTransform();
			g2.translate(x, y);

			switch (estacion) {

				case PRIMAVERA:
					if (esPetalo) {
						g2.rotate(giro);
						petalo(g2);
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

		/** Pétalo: una elipse asimétrica, en el acento de la estación aclarado. */
		private void petalo(Graphics2D g2) {

			g2.setColor(tinta(Theme.acc(), opacidad));
			g2.fill(new Ellipse2D.Float(-tamano * 0.55f, -tamano * 0.32f, tamano * 1.1f, tamano * 0.64f));
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

			g2.setColor(tinta(Theme.mut(), opacidad * 0.9f));

			for (int i = 0; i < 7; i++) {

				double angulo = giro + i * Math.PI * 2 / 7;
				float largo = tamano * (0.75f + (i % 3) * 0.12f);

				g2.drawLine(0, 0, (int) Math.round(Math.cos(angulo) * largo),
						(int) Math.round(Math.sin(angulo) * largo));
			}

			g2.setColor(tinta(Theme.mut(), opacidad));
			g2.fill(new Ellipse2D.Float(-1.2f, -1.2f, 2.4f, 2.4f));
		}

		/** Mota de luz: un punto muy tenue, sin forma reconocible. */
		private void mota(Graphics2D g2) {

			g2.setColor(tinta(Theme.acc(), opacidad * 0.75f));
			g2.fill(new Ellipse2D.Float(-tamano * 0.18f, -tamano * 0.18f, tamano * 0.36f, tamano * 0.36f));
		}

		/** Hoja: elipse con nervio, más ancha por un lado. */
		private void hoja(Graphics2D g2) {

			g2.setColor(tinta(Theme.acc(), opacidad));
			g2.fill(new Ellipse2D.Float(-tamano * 0.6f, -tamano * 0.34f, tamano * 1.2f, tamano * 0.68f));

			g2.setColor(tinta(Theme.hdr(), opacidad * 0.5f));
			g2.drawLine((int) (-tamano * 0.5f), 0, (int) (tamano * 0.5f), 0);
		}

		/** Copo: círculo blanco blando, sin puntas. */
		private void copo(Graphics2D g2) {

			g2.setColor(tinta(Color.WHITE, Math.min(0.55f, opacidad * 2.2f)));
			g2.fill(new Ellipse2D.Float(-tamano * 0.28f, -tamano * 0.28f, tamano * 0.56f, tamano * 0.56f));
		}

		private static Color tinta(Color base, float alfa) {

			int a = Math.max(0, Math.min(255, Math.round(alfa * 255)));
			return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
		}
	}
}
