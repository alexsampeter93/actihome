package fp.project.actihome.ui.sessionManagement;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.entities.User;

@Component
@Profile("!test")
public class SessionManager {

	private User loggedInUser;

	public void login(User user) {

		loggedInUser = user;
	}

	public User getLoggedInUser() {

		return loggedInUser;
	}

	public void setLoggedInUser(User user) {

		this.loggedInUser = user;
	}

	public void logout() {

		loggedInUser = null;
	}

	public boolean isUserLoggedIn() {

		return loggedInUser != null;
	}
}