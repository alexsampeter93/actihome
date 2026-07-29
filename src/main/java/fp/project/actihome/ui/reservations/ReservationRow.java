package fp.project.actihome.ui.reservations;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Typography;

/**
 * Una fila de "mis reservas": miniatura, datos de la estancia y estado.
 *
 * <p>
 * El estado no es un dato guardado en {@link Reservation} —solo existe el
 * booleano {@code checkedIn}—, se deriva aquí de dos hechos: si ya se hizo el
 * check-in y si la fecha de salida ya ha pasado. Tres estados, cada uno con su
 * propio peso visual:
 *
 * <ul>
 * <li><b>Check-in pendiente</b> — en el color de acento, porque es el estado
 * que pide una acción.</li>
 * <li><b>✓ Check-in realizado</b> — en el color secundario: ya no hace falta
 * hacer nada, pero la estancia sigue viva.</li>
 * <li><b>Completada</b> — también en el color secundario, sin la marca de
 * verificación: es historial, no algo que mirar dos veces.</li>
 * </ul>
 */
public class ReservationRow extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	public ReservationRow(Reservation reservation, Runnable alAbrir) {

		super(new MigLayout(Space.insets(Space.MD, 0, Space.MD, 0), "[96!]" + Space.XL + "[grow,fill]" + Space.XL + "[]",
				"[]"));

		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		add(miniatura(reservation.getHousing().getImage()), "w 96!, h 96!");
		add(informacion(reservation), "aligny center");
		add(totalYEstado(reservation), "aligny center");

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				alAbrir.run();
			}
		});
	}

	/**
	 * Miniatura sin etiquetas de tipo o disponibilidad: a 96px no hay sitio para
	 * leerlas y solo añadirían ruido.
	 */
	private ImagePlaceholder miniatura(String imagen) {
		return new ImagePlaceholder(null, null, true, imagen);
	}

	private JPanel informacion(Reservation reservation) {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XXS + "[]"));
		panel.setOpaque(false);

		JLabel codigo = Labels.capsAccent("#R-" + reservation.getReservationCode());
		panel.add(codigo);

		JLabel nombre = Labels.cardTitle(reservation.getHousing().getName());
		nombre.setFont(Typography.serifMedium(Typography.CARD_TITLE));
		panel.add(nombre, "gaptop " + Space.XXS);

		long noches = ChronoUnit.DAYS.between(reservation.getCheckIn().toLocalDate(),
				reservation.getCheckOut().toLocalDate());

		String meta = FECHA.format(reservation.getCheckIn()) + " – " + FECHA.format(reservation.getCheckOut()) + " · "
				+ Formato.plural((int) noches, "noche", "noches") + " · " + reservation.getHousing().getLocation();

		panel.add(Labels.muted(meta), "gaptop " + Space.XXS);

		return panel;
	}

	private JPanel totalYEstado(Reservation reservation) {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[]", "[]" + Space.XXS + "[]"));
		panel.setOpaque(false);

		JLabel total = Labels.price(Formato.precio(reservation.getTotalPrice()));
		total.setFont(Typography.serifMedium(20f));
		panel.add(total);

		panel.add(estado(reservation));

		return panel;
	}

	private JLabel estado(Reservation reservation) {

		boolean completada = LocalDateTime.now().isAfter(reservation.getCheckOut());

		if (completada) {
			return Labels.caps("Completada");
		}

		if (reservation.isCheckedIn()) {
			return Labels.caps("Check-in realizado");
		}

		return Labels.capsAccent("Check-in pendiente");
	}
}
