package fp.project.actihome.model.services;

import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;

public interface PermissionChecker {
	
	void checkUserExists(Long userId) throws InstanceNotFoundException;
	
	User checkUser(Long userId) throws InstanceNotFoundException; 

}
