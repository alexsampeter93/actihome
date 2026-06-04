package fp.project.actihome.ui;

import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.services.ReservationService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

@Component
@Profile("!test")
public class ReserveHousingFrame extends JFrame {

	private final ReservationService reservationService;
	private ApplicationContext context;
	private SessionManager sessionManager;
	private Long housingId;

	private JSpinner checkInSpinner;
	private JSpinner checkOutSpinner;
	private JTextField creditCardNumberField;

	public ReserveHousingFrame(ReservationService reservationService, ApplicationContext context,
			SessionManager sessionManager) {

		this.reservationService = reservationService;
		this.context = context;
		this.sessionManager = sessionManager;
		initUI();
	}

	public void setHousingId(Long id) {

		this.housingId = id;
	}

	private void initUI() {

		setTitle("Actihome");
		setSize(500, 500);
		setLocationRelativeTo(null);

		JPanel jpanel = new JPanel(new GridLayout(10, 2, 2, 2));
		jpanel.setBorder(BorderFactory.createTitledBorder("Reservar alojamiento"));

		jpanel.add(new JLabel("Fecha de check-In"));
		checkInSpinner = new JSpinner(new SpinnerDateModel());
		checkInSpinner.setEditor(new JSpinner.DateEditor(checkInSpinner, "dd/MM/yyyy"));
		jpanel.add(checkInSpinner);

		jpanel.add(new JLabel("Fecha de check-In"));
		checkOutSpinner = new JSpinner(new SpinnerDateModel());
		checkOutSpinner.setEditor(new JSpinner.DateEditor(checkOutSpinner, "dd/MM/yyyy"));
		jpanel.add(checkOutSpinner);

		jpanel.add(new JLabel("Tarjeta de crédito"));
		creditCardNumberField = new JTextField();
		jpanel.add(creditCardNumberField);

		JButton reserveButton = new JButton("Reservar");
		reserveButton.addActionListener(e -> reserve());
		jpanel.add(reserveButton);

		add(jpanel);
	}

	private void reserve() {

		Date checkInValue = (Date) checkInSpinner.getValue();
		LocalDateTime checkIn = checkInValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		Date checkOutValue = (Date) checkOutSpinner.getValue();
		LocalDateTime checkOut = checkOutValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		String creditCardNumber = creditCardNumberField.getText();

		try {
			reservationService.reserveHousing(sessionManager.getLoggedInUser().getId(), housingId, creditCardNumber,
					checkIn, checkOut);

			JOptionPane.showMessageDialog(this, "Reserva lista", "Éxito", JOptionPane.INFORMATION_MESSAGE);

			dispose();
			ShowMyReservationsFrame showMyReservationsFrame = context.getBean(ShowMyReservationsFrame.class);
			showMyReservationsFrame.setVisible(true);

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Error en la reserva, revise los campos", "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}
}