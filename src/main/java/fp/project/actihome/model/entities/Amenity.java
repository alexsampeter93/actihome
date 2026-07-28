package fp.project.actihome.model.entities;

import java.util.EnumSet;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Las comodidades que puede ofrecer un alojamiento.
 *
 * <p>
 * <b>Por qué un enum y no siete booleanos sueltos por ahí.</b> El catálogo
 * necesita recorrer las comodidades tres veces: para pintar la fila de chips de
 * filtro, para pintar los chips de cada ficha y para aplicar el filtro. Sin este
 * enum, cada uno de esos tres sitios tendría su propia lista escrita a mano —y
 * en cuanto se añadiera una comodidad nueva habría que acordarse de los tres.
 * Con el enum, añadir una comodidad es añadir una constante aquí.
 *
 * <p>
 * <b>Cada constante sabe leerse y escribirse sola.</b> En lugar de un
 * {@code switch} gigante repartido por la aplicación, cada valor lleva dentro la
 * función que consulta su campo en {@link Housing} y la que lo escribe. Es el
 * patrón de "enum con comportamiento": el enum no es solo una etiqueta, es la
 * pieza que sabe hacer su trabajo.
 *
 * <p>
 * <b>Nota sobre {@link #BREAKFAST}.</b> El desayuno ya existía en el modelo como
 * parte de la pensión (desayuno / comida / cena) y el diseño lo muestra además
 * como comodidad filtrable. No se duplica el dato: esta constante lee y escribe
 * exactamente el mismo campo {@code breakfast}. Un dato, dos lecturas.
 */
public enum Amenity {

	POOL("Piscina", Housing::isPool, Housing::setPool),

	WIFI("Wifi", Housing::isWifi, Housing::setWifi),

	TV("TV", Housing::isTv, Housing::setTv),

	PARKING("Parking", Housing::isParking, Housing::setParking),

	AIR_CONDITIONING("Aire ac.", Housing::isAirConditioning, Housing::setAirConditioning),

	BREAKFAST("Desayuno", Housing::isBreakfast, Housing::setBreakfast),

	PETS("Mascotas", Housing::isPets, Housing::setPets);

	private final String etiqueta;
	private final Predicate<Housing> lectura;
	private final BiConsumer<Housing, Boolean> escritura;

	Amenity(String etiqueta, Predicate<Housing> lectura, BiConsumer<Housing, Boolean> escritura) {

		this.etiqueta = etiqueta;
		this.lectura = lectura;
		this.escritura = escritura;
	}

	/** Texto que ve el usuario, tal cual aparece en el diseño. */
	public String etiqueta() {
		return etiqueta;
	}

	/** Si este alojamiento ofrece la comodidad. */
	public boolean presenteEn(Housing housing) {
		return lectura.test(housing);
	}

	/** Activa o desactiva la comodidad en el alojamiento. */
	public void aplicarA(Housing housing, boolean valor) {
		escritura.accept(housing, valor);
	}

	/** Las comodidades que ofrece un alojamiento, en el orden de declaración. */
	public static EnumSet<Amenity> de(Housing housing) {

		EnumSet<Amenity> presentes = EnumSet.noneOf(Amenity.class);

		for (Amenity amenity : values()) {
			if (amenity.presenteEn(housing)) {
				presentes.add(amenity);
			}
		}

		return presentes;
	}
}
