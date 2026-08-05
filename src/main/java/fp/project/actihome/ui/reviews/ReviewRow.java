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
import fp.project.actihome.ui.components.Avatar;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Chip;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.WrappingText;
import fp.project.actihome.ui.theme.Contenido;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
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

	/**
	 * El patrón cambia con el idioma activo, no solo el {@link Locale}: "d 'de'
	 * MMMM 'de' yyyy" lleva la palabra "de" escrita a mano, así que ponerle un
	 * locale inglés habría dejado fechas como "12 de January" — el nombre del mes
	 * en inglés, pegado a una gramática que sigue siendo española. Se resuelve en
	 * cada fila, no se guarda: la fila se reconstruye entera en cada visita a la
	 * pantalla (Fase 7.6), así que no hace falta ningún mecanismo de refresco.
	 */
	private static DateTimeFormatter formatoFecha() {

		return Textos.idioma().getLanguage().equals("en")
				? DateTimeFormatter.ofPattern("MMMM d, yyyy", Textos.idioma())
				: DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Textos.idioma());
	}

	/** Caracteres del extracto. Dos líneas largas aproximadamente. */
	private static final int LIMITE_EXTRACTO = 180;

	public ReviewRow(Review review, Runnable alAbrir) {

		super(new MigLayout("wrap 1, " + Space.insets(Space.LG, 0, Space.LG, 0), "[grow,fill]",
				"[]" + Space.XXS + "[]" + Space.SM + "[]" + Space.MD + "[]"));

		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		add(cabecera(review, alAbrir), "growx");
		add(autoria(review), "growx");
		add(new WrappingText(extracto(Contenido.de(review.getBody()))), "growx");
		add(subNotas(review), "growx");

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				alAbrir.run();
			}
		});
	}

	/**
	 * Quién la escribió y cuándo, con su avatar de iniciales delante (Fase 8.4).
	 *
	 * <p>
	 * El avatar no añade ninguna información que no estuviera ya en el texto —el
	 * nombre sigue ahí al lado—, y aun así hace un trabajo real: en una lista de
	 * reseñas seguidas, un disco de color permite ver de un golpe si son de
	 * personas distintas o de la misma. El color lo deriva {@link Avatar} del
	 * propio nombre, así que es estable entre sesiones sin guardar nada.
	 */
	private JPanel autoria(Review review) {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XS + "[]", "[]"));
		panel.setOpaque(false);

		panel.add(Avatar.relleno(review.getAuthor().getName(), review.getAuthor().getSurname(), 26),
				"w 26!, h 26!, aligny center");
		panel.add(Labels.muted(Textos.t("resenas.row.por", review.getAuthor().getUsername()) + " · "
				+ formatoFecha().format(review.getPublicationDate())), "aligny center");

		return panel;
	}

	/** Título a la izquierda, nota total, los indicadores de F15 y el enlace de apertura a la derecha. */
	private JPanel cabecera(Review review, Runnable alAbrir) {

		JPanel panel = new JPanel(
				new MigLayout(Space.insets(0), "[]" + Space.MD + "[]" + Space.MD + "[]push[]", "[]"));
		panel.setOpaque(false);

		JLabel titulo = Labels.cardTitle(Contenido.de(review.getTitle()));
		titulo.setFont(Typography.serifMedium(20f));
		panel.add(titulo);

		JLabel nota = Labels.price(Formato.nota(review.getTotalScore()));
		nota.setFont(Typography.serifMedium(20f));
		panel.add(nota);

		panel.add(indicadores(review));

		// La fila entera ya es pinchable; el enlace existe porque una zona pinchable
		// sin ninguna señal visible no se descubre.
		panel.add(Buttons.link(Textos.t("resenas.verResena"), e -> alAbrir.run()));

		return panel;
	}

	/**
	 * Pistas de F15 sin abrir la reseña: si lleva foto, si el propietario ya ha
	 * respondido. Un panel vacío cuando no hay ninguna de las dos no ocupa
	 * ancho, así que no hace falta un layout condicional distinto para ese caso.
	 */
	private JPanel indicadores(Review review) {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XS + "[]", "[]"));
		panel.setOpaque(false);

		if (review.getImage() != null) {
			panel.add(Chip.informativo(Textos.t("resenas.row.conFoto")));
		}

		if (review.getOwnerResponse() != null) {
			panel.add(Chip.informativo(Textos.t("resenas.row.respondida")));
		}

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

		panel.add(subNota(Textos.t("resenas.subnota.ubicacion"), review.getLocationScore()));
		panel.add(subNota(Textos.t("resenas.subnota.servicio"), review.getServiceScore()));
		panel.add(subNota(Textos.t("resenas.subnota.wifi"), review.getWifiScore()));
		panel.add(subNota(Textos.t("resenas.subnota.comida"), review.getFoodScore()));
		panel.add(subNota(Textos.t("resenas.subnota.limpieza"), review.getCleaningScore()));

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
