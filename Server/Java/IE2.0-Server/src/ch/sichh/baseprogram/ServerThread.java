package ch.sichh.baseprogram;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import ch.sichh.helpers.Helpers;

/**
 * Thread for the server, listening to data from the clients and elaborating it.
 * @author Moreno Colombo
 * @version 2.0
 * @since 14.10.2016
 */

public class ServerThread extends Thread {
	
	// Variables.
	private String serverIP;
	private int eventID;
	private int port, sensorID;
	private boolean isDataFiltered;
	private volatile String[] values = null;
	private JSONObject recordingsForDB;
	private boolean isReconnected;
	
	private volatile boolean isRunning;
	private volatile boolean isFinished;
	
	
	// Constructor
	public ServerThread(String serverIP, int port, int eventID, int sensorID, boolean isDataFiltered){
		this.serverIP = serverIP;
		this.port = port;
		this.eventID = eventID;
		this.sensorID = sensorID;
		this.isDataFiltered = isDataFiltered;
		this.isRunning = true;
		this.isFinished = false;
		this.isReconnected = false;
	}
	
	public String[] getRecordings(){
		return values;
	}

	@Override
	public void run() {
		execute();
	}
	
	private void execute(){
		//Variables.
		ServerSocket listener;
		
		// Initialize JSON object containing recordings to be sent to the db
		this.recordingsForDB = new JSONObject();
		this.recordingsForDB.put("eventID", this.eventID);
		this.recordingsForDB.put("sensorID", this.sensorID);
		this.recordingsForDB.put("timestamp", "");
		this.recordingsForDB.put("tagIds", new JSONArray());
		
		// Create Reader object, connecting to physical device
		try {	
			listener = new ServerSocket(port);
			listener.setSoTimeout(5000);
			try{
				Socket socket = listener.accept();
				GUI.connectedClients.put(this.sensorID, socket.getInetAddress().getHostAddress());
				// Console output.
				System.out.println(new Date().toString() + "> Reader " + sensorID + " created and listening");
				
				// Create input stream to read tag reads                	
	        	ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
	        	PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				
	        	// Repeat until the stop button is pressed
	        	while(this.isRunning) {
	        		
	        		// Send message to client to start reading(while specifying if data has to be pre-filtered)
	        		if (this.isDataFiltered)
	        			out.println("filter");
	        		else
	        			out.println("nofilter");
	        		out.flush();
	        			        		
	        		while((values = (String[])in.readObject())==null && this.isRunning){}
	            	
					// Send recorded values to database
					if(values.length > 0 && this.isRunning){
						// Get time.
						String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
						// If not already sent a record in the same second, send it
						if(!timeStamp.equals(this.recordingsForDB.getString("timestamp"))){
							this.recordingsForDB.put("tagIds", new JSONArray(this.values));
							this.recordingsForDB.put("timestamp", timeStamp);
						}else{ // Else only take the new recordings
							this.recordingsForDB.put("tagIds", new JSONArray(this.values));
						}
						Helpers.postJSON(this.serverIP, recordingsForDB);
					}
					// Restart loop.
	            }
	        	
	        	// Close connection and terminate all of the clients
	            out.print("terminate");
	            out.flush();
	            socket.close();
	            System.out.println(new Date().toString() + "> Reader " + sensorID + " Connection closed");
			}finally{		
				listener.close();
			}
		}catch(SocketTimeoutException e){
			if(!this.isReconnected)
				System.out.println(new Date().toString() + "> Reader " + sensorID + " not found, closing its connection");
			else
				System.out.println(new Date().toString() + "> Reader " + sensorID + " not found, trying again");
		}catch(Exception e) {
        	System.out.println(new Date().toString() + "> Reader " + sensorID + " exception: " + e.getMessage());
        	e.printStackTrace();
        }
		this.isFinished = true;
	}

	public void terminate() {
		this.isRunning = false;
	}
	
	public boolean checkFinished(){
		return this.isFinished;
	}
}