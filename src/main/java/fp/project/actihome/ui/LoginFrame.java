package fp.project.actihome.ui;

import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.sessionManagement.SessionManager;

@Component
@Profile("!test")
public class LoginFrame extends JFrame {

	private final UserService userService;
	private ApplicationContext context;
	private SessionManager sessionManager;

	// Campos que introducirá el usuario:
	private JTextField usernameField;
	private JPasswordField passwordField;

	public LoginFrame(UserService userService, ApplicationContext applicationContext, SessionManager sessionManager) {
		this.userService = userService;
		this.context = applicationContext;
		this.sessionManager = sessionManager;
		initUI();
	}

	private void initUI() {

		setTitle("Actihome");
		setSize(500, 500);
		setLocationRelativeTo(null);

		JPanel jpanel = new JPanel(new GridLayout(10, 2, 2, 2));
		jpanel.setBorder(BorderFactory.createTitledBorder("Iniciar sesión"));

		jpanel.add(new JLabel("Nombre de usuario"));
		usernameField = new JTextField();
		jpanel.add(usernameField);

		jpanel.add(new JLabel("Contraseña"));
		passwordField = new JPasswordField();
		jpanel.add(passwordField);

		JButton loginButton = new JButton("Iniciar sesión");
		loginButton.addActionListener(e -> login());
		jpanel.add(loginButton);

		JLabel link = new JLabel("<html><u>¿Es tu primera vez aquí? Regístrate</u></html>");
		link.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				dispose();
				displaySignUpFrame();
			}
		});

		jpanel.add(link);
		add(jpanel);

	}

	private void login() {

		String username = usernameField.getText();
		String password = new String(passwordField.getPassword());

		try {
			sessionManager.login(userService.login(username, password));

			if (sessionManager.isUserLoggedIn()) {

				JOptionPane.showMessageDialog(this, "Sesión iniciada", "Éxito", JOptionPane.INFORMATION_MESSAGE);
			}

			dispose();
			ShowHousingsFrame showHousingsFrame = context.getBean(ShowHousingsFrame.class);
			showHousingsFrame.setVisible(true);

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Credenciales incorrectas", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void displaySignUpFrame() {

		SignUpFrame signUpFrame = context.getBean(SignUpFrame.class);
		signUpFrame.setVisible(true);
	}
}