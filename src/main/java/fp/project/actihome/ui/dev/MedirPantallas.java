package fp.project.actihome.ui.dev;

import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import fp.project.actihome.ActihomeApplication;
import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.ChangePasswordFrame;
import fp.project.actihome.ui.DoCheckInFrame;
import fp.project.actihome.ui.HousingDetailsFrame;
import fp.project.actihome.ui.LoginFrame;
import fp.project.actihome.ui.MessagesFrame;
import fp.project.actihome.ui.PublishReviewFrame;
import fp.project.actihome.ui.RecoverPasswordFrame;
import fp.project.actihome.ui.ReserveHousingFrame;
import fp.project.actihome.ui.ReviewDetailsFrame;
import fp.project.actihome.ui.SettingsFrame;
import fp.project.actihome.ui.ShowHousingsFrame;
import fp.project.actihome.ui.ShowMyReservationsFrame;
import fp.project.actihome.ui.ShowReviewsFrame;
import fp.project.actihome.ui.SignUpFrame;
import fp.project.actihome.ui.TradeHousingsFrame;
import fp.project.actihome.ui.UpdateHousingFrame;
import fp.project.actihome.ui.UpdateReviewFrame;
import fp.project.actihome.ui.UploadHousingFrame;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.ActiHomeTheme;
import fp.project.actihome.ui.theme.Animacion;

/**
 * Comprueba que cada pantalla <b>cabe entera</b> en un portátil, y cuando no
 * cabe dice <b>dónde se va el alto</b>.
 *
 * <pre>
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.MedirPantallas"
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.MedirPantallas" "-Dexec.args=Registro"
 * </pre>
 *
 * <h2>Qué cambió y por qué</h2>
 *
 * <p>
 * <b>La versión anterior medía contra el escritorio de quien la ejecutaba</b>, y
 * eso la volvía inútil justo para lo que hacía falta: en un monitor de 2560×1392
 * daba "sí" a pantallas que en cualquier portátil se salían. Es la misma clase de
 * error que las capturas siempre a 1400×900 —comprobar en el sitio cómodo— y ya
 * había mordido dos veces a este proyecto. Ahora mide contra <b>tres tamaños de
 * portátil reales</b>, y el escritorio propio no interviene.
 *
 * <p>
 * <b>Y decía "falta 226px" sin decir de dónde.</b> Eso obliga a arreglar a
 * ciegas: se toca lo que parece grande, se vuelve a medir y se descubre que no
 * era eso. Pasó exactamente así con la ficha de alojamiento, donde se bajó el
 * preferido de la galería —que no movió el número ni un punto— antes de ver que
 * el cuello de botella estaba en la otra columna. Ahora, cuando algo no cabe,
 * imprime el reparto del alto por bloques.
 *
 * <h2>La regla que comprueba</h2>
 *
 * <p>
 * Del manual del proyecto: <em>«Cada pantalla cabe en la ventana. Sin scroll de
 * página. El scroll vive dentro de las listas»</em>. Por eso hay dos categorías:
 *
 * <ul>
 * <li><b>Pantallas de tarea</b> — formularios, login, registro, check-in. Tienen
 * que caber enteras. Si no caben, es un fallo.</li>
 * <li><b>Pantallas de lista</b> — catálogo, mis reservas, reseñas, mensajes. Su
 * alto preferido es el de la lista completa y por definición no cabe: para eso
 * está la barra. Se miden igual, pero solo se informa.</li>
 * </ul>
 *
 * <h2>Mide el mínimo, no el preferido, y esa es la tercera versión</h2>
 *
 * <p>
 * La pregunta que hay que contestar no es «¿ocupa la pantalla más de lo que hay?»
 * sino <b>«¿aparece la barra de desplazamiento?»</b>, y desde que
 * {@link fp.project.actihome.ui.components.Rescate} aprieta el aire antes de sacar
 * la barra, esas dos preguntas ya no tienen la misma respuesta. Una pantalla puede
 * preferir 780 puntos, apretar hasta 640 y caber sin barra en 672.
 *
 * <p>
 * Se imprimen las dos cifras a propósito. El <b>mínimo</b> es el que aprueba o
 * suspende; el <b>ideal</b> dice cuánto aire hay que ceder para llegar hasta él, y
 * una pantalla que sistemáticamente se ve apretada en los tres tamaños está
 * pidiendo menos contenido, no más rangos. Quedarse solo con el mínimo escondería
 * eso.
 *
 * <p>
 * Devuelve código de salida distinto de cero si alguna pantalla de tarea no
 * cabe, así que sirve tal cual como comprobación automática.
 */
public final class MedirPantallas {

	/**
	 * Los tamaños contra los que se mide.
	 *
	 * <p>
	 * No son inventados: son lo que de verdad recibe la aplicación en los tres
	 * equipos más frecuentes, ya descontada la barra de tareas. El tercero es el
	 * caso duro y el que hay que respetar — un portátil de 1920×1080 con el
	 * escalado de Windows al 150 % no le entrega a Swing 1080 puntos, le entrega
	 * <b>720</b>, porque el escalado no agranda la aplicación: le da menos sitio.
	 */
	private static final Objetivo[] OBJETIVOS = {
			new Objetivo("1536x824", 1536, 824, "1080p al 125 %"),
			new Objetivo("1366x728", 1366, 728, "portatil de 1366x768"),
			new Objetivo("1280x680", 1280, 680, "1080p al 150 %") };

	private MedirPantallas() {
	}

	public static void main(String[] args) throws Exception {

		System.setProperty("java.awt.headless", "false");
		ActiHomeTheme.install();
		Animacion.desactivarParaHerramientas();

		String soloEsta = args.length > 0 ? args[0] : null;

		SpringApplication app = new SpringApplication(ActihomeApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);

		int fallos = 0;

		try (ConfigurableApplicationContext c = app.run(
				"--spring.datasource.url=jdbc:h2:mem:medir;DB_CLOSE_DELAY=-1;MODE=MySQL",
				"--spring.datasource.username=sa",
				// Sin red: ver la nota de WeatherClientDeEjemplo.
				"--actihome.meteorologia.habilitada=false", "--actihome.mapa.habilitado=false")) {

			SessionManager sm = c.getBean(SessionManager.class);
			UserService us = c.getBean(UserService.class);
			HousingService hs = c.getBean(HousingService.class);

			sm.login(us.login("Lucia", "1234"));

			Housing propio = hs.showHousings().stream().filter(h -> h.getHousingCode().equals(10001L)).findFirst()
					.orElseThrow();

			List<Pantalla> pantallas = new ArrayList<>();

			// De tarea: tienen que caber.
			pantallas.add(tarea("Registro", c.getBean(SignUpFrame.class), null));
			pantallas.add(tarea("Login", c.getBean(LoginFrame.class), null));
			pantallas.add(tarea("Recuperar", c.getBean(RecoverPasswordFrame.class), null));
			pantallas.add(tarea("Alta alojamiento", c.getBean(UploadHousingFrame.class), null));
			pantallas.add(tarea("Editar alojamiento", c.getBean(UpdateHousingFrame.class),
					f -> ((UpdateHousingFrame) f).setHousingId(propio.getId())));
			pantallas.add(tarea("Publicar resena", c.getBean(PublishReviewFrame.class),
					f -> ((PublishReviewFrame) f).setHousingId(propio.getId())));
			pantallas.add(tarea("Editar resena", c.getBean(UpdateReviewFrame.class), null));
			pantallas.add(tarea("Reservar", c.getBean(ReserveHousingFrame.class),
					f -> ((ReserveHousingFrame) f).setHousingId(propio.getId())));
			pantallas.add(tarea("Check-in", c.getBean(DoCheckInFrame.class), null));
			pantallas.add(tarea("Contrasena", c.getBean(ChangePasswordFrame.class), null));
			pantallas.add(tarea("Ajustes", c.getBean(SettingsFrame.class), null));
			pantallas.add(tarea("Intercambio", c.getBean(TradeHousingsFrame.class),
					f -> ((TradeHousingsFrame) f).setHousingId(propio.getId())));
			pantallas.add(tarea("Detalle", c.getBean(HousingDetailsFrame.class),
					f -> ((HousingDetailsFrame) f).loadDetails(propio)));
			pantallas.add(tarea("Detalle resena", c.getBean(ReviewDetailsFrame.class), null));

			// De lista: su alto es el del contenido y el scroll es lo correcto.
			pantallas.add(lista("Catalogo", c.getBean(ShowHousingsFrame.class), null));
			pantallas.add(lista("Mis reservas", c.getBean(ShowMyReservationsFrame.class), null));
			pantallas.add(lista("Resenas", c.getBean(ShowReviewsFrame.class),
					f -> ((ShowReviewsFrame) f).setHousingId(propio.getId())));
			pantallas.add(lista("Mensajes", c.getBean(MessagesFrame.class), null));

			for (Objetivo objetivo : OBJETIVOS) {

				System.out.println();
				System.out.println("=== " + objetivo.nombre + "  (" + objetivo.descripcion + ") ===");
				System.out.printf("%-22s %8s %8s %8s  %s%n", "pantalla", "ideal", "apretado", "cabe en", "");

				for (Pantalla pantalla : pantallas) {

					if (soloEsta != null && !pantalla.nombre.equalsIgnoreCase(soloEsta)) {
						continue;
					}

					if (!medir(pantalla, objetivo)) {
						fallos++;
					}
				}
			}

			System.out.println();
			System.out.println("=== en su propio tamano de apertura ===");
			System.out.printf("%-22s %8s %8s %8s  %s%n", "pantalla", "ideal", "apretado", "cabe en", "");

			for (Pantalla pantalla : pantallas) {

				if (soloEsta != null && !pantalla.nombre.equalsIgnoreCase(soloEsta)) {
					continue;
				}

				if (!medir(pantalla, new Objetivo(pantalla.nombre, pantalla.suTamano.width, pantalla.suTamano.height,
						"su setSize"))) {
					fallos++;
				}
			}

			if (!autocontrol(pantallas.get(0))) {
				fallos++;
			}
		}

		System.out.println();

		if (fallos == 0) {
			System.out.println("TODAS LAS PANTALLAS DE TAREA CABEN en los tres tamanos.");
		} else {
			System.out.println(fallos + " casos en los que una pantalla de tarea NO CABE.");
		}

		System.exit(fallos == 0 ? 0 : 1);
	}

	/**
	 * Mide una pantalla contra un objetivo, y si no cabe imprime el reparto.
	 *
	 * @return {@code false} solo si es una pantalla de tarea y no cabe
	 */
	private static boolean medir(Pantalla pantalla, Objetivo objetivo) throws Exception {

		JFrame frame = pantalla.frame;

		if (pantalla.preparar != null) {
			pantalla.preparar.run(frame);
		}

		// **La ventana se pone al tamaño objetivo ANTES de medir, y eso es la mitad de
		// la herramienta.** El alto que necesita un contenido depende del ancho que le
		// den: un párrafo en una columna estrecha ocupa más líneas. Medir el preferido
		// a un ancho cualquiera y compararlo con el alto de otro es lo que producía
		// números que no correspondían a nada.
		frame.setSize(objetivo.ancho, objetivo.alto);
		frame.setLocation(-20000, -20000);
		frame.setVisible(true);

		Thread.sleep(120);
		frame.validate();

		Insets bordes = frame.getInsets();
		int disponible = objetivo.alto - bordes.top - bordes.bottom;

		Container contenido = frame.getContentPane();

		int ideal = contenido.getPreferredSize().height;

		// **El veredicto se lo da la barra, no una cuenta.** La versión anterior de
		// esto comparaba el mínimo del panel de contenido con el alto de la ventana, y
		// decía que las dieciocho pantallas cabían. Era falso, y de la peor manera: el
		// JScrollPane del rescate declara mínimo cero **a propósito** —para que, cuando
		// falta sitio, ceda él y no la cabecera— así que preguntarle su mínimo es
		// preguntarle a quien está diseñado para contestar cero.
		//
		// La pregunta de verdad es «¿le sale la barra al rescate?», y eso no hay que
		// modelarlo: la ventana ya está construida y colocada al tamaño objetivo, así
		// que basta con mirar si la barra está visible. Un modelo puede equivocarse;
		// la barra es el hecho.
		JScrollPane rescate = buscarRescate(contenido);

		boolean cabe = rescate == null || !rescate.getVerticalScrollBar().isVisible();

		// **El alto de ventana por debajo del cual sale la barra**, medido y no
		// estimado. Como la ventana ya está al tamaño objetivo y el contenido no cabe,
		// todo lo que rodea al rescate está ya apretado a su mínimo; así que lo que
		// ocupa ese marco es exactamente lo que le falta al visor para llegar al alto
		// disponible. Sumarle el mínimo del contenido da el alto exacto.
		//
		// El primer intento restaba el preferido del rescate al preferido de la
		// pantalla, y eso cargaba en la cuenta un margen que sí cede: daba "falta 114"
		// donde faltaban 32.
		int apretado = ideal;

		if (rescate != null) {

			Component vista = rescate.getViewport().getView();
			int marco = disponible - rescate.getViewport().getHeight();

			apretado = marco + vista.getMinimumSize().height;
		}

		// La columna "apretado" solo se imprime cuando el contenido está de verdad
		// apretado. Si la pantalla cabe holgada, el marco no está en su mínimo y esa
		// cuenta daría un número mayor que el ideal, que es un absurdo que invita a no
		// leer la tabla.
		System.out.printf("%-22s %8d %8s %8d  %s%n", pantalla.nombre, ideal,
				ideal <= disponible ? "-" : String.valueOf(apretado), disponible,
				cabe ? (ideal <= disponible ? "ok" : "ok, apretando " + (ideal - disponible))
						: (pantalla.esLista ? "scroll (correcto: es una lista)"
								: "NO CABE, falta " + Math.max(1, apretado - disponible)));

		if (!cabe && !pantalla.esLista) {
			desglosar(contenido, 0);
		}

		frame.setVisible(false);

		return cabe || pantalla.esLista;
	}

	/**
	 * Comprueba que la herramienta sabe fallar.
	 *
	 * <p>
	 * <b>A 420×300 no cabe nada, así que la barra de rescate tiene que salir.</b> Si
	 * también ahí dice que todo va bien, es que no está detectando nada y su "todas
	 * caben" no vale como prueba. Este proyecto ya se comió esa lección tres veces
	 * con {@code MedirResponsive} —y una cuarta hace diez minutos con esta misma
	 * clase, que dio las dieciocho por buenas midiendo un mínimo que siempre valía
	 * cero—, así que el autocontrol va dentro y no en un comentario.
	 *
	 * @return {@code true} si la herramienta se ha quejado, que es lo que se espera
	 */
	private static boolean autocontrol(Pantalla pantalla) throws Exception {

		JFrame frame = pantalla.frame;

		frame.setSize(420, 300);
		frame.setLocation(-20000, -20000);
		frame.setVisible(true);

		Thread.sleep(120);
		frame.validate();

		JScrollPane rescate = buscarRescate(frame.getContentPane());
		boolean seQueja = rescate != null && rescate.getVerticalScrollBar().isVisible();

		frame.setVisible(false);

		System.out.println();
		System.out.println("autocontrol (" + pantalla.nombre + " a 420x300): "
				+ (seQueja ? "la barra aparece, el detector detecta"
						: "LA BARRA NO APARECE — el detector esta ciego y su resultado no vale"));

		return seQueja;
	}

	/**
	 * El {@link fp.project.actihome.ui.components.Rescate} de la pantalla, si lo
	 * tiene.
	 *
	 * <p>
	 * Se busca por la marca que pone {@code Rescate} y no por ser un
	 * {@code JScrollPane}, porque hay dos clases de scroll en esta aplicación y solo
	 * una es un fallo: el de una lista es correcto —para eso está— y el de rescate
	 * no debería aparecer nunca en una ventana de tamaño real.
	 */
	private static JScrollPane buscarRescate(Container contenedor) {

		for (Component hijo : contenedor.getComponents()) {

			if (hijo instanceof JScrollPane
					&& Boolean.TRUE.equals(((JScrollPane) hijo).getClientProperty(fp.project.actihome.ui.components.Rescate.MARCA))) {

				return (JScrollPane) hijo;
			}

			if (hijo instanceof Container) {

				JScrollPane encontrado = buscarRescate((Container) hijo);

				if (encontrado != null) {
					return encontrado;
				}
			}
		}

		return null;
	}

	/**
	 * Imprime en qué se va el alto, bloque a bloque.
	 *
	 * <p>
	 * <b>Baja solo por el hijo más alto de cada nivel</b>, no por todos. Es lo que
	 * hace la salida legible: en un árbol de Swing con doscientos componentes, un
	 * volcado completo no se lee, y lo que hace que una pantalla no quepa es siempre
	 * una <em>cadena</em> concreta de contenedores anidados. Seguir al más alto es
	 * seguir esa cadena.
	 *
	 * <p>
	 * Un {@code JScrollPane} corta el recorrido: por debajo de él el alto ya no
	 * cuenta, porque ahí sí hay barra. Que la cadena termine en uno es la respuesta,
	 * no un punto muerto.
	 */
	private static void desglosar(Container contenedor, int profundidad) {

		if (profundidad > 4) {
			return;
		}

		Component masAlto = null;
		int total = 0;

		for (Component hijo : contenedor.getComponents()) {

			if (!hijo.isVisible()) {
				continue;
			}

			total += hijo.getPreferredSize().height;

			// **Se sigue al de mayor preferido, no al de mayor mínimo**, aunque lo que
			// decide el veredicto sea el mínimo. Y el motivo es el mismo scroll de rescate
			// de siempre: declara mínimo cero, así que por mínimo el bloque "más grande"
			// de media aplicación acaba siendo la cabecera de 72 puntos y el desglose se
			// para justo antes de entrar donde está el problema.
			if (masAlto == null || hijo.getPreferredSize().height > masAlto.getPreferredSize().height) {
				masAlto = hijo;
			}
		}

		if (masAlto == null) {
			return;
		}

		String sangria = "        " + "  ".repeat(profundidad);

		for (Component hijo : contenedor.getComponents()) {

			if (!hijo.isVisible()) {
				continue;
			}

			int ideal = hijo.getPreferredSize().height;
			int apretado = hijo.getMinimumSize().height;

			// Los bloques de menos de 24 puntos no explican nada y llenan la salida de
			// ruido: separadores, etiquetas sueltas, hairlines.
			if (ideal < 24 && apretado < 24) {
				continue;
			}

			// **Las dos cifras juntas, porque la interesante es la diferencia.** Un bloque
			// de 140/70 no es un problema: ya sabe ceder la mitad. Uno de 140/140 es el que
			// hay que mirar, porque cuando la ventana se queda corta ese alto se lo van a
			// tener que quitar los demás.
			System.out.printf("%s%-30s %5d %5d%s%n", sangria, nombrar(hijo), ideal, apretado,
					hijo == masAlto && contenedor.getComponentCount() > 1 ? "   <-- el que menos cede" : "");
		}

		// **Al llegar a un JScrollPane se entra en su contenido, no se para.** Es lo
		// contrario de lo que parece razonable, y el motivo es propio de esta
		// aplicación: casi todas las pantallas envuelven su cuerpo en un `Rescate`, que
		// es un JScrollPane. Ese scroll NO es el de una lista — es la barra de rescate
		// que solo debería aparecer en ventanas imposibles— así que pararse ahí deja el
		// desglose en "el alto se va en el scroll", que no dice nada. Lo que hay que
		// ver es qué lleva dentro.
		if (masAlto instanceof JScrollPane) {

			Component dentro = ((JScrollPane) masAlto).getViewport().getView();

			if (dentro instanceof Container) {
				System.out.println(sangria + "  dentro del Rescate:");
				desglosar((Container) dentro, profundidad + 1);
			}

			return;
		}

		if (masAlto instanceof Container && ((Container) masAlto).getComponentCount() > 0
				&& masAlto.getPreferredSize().height > total / 3) {

			desglosar((Container) masAlto, profundidad + 1);
		}
	}

	/** Un nombre reconocible: la clase, y el texto si lo tiene. */
	private static String nombrar(Component componente) {

		String clase = componente.getClass().getSimpleName();

		if (clase.isEmpty()) {
			clase = componente.getClass().getSuperclass().getSimpleName();
		}

		if (componente instanceof javax.swing.JLabel) {

			String texto = ((javax.swing.JLabel) componente).getText();

			if (texto != null && !texto.isBlank()) {
				return clase + " \"" + recortar(texto) + "\"";
			}
		}

		if (componente instanceof javax.swing.AbstractButton) {

			String texto = ((javax.swing.AbstractButton) componente).getText();

			if (texto != null && !texto.isBlank()) {
				return clase + " \"" + recortar(texto) + "\"";
			}
		}

		return clase;
	}

	private static String recortar(String texto) {
		return texto.length() <= 22 ? texto : texto.substring(0, 21) + "…";
	}

	private static Pantalla tarea(String nombre, JFrame frame, Preparar preparar) {
		return new Pantalla(nombre, frame, preparar, false);
	}

	private static Pantalla lista(String nombre, JFrame frame, Preparar preparar) {
		return new Pantalla(nombre, frame, preparar, true);
	}

	interface Preparar {
		void run(JFrame frame);
	}

	private static final class Pantalla {

		private final String nombre;
		private final JFrame frame;
		private final transient Preparar preparar;
		private final boolean esLista;

		/**
		 * El tamaño con el que la pantalla se abre, capturado <b>antes</b> de que esta
		 * herramienta la redimensione.
		 *
		 * <p>
		 * <b>Es el tamaño que más veces va a ver el usuario y el único que no se estaba
		 * comprobando.</b> Los tres objetivos de arriba son portátiles reales, pero
		 * ninguno es el {@code setSize} de la pantalla — y ese es justo el que Windows
		 * devuelve al restaurar una ventana maximizada. Varias pantallas se
		 * redistribuyeron en tres columnas y siguieron declarando el ancho que
		 * necesitaban cuando eran una: a ese ancho las columnas se pliegan, el
		 * contenido crece hacia abajo y aparece la barra. Un fallo que no se veía
		 * midiendo a 1280 ni a 1536.
		 */
		private final java.awt.Dimension suTamano;

		private Pantalla(String nombre, JFrame frame, Preparar preparar, boolean esLista) {
			this.nombre = nombre;
			this.frame = frame;
			this.preparar = preparar;
			this.esLista = esLista;
			this.suTamano = frame.getSize();
		}
	}

	private static final class Objetivo {

		private final String nombre;
		private final int ancho;
		private final int alto;
		private final String descripcion;

		private Objetivo(String nombre, int ancho, int alto, String descripcion) {
			this.nombre = nombre;
			this.ancho = ancho;
			this.alto = alto;
			this.descripcion = descripcion;
		}
	}
}
