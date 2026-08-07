package fp.project.actihome.ui.dev;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import fp.project.actihome.ActihomeApplication;
import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.Review;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.model.services.MessageService;
import fp.project.actihome.model.services.ReviewService;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.ChangePasswordFrame;
import fp.project.actihome.ui.ComparisonFrame;
import fp.project.actihome.ui.ConversationFrame;
import fp.project.actihome.ui.DoCheckInFrame;
import fp.project.actihome.ui.HousingDetailsFrame;
import fp.project.actihome.ui.LoginFrame;
import fp.project.actihome.ui.RecoverPasswordFrame;
import fp.project.actihome.ui.MessagesFrame;
import fp.project.actihome.ui.OnboardingFrame;
import fp.project.actihome.ui.OwnerPanelFrame;
import fp.project.actihome.ui.PlatformPanelFrame;
import fp.project.actihome.ui.PublishReviewFrame;
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
 * Comprueba que <b>ninguna pantalla se rompe a ningún tamaño de ventana</b>.
 *
 * <pre>
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.MedirResponsive"
 * </pre>
 *
 * <p>
 * <b>Por qué hacía falta otra herramienta.</b> {@link MedirPantallas} responde a
 * "¿cabe esta pantalla en <em>este</em> escritorio?", que es una pregunta sobre
 * la máquina de quien la ejecuta. La pregunta de verdad es otra: <b>¿se ve bien
 * en cualquier pantalla del cliente?</b> Y esa no se puede contestar mirando el
 * escritorio propio, porque el escalado de Windows cambia el tamaño
 * <em>lógico</em> de la pantalla: un portátil de 1920×1080 al 150 % le ofrece a
 * la aplicación 1280×720 puntos, no 1920×1080. Diseñar mirando un monitor sin
 * escalar y probar en él es garantizar que en el portátil del cliente se rompe.
 *
 * <p>
 * <b>Cómo detecta un recorte sin mirar.</b> Coloca la ventana en cada tamaño de
 * la lista, deja que el layout se resuelva y luego recorre el árbol de
 * componentes comparando el tamaño <em>real</em> de cada uno con su tamaño
 * <em>mínimo</em>. Si un componente ha quedado por debajo de su mínimo, está
 * aplastado: un texto que se dibuja con puntos suspensivos, un botón fuera de
 * sitio, un chip ilegible. Es exactamente el fallo que las capturas no enseñan,
 * porque hay que tener el escalado del cliente para verlo.
 *
 * <p>
 * Los contenedores con barra de desplazamiento se excluyen del recuento: ahí
 * quedarse corto es el comportamiento previsto, para eso está el scroll.
 */
public class MedirResponsive {

	/**
	 * Los tamaños que hay que soportar, en puntos lógicos.
	 *
	 * <p>
	 * No son tamaños de monitor, son <b>tamaños de ventana</b> después de aplicar el
	 * escalado de Windows. El más pequeño de la lista es el caso real que rompió la
	 * aplicación: un portátil 1920×1080 al 150 % deja 1280×720 puntos, y quitando la
	 * barra de tareas y el marco de la ventana quedan unos 1280×660.
	 */
	private static final int[][] TAMANOS = {
			{ 1024, 600 },   // el mínimo que nos comprometemos a soportar
			{ 1280, 660 },   // portátil 1920x1080 al 150 % (el caso del usuario)
			{ 1366, 700 },   // portátil 1366x768 sin escalar
			{ 1536, 800 },   // 1920x1080 al 125 %
			{ 1920, 1040 },  // 1920x1080 sin escalar
			{ 2560, 1350 },  // monitor 2K sin escalar
	};

	public static void main(String[] args) throws Exception {

		System.setProperty("java.awt.headless", "false");
		ActiHomeTheme.install();

		// Sin animaciones: una herramienta que lea un fotograma intermedio da un
		// resultado distinto en cada ejecucion. Ver Animacion.desactivarParaHerramientas.
		Animacion.desactivarParaHerramientas();

		SpringApplication app = new SpringApplication(ActihomeApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);

		int problemas = 0;

		// La previsión meteorológica va con el cliente de ejemplo y no con el de
		// verdad. Dos motivos: una comprobación automática no puede depender de que un
		// servicio de un tercero esté de pie, y una respuesta que llega por red hace la
		// medición indeterminista —el panel aparecería a mitad del recorrido del árbol
		// según lo rápida que fuera la red—. Ver WeatherClientDeEjemplo.
		try (ConfigurableApplicationContext c = app.run("--spring.datasource.url=jdbc:h2:mem:responsive;DB_CLOSE_DELAY=-1;MODE=MySQL",
				"--spring.datasource.username=sa", "--actihome.meteorologia.habilitada=false", "--actihome.mapa.habilitado=false")) {

			SessionManager sesion = c.getBean(SessionManager.class);
			UserService usuarios = c.getBean(UserService.class);
			HousingService alojamientos = c.getBean(HousingService.class);
			ReviewService resenas = c.getBean(ReviewService.class);
			MessageService mensajeria = c.getBean(MessageService.class);

			User lucia = usuarios.login("Lucia", "1234");
			sesion.login(lucia);

			Housing propio = alojamientos.showHousings().stream()
					.filter(h -> h.getHousingCode().equals(10001L)).findFirst().orElseThrow();
			Housing otro = alojamientos.showHousings().stream()
					.filter(h -> h.getHousingCode().equals(10002L)).findFirst().orElseThrow();

			// Se enriquece con foto y respuesta (F15) para que "Detalle resena" y
			// "Editar resena" midan de verdad esas dos filas nuevas, no una reseña
			// pelada. El nombre de foto no existe en disco -eso no hace falta aquí-, lo
			// que importa es que el layout reserve su hueco como si la hubiera.
			Review propiaResena = resenas.showHousingReviews(propio.getId()).get(0);
			resenas.setReviewImage(propiaResena.getId(), propiaResena.getAuthor().getId(), "medir-responsive.jpg");
			resenas.respondToReview(propiaResena.getId(), lucia.getId(),
					"Gracias por la reseña, esperamos verte pronto de nuevo.");

			// F10: una conversación real de dos mensajes, para que "Mensajes" y
			// "Conversacion" midan el hilo con contenido y no una bandeja vacía.
			mensajeria.sendMessage(propiaResena.getAuthor().getId(), lucia.getId(), propio.getId(),
					"Hola, ¿el alojamiento admite mascotas pequeñas?");
			mensajeria.sendMessage(lucia.getId(), propiaResena.getAuthor().getId(), propio.getId(),
					"¡Hola! Sí, admitimos mascotas pequeñas sin problema.");

			List<Pantalla> pantallas = new ArrayList<>();
			pantallas.add(new Pantalla("Catalogo", c.getBean(ShowHousingsFrame.class), null));
			pantallas.add(new Pantalla("Detalle", c.getBean(HousingDetailsFrame.class),
					f -> ((HousingDetailsFrame) f).loadDetails(propio)));
			pantallas.add(new Pantalla("Reservar", c.getBean(ReserveHousingFrame.class),
					f -> ((ReserveHousingFrame) f).setHousingId(propio.getId())));
			pantallas.add(new Pantalla("Mis reservas", c.getBean(ShowMyReservationsFrame.class), null));
			pantallas.add(new Pantalla("Check-in", c.getBean(DoCheckInFrame.class), null));
			pantallas.add(new Pantalla("Resenas", c.getBean(ShowReviewsFrame.class),
					f -> ((ShowReviewsFrame) f).setHousingId(propio.getId())));
			pantallas.add(new Pantalla("Detalle resena", c.getBean(ReviewDetailsFrame.class),
					f -> ((ReviewDetailsFrame) f).loadDetails(propiaResena)));
			pantallas.add(new Pantalla("Publicar resena", c.getBean(PublishReviewFrame.class),
					f -> ((PublishReviewFrame) f).setHousingId(propio.getId())));
			pantallas.add(new Pantalla("Editar resena", c.getBean(UpdateReviewFrame.class),
					f -> ((UpdateReviewFrame) f).setReviewId(propiaResena.getId())));
			pantallas.add(new Pantalla("Alta alojamiento", c.getBean(UploadHousingFrame.class), null));
			pantallas.add(new Pantalla("Editar alojamiento", c.getBean(UpdateHousingFrame.class),
					f -> ((UpdateHousingFrame) f).setHousingId(propio.getId())));
			pantallas.add(new Pantalla("Intercambio", c.getBean(TradeHousingsFrame.class),
					f -> ((TradeHousingsFrame) f).setHousingId(propio.getId())));
			// "Perfil" ya no es una pantalla propia: la Fase 8.4 la fusiono con Ajustes.
			pantallas.add(new Pantalla("Contrasena", c.getBean(ChangePasswordFrame.class), null));
			pantallas.add(new Pantalla("Ajustes", c.getBean(SettingsFrame.class), null));
			pantallas.add(new Pantalla("Login", c.getBean(LoginFrame.class), null));
			pantallas.add(new Pantalla("Recuperar", c.getBean(RecoverPasswordFrame.class), null));
			pantallas.add(new Pantalla("Registro", c.getBean(SignUpFrame.class), null));
			pantallas.add(new Pantalla("Bienvenida", c.getBean(OnboardingFrame.class), null));
			pantallas.add(new Pantalla("Panel propietario", c.getBean(OwnerPanelFrame.class), null));
			pantallas.add(new Pantalla("Panel plataforma", c.getBean(PlatformPanelFrame.class), null));
			pantallas.add(new Pantalla("Mensajes", c.getBean(MessagesFrame.class), null));
			pantallas.add(new Pantalla("Conversacion", c.getBean(ConversationFrame.class),
					f -> ((ConversationFrame) f).setConversacion(propiaResena.getAuthor(), propio)));
			pantallas.add(new Pantalla("Comparar", c.getBean(ComparisonFrame.class),
					f -> ((ComparisonFrame) f).loadHousings(List.of(propio.getId(), otro.getId()))));

			System.out.printf("%-22s %11s", "pantalla", "minimo");
			for (int[] t : TAMANOS) {
				System.out.printf(" %11s", t[0] + "x" + t[1]);
			}
			System.out.println();
			System.out.println("-".repeat(34 + TAMANOS.length * 12));

			for (Pantalla p : pantallas) {
				problemas += p.revisar();
			}

			System.out.println();
			System.out.println("--- autocontrol: por debajo del suelo (600x400) deberia romperse ---");

			int rotasAdrede = 0;

			for (Pantalla p : pantallas) {
				rotasAdrede += p.rompeA(600, 400) ? 1 : 0;
			}

			System.out.println(rotasAdrede + " de " + pantallas.size() + " pantallas acusan el tamano imposible.");

			// Si a 600x400 no se queja NINGUNA, el detector no está detectando y el "sin
			// recortes" de arriba no vale nada. Es la lección de la primera versión de esta
			// herramienta, que daba todo por bueno porque buscaba el fallo equivocado.
			if (rotasAdrede == 0) {
				System.out.println("AVISO: el detector no detecta. Revisa buscarAplastados antes de fiarte.");
				problemas++;
			}
		}

		System.out.println();
		System.out.println(problemas == 0 ? "SIN RECORTES en ningun tamano."
				: "TOTAL de componentes aplastados: " + problemas);

		System.exit(problemas == 0 ? 0 : 1);
	}

	interface Prep {
		void run(JFrame f);
	}

	/** Una pantalla y lo que hay que darle antes de mostrarla. */
	private static class Pantalla {

		private final String nombre;
		private final JFrame frame;
		private final Prep preparar;

		Pantalla(String nombre, JFrame frame, Prep preparar) {
			this.nombre = nombre;
			this.frame = frame;
			this.preparar = preparar;
		}

		int revisar() throws Exception {

			if (preparar != null) {
				preparar.run(frame);
			}

			// El mínimo declarado por la pantalla impediría probar tamaños pequeños: se
			// anula aquí a propósito, porque lo que se quiere medir es si el CONTENIDO
			// aguanta, no si la ventana se deja encoger.
			frame.setMinimumSize(new Dimension(1, 1));
			frame.setLocation(-30000, -30000);
			frame.setVisible(true);

			// El mínimo real que el contenido exige en ESTA máquina. Es el número que
			// decide si la aplicación cabe en el portátil del cliente, y el que antes se
			// escribía a mano en cada pantalla.
			frame.setSize(1400, 900);
			frame.validate();
			Dimension exige = frame.getContentPane().getMinimumSize();

			int total = 0;
			StringBuilder linea = new StringBuilder(
					String.format("%-22s %11s", nombre, exige.width + "x" + exige.height));
			List<String> detalle = new ArrayList<>();

			for (int[] tamano : TAMANOS) {

				frame.setSize(tamano[0], tamano[1]);
				frame.validate();
				Thread.sleep(60);

				List<String> aplastados = new ArrayList<>();
				Rectangle ventana = new Rectangle(0, 0, frame.getContentPane().getWidth(),
						frame.getContentPane().getHeight());
				buscarAplastados(frame.getContentPane(), 0, 0, ventana, aplastados);

				total += aplastados.size();
				linea.append(String.format(" %11s", aplastados.isEmpty() ? "ok" : aplastados.size() + " ROTO"));

				for (String a : aplastados) {
					detalle.add("      " + tamano[0] + "x" + tamano[1] + "  " + a);
				}
			}

			frame.setVisible(false);

			System.out.println(linea);
			detalle.stream().distinct().limit(8).forEach(System.out::println);

			return total;
		}

		/** Autocontrol: comprueba que a un tamaño imposible la pantalla sí se queja. */
		boolean rompeA(int ancho, int alto) throws Exception {

			if (preparar != null) {
				preparar.run(frame);
			}

			frame.setMinimumSize(new Dimension(1, 1));
			frame.setLocation(-30000, -30000);
			frame.setVisible(true);
			frame.setSize(ancho, alto);
			frame.validate();
			Thread.sleep(60);

			List<String> encontrados = new ArrayList<>();
			Rectangle ventana = new Rectangle(0, 0, frame.getContentPane().getWidth(),
					frame.getContentPane().getHeight());
			buscarAplastados(frame.getContentPane(), 0, 0, ventana, encontrados);

			frame.setVisible(false);

			return !encontrados.isEmpty();
		}
	}

	/**
	 * Recorre el árbol buscando lo que el usuario no va a poder ver.
	 *
	 * <p>
	 * <b>Busca dos cosas distintas, y la segunda es la que importa de verdad.</b>
	 *
	 * <ol>
	 * <li><b>Aplastado</b>: el componente ha quedado por debajo de su tamaño mínimo,
	 * así que se dibuja recortado — el "Tod…" de los chips.</li>
	 * <li><b>Desbordado</b>: el componente conserva su tamaño pero ha quedado
	 * <em>fuera</em> del área visible de la ventana. Este es el caso frecuente y el
	 * que se me escapó al escribir la primera versión de esta herramienta: MigLayout
	 * <b>no</b> encoge por debajo del mínimo, prefiere desbordar el contenedor. Así
	 * que buscar componentes aplastados devolvía "todo correcto" mientras el botón
	 * de guardar estaba doscientos puntos por debajo del borde inferior.</li>
	 * </ol>
	 *
	 * <p>
	 * <b>Dentro de un {@code JScrollPane} sí se busca, pero solo a lo ancho</b>, y
	 * corregirlo fue la ampliación más importante que ha tenido esta herramienta.
	 * Antes se saltaba el contenido de cualquier scroll con el argumento de que
	 * «quedarse fuera ahí es el comportamiento previsto, para eso está la barra».
	 * Eso es cierto <b>en vertical</b> y falso en horizontal:
	 * {@link fp.project.actihome.ui.components.Rescate} desactiva a propósito la
	 * barra horizontal, así que lo que se salga por la derecha <b>no se puede
	 * alcanzar nunca</b>, por mucho que se agrande la ventana.
	 *
	 * <p>
	 * Y como <em>todas</em> las pantallas envuelven su cuerpo en {@code Rescate},
	 * la regla vieja significaba que esta herramienta <b>no miraba dentro de
	 * ninguna</b>: solo comprobaba el marco. Decía «sin recortes» mientras la
	 * tarjeta de preferencias de Ajustes salía cortada a 1024 puntos de ancho.
	 *
	 * <p>
	 * Es, otra vez, la misma lección: <b>una comprobación automática solo protege
	 * de la clase de fallo que sabe buscar</b>, y aquí el propio comentario que
	 * justificaba la excepción era lo que la hacía ciega.
	 *
	 * @param x        desplazamiento acumulado hasta este contenedor, en coordenadas
	 *                 del panel de contenido
	 * @param visible  el área que el usuario ve de verdad
	 */
	private static void buscarAplastados(Container contenedor, int x, int y, Rectangle visible,
			List<String> encontrados) {

		buscarAplastados(contenedor, x, y, visible, encontrados, false);
	}

	/**
	 * @param dentroDeScroll si venimos de dentro de un scroll, en cuyo caso solo se
	 *                       mira el desbordamiento horizontal: el vertical ahi es
	 *                       legitimo y lo resuelve la barra.
	 */
	private static void buscarAplastados(Container contenedor, int x, int y, Rectangle visible,
			List<String> encontrados, boolean dentroDeScroll) {

		for (Component hijo : contenedor.getComponents()) {

			if (!hijo.isVisible()) {
				continue;
			}

			Rectangle real = hijo.getBounds();
			Rectangle enVentana = new Rectangle(x + real.x, y + real.y, real.width, real.height);

			if (merecePena(hijo)) {

				Dimension minimo = hijo.getMinimumSize();

				// Un margen de 1 punto: MigLayout redondea y un punto de diferencia no
				// recorta nada.
				boolean aplastado = real.width < minimo.width - 1 || real.height < minimo.height - 1;

				// Se tolera 1 punto por el mismo motivo. Un componente cuyo borde derecho o
				// inferior cae fuera del área visible está, literalmente, sin poderse ver.
				boolean seSaleALoAncho = enVentana.x < -1 || enVentana.x + enVentana.width > visible.width + 1;
				boolean seSaleALoAlto = enVentana.y < -1 || enVentana.y + enVentana.height > visible.height + 1;

				// Dentro de un scroll, salirse por abajo es para lo que esta la barra. Por la
				// derecha no: Rescate desactiva la barra horizontal a proposito.
				boolean fuera = dentroDeScroll ? seSaleALoAncho : (seSaleALoAncho || seSaleALoAlto);

				if (aplastado || fuera) {

					encontrados.add((aplastado ? "recortado " : "FUERA      ") + describir(hijo) + "  caja "
							+ enVentana.x + "," + enVentana.y + " " + real.width + "x" + real.height + (aplastado
									? "  min " + minimo.width + "x" + minimo.height
									: ""));
				}
			}

			if (hijo instanceof Container) {
				buscarAplastados((Container) hijo, x + real.x, y + real.y, visible, encontrados,
						dentroDeScroll || hijo instanceof JScrollPane);
			}
		}
	}

	/**
	 * Filtra el ruido.
	 *
	 * <p>
	 * Un panel intermedio aplastado casi siempre es la consecuencia de que algo de
	 * dentro lo esté, y contarlo dos veces no añade información. Lo que se reporta
	 * son las <b>hojas</b>: la etiqueta, el chip o el botón concretos que el usuario
	 * va a ver cortados.
	 */
	private static boolean merecePena(Component c) {

		if (c instanceof Container && ((Container) c).getComponentCount() > 0) {
			return false;
		}

		return c.getWidth() > 0 || c.getHeight() > 0;
	}

	private static String describir(Component c) {

		String clase = c.getClass().getSimpleName();

		if (clase.isEmpty()) {
			clase = c.getClass().getSuperclass().getSimpleName();
		}

		String texto = "";

		if (c instanceof JLabel) {
			texto = ((JLabel) c).getText();
		} else if (c instanceof javax.swing.AbstractButton) {
			texto = ((javax.swing.AbstractButton) c).getText();
		}

		if (texto != null && !texto.isEmpty()) {
			clase += " \"" + (texto.length() > 28 ? texto.substring(0, 28) + "…" : texto) + "\"";
		}

		Container padre = c.getParent();

		return padre == null ? clase : clase + " (dentro de " + padre.getClass().getSimpleName() + ")";
	}
}
