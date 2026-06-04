package fp.project.actihome.model.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.UserDao;
import fp.project.actihome.model.exceptions.DuplicateInstanceException;
import fp.project.actihome.model.exceptions.IncorrectLoginException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.WrongPasswordException;

@Service
@Transactional 
public class UserServiceImpl implements UserService {
	
	@Autowired
	private PermissionChecker permissionChecker;
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@Autowired
	private UserDao userDao;
	
	@Override
	public void signUp(User user) throws DuplicateInstanceException {
		
		if (userDao.existsByUsername(user.getUsername())) {
			throw new DuplicateInstanceException("project.entities.user", user);
		}
		
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		
		userDao.save(user);
	}
	
	@Override
	@Transactional(readOnly = true)
	public User login(String username, String password) throws IncorrectLoginException {
		
		Optional<User> user = userDao.findByUsername(username);
		
		if (!user.isPresent()) {
			throw new IncorrectLoginException(username, password); 
		}
		
		if (!passwordEncoder.matches(password, user.get().getPassword())) {
			throw new IncorrectLoginException(username, password);
		}
		
		return user.get();
	}
	
	@Override
	@Transactional(readOnly = true)
	public User loginFromId(Long id) throws InstanceNotFoundException {
		
		return permissionChecker.checkUser(id);
	}
	
	@Override
	public User updateProfile(Long id, String username, String name, String surname, String email, 
			int phonenumber, String locality) throws InstanceNotFoundException, DuplicateInstanceException {
		
		User user = permissionChecker.checkUser(id);
		
		if (userDao.existsByUsername(username) && (!user.getUsername().equals(username))) {
			throw new DuplicateInstanceException("project.entities.user", user);
		}
		
		user.setUsername(username);
		user.setName(name);
		user.setSurname(surname);
		user.setEmail(email);
		user.setPhoneNumber(phonenumber);
		user.setLocality(locality);
		
		return user;
	}
	
	@Override
	public void changePassword(Long userId, String oldPassword, String newPassword)
			throws InstanceNotFoundException, WrongPasswordException {
		
		User user = permissionChecker.checkUser(userId);
		
		if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
			throw new WrongPasswordException();
		} else {
			user.setPassword(passwordEncoder.encode(newPassword));
		}
	}
}
