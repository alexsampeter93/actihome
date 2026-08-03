package fp.project.actihome.model.entities;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Un alojamiento del catálogo.
 *
 * <p>
 * <b>Campos añadidos en la Fase 3a</b>, todos exigidos por el diseño del
 * catálogo:
 *
 * <ul>
 * <li>{@code name} — el nombre comercial ("Casa Rural El Pinar"). Antes no
 * existía y las fichas se titulaban con el tipo, así que cinco alojamientos
 * distintos se llamaban todos "Casa en la playa". El diseño usa nombre y tipo
 * como dos cosas distintas: el nombre titula la ficha, el tipo es la categoría
 * por la que se filtra.</li>
 * <li>{@code image} — nombre del archivo de foto dentro de los recursos. Nulo
 * mientras no haya foto: en ese caso la ficha pinta el marcador tintado por
 * estación que ya define el sistema de diseño.</li>
 * <li>Las seis comodidades ({@code pool}, {@code wifi}, {@code tv},
 * {@code parking}, {@code airConditioning}, {@code pets}), que son los chips de
 * filtro del catálogo. La séptima del diseño, "Desayuno", reutiliza el campo
 * {@code breakfast} que ya existía.</li>
 * </ul>
 *
 * <p>
 * <b>Ya no tiene campo {@code available} (Fase 7.5).</b> Era un booleano que se
 * ponía a {@code false} al reservar y nunca volvía a {@code true} (bug B5): un
 * alojamiento reservado quedaba bloqueado para siempre, sin importar lo lejana
 * que fuera la fecha. La disponibilidad ahora se calcula, no se guarda —ver
 * {@code ReservationDao.existsOverlappingReservation} y
 * {@code HousingService.isAvailableNow}—, así que un alojamiento reservado para
 * el mes que viene sigue disponible hoy.
 *
 * <p>
 * <b>Sobre las anotaciones.</b> Como el resto de entidades del proyecto, se
 * anotan los <i>getters</i> y no los campos (acceso por propiedad), y no se usa
 * {@code @Column}: la estrategia de nombres estándar mapea cada propiedad
 * camelCase sobre la columna del mismo nombre en {@code schema.sql}.
 */
@Entity
@Table(name = "HOUSINGS")
public class Housing {

	private Long id;

	private Long housingCode;

	private String name;

	private String type;

	private int numberOfRooms;

	private BigDecimal pricePerNight;

	private String description;

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

	private Double score;

	private String location;

	private User owner;

	/**
	 * Constructor sin argumentos, obligatorio para JPA.
	 *
	 * <p>
	 * Los dos constructores largos que había antes (once y trece argumentos
	 * posicionales, con tres booleanos seguidos) se han retirado: eran la puerta de
	 * entrada del bug B9 y con las comodidades nuevas habrían llegado a nueve
	 * booleanos consecutivos. Un alojamiento se construye ahora desde
	 * {@code HousingData}, donde cada valor va acompañado de su nombre.
	 */
	public Housing() {

	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getHousingCode() {
		return housingCode;
	}

	public void setHousingCode(Long housingCode) {
		this.housingCode = housingCode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getNumberOfRooms() {
		return numberOfRooms;
	}

	public void setNumberOfRooms(int numberOfRooms) {
		this.numberOfRooms = numberOfRooms;
	}

	public BigDecimal getPricePerNight() {
		return pricePerNight;
	}

	public void setPricePerNight(BigDecimal pricePerNight) {
		this.pricePerNight = pricePerNight;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public boolean isBreakfast() {
		return breakfast;
	}

	public void setBreakfast(boolean breakfast) {
		this.breakfast = breakfast;
	}

	public boolean isLunch() {
		return lunch;
	}

	public void setLunch(boolean lunch) {
		this.lunch = lunch;
	}

	public boolean isDinner() {
		return dinner;
	}

	public void setDinner(boolean dinner) {
		this.dinner = dinner;
	}

	public boolean isPool() {
		return pool;
	}

	public void setPool(boolean pool) {
		this.pool = pool;
	}

	public boolean isWifi() {
		return wifi;
	}

	public void setWifi(boolean wifi) {
		this.wifi = wifi;
	}

	public boolean isTv() {
		return tv;
	}

	public void setTv(boolean tv) {
		this.tv = tv;
	}

	public boolean isParking() {
		return parking;
	}

	public void setParking(boolean parking) {
		this.parking = parking;
	}

	public boolean isAirConditioning() {
		return airConditioning;
	}

	public void setAirConditioning(boolean airConditioning) {
		this.airConditioning = airConditioning;
	}

	public boolean isPets() {
		return pets;
	}

	public void setPets(boolean pets) {
		this.pets = pets;
	}

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "ownerId")
	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	@Override
	public String toString() {
		return "Housing [id=" + id + ", housingCode=" + housingCode + ", name=" + name + ", type=" + type
				+ ", numberOfRooms=" + numberOfRooms + ", pricePerNight=" + pricePerNight + ", location=" + location
				+ ", owner=" + owner + "]";
	}

}
