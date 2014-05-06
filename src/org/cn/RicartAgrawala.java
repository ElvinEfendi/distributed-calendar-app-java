package org.cn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.client.XmlRpcClient;

public class RicartAgrawala implements Runnable {
	private Map<RicartAgrawalaRequest, Integer> requestConfirmationCount;
	private List<RicartAgrawalaRequest> localRequests;
	private List<RicartAgrawalaRequest> pendingRequests;
	private int lampartClock;

	public RicartAgrawala() {
		this.setRequestConfirmationCount(new HashMap<RicartAgrawalaRequest, Integer>());
		this.setLocalRequests(new ArrayList<RicartAgrawalaRequest>());
		this.setPendingRequests(new ArrayList<RicartAgrawalaRequest>());
		this.lampartClock = 0;
	}

	public void syncLampartClockWith(int remoteLampartClock) {
		this.lampartClock = Math.max(lampartClock, remoteLampartClock);
	}

	public RicartAgrawalaRequest earliestSimilarLocalRequestFor(
			RicartAgrawalaRequest pendingRequest) throws Exception {
		List<RicartAgrawalaRequest> similarLocalRequests = new ArrayList<RicartAgrawalaRequest>();
		for (RicartAgrawalaRequest localRequest : localRequests) {
			if (localRequest.isSimilarTo(pendingRequest)) {
				similarLocalRequests.add(localRequest);
			}
		}
		if (similarLocalRequests.size() < 1) {
			return null;
		}
		Collections.sort(similarLocalRequests);
		return similarLocalRequests.get(0);
	}

	public boolean isLockedFor(RicartAgrawalaRequest request) {
		Integer count = this.requestConfirmationCount.get(request);
		if (count == null) {
			count = 0;
		}
		return count >= CalendarNetwork.onlineNodes.size();
	}

	public int incrementAndGetLampartClock() {
		this.lampartClock += 1;
		return this.lampartClock;
	}

	public Map<RicartAgrawalaRequest, Integer> getRequestConfirmationCount() {
		return requestConfirmationCount;
	}

	public void setRequestConfirmationCount(
			Map<RicartAgrawalaRequest, Integer> requestConfirmationCount) {
		this.requestConfirmationCount = requestConfirmationCount;
	}

	public List<RicartAgrawalaRequest> getLocalRequests() {
		return localRequests;
	}

	public void setLocalRequests(List<RicartAgrawalaRequest> localRequests) {
		this.localRequests = localRequests;
	}

	public List<RicartAgrawalaRequest> getPendingRequests() {
		return pendingRequests;
	}

	public void setPendingRequests(List<RicartAgrawalaRequest> pendingRequests) {
		this.pendingRequests = pendingRequests;
	}

	@Override
	public void run() {
		boolean stop = false;
		while (!stop) {
			try {
				Thread.sleep(1000);
				XmlRpcClient remoteServer = CalendarNetwork
						.getClient(CalendarNetwork.localNode);
				remoteServer.execute(
						"calendar_network.process_pending_requests",
						new Object[] {});
			} catch (Exception e) {
				stop = true;
			}
		}
	}
}
