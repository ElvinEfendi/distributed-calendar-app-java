package org.cn;

import java.util.ArrayList;
import java.util.List;

public class RicartAgrawalaRequest implements Comparable<RicartAgrawalaRequest> {
	private String action;
	private String appointmentId;
	private int timestamp;
	private Node node;

	public RicartAgrawalaRequest(Node node, String action,
			String appointmentId, int timestamp) {
		this.node = node;
		this.action = action;
		this.appointmentId = appointmentId;
		this.timestamp = timestamp;
	}
	
	public boolean isSimilarTo(RicartAgrawalaRequest o) throws Exception {
		List<String> udArr = new ArrayList<String>();
		udArr.add("update");
		udArr.add("destroy");
		if (action.equals("create") && o.getAction().equals("create")) {
			return true;
		} else if (udArr.contains(action) || udArr.contains(o.getAction())) {
			return appointmentId.equals(o.getAppointmentId());
		} else {
			throw new Exception("This method works only for 'create', 'update' and 'destroy' methods.");
		}
	}

	@Override
	public int compareTo(RicartAgrawalaRequest o) {
		if (this.timestamp < o.getTimestamp()) {
			return -1;
		} else if (this.timestamp > o.getTimestamp()) {
			return 1;
		} else if (this.timestamp == o.getTimestamp() && this.node != null) {
			return this.node.compareTo(o.getNode());
		} else {
			return 0;
		}
	}
	
	@Override 
	public int hashCode() {
		String hashStr = String.valueOf(node.hashCode()) + action
				+ appointmentId + timestamp;
		return hashStr.hashCode();
	}
	
	@Override 
	public boolean equals(Object other) {
		if (!(other instanceof RicartAgrawalaRequest)) {
			return false;
		}
		
		RicartAgrawalaRequest otherRequest = (RicartAgrawalaRequest) other;
		return action.equals(otherRequest.getAction()) &&
				appointmentId.equals(otherRequest.getAppointmentId()) &&
				node.equals(otherRequest.getNode()) &&
				timestamp == otherRequest.getTimestamp();
	}

	public Node getNode() {
		return node;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public String getAction() {
		return action;
	}

	public String getAppointmentId() {
		return appointmentId;
	}
}
