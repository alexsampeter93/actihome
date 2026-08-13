package fp.project.actihome.model.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "RESERVATIONS")
public class Reservation {

	private Long id;

	private Long reservationCode;

	private LocalDateTime checkIn;

	private LocalDateTime checkOut;

	/**
	 * Cuántos adultos viajan. Al menos uno: una reserva sin nadie no significa
	 * nada.
	 */
	private int numberOfAdults;

	/**
	 * Cuántos niños viajan, además de los adultos. Puede ser cero.
	 *
	 * <p>
	 * Los bebés no están aquí a propósito: no ocupan plaza en ningún sistema de
	 * reservas real, así que el buscador los pregunta por completitud pero no
	 * cuentan para el aforo del alojamiento ni se guardan en la reserva. Contar
	 * un dato que no se usa para nada sería peor que no preguntarlo.
	 */
	private int numberOfChildren;

	private String paymentMethod;

	private LocalDateTime reservationDate;

	private BigDecimal totalPrice;
	
	private boolean checkedIn;

	private boolean cancelled;

	private User customer;

	private Housing housing;

	public Reservation() {

	}

	public Reservation(Long id, Long reservationCode, LocalDateTime checkIn, LocalDateTime checkOut,
			int numberOfAdults, int numberOfChildren, String paymentMethod, LocalDateTime reservationDate,
			BigDecimal totalPrice, boolean checkedIn, User customer, Housing housing) {
		super();
		this.id = id;
		this.reservationCode = reservationCode;
		this.checkIn = checkIn;
		this.checkOut = checkOut;
		this.numberOfAdults = numberOfAdults;
		this.numberOfChildren = numberOfChildren;
		this.paymentMethod = paymentMethod;
		this.reservationDate = reservationDate;
		this.totalPrice = totalPrice;
		this.checkedIn = checkedIn;
		this.customer = customer;
		this.housing = housing;
	}

	public Reservation(Long reservationCode, LocalDateTime checkIn, LocalDateTime checkOut, int numberOfAdults,
			int numberOfChildren, String paymentMethod, LocalDateTime reservationDate, BigDecimal totalPrice,
			boolean checkedIn, User customer, Housing housing) {
		super();
		this.reservationCode = reservationCode;
		this.checkIn = checkIn;
		this.checkOut = checkOut;
		this.numberOfAdults = numberOfAdults;
		this.numberOfChildren = numberOfChildren;
		this.paymentMethod = paymentMethod;
		this.reservationDate = reservationDate;
		this.totalPrice = totalPrice;
		this.checkedIn = checkedIn;
		this.customer = customer;
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

	public Long getReservationCode() {
		return reservationCode;
	}

	public void setReservationCode(Long reservationCode) {
		this.reservationCode = reservationCode;
	}

	public LocalDateTime getCheckIn() {
		return checkIn;
	}

	public void setCheckIn(LocalDateTime checkIn) {
		this.checkIn = checkIn;
	}

	public LocalDateTime getCheckOut() {
		return checkOut;
	}

	public void setCheckOut(LocalDateTime checkOut) {
		this.checkOut = checkOut;
	}

	public int getNumberOfAdults() {
		return numberOfAdults;
	}

	public void setNumberOfAdults(int numberOfAdults) {
		this.numberOfAdults = numberOfAdults;
	}

	public int getNumberOfChildren() {
		return numberOfChildren;
	}

	public void setNumberOfChildren(int numberOfChildren) {
		this.numberOfChildren = numberOfChildren;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public LocalDateTime getReservationDate() {
		return reservationDate;
	}

	public void setReservationDate(LocalDateTime reservationDate) {
		this.reservationDate = reservationDate;
	}

	public BigDecimal getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(BigDecimal totalPrice) {
		this.totalPrice = totalPrice;
	}
	
	public boolean isCheckedIn() {
		return checkedIn;
	}

	public void setCheckedIn(boolean checkedIn) {
		this.checkedIn = checkedIn;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * No hay tercer constructor con este campo (Fase 7.5.3) a propósito: una
	 * reserva nunca nace cancelada, así que el valor por defecto de un
	 * {@code boolean} ({@code false}) ya es el correcto en el único sitio donde se
	 * construye una ({@code ReservationServiceImpl.reserveHousing}). Añadir un
	 * argumento posicional más a los dos constructores existentes solo habría
	 * obligado a tocar ese sitio para pasar un valor que siempre es el mismo.
	 */
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = " customerId")
	public User getCustomer() {
		return customer;
	}

	public void setCustomer(User customer) {
		this.customer = customer;
	}

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "housingId")
	public Housing getHousing() {
		return housing;
	}

	public void setHousing(Housing housing) {
		this.housing = housing;
	}

}