package fp.project.actihome.model.services;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Teselas dibujadas aquí mismo, sin salir a internet (F19).
 *
 * <p>
 * Hermana de {@code WeatherClientDeEjemplo} y por los mismos dos motivos, que
 * conviene no confundir:
 *
 * <ol>
 * <li><b>El CI no debe salir a internet.</b> {@code MedirResponsive} construye
 * las pantallas de verdad, así que sin esto cada ejecución de la comprobación
 * automática sería tráfico real contra la infraestructura donada de
 * OpenStreetMap — que es exactamente lo que su política de uso pide que no
 * hagas.</li>
 * <li><b>Una respuesta de red hace la medición indeterminista.</b> El mapa
 * aparece cuando llega, así que puede materializarse a mitad del recorrido del
 * árbol de componentes y hacer que la misma pantalla mida distinto según lo
 * rápida que vaya la red ese día.</li>
 * </ol>
 *
 * <p>
 * <b>Devuelve una imagen y no un fallo</b>, por lo mismo que allí: si lanzara la
 * excepción, las herramientas medirían la ficha <em>sin</em> el mapa, que es el
 * caso fácil. Hay que medir el peor caso, con el bloque presente ocupando sitio.
 *
 * <p>
 * <b>Y el dibujo no es un rectángulo liso a propósito.</b> Lleva una rejilla y
 * el número de la tesela, así que una captura enseña de un vistazo si la
 * composición encaja las piezas en el orden correcto o si hay alguna repetida o
 * desplazada. Un color plano habría ocultado justo el error que más fácil es
 * cometer al componer un mapa.
 */
@Component
@ConditionalOnProperty(name = "actihome.mapa.habilitado", havingValue = "false")
public class TileClientDeEjemplo implements TileClient {

	@Override
	public BufferedImage tesela(int zoom, int columna, int fila) {

		BufferedImage imagen = new BufferedImage(Teselas.LADO, Teselas.LADO, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = imagen.createGraphics();

		// Un gris muy claro, parecido en luminosidad a un mapa real, para que el velo
		// estacional que se aplica encima se vea como se verá de verdad.
		g2.setColor(new Color(232, 230, 224));
		g2.fillRect(0, 0, Teselas.LADO, Teselas.LADO);

		g2.setColor(new Color(208, 204, 196));

		for (int i = 32; i < Teselas.LADO; i += 32) {
			g2.drawLine(i, 0, i, Teselas.LADO);
			g2.drawLine(0, i, Teselas.LADO, i);
		}

		// El borde y la etiqueta son lo que delata una composición mal encajada.
		g2.setColor(new Color(180, 174, 164));
		g2.drawRect(0, 0, Teselas.LADO - 1, Teselas.LADO - 1);
		g2.drawString(columna + "," + fila + " z" + zoom, 10, 22);

		g2.dispose();

		return imagen;
	}
}
