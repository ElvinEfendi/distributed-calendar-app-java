package org.cn;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfig;
import org.apache.xmlrpc.server.AbstractReflectiveHandlerMapping;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

/**
 * Works separately from user interface. Handles XML RPC server and joining
 * network.
 * 
 */
public class CalendarNetwork {
	public static final String TOKEN_RING = "token_ring";
	public static final String RICART_AGRAWALA = "ricart_agrawala";
	public static List<Node> onlineNodes;
	public static Properties nodeConfig;
	public static RicartAgrawala ricartAgrawala;
	public static String meAlgorithm;
	public static boolean needToken;
	public static boolean hasToken;
	public static Node localNode;

	private boolean serverIsRunning = false;
	private Node remoteNode;
	private WebServer webServer;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// read configuration file
		Properties config = new Properties();
		try {
			config.load(new FileInputStream("config.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		// initialize the calendar network
		Node remoteNode = null;
		if (args.length == 2) {
			remoteNode = new Node(InetAddress.getByName(args[0]),
					Integer.parseInt(args[1]));
		}
		CalendarNetwork cn = new CalendarNetwork(config, remoteNode);

		// start mutual exclusion processor
		Thread meServerThread;
		if (meAlgorithm.equals(TOKEN_RING)) {
			TokenServer tServer = new TokenServer();
			meServerThread = new Thread(tServer);
		} else if (meAlgorithm.equals(RICART_AGRAWALA)) {
			RicartAgrawala raServer = new RicartAgrawala();
			meServerThread = new Thread(raServer);
		} else {
			meServerThread = null;
			throw new Exception(
					"Chosen Mutual Exclusion algorithm is not implemented.");
		}
		if (meServerThread != null) {
			meServerThread.setDaemon(true);
			meServerThread.start();
		}

		new AppointmentsController(cn);
	}

	public CalendarNetwork(Properties config, Node remoteNode)
			throws NumberFormatException, IOException, XmlRpcException,
			NoSuchAlgorithmException, SQLException {
		nodeConfig = config;
		this.remoteNode = remoteNode;
		// initialize online nodes
		onlineNodes = new ArrayList<Node>();
		if (remoteNode != null) {
			onlineNodes.add(remoteNode);
		}

		meAlgorithm = nodeConfig.getProperty("me_algorithm");
		ricartAgrawala = new RicartAgrawala();
		needToken = false;
		localNode = new Node(InetAddress.getByName(nodeConfig
				.getProperty("localHost")), Integer.parseInt(nodeConfig
				.getProperty("localPort")));

		join();
		startServer();
	}

	/**
	 * return XML RPC client initialized with given params also if credentials
	 * key exists in config.properties adds it to request
	 * 
	 * @return
	 * @throws MalformedURLException
	 */
	public static XmlRpcClient getClient(Node node)
			throws MalformedURLException {
		XmlRpcClientConfigImpl rpcConfig = new XmlRpcClientConfigImpl();
		rpcConfig.setServerURL(new URL("http://"
				+ node.getAddress().getHostAddress() + ":"
				+ Integer.toString(node.getPort())));
		if (nodeConfig.containsKey("credentials")) {
			String credentials = nodeConfig.getProperty("credentials");
			rpcConfig.setBasicUserName(credentials.split("::")[0]);
			rpcConfig.setBasicPassword(credentials.split("::")[1]);
		}
		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(rpcConfig);
		return client;
	}

	public static Node getNextRemoteNode() {
		List<Node> allNodes = new ArrayList<Node>(onlineNodes);
		allNodes.add(localNode);
		Collections.sort(allNodes);
		int localNodeIndex = allNodes.indexOf(localNode);
		return allNodes.get((localNodeIndex + 1) % allNodes.size());
	}

	public static int lockDBFor(String action, String appointmentId)
			throws Exception {
		int timestamp = ricartAgrawala.incrementAndGetLampartClock();
		System.out.print("==> Locking db...  ");
		if (meAlgorithm.equals(TOKEN_RING)) {
			needToken = true;
		} else if (meAlgorithm.equals(RICART_AGRAWALA)) {
			for (Node remoteNode : onlineNodes) {
				XmlRpcClient remoteServer = CalendarNetwork
						.getClient(remoteNode);
				remoteServer.execute("calendar_network.need_permission_for",
						new Object[] { localNode.getAddress().getHostAddress(),
								localNode.getPort(), action, appointmentId,
								timestamp });
			}
			ricartAgrawala.getLocalRequests().add(
					new RicartAgrawalaRequest(localNode, action, appointmentId,
							timestamp));

		} else {
			throw new Exception(
					"Chosen Mutual Exclusion algorithm is not implemented.");
		}
		return timestamp;
	}

	public static void unlockDBFor(String action, String appointmentId,
			int timestamp) throws Exception {
		if (meAlgorithm.equals(TOKEN_RING)) {
			needToken = false;
		} else if (meAlgorithm.equals(RICART_AGRAWALA)) {
			RicartAgrawalaRequest localRequest = new RicartAgrawalaRequest(
					localNode, action, appointmentId, timestamp);
			ricartAgrawala.getRequestConfirmationCount().remove(localRequest);
			ricartAgrawala.getLocalRequests().remove(localRequest);
		} else {
			throw new Exception(
					"Chosen Mutual Exclusion algorithm is not implemented.");
		}
	}

	public static boolean lockStatusFor(String action, String appointmentId,
			int timestamp) throws Exception {
		if (meAlgorithm.equals(TOKEN_RING)) {
			return hasToken;
		} else if (meAlgorithm.equals(RICART_AGRAWALA)) {
			RicartAgrawalaRequest localRequest = new RicartAgrawalaRequest(
					localNode, action, appointmentId, timestamp);
			return ricartAgrawala.isLockedFor(localRequest);
		} else {
			throw new Exception(
					"Chosen Mutual Exclusion algorithm is not implemented.");
		}
	}

	/**
	 * if an online remote Node is given then using it joins to the network and
	 * to initial one way(from network to local) synchronization
	 * 
	 * @throws XmlRpcException
	 * @throws MalformedURLException
	 * @throws UnknownHostException
	 * @throws NumberFormatException
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 */
	private void join() throws MalformedURLException, XmlRpcException,
			NumberFormatException, UnknownHostException,
			NoSuchAlgorithmException, SQLException {
		if (this.remoteNode == null) {
			// if there is no network other than this, init token
			hasToken = true;
			return;
		}
		System.out.print("Joining to the network... ");
		XmlRpcClient remoteServer = CalendarNetwork.getClient(remoteNode);
		Object[] otherOnlineNodes = (Object[]) remoteServer
				.execute(
						"calendar_network.join",
						new Object[] {
								nodeConfig.getProperty("localHost"),
								Integer.parseInt(nodeConfig
										.getProperty("localPort")) });

		// add online nodes to the list
		Object[] hostPortPair;
		for (Object pair : otherOnlineNodes) {
			hostPortPair = (Object[]) pair;
			CalendarNetwork.onlineNodes.add(new Node(InetAddress
					.getByName((String) hostPortPair[0]),
					(Integer) hostPortPair[1]));
		}
		Appointment.synchronize(remoteServer);
		System.out.println(" successfully joined to the network!");
	}

	/**
	 * Start the XML RPC server and listen for requests
	 * 
	 * @throws NumberFormatException
	 * @throws IOException
	 * @throws XmlRpcException
	 */
	private void startServer() throws NumberFormatException, IOException,
			XmlRpcException {
		System.out.println("Starting the server...");
		webServer = new WebServer(Integer.parseInt(nodeConfig
				.getProperty("localPort")), InetAddress.getByName(nodeConfig
				.getProperty("localHost")));

		XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();

		PropertyHandlerMapping phm = new PropertyHandlerMapping();
		phm.addHandler("calendar_network", org.cn.CalendarNetworkHandler.class);
		phm.addHandler("appointment", org.cn.AppointmentHandler.class);
		if (nodeConfig.containsKey("credentials")) {
			setAuthenticationHandler(phm);
		}
		xmlRpcServer.setHandlerMapping(phm);

		XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer
				.getConfig();
		serverConfig.setEnabledForExtensions(true);
		serverConfig.setContentLengthOptional(false);
		webServer.start();
		System.out.println("Server is started.");
		this.serverIsRunning = true;
	}

	public boolean isOnline() {
		return this.serverIsRunning && onlineNodes.size() > 0;
	}

	/**
	 * also inform others
	 * 
	 * @throws XmlRpcException
	 * @throws MalformedURLException
	 */
	public void quit() throws MalformedURLException, XmlRpcException {
		System.out.println("Quitting calendar network...");
		if (isOnline()) {
			System.out.println("Informing other online nodes...");
			for (Node node : onlineNodes) {
				if ((boolean) CalendarNetwork.getClient(node).execute(
						"calendar_network.bye_guys",
						new Object[] {
								nodeConfig.getProperty("localHost"),
								Integer.parseInt(nodeConfig
										.getProperty("localPort")) })) {
					System.out.println(node.toString()
							+ " was successfully informed.");
				} else {
					System.out.println("Something went wrong while informing "
							+ node.toString());
				}
			}
		}
		if (serverIsRunning) {
			// shut the server down
			webServer.shutdown();
			this.serverIsRunning = false;
		}
	}

	/*
	 * set basic authentication credentials = username + "::" + password
	 */
	private void setAuthenticationHandler(PropertyHandlerMapping phm) {
		AbstractReflectiveHandlerMapping.AuthenticationHandler handler = new AbstractReflectiveHandlerMapping.AuthenticationHandler() {
			public boolean isAuthorized(XmlRpcRequest pRequest) {
				XmlRpcHttpRequestConfig requestConfig = (XmlRpcHttpRequestConfig) pRequest
						.getConfig();
				return nodeConfig.getProperty("credentials").equals(
						requestConfig.getBasicUserName() + "::"
								+ requestConfig.getBasicPassword());
			};
		};
		phm.setAuthenticationHandler(handler);
	}
}
