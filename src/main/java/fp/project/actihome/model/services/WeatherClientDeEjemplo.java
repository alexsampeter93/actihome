package fp.project.actihome.model.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Una previsión inventada, siempre la misma, sin salir a internet (F18).
 *
 * <p>
 * <b>Existe por las herramientas de medida y por el CI, y resuelve dos problemas
 * distintos a la vez.</b>
 *
 * <p>
 * <b>El primero: el CI no debe salir a internet.</b> {@code MedirResponsive} y
 * {@code MedirContraste} construyen las pantallas de verdad, y desde que la ficha
 * de alojamiento consulta el tiempo, eso significaba una petición real en cada
 * ejecución. Una comprobación automática que depende de que un servicio de un
 * tercero esté de pie deja de comprobar lo que dice comprobar: el día que
 * Open-Meteo tenga un mal rato, el CI se pondría rojo por un motivo que no tiene
 * nada que ver con el código que se acaba de subir.
 *
 * <p>
 * <b>El segundo, y es el más sutil: una respuesta de red hace la medición
 * indeterminista.</b> La previsión llega en un hilo de fondo y aparece cuando
 * llega. Midiendo una ventana, eso significa que el panel puede materializarse a
 * mitad del recorrido del árbol de componentes: la misma pantalla mide una cosa
 * u otra según lo rápida que vaya la red en ese momento. Un detector cuyo
 * resultado depende del cronómetro no sirve para decidir si algo se sale.
 *
 * <p>
 * <b>Por qué devuelve datos y no un fallo.</b> Podría lanzar
 * {@code WeatherUnavailableException} y dejar el panel oculto, que también sería
 * determinista — pero entonces las herramientas medirían la pantalla <em>sin</em>
 * el bloque, que es justo el caso fácil. Lo que hay que medir es el peor caso: el
 * panel presente, con sus cinco columnas ocupando sitio. Un detector que mide la
 * versión cómoda de la pantalla no protege de nada.
 *
 * <p>
 * Se activa con {@code --actihome.meteorologia.habilitada=false}. Por omisión
 * está habilitada, así que la aplicación de verdad usa
 * {@link OpenMeteoWeatherClient} sin que nadie tenga que configurar nada.
 */
@Component
@ConditionalOnProperty(name = "actihome.meteorologia.habilitada", havingValue = "false")
public class WeatherClientDeEjemplo implements WeatherClient {

	/**
	 * Los cinco estados del cielo que sabe dibujar {@code IconoDelCielo}, en orden.
	 *
	 * <p>
	 * No es casualidad ni variedad decorativa: así una captura de la ficha enseña
	 * <b>todos</b> los iconos a la vez, y si alguno estuviera mal dibujado se vería
	 * en la misma imagen en lugar de hacer falta cinco.
	 */
	private static final int[] CODIGOS = { 0, 3, 61, 71, 95 };

	@Override
	public List<PrevisionDiaria> prevision(double latitud, double longitud, int dias) {

		List<PrevisionDiaria> inventados = new ArrayList<>();

		// Las fechas salen de LocalDate.now() y no se escriben a mano. Es la regla del
		// proyecto, aprendida cuando 19 fechas fijas caducaron y dejaron la suite en
		// rojo sin que nadie tocara una línea de código.
		LocalDate hoy = LocalDate.now();

		for (int i = 0; i < Math.min(dias, CODIGOS.length); i++) {
			inventados.add(new PrevisionDiaria(hoy.plusDays(i), 24 + i, 12 + i, CODIGOS[i]));
		}

		return inventados;
	}
}
