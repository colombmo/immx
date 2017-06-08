package ch.sichh.helpers;

import org.json.JSONObject;

public class Event {
	private int id;
	private String name;
	private int[] sensorList;
	
	public Event(JSONObject event){
			this.id = event.getInt("pk");
			this.name = event.getString("name");
			this.sensorList = Helpers.getIntArray(event, "sensors");
	}
	
	public Event(int id, String name, int[] sensorList) {
		this.id = id;
		this.name = name;
		this.sensorList = sensorList;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int[] getSensorList() {
		return sensorList;
	}

	public void setSensorList(int[] sensorList) {
		this.sensorList = sensorList;
	}

	@Override
	public String toString() {
		return this.name;
	}
	
	
}
