package fp.project.actihome.ui;

import java.awt.Dimension;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.ScrollPaneConstants;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.model.exceptions.AlreadyCancelledException;
import fp.project.actihome.model.exceptions.CannotCancelException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.NotMyReservationException;
import fp.project.actihome.model.services.ReservationService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Confirmacion;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.nav.ConNombre;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.reservations.ReservationRow;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.components.Rescate;

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
public class ShowMyReservationsFrame extends JFrame implements ConNombre {

	private static final long serialVersionUID = 1L;

	private final transient ReservationService reservationService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private JPanel lista;
	private JScrollPane scroll;
	private JLabel titulo;
	private JLabel error;

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
			titulo.setText(Textos.t("header.nav.misReservas"));
			cargarReservas();
			volverArriba();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(1000, 780);
		setLocationRelativeTo(null);

		headerPanel.marcarActual(ShowMyReservationsFrame.class);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[]0[grow,fill]"));

		raiz.add(headerPanel, "growx");
		raiz.add(titular(), "growx");
		raiz.add(zonaDeLista(), "grow");

		setContentPane(raiz);
	}

	private JPanel titular() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.XL, Space.HUGE, Space.LG, Space.HUGE),
				"[grow,fill]", "[]" + Space.XXS + "[]"));
		panel.setOpaque(false);

		titulo = Labels.title(" ");
		panel.add(titulo);

		error = Labels.error(" ");
		panel.add(error);

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

		// Rescate y no un JScrollPane crudo. La diferencia esta en el Scrollable que
		// Rescate envuelve: sin el, el contenido conserva su ancho preferido en vez de
		// seguir el del visor, y con la barra horizontal desactivada lo que se sale por
		// la derecha NO SE PUEDE ALCANZAR NUNCA, por mucho que se agrande la ventana.
		scroll = Rescate.envolver(lista);
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
		error.setText(" ");

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
				() -> navigator.ir(DoCheckInFrame.class, frame -> frame.setReservation(reserva)),
				() -> cancelar(reserva));
	}

	/**
	 * Pide confirmación y, si se acepta, cancela la reserva.
	 *
	 * <p>
	 * Un mensaje por excepción (patrón B11): las cuatro que declara
	 * {@code cancelReservation} pueden pasar por motivos distintos, y un
	 * "Error en los datos" genérico no diría cuál. Las dos que solo pueden darse
	 * por una carrera con otra pestaña o sesión —cancelar algo ya cancelado, o
	 * cancelar algo en lo que ya se ha hecho check-in mientras se decidía—
	 * recargan la lista igualmente: el estado real ha cambiado y hay que
	 * enseñarlo, no solo el mensaje.
	 */
	private void cancelar(Reservation reserva) {

		boolean confirmado = Confirmacion.preguntar(this, Textos.t("reservas.cancelar.titulo"),
				Textos.t("reservas.cancelar.mensaje", reserva.getHousing().getName()),
				Textos.t("reservas.cancelar.confirmar"));

		if (!confirmado) {
			return;
		}

		try {
			reservationService.cancelReservation(sessionManager.getLoggedInUser().getId(), reserva.getId());
			cargarReservas();

		} catch (InstanceNotFoundException ex) {
			error.setText(Textos.t("reservas.error.noExiste"));

		} catch (NotMyReservationException ex) {
			error.setText(Textos.t("reservas.error.noEsTuya"));

		} catch (AlreadyCancelledException ex) {
			// cargarReservas() limpia el error al principio (para la carga normal), así
			// que aquí va después: primero se refresca la lista con el estado real, luego
			// se deja el mensaje puesto encima.
			cargarReservas();
			error.setText(Textos.t("reservas.error.yaCancelada"));

		} catch (CannotCancelException ex) {
			cargarReservas();
			error.setText(Textos.t("reservas.error.noSePuedeCancelar"));
		}
	}

	private JPanel estadoVacio() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.HUGE, 0, Space.HUGE, 0), "[grow,fill]",
				"[]" + Space.LG + "[]" + Space.XS + "[]" + Space.LG + "[]"));
		panel.setOpaque(false);

		panel.add(centrar(new MascotSlot(MascotSlot.Tamano.MEDIANO, Pose.ACCION)));
		panel.add(centrar(Labels.title(Textos.t("reservas.vacio.titulo"))));
		panel.add(centrar(Labels.muted(Textos.t("reservas.vacio.cuerpo"))));
		panel.add(centrar(Buttons.link(Textos.t("reservas.vacio.irCatalogo"), e -> navigator.ir(ShowHousingsFrame.class))));

		return panel;
	}

	private JPanel centrar(JComponent componente) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "push[]push", ""));
		fila.setOpaque(false);
		fila.add(componente);
		return fila;
	}

	/** El nombre con el que la enseña el enlace de atrás de otra pantalla. */
	@Override
	public String nombreDePantalla() {
		return Textos.t("header.nav.misReservas");
	}
}
