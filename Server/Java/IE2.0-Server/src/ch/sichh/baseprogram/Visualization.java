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

import ch.sichh.helpers.Helpers;

/**
 * Class drawing and updating the live visualization based on the data recorded.
 * @author Moreno Colombo
 * @version 1.0
 * @since 08.07.2015
 */
public class Visualization extends JPanel implements Runnable {
	
	// ---------------------------------------------------------------
	// Variables.
	// ---------------------------------------------------------------	
	private static JFrame frame;
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); // Get size of the screen to fill it with the visualization
	private static final long serialVersionUID = 1L;					// Prevent a warning 
	
	private static boolean isRunning = true;
	
	private final int WIDTH = screenSize.width;							// The width of the screen
	private final int HEIGHT = screenSize.height;						// The height of the screen
	private Image bgImage;
	private JSONObject params;											// The parameters for this event (participants, groups and sensors)
	private JSONArray groups, sensors;
	private HashMap<String, Color> participants;
	private HashMap<Integer, ServerThread> serverThreads;
	// ---------------------------------------------------------------
	// Constructor.
	// ---------------------------------------------------------------
		
	public Visualization(JSONObject params, HashMap<Integer, ServerThread> serverThreads){
		this.params = params;
		this.groups = this.params.getJSONArray("groups");
		this.sensors = this.params.getJSONArray("sensors");
		this.serverThreads = serverThreads;
		
		try {
			bgImage = ImageIO.read(new File(this.params.getString("background")));
		} catch (IOException e) {}
		
		isRunning = true;
		
		// Initialize participants
		participants = new HashMap<String,Color>();
		JSONArray tempParts = this.params.getJSONArray("participants");
		for(int i=0;i<tempParts.length();i++){
			JSONObject tp = tempParts.getJSONObject(i);
			participants.put(tp.getString("tagId"), Helpers.createColor(tp.getString("color")));
		}
	};
	
	// ---------------------------------------------------------------
	// Methods.
	// ---------------------------------------------------------------
	/**
	 * Draws the legend for the live visualization.
	 * @param g A Graphics environment object
	 */
	private void drawLegend(Graphics g){
		Graphics2D g2d = (Graphics2D) g;
		int leftMargin = (WIDTH / 100);
		int topMargin = HEIGHT -(HEIGHT / 15);
		int fontSize = WIDTH / 90;
		int tabulation = leftMargin;
		
		// Draw the bar.
		g.setColor(Color.black);
		g2d.fillRect(0, topMargin, WIDTH, HEIGHT/15);
		
		g.setFont(new Font(g.getFont().getFontName(),Font.BOLD,fontSize));
		
		// Draw the legends.
		for (int i=0; i<groups.length();i++){
			JSONObject temp = groups.getJSONObject(i);
			// Get appropriate color.
			Color tagColor = Helpers.createColor(temp.getString("color"));
			
			// Draw color tag and text.
			g.setColor(tagColor);
			g2d.fillRect(tabulation, topMargin + (HEIGHT/100), (HEIGHT / 22), (HEIGHT / 22));
			
			g.setColor(Color.white);
			g.drawString(temp.getString("name"), tabulation + (HEIGHT / 20), topMargin + (HEIGHT / 25)); 

			tabulation += g.getFontMetrics().stringWidth(temp.getString("name")) + (HEIGHT / 10);
		}
	}
	
	public void drawRoom(Graphics g){
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
		g.setFont(new Font(g.getFont().getFontName(),Font.BOLD,14));
		
		for (int i=0; i<sensors.length(); i++){
			JSONObject temp = sensors.getJSONObject(i);
			int size = 100;
			// Set position of pie graph and sensor
			int tX = temp.getInt("xPos")*WIDTH/100;
			int tY = temp.getInt("yPos")*HEIGHT/100;
			int sensorNum = temp.getInt("sensorId");
			
			String[] recordings = serverThreads.get(temp.getInt("sensorId")).getRecordings();
			if (recordings!=null && recordings.length>0){
				int angle = (int)360.0/recordings.length;
				for (int j=0; j<recordings.length; j++){
					// Draw slice of pie
					g.setColor(this.participants.get(recordings[j]));
					g2d.fillArc(tX-size/2, tY-size/2, size, size, j*angle, angle);
				}
			}
			
			// Draw sensor
			int sizeOfsensor = 14;
			g.setColor(Color.black);
			g2d.fillOval(tX-sizeOfsensor, tY-sizeOfsensor, sizeOfsensor*2, sizeOfsensor*2);
			// Show sensor number
			g.setColor(Color.white);
			g2d.drawString(sensorNum+"", tX-5, tY+5);
		}
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.drawImage(bgImage, 0, 0, WIDTH, HEIGHT, null);
		drawLegend(g);
		drawRoom(g);
		drawPieGraph(g);
	}
	
	public void run() {	
		frame = new JFrame("Individual live visualization");
		Visualization liveVis = new Visualization(this.params, this.serverThreads);
		frame.add(liveVis);
		frame.setSize(WIDTH, HEIGHT);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		liveVis.setBackground(Color.WHITE);
		
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH); 
		frame.setUndecorated(true);
		frame.setVisible(true);
		
		while (isRunning) {
			liveVis.repaint();
			try {Thread.sleep(100);} catch (Exception e){}
		}
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
