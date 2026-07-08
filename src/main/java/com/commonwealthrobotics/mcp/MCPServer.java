package com.commonwealthrobotics.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private final CaDoodleAPI cadoodleAPI;
    
    public MCPServer(int port) {
        this.port = port;
        this.cadoodleAPI = new CaDoodleAPI();
    }
    
    public MCPServer() {
        this(DEFAULT_PORT);
    }
    
    public void start() {
        running = true;
        executor = Executors.newFixedThreadPool(10);
        
        try {
            serverSocket = new ServerSocket(port);
            Log.info("MCP Server started on port " + port);
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    
                    // Only allow localhost connections
                    String host = clientSocket.getInetAddress().getHostAddress();
                    if (!ALLOWED_HOST.equals(host)) {
                        Log.warning("Rejected connection from non-localhost: " + host);
                        clientSocket.close();
                        continue;
                    }
                    
                    Log.info("Client connected from: " + host);
                    executor.submit(new MCPHandler(clientSocket, cadoodleAPI));
                } catch (IOException e) {
                    if (running) {
                        Log.error("Error accepting connection");
                    }
                }
            }
        } catch (IOException e) {
            Log.error("Failed to start MCP server");
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
