package fp.project.actihome.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.model.exceptions.AlreadyCheckedInException;
import fp.project.actihome.model.exceptions.CannotCheckInException;
import fp.project.actihome.model.exceptions.CodeDoesNotMatchException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.NotMyReservationException;
import fp.project.actihome.model.services.ReservationService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Hacer el check-in de una reserva.
 *
 * <p>
 * <b>Por qué recibe la reserva entera y no solo su identificador.</b> El
 * servicio no tiene ningún método para consultar una reserva suelta —solo
 * {@code showMyReservations}, {@code reserveHousing} y {@code doCheckIn}—, y
 * añadir uno nuevo solo para pintar un resumen sería tocar la capa de negocio
 * por una necesidad puramente de interfaz. En su lugar, {@code ShowMyReservationsFrame}
 * ya tiene el objeto completo en memoria —viene de la lista que acaba de
 * cargar— y lo pasa directamente. La confirmación del check-in, en cambio, sí
 * va contra el servicio con el identificador, que es donde tiene que
 * validarse de verdad.
 *
 * <p>
 * Los cinco motivos por los que un check-in puede fallar
 * ({@link CodeDoesNotMatchException}, {@link CannotCheckInException},
 * {@link AlreadyCheckedInException}, {@link NotMyReservationException},
 * {@link InstanceNotFoundException}) tienen cada uno su propio mensaje en
 * español. La versión anterior los distinguía pero mezclaba la reacción: unas
 * veces cerraba la ventana y otras no, y en un caso mostraba <em>dos</em>
 * diálogos seguidos para el mismo error. Aquí la regla es una sola: el error
 * se enseña y la pantalla se queda quieta, para que se pueda leer con calma y
 * decidir si se corrige el código o se vuelve atrás.
 */
@Component
@Profile("!test")
@Lazy
public class DoCheckInFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final transient ReservationService reservationService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private transient Reservation reservation;

	private JLabel superTitulo;
	private JLabel tituloAlojamiento;
	private JLabel subtituloFechas;
	private Field codigo;
	private JLabel error;
	private JButton botonConfirmar;
	private JLabel enlaceVolver;

	public DoCheckInFrame(ReservationService reservationService, SessionManager sessionManager, Navigator navigator,
			HeaderPanel headerPanel) {

		this.reservationService = reservationService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	/** La reserva sobre la que se va a hacer el check-in. */
	public void setReservation(Reservation reservation) {
		this.reservation = reservation;
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			headerPanel.refresh();
			actualizarTextosFijos();
			cargarResumen();
			limpiar();
		}

		super.setVisible(visible);
	}

	private void cargarResumen() {

		if (reservation == null) {
			return;
		}

		tituloAlojamiento.setText(reservation.getHousing().getName());
		subtituloFechas.setText(
				FECHA.format(reservation.getCheckIn()) + " – " + FECHA.format(reservation.getCheckOut()) + " · "
						+ reservation.getHousing().getLocation());
	}

	private void actualizarTextosFijos() {

		superTitulo.setText(Textos.t("checkin.titulo"));
		codigo.setEtiqueta(Textos.t("checkin.codigo"));
		botonConfirmar.setText(Textos.t("checkin.confirmar"));
		enlaceVolver.setText(Textos.t("checkin.volver"));
	}

	private void limpiar() {

		codigo.setText("");
		error.setText(" ");
		SwingUtilities.invokeLater(() -> codigo.getInput().requestFocus());
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(720, 680);
		setLocationRelativeTo(null);

		headerPanel.marcarActual(ShowMyReservationsFrame.class);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[grow,fill]"));

		JPanel exterior = new JPanel(new MigLayout(Space.insets(Space.GIANT), "[grow]", "[grow]"));
		exterior.setOpaque(false);
		exterior.add(formulario(), Layout.ancho(Layout.FORMULARIO) + ", aligny center, alignx center");

		raiz.add(exterior, "grow");

		setContentPane(raiz);

		Foco.alPulsarEscape(this, () -> navigator.ir(ShowMyReservationsFrame.class));
	}

	private JPanel formulario() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.XXL + "[]" + Space.SM + "[]" + Space.XL + "[]"));
		panel.setOpaque(false);

		panel.add(cabecera());

		codigo = Field.text(Textos.t("checkin.codigo"));
		codigo.onEnter(this::confirmar);
		panel.add(codigo);

		error = Labels.error(" ");
		panel.add(error);

		panel.add(acciones());

		return panel;
	}

	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);

		superTitulo = Labels.capsAccent(Textos.t("checkin.titulo"));
		titulos.add(superTitulo);

		tituloAlojamiento = Labels.title(" ");
		titulos.add(tituloAlojamiento, "gaptop " + Space.XXS);

		subtituloFechas = Labels.muted(" ");
		titulos.add(subtituloFechas, "gaptop " + Space.XXS);

		panel.add(titulos);
		panel.add(new MascotSlot(MascotSlot.Tamano.PEQUENO, Pose.BIENVENIDA), "top, w 56!, h 56!");

		return panel;
	}

	private JPanel acciones() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]", ""));
		fila.setOpaque(false);

		botonConfirmar = Buttons.primary(Textos.t("checkin.confirmar"), e -> confirmar());
		fila.add(botonConfirmar, "height 44!");
		fila.add(volver());

		return fila;
	}

	private JLabel volver() {

		JLabel enlace = Labels.body(Textos.t("checkin.volver"));
		enlace.setFont(Typography.sansSemiBold(Typography.BODY_SM));
		enlace.setForeground(Theme.mut());
		enlace.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		enlace.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				navigator.ir(ShowMyReservationsFrame.class);
			}
		});

		enlaceVolver = enlace;
		return enlace;
	}

	private void confirmar() {

		Long reservationCode;

		try {
			reservationCode = Long.parseLong(codigo.getText().trim());

		} catch (NumberFormatException ex) {
			error.setText(Textos.t("checkin.error.codigoNumero"));
			codigo.requestFocus();
			return;
		}

		try {
			reservationService.doCheckIn(sessionManager.getLoggedInUser().getId(), reservation.getId(),
					reservationCode);

			navigator.ir(ShowMyReservationsFrame.class);

		} catch (CodeDoesNotMatchException ex) {
			error.setText(Textos.t("checkin.error.noCoincide"));
			codigo.setText("");
			codigo.requestFocus();

		} catch (CannotCheckInException ex) {
			error.setText(Textos.t("checkin.error.fechaNoLlegada"));

		} catch (AlreadyCheckedInException ex) {
			error.setText(Textos.t("checkin.error.yaHecho"));

		} catch (NotMyReservationException ex) {
			error.setText(Textos.t("checkin.error.noTuya"));

		} catch (InstanceNotFoundException ex) {
			error.setText(Textos.t("checkin.error.noEncontrada"));
		}
	}
}
