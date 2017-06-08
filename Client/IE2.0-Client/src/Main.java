/**
 * Program to be executed on each of the computers connected to a reader.
 * This reads tags next to a sensor and sends them back to the server in JSON format.
 * @author Moreno Colombo
 * @version 2.3
 * @since 14.10.2016
 */

public class Main {
	public static void main(String[] args){
		Client client = new Client();
		client.init();
		client.waitForStart();
	}
}
