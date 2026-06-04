package fp.project.actihome.ui;

import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

@Component
@Profile("!test")
public class SignUpFrame extends JFrame {

	private final UserService userService;
	private ApplicationContext context;
	private SessionManager sessionManager;

	// Campos que introducirá el usuario:
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JTextField nameField;
	private JTextField surnameField;
	private JTextField localityField;
	private JFormattedTextField phoneNumberField;
	private JTextField emailField;
	private JSpinner birthDateSpinner;
	private JComboBox roleBox;

	public SignUpFrame(UserService userService, ApplicationContext context, SessionManager sessionManager) {
		this.userService = userService;
		this.context = context;
		this.sessionManager = sessionManager;
		initUI();
	}

	public void setVisible(boolean visible) {
		if (visible) {
			usernameField.setText("");
			passwordField.setText("");
			nameField.setText("");
			surnameField.setText("");
			localityField.setText("");
			phoneNumberField.setValue(null);
			phoneNumberField.setText("");
			emailField.setText("");
		}
		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("Actihome");
		setSize(500, 500);
		setLocationRelativeTo(null);

		JPanel jpanel = new JPanel(new GridLayout(10, 2, 2, 2));
		jpanel.setBorder(BorderFactory.createTitledBorder("Registrar usuario"));

		jpanel.add(new JLabel("Nombre de usuario"));
		usernameField = new JTextField();
		jpanel.add(usernameField);

		jpanel.add(new JLabel("Contraseña"));
		passwordField = new JPasswordField();
		jpanel.add(passwordField);

		jpanel.add(new JLabel("Nombre"));
		nameField = new JTextField();
		jpanel.add(nameField);

		jpanel.add(new JLabel("Apellido"));
		surnameField = new JTextField();
		jpanel.add(surnameField);

		jpanel.add(new JLabel("Localidad"));
		localityField = new JTextField();
		jpanel.add(localityField);

		jpanel.add(new JLabel("Número de teléfono"));
		phoneNumberField = new JFormattedTextField();
		jpanel.add(phoneNumberField);

		jpanel.add(new JLabel("Correo electrónico"));
		emailField = new JTextField();
		jpanel.add(emailField);

		jpanel.add(new JLabel("Fecha de nacimiento"));
		birthDateSpinner = new JSpinner(new SpinnerDateModel());
		birthDateSpinner.setEditor(new JSpinner.DateEditor(birthDateSpinner, "dd/MM/yyyy"));
		jpanel.add(birthDateSpinner);

		jpanel.add(new JLabel("Rol"));
		roleBox = new JComboBox<>(RoleType.values());
		jpanel.add(roleBox);

		JButton registerButton = new JButton("Registrarse");
		registerButton.addActionListener(e -> signUp());
		jpanel.add(registerButton);

		add(jpanel);

	}

	private void signUp() {

		String password = new String(passwordField.getPassword());
		int phoneNumber = Integer.parseInt(phoneNumberField.getText());
		Date birthDateValue = (Date) birthDateSpinner.getValue();
		LocalDateTime birthDate = birthDateValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		RoleType role = (RoleType) roleBox.getSelectedItem();

		try {
			User user = new User(usernameField.getText(), password, nameField.getText(), surnameField.getText(),
					localityField.getText(), phoneNumber, emailField.getText(), birthDate, role);

			userService.signUp(user);

			JOptionPane.showMessageDialog(this, "El usuario se ha registrado correctamente", "Éxito",
					JOptionPane.INFORMATION_MESSAGE);

			dispose();
			LoginFrame loginFrame = context.getBean(LoginFrame.class);
			loginFrame.setVisible(true);

		} catch (DuplicateInstanceException ex) {
			JOptionPane.showMessageDialog(this, "Error en el registro, revise los campos", "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

}