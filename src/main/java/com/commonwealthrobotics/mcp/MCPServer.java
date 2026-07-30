package com.commonwealthrobotics.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neuronrobotics.sdk.common.Log;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * CaDoodle MCP server using the open Model Context Protocol over Streamable HTTP.
 * Binds to localhost only. Compatible with Cursor and other MCP clients.
 */
public class MCPServer {
	public static final int DEFAULT_PORT = 8080;
	public static final String MCP_PATH = "/mcp";

	private final int port;
	private final Gson gson = new Gson();
	private final McpProtocol protocol = new McpProtocol();

	private HttpServer httpServer;
	private ExecutorService executor;
	private volatile CaDoodleAPI cadoodleAPI;

	public MCPServer(int port) {
		this.port = port;
	}

	public MCPServer() {
		this(DEFAULT_PORT);
	}

	public void setDependencies(com.commonwealthrobotics.ActiveProject activeProject,
			com.commonwealthrobotics.controls.SelectionSession selectionSession) {
		Log.debug("Setting Up API with new project");
		this.cadoodleAPI = new CaDoodleAPI(activeProject, selectionSession);
		protocol.setApi(cadoodleAPI);
	}

	public void start() {
		Log.debug("MCPServer.start() called");
		executor = Executors.newCachedThreadPool(r -> {
			Thread t = new Thread(r, "CaDoodle-MCP");
			t.setDaemon(true);
			return t;
		});

		try {
			httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
			httpServer.createContext(MCP_PATH, this::handleExchange);
			httpServer.createContext("/", this::handleRoot);
			httpServer.setExecutor(executor);
			httpServer.start();
			Log.info("MCP Server (Streamable HTTP) started at http://127.0.0.1:" + port + MCP_PATH);
		} catch (IOException e) {
			Log.error("Failed to start MCP server: " + e.getMessage());
			e.printStackTrace();
			stop();
		}
	}

	public void stop() {
		if (httpServer != null) {
			httpServer.stop(0);
			httpServer = null;
		}
		if (executor != null) {
			executor.shutdownNow();
			executor = null;
		}
		Log.info("MCP Server stopped");
	}

	private void handleRoot(HttpExchange exchange) throws IOException {
		if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
			byte[] body = ("CaDoodle MCP endpoint: " + MCP_PATH).getBytes(StandardCharsets.UTF_8);
			Headers headers = exchange.getResponseHeaders();
			headers.set("Content-Type", "text/plain; charset=utf-8");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(body);
			}
			return;
		}
		exchange.sendResponseHeaders(404, -1);
		exchange.close();
	}

	private void handleExchange(HttpExchange exchange) throws IOException {
		try {
			String method = exchange.getRequestMethod();
			String remote = exchange.getRemoteAddress().getAddress().getHostAddress();
			if (!"127.0.0.1".equals(remote) && !"0:0:0:0:0:0:0:1".equals(remote)) {
				Log.warning("Rejected MCP connection from non-localhost: " + remote);
				exchange.sendResponseHeaders(403, -1);
				exchange.close();
				return;
			}

			addCorsHeaders(exchange);
			if ("OPTIONS".equalsIgnoreCase(method)) {
				exchange.sendResponseHeaders(204, -1);
				exchange.close();
				return;
			}

			if ("GET".equalsIgnoreCase(method)) {
				// Streamable HTTP optional SSE listen; we are request/response only.
				Headers headers = exchange.getResponseHeaders();
				headers.set("Content-Type", "text/event-stream");
				headers.set("Cache-Control", "no-cache");
				exchange.sendResponseHeaders(405, -1);
				exchange.close();
				return;
			}

			if (!"POST".equalsIgnoreCase(method)) {
				exchange.sendResponseHeaders(405, -1);
				exchange.close();
				return;
			}

			String body = readBody(exchange);
			Log.debug("MCP HTTP POST: " + body);
			JsonElement parsed = JsonParser.parseString(body.isEmpty() ? "null" : body);

			if (parsed.isJsonArray()) {
				JsonArray responses = new JsonArray();
				boolean anyResponse = false;
				for (JsonElement el : parsed.getAsJsonArray()) {
					JsonObject response = protocol.handleMessage(el.getAsJsonObject());
					if (response != null) {
						responses.add(response);
						anyResponse = true;
					}
				}
				if (!anyResponse) {
					exchange.sendResponseHeaders(202, -1);
					exchange.close();
					return;
				}
				writeJson(exchange, 200, gson.toJson(responses));
				return;
			}

			if (!parsed.isJsonObject()) {
				writeJson(exchange, 400, errorJson(null, -32700, "Parse error"));
				return;
			}

			JsonObject request = parsed.getAsJsonObject();
			boolean isInitialize = request.has("method") && "initialize".equals(request.get("method").getAsString());
			JsonObject response = protocol.handleMessage(request);
			if (response == null) {
				exchange.sendResponseHeaders(202, -1);
				exchange.close();
				return;
			}

			Headers headers = exchange.getResponseHeaders();
			if (isInitialize) {
				headers.set("Mcp-Session-Id", UUID.randomUUID().toString());
			}
			writeJson(exchange, 200, gson.toJson(response));
		} catch (Exception e) {
			Log.error("MCP HTTP error: " + e.getMessage());
			e.printStackTrace();
			try {
				writeJson(exchange, 500, errorJson(null, -32603, e.getMessage()));
			} catch (Exception ignored) {
				exchange.close();
			}
		}
	}

	private static void addCorsHeaders(HttpExchange exchange) {
		Headers headers = exchange.getResponseHeaders();
		headers.set("Access-Control-Allow-Origin", "*");
		headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		headers.set("Access-Control-Allow-Headers", "Content-Type, Accept, Mcp-Session-Id, MCP-Protocol-Version");
		headers.set("Access-Control-Expose-Headers", "Mcp-Session-Id");
	}

	private static String readBody(HttpExchange exchange) throws IOException {
		try (InputStream in = exchange.getRequestBody()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static void writeJson(HttpExchange exchange, int status, String json) throws IOException {
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		Headers headers = exchange.getResponseHeaders();
		headers.set("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}

	private static String errorJson(Object id, int code, String message) {
		JsonObject response = new JsonObject();
		response.addProperty("jsonrpc", "2.0");
		if (id instanceof Number) {
			response.addProperty("id", (Number) id);
		} else if (id != null) {
			response.addProperty("id", id.toString());
		} else {
			response.add("id", null);
		}
		JsonObject error = new JsonObject();
		error.addProperty("code", code);
		error.addProperty("message", message == null ? "Error" : message);
		response.add("error", error);
		return response.toString();
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
		Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
		server.start();
	}
}
