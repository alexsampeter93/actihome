package fp.project.actihome.model.services;

import java.util.List;
import java.util.Map;

/**
 * Traduce el <b>contenido</b> —descripciones, reseñas— con caché (Fase 8.7).
 *
 * <p>
 * <b>Es distinto de {@link TranslationService} y conviene tener clara la
 * diferencia.</b> Aquel traduce cuando el usuario pulsa "Traducir": una acción
 * explícita, que puede tardar y que puede fallar delante de él. Este resuelve
 * el contenido de una pantalla entera al pintarla, y por eso tiene tres reglas
 * que aquel no necesita:
 *
 * <ul>
 * <li><b>Nunca bloquea.</b> {@link #traducido} contesta con lo que hay en la
 * caché y, si no hay nada, devuelve el texto original. Jamás llama por
 * internet. Una pantalla que espera a una traducción para pintarse es una
 * pantalla congelada.</li>
 * <li><b>Nunca falla.</b> Sin red, sin cuota o con el proveedor caído, se ve el
 * texto en español. Es peor que verlo traducido y muchísimo mejor que no ver
 * nada.</li>
 * <li><b>Guarda lo que traduce.</b> Sin caché, entrar al catálogo en inglés
 * serían treinta llamadas <em>cada vez</em>, y con 5.000 palabras al día de
 * cuota eso se agota en dos sesiones. Con caché es una vez por texto y para
 * siempre.</li>
 * </ul>
 *
 * <p>
 * El reparto entre los dos métodos es el que hace que funcione: la pantalla
 * llama a {@link #traducido} y pinta ya, y por separado lanza
 * {@link #precalentar} en segundo plano para lo que faltara. La próxima vez que
 * se abra esa pantalla ya estará todo.
 */
public interface ContentTranslationService {

	/**
	 * Lo que haya en la caché, o el texto original si no hay nada.
	 *
	 * <p>
	 * <b>No llama por internet nunca</b>, así que se puede usar dentro del bucle
	 * que pinta una lista sin ningún miedo.
	 */
	String traducido(String texto, String idiomaDestino);

	/**
	 * Igual que {@link #traducido} pero para varios textos, con una sola consulta.
	 *
	 * @return un mapa del texto original a su traducción; los que no estén en la
	 *         caché se devuelven apuntando a sí mismos, de modo que quien lo use
	 *         no necesita comprobar nada
	 */
	Map<String, String> traducidos(List<String> textos, String idiomaDestino);

	/**
	 * Traduce y guarda los textos que todavía no estén en la caché.
	 *
	 * <p>
	 * <b>Esto sí llama por internet</b>, así que quien lo invoque tiene que
	 * hacerlo fuera del hilo de la interfaz. No devuelve nada ni lanza nada: si
	 * falla, la caché se queda como estaba y se volverá a intentar la próxima vez.
	 *
	 * @return cuántos textos se han añadido a la caché, para poder saber si merece
	 *         la pena repintar la pantalla
	 */
	int precalentar(List<String> textos, String idiomaOrigen, String idiomaDestino);
}
