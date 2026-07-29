package fp.project.actihome.ui.reviews;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Review;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.StarRating;
import fp.project.actihome.ui.theme.Space;

/**
 * Los campos de una reseña: título, cuerpo y las cinco valoraciones.
 *
 * <p>
 * <b>Existe para que publicar y editar no sean el mismo código dos veces.</b>
 * Las dos pantallas anteriores eran casi idénticas —cambiaban el rótulo, el
 * texto del botón y a qué método del servicio llamaban—, así que cualquier
 * arreglo en una había que acordarse de repetirlo en la otra. Aquí los campos se
 * definen una vez y cada pantalla aporta lo suyo: su encabezado, su acción y su
 * manejo de errores.
 */
public class ReviewForm extends JPanel {

	private static final long serialVersionUID = 1L;

	private final Field titulo;
	private final Field cuerpo;
	private final StarRating ubicacion;
	private final StarRating servicio;
	private final StarRating wifi;
	private final StarRating comida;
	private final StarRating limpieza;

	public ReviewForm() {

		super(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.LG + "[]" + Space.LG + "[]" + Space.SM + "[]" + Space.SM + "[]" + Space.SM + "[]"
						+ Space.SM + "[]" + Space.SM + "[]"));
		setOpaque(false);

		titulo = Field.text("Título");
		cuerpo = Field.textArea("Tu reseña", 110);

		ubicacion = new StarRating("Ubicación", 0);
		servicio = new StarRating("Servicio", 0);
		wifi = new StarRating("Wifi", 0);
		comida = new StarRating("Comida", 0);
		limpieza = new StarRating("Limpieza", 0);

		add(titulo);
		add(cuerpo);
		add(Labels.muted("La nota total se calcula automáticamente a partir de estas cinco."));
		add(ubicacion);
		add(servicio);
		add(wifi);
		add(comida);
		add(limpieza);
	}

	/**
	 * Precarga los valores de una reseña existente, para editarla.
	 *
	 * <p>
	 * Las notas se redondean al entero más cercano porque el selector es de
	 * estrellas enteras y el modelo guarda un {@code double}. Una reseña antigua
	 * con un 4,3 en wifi se precarga con 4 estrellas: es el valor representable más
	 * próximo, y dejar el selector a cero habría sido peor —parecería que la reseña
	 * no tenía nota, y guardar sin tocarlo la habría borrado.
	 */
	public void precargar(Review review) {

		titulo.setText(review.getTitle());
		cuerpo.setText(review.getBody());

		ubicacion.setValor(redondear(review.getLocationScore()));
		servicio.setValor(redondear(review.getServiceScore()));
		wifi.setValor(redondear(review.getWifiScore()));
		comida.setValor(redondear(review.getFoodScore()));
		limpieza.setValor(redondear(review.getCleaningScore()));
	}

	public String getTitulo() {
		return titulo.getText().trim();
	}

	public String getCuerpo() {
		return cuerpo.getText().trim();
	}

	public int getUbicacion() {
		return ubicacion.getValor();
	}

	public int getServicio() {
		return servicio.getValor();
	}

	public int getWifi() {
		return wifi.getValor();
	}

	public int getComida() {
		return comida.getValor();
	}

	public int getLimpieza() {
		return limpieza.getValor();
	}

	private static int redondear(double nota) {
		return (int) Math.round(nota);
	}
}
