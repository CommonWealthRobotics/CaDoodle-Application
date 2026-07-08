package com.commonwealthrobotics.mcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.neuronrobotics.sdk.common.Log;

/**
 * MCP Server for CaDoodle. Provides JSON-RPC interface to CaDoodle operations.
 * Only accepts localhost connections for security.
 */
public class MCPServer {
	public static final int DEFAULT_PORT = 8080;
	private static final String ALLOWED_HOST = "127.0.0.1";

	private final int port;
	private ServerSocket serverSocket;
	private ExecutorService executor;
	private volatile boolean running;
	private CaDoodleAPI cadoodleAPI;

	public MCPServer(int port) {
		this.port = port;
		// cadoodleAPI will be set via setDependencies
	}

	public MCPServer() {
		this(DEFAULT_PORT);
	}

	/**
	 * Set dependencies after UI initialization.
	 * This should be called from the JavaFX application thread.
	 */
	public void setDependencies(com.commonwealthrobotics.ActiveProject activeProject,
			com.commonwealthrobotics.controls.SelectionSession selectionSession) {
		this.cadoodleAPI = new CaDoodleAPI(activeProject, selectionSession);
	}

	public void start() {
		System.out.println("MCPServer.start() called");
		running = true;
		executor = Executors.newFixedThreadPool(10);

		try {
			serverSocket = new ServerSocket(port);
			System.out.println("Server socket bound to port " + port);
			Log.info("MCP Server started on port " + port);
			Log.flush();
			System.out.println("Ready to accept connections");

			while (running) {
				try {
					Socket clientSocket = serverSocket.accept();
					System.out.println("Accepted client connection");

					// Only allow localhost connections
					String host = clientSocket.getInetAddress().getHostAddress();
					if (!ALLOWED_HOST.equals(host)) {
						Log.warning("Rejected connection from non-localhost: " + host);
						clientSocket.close();
						continue;
					}

					Log.info("Client connected from: " + host);
					Log.flush();
					System.out.println("Submitting handler to executor");
					executor.submit(new MCPHandler(clientSocket, cadoodleAPI));
				} catch (IOException e) {
					if (running) {
						Log.error("Error accepting connection: " + e.getMessage());
						e.printStackTrace();
						Log.flush();
						System.out.println("Error: " + e.getMessage());
					}
				}
			}
		} catch (IOException e) {
			Log.error("Failed to start MCP server: " + e.getMessage());
			e.printStackTrace();
			Log.flush();
			System.out.println("Failed: " + e.getMessage());
		} finally {
			running = false;
			if (executor != null) {
				executor.shutdown();
			}
			System.out.println("MCPServer.start() finished");
		}
	}

	public void stop() {
		running = false;
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				Log.error("Error closing server socket");
			}
		}
		if (executor != null) {
			executor.shutdown();
		}
		cadoodleAPI.close();
		Log.info("MCP Server stopped");
	}

	public static void main(String[] args) {
		int port = DEFAULT_PORT;
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				Log.error("Invalid port number, using default");
			}
		}

		MCPServer server = new MCPServer(port);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			server.stop();
		}));

		server.start();
	}
}
