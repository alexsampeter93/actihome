package fp.project.actihome.model.exceptions;

@SuppressWarnings("serial")
public abstract class InstanceException extends Exception{

	private final String name;
	
	private final transient Object key;
	
	protected InstanceException(String name, Object key) {
		
		this.name = name;
		this.key = key;
	}
	
	public String getName() {
		
		return name;
	}
	public Object getKey() {
		
		return key;
	}
}
