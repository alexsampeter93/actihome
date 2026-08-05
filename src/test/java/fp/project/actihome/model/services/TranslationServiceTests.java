package fp.project.actihome.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import fp.project.actihome.model.exceptions.TranslationFailedException;

/**
 * La traducción de reseñas (Fase 8.5).
 *
 * <p>
 * <b>Estos tests no tocan la red, y esa es la razón de que
 * {@link TranslationClient} sea una interfaz.</b> {@code @MockBean} sustituye al
 * cliente de MyMemory por un doble que devuelve lo que se le diga, así que se
 * comprueba lo que de verdad le toca al servicio —que traduce las dos partes por
 * separado y que propaga el fallo— sin depender de que haya internet ni de que
 * el proveedor esté de pie. Un test que llamara a MyMemory de verdad fallaría en
 * un tren y gastaría la cuota diaria de la aplicación cada vez que alguien
 * ejecutara la suite.
 *
 * <p>
 * <b>Sin {@code @Transactional}</b>, a diferencia de las otras clases de test de
 * servicio: no hay nada que revertir porque este servicio no escribe en la base
 * de datos. Que no haga falta es en sí una comprobación de que la separación de
 * la que habla {@link TranslationService} está bien hecha.
 */
@SpringBootTest
@ActiveProfiles("test")
public class TranslationServiceTests {

	@Autowired
	private TranslationService translationService;

	@MockBean
	private TranslationClient translationClient;

	@Test
	public void testTraducirResenaDevuelveLasDosPartes() throws TranslationFailedException {

		when(translationClient.traducir("Título", "es", "en")).thenReturn("Title");
		when(translationClient.traducir("Cuerpo", "es", "en")).thenReturn("Body");

		TranslatedReview traducida = translationService.traducirResena("Título", "Cuerpo", "es", "en");

		assertEquals("Title", traducida.getTitle());
		assertEquals("Body", traducida.getBody());
	}

	/**
	 * Si el proveedor falla, el fallo llega arriba.
	 *
	 * <p>
	 * Lo importante es lo que <b>no</b> hace: no devuelve cadenas vacías ni el
	 * texto original disfrazado de traducción. La pantalla necesita poder
	 * distinguir "esto está traducido" de "esto no se pudo traducir" para decirlo,
	 * y un servicio que se traga el fallo se lo impide.
	 */
	@Test
	public void testTraducirResenaPropagaElFallo() throws TranslationFailedException {

		when(translationClient.traducir(anyString(), anyString(), anyString()))
				.thenThrow(new TranslationFailedException());

		assertThrows(TranslationFailedException.class,
				() -> translationService.traducirResena("Título", "Cuerpo", "es", "en"));
	}
}
