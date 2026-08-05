package fp.project.actihome.ui.dev;

import fp.project.actihome.model.exceptions.TranslationFailedException;
import fp.project.actihome.model.services.MyMemoryTranslationClient;

/**
 * Llama al traductor de verdad, una vez, desde la línea de comandos.
 *
 * <pre>
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.ProbarTraduccion"
 * </pre>
 *
 * <p>
 * <b>Por qué existe si ya hay tests.</b> {@code TranslationServiceTests} corre
 * con un doble y comprueba la lógica del servicio; eso es lo correcto para la
 * suite —no puede depender de que haya red— pero deja sin comprobar justo la
 * parte que trata con el mundo real: que la URL es la que es, que el proveedor
 * sigue contestando lo mismo y que el JSON se lee bien. Eso no se puede
 * automatizar sin volver la suite frágil, así que se ejecuta a mano cuando se
 * toca el cliente.
 *
 * <p>
 * Es la misma división que el proyecto ya hace en otro sitio: la suite prueba lo
 * que es determinista, y las herramientas de {@code ui/dev} contestan las
 * preguntas que solo tienen respuesta mirando la realidad.
 */
public final class ProbarTraduccion {

	private ProbarTraduccion() {
	}

	public static void main(String[] args) {

		MyMemoryTranslationClient cliente = new MyMemoryTranslationClient();

		String texto = args.length > 0 ? String.join(" ", args)
				: "Casa de piedra restaurada a media ladera, con chimenea y vistas abiertas a la sierra.";

		System.out.println();
		System.out.println("original : " + texto);

		try {
			System.out.println("es -> en : " + cliente.traducir(texto, "es", "en"));
			System.out.println("es -> es : " + cliente.traducir(texto, "es", "es") + "   (mismo idioma: no llama)");
			System.out.println("vacio    : [" + cliente.traducir("", "es", "en") + "]");

		} catch (TranslationFailedException ex) {
			System.out.println("FALLO: el proveedor no ha contestado. Sin red, o ha cambiado la API.");
			System.exit(1);
		}

		System.out.println();
		System.out.println("El cliente habla con MyMemory correctamente.");
		System.exit(0);
	}
}
