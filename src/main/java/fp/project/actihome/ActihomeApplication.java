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

		SpringApplication app = new SpringApplication(ActihomeApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		// Se arranca con la instancia configurada (app.run) en lugar de con el método
		// estático SpringApplication.run, que creaba una SpringApplication nueva y
		// descartaba silenciosamente el setWebApplicationType de la línea anterior.
		ConfigurableApplicationContext context = app.run(args);

		EventQueue.invokeLater(() -> {
			LoginFrame loginFrame = context.getBean(LoginFrame.class);
			loginFrame.setVisible(true);
		});
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