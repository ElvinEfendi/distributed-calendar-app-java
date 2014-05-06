package org.cn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;


/**
 * This is a user interface where users will be able to 
 * manage appointments using this module. Can be run
 * without server but this mode is offline mode and
 * in offline mode only read actions are available
 *
 */
public class AppointmentsController {
	private CalendarNetwork cn;
	BufferedReader br;

	public AppointmentsController(CalendarNetwork cn) throws SQLException, XmlRpcException, IOException {
		this.cn = cn;
		
		System.out.println("\nAvailable commands");
		List<String> offlineCommands = 
				Arrays.asList(new String[] {"list", "exit", "close_client"});
		List<String> onlineCommands = 
				Arrays.asList(new String[] {"create", "update", "destroy"});
		System.out.println("Basic commands: list, exit, close_client");
		System.out.println("Only online mode commands: create, update, destroy");
		
		String command = null;
		br = new BufferedReader(new InputStreamReader(System.in));
		
		while(!"close_client".equals(command)) {
			System.out.print("Enter the command you'd like to be executed: ");
			command = br.readLine();
			if (cn.isOnline() && 
					!onlineCommands.contains(command) && 
					!offlineCommands.contains(command) ||
					!cn.isOnline() && !offlineCommands.contains(command)) {
				System.out.println("Entered command is not allowed.");
				continue;
			}
			switch (command) {
			case "list":
				list();
				break;
			case "exit":
				this.cn.quit();
				break;
			case "create":
				create();
				break;
			case "update":
				update();
				break;
			case "destroy":
				destroy();
				break;
			case "close_client":
				if (cn.isOnline()) {
					System.out.println("User interface is closed. Server is still listening for requests.");
				}
				break;
			default:
				break;
			}
		}
	}
	
	private void list() throws SQLException{
		List<Appointment> appointments = Appointment.all();
		if (appointments.isEmpty()) {
			System.out.println("No appointment found.");
		} else {
			prettyPrintTitles();
			for (Appointment appointment : appointments) {
				prettyPrint(appointment);
			}
		}
	}
	
	private int consistentlyLock(String action, String appointmentId) {
		try {
			int timestamp = CalendarNetwork.lockDBFor(action, appointmentId);
			boolean locked = false;
			long timeout = 30000;
			long time = System.currentTimeMillis();
			while (!locked && (System.currentTimeMillis() - timeout < time)) {
				Thread.sleep(1000);
				locked = CalendarNetwork.lockStatusFor(action, appointmentId, timestamp);
			}
			if (!locked) {
				System.out.println("The resource is used by another process. Please try later.");
				CalendarNetwork.unlockDBFor(action, appointmentId, timestamp);
				return -1;
			}
			
			System.out.println(" db is locked.");
			return timestamp;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	private void consistentlyUnlock(String action, String appointmentId, int timestamp) {
		System.out.println("==> unlocking db...");
		try {
			CalendarNetwork.unlockDBFor(action, appointmentId, timestamp);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void create() throws SQLException, XmlRpcException, IOException{
		int timestamp = consistentlyLock("create", "-1");
		if (timestamp > -1) {
			System.out.println("Please, fill following fields to create a new appointment: ");
			Appointment appointment = new Appointment();
			fillFieldsOf(appointment);
			if (appointment.create()) {
				System.out.println("New appointment was successfully created.");
				// propogate this changes to all other online nodes
				propogateChange("create", appointment);
			} else {
				System.out.println("Error happened while saving the appointment.");
			}
			
			consistentlyUnlock("create", "-1", timestamp);
		}
	}
	private void update() throws SQLException, XmlRpcException, IOException{
		System.out.println("Please, fill following fields to create a new appointment: ");
		System.out.print("    Identifier of the appointment: ");
		Appointment appointment = Appointment.findById(Integer.parseInt(br.readLine()));
		int timestamp = consistentlyLock("update", appointment.getUuid().toString());
		if (timestamp > -1) {
			prettyPrintTitles();
			appointment.reload();
			prettyPrint(appointment);
			fillFieldsOf(appointment);
			if (appointment.save()) {
				System.out.println("The appointment was successfully updated.");
				propogateChange("update", appointment);
			} else {
				System.out.println("Error happened while saving the appointment.");
			}
			
			consistentlyUnlock("update", appointment.getUuid().toString(), timestamp);
		}
	}
	private void destroy() throws SQLException, IOException, XmlRpcException{
		System.out.print("Please, enter the identifier of appointment you want to destroy: ");
		Appointment appointment = Appointment.findById(Integer.parseInt(br.readLine()));
		int timestamp = consistentlyLock("destroy", appointment.getUuid().toString());
		if (timestamp > -1) {
			if (appointment.destroy()) {
				System.out.println("The appointment was successfully destroyed.");
				propogateChange("destroy", appointment);
			} else {
				System.out.println("Error happened while destroying the appointment.");
			}
			
			consistentlyUnlock("destroy", appointment.getUuid().toString(), timestamp);
		}
	}
	
	private void prettyPrintTitles() {
		System.out.printf("%-5s %-30s %-25s %-18s %s\n", "ID", "Header", "Start at", "Duration", "Comment");
	}
	private void prettyPrint(Appointment appointment) {
		System.out.printf("%-5s %-30s %-25s %-18s %s\n",
           appointment.getId(),
           appointment.getHeader(),
           appointment.getStartAt(),
           appointment.getDurationInMinutes(),
           appointment.getComment());
	}
	private void fillFieldsOf(Appointment appointment) throws IOException {
		String value = "";
		System.out.print("Header: ");
		value = br.readLine();
		if (value.length() > 0) {
			appointment.setHeader(value);
		}
		
		System.out.print("Start at: ");
		value = br.readLine();
		if (value.length() > 0) {
			appointment.setStartAt(value);
		}
		
		System.out.print("Duration: ");
		value = br.readLine();
		if (value.length() > 0) {
			appointment.setDurationInMinutes(value);
		}
		
		System.out.print("Comment: ");
		value = br.readLine();
		if (value.length() > 0) {
			appointment.setComment(value);
		}
	}
	/*
	 * run the action in all other online nodes
	 * this is another direction of synchronization
	 */
	private void propogateChange(String action, Appointment appointment) throws MalformedURLException, XmlRpcException {
		Object[] params;
		for (Node node : CalendarNetwork.onlineNodes) {
			System.out.print("Excuting the change(s) in " + node.toString() + "... ");
			params = new Object[] {appointment.xmlRpcCompatible()};
			if ((boolean) CalendarNetwork.getClient(node).execute("appointment." + action, params)) {
				System.out.println(" done!");
			} else {
				System.out.println(" could not be done!");
			}
		}
	}
}
