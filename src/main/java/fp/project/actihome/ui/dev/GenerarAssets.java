package fp.project.actihome.ui.dev;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import fp.project.actihome.ui.theme.ImageScaling;

/**
 * Reduce los originales de {@code assets/} al tamaño en que la aplicación los
 * usa y los deja en {@code src/main/resources/images/}.
 *
 * <pre>
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.GenerarAssets"
 * </pre>
 *
 * <p>
 * <b>Por qué existe esta herramienta en vez de arrastrar los archivos a mano.</b>
 * Los originales pesan entre 1 y 1,6 MB cada uno; las dieciséis piezas que
 * necesita la aplicación caben en menos de 3 MB. Tenerlo en código hace que
 * regenerar todo sea repetible y que las decisiones queden escritas.
 *
 * <p>
 * El escalado en sí —y las dos trampas de reducir una imagen con
 * transparencia que un editor cualquiera no resuelve solo— vive en
 * {@link ImageScaling} desde la Fase 7.9, compartido con
 * {@link fp.project.actihome.ui.theme.HousingPhotos}, que reduce en tiempo de
 * ejecución las fotos que sube un usuario real con el mismo algoritmo.
 */
public final class GenerarAssets {

	private static final String ORIGENES = "assets/References";
	private static final String DESTINO = "src/main/resources/images";

	/** Lado de la ranura más grande de {@code MascotSlot} (220) por dos, para HiDPI. */
	private static final int LADO_OLAZ = 440;

	/** Blanco roto de la baldosa del icono. Muestreado de la hoja de presentación. */
	private static final Color CREMA_ICONO = new Color(0xFD, 0xF5, 0xE8);

	private static final int[] TAMANOS_ICONO = { 16, 24, 32, 48, 64, 128, 256 };

	/**
	 * Lado mayor de las fotos de alojamiento.
	 *
	 * <p>
	 * La ficha más grande del catálogo mide unos 640 px de ancho, así que 1200
	 * deja margen para pantallas con escalado sin que las fotos pesen de más.
	 */
	private static final int LADO_FOTO = 1200;

	/**
	 * Qué foto le toca a cada alojamiento de ejemplo, por código.
	 *
	 * <p>
	 * Elegidas para que <b>cada una diga lo que dice su descripción</b>: la casa
	 * rural es de piedra y con jardín, la cabaña es de madera entre árboles, la
	 * villa es encalada con piscina. Una foto bonita pero que no corresponde con el
	 * texto se nota enseguida y resta credibilidad al catálogo.
	 */
	private static final Map<String, String> FOTOS = new LinkedHashMap<>();

	static {
		FOTOS.put("abby-rurenko-uOYak90r4L0-unsplash.jpg", "10001.jpg");
		FOTOS.put("roberto-nickson-tleCJiDOri0-unsplash.jpg", "10002.jpg");
		FOTOS.put("wes-fischer-g39p1kDjvSY-unsplash.jpg", "10003.jpg");
		FOTOS.put("li-yan-cZOouJsXs8k-unsplash.jpg", "10004.jpg");
		FOTOS.put("andrea-davis-nbI8gqbBaHo-unsplash.jpg", "10005.jpg");
		FOTOS.put("bernard-hermant-nM5-mS5eA8I-unsplash.jpg", "10006.jpg");

		// Fase 7.10: cuatro alojamientos más, con fotos que ya estaban descargadas en
		// assets/ desde el principio pero sin usar (ver el comentario de clase de
		// CREDITOS.md). No hizo falta salir a buscar nada nuevo.
		FOTOS.put("alberto-castillo-q-mx4mSkK9zeo-unsplash.jpg", "10007.jpg");
		FOTOS.put("webaliser-_TPTXZd9mOo-unsplash.jpg", "10008.jpg");
		FOTOS.put("maria-orlova-b37mDyPzdJM-unsplash.jpg", "10009.jpg");
		FOTOS.put("baptx-BTQWx51keUY-unsplash.jpg", "10010.jpg");
	}

	/** Original → nombre con el que lo pide {@code BrandAssets}. */
	private static final Map<String, String> OLAZ = new LinkedHashMap<>();

	static {
		OLAZ.put("olaz-primavera-1-transparent.png", "olaz-primavera-bienvenida.png");
		OLAZ.put("olaz-primavera-2-transparent.png", "olaz-primavera-accion.png");
		OLAZ.put("olaz-verano-1-transparent.png", "olaz-verano-bienvenida.png");
		// El render bueno de la toalla, entregado después que los otros siete.
		OLAZ.put("IconovERANO.png", "olaz-verano-accion.png");
		OLAZ.put("olaz-otono-1-transparent.png", "olaz-otono-bienvenida.png");
		OLAZ.put("olaz-otono-2-transparent.png", "olaz-otono-accion.png");
		OLAZ.put("olaz-invierno-1-transparent.png", "olaz-invierno-bienvenida.png");
		OLAZ.put("olaz-invierno-2-transparent.png", "olaz-invierno-accion.png");
	}

	private GenerarAssets() {
	}

	public static void main(String[] args) throws IOException {

		File origenes = new File(ORIGENES);
		File destino = new File(DESTINO);

		if (!origenes.isDirectory()) {
			throw new IllegalStateException("Ejecuta desde la raíz del proyecto: no encuentro " + ORIGENES);
		}

		olaz(origenes, new File(destino, "olaz"));
		iconos(origenes, new File(destino, "brand"));
		lockup(origenes, new File(destino, "brand"));
		fotos(origenes, new File(destino, "housings"));
	}

	/**
	 * Las fotos de los alojamientos de ejemplo.
	 *
	 * <p>
	 * Son de <b>Unsplash</b>, cuya licencia permite usarlas y modificarlas, incluso
	 * comercialmente, sin atribución obligatoria. Se atribuyen igualmente en
	 * {@code CREDITOS.md}: no cuesta nada y es lo correcto.
	 *
	 * <p>
	 * <b>Se guardan en JPEG y no en PNG</b>, al revés que el resto de recursos del
	 * proyecto. PNG comprime sin pérdida, que es lo que hace falta para una
	 * ilustración con zonas planas y transparencia; una fotografía no tiene ni lo
	 * uno ni lo otro, y en PNG ocuparía varias veces más sin ninguna ganancia
	 * visible.
	 */
	private static void fotos(File origenes, File destino) throws IOException {

		File carpeta = new File(origenes, "Alojamientos de ejemplo");

		if (!carpeta.isDirectory()) {
			System.out.println("(sin fotos que procesar: falta " + carpeta + ")");
			return;
		}

		destino.mkdirs();

		for (Map.Entry<String, String> foto : FOTOS.entrySet()) {

			File original = new File(carpeta, foto.getKey());

			if (!original.isFile()) {
				System.out.println("(falta " + foto.getKey() + ")");
				continue;
			}

			BufferedImage reducida = ImageScaling.escalarACaja(ImageIO.read(original), LADO_FOTO);

			// A RGB sin alfa antes de escribir: JPEG no tiene canal de transparencia, y
			// pasarle una imagen ARGB hace que el codificador interprete los cuatro
			// canales como si fueran color. El resultado son fotos con un tinte rosado,
			// y es un fallo que solo se ve al abrir el archivo, no al generarlo.
			BufferedImage sinAlfa = ImageScaling.copiar(reducida, reducida.getWidth(), reducida.getHeight(),
					BufferedImage.TYPE_INT_RGB, null);

			File salida = new File(destino, foto.getValue());
			ImageIO.write(sinAlfa, "jpg", salida);

			System.out.printf("%-16s %4dx%-4d  %3d KB%n", foto.getValue(), reducida.getWidth(), reducida.getHeight(),
					salida.length() / 1024);
		}
	}

	/**
	 * El lockup "CocoBrain presenta", <b>sin su fondo</b>.
	 *
	 * <p>
	 * El original es una ilustración sobre un rectángulo crema. En el splash ese
	 * rectángulo tapaba el fondo decorativo de la aplicación, así que se recorta
	 * para que el logotipo se apoye directamente sobre el dibujo.
	 *
	 * <p>
	 * <b>Por qué no vale un simple "todo lo claro, fuera".</b> Dentro del logotipo
	 * hay zonas tan claras como el fondo —las manos blancas del personaje y el
	 * rosa del cerebro— y un umbral por luminosidad las borraría también. La
	 * solución es {@link #recortarFondo}, que no mira el color de cada píxel
	 * aislado sino <b>si está conectado con el borde de la imagen</b>: el crema de
	 * alrededor sí lo está; una mano blanca en mitad del dibujo, no.
	 */
	private static void lockup(File origenes, File destino) throws IOException {

		BufferedImage original = ImageIO.read(new File(origenes, "Presentacion.png"));
		BufferedImage sinFondo = recortar(recortarFondo(original));
		BufferedImage reducido = ImageScaling.escalarACaja(sinFondo, 900);

		File salida = new File(destino, "cocobrain-presenta.png");
		ImageIO.write(reducido, "png", salida);

		System.out.printf("cocobrain-presenta  %dx%d -> %dx%d  %d KB%n", original.getWidth(), original.getHeight(),
				reducido.getWidth(), reducido.getHeight(), salida.length() / 1024);
	}

	/**
	 * Hace transparente el fondo de una ilustración, respetando las zonas claras
	 * que estén dentro del dibujo.
	 *
	 * <p>
	 * Dos pasos. Primero un <b>relleno por inundación</b> desde los cuatro bordes:
	 * se propaga por los píxeles cuyo color se parece al del borde, y así se marca
	 * exactamente la región de fondo, que es la única conectada con el exterior.
	 * Después, para que el recorte no quede dentado, cada píxel marcado recibe un
	 * alfa proporcional a lo <em>distinto</em> que sea del color de fondo: el crema
	 * puro desaparece del todo y la sombra suave que el logotipo proyecta sobre él
	 * queda semitransparente, que es lo que hace que se funda con el dibujo de
	 * detrás en lugar de recortarse con tijera.
	 */
	private static BufferedImage recortarFondo(BufferedImage origen) {

		int ancho = origen.getWidth();
		int alto = origen.getHeight();

		Color fondo = colorMedioDelBorde(origen);

		// Hasta esta distancia el píxel se considera fondo puro; a partir de la
		// segunda, dibujo. Entre las dos se reparte el degradado del borde.
		double dentro = 26;
		double fuera = 78;

		boolean[] esFondo = new boolean[ancho * alto];
		Deque<int[]> pendientes = new ArrayDeque<>();

		for (int x = 0; x < ancho; x++) {
			encolar(pendientes, esFondo, origen, fondo, fuera, x, 0, ancho);
			encolar(pendientes, esFondo, origen, fondo, fuera, x, alto - 1, ancho);
		}

		for (int y = 0; y < alto; y++) {
			encolar(pendientes, esFondo, origen, fondo, fuera, 0, y, ancho);
			encolar(pendientes, esFondo, origen, fondo, fuera, ancho - 1, y, ancho);
		}

		while (!pendientes.isEmpty()) {

			int[] p = pendientes.pop();

			for (int[] vecino : new int[][] { { p[0] + 1, p[1] }, { p[0] - 1, p[1] }, { p[0], p[1] + 1 },
					{ p[0], p[1] - 1 } }) {

				if (vecino[0] >= 0 && vecino[0] < ancho && vecino[1] >= 0 && vecino[1] < alto) {
					encolar(pendientes, esFondo, origen, fondo, fuera, vecino[0], vecino[1], ancho);
				}
			}
		}

		BufferedImage salida = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);

		for (int y = 0; y < alto; y++) {
			for (int x = 0; x < ancho; x++) {

				int rgb = origen.getRGB(x, y);

				if (!esFondo[y * ancho + x]) {
					salida.setRGB(x, y, rgb | 0xFF000000);
					continue;
				}

				double d = distancia(rgb, fondo);
				double alfa = Math.max(0, Math.min(1, (d - dentro) / (fuera - dentro)));

				salida.setRGB(x, y, (rgb & 0x00FFFFFF) | ((int) Math.round(alfa * 255) << 24));
			}
		}

		return salida;
	}

	private static void encolar(Deque<int[]> pendientes, boolean[] esFondo, BufferedImage img, Color fondo,
			double umbral, int x, int y, int ancho) {

		int i = y * ancho + x;

		if (esFondo[i] || distancia(img.getRGB(x, y), fondo) > umbral) {
			return;
		}

		esFondo[i] = true;
		pendientes.push(new int[] { x, y });
	}

	/** El color del fondo, promediado en el marco exterior de la imagen. */
	private static Color colorMedioDelBorde(BufferedImage img) {

		long r = 0;
		long g = 0;
		long b = 0;
		long n = 0;

		for (int x = 0; x < img.getWidth(); x++) {
			for (int y : new int[] { 0, img.getHeight() - 1 }) {
				int c = img.getRGB(x, y);
				r += (c >> 16) & 0xFF;
				g += (c >> 8) & 0xFF;
				b += c & 0xFF;
				n++;
			}
		}

		return new Color((int) (r / n), (int) (g / n), (int) (b / n));
	}

	private static double distancia(int rgb, Color referencia) {

		int dr = ((rgb >> 16) & 0xFF) - referencia.getRed();
		int dg = ((rgb >> 8) & 0xFF) - referencia.getGreen();
		int db = (rgb & 0xFF) - referencia.getBlue();

		return Math.sqrt((double) dr * dr + (double) dg * dg + (double) db * db);
	}

	private static void olaz(File origenes, File destino) throws IOException {

		for (Map.Entry<String, String> pieza : OLAZ.entrySet()) {

			BufferedImage original = ImageIO.read(new File(origenes, pieza.getKey()));

			// Recortar el margen transparente antes de escalar: los originales son
			// lienzos con la figura flotando en medio, y sin recortar la mascota
			// aparecería pequeña y perdida dentro de su ranura.
			BufferedImage reducida = ImageScaling.escalarACaja(recortar(original), LADO_OLAZ);

			File salida = new File(destino, pieza.getValue());
			ImageIO.write(reducida, "png", salida);

			System.out.printf("%-34s %4dx%-4d -> %3dx%-3d  %3d KB%n", pieza.getValue(), original.getWidth(),
					original.getHeight(), reducida.getWidth(), reducida.getHeight(), salida.length() / 1024);
		}
	}

	/**
	 * El icono de aplicación: el logotipo sobre un cuadrado redondeado de blanco
	 * roto.
	 *
	 * <p>
	 * Se usa la variante de la <b>"A" azul marino</b> y no la de la "A" blanca. No
	 * es preferencia: el interior del círculo del logotipo es transparente, así que
	 * la letra se apoya directamente en el fondo de la baldosa, y sobre blanco roto
	 * una letra blanca desaparece.
	 */
	private static void iconos(File origenes, File destino) throws IOException {

		BufferedImage logo = recortar(
				ImageIO.read(new File(origenes, "actihome-icon-variante1-azul-transparent.png")));

		for (int lado : TAMANOS_ICONO) {

			BufferedImage icono = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = icono.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			double radio = lado * 0.22;
			g2.setColor(CREMA_ICONO);
			g2.fill(new RoundRectangle2D.Double(0, 0, lado, lado, radio, radio));

			// Margen interior más generoso en los tamaños pequeños: ahí cada píxel de
			// aire cuenta para que la silueta siga leyéndose.
			int margen = Math.max(1, Math.round(lado * (lado <= 32 ? 0.10f : 0.13f)));
			BufferedImage escalado = ImageScaling.escalarACaja(logo, lado - margen * 2);

			g2.drawImage(escalado, (lado - escalado.getWidth()) / 2, (lado - escalado.getHeight()) / 2, null);
			g2.dispose();

			File salida = new File(destino, "actihome-icon-" + lado + ".png");
			ImageIO.write(icono, "png", salida);

			System.out.printf("actihome-icon-%-3d %d bytes%n", lado, salida.length());
		}
	}

	/** El rectángulo mínimo que contiene todo lo que no es transparente. */
	private static BufferedImage recortar(BufferedImage img) {

		int minX = img.getWidth();
		int minY = img.getHeight();
		int maxX = -1;
		int maxY = -1;

		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {

				if (((img.getRGB(x, y) >>> 24) & 0xFF) > 8) {
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}

		return maxX < 0 ? img : img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
	}
}
