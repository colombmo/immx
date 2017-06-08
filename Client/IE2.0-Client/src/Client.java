import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.thingmagic.Reader;
import com.thingmagic.ReaderException;
import com.thingmagic.TMConstants;
import com.thingmagic.TagReadData;


public class Client {
	
	// Variables.
	private Reader reader;
	private String serverAddress;
	private String comport;
	private int raspberryId;
	private int port;
	
	public Client(){	
		/**
		 *  Load configuration file (containing raspberry id and comport)
		 */
		JSONObject config;
		try {
			config = new JSONObject(new JSONTokener(new FileReader("config.json")));
			raspberryId=config.getInt("raspberryId");
			comport = config.getString("comPort");
		} catch (JSONException | FileNotFoundException e) {
			e.printStackTrace();
		}
	};
	
	/**
	 *  Create reader, connect and configure
	 */
	public void init(){
		try {
			reader = Reader.create("tmr:///" + comport);
			reader.connect();
					
			if (Reader.Region.UNSPEC == (Reader.Region)reader.paramGet("/reader/region/id")){
				Reader.Region[] supportedRegions = (Reader.Region[])reader.paramGet(TMConstants.TMR_PARAM_REGION_SUPPORTEDREGIONS);
				if (supportedRegions.length < 1){
					try {
						throw new Exception("Reader doesn't support any regions");
					} catch (Exception e) {}
				}
				else{
					reader.paramSet("/reader/region/id", supportedRegions[0]);
				}
			}
			
			reader.gpoSet(new Reader.GpioPin[]{new Reader.GpioPin(1,true)});
			
		} catch (ReaderException e1) {
			e1.printStackTrace();
		}
	};
	
	public void waitForStart(){
		DatagramSocket socket;
		try {
		    //Keep a socket open to listen to all the UDP traffic that is destined for this port (8888)
			socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
		    socket.setBroadcast(true);
		
		    //Receive a packet
		    byte[] recvBuf = new byte[15000];
		    DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
		    String message;
			// Wait for signal until getting the right message
			do{
				socket.receive(packet);
				message = new String(packet.getData()).trim();
				if(message.contains("START_TRANSMISSION_REQUEST")){
					/**
					 * Read input from server
					 */
					System.out.println("Message: "+message.toString());
					Scanner msg = new Scanner(message);
					msg.useDelimiter(",\\s*");
					msg.next(); //Skip first line ("START_TRANSMISSION_REQUEST")
					while (msg.hasNext()) {
						if(Integer.parseInt(msg.next())==raspberryId){ // If raspberryId=this
							port = Integer.parseInt(msg.next());
							break;
						}else{
							msg.next();
						}
					}
					msg.close();
				}
			}while(!message.startsWith("START_TRANSMISSION_REQUEST"));
			
			socket.close();
			
			// Start program
			try{Thread.sleep(1000);}catch(Exception e){e.printStackTrace();};
			this.execute(packet.getAddress().getHostAddress());
			
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private void execute(String serverAddress){
		this.serverAddress = serverAddress;
		execute();
	};
	
	private void execute(){
		BufferedReader in = null;
		ObjectOutputStream out = null;
		Socket s = null;
		TagReadData[] tagReads;
		try{
			// Connect to a socket
			s = new Socket(serverAddress, port);
			
			// Create output stream to send reads and input to get instructions on what to do from the server
			in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			out = new ObjectOutputStream(s.getOutputStream());
			
			/**
			 *  Read tags and send to server to be treated until the server doesn't terminate the connections
			 */
			// Last read tags
			Map <String,OldRecords> lastReads = new HashMap<String, OldRecords>();
			// Input received from server
			String inputFromServer;
			// Array to be sent to the server
			String [] results;
			
			while(!((inputFromServer = in.readLine()).equals("terminate"))){
				reader.gpoSet(new Reader.GpioPin[]{new Reader.GpioPin(2,true)});
				// Read tags
				tagReads = reader.read(700);			
				ArrayList<String> filtered = new ArrayList<String>();
				// Make the led blink when reading
				reader.gpoSet(new Reader.GpioPin[]{new Reader.GpioPin(2,false)});
				if(inputFromServer.equals("filter")){
					// Filter data
					// Filter out tagReads by the number of detections
					for(int i=0;i<tagReads.length;i++){
						if(tagReads[i].getReadCount()>2){
							filtered.add(tagReads[i].epcString());
						}
					}
					
					//Update recordings of last 3 loops by shifting 1 position to the right
					for (Map.Entry<String, OldRecords> entry : lastReads.entrySet())
					    entry.getValue().deleteFirst();
					
					// Update Map with last registered elements
					for(String tag:filtered){
						try{
							lastReads.get(tag).onRead();
						}catch(Exception e){
							lastReads.put(tag, new OldRecords());
						}
					}
					
					// Put tags reads at least half of the last readings in an array, they will be sent to the server
					List<String> fT = new ArrayList<String>();
					
					for(Map.Entry<String,OldRecords> entry : lastReads.entrySet()){
					    if(entry.getValue().getSum()>=3){
					    	fT.add(entry.getKey());
					    }
					}
					
					results = fT.toArray(new String[fT.size()]);
					// Records always sorted for visualization application
					Arrays.sort(results);
				}else{
					// Send non-filtered data
					// Filter out tagReads by the number of detections
					for(int i=0;i<tagReads.length;i++){
						if(tagReads[i].getReadCount()>2){
							filtered.add(tagReads[i].epcString());
						}
					}
					
					results = filtered.toArray(new String[filtered.size()]);
					// Records always sorted for visualization application
					Arrays.sort(results);
				}
				
				// Send records in JSON array via socket to server
				out.writeObject(results);				
				out.flush();
			}
			// When received termination signal, close socket connection and start waiting again
			in.close();
			out.close();
			s.close();
			reader.gpoSet(new Reader.GpioPin[]{new Reader.GpioPin(2,false)});
			this.waitForStart();
		}catch(Exception e){
			e.printStackTrace(); // TODO: remove this
			// When an error has occurred, close socket connection and start waiting again
			try {in.close();} catch (IOException e1) {e1.printStackTrace();}
			try {out.close();} catch (IOException e1) {e1.printStackTrace();}
			try {s.close();} catch (IOException e1) {e1.printStackTrace();}
			try {
				reader.gpoSet(new Reader.GpioPin[]{new Reader.GpioPin(2,false)});
			} catch (ReaderException e1) {
				e1.printStackTrace();
			}
			this.waitForStart();
			e.printStackTrace();
		}
	}
}