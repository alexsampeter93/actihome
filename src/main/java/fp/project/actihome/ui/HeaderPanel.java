package fp.project.actihome.ui;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import fp.project.actihome.ui.sessionManagement.SessionManager;

@Component
@Profile("!test")
@Lazy
@Scope("prototype")
public class HeaderPanel extends JPanel {

	private SessionManager sessionManager;
	private ApplicationContext context;
	private JButton menuButton;
	private JButton mainButton; 

	public HeaderPanel(ApplicationContext context, SessionManager sessionManager) {

		this.context = context;
		this.sessionManager = sessionManager;
		initUI();
	}

	private void initUI() {

		setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mainButton = new JButton("ActiHome");
        mainButton.addActionListener(e -> {

            var currentWindow = SwingUtilities.getWindowAncestor(this);

            if (currentWindow instanceof ShowHousingsFrame) {
                return;
            }

            ShowHousingsFrame showHousingsFrame = context.getBean(ShowHousingsFrame.class);
            showHousingsFrame.setVisible(true);
            SwingUtilities.getWindowAncestor(this).dispose();
        });

		menuButton = new JButton();
		refresh();

		JPopupMenu menu = new JPopupMenu();

		JMenuItem updateProfileItem = new JMenuItem("Editar perfil");
		updateProfileItem.addActionListener(e -> {
			UpdateProfileFrame updateProfileFrame = context.getBean(UpdateProfileFrame.class);
			updateProfileFrame.setVisible(true);
			SwingUtilities.getWindowAncestor(this).dispose();
		});

		JMenuItem changePasswordItem = new JMenuItem("Cambiar contraseña");
		changePasswordItem.addActionListener(e -> {
			ChangePasswordFrame changePasswordFrame = context.getBean(ChangePasswordFrame.class);
			changePasswordFrame.setVisible(true);
			SwingUtilities.getWindowAncestor(this).dispose();
		});

		JMenuItem logoutItem = new JMenuItem("Cerrar sesión");
		logoutItem.addActionListener(e -> {
			sessionManager.logout();
			LoginFrame loginFrame = context.getBean(LoginFrame.class);
			loginFrame.setVisible(true);
			SwingUtilities.getWindowAncestor(this).dispose();
		});

		menu.add(updateProfileItem);
		menu.add(changePasswordItem);
		menu.addSeparator();
		menu.add(logoutItem);

		menuButton.addActionListener(e -> menu.show(menuButton, 0, menuButton.getHeight()));

		add(menuButton, BorderLayout.EAST);
		add(mainButton, BorderLayout.WEST);
		
	}

	public void refresh() {

		menuButton.setText(sessionManager.getLoggedInUser().getUsername());
	}
}