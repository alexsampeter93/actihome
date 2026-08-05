package fp.project.actihome.ui.theme;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

import fp.project.actihome.model.services.ContentTranslationService;
import fp.project.actihome.model.services.ContentTranslationServiceImpl;

/**
 * Traduce el <b>contenido</b> al idioma activo, para que la aplicación no salga
 * medio en inglés y medio en español (Fase 8.7).
 *
 * <p>
 * <b>El problema que resuelve.</b> Desde la Fase 7.6 la interfaz está traducida
 * entera —botones, rótulos, mensajes— pero el contenido no: descripciones de
 * alojamientos y reseñas seguían en español con la aplicación en inglés. El
 * resultado era exactamente lo que el propio proyecto se había prohibido:
 * pantallas mezcladas. Traducir los rótulos y dejar el contenido es traducir la
 * mitad menos importante.
 *
 * <p>
 * <b>Es la hermana de {@link Textos}, y el reparto es limpio:</b> {@code Textos}
 * traduce lo que escribimos nosotros —está en los ficheros de idioma, es finito
 * y se conoce al compilar—; {@code Contenido} traduce lo que escriben los
 * usuarios, que no se conoce hasta que existe.
 *
 * <p>
 * <b>Nunca bloquea y nunca falla.</b> {@link #de(String)} contesta con la caché
 * o con el texto original, sin tocar la red jamás, así que se puede llamar
 * dentro del bucle que pinta una lista. Lo que sí llama por internet es
 * {@link #precalentar}, que va en segundo plano y avisa cuando ha traído algo
 * nuevo para que la pantalla se repinte. La primera vez que se abre el catálogo
 * en inglés puede verse un instante en español; a partir de ahí, ya no.
 *
 * <p>
 * <b>Por qué es estático como {@code Theme} y {@code Textos}.</b> Lo llaman
 * componentes que no son beans de Spring —{@code HousingCard},
 * {@code ReviewRow}, que se construyen a mano en cada recarga— y darles el
 * servicio por constructor obligaría a pasarlo por toda la cadena de la
 * interfaz. El servicio se inyecta una vez al arrancar, desde
 * {@link #instalar(ContentTranslationService)}.
 */
public final class Contenido {

	private static ContentTranslationService servicio;

	private Contenido() {
	}

	/**
	 * Le entrega el servicio. Lo llama {@code ActihomeApplication} al arrancar.
	 *
	 * <p>
	 * Mientras no se llame, {@link #de(String)} devuelve el texto tal cual. Eso no
	 * es una degradación silenciosa problemática: es exactamente lo que hace falta
	 * en las herramientas de {@code ui/dev} que dibujan componentes sin levantar
	 * Spring.
	 */
	public static void instalar(ContentTranslationService contentTranslationService) {
		servicio = contentTranslationService;
	}

	/**
	 * El texto en el idioma activo: traducido si está en la caché, original si no.
	 *
	 * <p>
	 * En español devuelve siempre el original sin consultar nada — el contenido
	 * está escrito en español, así que no hay nada que traducir.
	 */
	public static String de(String texto) {

		if (servicio == null || texto == null) {
			return texto;
		}

		return servicio.traducido(texto, Textos.idioma().getLanguage());
	}

	/**
	 * Traduce en segundo plano lo que falte y avisa si ha traído algo.
	 *
	 * <p>
	 * La pantalla llama a esto <b>después</b> de pintarse con lo que hubiera, y en
	 * {@code alTerminar} se limita a recargarse. Ese orden es el que hace que
	 * nunca haya una espera visible: primero se ve algo, luego mejora.
	 *
	 * @param textos    los textos de la pantalla que se acaba de pintar
	 * @param alTerminar qué hacer si se ha traducido algo nuevo. Corre en el hilo de
	 *                   la interfaz, así que puede repintar sin más
	 */
	public static void precalentar(List<String> textos, Runnable alTerminar) {

		String destino = Textos.idioma().getLanguage();

		if (servicio == null || textos.isEmpty()
				|| ContentTranslationServiceImpl.IDIOMA_DEL_CONTENIDO.equals(destino)) {
			return;
		}

		List<String> copia = new ArrayList<>(textos);

		new SwingWorker<Integer, Void>() {

			@Override
			protected Integer doInBackground() {
				return servicio.precalentar(copia, ContentTranslationServiceImpl.IDIOMA_DEL_CONTENIDO, destino);
			}

			@Override
			protected void done() {

				try {
					// Solo se repinta si de verdad hay algo nuevo. Repintar siempre haría
					// parpadear la pantalla cada vez que se abre con la caché ya completa,
					// que es el caso normal a partir de la segunda visita.
					if (get() > 0) {
						alTerminar.run();
					}

				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();

				} catch (java.util.concurrent.ExecutionException ex) {
					// precalentar() no propaga fallos de traducción: si llega algo aquí es un
					// fallo de programación, y lo peor que puede pasar es que la pantalla se
					// quede en español, que es justo el comportamiento previsto sin red.
					alTerminar.run();
				}
			}
		}.execute();
	}
}
