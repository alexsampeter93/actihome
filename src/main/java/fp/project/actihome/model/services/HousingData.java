package fp.project.actihome.model.services;

import fp.project.actihome.model.entities.User;
import java.math.BigDecimal;
import java.util.EnumSet;

import fp.project.actihome.model.entities.Amenity;

/**
 * Los datos editables de un alojamiento, viajando juntos en un solo objeto.
 *
 * <p>
 * <b>El problema que resuelve.</b> Hasta ahora, dar de alta un alojamiento era
 * llamar a un método con diez argumentos posicionales, tres de ellos booleanos
 * seguidos:
 *
 * <pre>
 * uploadHousing(codigo, tipo, habitaciones, precio, descripcion, true, false, true, ubicacion, propietarioId)
 * </pre>
 *
 * Al leer esa llamada no hay forma de saber qué significan ese {@code true,
 * false, true} sin ir a mirar la firma. Y al añadir las seis comodidades que
 * pide el diseño serían <b>nueve booleanos consecutivos</b>: un campo de minas.
 * No es una preocupación teórica —el bug B9 del proyecto es exactamente eso, un
 * formulario que enviaba {@code (…, true, true, true)} y activaba las tres
 * comidas cada vez que alguien editaba el precio.
 *
 * <p>
 * <b>La solución.</b> Un objeto de datos con un método por propiedad. La misma
 * llamada pasa a ser imposible de confundir:
 *
 * <pre>
 * HousingData datos = HousingData.basico(codigo, "Casa Rural El Pinar", "Casa", 3, precio, "Granada")
 *         .breakfast(true)
 *         .dinner(true)
 *         .amenities(Amenity.WIFI, Amenity.PARKING);
 * </pre>
 *
 * Cada valor va pegado a su nombre. Añadir un campo nuevo mañana no obliga a
 * tocar ninguna llamada existente, porque no hay posiciones que se desplacen.
 *
 * <p>
 * <b>Por qué los métodos devuelven {@code this}.</b> Es el patrón <i>fluent</i>:
 * cada llamada devuelve el propio objeto, así que se pueden encadenar. Lo único
 * que aporta es legibilidad —una expresión en lugar de ocho sentencias—, pero en
 * un objeto con quince campos opcionales esa legibilidad es justamente el punto.
 *
 * <p>
 * Este objeto <b>no</b> es una entidad: no se guarda en la base de datos, no
 * tiene identificador y no lo gestiona JPA. Es un mensaje de la interfaz hacia
 * el servicio, y por eso vive en el paquete de servicios y no en el de
 * entidades.
 */
public class HousingData {

	private Long housingCode;

	private String name;

	private String type;

	private int numberOfRooms;

	private BigDecimal pricePerNight;

	private String description = "";

	private String location;

	private String image;

	private boolean breakfast;

	private boolean lunch;

	private boolean dinner;

	private boolean pool;

	private boolean wifi;

	private boolean tv;

	private boolean parking;

	private boolean airConditioning;

	private boolean pets;

	private User.EstacionPreferida idealSeason;

	private boolean openToExchange;

	private String exchangeWanted;

	public HousingData() {

	}

	/**
	 * Atajo para los campos que siempre hacen falta.
	 *
	 * <p>
	 * El resto —descripción, comidas, comodidades, imagen— son opcionales y se
	 * encadenan solo cuando se necesitan. Así una llamada mínima sigue siendo corta
	 * y una completa sigue siendo legible.
	 */
	public static HousingData basico(Long housingCode, String name, String type, int numberOfRooms,
			BigDecimal pricePerNight, String location) {

		return new HousingData().housingCode(housingCode).name(name).type(type).numberOfRooms(numberOfRooms)
				.pricePerNight(pricePerNight).location(location);
	}

	/**
	 * Activa las comodidades indicadas y desactiva todas las demás.
	 *
	 * <p>
	 * Recibe las que están presentes en lugar de un booleano por cada una: una
	 * lista de lo que hay se lee mucho mejor que seis banderas de las que cuatro son
	 * {@code false}. {@link Amenity#BREAKFAST} escribe el mismo campo que
	 * {@link #breakfast(boolean)}, así que conviene usar uno de los dos y no ambos.
	 */
	public HousingData amenities(Amenity... presentes) {

		EnumSet<Amenity> activas = EnumSet.noneOf(Amenity.class);

		for (Amenity amenity : presentes) {
			activas.add(amenity);
		}

		this.pool = activas.contains(Amenity.POOL);
		this.wifi = activas.contains(Amenity.WIFI);
		this.tv = activas.contains(Amenity.TV);
		this.parking = activas.contains(Amenity.PARKING);
		this.airConditioning = activas.contains(Amenity.AIR_CONDITIONING);
		this.pets = activas.contains(Amenity.PETS);

		if (activas.contains(Amenity.BREAKFAST)) {
			this.breakfast = true;
		}

		return this;
	}

	/**
	 * Activa o desactiva una comodidad concreta.
	 *
	 * <p>
	 * Lo necesita cualquier pantalla que recorra {@link Amenity#values()} para
	 * pintar una casilla por comodidad: sin esto, cada formulario tendría que
	 * repetir el mismo {@code switch} para traducir la casilla al campo. Aquí está
	 * escrito una sola vez, y el compilador avisa si algún día se añade una
	 * comodidad y se olvida este método.
	 */
	public HousingData amenity(Amenity amenity, boolean valor) {

		switch (amenity) {
		case POOL:
			return pool(valor);
		case WIFI:
			return wifi(valor);
		case TV:
			return tv(valor);
		case PARKING:
			return parking(valor);
		case AIR_CONDITIONING:
			return airConditioning(valor);
		case PETS:
			return pets(valor);
		case BREAKFAST:
			return breakfast(valor);
		default:
			throw new IllegalArgumentException("Comodidad sin tratar: " + amenity);
		}
	}

	public HousingData housingCode(Long housingCode) {
		this.housingCode = housingCode;
		return this;
	}

	public HousingData name(String name) {
		this.name = name;
		return this;
	}

	public HousingData type(String type) {
		this.type = type;
		return this;
	}

	public HousingData numberOfRooms(int numberOfRooms) {
		this.numberOfRooms = numberOfRooms;
		return this;
	}

	public HousingData pricePerNight(BigDecimal pricePerNight) {
		this.pricePerNight = pricePerNight;
		return this;
	}

	public HousingData description(String description) {
		this.description = description;
		return this;
	}

	public HousingData location(String location) {
		this.location = location;
		return this;
	}

	public HousingData image(String image) {
		this.image = image;
		return this;
	}

	public HousingData breakfast(boolean breakfast) {
		this.breakfast = breakfast;
		return this;
	}

	public HousingData lunch(boolean lunch) {
		this.lunch = lunch;
		return this;
	}

	public HousingData dinner(boolean dinner) {
		this.dinner = dinner;
		return this;
	}

	public HousingData pool(boolean pool) {
		this.pool = pool;
		return this;
	}

	public HousingData wifi(boolean wifi) {
		this.wifi = wifi;
		return this;
	}

	public HousingData tv(boolean tv) {
		this.tv = tv;
		return this;
	}

	public HousingData parking(boolean parking) {
		this.parking = parking;
		return this;
	}

	public HousingData airConditioning(boolean airConditioning) {
		this.airConditioning = airConditioning;
		return this;
	}

	public HousingData pets(boolean pets) {
		this.pets = pets;
		return this;
	}

	/**
	 * En qué estación luce más el alojamiento. Admite {@code null}: no declararla
	 * es una respuesta válida y significa que la ficha no lleva distintivo.
	 */
	public HousingData idealSeason(User.EstacionPreferida idealSeason) {
		this.idealSeason = idealSeason;
		return this;
	}

	/** Si el propietario ofrece este alojamiento para permutar (Fase 8.4). */
	public HousingData openToExchange(boolean openToExchange) {
		this.openToExchange = openToExchange;
		return this;
	}

	/** Qué busca a cambio, en una línea. Admite {@code null}. */
	public HousingData exchangeWanted(String exchangeWanted) {
		this.exchangeWanted = exchangeWanted;
		return this;
	}

	public boolean isOpenToExchange() {
		return openToExchange;
	}

	public String getExchangeWanted() {
		return exchangeWanted;
	}

	public Long getHousingCode() {
		return housingCode;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public int getNumberOfRooms() {
		return numberOfRooms;
	}

	public BigDecimal getPricePerNight() {
		return pricePerNight;
	}

	public String getDescription() {
		return description;
	}

	public String getLocation() {
		return location;
	}

	public String getImage() {
		return image;
	}

	public User.EstacionPreferida getIdealSeason() {
		return idealSeason;
	}

	public boolean isBreakfast() {
		return breakfast;
	}

	public boolean isLunch() {
		return lunch;
	}

	public boolean isDinner() {
		return dinner;
	}

	public boolean isPool() {
		return pool;
	}

	public boolean isWifi() {
		return wifi;
	}

	public boolean isTv() {
		return tv;
	}

	public boolean isParking() {
		return parking;
	}

	public boolean isAirConditioning() {
		return airConditioning;
	}

	public boolean isPets() {
		return pets;
	}
}
