package fp.project.actihome;

import java.awt.EventQueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import fp.project.actihome.ui.LoginFrame;
import fp.project.actihome.ui.brand.SplashScreen;
import fp.project.actihome.ui.nav.Navigator;
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
		EventQueue.invokeLater(() -> context.getBean(Navigator.class).ir(LoginFrame.class));

		// El splash se retira cuando el login ya está pedido. Espera por su cuenta a
		// haber estado un mínimo en pantalla, para que en un arranque rápido no
		// parpadee.
		SplashScreen.cerrar();
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public MessageSource messageSource() {

		ReloadableResourceBundleMessageSource bean = new ReloadableResourceBundleMessageSource();

		bean.setBasename("classpath:messages");
		bean.setDefaultEncoding("UTF-8");

		return bean;
	}

	@Bean
	public LocalValidatorFactoryBean validator() {

		LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();

		bean.setValidationMessageSource(messageSource());

		return bean;
	}

}