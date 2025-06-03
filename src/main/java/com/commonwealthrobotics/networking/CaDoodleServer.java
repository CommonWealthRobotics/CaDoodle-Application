package com.commonwealthrobotics.networking;

import java.net.*;

public class CaDoodleServer {
	private String hostAddress = null;

	public String getLocalIP() throws SocketException, UnknownHostException {
		if (hostAddress == null) {
			try (DatagramSocket socket = new DatagramSocket()) {
				socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
				hostAddress = socket.getLocalAddress().getHostAddress();
				socket.close();
			} catch (Exception e) {
				hostAddress = InetAddress.getLocalHost().getHostAddress();
			}
		}
		return hostAddress;
	}
}
