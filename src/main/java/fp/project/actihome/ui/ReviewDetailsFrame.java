package fp.project.actihome.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.entities.Review;
import fp.project.actihome.model.services.ReviewService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

@Component
@Profile("!test")
@Lazy
public class ReviewDetailsFrame extends JFrame {

	private final ReviewService reviewService;
	private ApplicationContext context;
	private SessionManager sessionManager;
	private HeaderPanel headerPanel;
	private Review review;

	private JLabel titleLabel;
	private JLabel bodyArea;
	private JLabel locationScoreLabel;
	private JLabel serviceScoreLabel;
	private JLabel wifiScoreLabel;
	private JLabel foodScoreLabel;
	private JLabel cleaningScoreLabel;
	private JLabel totalScoreLabel;
	private JButton updateButton;

	public ReviewDetailsFrame(ReviewService reviewService, ApplicationContext context, SessionManager sessionManager,
			HeaderPanel headerPanel) {

		this.reviewService = reviewService;
		this.context = context;
		this.sessionManager = sessionManager;
		this.headerPanel = headerPanel;
		initUI();
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {

			loadDetails(review);
			headerPanel.refresh();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("Actihome");
		setSize(500, 500);
		setLocationRelativeTo(null);

		JPanel jpanel = new JPanel(new BorderLayout());
		jpanel.setBorder(BorderFactory.createTitledBorder("Datos de la reseña"));

		JPanel dataPanel = new JPanel(new GridLayout(0, 2, 10, 10));

		JPanel buttonPanel = new JPanel(new BorderLayout());

		NumberFormat roomsAndCodeformat = NumberFormat.getIntegerInstance();
		roomsAndCodeformat.setGroupingUsed(false);

		NumberFormat pricePerNightFormat = NumberFormat.getNumberInstance();
		pricePerNightFormat.setMaximumFractionDigits(2);
		pricePerNightFormat.setMinimumFractionDigits(2);

		dataPanel.add(new JLabel("Título"));
		titleLabel = new JLabel();
		dataPanel.add(titleLabel);

		dataPanel.add(new JLabel("Cuerpo"));
		bodyArea = new JLabel();
		dataPanel.add(bodyArea);

		dataPanel.add(new JLabel("Calificación de ubicación"));
		locationScoreLabel = new JLabel();
		dataPanel.add(locationScoreLabel);

		dataPanel.add(new JLabel("Calificación de servicio"));
		serviceScoreLabel = new JLabel();
		dataPanel.add(serviceScoreLabel);

		dataPanel.add(new JLabel("Calificación de wifi"));
		wifiScoreLabel = new JLabel();
		dataPanel.add(wifiScoreLabel);

		dataPanel.add(new JLabel("Calificación de comida"));
		foodScoreLabel = new JLabel();
		dataPanel.add(foodScoreLabel);

		dataPanel.add(new JLabel("Calificación de limpieza"));
		cleaningScoreLabel = new JLabel();
		dataPanel.add(cleaningScoreLabel);

		dataPanel.add(new JLabel("Calificación total"));
		totalScoreLabel = new JLabel();
		dataPanel.add(totalScoreLabel);

		updateButton = new JButton("Actualizar reseña");
		updateButton.addActionListener(e -> update());
		updateButton.setVisible(false);
		buttonPanel.add(updateButton, BorderLayout.CENTER);

		jpanel.add(dataPanel, BorderLayout.CENTER);
		jpanel.add(buttonPanel, BorderLayout.SOUTH);
		add(headerPanel, BorderLayout.NORTH);
		add(jpanel);

	}

	private void update() {

		dispose();
		UpdateReviewFrame updateReviewFrame = context.getBean(UpdateReviewFrame.class);
		updateReviewFrame.setReviewId(review.getId());
		updateReviewFrame.setVisible(true);
	}

	public void loadDetails(Review review) {

		this.review = review;

		titleLabel.setText(review.getTitle());
		bodyArea.setText(review.getBody());
		locationScoreLabel.setText(String.valueOf(review.getLocationScore()));
		serviceScoreLabel.setText(String.valueOf(review.getServiceScore()));
		wifiScoreLabel.setText(String.valueOf(review.getWifiScore()));
		foodScoreLabel.setText(String.valueOf(review.getFoodScore()));
		cleaningScoreLabel.setText(String.valueOf(review.getCleaningScore()));
		totalScoreLabel.setText(String.valueOf(review.getTotalScore()));

		if (sessionManager.getLoggedInUser().getId().equals(review.getAuthor().getId())) {
			updateButton.setVisible(true);
		} else {
			updateButton.setVisible(false);
		}

	}
}