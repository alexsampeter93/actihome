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
import java.util.Locale;

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
import fp.project.actihome.model.services.MessageService;
import fp.project.actihome.model.services.ReviewService;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.ConversationFrame;
import fp.project.actihome.ui.MessagesFrame;
import fp.project.actihome.ui.DoCheckInFrame;
import fp.project.actihome.ui.HousingDetailsFrame;
import fp.project.actihome.ui.PublishReviewFrame;
import fp.project.actihome.ui.ReserveHousingFrame;
import fp.project.actihome.ui.ReviewDetailsFrame;
import fp.project.actihome.ui.ShowHousingsFrame;
import fp.project.actihome.ui.ShowMyReservationsFrame;
import fp.project.actihome.ui.SettingsFrame;
import fp.project.actihome.ui.ShowReviewsFrame;
import fp.project.actihome.ui.TradeHousingsFrame;
import fp.project.actihome.ui.ChangePasswordFrame;
import fp.project.actihome.ui.UpdateHousingFrame;
import fp.project.actihome.ui.UploadHousingFrame;
import fp.project.actihome.ui.UpdateReviewFrame;
import fp.project.actihome.ui.components.Segmented;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.ActiHomeTheme;
import fp.project.actihome.ui.theme.Season;
import fp.project.actihome.ui.theme.Textos;
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

	/**
	 * Tamaño de ventana con el que se capturan las pantallas.
	 *
	 * <p>
	 * <b>Se puede cambiar desde la línea de comandos</b>, y conviene usarlo:
	 * capturar siempre a 1400×900 es lo que hizo que los recortes del portátil del
	 * usuario no aparecieran en ninguna captura durante meses. Un portátil de
	 * 1920×1080 con el escalado de Windows al 150 % le entrega a la aplicación
	 * 1280×660 puntos, y esa es la medida en la que hay que mirar el resultado.
	 *
	 * <pre>
	 * "-Dexec.args=fase7-compacto ADMIN 1280x660"
	 * </pre>
	 */
	private static int ancho = 1400;
	private static int alto = 900;

	private ScreenSnapshots() {
	}

	public static void main(String[] args) throws IOException {

		System.setProperty("java.awt.headless", "false");
		ActiHomeTheme.install();

		// El idioma se fija a mano, y va DESPUÉS de install() a propósito: install()
		// llama a Preferencias.restaurar(), que aplica lo que el dueño de la máquina
		// tuviera guardado en ~/.actihome. Sin esta línea, las capturas de la
		// documentación salían en español o en inglés según quién las generase. Es la
		// misma clase de fallo que la base de datos: una herramienta de documentación
		// no puede heredar el estado de quien la ejecuta.
		Textos.cambiarA(new Locale("es"));

		String prefijo = args.length > 0 ? args[0] : "catalogo";
		RoleType rol = args.length > 1 ? RoleType.valueOf(args[1]) : RoleType.ADMIN;

		if (args.length > 2 && args[2].contains("x")) {

			String[] medidas = args[2].split("x");
			ancho = Integer.parseInt(medidas[0].trim());
			alto = Integer.parseInt(medidas[1].trim());
		}

		SpringApplication app = new SpringApplication(ActihomeApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);

		// La URL va como argumento de línea de comandos y NO con setDefaultProperties.
		// Es una diferencia que parece de matiz y no lo es: los "default properties" son
		// lo ÚLTIMO en el orden de precedencia de Spring Boot, por debajo de
		// application.yaml, así que el yaml ganaba y esta herramienta acababa
		// escribiendo su usuario de prueba en la base de datos real del usuario. Los
		// argumentos de línea de comandos están arriba del todo y sí mandan.
		// Y la previsión meteorológica va con el cliente de ejemplo, para que una
		// captura salga siempre igual: la de verdad llega por red y aparecería o no
		// según lo rápida que fuese. El de ejemplo devuelve además los cinco estados
		// del cielo, así que una sola captura enseña los cinco iconos.
		try (ConfigurableApplicationContext context = app.run("--spring.datasource.url=" + BASE_EN_MEMORIA,
				"--spring.datasource.username=sa", "--actihome.meteorologia.habilitada=false", "--actihome.mapa.habilitado=false")) {

			comprobarQueLaBaseEsDesechable(context);

			if ("fase4".equals(prefijo)) {
				capturarFase4(context);

			} else if ("fase5".equals(prefijo)) {
				capturarFase5(context);

			} else if ("fase6".equals(prefijo)) {
				capturarFase6(context);

			} else if ("login".equals(prefijo)) {

				Theme.cambiarA(Season.INVIERNO);
				guardar(context.getBean(fp.project.actihome.ui.LoginFrame.class), "login");

			} else if ("ajustes".equals(prefijo)) {
				capturarAjustes(context);

			} else if ("mensajes".equals(prefijo)) {
				capturarMensajes(context);

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

	/**
	 * Captura las cinco pantallas de la Fase 6: alta y edición de alojamiento,
	 * intercambio, perfil y contraseña.
	 *
	 * <p>
	 * La sesión se abre como <b>Lucia</b>, propietaria real de los alojamientos de
	 * ejemplo desde la Fase 3d. Hace falta que sea la titular: editar e
	 * intercambiar son operaciones de propietario, y con un usuario cualquiera las
	 * pantallas saldrían con el aviso de "no eres el titular" en vez de con su
	 * contenido.
	 *
	 * <p>
	 * El intercambio se captura <b>con el candidato ya buscado</b>, no con el panel
	 * derecho vacío: el paso de buscar es justamente lo que esta pantalla añade
	 * frente a la anterior, y una captura del estado inicial no lo enseñaría.
	 */
	/**
	 * Ajustes como administrador, con un código de recuperación ya generado.
	 *
	 * <p>
	 * Es el estado que no se puede revisar de otra forma: el código solo aparece
	 * tras pulsar el botón, y {@code MedirResponsive} no sirve para juzgarlo —
	 * comprueba que nada caiga fuera del área visible, no que un dato se lea bien.
	 * Es justo la distinción que dejó veintinueve altos escritos a mano sin
	 * detectar hasta la Fase 8.1.
	 */
	private static void capturarAjustes(ConfigurableApplicationContext context) throws IOException {

		Theme.cambiarA(Season.OTONO);

		iniciarSesionComo(context, "Admin");

		SettingsFrame ajustes = context.getBean(SettingsFrame.class);

		guardar(ajustes, "ajustes-admin");
		guardar(ajustes, "ajustes-codigo", () -> ajustes.generarCodigoPara("Lucia"));
	}

	/**
	 * La bandeja de mensajes y una conversación con hilo.
	 *
	 * <p>
	 * <b>Eran las dos únicas pantallas sin forma de capturarlas</b>, y se añadió
	 * justo al corregir un fallo visual del compositor que el usuario encontró
	 * usando la aplicación: el botón "Enviar" quedaba alineado con la etiqueta del
	 * campo en vez de con el recuadro. Un fallo que solo se ve mirando, en una
	 * pantalla que no se podía mirar sin abrir la aplicación y escribirse a uno
	 * mismo, es un fallo con muchas papeletas de quedarse.
	 *
	 * <p>
	 * Los mensajes se siembran por el servicio y no con el DAO, a diferencia de las
	 * reservas de la Fase 4: aquí no hay ninguna regla de negocio que impida crear
	 * el caso que interesa, así que no hay motivo para saltarse la puerta.
	 */
	private static void capturarMensajes(ConfigurableApplicationContext context) throws IOException {

		Theme.cambiarA(Season.INVIERNO);

		HousingService housingService = context.getBean(HousingService.class);
		MessageService mensajeria = context.getBean(MessageService.class);
		UserService userService = context.getBean(UserService.class);

		iniciarSesionComo(context, "Lucia");

		Housing propio = housingService.showHousings().stream().filter(h -> h.getHousingCode().equals(10001L))
				.findFirst().orElseThrow(IllegalStateException::new);

		User lucia = context.getBean(SessionManager.class).getLoggedInUser();
		User cliente;

		try {
			cliente = userService.login("Customer16", "1234");
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo resolver el interlocutor", ex);
		}

		try {
			mensajeria.sendMessage(cliente.getId(), lucia.getId(), propio.getId(),
					"Hola, ¿el alojamiento admite mascotas pequeñas?");
			mensajeria.sendMessage(lucia.getId(), cliente.getId(), propio.getId(),
					"¡Hola! Sí, admitimos mascotas pequeñas sin problema.");
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo sembrar la conversación", ex);
		}

		guardar(context.getBean(MessagesFrame.class), "mensajes-bandeja");

		ConversationFrame conversacion = context.getBean(ConversationFrame.class);
		conversacion.setConversacion(cliente, propio);
		guardar(conversacion, "mensajes-conversacion");
	}

	private static void capturarFase6(ConfigurableApplicationContext context) throws IOException {

		Theme.cambiarA(Season.VERANO);

		HousingService housingService = context.getBean(HousingService.class);

		iniciarSesionComo(context, "Lucia");

		Housing propio = housingService.showHousings().stream().filter(h -> h.getHousingCode().equals(10001L))
				.findFirst().orElseThrow(IllegalStateException::new);

		UploadHousingFrame alta = context.getBean(UploadHousingFrame.class);
		guardar(alta, "fase6-alta-alojamiento");

		UpdateHousingFrame edicion = context.getBean(UpdateHousingFrame.class);
		edicion.setHousingId(propio.getId());
		guardar(edicion, "fase6-editar-alojamiento");

		TradeHousingsFrame intercambio = context.getBean(TradeHousingsFrame.class);
		intercambio.setHousingId(propio.getId());
		guardar(intercambio, "fase6-intercambio-vacio");
		// La búsqueda va DESPUÉS de mostrar: al mostrarse, la pantalla se recarga y
		// limpia el código, así que hacerla antes no dejaría rastro en la captura.
		guardar(intercambio, "fase6-intercambio", () -> intercambio.buscarPorCodigo("10004"));

		// Intercambio sin alojamientos propios: se abre con un ADMIN recien creado, que
		// no es duenno de nada, para ver el estado vacio.
		abrirSesion(context, "nuevoadmin", RoleType.ADMIN);
		TradeHousingsFrame sinNada = context.getBean(TradeHousingsFrame.class);
		sinNada.setHousingId(null);
		guardar(sinNada, "fase6-intercambio-sin-alojamientos");

		// El perfil ya no es una pantalla propia: la Fase 8.4 lo fusionó con Ajustes.
		SettingsFrame perfilYAjustes = context.getBean(SettingsFrame.class);
		guardar(perfilYAjustes, "fase6-perfil");

		ChangePasswordFrame contrasena = context.getBean(ChangePasswordFrame.class);
		guardar(contrasena, "fase6-contrasena");
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
		guardar(ventana, nombre, null);
	}

	/**
	 * Guarda una captura, con la posibilidad de tocar la pantalla <b>después</b> de
	 * mostrarla y antes de pintarla.
	 *
	 * <p>
	 * Hace falta porque los frames recargan sus datos dentro de {@code setVisible},
	 * y {@link #dibujar} tiene que llamar a {@code setVisible} para que exista el
	 * componente nativo. Cualquier estado que se prepare <em>antes</em> —una
	 * búsqueda hecha, un filtro aplicado— se pierde en esa recarga. Este parámetro
	 * es el hueco para volver a ponerlo cuando ya no hay nada que lo borre.
	 */
	private static void guardar(JFrame ventana, String nombre, Runnable despuesDeMostrar) throws IOException {

		File salida = new File(DESTINO, nombre + ".png");
		ImageIO.write(dibujar(ventana, despuesDeMostrar), "png", salida);

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
	private static BufferedImage dibujar(JFrame ventana, Runnable despuesDeMostrar) {

		ventana.setSize(new Dimension(ancho, alto));
		ventana.setLocation(-20000, -20000);
		ventana.setVisible(true);

		if (despuesDeMostrar != null) {
			despuesDeMostrar.run();
		}

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
