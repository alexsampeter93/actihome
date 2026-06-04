package fp.project.actihome.ui;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

@Component
@Profile("!test")
public class ChangePasswordFrame extends JFrame {

	private final UserService userService;
	private final ApplicationContext context;
	private final SessionManager sessionManager;

	// Campos que introducirá el usuario:
	private JPasswordField oldPasswordField;
	private JPasswordField newPasswordField;
	private JPasswordField repeatPasswordField;

	public ChangePasswordFrame(UserService userService, ApplicationContext context, SessionManager sessionManager) {
		this.userService = userService;
		this.context = context;
		this.sessionManager = sessionManager;
		initUI();
	}

	private void initUI() {

		setTitle("Actihome");
		setSize(500, 500);
		setLocationRelativeTo(null);

		JPanel jpanel = new JPanel(new GridLayout(10, 2, 2, 2));
		jpanel.setBorder(BorderFactory.createTitledBorder("Cambiar contraseña "));

		jpanel.add(new JLabel("Antigua contraseña"));
		oldPasswordField = new JPasswordField();
		jpanel.add(oldPasswordField);

		jpanel.add(new JLabel("Nueva contraseña"));
		newPasswordField = new JPasswordField();
		jpanel.add(newPasswordField);

		jpanel.add(new JLabel("Repetir nueva contraseña"));
		repeatPasswordField = new JPasswordField();
		jpanel.add(repeatPasswordField);

		JButton changeButton = new JButton("Confirmar");
		changeButton.addActionListener(e -> changePassword());
		jpanel.add(changeButton);

		add(jpanel);

	}

	private void changePassword() {

		String oldPassword = new String(oldPasswordField.getPassword());
		String newPassword = new String(newPasswordField.getPassword());
		String repeatedPassword = new String(repeatPasswordField.getPassword());

		try {

			while (!newPassword.equals(repeatedPassword)) {
				JOptionPane.showMessageDialog(this, "La repetición es incorrecta", "Error", JOptionPane.ERROR_MESSAGE);
				newPasswordField.requestFocus();
				return;
			}

			userService.changePassword(sessionManager.getLoggedInUser().getId(), oldPassword, newPassword);

			JOptionPane.showMessageDialog(this, "Contraseña cambiada", "Éxito", JOptionPane.INFORMATION_MESSAGE);

			dispose();
			ShowHousingsFrame showHousingFrame = context.getBean(ShowHousingsFrame.class);
			showHousingFrame.setVisible(true);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Contraseña errónea", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}