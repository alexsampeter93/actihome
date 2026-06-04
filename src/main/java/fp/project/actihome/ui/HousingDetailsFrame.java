package fp.project.actihome.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

@Component
@Profile("!test")
@Lazy
public class HousingDetailsFrame extends JFrame {

	private ApplicationContext context;
	private SessionManager sessionManager;
	private Housing housing;
	private HeaderPanel headerPanel;

	private JLabel housingCodeLabel;
	private JLabel typeLabel;
	private JLabel scoreLabel;
	private JLabel numberOfRoomsLabel;
	private JLabel pricePerNightLabel;
	private JLabel descriptionArea;
	private JLabel breakfastLabel;
	private JLabel lunchLabel;
	private JLabel dinnerLabel;
	private JLabel availableLabel;
	private JLabel locationLabel;
	private JButton updateButton;
	private JButton reserveButton;
	private JButton tradeButton;

	public HousingDetailsFrame(ApplicationContext context, SessionManager sessionManager,
			HeaderPanel headerPanel) {

		
		this.context = context;
		this.sessionManager = sessionManager;
		this.headerPanel = headerPanel;
		;
		initUI();
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			loadDetails(housing);
			headerPanel.refresh();
			refreshActions();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("Actihome");
		setSize(500, 500);
		setLocationRelativeTo(null);

		JPanel jpanel = new JPanel(new BorderLayout());
		jpanel.setBorder(BorderFactory.createTitledBorder("Datos del alojamiento"));

		JPanel dataPanel = new JPanel(new GridLayout(0, 2, 10, 10));

		JPanel buttonPanel = new JPanel(new BorderLayout());

		NumberFormat roomsAndCodeformat = NumberFormat.getIntegerInstance();
		roomsAndCodeformat.setGroupingUsed(false);

		NumberFormat pricePerNightFormat = NumberFormat.getNumberInstance();
		pricePerNightFormat.setMaximumFractionDigits(2);
		pricePerNightFormat.setMinimumFractionDigits(2);

		dataPanel.add(new JLabel("Código de alojamiento"));
		housingCodeLabel = new JLabel();
		dataPanel.add(housingCodeLabel);

		dataPanel.add(new JLabel("Tipo"));
		typeLabel = new JLabel();
		dataPanel.add(typeLabel);

		dataPanel.add(new JLabel("Calificación"));
		scoreLabel = new JLabel();
		dataPanel.add(scoreLabel);

		dataPanel.add(new JLabel("Nº de habitaciones"));
		numberOfRoomsLabel = new JLabel();
		dataPanel.add(numberOfRoomsLabel);

		dataPanel.add(new JLabel("Precio por noche"));
		pricePerNightLabel = new JLabel();
		dataPanel.add(pricePerNightLabel);

		dataPanel.add(new JLabel("Descripción"));
		descriptionArea = new JLabel();
		dataPanel.add(descriptionArea);

		dataPanel.add(new JLabel("Desayuno incluído?"));
		breakfastLabel = new JLabel();
		dataPanel.add(breakfastLabel);

		dataPanel.add(new JLabel("Comida incluída?"));
		lunchLabel = new JLabel();
		dataPanel.add(lunchLabel);

		dataPanel.add(new JLabel("Cena incluída?"));
		dinnerLabel = new JLabel();
		dataPanel.add(dinnerLabel);

		dataPanel.add(new JLabel("Disponible para reserva?"));
		availableLabel = new JLabel();
		dataPanel.add(availableLabel);

		dataPanel.add(new JLabel("Ubicación"));
		locationLabel = new JLabel();
		dataPanel.add(locationLabel);

		JButton reviewsButton = new JButton("Ver reseñas");
		reviewsButton.addActionListener(e -> showReviews());
		buttonPanel.add(reviewsButton, BorderLayout.WEST);

		updateButton = new JButton("Actualizar alojamiento");
		updateButton.addActionListener(e -> update());
		updateButton.setVisible(false);
		buttonPanel.add(updateButton, BorderLayout.CENTER);

		reserveButton = new JButton("Reservar");
		buttonPanel.add(reserveButton, BorderLayout.EAST);
		reserveButton.setVisible(false);

		tradeButton = new JButton("Intercambiar");
		buttonPanel.add(tradeButton, BorderLayout.NORTH);
		tradeButton.setVisible(false);

		jpanel.add(dataPanel, BorderLayout.CENTER);
		jpanel.add(buttonPanel, BorderLayout.SOUTH);
		add(headerPanel, BorderLayout.NORTH);
		add(jpanel);

	}

	public void loadDetails(Housing housing) {

		this.housing = housing;

		housingCodeLabel.setText(String.valueOf(housing.getHousingCode()));
		typeLabel.setText(housing.getType());

		if (housing.getScore() == null) {
			scoreLabel.setText("Sin calificación");
		} else {
			scoreLabel.setText(String.valueOf(housing.getScore()));
		}

		if (housing.isBreakfast()) {
			breakfastLabel.setText("Sí");
		} else {
			breakfastLabel.setText("No");
		}

		if (housing.isLunch()) {
			lunchLabel.setText("Sí");
		} else {
			lunchLabel.setText("No");
		}

		if (housing.isBreakfast()) {
			dinnerLabel.setText("Sí");
		} else {
			dinnerLabel.setText("No");
		}

		if (housing.isAvailable()) {
			availableLabel.setText("Sí");
		} else {
			availableLabel.setText("No");
		}

		numberOfRoomsLabel.setText(String.valueOf(housing.getNumberOfRooms()));
		pricePerNightLabel.setText(String.valueOf(housing.getPricePerNight()));
		descriptionArea.setText(housing.getDescription());
		locationLabel.setText(housing.getLocation());

		if (sessionManager.getLoggedInUser().getId().equals(housing.getOwner().getId())) {
			updateButton.setVisible(true);
		} else {
			updateButton.setVisible(false);
		}
	}

	private void update() {

		dispose();
		UpdateHousingFrame updateHousingFrame = context.getBean(UpdateHousingFrame.class);
		updateHousingFrame.setHousingId(housing.getId());
		updateHousingFrame.setVisible(true);
	}

	private void showReviews() {

		dispose();
		ShowReviewsFrame showReviewsFrame = context.getBean(ShowReviewsFrame.class);
		showReviewsFrame.setHousingId(housing.getId());
		showReviewsFrame.setVisible(true);
	}

	private void reserve() {

		dispose();
		ReserveHousingFrame reserveHousingFrame = context.getBean(ReserveHousingFrame.class);
		reserveHousingFrame.setHousingId(housing.getId());
		reserveHousingFrame.setVisible(true);

	}

	private void refreshActions() {

		if (sessionManager.getLoggedInUser().getRole() == RoleType.CUSTOMER) {
			reserveButton.addActionListener(e -> reserve());
			reserveButton.setVisible(true);
			tradeButton.setVisible(false);
		}

		if (sessionManager.getLoggedInUser().getRole() == RoleType.ADMIN
				&& sessionManager.getLoggedInUser().getId().equals(housing.getOwner().getId())) {

			tradeButton.addActionListener(e -> trade());
			tradeButton.setVisible(true);
			reserveButton.setVisible(false);
		}
	}

	private void trade() {

		dispose();
		TradeHousingsFrame tradeHousingsFrame = context.getBean(TradeHousingsFrame.class);
		tradeHousingsFrame.setHousingId(housing.getId());
		tradeHousingsFrame.setVisible(true);
	}
}