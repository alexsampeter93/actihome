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
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;

import fp.project.actihome.ActihomeApplication;
import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.model.entities.ReservationDao;
import fp.project.actihome.model.entities.Review;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.model.services.ReviewService;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.DoCheckInFrame;
import fp.project.actihome.ui.HousingDetailsFrame;
import fp.project.actihome.ui.PublishReviewFrame;
import fp.project.actihome.ui.ReserveHousingFrame;
import fp.project.actihome.ui.ReviewDetailsFrame;
import fp.project.actihome.ui.ShowHousingsFrame;
import fp.project.actihome.ui.ShowMyReservationsFrame;
import fp.project.actihome.ui.ShowReviewsFrame;
import fp.project.actihome.ui.UpdateReviewFrame;
import fp.project.actihome.ui.components.Segmented;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.ActiHomeTheme;
import fp.project.actihome.ui.theme.Season;
import fp.project.actihome.ui.theme.Theme;

/**
 * Captura pantallas completas de la aplicación, con datos reales y sin abrir
 * ninguna ventana.
 *
 * <pre>
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.ScreenSnapshots"
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.ScreenSnapshots" "-Dexec.args=fase3-catalogo ADMIN"
 * </pre>
 *
 * <p>
 * <b>Por qué hace falta otra herramienta además de {@code ThemeSnapshots}.</b>
 * Aquella dibuja la guía de estilo, que es un panel suelto sin dependencias.
 * Esta dibuja <em>pantallas de la aplicación</em>, y una pantalla de ActiHome es
 * un bean de Spring que necesita servicios, base de datos y una sesión abierta.
 * Sin eso no hay nada que pintar.
 *
 * <p>
 * <b>El problema que resuelve de verdad.</b> Para ver el catálogo hay que
 * iniciar sesión, y para iniciar sesión hay que escribir en un formulario. Eso
 * convierte cada comprobación visual en "abre la aplicación, escribe, mira" —un
 * bucle lento y, sobre todo, imposible de automatizar. Aquí la sesión se abre
 * por código y la pantalla se pinta sobre una imagen en memoria.
 *
 * <p>
 * <b>Base de datos aparte.</b> Arranca contra una H2 <b>en memoria</b>, no
 * contra {@code ~/.actihome}. Dos motivos: no ensucia los datos de trabajo con
 * el usuario de prueba que crea para iniciar sesión, y el resultado es siempre
 * el mismo porque la base se siembra desde cero con {@code data.sql}. Una
 * captura que depende del estado acumulado de tu base de datos no sirve para
 * comparar dos versiones.
 */
public final class ScreenSnapshots {

	private static final String DESTINO = "docs/progreso";

	/** Base desechable: se crea al arrancar y muere con el proceso. */
	private static final String BASE_EN_MEMORIA = "jdbc:h2:mem:snapshots;DB_CLOSE_DELAY=-1;MODE=MySQL";

	/** Tamaño de ventana con el que se capturan las pantallas. */
	private static final int ANCHO = 1400;
	private static final int ALTO = 900;

	private ScreenSnapshots() {
	}

	public static void main(String[] args) throws IOException {

		System.setProperty("java.awt.headless", "false");
		ActiHomeTheme.install();

		String prefijo = args.length > 0 ? args[0] : "catalogo";
		RoleType rol = args.length > 1 ? RoleType.valueOf(args[1]) : RoleType.ADMIN;

		SpringApplication app = new SpringApplication(ActihomeApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);

		// La URL va como argumento de línea de comandos y NO con setDefaultProperties.
		// Es una diferencia que parece de matiz y no lo es: los "default properties" son
		// lo ÚLTIMO en el orden de precedencia de Spring Boot, por debajo de
		// application.yaml, así que el yaml ganaba y esta herramienta acababa
		// escribiendo su usuario de prueba en la base de datos real del usuario. Los
		// argumentos de línea de comandos están arriba del todo y sí mandan.
		try (ConfigurableApplicationContext context = app.run("--spring.datasource.url=" + BASE_EN_MEMORIA,
				"--spring.datasource.username=sa")) {

			comprobarQueLaBaseEsDesechable(context);

			if ("fase4".equals(prefijo)) {
				capturarFase4(context);

			} else if ("fase5".equals(prefijo)) {
				capturarFase5(context);

			} else {

				abrirSesion(context, "alex", rol);

				ShowHousingsFrame catalogo = context.getBean(ShowHousingsFrame.class);

				for (Season estacion : Season.values()) {

					Theme.cambiarA(estacion);
					guardar(catalogo, prefijo + "-" + estacion.name().toLowerCase());
				}

				// Y la otra vista, para poder compararlas. Se cambia por código en lugar de
				// simular un clic: el conmutador expone setActivo justamente para esto.
				Theme.cambiarA(Season.VERANO);
				buscarConmutador(catalogo).setActivo(1);
				guardar(catalogo, prefijo + "-cuadricula");
			}
		}

		// Swing deja hilos vivos (el de eventos, el de temporizadores) que impedirían
		// que el proceso termine solo. En una herramienta de línea de comandos eso se
		// traduce en una consola colgada.
		System.exit(0);
	}

	/**
	 * Captura las cuatro pantallas de la Fase 4: detalle (como cliente y como
	 * propietario), reservar, mis reservas y check-in.
	 *
	 * <p>
	 * Para "mis reservas" hacen falta sus tres estados a la vez —pendiente,
	 * realizado y completada— y el servicio no permite crear ninguno directamente:
	 * {@code reserveHousing} exige que la entrada no sea anterior a hoy, así que
	 * nunca se puede pedir una reserva ya completada por ese camino. Aquí no hace
	 * falta respetar esa regla: se están fabricando datos de attrezzo para una
	 * captura, no ejercitando el flujo de negocio, así que las tres reservas se
	 * escriben directamente con el DAO.
	 */
	private static void capturarFase4(ConfigurableApplicationContext context) throws IOException {

		Theme.cambiarA(Season.VERANO);

		HousingService housingService = context.getBean(HousingService.class);
		Housing paraDetalle = housingService.showHousings().stream()
				.filter(h -> h.getHousingCode().equals(10001L)).findFirst().orElseThrow(IllegalStateException::new);
		Housing paraReservar = housingService.showHousings().stream()
				.filter(h -> h.getHousingCode().equals(10002L)).findFirst().orElseThrow(IllegalStateException::new);

		// Detalle visto por un CUSTOMER: aparece "Reservar".
		User cliente = abrirSesion(context, "cliente", RoleType.CUSTOMER);
		HousingDetailsFrame detalle = context.getBean(HousingDetailsFrame.class);
		detalle.loadDetails(paraDetalle);
		guardar(detalle, "fase4-detalle-cliente");

		ReserveHousingFrame reservar = context.getBean(ReserveHousingFrame.class);
		reservar.setHousingId(paraReservar.getId());
		guardar(reservar, "fase4-reservar");

		List<Reservation> reservas = crearReservasDeEjemplo(context, cliente, housingService);

		ShowMyReservationsFrame misReservas = context.getBean(ShowMyReservationsFrame.class);
		guardar(misReservas, "fase4-mis-reservas");

		DoCheckInFrame checkIn = context.getBean(DoCheckInFrame.class);
		checkIn.setReservation(reservas.get(0));
		guardar(checkIn, "fase4-checkin");

		// Detalle visto por el ADMIN propietario: aparecen "Actualizar" e
		// "Intercambiar" en vez de "Reservar". Se inicia sesión como la propia
		// "Lucia" sembrada en data.sql —dueña real de paraDetalle (10001)— en lugar de
		// crear un usuario nuevo, que no sería propietario de nada. Esto solo funciona
		// desde la Fase 3d, que sustituyó la contraseña en texto plano de los usuarios
		// de ejemplo por un hash BCrypt real.
		iniciarSesionComo(context, "Lucia");
		HousingDetailsFrame detalleAdmin = context.getBean(HousingDetailsFrame.class);
		detalleAdmin.loadDetails(paraDetalle);
		guardar(detalleAdmin, "fase4-detalle-propietario");
	}

	/**
	 * Captura las cuatro pantallas de la Fase 5: listado de reseñas, detalle,
	 * publicar y editar.
	 *
	 * <p>
	 * Aquí <b>no</b> hace falta fabricar datos de attrezzo, a diferencia de la Fase
	 * 4: {@code data.sql} ya siembra dos reseñas por alojamiento con texto real,
	 * autoría y sub-notas variadas. Sembrar más sería duplicar en la herramienta
	 * algo que ya está en el arranque de la aplicación.
	 *
	 * <p>
	 * La sesión se abre como <b>Customer16</b>, un usuario de {@code data.sql} que
	 * es autor de una de las reseñas del alojamiento 10002. Así el detalle sale con
	 * el botón "Actualizar reseña" visible, que es el caso interesante: con un
	 * usuario cualquiera esa acción no aparece y la captura no enseñaría nada de
	 * ella.
	 */
	private static void capturarFase5(ConfigurableApplicationContext context) throws IOException {

		Theme.cambiarA(Season.VERANO);

		HousingService housingService = context.getBean(HousingService.class);
		ReviewService reviewService = context.getBean(ReviewService.class);

		Housing alojamiento = housingService.showHousings().stream().filter(h -> h.getHousingCode().equals(10002L))
				.findFirst().orElseThrow(IllegalStateException::new);

		iniciarSesionComo(context, "Customer16");

		ShowReviewsFrame listado = context.getBean(ShowReviewsFrame.class);
		listado.setHousingId(alojamiento.getId());
		guardar(listado, "fase5-resenas");

		List<Review> resenas;

		try {
			resenas = reviewService.showHousingReviews(alojamiento.getId());
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudieron leer las reseñas de ejemplo", ex);
		}

		Review propia = resenas.stream().filter(r -> "Customer16".equals(r.getAuthor().getUsername())).findFirst()
				.orElseThrow(IllegalStateException::new);

		ReviewDetailsFrame detalle = context.getBean(ReviewDetailsFrame.class);
		detalle.loadDetails(propia);
		guardar(detalle, "fase5-detalle-resena");

		UpdateReviewFrame editar = context.getBean(UpdateReviewFrame.class);
		editar.setReviewId(propia.getId());
		guardar(editar, "fase5-editar-resena");

		// Publicar exige no haber opinado ya sobre ese alojamiento, así que se usa
		// otro: Customer16 no tiene reseña del 10004.
		Housing sinResenaSuya = housingService.showHousings().stream().filter(h -> h.getHousingCode().equals(10004L))
				.findFirst().orElseThrow(IllegalStateException::new);

		PublishReviewFrame publicar = context.getBean(PublishReviewFrame.class);
		publicar.setHousingId(sinResenaSuya.getId());
		guardar(publicar, "fase5-publicar-resena");
	}

	/** Tres reservas del mismo cliente, una en cada estado visual. */
	private static List<Reservation> crearReservasDeEjemplo(ConfigurableApplicationContext context, User cliente,
			HousingService housingService) {

		ReservationDao reservationDao = context.getBean(ReservationDao.class);
		List<Housing> housings = housingService.showHousings();

		List<Reservation> creadas = new ArrayList<>();

		creadas.add(reservationDao.save(reservaDeEjemplo(1001L, LocalDateTime.now().plusDays(5),
				LocalDateTime.now().plusDays(8), false, cliente, housings.get(0))));

		creadas.add(reservationDao.save(reservaDeEjemplo(1002L, LocalDateTime.now().minusDays(1),
				LocalDateTime.now().plusDays(3), true, cliente, housings.get(1))));

		creadas.add(reservationDao.save(reservaDeEjemplo(1003L, LocalDateTime.now().minusDays(20),
				LocalDateTime.now().minusDays(15), true, cliente, housings.get(3))));

		return creadas;
	}

	private static Reservation reservaDeEjemplo(Long codigo, LocalDateTime checkIn, LocalDateTime checkOut,
			boolean checkedIn, User cliente, Housing housing) {

		return new Reservation(codigo, checkIn, checkOut, "Tarjeta de crédito", LocalDateTime.now().minusDays(30),
				housing.getPricePerNight().multiply(BigDecimal.valueOf(3)), checkedIn, cliente, housing);
	}

	private static void guardar(JFrame ventana, String nombre) throws IOException {

		File salida = new File(DESTINO, nombre + ".png");
		ImageIO.write(dibujar(ventana), "png", salida);

		System.out.println("Captura generada: " + salida.getPath());
	}

	/** Localiza el conmutador de vista recorriendo el árbol de componentes. */
	private static Segmented buscarConmutador(Component raiz) {

		if (raiz instanceof Segmented) {
			return (Segmented) raiz;
		}

		if (raiz instanceof Container) {

			for (Component hijo : ((Container) raiz).getComponents()) {

				Segmented encontrado = buscarConmutador(hijo);

				if (encontrado != null) {
					return encontrado;
				}
			}
		}

		return null;
	}

	/**
	 * Se asegura de que estamos sobre la base desechable antes de escribir nada.
	 *
	 * <p>
	 * Esta comprobación existe porque el fallo ya ocurrió: una configuración con la
	 * precedencia equivocada hizo que la herramienta se conectara a
	 * {@code ~/.actihome} y dejara allí su usuario de prueba. Un error de
	 * configuración no avisa —todo funciona, solo que contra la base que no era—, así
	 * que la única defensa es preguntar en voz alta a dónde nos hemos conectado.
	 */
	private static void comprobarQueLaBaseEsDesechable(ConfigurableApplicationContext context) {

		String url = context.getEnvironment().getProperty("spring.datasource.url", "");

		if (!url.startsWith("jdbc:h2:mem:")) {
			throw new IllegalStateException(
					"Esta herramienta escribe datos de prueba y solo debe usar una base en memoria. Conectada a: " + url);
		}
	}

	/**
	 * Registra un usuario de prueba y lo deja como sesión activa.
	 *
	 * <p>
	 * Se pasa por {@code signUp} en lugar de insertar la fila a mano para que la
	 * contraseña quede cifrada con BCrypt igual que en la aplicación real. La base es
	 * en memoria, así que este usuario desaparece al terminar.
	 *
	 * @return el usuario creado, para poder usarlo al fabricar datos de attrezzo
	 *         (reservas, por ejemplo) sin volver a consultar la base
	 */
	private static User abrirSesion(ConfigurableApplicationContext context, String username, RoleType rol) {

		UserService userService = context.getBean(UserService.class);
		SessionManager sessionManager = context.getBean(SessionManager.class);

		User usuario = new User(username, "1234", "Alejandro", "Sampedro", "Coruña", 666777892,
				username + "@actihome.example", LocalDateTime.now().minusYears(32), rol);

		try {
			userService.signUp(usuario);
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo crear el usuario de captura", ex);
		}

		sessionManager.login(usuario);

		return usuario;
	}

	/** Inicia sesión como un usuario ya sembrado en {@code data.sql}, con su contraseña real. */
	private static void iniciarSesionComo(ConfigurableApplicationContext context, String username) {

		UserService userService = context.getBean(UserService.class);
		SessionManager sessionManager = context.getBean(SessionManager.class);

		try {
			sessionManager.login(userService.login(username, "1234"));
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo iniciar sesión como " + username, ex);
		}
	}

	/**
	 * Pinta una ventana sobre una imagen, al tamaño de captura, sin mostrarla.
	 *
	 * <p>
	 * {@code setVisible(true)} sí se llama —es donde cada pantalla recarga sus
	 * datos— pero sobre una ventana colocada muy fuera de la pantalla, para que no
	 * aparezca delante de lo que estés haciendo. Es el único modo de que la captura
	 * refleje el estado real: una pantalla que nunca se ha mostrado tiene la lista
	 * vacía.
	 */
	private static BufferedImage dibujar(JFrame ventana) {

		ventana.setSize(new Dimension(ANCHO, ALTO));
		ventana.setLocation(-20000, -20000);
		ventana.setVisible(true);

		disponer(ventana.getContentPane());

		// La imagen se hace del tamaño del panel de contenido, no del de la ventana. La
		// diferencia es la barra de título y los bordes que pone Windows: si se usara el
		// tamaño de la ventana quedarían unas franjas negras abajo y a la derecha, y una
		// franja negra en una captura se interpreta como un fallo de maquetación que no
		// existe. Ya nos costó un diagnóstico equivocado una herramienta que mentía.
		int ancho = ventana.getContentPane().getWidth();
		int alto = ventana.getContentPane().getHeight();

		BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = imagen.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// printAll pinta el componente y toda su descendencia de forma síncrona, que es
		// justo lo que hace falta al dibujar fuera de la pantalla; paint() no garantiza
		// que los hijos estén ya dibujados cuando vuelve.
		ventana.getContentPane().printAll(g2);

		g2.dispose();

		return imagen;
	}

	/** Recorre el árbol colocando cada componente. Ver {@code ThemeSnapshots}. */
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
}
