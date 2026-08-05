package fp.project.actihome.ui.catalog;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;

/**
 * Decide si un alojamiento lleva el distintivo "Ideal en {estación}" (Fase 8.4).
 *
 * <p>
 * <b>El distintivo solo aparece cuando la estación del alojamiento coincide con
 * la activa</b>, y esa condición es la idea entera. Si se enseñara siempre
 * —"Ideal en invierno" en pleno agosto— sería un dato más de la ficha, y de los
 * menos útiles. Mostrándolo solo cuando coincide, cambiar de estación deja de
 * ser un cambio de colores y pasa a <em>reordenar visualmente el catálogo</em>:
 * en invierno destacan las casas de montaña, en verano las de playa.
 *
 * <p>
 * Vive aparte de {@code HousingCard} y {@code HousingRow} porque las dos vistas
 * del catálogo tienen que decidir exactamente lo mismo. Duplicar la condición
 * en ambas sería la forma habitual de que un día la lista y la cuadrícula
 * dejaran de coincidir.
 *
 * <p>
 * Un alojamiento sin estación declarada nunca lleva distintivo: ver
 * {@link Housing#getIdealSeason()} sobre por qué el campo admite nulo.
 */
public final class Destacado {

	private Destacado() {
	}

	/**
	 * El texto del distintivo, o {@code null} si este alojamiento no lo lleva
	 * ahora mismo.
	 *
	 * <p>
	 * Devuelve {@code null} en lugar de una cadena vacía porque es justo lo que
	 * {@code ImagePlaceholder.setDestacado} espera para no pintar nada: así la
	 * ausencia se propaga sola y ninguna pantalla necesita un {@code if}.
	 */
	public static String de(Housing housing) {

		if (housing.getIdealSeason() == null) {
			return null;
		}

		// Los dos enumerados se comparan por nombre y no por identidad porque son
		// tipos distintos a propósito: User.EstacionPreferida es un valor de negocio
		// que se guarda en la base de datos, y Season es una paleta de colores del
		// sistema de diseño. Que hoy tengan los mismos cuatro valores no los convierte
		// en el mismo concepto, y hacer que la capa de modelo dependiera de la de
		// interfaz para ahorrarse esta línea rompería la regla de dependencia del
		// proyecto.
		if (!housing.getIdealSeason().name().equals(Theme.estacion().name())) {
			return null;
		}

		return Textos.t("catalogo.idealEn", Theme.estacion().nombre().toLowerCase());
	}
}
