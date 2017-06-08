package ch.sichh.baseprogram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class drawing and updating the live visualization based on the data recorded.
 * @author Moreno Colombo
 * @version 1.0
 * @since 08.07.2015
 */
public class GradientsVisualization extends JPanel implements Runnable {
	
	// ---------------------------------------------------------------
	// Variables.
	// ---------------------------------------------------------------	
	private float MAX = 100;
	
	private static JFrame frame;
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); // Get size of the screen to fill it with the visualization
	private static final long serialVersionUID = 1L;					// Prevent a warning 
	
	private static boolean isRunning = true;
	
	private final int WIDTH = screenSize.width;							// The width of the screen
	private final int HEIGHT = screenSize.height;						// The height of the screen
	private JSONObject params;											// The parameters for this event (participants, groups and sensors)
	private JSONArray sensors;
	
	private Image bgImage;
	
	private HashMap<Integer, ServerThread> serverThreads;
	private HashMap<Integer,HashMap<String, Long>> roomOccupation;
	private HashMap<Integer,HashMap<String, Long>> roomLeftTime;
	// ---------------------------------------------------------------
	// Constructor.
	// ---------------------------------------------------------------
		
	public GradientsVisualization(JSONObject params, HashMap<Integer, ServerThread> serverThreads){
		this.params = params;
		this.sensors = this.params.getJSONArray("sensors");
		this.MAX = this.params.getJSONArray("participants").length()>0?this.params.getJSONArray("participants").length():100;
		this.serverThreads = serverThreads;
	

		try {
			bgImage = ImageIO.read(new File(this.params.getString("background")));
		} catch (IOException e) {}
				
		isRunning = true;
		roomOccupation = new HashMap<Integer,HashMap<String, Long>>();
		roomLeftTime = new HashMap<Integer,HashMap<String, Long>>();
		
		for(int i=0;i<this.sensors.length();i++){
			JSONObject temp = sensors.getJSONObject(i);
			roomOccupation.put(temp.getInt("sensorId"), new HashMap<String,Long>());
			roomLeftTime.put(temp.getInt("sensorId"), new HashMap<String,Long>());
		}
	};
	
	// ---------------------------------------------------------------
	// Methods.
	// ---------------------------------------------------------------
	
	public void drawSensor(Graphics g){
		int[] xPoints = {WIDTH/2-20, WIDTH/2+20, WIDTH/2};
		int[] yPoints = {70,70,30};
		g.setColor(Color.RED);	
		g.fillPolygon(xPoints, yPoints, 3);
	}
	
	/**
	 * Draw the slices of pie around the sensors and the sensors themselves.
	 * @param g A Graphics environment object
	 */
	public void drawPieGraph(Graphics g){
		Graphics2D g2d = (Graphics2D) g;
		g.setFont(new Font(g.getFont().getFontName(),Font.BOLD,20));
		
		for (int i=0; i<sensors.length(); i++){
			JSONObject temp = sensors.getJSONObject(i);
			int size = 100;
			// Set position of pie graph and table
			int tX = temp.getInt("xPos")*WIDTH/100;
			int tY = temp.getInt("yPos")*HEIGHT/100;
			String tableNum = ""+temp.getInt("sensorId");
			
			String[] recordings = serverThreads.get(temp.getInt("sensorId")).getRecordings();
			updateOccupation(recordings, temp.getInt("sensorId"));
			
			int color = (int)(((float)roomOccupation.get(temp.getInt("sensorId")).size())/MAX*255);
			// Draw slice of pie
			g.setColor(new Color(color<=255?color:255, color<=255?255-color:0, 0));
			g2d.fillOval(tX-size/2, tY-size/2, size, size);
			
			// Show table number
			g.setColor(Color.black);
			g2d.drawString(tableNum, tX-5, tY+5);
		}
	}
	
	private void updateOccupation(String[] values, int id){
		if(values!=null && values.length>0){
			for(String val:values){
				long temp = roomOccupation.get(id).containsKey(val)?roomOccupation.get(id).get(val):0;
				long temp1 = roomLeftTime.get(id).containsKey(val)?roomLeftTime.get(id).get(val):0;
				long currentTime = System.currentTimeMillis();
				if(temp>0 && currentTime-temp>5000){
					roomOccupation.get(id).remove(val);
					roomLeftTime.get(id).put(val, currentTime);
				}else if(temp1==0 || currentTime-temp1>5000){
					if(roomLeftTime.get(id).containsKey(val))
						roomLeftTime.get(id).remove(val);
					roomOccupation.get(id).put(val, currentTime);
					for(int i : roomOccupation.keySet()){
						if(i!=id){
							HashMap<String,Long> t = roomOccupation.get(i);
							if(t.containsKey(val))
								t.remove(val);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.drawImage(bgImage, 0, 0, WIDTH, HEIGHT, null);
		drawSensor(g);
		drawPieGraph(g);
	}
		
	public void run() {	
		frame = new JFrame("Individual live visualization");
		GradientsVisualization liveVis = new GradientsVisualization(this.params, this.serverThreads);
		frame.add(liveVis);
		frame.setSize(WIDTH, HEIGHT);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		liveVis.setBackground(Color.WHITE);
		
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH); 
		frame.setUndecorated(true);
		frame.setVisible(true);
		
		try{
			while (isRunning) {
				liveVis.repaint();
				try {Thread.sleep(100);} catch (Exception e){}
			}
		}catch(Exception e){
			e.printStackTrace();
		};
		kill();
	}
	
	/**
	 * Kill drawing thread.
	 */
	private static void kill(){
		frame.setVisible(false);
		frame.dispose();
		frame = null;
	}
	
	public void stop(){
		isRunning = false;
	}
}
