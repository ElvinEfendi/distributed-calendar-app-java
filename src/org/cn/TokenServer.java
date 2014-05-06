package org.cn;

import org.apache.xmlrpc.client.XmlRpcClient;

public class TokenServer implements Runnable {
	public void run() {
		boolean stop = false;
		int reTry = 0;
		while (!stop) {
			try {
				Thread.sleep(2000);
				XmlRpcClient remoteServer = CalendarNetwork.getClient(CalendarNetwork.localNode);
				remoteServer.execute("calendar_network.pass_token", new Object[] {});
			} catch (Exception e) {
				reTry++;
				if (reTry > 3) {
					stop = true;
				}
			}
		}
	}
}
