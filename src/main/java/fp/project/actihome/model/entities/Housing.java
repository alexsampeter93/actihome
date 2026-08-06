package fp.project.actihome.model.entities;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

	private User.EstacionPreferida idealSeason;

	private boolean openToExchange;

	private String exchangeWanted;

	private Double score;

	private String location;

	private Double latitude;

	private Double longitude;

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

	/**
	 * En qué estación luce más este alojamiento, o {@code null} si no lo declara.
	 *
	 * <p>
	 * Alimenta el distintivo "Ideal en {estación}" del catálogo (Fase 8.4), que
	 * solo aparece cuando coincide con la estación activa. Es lo que convierte el
	 * selector de estación en algo que <em>cambia lo que ves</em> y no solo los
	 * colores: en invierno destacan las casas de montaña y en verano las de playa.
	 *
	 * <p>
	 * <b>Admite nulo a propósito.</b> Un alojamiento publicado desde la aplicación
	 * no tiene por qué declarar estación, y entonces sencillamente no lleva
	 * distintivo. Obligar a elegir una convertiría un matiz editorial en un
	 * trámite, y llenaría el catálogo de etiquetas puestas al azar — que es peor
	 * que no tenerlas, porque dejarían de significar nada.
	 *
	 * <p>
	 * Se persiste como <b>texto</b> ({@code EnumType.STRING}) y no por ordinal,
	 * como el resto de enumerados del proyecto: reordenar o insertar un valor en
	 * el enum no debe convertir en silencio una casa de playa en una de montaña.
	 */
	@Enumerated(EnumType.STRING)
	public User.EstacionPreferida getIdealSeason() {
		return idealSeason;
	}

	public void setIdealSeason(User.EstacionPreferida idealSeason) {
		this.idealSeason = idealSeason;
	}

	/**
	 * Si el propietario acepta permutar este alojamiento (Fase 8.4).
	 *
	 * <p>
	 * <b>Es una oferta pública, no una propuesta entre dos personas</b>, y por eso
	 * vive aquí y no en una tabla de propuestas con remitente y destinatario. La
	 * diferencia no es de implementación: una propuesta necesita un flujo de
	 * aceptar y rechazar, estados intermedios y avisos; una oferta abierta es un
	 * tablón de anuncios, y es exactamente lo que la pantalla de intercambio
	 * necesita para dejar de estar vacía. El intercambio en sí ya existía y sigue
	 * siendo inmediato.
	 */
	public boolean isOpenToExchange() {
		return openToExchange;
	}

	public void setOpenToExchange(boolean openToExchange) {
		this.openToExchange = openToExchange;
	}

	/**
	 * Qué busca a cambio, en una línea ("una casa rural para agosto"), o
	 * {@code null} si no lo concreta.
	 *
	 * <p>
	 * Es texto libre y no una lista de criterios a propósito. Un buscador de
	 * intercambios con filtros de tipo, zona y fechas sería un producto entero; lo
	 * que hace falta aquí es que una persona pueda decir en sus palabras qué le
	 * interesa, que es como se habla de esto de verdad.
	 */
	public String getExchangeWanted() {
		return exchangeWanted;
	}

	public void setExchangeWanted(String exchangeWanted) {
		this.exchangeWanted = exchangeWanted;
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

	/**
	 * Latitud en grados decimales, o {@code null} si el alojamiento no está
	 * localizado (F17).
	 *
	 * <p>
	 * <b>Por qué hacen falta dos números si ya hay un campo {@code location}.</b>
	 * Porque {@code location} es texto para leer —"Sierra Nevada, Granada"— y no
	 * hay forma de preguntarle a un servicio meteorológico o a un mapa por una
	 * cadena así. Los dos campos no compiten: el texto es lo que se enseña y las
	 * coordenadas son lo que se consulta. Cambiar el texto por unas coordenadas
	 * habría sido peor en las dos direcciones.
	 *
	 * <p>
	 * <b>Admite nulo, y esa es la decisión importante.</b> Un alojamiento sin
	 * coordenadas es un caso normal, no un error: los diez de ejemplo las traen
	 * sembradas, pero uno publicado desde la aplicación solo las tiene si su
	 * propietario pulsó "Localizar". Todo lo que dependa de ellas —hoy la
	 * previsión meteorológica, mañana el mapa— tiene que saber desaparecer sin
	 * ruido cuando faltan. Exigirlas habría convertido publicar un alojamiento en
	 * un trámite que además depende de que haya red en ese momento.
	 *
	 * <p>
	 * Se guardan como {@code Double} y no como {@code BigDecimal}, a diferencia
	 * del precio: aquí no hay dinero que cuadrar y el error de redondeo de un
	 * {@code double} en la sexta cifra decimal son centímetros sobre el terreno.
	 */
	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	/** Longitud en grados decimales, o {@code null}. Ver {@link #getLatitude()}. */
	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	/**
	 * Si tiene las dos coordenadas y por tanto se puede consultar por él.
	 *
	 * <p>
	 * Está aquí y no repetido en cada pantalla porque <b>una sola de las dos no
	 * sirve para nada</b>: media coordenada no localiza un sitio. Que la pregunta
	 * viva en la entidad evita que alguien compruebe solo la latitud y acabe
	 * enviando un {@code null} a la petición.
	 */
	public boolean estaLocalizado() {
		return latitude != null && longitude != null;
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
