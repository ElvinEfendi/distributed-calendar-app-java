package org.cn;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Remote XML RPC operations related to appointments are handled here
 * 
 */
public class AppointmentHandler {
	public boolean create(Map<String, String> xmlRpcAppointment) throws SQLException {
		Appointment appointment = new Appointment();
		appointment.setUuid(UUID.fromString(xmlRpcAppointment.get("uuid")));
		appointment.setHeader(xmlRpcAppointment.get("header"));
		appointment.setStartAt(xmlRpcAppointment.get("start_at"));
		appointment.setDurationInMinutes(xmlRpcAppointment.get("duration_in_minutes"));
		appointment.setComment(xmlRpcAppointment.get("comment"));
		
		return appointment.create();
	}
	
	public boolean update(Map<String, String> xmlRpcAppointment) throws SQLException {
		Appointment appointment = Appointment.find(UUID.fromString(xmlRpcAppointment.get("uuid")));
		appointment.setHeader(xmlRpcAppointment.get("header"));
		appointment.setStartAt(xmlRpcAppointment.get("start_at"));
		appointment.setDurationInMinutes(xmlRpcAppointment.get("duration_in_minutes"));
		appointment.setComment(xmlRpcAppointment.get("comment"));
		
		return appointment.save();
	}
	
	public boolean destroy(Map<String, String> xmlRpcAppointment) throws SQLException {
		return Appointment.destroy(UUID.fromString(xmlRpcAppointment.get("uuid")));
	}

	/**
	 * 
	 * @param idChecksumPairs
	 * @return
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException 
	 */
	public Map<String, List<Map<String, String>>> get_delta(
			Map<String, String> idChecksumPairs) throws SQLException, NoSuchAlgorithmException {
		// initialize empty values
		Map<String, List<Map<String, String>>> delta = new HashMap<String, List<Map<String, String>>>();
		delta.put("destroy", new ArrayList<Map<String, String>>());
		delta.put("update", new ArrayList<Map<String, String>>());
		delta.put("create", new ArrayList<Map<String, String>>());
		
		Appointment tmpAppointment = null;
		Set<String> tmpKeys = null;

		Map<String, String> localIdChecksumPairs = Appointment.allIdChecksumPairs();
		// get the intersection of keys
		Set<String> intersectionOfKeys = new HashSet<String>(localIdChecksumPairs.keySet());
		intersectionOfKeys.retainAll(idChecksumPairs.keySet());
		
		// find out keys to destroy in requester node
		tmpKeys = new HashSet<String>(idChecksumPairs.keySet());
		tmpKeys.removeAll(intersectionOfKeys);
		for (String key : tmpKeys) {
			tmpAppointment = new Appointment();
			tmpAppointment.setUuid(UUID.fromString(key));
			delta.get("destroy").add(tmpAppointment.xmlRpcCompatible());
		}
		
		// find out keys(appointment) to create in requester node
		tmpKeys = new HashSet<String>(localIdChecksumPairs.keySet());
		tmpKeys.removeAll(intersectionOfKeys);
		for (String key : tmpKeys) {
			delta.get("create").add(Appointment.find(UUID.fromString(key)).xmlRpcCompatible());
		}
		
		// find out keys(appointment) to update in requester node
		for (String key : intersectionOfKeys) {
			if (!idChecksumPairs.get(key).equals(localIdChecksumPairs.get(key))) {
				delta.get("update").add(Appointment.find(UUID.fromString(key)).xmlRpcCompatible());
			}
		}

		return delta;
	}
}
