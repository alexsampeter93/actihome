package fp.project.actihome.ui.dev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import fp.project.actihome.ActihomeApplication;
import fp.project.actihome.model.exceptions.EmailFailedException;
import fp.project.actihome.model.services.EmailSender;

/**
 * Comprueba la configuración de correo y, si se le da una dirección, envía un
 * mensaje de verdad.
 *
 * <pre>
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.ProbarCorreo"
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.ProbarCorreo" "-Dexec.args=tu@correo.com"
 * </pre>
 *
 * <p>
 * <b>Por qué existe.</b> Hasta ahora el envío por SMTP era <b>el único camino de
 * red del proyecto que nunca se había ejecutado contra un servidor real</b>. La
 * traducción sí tenía su {@link ProbarTraduccion}, y no por capricho: se escribió
 * después de descubrir que la barra vertical de {@code langpair=es|en} es un
 * carácter ilegal en una URI, algo que {@code curl} acepta sin rechistar y
 * {@code java.net.URI} rechaza. La lección de aquello —<b>verificar el servicio
 * no verifica tu cliente</b>— vale igual para SMTP, y aquí no se había aplicado.
 *
 * <p>
 * <b>Por qué arranca Spring, en vez de construir el cliente a mano.</b> Eso es lo
 * que hace {@code ProbarTraduccion}, y aquí no serviría, porque lo que más
 * probabilidades tiene de fallar no es el código: <b>es la cadena de tres
 * eslabones que lleva un valor desde el entorno hasta el campo</b>.
 *
 * <pre>
 * ACTIHOME_MAIL_HOST  →  application.yaml: actihome.mail.host  →  &#64;Value("${actihome.mail.host:}")
 * </pre>
 *
 * <p>
 * Una errata en cualquiera de los tres deja el valor vacío, {@code
 * estaConfigurado()} devuelve {@code false} y la aplicación <b>se va por el
 * camino alternativo sin decir nada</b> — que es el comportamiento correcto ante
 * una instalación sin correo, e indistinguible de un error de configuración.
 * Construir el cliente a mano se saltaría justo el trozo que hay que comprobar.
 *
 * <p>
 * <b>Qué significa cada salida.</b> No estar configurado <b>no es un fallo</b>:
 * es el estado normal del ejecutable repartido, donde a propósito no viaja
 * ninguna credencial. Por eso solo devuelve código distinto de cero cuando está
 * configurado y aun así el envío falla, que es el único caso en el que algo está
 * de verdad roto.
 */
public final class ProbarCorreo {

	/** Las cuatro que tienen que estar, con la variable de entorno que las llena. */
	private static final String[][] REQUERIDAS = {
			{ "actihome.mail.host", "ACTIHOME_MAIL_HOST" },
			{ "actihome.mail.user", "ACTIHOME_MAIL_USER" },
			{ "actihome.mail.password", "ACTIHOME_MAIL_PASSWORD" },
			{ "actihome.mail.from", "ACTIHOME_MAIL_FROM" } };

	private ProbarCorreo() {
	}

	public static void main(String[] args) {

		String destinatario = args.length > 0 ? args[0].trim() : null;

		SpringApplication app = new SpringApplication(ActihomeApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);

		// Base en memoria y como argumento de línea de comandos, no con
		// setDefaultProperties: los "default properties" están por DEBAJO de
		// application.yaml en la precedencia de Spring Boot, así que el yaml ganaría y
		// esta herramienta acabaría tocando la base de datos real. Es exactamente el
		// fallo que ya se cometió una vez en ScreenSnapshots.
		try (ConfigurableApplicationContext contexto = app.run(
				"--spring.datasource.url=jdbc:h2:mem:probarcorreo;DB_CLOSE_DELAY=-1;MODE=MySQL",
				"--spring.datasource.username=sa", "--spring.main.headless=true")) {

			System.exit(comprobar(contexto, destinatario));
		}
	}

	private static int comprobar(ConfigurableApplicationContext contexto, String destinatario) {

		Environment entorno = contexto.getEnvironment();
		EmailSender correo = contexto.getBean(EmailSender.class);

		System.out.println();
		System.out.println("Configuracion de correo");
		System.out.println("-----------------------");

		boolean completa = true;

		for (String[] propiedad : REQUERIDAS) {

			String valor = entorno.getProperty(propiedad[0], "");
			boolean puesta = !valor.trim().isEmpty();

			completa &= puesta;

			// La contraseña NUNCA se imprime, ni siquiera parcialmente. Esta salida acaba
			// pegada en un chat o en una incidencia con más facilidad de la que parece, y
			// una credencial filtrada por una herramienta de diagnóstico sería una ironía
			// cara. Basta con saber si está.
			System.out.printf("  %-24s %s%n", propiedad[1],
					puesta ? (esSecreta(propiedad[0]) ? "(puesta)" : valor) : "-- VACIA --");
		}

		System.out.println();
		System.out.println("  puerto                   " + entorno.getProperty("actihome.mail.port", "587"));
		System.out.println("  estaConfigurado()        " + correo.estaConfigurado());
		System.out.println();

		if (!completa) {
			return sinConfigurar();
		}

		if (destinatario == null) {
			System.out.println("Configuracion completa. Para enviar un correo de prueba de verdad:");
			System.out.println();
			System.out.println("  ...ProbarCorreo\" \"-Dexec.args=tu@correo.com");
			System.out.println();
			return 0;
		}

		return enviar(correo, destinatario);
	}

	private static int sinConfigurar() {

		System.out.println("SIN CORREO. Esto NO es un fallo: es el estado normal de una instalacion");
		System.out.println("repartida, donde a proposito no viaja ninguna credencial dentro del .exe.");
		System.out.println("La recuperacion de contrasena usa el codigo que genera un administrador");
		System.out.println("desde Ajustes, y la pantalla lo detecta sola y lo explica.");
		System.out.println();
		System.out.println("Para activar el envio, en la sesion de PowerShell desde la que arranques:");
		System.out.println();
		System.out.println("  $env:ACTIHOME_MAIL_HOST     = \"smtp-relay.brevo.com\"");
		System.out.println("  $env:ACTIHOME_MAIL_USER     = \"el que da Brevo\"");
		System.out.println("  $env:ACTIHOME_MAIL_PASSWORD = \"la clave SMTP de Brevo\"");
		System.out.println("  $env:ACTIHOME_MAIL_FROM     = \"la direccion remitente verificada\"");
		System.out.println();

		return 0;
	}

	private static int enviar(EmailSender correo, String destinatario) {

		System.out.println("Enviando a " + destinatario + " ...");

		try {
			correo.enviar(destinatario, "ActiHome - correo de prueba",
					"Si estas leyendo esto, el envio por SMTP funciona.\n\n"
							+ "Lo ha mandado ui/dev/ProbarCorreo, no la aplicacion.");

			System.out.println();
			System.out.println("ENVIADO. Comprueba la bandeja de entrada, y tambien el correo no deseado:");
			System.out.println("un remitente nuevo suele caer ahi las primeras veces.");
			System.out.println();
			return 0;

		} catch (EmailFailedException ex) {

			System.out.println();
			System.out.println("FALLO EL ENVIO, y la configuracion estaba completa, asi que algo esta roto.");
			System.out.println("Lo mas habitual, por orden:");
			System.out.println("  - la direccion remitente no esta verificada en Brevo");
			System.out.println("  - la clave SMTP no es la de SMTP (Brevo tiene tambien clave de API, y no valen)");
			System.out.println("  - el puerto 587 esta cerrado en la red desde la que sales");
			System.out.println();
			return 1;
		}
	}

	private static boolean esSecreta(String propiedad) {
		return propiedad.endsWith("password");
	}
}
