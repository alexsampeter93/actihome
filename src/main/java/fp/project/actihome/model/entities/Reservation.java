package fp.project.actihome.model.entities;

import java.math.BigDecimal;
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
@Table(name = "RESERVATIONS")
public class Reservation {

	private Long id;

	private Long reservationCode;

	private LocalDateTime checkIn;

	private LocalDateTime checkOut;

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
			String paymentMethod, LocalDateTime reservationDate, BigDecimal totalPrice, boolean checkedIn, User customer,
			Housing housing) {
		super();
		this.id = id;
		this.reservationCode = reservationCode;
		this.checkIn = checkIn;
		this.checkOut = checkOut;
		this.paymentMethod = paymentMethod;
		this.reservationDate = reservationDate;
		this.totalPrice = totalPrice;
		this.checkedIn = checkedIn;
		this.customer = customer;
		this.housing = housing;
	}

	public Reservation(Long reservationCode, LocalDateTime checkIn, LocalDateTime checkOut, String paymentMethod, LocalDateTime reservationDate,
			BigDecimal totalPrice, boolean checkedIn, User customer, Housing housing) {
		super();
		this.reservationCode = reservationCode;
		this.checkIn = checkIn;
		this.checkOut = checkOut;
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