package fp.project.actihome.model.services;

import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.IncorrectLoginException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.WrongPasswordException;

public interface UserService {
	
	void signUp(User user) throws DuplicateInstanceException;
	
	User login(String username, String password) throws IncorrectLoginException;
	
	User loginFromId(Long id) throws InstanceNotFoundException;
	
	User updateProfile(Long id, String username, String name, String surname, String email, int phonenumber,
			String locality) throws InstanceNotFoundException, DuplicateInstanceException;
	
	void changePassword(Long userId, String oldPassword, String newPassword)
			throws InstanceNotFoundException, WrongPasswordException;
}
