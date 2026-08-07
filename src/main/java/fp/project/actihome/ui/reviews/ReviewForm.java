package fp.project.actihome.ui.reviews;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Review;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Columnas;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.StarRating;
import fp.project.actihome.ui.theme.BrandAssets;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;

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
	private final JLabel nota;
	private final StarRating ubicacion;
	private final StarRating servicio;
	private final StarRating wifi;
	private final StarRating comida;
	private final StarRating limpieza;

	private final ImagePlaceholder previsualizacion = new ImagePlaceholder();
	private JLabel etiquetaFoto;
	private JButton botonElegirFoto;
	private JButton botonQuitarFoto;
	private JLabel errorFoto;

	/** La foto recién elegida en el selector, sin guardar todavía. Ver la nota de {@link #guardarFotoSiHaceFalta}. */
	private File fotoElegida;

	/** El nombre de la foto que ya tenía la reseña, o {@code null} si nunca tuvo o se acaba de quitar. */
	private String imagenExistente;

	public ReviewForm() {

		// **Dos columnas: lo que se escribe y lo que se puntúa.**
		//
		// Los nueve bloques iban apilados y pedían 685 puntos de alto en una ventana
		// que da 672 contando cabecera y botones, así que el formulario de reseña
		// nunca cupo en un portátil. Y el corte estaba servido: escribir un título,
		// un texto y adjuntar una foto es una tarea —se hace con el teclado, seguida—
		// y puntuar cinco aspectos es otra —se hace con el ratón, en cualquier orden—.
		// Apiladas, las cinco valoraciones quedaban además tan abajo que había que
		// buscarlas.
		// Y en dos columnas solo mientras quepan: por debajo de 340 puntos cada una, se
		// apilan como estaban. Es Columnas quien lo decide, no un umbral escrito aquí.
		super(new java.awt.BorderLayout());
		setOpaque(false);

		titulo = Field.text(Textos.t("resenaForm.titulo"));
		cuerpo = Field.textArea(Textos.t("resenaForm.cuerpo"), 4);

		ubicacion = new StarRating(Textos.t("resenas.subnota.ubicacion"), 0);
		servicio = new StarRating(Textos.t("resenas.subnota.servicio"), 0);
		wifi = new StarRating(Textos.t("resenas.subnota.wifi"), 0);
		comida = new StarRating(Textos.t("resenas.subnota.comida"), 0);
		limpieza = new StarRating(Textos.t("resenas.subnota.limpieza"), 0);

		nota = Labels.muted(Textos.t("resenaForm.notaAutomatica"));

		JPanel loQueSeEscribe = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.aire(Space.LG) + "[]" + Space.aire(Space.LG) + "[]"));
		loQueSeEscribe.setOpaque(false);
		loQueSeEscribe.add(titulo);
		loQueSeEscribe.add(cuerpo);
		loQueSeEscribe.add(campoFoto());

		JPanel loQueSePuntua = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.aire(Space.LG) + "[]" + Space.aire(Space.SM) + "[]" + Space.aire(Space.SM) + "[]"
						+ Space.aire(Space.SM) + "[]" + Space.aire(Space.SM) + "[]"));
		loQueSePuntua.setOpaque(false);
		loQueSePuntua.add(nota);
		loQueSePuntua.add(ubicacion);
		loQueSePuntua.add(servicio);
		loQueSePuntua.add(wifi);
		loQueSePuntua.add(comida);
		loQueSePuntua.add(limpieza);

		Columnas columnas = new Columnas(340, Space.XXL);
		columnas.add(loQueSeEscribe);
		columnas.add(loQueSePuntua);

		add(columnas, java.awt.BorderLayout.CENTER);
	}

	/**
	 * Elegir una foto para adjuntar a la reseña (F15), opcional. Mismo patrón
	 * que {@code HousingForm.campoFoto}: la reducción de verdad se pospone a
	 * {@link #guardarFotoSiHaceFalta}, que llama quien tiene la pantalla, solo
	 * una vez que la reseña ya se ha publicado o actualizado con éxito.
	 */
	private JPanel campoFoto() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.MD + "[grow,fill]", ""));
		panel.setOpaque(false);

		// Alto negociable por lo mismo que en HousingForm: una miniatura cede, un campo
		// de texto no.
		panel.add(previsualizacion, "w 120!, h 60:84:84, aligny top");

		JPanel acciones = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		acciones.setOpaque(false);

		etiquetaFoto = Labels.caps(Textos.t("resenaForm.foto.label"));
		acciones.add(etiquetaFoto, "gapbottom " + Space.XS);

		botonElegirFoto = Buttons.secondary(Textos.t("resenaForm.foto.elegir"), e -> elegirFoto());
		acciones.add(botonElegirFoto, "gapbottom " + Space.XXS);

		botonQuitarFoto = Buttons.link(Textos.t("resenaForm.foto.quitar"), e -> quitarFoto());
		acciones.add(botonQuitarFoto);

		errorFoto = Labels.error(" ");
		acciones.add(errorFoto, "gaptop " + Space.XXS);

		panel.add(acciones, "aligny top");

		return panel;
	}

	private void elegirFoto() {

		JFileChooser selector = new JFileChooser();
		selector.setFileFilter(new FileNameExtensionFilter(Textos.t("alojamientoForm.foto.filtro"), "jpg", "jpeg", "png"));

		if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File elegido = selector.getSelectedFile();

		try {
			BufferedImage leida = ImageIO.read(elegido);

			if (leida == null) {
				throw new IOException("formato no reconocido");
			}

			fotoElegida = elegido;
			previsualizacion.setFoto(leida);
			errorFoto.setText(" ");

		} catch (IOException ex) {
			errorFoto.setText(Textos.t("alojamientoForm.foto.error.noSeLee"));
		}
	}

	private void quitarFoto() {

		fotoElegida = null;
		imagenExistente = null;
		previsualizacion.setFoto(null);
		errorFoto.setText(" ");
	}

	/**
	 * Vuelve a fijar los textos fijos del formulario en el idioma activo (Fase
	 * 7.6). Lo llaman {@code PublishReviewFrame}/{@code UpdateReviewFrame} desde
	 * su propio {@code setVisible(true)}: este formulario es un campo de esas
	 * pantallas singleton, así que no se reconstruye solo con el idioma.
	 */
	public void actualizarTextos() {

		titulo.setEtiqueta(Textos.t("resenaForm.titulo"));
		cuerpo.setEtiqueta(Textos.t("resenaForm.cuerpo"));
		nota.setText(Textos.t("resenaForm.notaAutomatica"));
		ubicacion.setEtiqueta(Textos.t("resenas.subnota.ubicacion"));
		servicio.setEtiqueta(Textos.t("resenas.subnota.servicio"));
		wifi.setEtiqueta(Textos.t("resenas.subnota.wifi"));
		comida.setEtiqueta(Textos.t("resenas.subnota.comida"));
		limpieza.setEtiqueta(Textos.t("resenas.subnota.limpieza"));

		etiquetaFoto.setText(Textos.t("resenaForm.foto.label"));
		botonElegirFoto.setText(Textos.t("resenaForm.foto.elegir"));
		botonQuitarFoto.setText(Textos.t("resenaForm.foto.quitar"));
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

		fotoElegida = null;
		imagenExistente = review.getImage();
		previsualizacion.setFoto(BrandAssets.fotoDeResena(imagenExistente));
		errorFoto.setText(" ");
	}

	/** La foto recién elegida sin guardar, o {@code null} si no se ha tocado el campo. */
	public File getFotoElegida() {
		return fotoElegida;
	}

	/**
	 * El nombre de la foto que ya tenía la reseña antes de este formulario, o
	 * {@code null} si nunca tuvo o si se acaba de pulsar "Quitar". Junto con
	 * {@link #getFotoElegida()}, es lo que necesita quien tiene la pantalla
	 * para decidir si hay que llamar a {@code ReviewService.setReviewImage}
	 * tras guardar: foto nueva, foto quitada, o ningún cambio.
	 */
	public String getImagenExistente() {
		return imagenExistente;
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
