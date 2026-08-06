package fp.project.actihome.ui.dev;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import fp.project.actihome.model.exceptions.LocationNotFoundException;
import fp.project.actihome.model.exceptions.TilesUnavailableException;
import fp.project.actihome.model.services.Coordenadas;
import fp.project.actihome.model.services.OpenMeteoGeocodingClient;
import fp.project.actihome.model.services.OpenStreetMapTileClient;
import fp.project.actihome.model.services.Teselas;

/**
 * Compone un mapa de verdad y lo escribe en un PNG (F19).
 *
 * <pre>
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.ProbarMapa"
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.ProbarMapa" "-Dexec.args=Ronda, Málaga"
 * </pre>
 *
 * <p>
 * <b>Por qué hizo falta, y es un motivo distinto al de las otras herramientas de
 * prueba.</b> {@code ProbarMeteorologia} existe porque un test con un doble no
 * comprueba que la URL y el JSON del proveedor sigan siendo los que eran. Esta
 * existe por eso <b>y por algo más: el mapa no aparece en las capturas</b>.
 *
 * <p>
 * Las teselas llegan en un {@code SwingWorker}, y {@code ScreenSnapshots} pinta
 * la ventana en cuanto el layout se resuelve — sin esperar a que ningún hilo de
 * fondo termine. El hueco del mapa sale reservado (que es lo que hace que la
 * medición sea determinista) pero <b>vacío</b>. Así que la captura no sirve para
 * responder a la única pregunta que importa aquí: <b>¿encajan las teselas donde
 * deben?</b>
 *
 * <p>
 * Componer un mapa es fácil de programar <em>casi</em> bien: basta equivocarse en
 * un signo para que las piezas salgan desplazadas media tesela, y eso no lanza
 * ninguna excepción — se ve, y solo se ve, mirando la imagen. Esta herramienta la
 * produce.
 *
 * <p>
 * <b>Comprueba además la parte que ningún test puede comprobar:</b> que
 * OpenStreetMap acepta nuestras peticiones. Su política bloquea a los clientes
 * que no se identifican, y ese bloqueo llegaría como un 403 que aquí se ve al
 * momento en vez de como un mapa que no aparece en la aplicación.
 *
 * <p>
 * Escribe en {@code docs/progreso/prueba-mapa.png}, que está fuera del control de
 * versiones a propósito: es una comprobación, no documentación.
 */
public final class ProbarMapa {

	private static final int ZOOM = 13;
	private static final int ANCHO = 640;
	private static final int ALTO = 320;

	private static final String DESTINO = "docs/progreso/prueba-mapa.png";

	private ProbarMapa() {
	}

	public static void main(String[] args) throws IOException {

		String lugar = args.length > 0 ? String.join(" ", args) : "Sierra Nevada, Granada";

		System.out.println();
		System.out.println("Buscando: " + lugar);

		Coordenadas punto;

		try {
			punto = new OpenMeteoGeocodingClient().localizar(lugar);

		} catch (LocationNotFoundException ex) {
			System.out.println("FALLO al localizar. Sin red, o el buscador no conoce ese sitio.");
			System.exit(1);
			return;
		}

		System.out.printf("Encontrado: %s  (%.4f, %.4f)%n", punto.etiqueta(), punto.latitud(), punto.longitud());

		BufferedImage mapa = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = mapa.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		try {
			int traidas = componer(g2, punto);
			System.out.println("Teselas compuestas: " + traidas);

		} catch (TilesUnavailableException ex) {
			System.out.println("FALLO al traer las teselas. Sin red, o OpenStreetMap ha rechazado la peticion.");
			System.out.println("Si es lo segundo, revisa la cabecera User-Agent de OpenStreetMapTileClient.");
			System.exit(1);
			return;
		}

		marcador(g2);
		g2.dispose();

		File destino = new File(DESTINO);
		destino.getParentFile().mkdirs();
		ImageIO.write(mapa, "png", destino);

		System.out.println();
		System.out.println("Escrito: " + destino.getPath());
		System.out.println("Abrelo y comprueba que las piezas encajan y que el punto cae donde debe.");
		System.exit(0);
	}

	/**
	 * La misma cuenta que hace {@code MapaDeUbicacion}, a propósito.
	 *
	 * <p>
	 * <b>Está duplicada y conviene decir por qué</b>, porque duplicar suele ser un
	 * error: el original vive dentro de un componente de Swing que necesita una
	 * ventana, un layout y un hilo de eventos para existir. Sacarlo a una clase
	 * compartida solo para esto le añadiría una abstracción al código de
	 * producción que nadie más usa. Aquí son quince líneas y la parte no trivial
	 * —la proyección— sí está compartida, en {@link Teselas}, que es donde estaba
	 * el riesgo de verdad.
	 */
	private static int componer(Graphics2D g2, Coordenadas punto) throws TilesUnavailableException {

		OpenStreetMapTileClient cliente = new OpenStreetMapTileClient();

		double columnaExacta = Teselas.columna(punto.longitud(), ZOOM);
		double filaExacta = Teselas.fila(punto.latitud(), ZOOM);

		double izquierdaGlobal = columnaExacta * Teselas.LADO - ANCHO / 2.0;
		double arribaGlobal = filaExacta * Teselas.LADO - ALTO / 2.0;

		int primeraColumna = (int) Math.floor(izquierdaGlobal / Teselas.LADO);
		int primeraFila = (int) Math.floor(arribaGlobal / Teselas.LADO);

		int columnas = (int) Math.ceil((double) ANCHO / Teselas.LADO) + 1;
		int filas = (int) Math.ceil((double) ALTO / Teselas.LADO) + 1;

		int traidas = 0;

		for (int dx = 0; dx < columnas; dx++) {
			for (int dy = 0; dy < filas; dy++) {

				int fila = primeraFila + dy;

				if (!Teselas.filaValida(fila, ZOOM)) {
					continue;
				}

				int columna = Teselas.envolverColumna(primeraColumna + dx, ZOOM);

				BufferedImage tesela = cliente.tesela(ZOOM, columna, fila);

				int x = (int) Math.round((primeraColumna + dx) * (double) Teselas.LADO - izquierdaGlobal);
				int y = (int) Math.round(fila * (double) Teselas.LADO - arribaGlobal);

				g2.drawImage(tesela, x, y, null);
				traidas++;
			}
		}

		return traidas;
	}

	/**
	 * El punto, en el centro exacto.
	 *
	 * <p>
	 * <b>Aquí no se aplica el velo estacional</b>, a diferencia de la aplicación:
	 * esta herramienta sirve para comprobar que la composición encaja, y teñir la
	 * imagen solo dificultaría ver una costura entre teselas.
	 */
	private static void marcador(Graphics2D g2) {

		double radio = 8;
		double cx = ANCHO / 2.0;
		double cy = ALTO / 2.0;

		g2.setStroke(new BasicStroke(3f));
		g2.setColor(Color.WHITE);
		g2.draw(new Ellipse2D.Double(cx - radio, cy - radio, radio * 2, radio * 2));

		g2.setColor(new Color(224, 172, 27));
		g2.fill(new Ellipse2D.Double(cx - radio, cy - radio, radio * 2, radio * 2));
	}
}
