package fp.project.actihome.model.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fp.project.actihome.model.exceptions.TranslationFailedException;

/**
 * Implementación de {@link TranslationService}.
 *
 * <p>
 * <b>Sin {@code @Transactional}</b>, al contrario que los otros cinco servicios
 * del proyecto. No es un olvido: aquí no se toca la base de datos, y abrir una
 * transacción para una llamada por internet es exactamente lo que esta clase
 * existe para no hacer.
 *
 * <p>
 * Se traducen título y cuerpo en dos peticiones y no concatenados en una: unir
 * los dos textos obligaría a volver a partirlos por alguna marca, y esa marca la
 * puede alterar —o traducir— el propio traductor. Dos llamadas cortas tardan más
 * y no se rompen.
 */
@Service
public class TranslationServiceImpl implements TranslationService {

	@Autowired
	private TranslationClient translationClient;

	@Override
	public TranslatedReview traducirResena(String titulo, String cuerpo, String desde, String hasta)
			throws TranslationFailedException {

		return new TranslatedReview(translationClient.traducir(titulo, desde, hasta),
				translationClient.traducir(cuerpo, desde, hasta));
	}
}
