package fp.project.actihome.ui.dev;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JFrame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import fp.project.actihome.ActihomeApplication;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.ShowHousingsFrame;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.ActiHomeTheme;
import fp.project.actihome.ui.theme.Animacion;
import fp.project.actihome.ui.theme.Season;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;

/**
 * Genera el GIF animado del cambio de estación: el catálogo reambientándose en
 * las cuatro paletas, en bucle.
 *
 * <pre>
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.GenerarGif"
 * </pre>
 *
 * <p>
 * <b>Para qué sirve.</b> El sistema estacional es lo que distingue a esta
 * aplicación de cualquier otra hecha con una plantilla, y es justo lo que una
 * captura fija no puede enseñar: cuatro imágenes puestas en fila obligan a
 * comparar a ojo, mientras que verlo transformarse se entiende sin leer nada.
 * Un GIF se reproduce solo al abrir el README de GitHub, así que no hace falta
 * que nadie decida pulsar nada.
 *
 * <p>
 * <b>Por qué no es una grabación de pantalla.</b> Una captura de vídeo depende
 * del escalado del monitor de quien la graba, del cursor, del rendimiento del
 * equipo y de que la mano acierte a pulsar en el momento justo — y hay que
 * rehacerla entera cada vez que cambia una pantalla. Aquí los fotogramas se
 * pintan sobre imágenes en memoria: el resultado es idéntico en cualquier
 * máquina y se regenera con un comando.
 *
 * <p>
 * <b>Hereda de {@link ScreenSnapshots} sus dos precauciones.</b> Arranca contra
 * una H2 <b>en memoria</b> —no contra {@code ~/.actihome}, que es la base de
 * trabajo del usuario— y desactiva la meteorología y el mapa reales: los dos
 * llegan por red en un hilo de fondo, así que aparecerían o no en cada
 * fotograma según lo rápida que fuese la conexión, y un fotograma de una
 * animación que parpadea se ve como un fallo.
 */
public final class GenerarGif {

	private static final String SALIDA = "docs/progreso/estaciones.gif";

	private static final String BASE_EN_MEMORIA = "jdbc:h2:mem:gif;DB_CLOSE_DELAY=-1;MODE=MySQL";

	/**
	 * Tamaño al que se pinta la pantalla, antes de reducir.
	 *
	 * <p>
	 * Se pinta grande y se reduce después en lugar de pintar directamente al
	 * tamaño final: reducir promedia píxeles, y ese promediado suaviza los bordes
	 * del texto. Pintar a 800 de ancho daría una maquetación distinta —más
	 * apretada— y encima con el texto peor.
	 */
	private static final int ANCHO_DE_PINTADO = 1400;
	private static final int ALTO_DE_PINTADO = 900;

	/**
	 * Ancho del GIF.
	 *
	 * <p>
	 * <b>Es el compromiso central de esta herramienta.</b> Un GIF pesa
	 * aproximadamente lo que ocupa un fotograma multiplicado por cuántos hay, y en
	 * un README de GitHub un archivo de veinte megas no lo ve nadie: se queda
	 * cargando. 800 puntos es el ancho en el que el texto de las fichas todavía se
	 * lee y el archivo se mantiene en pocos megas.
	 */
	private static final int ANCHO_FINAL = 800;

	/** Centésimas de segundo que se queda quieta cada estación. */
	private static final int ESPERA_EN_ESTACION = 120;

	/** Centésimas de segundo de cada paso de la transición. */
	private static final int ESPERA_EN_TRANSICION = 8;

	/**
	 * Pasos intermedios entre una estación y la siguiente.
	 *
	 * <p>
	 * Cinco es suficiente para que se lea como una transformación y no como un
	 * corte, y cada paso de más son 800×514 píxeles más de archivo. Con cuatro
	 * estaciones salen 4 × (1 + 5) = <b>24 fotogramas</b>.
	 */
	private static final int PASOS_DE_TRANSICION = 5;

	private GenerarGif() {
	}

	public static void main(String[] args) throws IOException {

		System.setProperty("java.awt.headless", "false");
		ActiHomeTheme.install();

		// Sin animaciones: una herramienta que lea un fotograma intermedio da un
		// resultado distinto en cada ejecucion. Ver Animacion.desactivarParaHerramientas.
		Animacion.desactivarParaHerramientas();

		// El idioma se fija DESPUÉS de install(), y esa es toda la gracia: install()
		// llama a Preferencias.restaurar(), que lee ~/.actihome y aplica lo que el
		// dueño de la máquina tuviera puesto la última vez. Sin esta línea, el GIF
		// salía en inglés o en español según quién lo generara — un resultado que
		// depende del estado de una máquina no es reproducible, que es exactamente lo
		// que esta herramienta existe para evitar.
		Textos.cambiarA(new Locale("es"));

		SpringApplication app = new SpringApplication(ActihomeApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);

		List<BufferedImage> estaciones = new ArrayList<>();

		try (ConfigurableApplicationContext context = app.run("--spring.datasource.url=" + BASE_EN_MEMORIA,
				"--spring.datasource.username=sa", "--actihome.meteorologia.habilitada=false",
				"--actihome.mapa.habilitado=false")) {

			comprobarQueLaBaseEsDesechable(context);
			abrirSesion(context);

			ShowHousingsFrame catalogo = context.getBean(ShowHousingsFrame.class);

			for (Season estacion : Season.values()) {

				Theme.cambiarA(estacion);
				estaciones.add(reducir(dibujar(catalogo)));

				System.out.println("Fotograma base: " + estacion);
			}
		}

		escribirGif(montar(estaciones), new File(SALIDA));

		System.out.println("GIF generado: " + SALIDA);

		// Swing deja vivo su hilo de eventos y el de los temporizadores de las
		// partículas. Sin esto, la consola se queda colgada con el trabajo ya hecho.
		System.exit(0);
	}

	/**
	 * Convierte las cuatro imágenes base en la secuencia completa: cada estación
	 * quieta, y detrás su fundido hacia la siguiente.
	 *
	 * <p>
	 * <b>La última enlaza con la primera</b> ({@code % estaciones.size()}), y no es
	 * un detalle cosmético: un GIF se reproduce en bucle infinito, así que sin ese
	 * cierre habría un salto brusco de invierno a primavera cada vuelta, justo en
	 * el punto donde el ojo ya está esperando otra transición suave.
	 */
	private static List<Fotograma> montar(List<BufferedImage> estaciones) {

		List<Fotograma> secuencia = new ArrayList<>();

		for (int i = 0; i < estaciones.size(); i++) {

			BufferedImage actual = estaciones.get(i);
			BufferedImage siguiente = estaciones.get((i + 1) % estaciones.size());

			secuencia.add(new Fotograma(actual, ESPERA_EN_ESTACION));

			for (int paso = 1; paso <= PASOS_DE_TRANSICION; paso++) {

				double avance = (double) paso / (PASOS_DE_TRANSICION + 1);
				secuencia.add(new Fotograma(mezclar(actual, siguiente, avance), ESPERA_EN_TRANSICION));
			}
		}

		return secuencia;
	}

	/**
	 * Mezcla dos imágenes píxel a píxel.
	 *
	 * <p>
	 * Se hace a mano y no con {@code AlphaComposite} porque las dos imágenes son
	 * opacas: pintar una encima de otra con transparencia da exactamente esto, pero
	 * pasando por el canal alfa y dependiendo de cómo esté configurado el
	 * {@code Graphics2D} que toque. Aquí la cuenta está a la vista y no depende de
	 * nada.
	 *
	 * @param avance 0 devuelve la primera, 1 devuelve la segunda
	 */
	private static BufferedImage mezclar(BufferedImage desde, BufferedImage hasta, double avance) {

		int ancho = desde.getWidth();
		int alto = desde.getHeight();

		BufferedImage mezcla = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < alto; y++) {
			for (int x = 0; x < ancho; x++) {

				int a = desde.getRGB(x, y);
				int b = hasta.getRGB(x, y);

				int rojo = interpolar((a >> 16) & 0xFF, (b >> 16) & 0xFF, avance);
				int verde = interpolar((a >> 8) & 0xFF, (b >> 8) & 0xFF, avance);
				int azul = interpolar(a & 0xFF, b & 0xFF, avance);

				mezcla.setRGB(x, y, (rojo << 16) | (verde << 8) | azul);
			}
		}

		return mezcla;
	}

	private static int interpolar(int desde, int hasta, double avance) {
		return (int) Math.round(desde + (hasta - desde) * avance);
	}

	/**
	 * Escribe la secuencia como GIF animado.
	 *
	 * <p>
	 * <b>Esto es más enrevesado de lo que debería, y conviene saber por qué.</b> El
	 * escritor de GIF de Java no tiene ningún método para decir "este fotograma
	 * dura tanto" ni "repite en bucle": esas dos cosas viven en extensiones del
	 * formato, y la única forma de tocarlas desde ImageIO es <b>construir a mano el
	 * árbol de metadatos</b> del fotograma. De ahí los {@code IIOMetadataNode}.
	 *
	 * <p>
	 * <b>La extensión de bucle es de Netscape</b>, literalmente: el formato GIF de
	 * 1989 no contempla repetir, y la repetición infinita que hoy se da por
	 * supuesta la inventó aquel navegador como un bloque de aplicación con su
	 * nombre dentro. Todos los visores la entienden desde entonces. Va solo en el
	 * primer fotograma, que es donde el formato la busca.
	 */
	private static void escribirGif(List<Fotograma> secuencia, File destino) throws IOException {

		Iterator<ImageWriter> escritores = ImageIO.getImageWritersByFormatName("gif");

		if (!escritores.hasNext()) {
			throw new IllegalStateException("Esta máquina virtual no trae escritor de GIF");
		}

		ImageWriter escritor = escritores.next();

		try (ImageOutputStream flujo = ImageIO.createImageOutputStream(destino)) {

			escritor.setOutput(flujo);
			escritor.prepareWriteSequence(null);

			ImageWriteParam parametros = escritor.getDefaultWriteParam();
			ImageTypeSpecifier tipo = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);

			for (int i = 0; i < secuencia.size(); i++) {

				Fotograma fotograma = secuencia.get(i);

				IIOMetadata metadatos = escritor.getDefaultImageMetadata(tipo, parametros);
				configurar(metadatos, fotograma.centesimas, i == 0);

				escritor.writeToSequence(new IIOImage(fotograma.imagen, null, metadatos), parametros);
			}

			escritor.endWriteSequence();

		} finally {
			escritor.dispose();
		}
	}

	private static void configurar(IIOMetadata metadatos, int centesimas, boolean primero) throws IOException {

		String formato = metadatos.getNativeMetadataFormatName();
		IIOMetadataNode raiz = (IIOMetadataNode) metadatos.getAsTree(formato);

		IIOMetadataNode control = nodo(raiz, "GraphicControlExtension");
		control.setAttribute("disposalMethod", "none");
		control.setAttribute("userInputFlag", "FALSE");
		control.setAttribute("transparentColorFlag", "FALSE");
		control.setAttribute("transparentColorIndex", "0");
		control.setAttribute("delayTime", String.valueOf(centesimas));

		if (primero) {

			IIOMetadataNode extensiones = nodo(raiz, "ApplicationExtensions");
			IIOMetadataNode bucle = new IIOMetadataNode("ApplicationExtension");

			bucle.setAttribute("applicationID", "NETSCAPE");
			bucle.setAttribute("authenticationCode", "2.0");

			// El 1 es el subtipo "número de repeticiones" y los dos ceros que siguen son
			// ese número en little endian: cero significa "sin límite".
			bucle.setUserObject(new byte[] { 0x1, 0x0, 0x0 });

			extensiones.appendChild(bucle);
		}

		metadatos.setFromTree(formato, raiz);
	}

	/** Devuelve el hijo con ese nombre, creándolo si el árbol no lo trae. */
	private static IIOMetadataNode nodo(IIOMetadataNode raiz, String nombre) {

		for (int i = 0; i < raiz.getLength(); i++) {

			if (nombre.equalsIgnoreCase(raiz.item(i).getNodeName())) {
				return (IIOMetadataNode) raiz.item(i);
			}
		}

		IIOMetadataNode nuevo = new IIOMetadataNode(nombre);
		raiz.appendChild(nuevo);

		return nuevo;
	}

	/**
	 * Reduce al ancho final manteniendo la proporción.
	 *
	 * <p>
	 * En dos pasos y con interpolación bicúbica. Es la misma precaución que ya está
	 * escrita en {@code GenerarAssets}: bajar de golpe descarta píxeles en lugar de
	 * promediarlos y deja el texto dentado, que en un GIF se nota el doble porque
	 * los bordes sucios malgastan la paleta de 256 colores.
	 */
	private static BufferedImage reducir(BufferedImage original) {

		int alto = Math.round(original.getHeight() * (ANCHO_FINAL / (float) original.getWidth()));

		BufferedImage intermedia = escalar(original, (original.getWidth() + ANCHO_FINAL) / 2,
				(original.getHeight() + alto) / 2);

		return escalar(intermedia, ANCHO_FINAL, alto);
	}

	private static BufferedImage escalar(BufferedImage origen, int ancho, int alto) {

		BufferedImage destino = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);

		Graphics2D g2 = destino.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.drawImage(origen, 0, 0, ancho, alto, null);
		g2.dispose();

		return destino;
	}

	/** Pinta la ventana fuera de la pantalla. Ver {@code ScreenSnapshots.dibujar}. */
	private static BufferedImage dibujar(JFrame ventana) {

		ventana.setSize(new Dimension(ANCHO_DE_PINTADO, ALTO_DE_PINTADO));
		ventana.setLocation(-20000, -20000);
		ventana.setVisible(true);

		disponer(ventana.getContentPane());

		int ancho = ventana.getContentPane().getWidth();
		int alto = ventana.getContentPane().getHeight();

		BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);

		Graphics2D g2 = imagen.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		ventana.getContentPane().printAll(g2);
		g2.dispose();

		return imagen;
	}

	private static void disponer(Component componente) {

		synchronized (componente.getTreeLock()) {

			componente.doLayout();

			if (componente instanceof Container) {
				for (Component hijo : ((Container) componente).getComponents()) {
					disponer(hijo);
				}
			}
		}
	}

	private static void comprobarQueLaBaseEsDesechable(ConfigurableApplicationContext context) {

		String url = context.getEnvironment().getProperty("spring.datasource.url", "");

		if (!url.startsWith("jdbc:h2:mem:")) {
			throw new IllegalStateException(
					"Esta herramienta crea un usuario de prueba y solo debe usar una base en memoria. Conectada a: "
							+ url);
		}
	}

	private static void abrirSesion(ConfigurableApplicationContext context) {

		UserService userService = context.getBean(UserService.class);
		SessionManager sessionManager = context.getBean(SessionManager.class);

		// El nombre de usuario sale en la cabecera, así que no puede ser el nombre de
		// la herramienta: un "GIF" ahí arriba delata el andamiaje.
		User usuario = new User("alex", "1234", "Alejandro", "Sampedro", "Coruña", 666777892, "alex@actihome.example",
				LocalDateTime.now().minusYears(32), RoleType.ADMIN);

		try {
			userService.signUp(usuario);
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo crear el usuario del GIF", ex);
		}

		sessionManager.login(usuario);
	}

	/** Una imagen y cuánto se queda en pantalla, en centésimas de segundo. */
	private static final class Fotograma {

		private final BufferedImage imagen;
		private final int centesimas;

		private Fotograma(BufferedImage imagen, int centesimas) {
			this.imagen = imagen;
			this.centesimas = centesimas;
		}
	}
}
