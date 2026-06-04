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

@Entity
@Table(name = "HOUSINGS")
public class Housing {

	private Long id;

	private Long housingCode;

	private String type;

	private int numberOfRooms;

	private BigDecimal pricePerNight;

	private String description;

	private boolean breakfast;

	private boolean lunch;

	private boolean dinner;

	private Double score;

	private boolean available;

	private String location;

	private User owner;

	public Housing() {

	}

	public Housing(Long id, Long housingCode, String type, int numberOfRooms, BigDecimal pricePerNight,
			String description, boolean breakfast, boolean lunch, boolean dinner, Double score, boolean available,
			String location, User owner) {
		super();
		this.id = id;
		this.housingCode = housingCode;
		this.type = type;
		this.numberOfRooms = numberOfRooms;
		this.pricePerNight = pricePerNight;
		this.description = description;
		this.breakfast = breakfast;
		this.lunch = lunch;
		this.dinner = dinner;
		this.score = score;
		this.available = available;
		this.location = location;
		this.owner = owner;
	}

	public Housing(Long housingCode, String type, int numberOfRooms, BigDecimal pricePerNight, String description,
			boolean breakfast, boolean lunch, boolean dinner, boolean available, String location, User owner) {
		super();
		this.housingCode = housingCode;
		this.type = type;
		this.numberOfRooms = numberOfRooms;
		this.pricePerNight = pricePerNight;
		this.description = description;
		this.breakfast = breakfast;
		this.lunch = lunch;
		this.dinner = dinner;
		this.available = available;
		this.location = location;
		this.owner = owner;
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

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
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
		return "Housing [id=" + id + ", housingCode=" + housingCode + ", type=" + type + ", numberOfRooms="
				+ numberOfRooms + ", pricePerNight=" + pricePerNight + ", description=" + description + ", location="
				+ location + ", owner=" + owner + "]";
	}

}