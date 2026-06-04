package fp.project.actihome.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.exceptions.AlreadyCheckedInException;
import fp.project.actihome.model.exceptions.CannotCheckInException;
import fp.project.actihome.model.exceptions.CodeDoesNotMatchException;
import fp.project.actihome.model.exceptions.NotMyReservationException;
import fp.project.actihome.model.services.ReservationService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

@Component
@Profile("!test")
@Lazy
public class DoCheckInFrame extends JFrame {

	private final ReservationService reservationService;
	private ApplicationContext context;
	private SessionManager sessionManager;
	private HeaderPanel headerPanel;
	private Long reservationId;

	private JFormattedTextField reservationCodeField;

	public DoCheckInFrame(ReservationService reservationService, ApplicationContext context,
			SessionManager sessionManager, HeaderPanel headerPanel) {

		this.reservationService = reservationService;
		this.context = context;
		this.sessionManager = sessionManager;
		this.headerPanel = headerPanel;
		initUI();
	}

	public void setReservationId(Long id) {

		this.reservationId = id;
	}

	private void initUI() {

		setTitle("Actihome");
		setSize(500, 500);
		setLocationRelativeTo(null);

		JPanel jpanel = new JPanel(new GridLayout(10, 2, 2, 2));
		jpanel.setBorder(BorderFactory.createTitledBorder("Realizar check-In"));

		NumberFormat reservationCodeFormat = NumberFormat.getIntegerInstance();
		reservationCodeFormat.setGroupingUsed(false);

		jpanel.add(new JLabel("Código de alojamiento"));
		reservationCodeField = new JFormattedTextField(reservationCodeFormat);
		jpanel.add(reservationCodeField);

		JButton checkInButton = new JButton("Registrar");
		checkInButton.addActionListener(e -> doCheckIn());
		jpanel.add(checkInButton);

		add(headerPanel, BorderLayout.NORTH);
		add(jpanel);
	}

	private void doCheckIn() {

		Long reservationCode = ((Number) reservationCodeField.getValue()).longValue();

		try {

			reservationService.doCkeckIn(sessionManager.getLoggedInUser().getId(), reservationId, reservationCode);

			JOptionPane.showMessageDialog(this, "Check-In completado", "Éxito", JOptionPane.INFORMATION_MESSAGE);

			dispose();
			ShowMyReservationsFrame showMyReservationsFrame = context.getBean(ShowMyReservationsFrame.class);
			showMyReservationsFrame.setVisible(true);
		} catch (CannotCheckInException e) {
			JOptionPane.showMessageDialog(this, "Todavía no llegó la fecha de checkIn", "Error",
					JOptionPane.ERROR_MESSAGE);
			dispose();
			ShowMyReservationsFrame showMyReservationsFrame = context.getBean(ShowMyReservationsFrame.class);
			showMyReservationsFrame.setVisible(true);

		} catch (CodeDoesNotMatchException e) {
			JOptionPane.showMessageDialog(this, "El código no cocincide", "Error", JOptionPane.ERROR_MESSAGE);
			dispose();
			ShowMyReservationsFrame showMyReservationsFrame = context.getBean(ShowMyReservationsFrame.class);
			showMyReservationsFrame.setVisible(true);

		} catch (AlreadyCheckedInException e) {
			JOptionPane.showMessageDialog(this, "El checkIn ya está completado", "Error", JOptionPane.ERROR_MESSAGE);
			JOptionPane.showMessageDialog(this, "La reserva ya está registrada");
			dispose();
			ShowMyReservationsFrame showMyReservationsFrame = context.getBean(ShowMyReservationsFrame.class);
			showMyReservationsFrame.setVisible(true);

		} catch (NotMyReservationException e) {
			JOptionPane.showMessageDialog(this, "El código de reserva no le pertenece", "Error",
					JOptionPane.ERROR_MESSAGE);
			JOptionPane.showMessageDialog(this, "Esta reserva no te pertenece");

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error inesperado");
		}
	}

}