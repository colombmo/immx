package ch.sichh.registration.helpers;
import org.json.JSONObject;

public class Group {	
	private int id;
	private String name;
	
	public Group(JSONObject event){
		this.id = event.getInt("pk");
		this.name = event.getString("name");
	}
	
	public Group(int pk, String description) {
		super();
		this.id = pk;
		this.name = description;
	}

	public int getId() {
		return id;
	}

	public void setId(int pk) {
		this.id = pk;
	}

	public String getDescription() {
		return name;
	}

	public void setDescription(String description) {
		this.name = description;
	}

	@Override
	public String toString() {
		return name;
	}
}
