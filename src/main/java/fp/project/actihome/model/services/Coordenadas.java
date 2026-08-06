package fp.project.actihome.model.services;

/**
 * Un punto del mapa, con el nombre que el buscador le dio (F17).
 *
 * <p>
 * <b>Lleva {@code etiqueta} y no solo los dos números</b>, y esa es la parte
 * pensada. Buscar "Granada" devuelve un sitio en España y otro en Nicaragua, y
 * un formulario que conteste "localizado ✓" sin decir <em>cuál</em> deja al
 * propietario publicando su casa a ocho mil kilómetros sin enterarse. La
 * etiqueta —"Granada, Andalucía, España"— es lo que convierte un resultado en
 * una confirmación.
 *
 * <p>
 * Es un {@code record}: tres valores que viajan juntos y no cambian. Ver la nota
 * de {@link PrevisionDiaria} sobre por qué eso no lo hace apto para JPA.
 */
public record Coordenadas(double latitud, double longitud, String etiqueta) {

}
