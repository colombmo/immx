package ch.sichh.baseprogram;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import ch.sichh.helpers.CustomOutputStream;
import ch.sichh.helpers.Event;
import ch.sichh.helpers.Helpers;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.JCheckBox;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;

public class GUI extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static JFrame logWindow;
	private static JTextArea log;
	
	JPanel contentPane;
	private static JTextField serverIP;
	private static JCheckBox chckbxVisualization;
	private static JCheckBox chckbxFiltering;
	private static JButton btnStartReading;
	private static JComboBox<Event> eventSelector;
	
	static SocketFactory s;
	private static boolean canBeStarted;
	
	//Public list of the ipAddresses of the clients
	public static HashMap<Integer, String> connectedClients = new HashMap<Integer,String>();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				initLog();
				logWindow.setVisible(true);

				// Create GUI
				try{
					GUI frame = new GUI();
					// Try to load list of events from server, then show them inside eventSelector
					loadEvents();
					
					try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");} catch (Exception e) {};
					SwingUtilities.updateComponentTreeUI(frame);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	
	/**
	 * Initialize log window
	 */
	private static void initLog(){
		logWindow = new JFrame();
		logWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		logWindow.setTitle("Immersive Experience SICHH");
		logWindow.setType(Type.NORMAL);
		
		int width = 1000;
		int height = 700;
		logWindow.setSize(width, height);
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		SwingUtilities.updateComponentTreeUI(logWindow);
	
		// Create scrollable log area
		log = new JTextArea();
		log.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(log);
		logWindow.getContentPane().add(scrollPane);
		
		PrintStream printStream = new PrintStream(new CustomOutputStream(log)); 
		System.setOut(printStream);
		System.setErr(printStream);
	}
	
	/**
	 * Create the frame.
	 * @param contentPane 
	 */
	public GUI() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 304, 205);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblServerAddress = new JLabel("Server address:");
		lblServerAddress.setBounds(19, 5, 115, 19);
		contentPane.add(lblServerAddress);
		
		serverIP = new JTextField();
		serverIP.setText("127.0.0.1");
		serverIP.setBounds(158, 5, 116, 22);
		contentPane.add(serverIP);
		serverIP.setColumns(10);
		
		chckbxVisualization = new JCheckBox("Visualization");
		chckbxVisualization.setSelected(true);
		chckbxVisualization.setBounds(16, 71, 113, 25);
		contentPane.add(chckbxVisualization);
		
		chckbxFiltering = new JCheckBox("Filtering");
		chckbxFiltering.setSelected(true);
		chckbxFiltering.setBounds(158, 71, 113, 25);
		contentPane.add(chckbxFiltering);
		
		btnStartReading = new JButton("Start reading");
		btnStartReading.setBounds(65, 105, 130, 40);
		contentPane.add(btnStartReading);
		
		JLabel lblEvent = new JLabel("Event:");
		lblEvent.setBounds(19, 40, 115, 16);
		contentPane.add(lblEvent);
		
		eventSelector = new JComboBox<Event>();
		eventSelector.setBounds(158, 40, 116, 22);
		contentPane.add(eventSelector);
		
		// Start reading
		btnStartReading.addActionListener(new ActionListener() {
	        @Override
	        public void actionPerformed(ActionEvent arg0) {
	        	if(btnStartReading.getText().equals("Start reading"))
	        		startReading();
	        	else
	        		stopReading();
	        }
		});
		
		// Listen for changes in the text
		serverIP.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent arg0) {}
			@Override
			public void focusLost(FocusEvent e) {
				loadEvents();
			}
		});
	}
	
	
	public void startReading(){
		if(canBeStarted){
			// Lock button to prevent other clicks
			btnStartReading.setEnabled(false);
					
			// Change button to "Stop reading" and disable all other widgets
			btnStartReading.setText("Stop reading");
			btnStartReading.setEnabled(true);
			serverIP.setEnabled(false);
			chckbxVisualization.setEnabled(false);
			chckbxFiltering.setEnabled(false);
			Event ev = (Event) eventSelector.getSelectedItem();
			int eventID = ev.getId();
			int[] sensorIDs = ev.getSensorList();
			
			// Send start signal to clients
			// Start reading
			s = new SocketFactory(serverIP.getText(), sensorIDs, eventID, chckbxVisualization.isSelected(), chckbxFiltering.isSelected());
			s.sendStartSignal();
		}
	}
	
	// Stop reading
	public static void stopReading(){
		s.stop();
		// Actively wait that all threads have stopped
		while(!s.checkFinishedExecution()){
			try{Thread.sleep(100);}catch(Exception e){};
		}
		//Revert button to "start reading" and enable again other options
		btnStartReading.setText("Start reading");
		serverIP.setEnabled(true);
		chckbxFiltering.setEnabled(true);
		chckbxVisualization.setEnabled(true);
	}
	
	// Load events from server
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void loadEvents(){
		JSONObject JSONEvents;
		ArrayList<Event> events = new ArrayList<Event>();
		if((JSONEvents = Helpers.getFromURL("http://" + serverIP.getText() + "/hubnet/getEvents")) == null){
			// Show error in case of wrong serverIP
			canBeStarted = false;
        	serverIP.setBorder(BorderFactory.createLineBorder(Color.RED));
        	JOptionPane.showMessageDialog(null, "Please insert a working server address", "Server not found", JOptionPane.PLAIN_MESSAGE);
        	btnStartReading.setEnabled(false);
        	events = null;
		}else{
			serverIP.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			JSONArray evs = JSONEvents.getJSONArray("events");
			for(int i=0; i<evs.length(); i++){
				events.add(new Event(evs.getJSONObject(i)));
			}
			eventSelector.setModel(new DefaultComboBoxModel(events.toArray()));
			btnStartReading.setEnabled(true);
			canBeStarted = true;
		}
	}
}
