package fp.project.actihome.ui.reviews;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Review;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.WrappingText;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Typography;

/**
 * Una reseña dentro del listado de un alojamiento.
 *
 * <p>
 * Título y nota arriba, autoría y fecha debajo, un extracto del cuerpo, y la
 * fila de las cinco sub-notas en versalita.
 *
 * <p>
 * <b>El cuerpo se recorta, no se enseña entero.</b> Una reseña puede tener
 * varios párrafos, y cinco reseñas completas convierten el listado en algo que
 * no se puede recorrer de un vistazo. El corte se hace por la última palabra
 * que cabe, nunca a mitad de palabra: partir "extraordinariamente" en
 * "extraordina…" se lee como un error de la aplicación, no como un resumen.
 */
public class ReviewRow extends JPanel {

	private static final long serialVersionUID = 1L;

	// El idioma se fija a propósito: sin él, el nombre del mes sale en el idioma
	// del sistema y la aplicación mezclaría "12 de enero" con "12 de January"
	// según el equipo donde se abra.
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy",
			new Locale("es", "ES"));

	/** Caracteres del extracto. Dos líneas largas aproximadamente. */
	private static final int LIMITE_EXTRACTO = 180;

	public ReviewRow(Review review, Runnable alAbrir) {

		super(new MigLayout("wrap 1, " + Space.insets(Space.LG, 0, Space.LG, 0), "[grow,fill]",
				"[]" + Space.XXS + "[]" + Space.SM + "[]" + Space.MD + "[]"));

		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		add(cabecera(review, alAbrir), "growx");
		add(Labels.muted("por " + review.getAuthor().getUsername() + " · " + FECHA.format(review.getPublicationDate())));
		add(new WrappingText(extracto(review.getBody())), "growx");
		add(subNotas(review), "growx");

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				alAbrir.run();
			}
		});
	}

	/** Título a la izquierda, nota total y el enlace de apertura a la derecha. */
	private JPanel cabecera(Review review, Runnable alAbrir) {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.MD + "[]push[]", "[]"));
		panel.setOpaque(false);

		JLabel titulo = Labels.cardTitle(review.getTitle());
		titulo.setFont(Typography.serifMedium(20f));
		panel.add(titulo);

		JLabel nota = Labels.price(Formato.nota(review.getTotalScore()));
		nota.setFont(Typography.serifMedium(20f));
		panel.add(nota);

		// La fila entera ya es pinchable; el enlace existe porque una zona pinchable
		// sin ninguna señal visible no se descubre.
		panel.add(Buttons.link("Ver reseña →", e -> alAbrir.run()));

		return panel;
	}

	/**
	 * Las cinco sub-notas en una línea, en versalita.
	 *
	 * <p>
	 * Aquí van como texto y no como las barras de {@code ScoreBar}: en el listado
	 * lo que interesa es poder comparar reseñas entre sí, y para eso cinco cifras
	 * en la misma posición de cada fila funcionan mejor que cinco barras, que
	 * ocuparían el alto de toda la fila. Las barras viven en el detalle, donde solo
	 * hay una reseña y sí hay sitio para leerlas despacio.
	 */
	private JPanel subNotas(Review review) {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0),
				"[]" + Space.LG + "[]" + Space.LG + "[]" + Space.LG + "[]" + Space.LG + "[]", "[]"));
		panel.setOpaque(false);

		panel.add(subNota("Ubicación", review.getLocationScore()));
		panel.add(subNota("Servicio", review.getServiceScore()));
		panel.add(subNota("Wifi", review.getWifiScore()));
		panel.add(subNota("Comida", review.getFoodScore()));
		panel.add(subNota("Limpieza", review.getCleaningScore()));

		return panel;
	}

	private JLabel subNota(String etiqueta, double valor) {
		return Labels.caps(etiqueta + " " + Formato.nota(valor));
	}

	private static String extracto(String cuerpo) {

		if (cuerpo == null) {
			return "";
		}

		String limpio = cuerpo.trim();

		if (limpio.length() <= LIMITE_EXTRACTO) {
			return limpio;
		}

		int corte = limpio.lastIndexOf(' ', LIMITE_EXTRACTO);

		return limpio.substring(0, corte < 0 ? LIMITE_EXTRACTO : corte) + "…";
	}
}
