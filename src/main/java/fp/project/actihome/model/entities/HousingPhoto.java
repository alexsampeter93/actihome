package fp.project.actihome.model.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Una foto adicional de un alojamiento, para la galería del detalle (Fase 8.4).
 *
 * <p>
 * <b>Por qué una tabla y no una lista de nombres en {@code Housing}.</b> La
 * alternativa barata era guardar los archivos separados por comas en una
 * columna de texto. Funciona hasta el primer nombre que contenga una coma, y
 * sobre todo hace imposible lo único que una galería necesita de verdad:
 * borrar la tercera foto sin tocar las otras, y reordenarlas. Una fila por
 * foto convierte las dos operaciones en un borrado y un {@code UPDATE}.
 *
 * <p>
 * <b>La foto principal sigue estando en {@code Housing.image} y no aquí.</b>
 * Podría parecer más limpio meterlas todas en esta tabla con la primera marcada
 * como principal, pero esa foto la usan el catálogo, la comparación y el panel
 * de propietario, ninguno de los cuales quiere una galería: pedirles una
 * consulta más para obtener lo que ya tenían sería pagar en todas las pantallas
 * el precio de una. Además, un alojamiento <em>siempre</em> puede tener foto
 * principal y <em>a veces</em> tiene galería, y esa asimetría es real.
 *
 * <p>
 * <b>El orden se guarda</b> ({@code position}) en lugar de deducirse del nombre
 * del archivo. Un nombre no se puede reordenar sin renombrar ficheros, y
 * renombrar ficheros para mover una foto de sitio es exactamente el tipo de
 * acoplamiento entre datos y almacenamiento que luego no se puede deshacer.
 */
@Entity
@Table(name = "HOUSING_PHOTOS")
public class HousingPhoto {

	private Long id;

	private Housing housing;

	private String image;

	private int position;

	/** Constructor sin argumentos, obligatorio para JPA. */
	public HousingPhoto() {

	}

	public HousingPhoto(Housing housing, String image, int position) {

		this.housing = housing;
		this.image = image;
		this.position = position;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "housingId")
	public Housing getHousing() {
		return housing;
	}

	public void setHousing(Housing housing) {
		this.housing = housing;
	}

	/** Nombre del archivo dentro de {@code /images/housings/}. */
	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	/** Orden dentro de la galería, empezando en 1. */
	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}
}
