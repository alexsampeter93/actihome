package fp.project.actihome.model.services;

import fp.project.actihome.model.exceptions.TranslationFailedException;

/**
 * Traduce el texto de una reseña para poder leerla, sin guardar nada.
 *
 * <p>
 * <b>Recibe los textos y no el identificador de la reseña, y esa es la decisión
 * de la Fase 8.5.</b> La primera versión vivía en {@code ReviewService} y
 * recibía un {@code reviewId}: leía la reseña de la base de datos y luego
 * llamaba al traductor. Parecía lo natural —el resto de los métodos del proyecto
 * funcionan así— y estaba mal por dos motivos que se descubrieron por caminos
 * distintos:
 *
 * <ul>
 * <li><b>Mezclaba base de datos y red en la misma operación.</b>
 * {@code ReviewServiceImpl} está anotado {@code @Transactional} de clase, así
 * que la llamada por internet ocurría con una conexión de base de datos abierta
 * y retenida. Diez segundos de espera de red son diez segundos de una conexión
 * del pozo sin poder usarse, por algo que ni siquiera escribe en la base.
 * Marcarlo {@code NOT_SUPPORTED} lo arreglaba…</li>
 * <li>…<b>y entonces los tests empezaron a fallar</b>, que es como se vio el
 * problema de fondo. Suspender la transacción hace que el método deje de ver lo
 * que la transacción de fuera todavía no ha confirmado. Se puede trabajar
 * alrededor, pero la señal era otra: <b>si leer y traducir no pueden convivir en
 * la misma transacción, es que no son la misma operación</b>.</li>
 * </ul>
 *
 * <p>
 * Separadas, todo se simplifica: esto no toca la base de datos, así que no
 * necesita transacción ni puede fallar por no encontrar nada. Y quien lo llama
 * es una pantalla que <em>está mostrando la reseña</em>, así que ya tiene los
 * textos delante — releerlos de la base para traducirlos era trabajo de más para
 * confirmar algo que ya se sabía.
 */
public interface TranslationService {

	/**
	 * @param titulo el título de la reseña
	 * @param cuerpo el cuerpo de la reseña
	 * @param desde  código ISO del idioma en que está escrita ("es")
	 * @param hasta  código ISO del idioma al que se quiere leer ("en")
	 * @throws TranslationFailedException si no hay red o el proveedor no responde
	 */
	TranslatedReview traducirResena(String titulo, String cuerpo, String desde, String hasta)
			throws TranslationFailedException;
}
