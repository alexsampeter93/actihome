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
import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.services.HousingData;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

/**
 * Edición de alojamiento (rol ADMIN propietario).
 *
 * <p>
 * <b>Aquí se corrige el bug B9, que corrompía datos.</b> El formulario mostraba
 * tres campos (habitaciones, precio y descripción) pero llamaba al servicio con
 * {@code updateHousing(..., true, true, true)}: los tres booleanos de la pensión
 * escritos a mano. Resultado: cualquiera que corrigiese una errata en la
 * descripción activaba de paso desayuno, comida y cena, sin verlo y sin poder
 * deshacerlo desde la aplicación.
 *
 * <p>
 * La causa de fondo no era el descuido, sino la firma: diez argumentos
 * posicionales invitan a rellenar los que no interesan con cualquier cosa. La
 * corrección es doble. Por un lado, el servicio recibe ahora un
 * {@link HousingData} donde cada valor lleva su nombre. Por otro —y esto es lo
 * importante— el formulario <b>carga primero el alojamiento y precarga todos sus
 * campos</b>, así que lo que se envía es lo que hay más lo que el usuario haya
 * cambiado. No hay ningún valor inventado en el camino.
 *
 * <p>
 * La maquetación sigue siendo la antigua: esta pantalla se rediseña en la Fase
 * 6. Se ha arreglado ahora porque la Fase 3a cambia la firma del servicio y
 * dejarla escribiendo valores falsos habría sido dejar el bug a sabiendas.
 */
@Component
@Profile("!test")
public class UpdateHousingFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final HousingService housingService;
	private ApplicationContext context;
	private SessionManager sessionManager;
	private Long housingId;

	private JTextField nameField;
	private JComboBox<String> typeField;
	private JFormattedTextField numberOfRoomsField;
	private JFormattedTextField pricePerNightField;
	private JTextArea descriptionArea;
	private JTextField locationField;
	private JCheckBox breakfastBox;
	private JCheckBox lunchBox;
	private JCheckBox dinnerBox;
	private final Map<Amenity, JCheckBox> amenityBoxes = new EnumMap<>(Amenity.class);

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

	/**
	 * Construir va en el constructor; refrescar va aquí.
	 *
	 * <p>
	 * El frame es un singleton de Spring, así que la segunda vez que se abre es la
	 * misma instancia con los valores del alojamiento anterior todavía escritos. Por
	 * eso los campos se recargan al mostrarse, y no al construirse.
	 */
	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			cargarDatosActuales();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("Actihome");
		setSize(500, 620);
		setLocationRelativeTo(null);

		JPanel jpanel = new JPanel(new GridLayout(0, 2, 2, 2));
		jpanel.setBorder(BorderFactory.createTitledBorder("Actualizar alojamiento"));

		NumberFormat roomsFormat = NumberFormat.getIntegerInstance();
		roomsFormat.setGroupingUsed(false);

		NumberFormat pricePerNightFormat = NumberFormat.getNumberInstance();
		pricePerNightFormat.setMaximumFractionDigits(2);
		pricePerNightFormat.setMinimumFractionDigits(2);

		jpanel.add(new JLabel("Nombre"));
		nameField = new JTextField();
		jpanel.add(nameField);

		jpanel.add(new JLabel("Tipo"));
		typeField = new JComboBox<>(UploadHousingFrame.TIPOS);
		jpanel.add(typeField);

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

		for (Amenity amenity : Amenity.values()) {

			if (amenity == Amenity.BREAKFAST) {
				continue;
			}

			jpanel.add(new JLabel(amenity.etiqueta()));
			JCheckBox box = new JCheckBox();
			amenityBoxes.put(amenity, box);
			jpanel.add(box);
		}

		JButton updateButton = new JButton("Confirmar");
		updateButton.addActionListener(e -> update());
		jpanel.add(updateButton);

		add(jpanel);

	}

	/** Rellena el formulario con lo que hay guardado hoy. */
	private void cargarDatosActuales() {

		if (housingId == null) {
			return;
		}

		try {

			Housing housing = housingService.findHousing(housingId);

			nameField.setText(housing.getName());
			typeField.setSelectedItem(housing.getType());
			numberOfRoomsField.setValue(housing.getNumberOfRooms());
			pricePerNightField.setValue(housing.getPricePerNight());
			descriptionArea.setText(housing.getDescription());
			locationField.setText(housing.getLocation());

			breakfastBox.setSelected(housing.isBreakfast());
			lunchBox.setSelected(housing.isLunch());
			dinnerBox.setSelected(housing.isDinner());

			amenityBoxes.forEach((amenity, box) -> box.setSelected(amenity.presenteEn(housing)));

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "No se ha podido cargar el alojamiento", "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void update() {

		int numberOfRooms = ((Number) numberOfRoomsField.getValue()).intValue();
		BigDecimal pricePerNight = BigDecimal.valueOf(((Number) pricePerNightField.getValue()).doubleValue());

		HousingData datos = HousingData
				.basico(null, nameField.getText(), (String) typeField.getSelectedItem(), numberOfRooms, pricePerNight,
						locationField.getText())
				.description(descriptionArea.getText())
				.breakfast(breakfastBox.isSelected())
				.lunch(lunchBox.isSelected())
				.dinner(dinnerBox.isSelected());

		amenityBoxes.forEach((amenity, box) -> datos.amenity(amenity, box.isSelected()));

		try {

			housingService.updateHousing(housingId, sessionManager.getLoggedInUser().getId(), datos);

			JOptionPane.showMessageDialog(this, "Alojamiento actualizado con éxito", "Éxito",
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
