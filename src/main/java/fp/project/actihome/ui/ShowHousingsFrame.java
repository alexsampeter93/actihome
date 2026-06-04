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

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

@Component
@Profile("!test")
@Lazy
public class ShowHousingsFrame extends JFrame {

	private final HousingService housingService;
	private ApplicationContext context;
	private SessionManager sessionManager;
	private HeaderPanel headerPanel;

	private DefaultTableModel housingsModel;
	private JTable housingsTable;
	private JButton showButton;
	private JButton uploadButton;

	public ShowHousingsFrame(HousingService housingService, ApplicationContext context, SessionManager sessionManager,
			HeaderPanel headerPanel) {

		this.housingService = housingService;
		this.context = context;
		this.sessionManager = sessionManager;
		this.headerPanel = headerPanel;
		initUI();
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			loadHousings();
			headerPanel.refresh();
			refreshActions();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("Actihome");
		setSize(500, 500);
		setLocationRelativeTo(null);

		JPanel buttonPanel = new JPanel();
		JPanel jpanel = new JPanel(new BorderLayout());
		jpanel.setBorder(BorderFactory.createTitledBorder("Alojamientos"));

		String[] columns = { "ID", "Código", "Dueño", "Tipo", "Ubicación" };

		housingsModel = new DefaultTableModel(columns, 0) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		housingsTable = new JTable(housingsModel);
		housingsTable.getColumnModel().getColumn(0).setMinWidth(0);
		housingsTable.getColumnModel().getColumn(0).setMaxWidth(0);
		housingsTable.getColumnModel().getColumn(0).setPreferredWidth(0);
		housingsTable.setRowHeight(40);
		housingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		housingsTable.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {

				if (e.getClickCount() == 2) {
					try {
						showHousingDetails();
					} catch (InstanceNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		JScrollPane scrollPane = new JScrollPane(housingsTable);

		jpanel.add(scrollPane, BorderLayout.CENTER);

		showButton = new JButton();
		buttonPanel.add(showButton, BorderLayout.WEST);

		uploadButton = new JButton();
		buttonPanel.add(uploadButton, BorderLayout.EAST);

		add(headerPanel, BorderLayout.NORTH);
		add(buttonPanel, BorderLayout.SOUTH);
		add(jpanel);

	}

	private void loadHousings() {

		housingsModel.setRowCount(0);

		ArrayList<Housing> housingsList = housingService.showHousings();

		for (Housing housing : housingsList) {
			housingsModel.addRow(new Object[] { housing.getId(), housing.getHousingCode(),
					housing.getOwner().getUsername(), housing.getType(), housing.getLocation() });

		}
	}

	private void upload() {

		dispose();
		UploadHousingFrame uploadHousingFrame = context.getBean(UploadHousingFrame.class);
		uploadHousingFrame.setVisible(true);

	}

	private void showMyReservations() {

		dispose();
		ShowMyReservationsFrame showMyReservationsFrame = context.getBean(ShowMyReservationsFrame.class);
		showMyReservationsFrame.setVisible(true);

	}

	private void showHousingDetails() throws InstanceNotFoundException {

		int selectedRow = housingsTable.getSelectedRow();

		if (selectedRow == -1) {

			return;
		}

		Long housingId = (Long) housingsTable.getValueAt(selectedRow, 0);

		Housing housing = housingService.findHousing(housingId);

		dispose();
		HousingDetailsFrame housingDetailsFrame = context.getBean(HousingDetailsFrame.class);
		housingDetailsFrame.loadDetails(housing);
		housingDetailsFrame.setVisible(true);
	}

	private void refreshActions() {

		if (sessionManager.getLoggedInUser().getRole() == RoleType.ADMIN) {
			uploadButton.setText("Registrar alojamiento");
			uploadButton.addActionListener(e -> upload());
			uploadButton.setVisible(true);
			uploadButton.setSize(10, 20);
			showButton.setVisible(false);

		}

		if (sessionManager.getLoggedInUser().getRole() == RoleType.CUSTOMER) {
			showButton.setText("Ver mis reservas");
			showButton.addActionListener(e -> showMyReservations());
			showButton.setVisible(true);
			showButton.setSize(10, 20);
			uploadButton.setVisible(false);

		}
	}

}