package fp.project.actihome.ui;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

@Component
@Profile("!test")
@Lazy
public class UpdateProfileFrame extends JFrame {

	private final UserService userService;
	private ApplicationContext context;
	private SessionManager sessionManager;

	// Campos que introducirá el usuario:
	private JTextField usernameField;
	private JTextField nameField;
	private JTextField surnameField;
	private JTextField localityField;
	private JFormattedTextField phoneNumberField;
	private JTextField emailField;

	public UpdateProfileFrame(UserService userService, ApplicationContext context, SessionManager sessionManager) {
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
		jpanel.setBorder(BorderFactory.createTitledBorder("Actualizar perfil"));

		jpanel.add(new JLabel("Nombre de usuario"));
		usernameField = new JTextField();
		usernameField.setText(sessionManager.getLoggedInUser().getUsername());
		jpanel.add(usernameField);

		jpanel.add(new JLabel("Nombre"));
		nameField = new JTextField();
		nameField.setText(sessionManager.getLoggedInUser().getName());
		jpanel.add(nameField);

		jpanel.add(new JLabel("Apellido"));
		surnameField = new JTextField();
		surnameField.setText(sessionManager.getLoggedInUser().getSurname());
		jpanel.add(surnameField);

		jpanel.add(new JLabel("Localidad"));
		localityField = new JTextField();
		localityField.setText(sessionManager.getLoggedInUser().getLocality());
		jpanel.add(localityField);

		jpanel.add(new JLabel("Número de teléfono"));
		phoneNumberField = new JFormattedTextField();
		// phoneNumberField.setValue(sessionManager.getLoggedInUser().getPhoneNumber());
		jpanel.add(phoneNumberField);

		jpanel.add(new JLabel("Correo electrónico"));
		emailField = new JTextField();
		emailField.setText(sessionManager.getLoggedInUser().getEmail());
		jpanel.add(emailField);

		JButton updateButton = new JButton("Confirmar");
		updateButton.addActionListener(e -> updateProfile());
		jpanel.add(updateButton);

		add(jpanel);

	}

	private void updateProfile() {

		try {

			User updatedUser = userService.updateProfile(sessionManager.getLoggedInUser().getId(),
					usernameField.getText(), nameField.getText(), surnameField.getText(), emailField.getText(),
					Integer.parseInt(phoneNumberField.getText()), localityField.getText());

			JOptionPane.showMessageDialog(this, "Datos modificados correctamente", "Éxito",
					JOptionPane.INFORMATION_MESSAGE);

			sessionManager.setLoggedInUser(updatedUser);
			dispose();
			ShowHousingsFrame showHousingFrame = context.getBean(ShowHousingsFrame.class);
			showHousingFrame.setVisible(true);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "El nombre de usuario ya existe", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

}