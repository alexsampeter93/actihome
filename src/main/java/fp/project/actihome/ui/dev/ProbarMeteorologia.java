package fp.project.actihome.ui.dev;

import java.util.List;

import fp.project.actihome.model.exceptions.LocationNotFoundException;
import fp.project.actihome.model.exceptions.WeatherUnavailableException;
import fp.project.actihome.model.services.Coordenadas;
import fp.project.actihome.model.services.OpenMeteoGeocodingClient;
import fp.project.actihome.model.services.OpenMeteoWeatherClient;
import fp.project.actihome.model.services.PrevisionDiaria;
import fp.project.actihome.model.services.TiempoAhora;
import fp.project.actihome.model.services.TiempoDelSitio;

/**
 * Llama a Open-Meteo de verdad, una vez, desde la línea de comandos (F17, F18).
 *
 * <pre>
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.ProbarMeteorologia"
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.ProbarMeteorologia" "-Dexec.args=Ronda, Málaga"
 * </pre>
 *
 * <p>
 * <b>Por qué existe si ya hay tests.</b> {@code WeatherServiceTests} corre con un
 * doble y comprueba la caché; eso es lo correcto para la suite —no puede depender
 * de que haya red— pero deja sin comprobar la parte que trata con el mundo real:
 * que la URL es la que es, que el proveedor sigue contestando con la misma forma
 * de JSON y que los arrays paralelos se leen bien. Es la misma división y el
 * mismo motivo que {@link ProbarTraduccion}.
 *
 * <p>
 * <b>No arranca Spring</b>, a diferencia de {@code ProbarCorreo}, y la razón es
 * la misma que allí pero al revés: en el correo lo más probable que falle es la
 * cadena de configuración (variable de entorno → {@code application.yaml} →
 * {@code @Value}), y hay que ejercitarla. Aquí <b>no hay ninguna configuración
 * que pueda estar mal</b> —ni clave, ni credenciales, ni variables— así que
 * levantar el contexto solo añadiría segundos. Que esta herramienta pueda
 * permitírselo es en sí la prueba de por qué se eligió este proveedor.
 *
 * <p>
 * Prueba las dos mitades en el orden en que las usa la aplicación: primero
 * localiza el sitio, luego pide el tiempo de ese punto. Si la primera falla, la
 * segunda no tendría a dónde preguntar.
 */
public final class ProbarMeteorologia {

	private ProbarMeteorologia() {
	}

	public static void main(String[] args) {

		String lugar = args.length > 0 ? String.join(" ", args) : "Sierra Nevada, Granada";

		System.out.println();
		System.out.println("Buscando: " + lugar);

		Coordenadas punto;

		try {
			punto = new OpenMeteoGeocodingClient().localizar(lugar);

		} catch (LocationNotFoundException ex) {
			System.out.println("FALLO al localizar. Sin red, o el buscador no conoce ese sitio.");
			System.exit(1);
			return;
		}

		System.out.printf("Encontrado: %s  (%.4f, %.4f)%n", punto.etiqueta(), punto.latitud(), punto.longitud());
		System.out.println();

		try {
			TiempoDelSitio tiempo = new OpenMeteoWeatherClient().tiempo(punto.latitud(), punto.longitud(), 5);

			// El instante primero, que es la parte nueva y la que hay que mirar: si esta
			// linea no coincide con lo que dice cualquier aplicacion del tiempo para ese
			// sitio, algo va mal en el cliente.
			TiempoAhora ahora = tiempo.ahora();

			if (ahora == null) {
				System.out.println("AVISO: el proveedor no ha mandado el bloque de tiempo actual.");
			} else {
				System.out.printf("AHORA: %.1f grados (sensacion %.1f)  %s  %s%n", ahora.temperatura(),
						ahora.sensacion(), ahora.cielo(), ahora.esDeDia() ? "de dia" : "de noche");
			}

			System.out.println();

			List<PrevisionDiaria> dias = tiempo.dias();

			for (PrevisionDiaria dia : dias) {
				System.out.printf("  %s   %3.0f / %3.0f    %-10s (codigo %d)%n", dia.fecha(), dia.maxima(),
						dia.minima(), dia.cielo(), dia.codigo());
			}

			// Un código sin traducir no rompe nada —la ficha no pinta icono— pero es la
			// señal de que la tabla WMO se ha quedado corta, y eso solo se ve mirando.
			long sinTraducir = dias.stream().filter(d -> d.cielo() == fp.project.actihome.model.services.CieloWmo.DESCONOCIDO).count();

			if (sinTraducir > 0) {
				System.out.println();
				System.out.println("AVISO: " + sinTraducir + " dia(s) con un codigo que CieloWmo no conoce.");
			}

		} catch (WeatherUnavailableException ex) {
			System.out.println("FALLO al pedir la prevision. Sin red, o ha cambiado la API.");
			System.exit(1);
			return;
		}

		System.out.println();
		System.out.println("Los dos clientes hablan con Open-Meteo correctamente. Sin clave de API.");
		System.exit(0);
	}
}
