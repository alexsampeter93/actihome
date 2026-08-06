package fp.project.actihome.model.exceptions;

/**
 * No se ha podido situar en el mapa el texto de ubicación escrito (F17).
 *
 * <p>
 * <b>Cubre dos situaciones distintas y eso es deliberado</b>: que el buscador no
 * conozca ese sitio, y que no se haya podido preguntar (sin red, el proveedor no
 * contesta, tarda demasiado). Distinguirlas tendría sentido si el usuario
 * pudiera hacer algo diferente en cada caso, y no puede: en los dos, lo que
 * ocurre es que el alojamiento se publica sin coordenadas, que es un estado
 * perfectamente válido.
 *
 * <p>
 * La consecuencia importante es que <b>esto no bloquea el alta</b>. Localizar es
 * una ayuda opcional; si falla, el formulario lo dice y sigue funcionando.
 * Convertirlo en un requisito habría hecho que publicar un alojamiento
 * dependiera de que hubiera internet en ese momento.
 */
@SuppressWarnings("serial")
public class LocationNotFoundException extends Exception {

}
