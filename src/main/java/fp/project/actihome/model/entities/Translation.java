package fp.project.actihome.model.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Un texto ya traducido, guardado para no volver a pedirlo (Fase 8.7).
 *
 * <p>
 * <b>Traduce textos, no campos de entidades</b>, y esa es la decisión que hace
 * que todo lo demás encaje. No hay clave ajena a {@code HOUSINGS} ni a
 * {@code REVIEWS}: la clave es el propio texto. Consecuencias, todas buenas: la
 * misma descripción escrita en dos alojamientos se traduce una sola vez, borrar
 * un alojamiento no deja basura, y traducir un campo nuevo mañana no exige
 * tocar nada aquí.
 *
 * <p>
 * La clave es un <b>hash</b> del texto original y no el texto: un índice único
 * sobre una columna de 500 caracteres es frágil entre motores —MySQL limita los
 * bytes por índice— y obliga a comparar cadenas largas en cada búsqueda.
 */
@Entity
@Table(name = "TRANSLATIONS")
public class Translation {

	private Long id;

	private String sourceHash;

	private String targetLanguage;

	private String translatedText;

	/** Constructor sin argumentos, obligatorio para JPA. */
	public Translation() {

	}

	public Translation(String sourceHash, String targetLanguage, String translatedText) {

		this.sourceHash = sourceHash;
		this.targetLanguage = targetLanguage;
		this.translatedText = translatedText;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSourceHash() {
		return sourceHash;
	}

	public void setSourceHash(String sourceHash) {
		this.sourceHash = sourceHash;
	}

	public String getTargetLanguage() {
		return targetLanguage;
	}

	public void setTargetLanguage(String targetLanguage) {
		this.targetLanguage = targetLanguage;
	}

	public String getTranslatedText() {
		return translatedText;
	}

	public void setTranslatedText(String translatedText) {
		this.translatedText = translatedText;
	}
}
