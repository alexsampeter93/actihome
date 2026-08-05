package fp.project.actihome;

import java.awt.EventQueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import fp.project.actihome.ui.LoginFrame;
import fp.project.actihome.ui.brand.SplashScreen;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.reminders.TrayReminders;
import fp.project.actihome.ui.theme.ActiHomeTheme;

@SpringBootApplication
public class ActihomeApplication {

	public static void main(String[] args) {

		System.setProperty("java.awt.headless", "false");

		// El Look and Feel se instala antes de arrancar Spring: varias ventanas son
		// beans no-@Lazy y se construyen durante el refresco del contexto, así que si
		// el L&F se instalara después ya se habrían dibujado con el aspecto por
		// defecto de Java.
		ActiHomeTheme.install();

		// El splash se muestra antes de arrancar Spring, que es lo que tarda unos tres
		// segundos: así aparece de inmediato y esos segundos dejan de ser un vacío en
		// el que no ocurre nada.
		SplashScreen.mostrar();

		SpringApplication app = new SpringApplication(ActihomeApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		// Se arranca con la instancia configurada (app.run) en lugar de con el método
		// estático SpringApplication.run, que creaba una SpringApplication nueva y
		// descartaba silenciosamente el setWebApplicationType de la línea anterior.
		ConfigurableApplicationContext context = app.run(args);

		// La primera ventana también se abre por el navegador: así queda registrada
		// como ventana visible y recibe el mismo tratamiento que las demás (entre otras
		// cosas, el icono y que pulsar la X cierre la aplicación de verdad).
		//
		// Los recordatorios de bandeja (F11) arrancan aquí mismo, una sola vez en toda
		// la sesión: no pertenecen a ninguna pantalla, así que no tienen un
		// setVisible(true) natural del que colgarse.
		EventQueue.invokeLater(() -> {
			context.getBean(Navigator.class).ir(LoginFrame.class);
			context.getBean(TrayReminders.class).iniciar();
		});

		// El splash se retira cuando el login ya está pedido. Espera por su cuenta a
		// haber estado un mínimo en pantalla, para que en un arranque rápido no
		// parpadee.
		SplashScreen.cerrar();
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// Aquí vivían dos beans más, retirados en la auditoría de la Fase 8.3:
	// `messageSource()` y `validator()`.
	//
	// Eran infraestructura muerta encadenada. `messageSource()` apuntaba a
	// `classpath:messages`, un fichero que NO EXISTE en el proyecto y que nunca ha
	// existido; su único consumidor era `validator()`, que a su vez montaba un
	// `LocalValidatorFactoryBean` para la validación por anotaciones de Bean
	// Validation. Y la aplicación no tiene **ni una sola** anotación de validación:
	// ni un `@NotNull`, ni un `@Size`, ni un `@Valid` en 145 ficheros. Toda la
	// validación es código explícito en los servicios y en las pantallas.
	//
	// Merece anotarse por qué duró tanto: cada pieza justificaba a la siguiente
	// —el validador necesita un origen de mensajes, el origen de mensajes tiene un
	// consumidor— y leyéndolas por separado las dos parecían tener sentido. Lo que
	// no lo tenía era el conjunto, porque **nada de fuera llamaba a ninguna de las
	// dos**. Es la forma más difícil de detectar del código muerto: no un método
	// suelto sin usar, sino un pequeño sistema coherente conectado a nada.
	//
	// Con ellos se retiró también `spring-boot-starter-validation` del `pom.xml`,
	// que era quien los traía, y con esa dependencia se fue `tomcat-embed-el`: una
	// implementación de Expression Language para contenedores de servlets, dentro
	// de una aplicación de escritorio que no levanta ningún servidor web.

}