package fp.project.actihome.model.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.Translation;
import fp.project.actihome.model.entities.TranslationDao;
import fp.project.actihome.model.exceptions.TranslationFailedException;

@Service
@Transactional
public class ContentTranslationServiceImpl implements ContentTranslationService {

	/** El idioma en el que está escrito el contenido de la aplicación. */
	public static final String IDIOMA_DEL_CONTENIDO = "es";

	@Autowired
	private TranslationDao translationDao;

	@Autowired
	private TranslationClient translationClient;

	@Override
	@Transactional(readOnly = true)
	public String traducido(String texto, String idiomaDestino) {

		if (noHayNadaQueTraducir(texto, idiomaDestino)) {
			return texto;
		}

		Optional<Translation> guardada = translationDao.findBySourceHashAndTargetLanguage(hash(texto), idiomaDestino);

		return guardada.map(Translation::getTranslatedText).orElse(texto);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, String> traducidos(List<String> textos, String idiomaDestino) {

		// LinkedHashMap y no HashMap: quien lo reciba puede querer recorrerlo, y un
		// orden que cambia entre ejecuciones convierte cualquier salida en algo
		// imposible de comparar. Es la misma decisión que ya se tomó en
		// PlatformPanelFrame y en MessageServiceImpl.
		Map<String, String> resultado = new LinkedHashMap<>();

		for (String texto : textos) {
			resultado.put(texto, texto);
		}

		if (IDIOMA_DEL_CONTENIDO.equals(idiomaDestino)) {
			return resultado;
		}

		// Un mapa de hash a texto para poder volver del resultado de la consulta al
		// texto original: la tabla guarda el hash, no el texto.
		Map<String, String> porHash = new HashMap<>();

		for (String texto : textos) {
			if (texto != null && !texto.trim().isEmpty()) {
				porHash.put(hash(texto), texto);
			}
		}

		if (porHash.isEmpty()) {
			return resultado;
		}

		for (Translation guardada : translationDao.findBySourceHashInAndTargetLanguage(
				new ArrayList<>(porHash.keySet()), idiomaDestino)) {

			String original = porHash.get(guardada.getSourceHash());

			if (original != null) {
				resultado.put(original, guardada.getTranslatedText());
			}
		}

		return resultado;
	}

	/**
	 * {@code NOT_SUPPORTED} porque dentro hay llamadas por internet, y no se
	 * mantiene abierta una transacción a través de un sistema externo (regla del
	 * proyecto desde la Fase 8.5).
	 *
	 * <p>
	 * Cada traducción se guarda en su propia transacción, en cuanto llega: si a la
	 * quinta se agota la cuota o se cae la red, las cuatro primeras ya están en la
	 * caché y no se pierden. Con una transacción única, un fallo al final tiraría
	 * todo el trabajo — y todas las llamadas que ya se han gastado de la cuota.
	 */
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public int precalentar(List<String> textos, String idiomaOrigen, String idiomaDestino) {

		if (idiomaOrigen.equals(idiomaDestino)) {
			return 0;
		}

		int guardadas = 0;

		for (String texto : textos) {

			if (noHayNadaQueTraducir(texto, idiomaDestino)) {
				continue;
			}

			String clave = hash(texto);

			if (translationDao.findBySourceHashAndTargetLanguage(clave, idiomaDestino).isPresent()) {
				continue;
			}

			try {
				String traducido = translationClient.traducir(texto, idiomaOrigen, idiomaDestino);

				if (traducido != null && !traducido.equals(texto)) {
					translationDao.save(new Translation(clave, idiomaDestino, recortar(traducido)));
					guardadas++;
				}

			} catch (TranslationFailedException ex) {
				// Se para en el primer fallo en lugar de seguir intentando el resto. Si la
				// causa es que se ha agotado la cuota o no hay red -las dos más probables-
				// los siguientes van a fallar igual, y seguir solo sirve para tardar más.
				break;
			}
		}

		return guardadas;
	}

	private boolean noHayNadaQueTraducir(String texto, String idiomaDestino) {

		return texto == null || texto.trim().isEmpty() || IDIOMA_DEL_CONTENIDO.equals(idiomaDestino);
	}

	/** La columna admite 1.000 caracteres; una traducción más larga se recorta antes de guardarla. */
	private String recortar(String texto) {
		return texto.length() > 1000 ? texto.substring(0, 1000) : texto;
	}

	/**
	 * SHA-256 del texto, en hexadecimal.
	 *
	 * <p>
	 * No es criptografía: aquí un hash es solo una clave de longitud fija para un
	 * texto de longitud variable. Se usa SHA-256 y no {@code String.hashCode()}
	 * porque este último son 32 bits y las colisiones aparecen con unos pocos miles
	 * de valores — y una colisión aquí significa <b>enseñar la traducción de otro
	 * texto</b>, que es un fallo silencioso y desconcertante.
	 */
	private String hash(String texto) {

		try {
			byte[] resumen = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexadecimal = new StringBuilder(resumen.length * 2);

			for (byte b : resumen) {
				hexadecimal.append(String.format("%02x", b));
			}

			return hexadecimal.toString();

		} catch (NoSuchAlgorithmException ex) {
			// SHA-256 es obligatorio en toda implementación de Java desde hace décadas.
			// Si faltara, no es un caso que se pueda manejar: es una JVM rota.
			throw new IllegalStateException("SHA-256 no disponible", ex);
		}
	}
}
