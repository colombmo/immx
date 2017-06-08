import java.awt.EventQueue;

import javax.swing.JFrame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.JLabel;
import javax.swing.JTextField;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.factories.FormFactory;
import com.thingmagic.TagReadData;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.JComboBox;
import javax.swing.JButton;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;


@SuppressWarnings("deprecation")
public class Register {
	
	private JFrame frmGroupsRegistration;
	private JLabel lblComPort;
	private JTextField textField;
	private JLabel lblServerAddr;
	private JTextField textField_3;
	private JLabel lblInterestTag;
	private JLabel lblNewLabel;
	private JLabel label;
	//private JComboBox<InterestTag> comboBox;
	private JButton btnReads;
	private JButton btnSendToDb;
	
	private TagReadData[] tagReads;
	private String serverAddr;
	
	/**
	 * Launch the application.
	 */
	/*public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Register window = new Register();
					window.frmGroupsRegistration.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}*/

	/**
	 * Create the application.
	 */
	public Register() {
		initialize();
	}
	
	/**
	 * Get data from JComboBox from database
	 */
	@SuppressWarnings("resource")
	private InterestTag[] getInterestTags(String serverAddress) {
		try {
			// Get tables from server for this event
			HttpClient client = new DefaultHttpClient();
			HttpGet getRequest = new HttpGet("http://"+serverAddress+"/hubnet/getInterestTags");
			HttpResponse response;
			String token = "";
			
			response = client.execute(getRequest);
			BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
			String res = br.readLine();
			
			Scanner in = new Scanner(res).useDelimiter(":&:");
			List<String> temps = new ArrayList<String>();
			
			// while loop
			while (in.hasNext()) {
				// find next line
				token = in.next();
				temps.add(token);
			}
			
			List<InterestTag> interestTags = new ArrayList<InterestTag>();
			
			for(int i=0; i<temps.size(); i+=2){
				interestTags.add(new InterestTag(Integer.parseInt(temps.get(i)),temps.get(i+1)));
			}
			InterestTag[] interestsArray = interestTags.toArray(new InterestTag[0]);
			in.close();
			
			return interestsArray;
		} catch (Exception e) {
			return new InterestTag[0];
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmGroupsRegistration = new JFrame();
		frmGroupsRegistration.setTitle("Groups Registration");
		frmGroupsRegistration.setBounds(100, 100, 500, 250);
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
				FormFactory.DEFAULT_ROWSPEC,}));
		
		lblServerAddr = new JLabel("Server address:");
		lblServerAddr.setHorizontalAlignment(SwingConstants.CENTER);
		frmGroupsRegistration.getContentPane().add(lblServerAddr, "4, 4, left, default");
		
		textField_3 = new JTextField();
		frmGroupsRegistration.getContentPane().add(textField_3, "8, 4, fill, default");
		textField_3.setText("127.0.0.1");
		textField_3.setColumns(10);
		
		// Listen for changes in the text
		textField_3.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e){
				System.out.println("Focus gained");
			}

			@Override
			public void focusLost(FocusEvent e) {
				System.out.println("Focus lost");
				serverAddr = textField_3.getText();
				InterestTag[] inters = getInterestTags(serverAddr);
				comboBox.setModel(new DefaultComboBoxModel<InterestTag>(inters));
				if (inters.length <= 0){
					JOptionPane.showMessageDialog(null, "Enter a valid server address");
				}
			}
		});
		
		lblComPort = new JLabel("COM port:");
		lblComPort.setHorizontalAlignment(SwingConstants.CENTER);
		frmGroupsRegistration.getContentPane().add(lblComPort, "4, 8, left, default");
		
		textField = new JTextField();
		frmGroupsRegistration.getContentPane().add(textField, "8, 8, fill, default");
		textField.setColumns(10);
		
		lblEvent = new JLabel("Event:");
		frmGroupsRegistration.getContentPane().add(lblInterestTag, "4, 12, left, default");
		
		comboBox = new JComboBox<Event>(getEvents("127.0.0.1"));
		frmGroupsRegistration.getContentPane().add(comboBox, "8, 12, fill, default");
		
		lblNewLabel = new JLabel("# of tag reads:");
		frmGroupsRegistration.getContentPane().add(lblNewLabel, "4, 16, 4, 1");
		
		label = new JLabel("0");
		frmGroupsRegistration.getContentPane().add(label, "8, 16");
		
		btnReads = new JButton("Read 5s");
		frmGroupsRegistration.getContentPane().add(btnReads, "4, 20");
		
		// add the listener to the jbutton to handle the "pressed" event
	    btnReads.addActionListener(new ActionListener(){
	      public void actionPerformed(ActionEvent e){	
	    	  String comPort = textField.getText();
	    	  ReaderFactory r = new ReaderFactory(comPort);
	    	  tagReads = r.read();
	    	  if(tagReads!=null){
	    		  label.setText(""+tagReads.length);
	    		  btnSendToDb.setEnabled(true);
	    	  }
	      }
	    });

	    frmGroupsRegistration.addWindowListener( new WindowAdapter() {
	        public void windowOpened( WindowEvent e ){
	            btnReads.requestFocus();
	        }
	    }); 
	 
		
		btnSendToDb = new JButton("Send to database");
		frmGroupsRegistration.getContentPane().add(btnSendToDb, "8, 20");
		
		// add the listener to the jbutton to handle the "pressed" event
	    btnSendToDb.addActionListener(new ActionListener(){
	      public void actionPerformed(ActionEvent e){
	    	  int interPk = ((InterestTag)comboBox.getSelectedItem()).getPk();
	    	  String[] tagIds = new String[tagReads.length];
	    	  for(int i=0; i<tagIds.length; i++){
	    		  tagIds[i] = tagReads[i].epcString();
	    		  System.out.printf("%s\n",tagIds[i]);
	    	  }
	    	  ForwardData data = new ForwardData(serverAddr,interPk, tagIds);
	    	  try {
	    		  data.send();
	    	  } catch (IOException e1) {
	    		  System.out.println("Data not sent");
	    	  }
	      }
	    });
	    btnSendToDb.setEnabled(false);
	}
}