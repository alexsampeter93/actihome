package fp.project.actihome.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * La caché de traducción de contenido (Fase 8.7).
 *
 * <p>
 * <b>El test que más importa es el primero, y no es evidente por qué.</b> La
 * caché se busca por un <em>hash</em> del texto original, y ese hash lo calculan
 * <b>dos sitios distintos</b>: {@code ui/dev/SembrarTraducciones}, que genera el
 * SQL de precarga, y {@code ContentTranslationServiceImpl}, que consulta. Si las
 * dos implementaciones dejaran de coincidir —un cambio de algoritmo, de
 * codificación, un {@code trim()} de más— la aplicación no fallaría: se limitaría
 * a no encontrar nunca nada y a enseñarlo todo en español, exactamente igual que
 * si no hubiera red.
 *
 * <p>
 * Es un fallo silencioso entre dos ficheros que nadie mira a la vez.
 * {@code testLaSemillaSeResuelve} es lo que lo convierte en un test rojo.
 *
 * <p>
 * {@code TranslationClient} va sustituido por un doble: estos tests comprueban
 * la caché, no el traductor, y no deben depender de que haya red.
 */
@SpringBootTest
@ActiveProfiles("test")
public class ContentTranslationServiceTests {

	@Autowired
	private ContentTranslationService contentTranslationService;

	@MockBean
	private TranslationClient translationClient;

	/**
	 * Un texto sembrado a mano se encuentra en la caché.
	 *
	 * <p>
	 * Comprueba de una vez tres cosas que solo funcionan si están alineadas: que
	 * {@code traducciones.sql} se carga al arrancar, que el hash generado coincide
	 * con el calculado, y que la consulta lo encuentra.
	 */
	@Test
	public void testLaSemillaSeResuelve() {

		String original = "Silencio y buen desayuno";

		assertEquals("Quiet, and a good breakfast", contentTranslationService.traducido(original, "en"));
	}

	/** En español se devuelve el original sin consultar nada. */
	@Test
	public void testEnEspanolNoSeTraduce() {

		String original = "Silencio y buen desayuno";

		assertEquals(original, contentTranslationService.traducido(original, "es"));
	}

	/**
	 * Un texto que no está en la caché se devuelve <b>tal cual</b>, no vacío ni
	 * nulo.
	 *
	 * <p>
	 * Es lo que hace que se pueda llamar dentro del bucle que pinta una lista sin
	 * comprobar nada: sin traducción se ve el español, que es peor que verlo en
	 * inglés y muchísimo mejor que ver un hueco.
	 */
	@Test
	public void testSinTraduccionDevuelveElOriginal() {

		String desconocido = "Un texto que nadie ha traducido nunca";

		assertEquals(desconocido, contentTranslationService.traducido(desconocido, "en"));
	}

	/**
	 * La versión en lote devuelve una entrada por texto, traducida o no.
	 *
	 * <p>
	 * Que los no traducidos apunten a sí mismos es parte del contrato: quien lo use
	 * puede pintar el mapa entero sin un solo {@code if}.
	 */
	@Test
	public void testEnLoteDevuelveTodosLosTextos() {

		String conocido = "Perfecta para desconectar";
		String desconocido = "Otro texto sin traducir";

		List<String> textos = Arrays.asList(conocido, desconocido);
		Map<String, String> resultado = contentTranslationService.traducidos(textos, "en");

		assertEquals(2, resultado.size());
		assertNotEquals(conocido, resultado.get(conocido));
		assertEquals(desconocido, resultado.get(desconocido));
	}

	/** Sin nada que traducir, ni se intenta. */
	@Test
	public void testNuloYVacioSeDevuelvenIgual() {

		assertEquals(null, contentTranslationService.traducido(null, "en"));
		assertEquals("   ", contentTranslationService.traducido("   ", "en"));
	}
}
