package org.cn;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * This where we encapsulate a Node properties
 *
 */
public class Node implements Comparable<Node> {
	private InetAddress address;
	private int port;
	
	public Node(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}
	
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public InetAddress getAddress() {
		return address;
	}
	public void setAddress(InetAddress address) {
		this.address = address;
	}
	
	public String toString() {
		return "(" + address.getHostAddress() + ", " + port + ")";
	}
	
	public static List<String[]> asArray(List<Node> nodes) {
		List<String[]> ret = new ArrayList<String[]>();
		for (Node node : nodes) {
			ret.add(new String[] {node.getAddress().getHostAddress(), Integer.toString(node.getPort())});
		}
		return ret;
	}
	
	@Override 
	public int hashCode() {
		String hashStr = address.getHostAddress() + port;
		return hashStr.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		Node node = (Node) o;
		if (this.getAddress().equals(node.getAddress()) &&
				this.getPort() == node.getPort()) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int compareTo(Node o) {
		if (this.canonicalForm() < o.canonicalForm()) {
			return -1;
		} else if (this.canonicalForm() > o.canonicalForm()) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public int canonicalForm() {
		return Integer.parseInt(address.getHostAddress().replace(".", "")
				+ port);
	}
}
