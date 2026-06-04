package fp.project.actihome.ui;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.services.ReviewService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

@Component
@Profile("!test")
public class UpdateReviewFrame extends JFrame {

	private final ReviewService reviewService;
	private ApplicationContext context;
	private SessionManager sessionManager;
	private Long reviewId;

	private JTextField titleField;
	private JTextArea bodyArea;
	private JSpinner locationScoreField;
	private JSpinner serviceScoreField;
	private JSpinner wifiScoreField;
	private JSpinner foodScoreField;
	private JSpinner cleaningScoreField;

	public UpdateReviewFrame(ReviewService reviewService, ApplicationContext context, SessionManager sessionManager) {
		this.reviewService = reviewService;
		this.context = context;
		this.sessionManager = sessionManager;
		initUi();
	}

	public void setReviewId(Long id) {

		this.reviewId = id;
	}

	private JSpinner createRatingSpinner(SpinnerNumberModel numberSpinner) {
		JSpinner spinner = new JSpinner(numberSpinner);
		spinner.setEditor(new JSpinner.NumberEditor(spinner, "0.0"));
		return spinner;
	}

	private void initUi() {

		setTitle("Actihome");
		setSize(500, 500);
		setLocationRelativeTo(null);

		JPanel jpanel = new JPanel(new GridLayout(10, 2, 2, 2));
		jpanel.setBorder(BorderFactory.createTitledBorder("Editar crítica"));

		jpanel.add(new JLabel("Título"));
		titleField = new JTextField();
		jpanel.add(titleField);

		jpanel.add(new JLabel("Cuerpo"));
		bodyArea = new JTextArea(5, 20);
		JScrollPane scrollPane = new JScrollPane(bodyArea);
		jpanel.add(scrollPane);
		bodyArea.setEditable(true);

		jpanel.add((new JLabel("Nota de ubicación")));
		locationScoreField = createRatingSpinner(new SpinnerNumberModel(0, 0, 5, 0.1));
		jpanel.add(locationScoreField);

		jpanel.add((new JLabel("Nota de servicio")));
		serviceScoreField = createRatingSpinner(new SpinnerNumberModel(0, 0, 5, 0.1));
		jpanel.add(serviceScoreField);

		jpanel.add((new JLabel("Nota de wifi")));
		wifiScoreField = createRatingSpinner(new SpinnerNumberModel(0, 0, 5, 0.1));
		jpanel.add(wifiScoreField);

		jpanel.add((new JLabel("Nota de comida")));
		foodScoreField = createRatingSpinner(new SpinnerNumberModel(0, 0, 5, 0.1));
		jpanel.add(foodScoreField);

		jpanel.add((new JLabel("Nota de limpieza")));
		cleaningScoreField = createRatingSpinner(new SpinnerNumberModel(0, 0, 5, 0.1));
		jpanel.add(cleaningScoreField);

		JButton updateButton = new JButton("Actualizar");
		updateButton.addActionListener(e -> update());
		jpanel.add(updateButton);

		add(jpanel);

	}

	private void update() {

		double locationScore = ((Number) locationScoreField.getValue()).doubleValue();
		double serviceScore = ((Number) serviceScoreField.getValue()).doubleValue();
		double wifiScore = ((Number) wifiScoreField.getValue()).doubleValue();
		double foodScore = ((Number) foodScoreField.getValue()).doubleValue();
		double cleaningScore = ((Number) cleaningScoreField.getValue()).doubleValue();

		try {

			reviewService.updateReview(reviewId, sessionManager.getLoggedInUser().getId(), titleField.getText(),
					bodyArea.getText(), locationScore, serviceScore, wifiScore, foodScore, cleaningScore);

			JOptionPane.showMessageDialog(this, "Reseña actualizada correctamente", "Éxito",
					JOptionPane.INFORMATION_MESSAGE);

			dispose();
			ReviewDetailsFrame reviewDetailsFrame = context.getBean(ReviewDetailsFrame.class);
			reviewDetailsFrame.loadDetails(reviewService.findReview(reviewId));
			reviewDetailsFrame.setVisible(true);

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Error en los datos", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}