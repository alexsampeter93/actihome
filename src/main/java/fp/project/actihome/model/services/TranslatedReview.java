package fp.project.actihome.model.services;

/**
 * El título y el cuerpo de una reseña, traducidos.
 *
 * <p>
 * <b>La traducción no se guarda en la reseña</b>, y esa es la decisión de fondo
 * de toda la Fase 8.5. Guardarla parecía tentador —se traduce una vez y ya está—
 * pero trae tres problemas que no compensan: habría que decidir a qué idioma se
 * guarda (la aplicación tiene dos, mañana podría tener cinco), habría que
 * invalidarla si el autor edita la reseña, y sobre todo <b>convertiría en dato
 * permanente lo que es una conveniencia de lectura</b>. Una reseña la escribió
 * alguien en su idioma; la traducción automática es una ayuda para quien no lo
 * habla, no una segunda versión oficial del texto.
 *
 * <p>
 * Por eso esto es un objeto de paso y no una entidad: nace al pulsar "Traducir",
 * vive lo que dure la pantalla abierta y desaparece.
 */
public class TranslatedReview {

	private final String title;
	private final String body;

	public TranslatedReview(String title, String body) {
		this.title = title;
		this.body = body;
	}

	public String getTitle() {
		return title;
	}

	public String getBody() {
		return body;
	}
}
