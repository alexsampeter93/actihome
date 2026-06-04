package fp.project.actihome.model.exceptions;

@SuppressWarnings("serial")
public class IncorrectLoginException extends Exception {
	
	private final String username;
	
	private final String password;
	
	public IncorrectLoginException(String username, String password) {
		super();
		this.username = username;
		this.password = password;
		
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}

}
