package fp.project.actihome.ui.components;

import java.awt.Graphics2D;
import java.awt.geom.Line2D;

/**
 * Las cuatro esquinas de un marco, que <b>se cierran</b> hasta formar el
 * rectángulo entero.
 *
 * <p>
 * <b>Son marcas de corte, el recurso más reconocible de la imprenta.</b> En un
 * pliego sin cortar, las escuadras de las esquinas indican dónde termina la
 * página: cuatro ángulos que sugieren un rectángulo sin dibujarlo. Es una forma
 * que cualquiera ha visto mil veces y que, sin embargo, <b>no aparece en ninguna
 * interfaz de plantilla</b>, porque no viene del software: viene del taller.
 *
 * <h2>Por qué esta forma y no otra</h2>
 *
 * <p>
 * El problema de un contorno completo es que ya está todo dicho: la única manera
 * de que responda al ratón es cambiarle el color, que es lo que hace todo el
 * mundo y por eso todos los botones de contorno del mundo se parecen. Unas
 * escuadras, en cambio, tienen <b>a dónde ir</b>: al pasar el ratón crecen hasta
 * juntarse y el marco se completa solo. El control no se enciende, <b>se
 * termina de dibujar</b>.
 *
 * <p>
 * Y encaja con lo que el sistema ya era: líneas de un punto, sin sombras
 * difusas, radios pequeños, precisión antes que amabilidad. No añade un lenguaje
 * nuevo — usa el que había en el sitio donde no estaba.
 *
 * <h2>Es un vocabulario, no un adorno de un botón</h2>
 *
 * <p>
 * Lo usan el botón secundario y el buscador, y esa repetición es intencionada:
 * en este sistema, <b>unas escuadras que se cierran significan "esto responde"</b>.
 * Un gesto que aparece una sola vez es una ocurrencia; el mismo gesto en tres
 * sitios distintos es una decisión de diseño, y es lo que hace que una interfaz
 * se lea como pensada y no como ensamblada.
 */
public final class Escuadras {

	/**
	 * Por debajo de esto la escuadra no se lee como tal, es un punto suelto.
	 *
	 * <p>
	 * Los brazos son <b>proporcionales y no un número fijo de puntos</b>, y la
	 * primera versión se equivocó ahí. Con once puntos fijos, un botón de 44 de alto
	 * llevaba unas escuadras razonables y uno de 200 de ancho llevaba cuatro marcas
	 * diminutas perdidas en las esquinas, con el rótulo flotando en medio de la nada:
	 * no se leía como un botón. Una marca de corte tiene que guardar relación con lo
	 * que enmarca, igual que en un pliego — si no, deja de señalar y decora.
	 */
	private static final double BRAZO_MINIMO = 9;

	/** El brazo pedido, con suelo para que se lea y techo para que no cierre solo. */
	private static double acotar(double pedido, double techo) {
		return Math.max(Math.min(BRAZO_MINIMO, techo), Math.min(pedido, techo));
	}

	private Escuadras() {
	}

	/**
	 * Dibuja las escuadras de un rectángulo con el color y el trazo ya puestos en
	 * {@code g2}.
	 *
	 * <p>
	 * <b>El avance no interpola el color, interpola la longitud del brazo.</b> A
	 * cero son cuatro ángulos cortos; a uno, cada brazo llega hasta la mitad del
	 * lado y se encuentra con el de la esquina contigua, de modo que el marco queda
	 * cerrado sin haber dibujado nunca un rectángulo. Que las dos mitades se toquen
	 * exactamente en el centro del lado es lo que hace que el cierre se vea limpio y
	 * no como dos rayas que casi llegan.
	 *
	 * @param avance de 0 (escuadras sueltas) a 1 (marco completo)
	 */
	public static void pintar(Graphics2D g2, double x, double y, double ancho, double alto, double avance) {

		if (ancho < 4 || alto < 4) {
			return;
		}

		double v = Math.max(0, Math.min(1, avance));

		// **Un brazo por eje, y no uno solo medido contra el lado corto.** Con un valor
		// único, un control bajo y ancho —el buscador, 260×38— recibía brazos verticales
		// de diecisiete puntos sobre un lado de treinta y ocho: los dos de cada lado
		// casi se tocaban y el marco se veía ya cerrado en reposo, con lo que enfocarlo
		// no cambiaba nada. Cada eje tiene que mirarse a sí mismo.
		double reposoH = acotar(ancho * 0.12, ancho / 3);
		double reposoV = acotar(alto * 0.32, alto / 2);

		// Nunca más de la mitad del lado: pasarse haría que los brazos se solaparan y
		// el trazo se viera doble justo en el centro, que es donde más se nota.
		double horizontal = reposoH + (ancho / 2 - reposoH) * v;
		double vertical = reposoV + (alto / 2 - reposoV) * v;

		double derecha = x + ancho;
		double abajo = y + alto;

		// Superior izquierda
		g2.draw(new Line2D.Double(x, y, x + horizontal, y));
		g2.draw(new Line2D.Double(x, y, x, y + vertical));

		// Superior derecha
		g2.draw(new Line2D.Double(derecha - horizontal, y, derecha, y));
		g2.draw(new Line2D.Double(derecha, y, derecha, y + vertical));

		// Inferior izquierda
		g2.draw(new Line2D.Double(x, abajo - vertical, x, abajo));
		g2.draw(new Line2D.Double(x, abajo, x + horizontal, abajo));

		// Inferior derecha
		g2.draw(new Line2D.Double(derecha - horizontal, abajo, derecha, abajo));
		g2.draw(new Line2D.Double(derecha, abajo - vertical, derecha, abajo));
	}
}
