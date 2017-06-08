package ch.sichh.baseprogram;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;

import ch.sichh.helpers.Helpers;

/**
 * Find open ports, connect to them and tell the clients to which port to connect. Then start listening on those ports.
 * @author Moreno Colombo
 * @version 2.0
 * @since 14.10.2016
 */

public class SocketFactory{

	// Variables.
	private String serverAddress;
	private int eventID;
	private int [] sensorIDs, ports;
	private boolean isVisualizationActive, isDataFiltered;
	private HashMap<Integer, ServerThread> serverThreads;
	private GradientsVisualization gradVis;
	private Visualization visualization;
	
	// Constructor
	public SocketFactory(String serverAddress, int [] sensorIDs, int eventID, boolean isVisualizationActive, boolean isDataFiltered){
		this.serverAddress = serverAddress;
		this.eventID = eventID;
		this.sensorIDs = sensorIDs;
		System.out.println(Arrays.toString(sensorIDs));
		this.isVisualizationActive = isVisualizationActive;
		this.isDataFiltered = isDataFiltered;
	}
	
	// Broadcast start message to all clients
	public void sendStartSignal(){
		try {
			// Send free ports associated to correct sensorID to the clients
			this.ports = this.findPorts(this.sensorIDs.length);
			String sendConfig = "START_TRANSMISSION_REQUEST";
			for(int i=0;i<sensorIDs.length;i++){
				sendConfig = sendConfig+", "+sensorIDs[i]+", "+this.ports[i];
			}
			byte[] sensConfig = sendConfig.getBytes();
			
			// Broadcast start message to all addresses in the local network
			DatagramSocket c;
			c = new DatagramSocket();
			c.setBroadcast(true);
			DatagramPacket sendPacket = new DatagramPacket(sensConfig, sensConfig.length, InetAddress.getByName("192.168.1.255"), 8888);
			c.send(sendPacket);
			
			// Broadcast the message over all the network interfaces
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
			    NetworkInterface networkInterface = interfaces.nextElement();

			    if (networkInterface.isLoopback() || !networkInterface.isUp()) {
			      continue; // Don't want to broadcast to the loopback interface
			    }

			    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
			      InetAddress broadcast = interfaceAddress.getBroadcast();
			      if (broadcast == null) {
			        continue;
			      }

			      // Send the broadcast packet!
			      try {
			        c.send(sendPacket);
			      } catch (Exception e) {
			    	  e.printStackTrace();
			      }

			      System.out.println(">>> Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
			    }
			  }
			c.close();
			
			this.start();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//Find free ports
	private int[] findPorts(int sensorN) throws IOException {
	    int i = 0;
	    int[] ports = new int[sensorN];
		for (int j = 49152;j<=65535;j++) {
	        try {
	            (new ServerSocket(j)).close();
	        } catch (IOException ex) {
	            continue; // try next port
	        }
	        ports[i] = j;
	        i++;
	        if(i == sensorN) return ports;
	    }

	    // if the program gets here, no port in the range was found
	    throw new IOException("No free port found");
	}
	
	private void start(){
		// Start reading on defined ports and store the ServerThread objects
		serverThreads = new HashMap<Integer,ServerThread>();
		int threadN = isVisualizationActive? sensorIDs.length+2 : sensorIDs.length;
		ExecutorService executor = Executors.newFixedThreadPool(threadN);
		ServerThread temp;
		
		for(int i=0;i<sensorIDs.length;i++){
			temp = new ServerThread(serverAddress,ports[i],eventID,sensorIDs[i],isDataFiltered);
			serverThreads.put(sensorIDs[i],temp);
			executor.execute(temp);
		}
		
		/**
		 * Start visualization and send it the serverthreads arraylist to access the detections of each thread
		 */
		if(isVisualizationActive){
			JSONObject config = Helpers.eventConfig(this.serverAddress, this.eventID);
			if(config == null){
				System.out.println("Error: the configuration of the event is not complete on server side.");
			}else{
				gradVis = new GradientsVisualization(config, this.serverThreads);
				executor.execute(gradVis);
				visualization = new Visualization(config, this.serverThreads);
				executor.execute(visualization);
			}
		}
	}

	public void stop() {
		// Stop and close visualization
		if(isVisualizationActive)
			visualization.stop();
			gradVis.stop();
		// Stop serverThreads
		for (int id : sensorIDs){
			ServerThread temp = serverThreads.get(id);
			temp.terminate();
		}
	}
	
	public boolean checkFinishedExecution(){
		for (int id : sensorIDs){
			ServerThread temp = serverThreads.get(id);
			if(!temp.checkFinished()) return false;
		}
		return true;
	}
}