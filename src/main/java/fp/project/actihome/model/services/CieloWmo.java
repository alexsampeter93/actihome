package fp.project.actihome.model.services;

/**
 * El estado del cielo, agrupado en seis categorías que se pueden enseñar (F18).
 *
 * <p>
 * <b>Por qué existe esta traducción.</b> Open-Meteo no devuelve "nublado":
 * devuelve un número entre 0 y 99, el <i>código WMO 4677</i>, un estándar de la
 * Organización Meteorológica Mundial que distingue hasta veintiocho situaciones
 * —llovizna ligera, llovizna moderada, llovizna densa, llovizna helada ligera…—.
 * Ese detalle es correcto para un meteorólogo y ruido para alguien que está
 * decidiendo si reserva una cabaña.
 *
 * <p>
 * Así que se agrupa. Seis categorías es lo que distingue una decisión de viaje:
 * si hará sol, si estará tapado, si lloverá, si nevará, si habrá tormenta o si
 * habrá niebla. <b>Agrupar es una decisión de producto, no una simplificación
 * perezosa</b>: enseñar veintiocho estados obligaría a veintiocho textos
 * traducidos a dos idiomas y a veintiocho iconos, para que el usuario acabara
 * leyendo igualmente "va a llover".
 *
 * <p>
 * <b>El caso por defecto no es decorativo.</b> La tabla WMO tiene huecos y el
 * proveedor puede empezar a usar un código nuevo cualquier día sin avisarnos.
 * Devolver {@link #DESPEJADO} ante lo desconocido sería inventarse el tiempo, así
 * que existe {@link #DESCONOCIDO}: la ficha lo trata como "sin dato" y no pinta
 * nada, que es lo único honesto.
 */
public enum CieloWmo {

	DESPEJADO,
	NUBLADO,
	NIEBLA,
	LLUVIA,
	NIEVE,
	TORMENTA,
	DESCONOCIDO;

	/**
	 * Traduce un código WMO 4677 a su categoría.
	 *
	 * <p>
	 * Los rangos salen de la tabla que publica el propio Open-Meteo en su
	 * documentación. Se escriben como {@code switch} sobre valores sueltos y no
	 * como comparaciones de rango a propósito: la tabla <b>no es continua</b> —el
	 * 45 y el 48 son niebla, el 51 ya es llovizna— y un {@code if (codigo < 50)}
	 * daría por buena cualquier cifra intermedia que la tabla no define.
	 */
	public static CieloWmo de(int codigo) {

		switch (codigo) {

		case 0:
		case 1:
			return DESPEJADO;

		case 2:
		case 3:
			return NUBLADO;

		case 45:
		case 48:
			return NIEBLA;

		// Llovizna (51-57), lluvia (61-67) y chubascos (80-82) acaban en la misma
		// categoría: para decidir un viaje, lo que importa es que cae agua.
		case 51:
		case 53:
		case 55:
		case 56:
		case 57:
		case 61:
		case 63:
		case 65:
		case 66:
		case 67:
		case 80:
		case 81:
		case 82:
			return LLUVIA;

		case 71:
		case 73:
		case 75:
		case 77:
		case 85:
		case 86:
			return NIEVE;

		case 95:
		case 96:
		case 99:
			return TORMENTA;

		default:
			return DESCONOCIDO;
		}
	}
}
