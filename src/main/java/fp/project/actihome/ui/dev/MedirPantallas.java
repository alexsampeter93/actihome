package fp.project.actihome.ui.dev;

import java.awt.*;
import javax.swing.*;
import org.springframework.boot.*;
import org.springframework.context.ConfigurableApplicationContext;
import fp.project.actihome.ActihomeApplication;
import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.services.*;
import fp.project.actihome.ui.*;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.ActiHomeTheme;

/**
 * Mide cuánto espacio necesita cada pantalla y lo compara con el área utilizable
 * de esta pantalla.
 *
 * <pre>
 * .mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.MedirPantallas"
 * </pre>
 *
 * <p>
 * <b>Existe porque los botones cortados no se detectan mirando capturas.</b> El
 * alto que ocupa un formulario depende de cuánto miden las fuentes, y eso cambia
 * con el escalado del sistema: lo que en una captura a 1400x900 sin escalar entra
 * justo, en un Windows al 125 % deja el botón fuera. Aquí se le pregunta a cada
 * pantalla ya construida cuánto necesita <em>en esta máquina</em> y se compara con
 * el escritorio real.
 *
 * <p>
 * Que el catálogo aparezca como "no cabe" es correcto y no hay que arreglarlo: su
 * alto preferido es el de la lista entera, y para eso está la barra de
 * desplazamiento. Lo que importa son las pantallas de formulario, que no tienen
 * scroll de rescate.
 */
public class MedirPantallas {
	public static void main(String[] a) throws Exception {
		System.setProperty("java.awt.headless","false");
		ActiHomeTheme.install();
		SpringApplication app = new SpringApplication(ActihomeApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		try (ConfigurableApplicationContext c = app.run(
				"--spring.datasource.url=jdbc:h2:mem:medir;DB_CLOSE_DELAY=-1;MODE=MySQL",
				"--spring.datasource.username=sa",
				// Sin red: ver la nota de WeatherClientDeEjemplo.
				"--actihome.meteorologia.habilitada=false",
				"--actihome.mapa.habilitado=false")) {

			var sm = c.getBean(SessionManager.class);
			var us = c.getBean(UserService.class);
			var hs = c.getBean(HousingService.class);
			sm.login(us.login("Lucia", "1234"));
			Housing propio = hs.showHousings().stream()
				.filter(h -> h.getHousingCode().equals(10001L)).findFirst().orElseThrow();

			Rectangle util = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			System.out.println("AREA UTIL: " + util.width + "x" + util.height);
			System.out.printf("%-26s %11s %11s  %s%n", "pantalla", "diseno", "necesita", "cabe?");

			medir("Catalogo", c.getBean(ShowHousingsFrame.class), null, util);
			medir("Detalle", c.getBean(HousingDetailsFrame.class), f -> ((HousingDetailsFrame)f).loadDetails(propio), util);
			medir("Reservar", c.getBean(ReserveHousingFrame.class), f -> ((ReserveHousingFrame)f).setHousingId(propio.getId()), util);
			medir("Mis reservas", c.getBean(ShowMyReservationsFrame.class), null, util);
			medir("Check-in", c.getBean(DoCheckInFrame.class), null, util);
			medir("Resenas", c.getBean(ShowReviewsFrame.class), f -> ((ShowReviewsFrame)f).setHousingId(propio.getId()), util);
			medir("Detalle resena", c.getBean(ReviewDetailsFrame.class), null, util);
			medir("Publicar resena", c.getBean(PublishReviewFrame.class), f -> ((PublishReviewFrame)f).setHousingId(propio.getId()), util);
			medir("Editar resena", c.getBean(UpdateReviewFrame.class), null, util);
			medir("Alta alojamiento", c.getBean(UploadHousingFrame.class), null, util);
			medir("Editar alojamiento", c.getBean(UpdateHousingFrame.class), f -> ((UpdateHousingFrame)f).setHousingId(propio.getId()), util);
			medir("Intercambio", c.getBean(TradeHousingsFrame.class), f -> ((TradeHousingsFrame)f).setHousingId(propio.getId()), util);
			// "Perfil" se fusiono con Ajustes en la Fase 8.4.
			medir("Contrasena", c.getBean(ChangePasswordFrame.class), null, util);
			medir("Login", c.getBean(LoginFrame.class), null, util);
			medir("Registro", c.getBean(SignUpFrame.class), null, util);
		}
		System.exit(0);
	}

	interface Prep { void run(JFrame f); }

	static void medir(String nombre, JFrame f, Prep prep, Rectangle util) throws Exception {
		if (prep != null) prep.run(f);
		int dw = f.getWidth(), dh = f.getHeight();
		f.setLocation(-20000,-20000);
		f.setVisible(true);
		Thread.sleep(250);
		f.validate();
		Dimension pref = f.getContentPane().getPreferredSize();
		Insets bo = f.getInsets();
		int nw = pref.width + bo.left + bo.right, nh = pref.height + bo.top + bo.bottom;
		boolean cabe = nh <= util.height && nw <= util.width;
		System.out.printf("%-26s %5dx%-5d %5dx%-5d  %s%n", nombre, dw, dh, nw, nh,
			cabe ? "si" : "NO -> falta " + Math.max(0, nh-util.height) + "px de alto");
		f.setVisible(false);
	}
}
