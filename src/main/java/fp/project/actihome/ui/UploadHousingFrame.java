package fp.project.actihome.ui;

import java.awt.GridLayout;
import java.math.BigDecimal;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

@Component
@Profile("!test")
public class UploadHousingFrame extends JFrame {

	private final HousingService housingService;
	private ApplicationContext context;
	private SessionManager sessionManager;

	private JFormattedTextField housingCodeField;
	private JTextField typeField;
	private JFormattedTextField numberOfRoomsField;
	private JFormattedTextField pricePerNightField;
	private JTextArea descriptionArea;
	private JCheckBox breakfastBox;
	private JCheckBox lunchBox;
	private JCheckBox dinnerBox;
	private JTextField locationField;

	public UploadHousingFrame(HousingService housingService, ApplicationContext context,
			SessionManager sessionManager) {

		this.housingService = housingService;
		this.context = context;
		this.sessionManager = sessionManager;
		initUI();
	}

	private void initUI() {

		setTitle("Actihome");
		setSize(500, 500);
		setLocationRelativeTo(null);

		JPanel jpanel = new JPanel(new GridLayout(10, 2, 2, 2));
		jpanel.setBorder(BorderFactory.createTitledBorder("Registrar alojamiento"));

		NumberFormat roomsAndCodeformat = NumberFormat.getIntegerInstance();
		roomsAndCodeformat.setGroupingUsed(false);

		NumberFormat pricePerNightFormat = NumberFormat.getNumberInstance();
		pricePerNightFormat.setMaximumFractionDigits(2);
		pricePerNightFormat.setMinimumFractionDigits(2);

		jpanel.add(new JLabel("Código de alojamiento"));
		housingCodeField = new JFormattedTextField(roomsAndCodeformat);
		jpanel.add(housingCodeField);

		jpanel.add(new JLabel("Tipo"));
		typeField = new JTextField();
		jpanel.add(typeField);

		jpanel.add(new JLabel("Nº de habitaciones"));
		numberOfRoomsField = new JFormattedTextField(roomsAndCodeformat);
		jpanel.add(numberOfRoomsField);

		jpanel.add(new JLabel("Precio por noche"));
		pricePerNightField = new JFormattedTextField(pricePerNightFormat);
		jpanel.add(pricePerNightField);

		jpanel.add(new JLabel("Descripción"));
		descriptionArea = new JTextArea(5, 20);
		JScrollPane scrollPane = new JScrollPane(descriptionArea);
		jpanel.add(scrollPane);
		descriptionArea.setEditable(true);

		jpanel.add(new JLabel("Desayuno"));
		breakfastBox = new JCheckBox();
		jpanel.add(breakfastBox);

		jpanel.add(new JLabel("Comida"));
		lunchBox = new JCheckBox();
		jpanel.add(lunchBox);

		jpanel.add(new JLabel("Cena"));
		dinnerBox = new JCheckBox();
		jpanel.add(dinnerBox);

		jpanel.add(new JLabel("Ubicación"));
		locationField = new JTextField();
		jpanel.add(locationField);

		JButton uploadButton = new JButton("Registrar");
		uploadButton.addActionListener(e -> upload());
		jpanel.add(uploadButton);

		add(jpanel);

	}

	private void upload() {

		Long housingCode = ((Number) housingCodeField.getValue()).longValue();
		int numberOfRooms = ((Number) numberOfRoomsField.getValue()).intValue();
		BigDecimal pricePerNight = BigDecimal.valueOf(((Number) pricePerNightField.getValue()).doubleValue());

		try {

			housingService.uploadHousing(housingCode, typeField.getText(), numberOfRooms, pricePerNight,
					descriptionArea.getText(), breakfastBox.isSelected(), lunchBox.isSelected(), dinnerBox.isSelected(),
					locationField.getText(), sessionManager.getLoggedInUser().getId());

			JOptionPane.showMessageDialog(this, "Alojamiento actualizado con éxito", "Éxito",
					JOptionPane.INFORMATION_MESSAGE);

			dispose();
			ShowHousingsFrame showHousingsFrame = context.getBean(ShowHousingsFrame.class);
			showHousingsFrame.setVisible(true);

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Error en los datos", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}