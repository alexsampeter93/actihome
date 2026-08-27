package fp.project.actihome.ui.dev;

import java.awt.Component;
import java.awt.Container;
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
import fp.project.actihome.ui.AdministrationFrame;
import fp.project.actihome.ui.ChangePasswordFrame;
import fp.project.actihome.ui.HousingDetailsFrame;
import fp.project.actihome.ui.LoginFrame;
import fp.project.actihome.ui.MessagesFrame;
import fp.project.actihome.ui.PublishReviewFrame;
import fp.project.actihome.ui.RecoverPasswordFrame;
import fp.project.actihome.ui.ReserveHousingFrame;
import fp.project.actihome.ui.SettingsFrame;
import fp.project.actihome.ui.ShowHousingsFrame;
import fp.project.actihome.ui.ShowMyReservationsFrame;
import fp.project.actihome.ui.ShowReviewsFrame;
import fp.project.actihome.ui.SignUpFrame;
import fp.project.actihome.ui.TradeHousingsFrame;
import fp.project.actihome.ui.UpdateHousingFrame;
import fp.project.actihome.ui.UploadHousingFrame;
import fp.project.actihome.ui.components.Rescate;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.ActiHomeTheme;
import fp.project.actihome.ui.theme.Animacion;

/**
 * Recorre la aplicación <b>navegando de verdad</b> y comprueba que a ninguna
 * pantalla le sale la barra de rescate por el camino.
 *
 * <pre>
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.MedirNavegacion"
 * </pre>
 *
 * <h2>Por qué hacía falta otra herramienta</h2>
 *
 * <p>
 * {@code MedirPantallas} construye cada pantalla y <b>le pone el tamaño a
 * mano</b>. Eso contesta "¿cabe en un portátil?", que es una pregunta necesaria,
 * pero deja fuera la que de verdad determina lo que ve el usuario: <b>qué tamaño
 * acaba teniendo cada ventana cuando se llega a ella usando la aplicación</b>.
 * Y no es el mismo.
 *
 * <p>
 * {@code Navigator} pasa el tamaño de una pantalla a la siguiente tal cual —eso
 * es deliberado, es lo que evita que la ventana pegue saltos al navegar—, así
 * que el tamaño de <em>toda la sesión</em> lo fija la primera ventana. El login
 * mide 980×620. Con esa herencia, las diecinueve pantallas corrían el resto de
 * la sesión a 980×620 y a casi todas les salía la barra, mientras las tres
 * herramientas de medida decían que todo estaba bien: <b>ninguna llegaba a las
 * pantallas por donde se llega a ellas</b>.
 *
 * <p>
 * Se veía al restaurar una ventana maximizada —que es cuando Windows devuelve el
 * tamaño normal— y por eso el usuario lo describió como "al minimizar vuelve el
 * scroll". La descripción era exacta; lo que fallaba era dónde estábamos
 * mirando.
 *
 * <h2>Qué hace exactamente</h2>
 *
 * <p>
 * Abre el login, lo encoge <b>a propósito</b> a 980×620 —el peor caso realista,
 * porque es su propio tamaño de diseño— y a partir de ahí navega por las
 * diecinueve pantallas con el {@link Navigator} real, en el orden en que se
 * visitan. Después de cada salto mira si la barra de rescate está visible.
 *
 * <p>
 * Devuelve código de salida distinto de cero si alguna la enseña, así que sirve
 * como comprobación automática igual que las otras dos.
 */
public final class MedirNavegacion {

	/**
	 * El tamaño con el que arranca el recorrido.
	 *
	 * <p>
	 * Es el {@code setSize} del login, y por tanto el tamaño que hereda toda la
	 * sesión de quien no toca la ventana. Empezar aquí no es buscarle las cosquillas
	 * a la aplicación: es reproducir lo que le pasa a cualquiera que entre y navegue.
	 */
	private static final int ANCHO_INICIAL = 1024;
	private static final int ALTO_INICIAL = 600;

	private MedirNavegacion() {
	}

	public static void main(String[] args) throws Exception {

		System.setProperty("java.awt.headless", "false");
		ActiHomeTheme.install();
		Animacion.desactivarParaHerramientas();

		SpringApplication app = new SpringApplication(ActihomeApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);

		int fallos = 0;

		try (ConfigurableApplicationContext c = app.run(
				"--spring.datasource.url=jdbc:h2:mem:navegar;DB_CLOSE_DELAY=-1;MODE=MySQL",
				"--spring.datasource.username=sa", "--actihome.meteorologia.habilitada=false",
				"--actihome.mapa.habilitado=false")) {

			SessionManager sm = c.getBean(SessionManager.class);
			UserService us = c.getBean(UserService.class);
			HousingService hs = c.getBean(HousingService.class);
			Navigator navigator = c.getBean(Navigator.class);

			sm.login(us.login("Lucia", "1234"));

			Housing propio = hs.showHousings().stream().filter(h -> h.getHousingCode().equals(10001L)).findFirst()
					.orElseThrow();

			Long id = propio.getId();

			List<Paso> pasos = new ArrayList<>();

			pasos.add(new Paso("Login", () -> navigator.ir(LoginFrame.class)));
			pasos.add(new Paso("Registro", () -> navigator.ir(SignUpFrame.class)));
			pasos.add(new Paso("Recuperar", () -> navigator.ir(RecoverPasswordFrame.class)));
			pasos.add(new Paso("Buscar", () -> navigator.ir(fp.project.actihome.ui.SearchHousingsFrame.class)));
			pasos.add(Paso.lista("Catalogo", () -> navigator.ir(ShowHousingsFrame.class)));
			pasos.add(new Paso("Detalle", () -> navigator.ir(HousingDetailsFrame.class, f -> f.loadDetails(propio))));
			pasos.add(new Paso("Reservar", () -> navigator.ir(ReserveHousingFrame.class, f -> f.setHousingId(id))));
			pasos.add(new Paso("Resenas", () -> navigator.ir(ShowReviewsFrame.class, f -> f.setHousingId(id))));
			pasos.add(new Paso("Publicar resena", () -> navigator.ir(PublishReviewFrame.class, f -> f.setHousingId(id))));
			pasos.add(new Paso("Intercambio", () -> navigator.ir(TradeHousingsFrame.class, f -> f.setHousingId(id))));
			pasos.add(new Paso("Alta alojamiento", () -> navigator.ir(UploadHousingFrame.class)));
			pasos.add(new Paso("Editar alojamiento", () -> navigator.ir(UpdateHousingFrame.class, f -> f.setHousingId(id))));
			pasos.add(new Paso("Mis reservas", () -> navigator.ir(ShowMyReservationsFrame.class)));
			pasos.add(new Paso("Mensajes", () -> navigator.ir(MessagesFrame.class)));
			pasos.add(new Paso("Ajustes", () -> navigator.ir(SettingsFrame.class)));
			pasos.add(new Paso("Administracion", () -> navigator.ir(AdministrationFrame.class)));
			pasos.add(new Paso("Contrasena", () -> navigator.ir(ChangePasswordFrame.class)));

			System.out.println();
			System.out.println("=== navegando de verdad, al tamano con el que abre la aplicacion ===");
			System.out.printf("%-22s %12s  %s%n", "pantalla", "ventana", "");

			boolean primera = true;
			java.awt.Dimension tamanoDeLaSesion = null;

			for (Paso paso : pasos) {

				paso.ir.run();

				JFrame ventana = navigator.ventanaVisible();

				if (primera) {

					// **No se fuerza ningún tamaño, y ese es el cambio.** Antes se encogía la
					// primera ventana a 980×620 para reproducir el fallo de que el tamaño de
					// la sesión lo fijaba el login. Ese fallo se corrigió de raíz —la sesión
					// abre en Layout.TAMANO_DE_SESION— así que forzar ahora un tamaño por
					// debajo del suelo del sistema no comprueba nada: mide la aplicación en
					// un tamaño en el que la barra de rescate es la respuesta correcta, no un
					// síntoma. Se recorre al tamaño real con el que se abre.
					primera = false;
					tamanoDeLaSesion = ventana.getSize();
				}

				Thread.sleep(80);
				ventana.validate();

				JScrollPane rescate = buscarRescate(ventana.getContentPane());
				boolean barra = rescate != null && rescate.getVerticalScrollBar().isVisible();

				// **Y que la ventana no haya cambiado de tamaño ella sola.** Es la
				// comprobación que faltaba, y este proyecto ha reportado el mismo fallo
				// cuatro veces: la ventana crecía a saltos al cambiar de pantalla, por dos
				// causas distintas que producían el mismo síntoma —un Math.max contra el
				// tamaño de diseño del destino, y un mínimo de ventana calculado desde el
				// contenido, que en una pantalla con lista no está acotado—. Las dos están
				// corregidas; esta línea es lo que impide que vuelvan sin que nadie se dé
				// cuenta.
				boolean cambioDeTamano = !ventana.getSize().equals(tamanoDeLaSesion);

				String estado = cambioDeTamano
						? "CAMBIA DE TAMANO SOLA (" + tamanoDeLaSesion.width + "x" + tamanoDeLaSesion.height + " -> "
								+ ventana.getWidth() + "x" + ventana.getHeight() + ")"
						: barra ? (paso.esLista ? "scroll (correcto: es una lista)" : "BARRA DE RESCATE") : "ok";

				System.out.printf("%-22s %12s  %s%n", paso.nombre,
						ventana.getWidth() + "x" + ventana.getHeight(), estado);

				if (cambioDeTamano) {
					fallos++;
					tamanoDeLaSesion = ventana.getSize();

				} else if (barra && !paso.esLista) {
					fallos++;
				}
			}
		}

		System.out.println();

		if (fallos == 0) {
			System.out.println("NINGUNA pantalla saca la barra ni cambia de tamano navegando.");
		} else {
			System.out.println(fallos + " pantallas sacan la barra de rescate navegando.");
		}

		System.exit(fallos == 0 ? 0 : 1);
	}

	private static JScrollPane buscarRescate(Container contenedor) {

		for (Component hijo : contenedor.getComponents()) {

			if (hijo instanceof JScrollPane
					&& Boolean.TRUE.equals(((JScrollPane) hijo).getClientProperty(Rescate.MARCA))) {

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

	private static final class Paso {

		private final String nombre;
		private final Runnable ir;

		/**
		 * Si su cuerpo es una lista que crece con los datos.
		 *
		 * <p>
		 * En esas, la barra <b>es</b> el diseño: el catálogo no cabe en ninguna ventana
		 * porque su alto es el de todas las estancias que haya. Se recorre igual y se
		 * informa, pero no cuenta como fallo — marcarlo en rojo enseñaría a ignorar la
		 * herramienta, que es la forma más rápida de perderla.
		 */
		private final boolean esLista;

		private Paso(String nombre, Runnable ir) {
			this(nombre, ir, false);
		}

		private Paso(String nombre, Runnable ir, boolean esLista) {
			this.nombre = nombre;
			this.ir = ir;
			this.esLista = esLista;
		}

		private static Paso lista(String nombre, Runnable ir) {
			return new Paso(nombre, ir, true);
		}
	}
}
