package fp.project.actihome.model.services;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.exceptions.EmailFailedException;

/**
 * Envío de correo por SMTP, configurado para <b>Brevo</b> (Fase 8.6).
 *
 * <p>
 * <b>Por qué Brevo.</b> 300 correos al día gratis, sin tarjeta y sin período de
 * prueba que caduque. Para una recuperación de contraseña de una aplicación
 * personal sobra de largo, y no obliga a montar nada.
 *
 * <p>
 * <b>Las credenciales vienen de variables de entorno y nunca del código.</b> Es
 * el mismo patrón que el proyecto ya usa para la base de datos MySQL desde la
 * Fase 1.5, y aquí importa todavía más:
 *
 * <pre>
 * ACTIHOME_MAIL_HOST      smtp-relay.brevo.com
 * ACTIHOME_MAIL_PORT      587
 * ACTIHOME_MAIL_USER      el que da Brevo
 * ACTIHOME_MAIL_PASSWORD  la clave SMTP de Brevo
 * ACTIHOME_MAIL_FROM      la dirección remitente verificada
 * </pre>
 *
 * <p>
 * <b>Y por qué eso no es suficiente por sí solo.</b> ActiHome se reparte como un
 * {@code .exe}: un fichero en el disco de otra persona. Cualquier credencial que
 * viajara dentro —en el código, en el {@code application.yaml}, en un recurso—
 * <b>se puede extraer descompilándolo</b>, y quien la extraiga puede enviar
 * correo haciéndose pasar por el dueño de la cuenta hasta que Brevo la cierre.
 * Sacarlas a variables de entorno no es una buena práctica genérica: es lo que
 * hace que el ejecutable repartido, sencillamente, <b>no lleve nada que robar</b>
 * — sin las variables {@link #estaConfigurado()} devuelve {@code false} y la
 * aplicación usa el camino del código entregado por un administrador.
 *
 * <p>
 * Se usa JavaMail directamente en lugar de {@code JavaMailSender} de Spring por
 * una razón concreta: el autoconfigurador de Spring Boot crea el bean lea o no
 * la configuración, así que preguntarle "¿estás configurado?" no tiene respuesta
 * clara. Aquí las cuatro propiedades se leen a la vista y la respuesta es
 * evidente.
 */
@Component
public class BrevoEmailSender implements EmailSender {

	@Value("${actihome.mail.host:}")
	private String host;

	@Value("${actihome.mail.port:587}")
	private String puerto;

	@Value("${actihome.mail.user:}")
	private String usuario;

	@Value("${actihome.mail.password:}")
	private String clave;

	@Value("${actihome.mail.from:}")
	private String remitente;

	@Override
	public boolean estaConfigurado() {

		return !vacio(host) && !vacio(usuario) && !vacio(clave) && !vacio(remitente);
	}

	@Override
	public void enviar(String destinatario, String asunto, String cuerpo) throws EmailFailedException {

		if (!estaConfigurado()) {
			throw new EmailFailedException();
		}

		Properties propiedades = new Properties();
		propiedades.put("mail.smtp.auth", "true");
		propiedades.put("mail.smtp.starttls.enable", "true");
		propiedades.put("mail.smtp.host", host);
		propiedades.put("mail.smtp.port", puerto);

		// Sin estos dos, una conexión que se queda colgada bloquea el hilo que llama
		// para siempre. Va en un SwingWorker igual que la traducción, así que no
		// congela la interfaz, pero sí dejaría un "Enviando…" eterno.
		propiedades.put("mail.smtp.connectiontimeout", "10000");
		propiedades.put("mail.smtp.timeout", "10000");

		Session sesion = Session.getInstance(propiedades, new javax.mail.Authenticator() {

			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(usuario, clave);
			}
		});

		try {
			MimeMessage mensaje = new MimeMessage(sesion);
			mensaje.setFrom(new InternetAddress(remitente));
			mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
			mensaje.setSubject(asunto, "UTF-8");
			mensaje.setText(cuerpo, "UTF-8");

			Transport.send(mensaje);

		} catch (MessagingException ex) {
			// Servidor caído, credenciales rechazadas, dirección mal formada: al usuario
			// le sirve el mismo mensaje para todas, y el camino alternativo también.
			throw new EmailFailedException();
		}
	}

	private boolean vacio(String valor) {
		return valor == null || valor.trim().isEmpty();
	}
}
