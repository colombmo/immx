package ch.sichh.registration.helpers;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Helpers {
	/* JSON */
	/**
	 * Read JSONArray from @param json (under key @param arg) and return it as an int [] 
	 * @param json
	 * @param arg
	 * @return
	 */
	public static int[] getIntArray(JSONObject json, String arg){
		try{
			int [] intArray;
			JSONArray temp = json.getJSONArray(arg);
			intArray = new int[temp.length()];
			for(int i=0;i<temp.length();i++){
				intArray[i]=temp.getInt(i);
			}
			return intArray;
		}catch(JSONException e){
			return new int[]{};
		}
	}
	
	/* Event configuration */
	// Get wanted elements from server for this event
	public static JSONObject eventConfig(String ipAddress, int event){
		return getFromURL("http://" + ipAddress + "/hubnet/getConfig/"+event);
	}
	
	/* General helpers */
	/**
	 * GET requests to the server
	 * @param url the url of the page
	 */
	public static JSONObject getFromURL(String strURL){
		try {
			URL url = new URL(strURL);
			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
			JSONObject response = new JSONObject(new JSONTokener(br));
			return response;
		} catch (Exception e) {
			return null;
		}
	}
	
	public static String getStrFromURL(String strURL){
		try {
			URL url = new URL(strURL);
			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
			String response = br.toString();
			return response;
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * POST Json object to URL
	 */
	public static void postJSONToURL(String strURL, JSONObject json){
		try {
			URL object = new URL(strURL);
			HttpURLConnection con = (HttpURLConnection) object.openConnection();
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Accept", "application/json");
			con.setRequestMethod("POST");
			OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
			wr.write(json.toString());
			wr.flush();
			con.getResponseCode();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * POST JSON object to database
	 */
	public static void postJSON(String ipAddress, JSONObject json){
		postJSONToURL("http://" + ipAddress + "/hubnet/irec/", json);
	}
	/**
	 * Translate hexadecimal code of a color into a Color object.
	 * @param hexstring a string containing the hexadecimal code of a color
	 * @return A color
	 */
	public static Color createColor(String hexstring){
		hexstring = hexstring.substring(1);
		int i = Integer.parseInt(hexstring,16);
		Color color = new Color(i);
		return color;
	}
}
