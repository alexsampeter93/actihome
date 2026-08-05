package fp.project.actihome.model.services;

import fp.project.actihome.model.exceptions.EmailFailedException;

/**
 * Envía un correo. Una sola operación y una pregunta.
 *
 * <p>
 * <b>{@link #estaConfigurado()} es la parte que importa del diseño.</b> Sin
 * ella, la aplicación tendría que intentar el envío para descubrir que no puede
 * —esperando a que falle la conexión— y solo entonces ofrecer la alternativa. Con
 * ella, la pantalla sabe <em>antes de pedir nada</em> qué camino tiene delante y
 * puede decirlo con claridad: "te hemos enviado un código" o "pídeselo a un
 * administrador". Un usuario al que se le promete un correo que nunca llega se
 * queda esperando; uno al que se le dice desde el principio que vaya por otro
 * lado, no.
 *
 * <p>
 * Es una interfaz por lo mismo que {@link TranslationClient}: detrás hay una
 * conexión a un servidor ajeno, y eso no se puede usar en un test.
 */
public interface EmailSender {

	/**
	 * Si hay credenciales de servidor de correo configuradas.
	 *
	 * <p>
	 * No comprueba que funcionen —eso solo se sabe intentándolo— sino que estén.
	 * Es la diferencia entre "esta instalación no envía correo" y "el envío ha
	 * fallado", que son dos situaciones distintas y se cuentan distinto.
	 */
	boolean estaConfigurado();

	void enviar(String destinatario, String asunto, String cuerpo) throws EmailFailedException;
}
