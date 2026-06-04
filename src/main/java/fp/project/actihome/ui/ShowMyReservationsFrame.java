package fp.project.actihome.ui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
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

import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.ReservationService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

@Component
@Profile("!test")
@Lazy
public class ShowMyReservationsFrame extends JFrame {

	private final ReservationService reservationService;
	private ApplicationContext context;
	private SessionManager sessionManager;
	private Long housingId;
	private HeaderPanel headerPanel;

	private DefaultTableModel reservationsModel;
	private JTable reservationsTable;

	public ShowMyReservationsFrame(ReservationService reservationService, ApplicationContext context,
			SessionManager sessionManager, HeaderPanel headerPanel) {

		this.reservationService = reservationService;
		this.context = context;
		this.sessionManager = sessionManager;
		this.headerPanel = headerPanel;
		initUI();
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			try {
				loadReservations();
			} catch (InstanceNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			headerPanel.refresh();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("Actihome");
		setSize(500, 500);
		setLocationRelativeTo(null);

		JPanel jpanel = new JPanel(new BorderLayout());
		jpanel.setBorder(BorderFactory.createTitledBorder("Mis reservas"));

		String[] columns = { "ID", "Código de reserva", "Fecha check-In", "Fecha check-Out", "Método de pago",
				"Fecha de reserva", "Precio total", "Check-in" };

		reservationsModel = new DefaultTableModel(columns, 0) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		reservationsTable = new JTable(reservationsModel);
		reservationsTable.getColumnModel().getColumn(0).setMinWidth(0);
		reservationsTable.getColumnModel().getColumn(0).setMaxWidth(0);
		reservationsTable.getColumnModel().getColumn(0).setPreferredWidth(0);
		reservationsTable.setRowHeight(40);
		reservationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		reservationsTable.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {

				if (e.getClickCount() == 2) {
					doCheckIn();
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(reservationsTable);

		jpanel.add(scrollPane, BorderLayout.CENTER);

		add(headerPanel, BorderLayout.NORTH);
		add(jpanel);

	}

	private void loadReservations() throws InstanceNotFoundException {

		reservationsModel.setRowCount(0);

		ArrayList<Reservation> reservationsList = reservationService
				.showMyReservations(sessionManager.getLoggedInUser().getId());

		for (Reservation reservation : reservationsList) {

			String checkedIn = "";
			if (reservation.isCheckedIn()) {
				checkedIn = "Hecho";
			} else {
				checkedIn = "Pendiente";
			}

			reservationsModel.addRow(new Object[] { reservation.getId(), reservation.getReservationCode(),
					reservation.getCheckIn(), reservation.getCheckOut(), reservation.getPaymentMethod(),
					reservation.getReservationDate(), reservation.getTotalPrice(), checkedIn });

		}
	}

	private void doCheckIn() {

		int selectedRow = reservationsTable.getSelectedRow();

		if (selectedRow == -1) {

			return;
		}

		Long reservationId = (Long) reservationsTable.getValueAt(selectedRow, 0);

		dispose();
		DoCheckInFrame doCheckInFrame = context.getBean(DoCheckInFrame.class);
		doCheckInFrame.setReservationId(reservationId);
		doCheckInFrame.setVisible(true);

	}
}