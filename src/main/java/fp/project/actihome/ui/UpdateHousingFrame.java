package fp.project.actihome.ui;

import java.awt.GridLayout;
import java.math.BigDecimal;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

@Component
@Profile("!test")
public class UpdateHousingFrame extends JFrame {

	private final HousingService housingService;
	private ApplicationContext context;
	private SessionManager sessionManager;
	private Long housingId;

	private JFormattedTextField numberOfRoomsField;
	private JFormattedTextField pricePerNightField;
	private JTextArea descriptionArea;

	public UpdateHousingFrame(HousingService housingService, ApplicationContext context,
			SessionManager sessionManager) {

		this.housingService = housingService;
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
		jpanel.setBorder(BorderFactory.createTitledBorder("Actualizar alojamiento"));

		NumberFormat roomsFormat = NumberFormat.getIntegerInstance();
		roomsFormat.setGroupingUsed(false);

		NumberFormat pricePerNightFormat = NumberFormat.getNumberInstance();
		pricePerNightFormat.setMaximumFractionDigits(2);
		pricePerNightFormat.setMinimumFractionDigits(2);

		jpanel.add(new JLabel("Nº de habitaciones"));
		numberOfRoomsField = new JFormattedTextField(roomsFormat);
		jpanel.add(numberOfRoomsField);

		jpanel.add(new JLabel("Precio por noche"));
		pricePerNightField = new JFormattedTextField(pricePerNightFormat);
		jpanel.add(pricePerNightField);

		jpanel.add(new JLabel("Descripción"));
		descriptionArea = new JTextArea(5, 20);
		JScrollPane scrollPane = new JScrollPane(descriptionArea);
		jpanel.add(scrollPane);
		descriptionArea.setEditable(true);

		JButton updateButton = new JButton("Confirmar");
		updateButton.addActionListener(e -> update());
		jpanel.add(updateButton);

		add(jpanel);

	}

	private void update() {

		int numberOfRooms = ((Number) numberOfRoomsField.getValue()).intValue();
		BigDecimal pricePerNight = BigDecimal.valueOf(((Number) pricePerNightField.getValue()).doubleValue());

		try {

			housingService.updateHousing(housingId, sessionManager.getLoggedInUser().getId(), numberOfRooms,
					pricePerNight, descriptionArea.getText(), true, true, true);

			JOptionPane.showMessageDialog(this, "Alojamiento registrado con éxito", "Éxito",
					JOptionPane.INFORMATION_MESSAGE);

			dispose();
			HousingDetailsFrame housingDetailsFrame = context.getBean(HousingDetailsFrame.class);
			housingDetailsFrame.loadDetails(housingService.findHousing(housingId));
			housingDetailsFrame.setVisible(true);

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Error en los datos", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

}