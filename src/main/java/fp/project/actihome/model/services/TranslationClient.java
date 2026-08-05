package fp.project.actihome.model.services;

import fp.project.actihome.model.exceptions.TranslationFailedException;

/**
 * Traduce un texto suelto de un idioma a otro.
 *
 * <p>
 * <b>Es una interfaz y no una clase por un motivo concreto</b>, no por costumbre:
 * detrás hay una llamada por internet, y una llamada por internet no se puede
 * usar en un test. Con esta costura, {@code ReviewServiceTests} sustituye el
 * cliente por uno que devuelve lo que le apetece y comprueba lo que de verdad
 * le toca comprobar al servicio —que valida, que propaga el fallo, que no
 * inventa nada— sin depender de que haya red ni de que el proveedor esté de
 * pie.
 *
 * <p>
 * Es también el punto por el que se cambia de proveedor sin tocar nada más. La
 * Fase 7.7 dejó el hueco montado a propósito sin elegir uno; la decisión —
 * MyMemory— llegó en la 8.5 y vive entera en la implementación.
 */
public interface TranslationClient {

	/**
	 * @param texto  lo que se quiere traducir; si viene vacío se devuelve igual
	 * @param desde  código ISO del idioma de origen ("es")
	 * @param hasta  código ISO del idioma de destino ("en")
	 * @throws TranslationFailedException si no hay red, el proveedor no responde,
	 *                                    tarda demasiado o contesta con un error
	 */
	String traducir(String texto, String desde, String hasta) throws TranslationFailedException;
}
