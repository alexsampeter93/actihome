package fp.project.actihome.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import fp.project.actihome.model.exceptions.WeatherUnavailableException;

/**
 * La previsión meteorológica y, sobre todo, su caché (F18).
 *
 * <p>
 * <b>Ninguno de estos tests toca la red</b>, y esa es la razón de que
 * {@link WeatherClient} sea una interfaz. Un test que llamara a Open-Meteo de
 * verdad fallaría en un tren, sería lento y convertiría cada ejecución de la
 * suite en tráfico real contra un servicio gratuito. Aquí se comprueba lo que de
 * verdad le toca al servicio: <b>cuántas veces llama al cliente</b>, que es toda
 * su razón de existir.
 *
 * <p>
 * <b>Sin {@code @Transactional}</b>, igual que {@code TranslationServiceTests}:
 * no hay nada que revertir porque este servicio no escribe en la base de datos.
 * Que no haga falta es en sí una comprobación de que la separación está bien
 * hecha.
 */
@SpringBootTest
@ActiveProfiles("test")
public class WeatherServiceTests {

	@Autowired
	private WeatherService weatherService;

	@MockBean
	private WeatherClient weatherClient;

	/**
	 * Dos consultas iguales, una sola llamada al proveedor.
	 *
	 * <p>
	 * Es la razón de existir de este servicio: la ficha de un alojamiento se abre y
	 * se cierra constantemente y sin caché cada visita sería otra petición para
	 * obtener exactamente la misma respuesta.
	 *
	 * <p>
	 * <b>Cada test usa coordenadas propias</b> porque la caché es un campo de
	 * instancia del bean y el contexto de Spring se comparte entre tests. Con
	 * coordenadas repetidas, el orden de ejecución decidiría el resultado — y un
	 * test que depende del orden es un test que falla un día al azar.
	 */
	@Test
	public void testLaSegundaConsultaSaleDeLaCache() throws WeatherUnavailableException {

		when(weatherClient.tiempo(anyDouble(), anyDouble(), anyInt())).thenReturn(unDia());

		weatherService.tiempoDe(10.0, 10.0, 5);
		weatherService.tiempoDe(10.0, 10.0, 5);

		verify(weatherClient, times(1)).tiempo(10.0, 10.0, 5);
	}

	/**
	 * Pedir más días no se sirve desde una respuesta más corta.
	 *
	 * <p>
	 * El número de días entra en la clave de la caché a propósito: una respuesta de
	 * tres días no contesta una pregunta de siete. Sin esto, la primera consulta de
	 * la sesión decidiría cuántos días ve todo el mundo durante la hora siguiente.
	 */
	@Test
	public void testElNumeroDeDiasFormaParteDeLaClave() throws WeatherUnavailableException {

		when(weatherClient.tiempo(anyDouble(), anyDouble(), anyInt())).thenReturn(unDia());

		weatherService.tiempoDe(20.0, 20.0, 3);
		weatherService.tiempoDe(20.0, 20.0, 7);

		verify(weatherClient, times(1)).tiempo(20.0, 20.0, 3);
		verify(weatherClient, times(1)).tiempo(20.0, 20.0, 7);
	}

	/** Dos alojamientos distintos no comparten previsión. */
	@Test
	public void testCadaPuntoTieneLaSuya() throws WeatherUnavailableException {

		when(weatherClient.tiempo(anyDouble(), anyDouble(), anyInt())).thenReturn(unDia());

		weatherService.tiempoDe(30.0, 30.0, 5);
		weatherService.tiempoDe(31.0, 30.0, 5);

		verify(weatherClient, times(1)).tiempo(30.0, 30.0, 5);
		verify(weatherClient, times(1)).tiempo(31.0, 30.0, 5);
	}

	/**
	 * Un fallo no se guarda: la siguiente consulta vuelve a intentarlo.
	 *
	 * <p>
	 * <b>Es la parte de la caché que más fácil sería hacer mal.</b> Guardar el
	 * fallo convertiría un corte de red de dos segundos en una hora entera sin
	 * previsión, cuando reintentar cuesta una petición. Y el usuario no tendría
	 * forma de saber por qué: la ficha se vería idéntica con red y sin ella.
	 */
	@Test
	public void testUnFalloNoSeGuardaEnLaCache() throws WeatherUnavailableException {

		when(weatherClient.tiempo(anyDouble(), anyDouble(), anyInt()))
				.thenThrow(new WeatherUnavailableException());

		assertThrows(WeatherUnavailableException.class, () -> weatherService.tiempoDe(40.0, 40.0, 5));
		assertThrows(WeatherUnavailableException.class, () -> weatherService.tiempoDe(40.0, 40.0, 5));

		verify(weatherClient, times(2)).tiempo(40.0, 40.0, 5);
	}

	/** Lo que llega del proveedor llega intacto arriba, las dos mitades. */
	@Test
	public void testDevuelveLoQueDaElProveedor() throws WeatherUnavailableException {

		when(weatherClient.tiempo(anyDouble(), anyDouble(), anyInt())).thenReturn(unDia());

		TiempoDelSitio tiempo = weatherService.tiempoDe(50.0, 50.0, 5);

		assertEquals(1, tiempo.dias().size());
		assertEquals(28.4, tiempo.dias().get(0).maxima());
		assertEquals(CieloWmo.DESPEJADO, tiempo.dias().get(0).cielo());

		assertEquals(24.0, tiempo.ahora().temperatura());
		assertEquals(CieloWmo.NUBLADO, tiempo.ahora().cielo());
	}

	/**
	 * La sensación térmica solo se enseña cuando difiere de verdad.
	 *
	 * <p>
	 * <b>Es una regla de producto, no de formato, y por eso está fijada aquí.</b>
	 * Escribir "24°, sensación 24°" ocupa sitio para no decir nada y entrena al
	 * usuario a saltarse esa línea — con lo cual tampoco la leerá el día que sí
	 * signifique algo. El umbral son dos grados: por debajo es ruido de medición,
	 * por encima significa viento o humedad.
	 */
	@Test
	public void testLaSensacionSoloCuentaSiSeNota() {

		assertFalse(new TiempoAhora(24, 24, 0, true).sensacionRelevante());
		assertFalse(new TiempoAhora(24, 25, 0, true).sensacionRelevante());

		assertTrue(new TiempoAhora(24, 26, 0, true).sensacionRelevante());

		// Y en los dos sentidos: el viento resta y la humedad suma.
		assertTrue(new TiempoAhora(24, 19, 0, true).sensacionRelevante());
	}

	/**
	 * Un ejemplo con las dos mitades.
	 *
	 * <p>
	 * La fecha se calcula desde {@code LocalDate.now()} y no se escribe a mano. Es
	 * la regla del proyecto, aprendida cuando 19 fechas fijas caducaron y dejaron
	 * la suite en rojo sin que nadie tocara una línea de código.
	 *
	 * <p>
	 * <b>El instante lleva un código de cielo distinto del día</b> (nublado ahora,
	 * despejado hoy) a propósito: si los dos fueran iguales, un fallo que
	 * confundiera una mitad con la otra pasaría desapercibido.
	 */
	private TiempoDelSitio unDia() {

		return new TiempoDelSitio(new TiempoAhora(24.0, 22.0, 3, true),
				List.of(new PrevisionDiaria(LocalDate.now(), 28.4, 15.1, 0)));
	}
}
