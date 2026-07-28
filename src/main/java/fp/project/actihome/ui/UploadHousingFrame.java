package fp.project.actihome.ui;

import java.awt.GridLayout;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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

import fp.project.actihome.model.entities.Amenity;
import fp.project.actihome.model.services.HousingData;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

/**
 * Alta de alojamiento (rol ADMIN).
 *
 * <p>
 * Sigue con la maquetación antigua: esta pantalla se rediseña en la Fase 6. Lo
 * que cambia en la Fase 3a son los <b>datos</b> que recoge —nombre, tipo como
 * categoría cerrada y comodidades—, porque sin ellos un alojamiento creado desde
 * la aplicación saldría en el catálogo sin título y sin ningún chip, y no
 * aparecería en ningún filtro.
 */
@Component
@Profile("!test")
public class UploadHousingFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	/**
	 * Categorías de alojamiento del diseño.
	 *
	 * <p>
	 * Antes el tipo era un campo de texto libre, y por eso los datos de ejemplo
	 * tenían tipos como "Casa en la playa" o "Casa con piscina": cada uno un valor
	 * distinto. El catálogo filtra por tipo con chips fijos, y un filtro por chips
	 * solo funciona si el conjunto de valores posibles es cerrado. Con texto libre,
	 * un alojamiento nuevo simplemente no aparecería bajo ningún chip.
	 */
	static final String[] TIPOS = { "Casa", "Apartamento", "Villa", "Cabaña" };

	private final HousingService housingService;
	private ApplicationContext context;
	private SessionManager sessionManager;

	private JFormattedTextField housingCodeField;
	private JTextField nameField;
	private JComboBox<String> typeField;
	private JFormattedTextField numberOfRoomsField;
	private JFormattedTextField pricePerNightField;
	private JTextArea descriptionArea;
	private JCheckBox breakfastBox;
	private JCheckBox lunchBox;
	private JCheckBox dinnerBox;
	private JTextField locationField;
	private final Map<Amenity, JCheckBox> amenityBoxes = new EnumMap<>(Amenity.class);

	public UploadHousingFrame(HousingService housingService, ApplicationContext context,
			SessionManager sessionManager) {

		this.housingService = housingService;
		this.context = context;
		this.sessionManager = sessionManager;
		initUI();
	}

	private void initUI() {

		setTitle("Actihome");
		setSize(500, 640);
		setLocationRelativeTo(null);

		// GridLayout(0, 2): cero filas significa "las que hagan falta". Antes estaba
		// fijado a 10 y añadir un campo más habría descuadrado la rejilla en silencio.
		JPanel jpanel = new JPanel(new GridLayout(0, 2, 2, 2));
		jpanel.setBorder(BorderFactory.createTitledBorder("Registrar alojamiento"));

		NumberFormat roomsAndCodeformat = NumberFormat.getIntegerInstance();
		roomsAndCodeformat.setGroupingUsed(false);

		NumberFormat pricePerNightFormat = NumberFormat.getNumberInstance();
		pricePerNightFormat.setMaximumFractionDigits(2);
		pricePerNightFormat.setMinimumFractionDigits(2);

		jpanel.add(new JLabel("Código de alojamiento"));
		housingCodeField = new JFormattedTextField(roomsAndCodeformat);
		jpanel.add(housingCodeField);

		jpanel.add(new JLabel("Nombre"));
		nameField = new JTextField();
		jpanel.add(nameField);

		jpanel.add(new JLabel("Tipo"));
		typeField = new JComboBox<>(TIPOS);
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

		jpanel.add(new JLabel("Ubicación"));
		locationField = new JTextField();
		jpanel.add(locationField);

		jpanel.add(new JLabel("Desayuno"));
		breakfastBox = new JCheckBox();
		jpanel.add(breakfastBox);

		jpanel.add(new JLabel("Comida"));
		lunchBox = new JCheckBox();
		jpanel.add(lunchBox);

		jpanel.add(new JLabel("Cena"));
		dinnerBox = new JCheckBox();
		jpanel.add(dinnerBox);

		// El desayuno ya tiene su casilla arriba, en la pensión: es el mismo campo, y
		// dos casillas para un solo dato solo sirven para que se contradigan.
		for (Amenity amenity : Amenity.values()) {

			if (amenity == Amenity.BREAKFAST) {
				continue;
			}

			jpanel.add(new JLabel(amenity.etiqueta()));
			JCheckBox box = new JCheckBox();
			amenityBoxes.put(amenity, box);
			jpanel.add(box);
		}

		JButton uploadButton = new JButton("Registrar");
		uploadButton.addActionListener(e -> upload());
		jpanel.add(uploadButton);

		add(jpanel);

	}

	private void upload() {

		Long housingCode = ((Number) housingCodeField.getValue()).longValue();
		int numberOfRooms = ((Number) numberOfRoomsField.getValue()).intValue();
		BigDecimal pricePerNight = BigDecimal.valueOf(((Number) pricePerNightField.getValue()).doubleValue());

		HousingData datos = HousingData
				.basico(housingCode, nameField.getText(), (String) typeField.getSelectedItem(), numberOfRooms,
						pricePerNight, locationField.getText())
				.description(descriptionArea.getText())
				.breakfast(breakfastBox.isSelected())
				.lunch(lunchBox.isSelected())
				.dinner(dinnerBox.isSelected());

		amenityBoxes.forEach((amenity, box) -> datos.amenity(amenity, box.isSelected()));

		try {

			housingService.uploadHousing(datos, sessionManager.getLoggedInUser().getId());

			JOptionPane.showMessageDialog(this, "Alojamiento registrado con éxito", "Éxito",
					JOptionPane.INFORMATION_MESSAGE);

			dispose();
			ShowHousingsFrame showHousingsFrame = context.getBean(ShowHousingsFrame.class);
			showHousingsFrame.setVisible(true);

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Error en los datos", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
