package fp.project.actihome.model.services;

import java.time.LocalDate;

/**
 * El tiempo previsto para un día concreto en un sitio concreto (F18).
 *
 * <p>
 * <b>Es un {@code record} y no una clase con quince líneas de getters</b>, y esa
 * es la primera vez que el proyecto aprovecha algo que Java 17 trae y Java 11 no
 * (la migración fue la Fase 8.9). Un {@code record} declara datos inmutables: el
 * compilador escribe el constructor, los accesores, {@code equals},
 * {@code hashCode} y {@code toString}. Aquí encaja porque esto <em>es</em>
 * exactamente eso —cuatro valores que viajan juntos y no cambian nunca— y no una
 * entidad con identidad y ciclo de vida.
 *
 * <p>
 * Ojo con una diferencia de estilo: los accesores de un {@code record} se llaman
 * {@code fecha()}, no {@code getFecha()}. Es intencionado en el lenguaje —marca
 * que el objeto es un dato y no un bean— y por eso este tipo <b>no</b> puede
 * usarse como entidad JPA, que exige el convenio de los beans. No hace falta: la
 * previsión no se guarda en la base de datos, se pide y se olvida.
 *
 * @param fecha       el día al que se refiere
 * @param maxima      temperatura máxima en grados Celsius
 * @param minima      temperatura mínima en grados Celsius
 * @param codigo      código WMO del estado del cielo; ver {@link CieloWmo}
 */
public record PrevisionDiaria(LocalDate fecha, double maxima, double minima, int codigo) {

	/** El estado del cielo ya interpretado, listo para enseñar. */
	public CieloWmo cielo() {
		return CieloWmo.de(codigo);
	}
}
