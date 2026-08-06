package fp.project.actihome.ui.housings;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.exceptions.TilesUnavailableException;
import fp.project.actihome.model.services.TileClient;
import fp.project.actihome.model.services.Teselas;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;

/**
 * Dónde está el alojamiento, en un mapa (F19).
 *
 * <p>
 * <b>Por qué esto estaba pendiente y por qué se pudo hacer al final.</b> Se
 * descartó dos veces con dos argumentos, y los dos eran malos:
 *
 * <ul>
 * <li><i>"Las teselas romperían la paleta estacional"</i>. El proyecto ya
 * resolvía eso: el velo {@code Theme.imgTint()} es, según el propio manual, "lo
 * que hace que diez fotos de diez fotógrafos parezcan un mismo catálogo". <b>Un
 * mapa es una imagen igual que una foto</b> y admite el mismo velo, que es lo
 * que se hace abajo.</li>
 * <li><i>"Todos los proveedores piden clave"</i>. Falso: OpenStreetMap no pide
 * ninguna. Lo que tiene es una política de uso, que se cumple en
 * {@code OpenStreetMapTileClient}.</li>
 * </ul>
 *
 * <p>
 * Y el argumento a favor, que es de producto y no estético: <b>la ubicación es
 * un criterio de decisión, no un adorno.</b> "Sierra Nevada, Granada" no dice si
 * estás a pie de pista o a veinte minutos en coche.
 *
 * <hr>
 *
 * <p>
 * <b>Se comporta igual que {@link PrevisionPanel} ante lo que falta</b>, porque
 * es la misma clase de bloque: si el alojamiento no tiene coordenadas no se
 * construye, y si las teselas no llegan se oculta <b>sin enseñar ningún
 * error</b>. Nadie pidió ver un mapa.
 *
 * <p>
 * <b>La atribución no es opcional</b> y por eso vive aquí y no en el cliente: es
 * una obligación de lo que se <em>enseña</em>, no de lo que se descarga. Los
 * datos son de los colaboradores de OpenStreetMap y hay que decirlo donde se vea
 * el mapa.
 */
public class MapaDeUbicacion extends JPanel {

	private static final long serialVersionUID = 1L;

	/**
	 * Nivel de zoom.
	 *
	 * <p>
	 * 13 enseña un pueblo entero o un barrio grande, que es la pregunta que se hace
	 * quien mira una ficha: <em>¿dónde cae esto?</em>. Con más zoom se ven las
	 * calles y se pierde la referencia de la zona; con menos, el marcador cae en
	 * mitad de una provincia y no dice nada.
	 */
	private static final int ZOOM = 13;

	/** Alto del mapa. El ancho lo pone la columna. */
	private static final int ALTO = 150;

	private final transient TileClient tileClient;
	private final transient Housing housing;

	private final Lienzo lienzo;

	public MapaDeUbicacion(Housing housing, TileClient tileClient) {

		super(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XXS + "[]"));

		this.housing = housing;
		this.tileClient = tileClient;

		setOpaque(false);

		lienzo = new Lienzo();
		add(lienzo, "h " + ALTO + "!");

		JLabel atribucion = Labels.muted(Textos.t("detalle.mapa.atribucion"));
		add(atribucion);

		// Mínimo cero por lo mismo que en la previsión: un bloque informativo no puede
		// ser lo que decide el ancho mínimo de una pantalla.
		setMinimumSize(new Dimension(0, 0));

		// **Arranca VISIBLE si el alojamiento está localizado, aunque todavía no haya
		// nada que dibujar, y esto costó una corrección.**
		//
		// La primera versión arrancaba oculto y solo aparecía cuando llegaban las
		// teselas. Se veía mejor —nada de huecos— y hacía que la ficha midiera distinto
		// según cuándo llegara la respuesta: en las capturas y en MedirResponsive el
		// bloque sencillamente NO ESTABA, así que las herramientas daban por buena una
		// pantalla que no incluía este componente. Es literalmente la trampa que se
		// documentó al construir la previsión: hay que medir el peor caso, con el
		// bloque presente ocupando sitio, no la versión cómoda de la pantalla.
		//
		// Reservando el hueco desde el principio, el reparto vertical es el mismo haya
		// red o no. Lo que sigue siendo asíncrono es el CONTENIDO, no el tamaño.
		setVisible(housing.estaLocalizado());

		if (!housing.estaLocalizado()) {
			return;
		}

		// **La descarga NO se lanza aquí, y esa es la parte que costó una corrección.**
		// En el constructor el componente todavía no se ha distribuido, así que su
		// ancho es CERO: calcular con él daría un mapa de una tesela, centrado en
		// cualquier sitio. Hay que esperar a que el layout le dé un tamaño real, y eso
		// ocurre después.
		//
		// Se dispara en el primer redimensionado con ancho útil y una sola vez —el
		// testigo 'pedido'—, porque este escuchador seguirá recibiendo avisos cada vez
		// que la ventana cambie de tamaño y no hay que volver a pedir nada.
		lienzo.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {

				if (!pedido && lienzo.getWidth() > 0) {
					pedido = true;
					descargar(lienzo.getWidth());
				}
			}
		});
	}

	/** Si ya se lanzó la descarga. Ver la nota del constructor. */
	private boolean pedido;

	/**
	 * Trae las teselas que hacen falta y, si llegan todas, se enseña.
	 *
	 * <p>
	 * <b>Arranca invisible y solo aparece cuando hay algo que enseñar</b>, al revés
	 * que la previsión, que enseña "Consultando…". El motivo es que un mapa a medio
	 * cargar —dos cuadrados grises de cuatro— se lee como un fallo, mientras que un
	 * texto que dice que está consultando se lee como lo que es. Aquí no hay forma
	 * de decirlo sin ocupar el sitio del propio mapa.
	 *
	 * <p>
	 * Va en un {@code SwingWorker} por la regla de siempre: es la única excepción al
	 * modelo de un hilo del proyecto, y está justificada por el tipo de espera —una
	 * petición HTTP puede no contestar nunca.
	 */
	private void descargar(int ancho) {

		// El ancho se captura AQUÍ, en el hilo de la interfaz, y se le pasa al worker.
		// Leer getWidth() desde doInBackground sería tocar un componente de Swing desde
		// otro hilo, que es justo lo que el SwingWorker existe para evitar.
		new SwingWorker<List<Pieza>, Void>() {

			@Override
			protected List<Pieza> doInBackground() throws TilesUnavailableException {
				return piezasNecesarias(ancho);
			}

			@Override
			protected void done() {

				try {
					List<Pieza> piezas = get();

					if (piezas.isEmpty()) {
						setVisible(false);
						return;
					}

					lienzo.setPiezas(piezas);

				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					setVisible(false);

				} catch (ExecutionException ex) {
					// Sin red o proveedor caído: **ahora sí** se retira el bloque, y con él su
					// hueco. Es el mismo criterio que la previsión — la ficha estaba completa
					// sin el mapa y lo sigue estando — pero ocurre al fallar, no antes de
					// intentarlo, que es lo que mantiene el reparto predecible mientras tanto.
					setVisible(false);
				}

				// Revalidar en los dos caminos: o acaba de aparecer contenido, o acaba de
				// desaparecer el hueco. Las dos cosas cambian el reparto de la columna.
				revalidate();
				repaint();
			}
		}.execute();
	}

	/**
	 * Qué teselas hacen falta y dónde va cada una.
	 *
	 * <p>
	 * <b>El cálculo, en una frase:</b> se localiza el punto en coordenadas de tesela
	 * —con decimales—, se mira qué trozo del mapa cabe en el hueco disponible, y se
	 * piden las teselas que tocan ese trozo, apuntando en qué píxel de este
	 * componente va la esquina de cada una.
	 *
	 * <p>
	 * <b>Se pide una tesela de más por cada lado</b> ({@code +1} en los bucles), y
	 * no es un descuido: el centro casi nunca cae justo en el borde de una tesela,
	 * así que la primera se dibuja parcialmente fuera y hace falta otra al final
	 * para tapar el hueco. Sin ese margen, el mapa sale con una franja vacía a la
	 * derecha y abajo — un fallo que además solo aparece con ciertas coordenadas, o
	 * sea de los que se cuelan.
	 *
	 * <p>
	 * <b>Y esto NO es una descarga por lotes</b>, que la política de OpenStreetMap
	 * prohíbe: son las teselas que se van a dibujar ahora mismo en este hueco, y no
	 * hay forma de pedir un área.
	 */
	private List<Pieza> piezasNecesarias(int ancho) throws TilesUnavailableException {

		int alto = ALTO;

		// El punto exacto, en coordenadas de tesela con decimales.
		double columnaExacta = Teselas.columna(housing.getLongitude(), ZOOM);
		double filaExacta = Teselas.fila(housing.getLatitude(), ZOOM);

		// Dónde caería la esquina superior izquierda del lienzo, en píxeles globales
		// del mapa, si el punto quedara centrado.
		double izquierdaGlobal = columnaExacta * Teselas.LADO - ancho / 2.0;
		double arribaGlobal = filaExacta * Teselas.LADO - alto / 2.0;

		int primeraColumna = (int) Math.floor(izquierdaGlobal / Teselas.LADO);
		int primeraFila = (int) Math.floor(arribaGlobal / Teselas.LADO);

		int columnas = (int) Math.ceil((double) ancho / Teselas.LADO) + 1;
		int filas = (int) Math.ceil((double) alto / Teselas.LADO) + 1;

		List<Pieza> piezas = new ArrayList<>();

		for (int dx = 0; dx < columnas; dx++) {
			for (int dy = 0; dy < filas; dy++) {

				int fila = primeraFila + dy;

				// Por encima del polo no hay mapa. Se salta en vez de pedirla: el servidor
				// devolvería un error y el hueco quedaría gris sin explicación.
				if (!Teselas.filaValida(fila, ZOOM)) {
					continue;
				}

				int columna = Teselas.envolverColumna(primeraColumna + dx, ZOOM);

				BufferedImage imagen = tileClient.tesela(ZOOM, columna, fila);

				int x = (int) Math.round((primeraColumna + dx) * (double) Teselas.LADO - izquierdaGlobal);
				int y = (int) Math.round(fila * (double) Teselas.LADO - arribaGlobal);

				piezas.add(new Pieza(imagen, x, y));
			}
		}

		return piezas;
	}

	/** Una tesela ya situada en el lienzo. */
	private record Pieza(BufferedImage imagen, int x, int y) {
	}

	/**
	 * El mapa dibujado: teselas, velo estacional y marcador.
	 *
	 * <p>
	 * Es una clase aparte y no {@code paintComponent} del panel entero porque el
	 * panel lleva además la atribución debajo, y el velo no debe teñirla.
	 */
	private class Lienzo extends javax.swing.JComponent {

		private static final long serialVersionUID = 1L;

		private final transient List<Pieza> piezas = new ArrayList<>();

		void setPiezas(List<Pieza> nuevas) {

			piezas.clear();
			piezas.addAll(nuevas);
			repaint();
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(240, ALTO);
		}

		/** Si defines el preferido, define el mínimo. Aquí el suelo es cero a lo ancho. */
		@Override
		public Dimension getMinimumSize() {
			return new Dimension(0, ALTO);
		}

		@Override
		protected void paintComponent(Graphics g) {

			if (piezas.isEmpty()) {
				return;
			}

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// Recortar al propio componente: las teselas de los bordes sobresalen a
			// propósito (ver piezasNecesarias) y sin esto se pintarían sobre sus vecinos.
			g2.clipRect(0, 0, getWidth(), getHeight());

			for (Pieza pieza : piezas) {
				g2.drawImage(pieza.imagen(), pieza.x(), pieza.y(), null);
			}

			// **El velo estacional, que es lo que integra el mapa con el resto.** Es el
			// mismo imgTint que llevan las fotos de los alojamientos: sin él, las teselas
			// traen sus propios verdes y grises y el bloque se ve pegado de otra
			// aplicación. Con él, el mapa pertenece a la estación activa igual que todo lo
			// demás.
			g2.setColor(Theme.imgTint());
			g2.fillRect(0, 0, getWidth(), getHeight());

			marcador(g2, getWidth() / 2, getHeight() / 2);

			g2.dispose();
		}

		/**
		 * El punto donde está el alojamiento.
		 *
		 * <p>
		 * <b>Se dibuja, no se escribe</b>, como {@code StarRating}, {@code SwapGlyph} e
		 * {@code IconoDelCielo}: ninguna de las dos fuentes empaquetadas tiene un glifo
		 * de chincheta, y cuando a Swing le falta uno no avisa — pinta un rectángulo
		 * vacío que se lee como un icono raro.
		 *
		 * <p>
		 * <b>Lleva un anillo blanco alrededor, y no es decorativo.</b> Un disco de color
		 * sobre un mapa puede caer encima de cualquier cosa —un bosque verde, una
		 * carretera, un lago— así que no hay ningún fondo contra el que garantizar
		 * contraste. El anillo claro lo separa de lo que haya debajo sea lo que sea. Es
		 * el mismo problema que {@code MedirContraste} resuelve con colores declarados,
		 * pero aquí el fondo es una fotografía del mundo y no se puede medir.
		 */
		private void marcador(Graphics2D g2, int cx, int cy) {

			double radio = 7;

			g2.setStroke(new BasicStroke(3f));
			g2.setColor(Color.WHITE);
			g2.draw(new Ellipse2D.Double(cx - radio, cy - radio, radio * 2, radio * 2));

			g2.setColor(Theme.acc());
			g2.fill(new Ellipse2D.Double(cx - radio, cy - radio, radio * 2, radio * 2));
		}
	}
}
