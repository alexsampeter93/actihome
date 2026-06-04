package fp.project.actihome.ui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.entities.Review;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.ReviewService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

@Component
@Profile("!test")
@Lazy
public class ShowReviewsFrame extends JFrame {

	private final ReviewService reviewService;
	private ApplicationContext context;
	private SessionManager sessionManager;
	private HeaderPanel headerPanel;

	private DefaultTableModel reviewsModel;
	private JTable reviewsTable;
	private JButton button;
	private Long housingId;

	public ShowReviewsFrame(ReviewService reviewService, ApplicationContext context, SessionManager sessionManager,
			HeaderPanel headerPanel) throws InstanceNotFoundException {

		this.reviewService = reviewService;
		this.context = context;
		this.sessionManager = sessionManager;
		this.headerPanel = headerPanel;
		initUI();
	}

	public void setHousingId(Long id) {

		this.housingId = id;
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			try {
				loadReviews();
			} catch (InstanceNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			headerPanel.refresh();
			refreshActions();
		}

		super.setVisible(visible);
	}

	private void initUI() throws InstanceNotFoundException {

		setTitle("Actihome");
		setSize(500, 500);
		setLocationRelativeTo(null);

		JPanel jpanel = new JPanel(new BorderLayout());
		jpanel.setBorder(BorderFactory.createTitledBorder("Críticas"));

		String[] columns = { "ID", "Autor", "Título", "Nota total" };

		reviewsModel = new DefaultTableModel(columns, 0) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		reviewsTable = new JTable(reviewsModel);
		reviewsTable.getColumnModel().getColumn(0).setMinWidth(0);
		reviewsTable.getColumnModel().getColumn(0).setMaxWidth(0);
		reviewsTable.getColumnModel().getColumn(0).setPreferredWidth(0);
		reviewsTable.setRowHeight(40);
		reviewsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		reviewsTable.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {

				if (e.getClickCount() == 2) {
					try {
						showReviewDetails();
					} catch (InstanceNotFoundException e1) { // TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(reviewsTable);

		jpanel.add(scrollPane, BorderLayout.CENTER);

		button = new JButton("Publicar reseña");
		button.setVisible(false);
		jpanel.add(button, BorderLayout.SOUTH);
		add(headerPanel, BorderLayout.NORTH);
		add(jpanel);

	}

	private void loadReviews() throws InstanceNotFoundException {

		reviewsModel.setRowCount(0);

		ArrayList<Review> reviewsList = reviewService.showHousingReviews(housingId);

		for (Review review : reviewsList) {
			reviewsModel.addRow(new Object[] { review.getId(), review.getAuthor().getUsername(), review.getTitle(),
					review.getTotalScore() });

		}
	}

	private void publish() {

		dispose();
		PublishReviewFrame publishReviewFrame = context.getBean(PublishReviewFrame.class);
		publishReviewFrame.setHousingId(housingId);
		publishReviewFrame.setVisible(true);
	}

	private void showReviewDetails() throws InstanceNotFoundException {

		int selectedRow = reviewsTable.getSelectedRow();

		if (selectedRow == -1) {

			return;
		}

		Long reviewId = (Long) reviewsTable.getValueAt(selectedRow, 0);

		Review review = reviewService.findReview(reviewId);

		dispose();
		ReviewDetailsFrame reviewDetailsFrame = context.getBean(ReviewDetailsFrame.class);
		reviewDetailsFrame.loadDetails(review);
		reviewDetailsFrame.setVisible(true);

	}

	private void refreshActions() {

		if (sessionManager.getLoggedInUser().getRole() == RoleType.CUSTOMER) {

			button.addActionListener(e -> publish());
			button.setSize(10, 20);
			button.setVisible(true);
		} else {
			button.setVisible(false);
		}
	}
}