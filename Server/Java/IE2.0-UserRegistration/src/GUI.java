import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.json.JSONArray;
import org.json.JSONObject;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.thingmagic.TagReadData;

import ch.sichh.registration.helpers.Event;
import ch.sichh.registration.helpers.Group;
import ch.sichh.registration.helpers.Helpers;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;

public class GUI {
	private JFrame frmGroupsRegistration;
	private JLabel lblComPort;
	private JTextField textField;
	private JLabel lblServerAddr;
	private JTextField serverAddr;
	private JLabel lblEvent;
	private JLabel lblNewLabel;
	private JLabel label;
	private JLabel lblGroup;
	private JComboBox<Event> eventcb;
	private JComboBox<Group> groupcb;
	private JButton btnReads;
	private JButton btnSendToDb;
	
	private TagReadData[] tagReads;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI window = new GUI();
					SwingUtilities.updateComponentTreeUI(window.frmGroupsRegistration);
					window.frmGroupsRegistration.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GUI() {
		initialize();
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmGroupsRegistration = new JFrame();
		frmGroupsRegistration.setTitle("Groups Registration");
		frmGroupsRegistration.setBounds(100, 100, 400, 400);
		frmGroupsRegistration.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmGroupsRegistration.getContentPane().setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));
		
		lblServerAddr = new JLabel("Server address:");
		lblServerAddr.setHorizontalAlignment(SwingConstants.CENTER);
		frmGroupsRegistration.getContentPane().add(lblServerAddr, "4, 4, left, default");
		
		serverAddr = new JTextField();
		frmGroupsRegistration.getContentPane().add(serverAddr, "8, 4, fill, default");
		serverAddr.setText("127.0.0.1");
		serverAddr.setColumns(10);
		
		// Listen for changes in the text
		serverAddr.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e){}

			@Override
			public void focusLost(FocusEvent e) {
				Event[] evs = getEvents();
				eventcb.setModel(new DefaultComboBoxModel<Event>(evs));
			}
		});
		
		lblComPort = new JLabel("COM port:");
		lblComPort.setHorizontalAlignment(SwingConstants.CENTER);
		frmGroupsRegistration.getContentPane().add(lblComPort, "4, 8, left, default");
		
		textField = new JTextField();
		textField.setText("tmr:///dev/ttyUSB0");
		frmGroupsRegistration.getContentPane().add(textField, "8, 8, fill, default");
		textField.setColumns(10);
		
		lblEvent = new JLabel("Event:");
		frmGroupsRegistration.getContentPane().add(lblEvent, "4, 12, left, default");
		
		lblGroup = new JLabel("Group:");
		frmGroupsRegistration.getContentPane().add(lblGroup, "4, 16, left, default");
		
		groupcb = new JComboBox<Group>();
		frmGroupsRegistration.getContentPane().add(groupcb, "8, 16, fill, default");
		// Do something when a group is selected
		groupcb.addActionListener (new ActionListener () {
		    public void actionPerformed(ActionEvent e) {
		    	// On event change, decide if the button to send to db should be activated
		    	if(tagReads!=null && tagReads.length>0 && groupcb.getSelectedIndex()>=0)
					btnSendToDb.setEnabled(true);
		    }
		});
		
		lblNewLabel = new JLabel("# of tag reads:");
		frmGroupsRegistration.getContentPane().add(lblNewLabel, "4, 20, 4, 1");
		
		label = new JLabel("0");
		frmGroupsRegistration.getContentPane().add(label, "8, 20");
		
		btnReads = new JButton("Read 5s");
		frmGroupsRegistration.getContentPane().add(btnReads, "4, 24");
		
		// add the listener to the jbutton to handle the "pressed" event
	    btnReads.addActionListener(new ActionListener(){
	      public void actionPerformed(ActionEvent e){	
	    	  String comPort = textField.getText();
	    	  ReaderFactory r = new ReaderFactory(comPort);
	    	  try {
				tagReads = r.read();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
	    	  if(tagReads!=null){
	    		  label.setText(""+tagReads.length);
	    		  if(groupcb.getSelectedIndex()>=0)
	    			  btnSendToDb.setEnabled(true);
	    	  }
	      }
	    });

	    eventcb = new JComboBox<Event>(getEvents());
		frmGroupsRegistration.getContentPane().add(eventcb, "8, 12, fill, default");
		// Add listener to eventcb to show the linked groups
		eventcb.addActionListener (new ActionListener () {
		    public void actionPerformed(ActionEvent e) {
		    	// On event change, load the corresponding groups into groupcb
		    	Group[] gr = getGroups(eventcb.getItemAt(eventcb.getSelectedIndex()).getId());
				groupcb.setModel(new DefaultComboBoxModel<Group>(gr));
		    }
		});
	    
	    frmGroupsRegistration.addWindowListener( new WindowAdapter() {
	        public void windowOpened( WindowEvent e ){
	            btnReads.requestFocus();
	        }
	    }); 
	 
		
		btnSendToDb = new JButton("Send to database");
		frmGroupsRegistration.getContentPane().add(btnSendToDb, "8, 24");
		
		// add the listener to the jbutton to handle the "pressed" event
	    btnSendToDb.addActionListener(new ActionListener(){
	      public void actionPerformed(ActionEvent e){
	    	  if(true){//tagReads != null){
		    	  int eventId = ((Event)eventcb.getSelectedItem()).getId();
		    	  int groupId = ((Group)groupcb.getSelectedItem()).getId();
		    	  String[] tagIds = new String[tagReads.length];
		    	  for(int i=0; i<tagIds.length; i++){
		    		  tagIds[i] = tagReads[i].epcString();
		    	  }
		    	  ForwardData data = new ForwardData(serverAddr.getText(), eventId, groupId, tagIds);
		    	  try {
		    		  data.send();
		    	  } catch (IOException e1) {
		    		  JOptionPane.showMessageDialog(null, "Data could not be sent\n"+e1.getMessage(), "Data not sent", JOptionPane.PLAIN_MESSAGE);
		    	  }
	    	  }
	      }
	    });
	    btnSendToDb.setEnabled(false);
	}
	
	// Load events from server
	private Event[] getEvents(){
		JSONObject JSONEvents;
		ArrayList<Event> events = new ArrayList<Event>();
		if((JSONEvents = Helpers.getFromURL("http://" + serverAddr.getText() + "/hubnet/getEvents")) == null){
			// Show error in case of wrong serverIP
			serverAddr.setBorder(BorderFactory.createLineBorder(Color.RED));
        	JOptionPane.showMessageDialog(null, "Please insert a working server address", "Server not found", JOptionPane.PLAIN_MESSAGE);
        	btnReads.setEnabled(false);
        	return new Event[0];
		}else{
			serverAddr.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			JSONArray evs = JSONEvents.getJSONArray("events");
			for(int i=0; i<evs.length(); i++){
				events.add(new Event(evs.getJSONObject(i)));
			}
			btnReads.setEnabled(true);
			return events.toArray(new Event[events.size()]);
		}
	}
	
	private Group[] getGroups(int eventId){
		JSONObject JSONGroups;
		ArrayList<Group> groups = new ArrayList<Group>();
		if((JSONGroups = Helpers.getFromURL("http://" + serverAddr.getText() + "/hubnet/getGroups/"+eventId)) == null){
        	btnSendToDb.setEnabled(false);
        	return new Group[0];
		}else{
			JSONArray gr = JSONGroups.getJSONArray("groups");
			for(int i=0; i<gr.length(); i++){
				groups.add(new Group(gr.getJSONObject(i)));
			}
			return groups.toArray(new Group[groups.size()]);
		}
	}
}
