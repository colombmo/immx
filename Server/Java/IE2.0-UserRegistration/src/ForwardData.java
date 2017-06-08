import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONObject;

import ch.sichh.registration.helpers.Helpers;


public class ForwardData {
	String serverAddr;
	int eventId;
	int groupId;
	String[] tagIds;
	
	public ForwardData(String serverAddr, int eventId, int groupId, String[] tagIds) {
		this.serverAddr = serverAddr;
		this.eventId = eventId;
		this.groupId = groupId;
		this.tagIds = tagIds.clone();
	}

	public int getInterId() {
		return groupId;
	}

	public void setInterId(int groupId) {
		this.groupId = groupId;
	}

	public String[] getTagIds() {
		return tagIds;
	}

	public void setTagId(String[] tagIds) {
		this.tagIds = tagIds.clone();
	}
	
	public void send() throws ClientProtocolException, IOException{
		
		// Initialize JSON object containing recordings to be sent to the db
		JSONObject data = new JSONObject();
		data.put("eventId", this.eventId);
		data.put("groupId", this.groupId);
		data.put("participants", new JSONArray(this.tagIds));
		
		// POST data to db to be saved
		Helpers.postJSONToURL("http://" + this.serverAddr + "/hubnet/registerParts", data);
	}
}
