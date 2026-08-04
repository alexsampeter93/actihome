package fp.project.actihome.ui.reservations;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Typography;

/**
 * Una fila de "mis reservas": miniatura, datos de la estancia y estado.
 *
 * <p>
 * El estado no es un dato guardado en {@link Reservation} —además de
 * {@code cancelled} (Fase 7.5.3), solo existe el booleano {@code checkedIn}—,
 * se deriva aquí de esos dos hechos y de si la fecha de salida ya ha pasado.
 * Cuatro estados, cada uno con su propio peso visual:
 *
 * <ul>
 * <li><b>Cancelada</b> — en el color secundario, y se comprueba <em>antes</em>
 * que los demás: da igual que la salida ya haya pasado o que se hubiera hecho
 * el check-in, cancelada manda.</li>
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

	/**
	 * @param alAbrir    qué hacer al pulsar la fila (va al check-in)
	 * @param alCancelar qué hacer al confirmar la cancelación; solo se invoca si
	 *                   {@link #esCancelable} decide mostrar el botón, así que
	 *                   quien construye la fila no necesita repetir esa lógica
	 */
	public ReservationRow(Reservation reservation, Runnable alAbrir, Runnable alCancelar) {

		super(new MigLayout(Space.insets(Space.MD, 0, Space.MD, 0), "[96!]" + Space.XL + "[grow,fill]" + Space.XL + "[]",
				"[]"));

		setOpaque(false);

		add(miniatura(reservation.getHousing().getImage()), "w 96!, h 96!");
		add(informacion(reservation), "aligny center");
		add(totalYEstado(reservation, alCancelar), "aligny center");

		// Una reserva cancelada no lleva a ningún sitio útil: el check-in de algo que
		// ya no va a suceder no tiene sentido, así que ni el cursor ni el clic
		// invitan a pulsar la fila.
		if (!reservation.isCancelled()) {

			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					alAbrir.run();
				}
			});
		}
	}

	/**
	 * Si todavía tiene sentido ofrecer cancelar: ni cancelada ya, ni con el
	 * check-in hecho, ni con la estancia ya empezada. Mismo criterio que aplica
	 * {@code ReservationServiceImpl.cancelReservation} — repetido aquí porque es
	 * lo que decide si el botón se ve, no una llamada al servicio desde un
	 * componente visual.
	 */
	private static boolean esCancelable(Reservation reservation) {

		return !reservation.isCancelled() && !reservation.isCheckedIn()
				&& LocalDateTime.now().isBefore(reservation.getCheckIn());
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
				+ Formato.plural((int) noches, Textos.t("palabra.noche.singular"), Textos.t("palabra.noche.plural"))
				+ " · " + reservation.getHousing().getLocation();

		panel.add(Labels.muted(meta), "gaptop " + Space.XXS);

		return panel;
	}

	private JPanel totalYEstado(Reservation reservation, Runnable alCancelar) {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[]",
				"[]" + Space.XXS + "[]" + Space.XS + "[]"));
		panel.setOpaque(false);

		JLabel total = Labels.price(Formato.precio(reservation.getTotalPrice()));
		total.setFont(Typography.serifMedium(20f));
		panel.add(total);

		panel.add(estado(reservation));

		if (esCancelable(reservation)) {
			panel.add(cancelar(alCancelar));
		}

		return panel;
	}

	private JLabel estado(Reservation reservation) {

		if (reservation.isCancelled()) {
			return Labels.caps(Textos.t("reservas.estado.cancelada"));
		}

		boolean completada = LocalDateTime.now().isAfter(reservation.getCheckOut());

		if (completada) {
			return Labels.caps(Textos.t("reservas.estado.completada"));
		}

		if (reservation.isCheckedIn()) {
			return Labels.caps(Textos.t("reservas.estado.checkinRealizado"));
		}

		return Labels.capsAccent(Textos.t("reservas.estado.checkinPendiente"));
	}

	/**
	 * El botón "Cancelar" de la fila. Es un {@code Buttons.link} de verdad, no un
	 * texto pintado a mano como el enlace de {@code HousingRow} — aquí no hace
	 * falta consumir el clic para que no compita con el de la fila, porque el
	 * clic de fila solo lleva al check-in de reservas ya no cancelables, y esta
	 * columna vive fuera de esa zona clicable en el resto de casos.
	 */
	private JButton cancelar(Runnable alCancelar) {
		return Buttons.link(Textos.t("reservas.cancelar"), e -> alCancelar.run());
	}
}
