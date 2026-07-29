package fp.project.actihome.ui;

import java.awt.Dimension;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.ScrollPaneConstants;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.ReservationService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.reservations.ReservationRow;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.Space;

/**
 * Mis reservas: el historial de un CUSTOMER, más reciente primero.
 *
 * <p>
 * Cabecera de alto fijo, lista con scroll ocupando el resto — la misma regla
 * de escritorio que el catálogo. Aquí no hay filtros ni buscador porque no
 * hacen falta: las reservas de una persona son pocas, y una lista corta no
 * necesita herramientas para recorrerla.
 */
@Component
@Profile("!test")
@Lazy
public class ShowMyReservationsFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final transient ReservationService reservationService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private JPanel lista;
	private JScrollPane scroll;

	public ShowMyReservationsFrame(ReservationService reservationService, SessionManager sessionManager,
			Navigator navigator, HeaderPanel headerPanel) {

		this.reservationService = reservationService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			headerPanel.refresh();
			cargarReservas();
				volverArriba();
	}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(1000, 780);
		setMinimumSize(new Dimension(820, 620));
		setLocationRelativeTo(null);

		headerPanel.marcarActual(ShowMyReservationsFrame.class);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[]0[grow,fill]"));

		raiz.add(headerPanel, "growx");
		raiz.add(titular(), "growx");
		raiz.add(zonaDeLista(), "grow");

		setContentPane(raiz);
	}

	private JPanel titular() {

		JPanel panel = new JPanel(
				new MigLayout(Space.insets(Space.XL, Space.HUGE, Space.LG, Space.HUGE), "[grow,fill]", "[]"));
		panel.setOpaque(false);

		panel.add(Labels.title("Mis reservas"));

		return panel;
	}

	/**
	 * Devuelve la lista al principio.
	 *
	 * <p>
	 * <b>Hace falta porque los frames son singleton.</b> El {@code JScrollPane} es
	 * el mismo objeto en cada visita y conserva su posición, así que al volver a
	 * esta pantalla la lista aparecía desplazada desde la vez anterior —con la
	 * primera fila cortada por arriba— sin que el usuario hubiera tocado la rueda.
	 * Se leía como un fallo de maquetación y era memoria de estado.
	 *
	 * <p>
	 * Va dentro de {@code invokeLater} porque en el momento de llamarlo la lista
	 * acaba de reconstruirse y todavía no se ha distribuido: poner el scroll a cero
	 * antes de que el layout calcule el alto no serviría de nada.
	 */
	private void volverArriba() {

		if (scroll != null) {
			SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
		}
	}

	private JScrollPane zonaDeLista() {


		lista = new JPanel(
				new MigLayout("wrap 1, " + Space.insets(0, Space.HUGE, Space.XXL, Space.HUGE), "[grow,fill]", "[]"));
		lista.setOpaque(false);

		scroll = new JScrollPane(lista);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setBorder(null);
		scroll.setViewportBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.getVerticalScrollBar().setUnitIncrement(24);

		// Mínimo cero: si la ventana se queda corta, el espacio se lo quita la lista
		// —que tiene scroll para eso— y no la cabecera. Ver la nota extensa sobre este
		// mismo problema en ShowHousingsFrame.
		scroll.setMinimumSize(new Dimension(0, 0));

		return scroll;
	}

	private void cargarReservas() {

		lista.removeAll();

		List<Reservation> reservas;

		try {
			reservas = reservationService.showMyReservations(sessionManager.getLoggedInUser().getId());

		} catch (InstanceNotFoundException ex) {
			// No debería ocurrir: el id viene de la sesión activa. Si el usuario ha sido
			// eliminado a mitad de sesión —algo que hoy no tiene ni siquiera un botón que
			// lo provoque— lo razonable es volver al login y no enseñar una lista muerta.
			navigator.ir(LoginFrame.class);
			return;
		}

		if (reservas.isEmpty()) {
			lista.add(estadoVacio(), "growx");

		} else {
			boolean primera = true;

			for (Reservation reserva : reservas) {

				if (!primera) {
					lista.add(Hairline.horizontal(), "growx, h 1!");
				}

				lista.add(fila(reserva), "growx");
				primera = false;
			}
		}

		lista.revalidate();
		lista.repaint();
	}

	private ReservationRow fila(Reservation reserva) {

		return new ReservationRow(reserva,
				() -> navigator.ir(DoCheckInFrame.class, frame -> frame.setReservation(reserva)));
	}

	private JPanel estadoVacio() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.HUGE, 0, Space.HUGE, 0), "[grow,fill]",
				"[]" + Space.LG + "[]" + Space.XS + "[]" + Space.LG + "[]"));
		panel.setOpaque(false);

		panel.add(centrar(new MascotSlot(MascotSlot.Tamano.MEDIANO, Pose.ACCION)));
		panel.add(centrar(Labels.title("Todavía no tienes reservas")));
		panel.add(centrar(Labels.muted("Cuando reserves un alojamiento, aparecerá aquí.")));
		panel.add(centrar(Buttons.link("Ir al catálogo →", e -> navigator.ir(ShowHousingsFrame.class))));

		return panel;
	}

	private JPanel centrar(JComponent componente) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "push[]push", ""));
		fila.setOpaque(false);
		fila.add(componente);
		return fila;
	}
}
