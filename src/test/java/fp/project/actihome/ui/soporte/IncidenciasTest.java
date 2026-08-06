package fp.project.actihome.ui.soporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * El manejador global de fallos.
 *
 * <p>
 * <b>Por qué merece un test, siendo cuatro líneas.</b> Todo el valor de
 * {@link Incidencias} descansa en una afirmación sobre el JDK que es fácil de
 * dar por buena y difícil de comprobar mirando: que una excepción escapada de un
 * manejador de eventos de Swing acaba en el manejador de excepciones no
 * capturadas del hilo. Fue verdad a partir de Java 7 y antes no lo era —hacía
 * falta la propiedad interna {@code sun.awt.exception.handler}—, así que es
 * exactamente la clase de suposición que envejece mal.
 *
 * <p>
 * Y si esa suposición fuese falsa, <b>nada fallaría de forma visible</b>: la
 * aplicación seguiría arrancando, el manejador seguiría instalado, y solo se
 * notaría el día que un usuario tuviera un fallo y el fichero de registro
 * estuviera vacío. Es decir, el peor día posible para descubrirlo.
 *
 * <p>
 * Este test no comprueba que se escriba el fichero —eso es trabajo de Logback,
 * no nuestro— sino lo único que sí es nuestro: <b>que el fallo llega hasta
 * aquí</b>.
 */
public class IncidenciasTest {

	private Thread.UncaughtExceptionHandler anterior;

	@AfterEach
	public void restaurarElManejador() {

		// Este test toca estado global de la JVM. Dejarlo puesto haría que un fallo de
		// cualquier otro test de la suite se desviara a este manejador en vez de
		// informarse como un fallo del test, que es la forma de que un test rompa a
		// otro sin ninguna relación aparente entre los dos.
		Thread.setDefaultUncaughtExceptionHandler(anterior);
	}

	/**
	 * Una excepción lanzada dentro del hilo de eventos llega al manejador global.
	 *
	 * <p>
	 * Es la afirmación entera sobre la que se sostiene {@link Incidencias}.
	 */
	@Test
	public void testUnFalloEnElHiloDeEventosLlegaAlManejador() throws Exception {

		anterior = Thread.getDefaultUncaughtExceptionHandler();

		AtomicReference<Throwable> capturado = new AtomicReference<>();
		CountDownLatch avisado = new CountDownLatch(1);

		Thread.setDefaultUncaughtExceptionHandler((hilo, fallo) -> {
			capturado.set(fallo);
			avisado.countDown();
		});

		java.awt.EventQueue.invokeLater(() -> {
			throw new IllegalStateException("fallo de prueba");
		});

		assertTrue(avisado.await(5, TimeUnit.SECONDS),
				"El hilo de eventos no propago el fallo al manejador global en 5 segundos");

		assertNotNull(capturado.get());
		assertEquals("fallo de prueba", capturado.get().getMessage());
	}

	/**
	 * Registrar un fallo no lanza a su vez.
	 *
	 * <p>
	 * Un manejador de errores que revienta se come el error original, que es el que
	 * de verdad importaba. Aquí se le pasa lo peor que puede recibir —un nombre de
	 * hilo nulo— para comprobar que aun así termina.
	 */
	@Test
	public void testRegistrarNuncaLanza() {

		anterior = Thread.getDefaultUncaughtExceptionHandler();

		Incidencias.registrar(null, new IllegalStateException("otro fallo de prueba"));
		Incidencias.registrar("hilo-de-prueba", new IllegalStateException((String) null));
	}
}
