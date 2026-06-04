package fp.project.actihome.model.entities;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "USERS")
public class User {

	public enum RoleType {
		ADMIN, CUSTOMER;
	}

	private Long id;

	private String username;

	private String password;

	private String name;

	private String surname;

	private String locality;

	private int phoneNumber;

	private String email;

	private LocalDateTime birthDate;

	private RoleType role;

	public User() {

	}

	public User(Long id, String username, String password, String name, String surname, String locality,
			int phoneNumber, String email, LocalDateTime birthDate, RoleType role) {
		super();
		this.id = id;
		this.username = username;
		this.password = password;
		this.name = name;
		this.surname = surname;
		this.locality = locality;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.birthDate = birthDate;
		this.role = role;
	}

	public User(String username, String password, String name, String surname, String locality, int phoneNumber,
			String email, LocalDateTime birthDate, RoleType role) {
		super();
		this.username = username;
		this.password = password;
		this.name = name;
		this.surname = surname;
		this.locality = locality;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.birthDate = birthDate;
		this.role = role;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getLocality() {
		return locality;
	}

	public void setLocality(String locality) {
		this.locality = locality;
	}

	public int getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(int phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public LocalDateTime getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(LocalDateTime birthDate) {
		this.birthDate = birthDate;
	}

	public RoleType getRole() {
		return role;
	}

	public void setRole(RoleType role) {
		this.role = role;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", username=" + username + ", name=" + name + ", surname=" + surname + ", locality="
				+ locality + ", phoneNumber=" + phoneNumber + ", email=" + email + ", birthDate=" + birthDate
				+ ", role=" + role + "]";
	}

}
