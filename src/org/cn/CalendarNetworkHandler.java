package org.cn;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;

/**
 * Remote XML RPC operations related to calendar network are handled here
 * 
 */
public class CalendarNetworkHandler {
	public boolean need_permission_for(String host, int port, String action,
			String appointmentId, int timestamp) throws UnknownHostException {
		System.out.println(host + ", " + port + " asks for confirmation to do " + action);
		RicartAgrawalaRequest request = new RicartAgrawalaRequest(new Node(
				InetAddress.getByName(host), port), action, appointmentId,
				timestamp);
		CalendarNetwork.ricartAgrawala.getPendingRequests().add(request);

		CalendarNetwork.ricartAgrawala.syncLampartClockWith(timestamp);
		return true;
	}

	/**
	 * Ricart Agrawala processor
	 * 
	 * @return
	 * @throws Exception
	 */
	public boolean process_pending_requests() throws Exception {
		List<RicartAgrawalaRequest> pendingRequests = new ArrayList<RicartAgrawalaRequest>(
				CalendarNetwork.ricartAgrawala.getPendingRequests());
		for (RicartAgrawalaRequest pendingRequest : pendingRequests) {
			RicartAgrawalaRequest earliestSimilarLocalRequest = CalendarNetwork.ricartAgrawala
					.earliestSimilarLocalRequestFor(pendingRequest);
			if (earliestSimilarLocalRequest == null
					|| pendingRequest.compareTo(earliestSimilarLocalRequest) == -1) {
				System.out.println("Giving permission to " + pendingRequest.getNode().toString());
				XmlRpcClient remoteServer = CalendarNetwork
						.getClient(pendingRequest.getNode());
				remoteServer.execute("calendar_network.confirm_request",
						new Object[] { pendingRequest.getAction(),
								pendingRequest.getAppointmentId(),
								pendingRequest.getTimestamp() });

				CalendarNetwork.ricartAgrawala.getPendingRequests().remove(
						pendingRequest);
			}
		}
		return true;
	}

	/**
	 * Ricart Agrawala Receives am acknowledgement from remote online node for
	 * given action
	 * 
	 * @param action
	 * @param appointmentId
	 * @param timestamp
	 * @return
	 */
	public boolean confirm_request(String action, String appointmentId,
			int timestamp) {
		RicartAgrawalaRequest localRequest = new RicartAgrawalaRequest(
				CalendarNetwork.localNode, action, appointmentId, timestamp);
		System.out.println("Confirming " + localRequest);
		Integer count = CalendarNetwork.ricartAgrawala.getRequestConfirmationCount().get(localRequest);
		if (count == null) {
			count = 0;
		}
		CalendarNetwork.ricartAgrawala.getRequestConfirmationCount().put(
				localRequest, count + 1);
		return true;
	}

	/**
	 * Token Ring processor
	 * 
	 * @return
	 * @throws XmlRpcException
	 * @throws MalformedURLException
	 */
	public boolean pass_token() throws XmlRpcException, MalformedURLException {
		Node nextRemoteNode = CalendarNetwork.getNextRemoteNode();
		if (!CalendarNetwork.hasToken
				|| nextRemoteNode.equals(CalendarNetwork.localNode)
				|| CalendarNetwork.needToken) {
			return true;
		}
		//System.out.println("Passing token to: " + nextRemoteNode.toString());

		XmlRpcClient remoteServer = CalendarNetwork.getClient(nextRemoteNode);
		remoteServer.execute("calendar_network.take_token", new Object[] {});
		CalendarNetwork.hasToken = false;
		return true;
	}

	/*
	 * Token Ring token receiver
	 */
	public boolean take_token() {
		//System.out.println("Received token.");
		CalendarNetwork.hasToken = true;
		return true;
	}

	public List<String[]> join(String host, int port)
			throws UnknownHostException, MalformedURLException, XmlRpcException {
		return join(host, port, false);
	}

	/**
	 * returns list of ip, port pairs
	 * 
	 * @param host
	 * @param port
	 * @param onlyJoin
	 * @return
	 * @throws UnknownHostException
	 * @throws MalformedURLException
	 * @throws XmlRpcException
	 */
	public List<String[]> join(String host, int port, boolean onlyJoin)
			throws UnknownHostException, MalformedURLException, XmlRpcException {
		System.out.println("\n(" + host + ", " + port
				+ ") requested to join. Joining...");
		List<Node> tmpOnlineNodes = new ArrayList<>(CalendarNetwork.onlineNodes);

		// add given node to the list of online nodes in this node
		CalendarNetwork.onlineNodes.add(new Node(InetAddress.getByName(host),
				port));

		if (!onlyJoin) {
			System.out.println("Informing other online nodes: ");
			Object[] params = new Object[] { host, port, true };
			// inform all other online nodes
			for (Node node : tmpOnlineNodes) {
				CalendarNetwork.getClient(node).execute(
						"calendar_network.join", params);
				System.out.println(node.toString() + " informed.");
			}
		}
		System.out.println("Joining is done.");
		return Node.asArray(tmpOnlineNodes);
	}

	public boolean bye_guys(String host, int port) throws UnknownHostException {
		return CalendarNetwork.onlineNodes.remove(new Node(InetAddress
				.getByName(host), port));
	}
}
