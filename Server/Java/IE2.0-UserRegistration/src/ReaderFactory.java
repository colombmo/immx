import java.util.Date;

import javax.swing.JOptionPane;

import com.thingmagic.Reader;
import com.thingmagic.TMConstants;
import com.thingmagic.TagReadData;

public class ReaderFactory{

	// Variables.
	private String comPort;

	// Constructor,
	public ReaderFactory (String comPort){
		this.comPort = comPort;
	}

	// Read tags with the sensor
	public TagReadData[] read() throws Exception {
		// Create Reader object, connecting to physical device
		Reader reader = null;
		TagReadData[] tagReads;
		try{

			reader = Reader.create(comPort);
			reader.connect();
		}catch(Exception e){
			JOptionPane.showMessageDialog(null, "Enter a valid COM port of the form \'com<number>\'");
			return null;
		}
			if (Reader.Region.UNSPEC == (Reader.Region)reader.paramGet("/reader/region/id")){
				Reader.Region[] supportedRegions = (Reader.Region[])reader.paramGet(TMConstants.TMR_PARAM_REGION_SUPPORTEDREGIONS);
				if (supportedRegions.length < 1){
					throw new Exception("Reader doesn't support any regions");
				}
				else{
					reader.paramSet("/reader/region/id", supportedRegions[0]);
				}
			}

			// Console output.
			System.out.println(new Date().toString() + "> Reader created and listening"); 

			// Read tags
			tagReads = reader.read(4000);
			reader.destroy();
			return tagReads;
	}
}