package fp.project.actihome.ui;

import java.awt.GridLayout;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

@Component
@Profile("!test")
@Lazy
public class TradeHousingsFrame extends JFrame {

	private final HousingService housingService;
	private ApplicationContext context;
	private SessionManager sessionManager;
	private Long housingId;

	private JFormattedTextField housingToTradeField;

	public TradeHousingsFrame(HousingService housingService, ApplicationContext context,
			SessionManager sessionManager) {

		this.housingService = housingService;
		this.context = context;
		this.sessionManager = sessionManager;
		initUI();
	}

	public void setHousingId(Long housingId) {

		this.housingId = housingId;
	}

	private void initUI() {

		setTitle("Actihome");
		setSize(500, 500);
		setLocationRelativeTo(null);

		JPanel jpanel = new JPanel(new GridLayout(10, 2, 2, 2));
		jpanel.setBorder(BorderFactory.createTitledBorder("Intercambiar alojamiento"));

		NumberFormat codeformat = NumberFormat.getIntegerInstance();
		codeformat.setGroupingUsed(false);

		jpanel.add(new JLabel("Código de alojamiento a intercambiar"));
		housingToTradeField = new JFormattedTextField(codeformat);
		jpanel.add(housingToTradeField);

		JButton tradeButton = new JButton("Confirmar");
		tradeButton.addActionListener(e -> trade());
		jpanel.add(tradeButton);

		add(jpanel);
	}

	private void trade() {

		Long housingToTradeCode = ((Number) housingToTradeField.getValue()).longValue();

		try {

			housingService.tradeHousings(sessionManager.getLoggedInUser().getId(), housingId, housingToTradeCode);

			JOptionPane.showMessageDialog(this, "Intercambio realizado", "Éxito", JOptionPane.INFORMATION_MESSAGE);

			dispose();
			ShowHousingsFrame showHousingsFrame = context.getBean(ShowHousingsFrame.class);
			showHousingsFrame.setVisible(true);

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "No se pudo llevar a cabo el intercambio", "Error",
					JOptionPane.ERROR_MESSAGE);
			dispose();
			ShowHousingsFrame showHousingsFrame = context.getBean(ShowHousingsFrame.class);
			showHousingsFrame.setVisible(true);
		}
	}
}