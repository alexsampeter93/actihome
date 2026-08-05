package fp.project.actihome.model.entities;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "REVIEWS")
public class Review {

	private Long id;

	private String title;

	private String body;

	private double locationScore;

	private double serviceScore;

	private double wifiScore;

	private double foodScore;

	private double cleaningScore;
	
	private double totalScore;

	private LocalDateTime publicationDate;

	private User author;

	private Housing housing;

	private String image;

	private String ownerResponse;

	private LocalDateTime ownerResponseDate;

	public Review() {

	}

	public Review(Long id, String title, String body, double locationScore, double serviceScore, double wifiScore,
			double foodScore, double cleaningScore, double totalScore, LocalDateTime publicationDate, User author, Housing housing) {
		super();
		this.id = id;
		this.title = title;
		this.body = body;
		this.locationScore = locationScore;
		this.serviceScore = serviceScore;
		this.wifiScore = wifiScore;
		this.foodScore = foodScore;
		this.cleaningScore = cleaningScore;
		this.totalScore = totalScore;
		this.publicationDate = publicationDate;
		this.author = author;
		this.housing = housing;
	}

	public Review(String title, String body, double locationScore, double serviceScore, double wifiScore,
			double foodScore, double cleaningScore, double totalScore, LocalDateTime publicationDate, User author, Housing housing) {
		super();
		this.title = title;
		this.body = body;
		this.locationScore = locationScore;
		this.serviceScore = serviceScore;
		this.wifiScore = wifiScore;
		this.foodScore = foodScore;
		this.cleaningScore = cleaningScore;
		this.totalScore = totalScore;
		this.publicationDate = publicationDate;
		this.author = author;
		this.housing = housing;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public double getLocationScore() {
		return locationScore;
	}

	public void setLocationScore(double locationScore) {
		this.locationScore = locationScore;
	}

	public double getServiceScore() {
		return serviceScore;
	}

	public void setServiceScore(double serviceScore) {
		this.serviceScore = serviceScore;
	}

	public double getWifiScore() {
		return wifiScore;
	}

	public void setWifiScore(double wifiScore) {
		this.wifiScore = wifiScore;
	}

	public double getFoodScore() {
		return foodScore;
	}

	public void setFoodScore(double foodScore) {
		this.foodScore = foodScore;
	}

	public double getCleaningScore() {
		return cleaningScore;
	}

	public void setCleaningScore(double cleaningScore) {
		this.cleaningScore = cleaningScore;
	}

	public double getTotalScore() {
		return totalScore;
	}

	public void setTotalScore(double totalScore) {
		this.totalScore = totalScore;
	}

	public LocalDateTime getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(LocalDateTime publicationDate) {
		this.publicationDate = publicationDate;
	}

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "authorId")
	public User getAuthor() {
		return author;
	}

	public void setAuthor(User author) {
		this.author = author;
	}

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "housingId")
	public Housing getHousing() {
		return housing;
	}

	public void setHousing(Housing housing) {
		this.housing = housing;
	}

	/** Nombre del archivo de foto dentro de {@code /images/reviews/}, o {@code null} sin foto adjunta (F15). */
	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	/** Respuesta pública del propietario del alojamiento, o {@code null} si no ha respondido (F15). */
	public String getOwnerResponse() {
		return ownerResponse;
	}

	public void setOwnerResponse(String ownerResponse) {
		this.ownerResponse = ownerResponse;
	}

	public LocalDateTime getOwnerResponseDate() {
		return ownerResponseDate;
	}

	public void setOwnerResponseDate(LocalDateTime ownerResponseDate) {
		this.ownerResponseDate = ownerResponseDate;
	}

	@Override
	public String toString() {
		return "Review [id=" + id + ", title=" + title + ", body=" + body + ", locationScore=" + locationScore
				+ ", serviceScore=" + serviceScore + ", wifiScore=" + wifiScore + ", foodScore=" + foodScore
				+ ", cleaningScore=" + cleaningScore + ", publicationDate=" + publicationDate + ", author=" + author
				+ ", housing=" + housing + ", image=" + image + ", ownerResponse=" + ownerResponse
				+ ", ownerResponseDate=" + ownerResponseDate + "]";
	}

}