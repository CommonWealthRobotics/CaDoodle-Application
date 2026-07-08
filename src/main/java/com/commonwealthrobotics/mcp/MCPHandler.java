package com.commonwealthrobotics.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.neuronrobotics.sdk.common.Log;

/**
 * Handles JSON-RPC communication with MCP clients.
 */
public class MCPHandler implements Runnable {
	private final Socket clientSocket;
	private final CaDoodleAPI api;
	private final Gson gson = new Gson();

	public MCPHandler(Socket socket, CaDoodleAPI api) {
		this.clientSocket = socket;
		this.api = api;
	}

	@Override
	public void run() {
		try (BufferedReader in = new BufferedReader(
				new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
				OutputStreamWriter out = new OutputStreamWriter(clientSocket.getOutputStream(),
						StandardCharsets.UTF_8)) {

			String line;
			while ((line = in.readLine()) != null) {
				try {
					Log.debug("Got: >> "+line);
					JsonObject request = gson.fromJson(line, JsonObject.class);
					JsonObject response = processRequest(request);
					String json = gson.toJson(response);
					Log.debug("Sent:>> "+json);
					out.write(json);
					out.flush();
				} catch (Exception e) {
					Log.error("Error processing request from client");
					JsonObject errorResponse = createErrorResponse(null, "ERROR", e.getMessage());
					out.write(gson.toJson(errorResponse));
					out.flush();
				}
			}
		} catch (IOException e) {
			Log.error("Error handling client");
		} finally {
			try {
				clientSocket.close();
			} catch (IOException e) {
				Log.error("Error closing client socket");
			}
		}
	}

	private JsonObject processRequest(JsonObject request) {
		String method = request.get("method").getAsString();
		JsonObject params = request.get("params") instanceof JsonObject
				? request.get("params").getAsJsonObject()
				: new JsonObject();
		String id = request.has("id") ? request.get("id").getAsString() : null;

		try {
			JsonObject result;
			switch (method) {
				case "initialize" :
					result = handleInitialize(params);
					break;
				case "operations.add" :
					result = handleAddOperation(params);
					break;
				case "operations.remove" :
					result = handleRemoveOperation(params);
					break;
				case "operations.regenerate" :
					result = handleRegenerate(params);
					break;
				case "state.getCurrent" :
					result = handleGetCurrentState(params);
					break;
				case "state.getSelected" :
					result = handleGetSelected(params);
					break;
				case "state.select" :
					result = handleSelect(params);
					break;
				case "state.getCSGs" :
					result = handleGetCSGs(params);
					break;
				case "csg.getParameters" :
					result = handleGetParameters(params);
					break;
				case "csg.setBounds" :
					result = handleSetBounds(params);
					break;
				case "shapes.getPalette" :
					result = handleGetShapesPalette(params);
					break;
				default :
					throw new IllegalArgumentException("Unknown method: " + method);
			}
			return createResponse(id, result);
		} catch (Exception e) {
			Log.error("Error handling method " + method);
			return createErrorResponse(id, "ERROR", e.getMessage());
		}
	}

	private JsonObject handleInitialize(JsonObject params) {
		// Initialize the connection
		return createSuccessResponse(idFromParams(params), new HashMap<String, Object>() {
			{
				put("protocolVersion", "1.0");
				put("serverName", "CaDoodle MCP");
				put("serverVersion", "1.0.0");
			}
		});
	}

	private JsonObject handleAddOperation(JsonObject params) {
		try {
			String operationType = params.get("operationType").getAsString();
			// Parse params as a Map
			@SuppressWarnings("unchecked")
			Map<String, Object> paramMap = gson.fromJson(params, Map.class);
			Map<String, Object> result = api.addOperation(operationType, paramMap);
			return createSuccessResponse(idFromParams(params), result);
		} catch (Exception e) {
			throw new RuntimeException("Failed to add operation", e);
		}
	}

	private JsonObject handleRemoveOperation(JsonObject params) {
		try {
			String operationId = params.get("operationId").getAsString();
			// Remove operation logic here
			return createSuccessResponse(idFromParams(params), new HashMap<String, Object>() {
				{
					put("success", true);
					put("message", "Operation removed: " + operationId);
				}
			});
		} catch (Exception e) {
			throw new RuntimeException("Failed to remove operation", e);
		}
	}

	private JsonObject handleRegenerate(JsonObject params) {
		try {
			String sourceOperationId = params.has("sourceOperationId")
					? params.get("sourceOperationId").getAsString()
					: null;
			// Regenerate logic here
			return createSuccessResponse(idFromParams(params), new HashMap<String, Object>() {
				{
					put("success", true);
					put("message", "Regeneration started");
				}
			});
		} catch (Exception e) {
			throw new RuntimeException("Failed to regenerate", e);
		}
	}

	private JsonObject handleGetCurrentState(JsonObject params) {
		try {
			// Get current state of CaDoodleFile
			return createSuccessResponse(idFromParams(params), api.getCurrentState());
		} catch (Exception e) {
			throw new RuntimeException("Failed to get current state", e);
		}
	}

	private JsonObject handleGetSelected(JsonObject params) {
		try {
			// Get currently selected CSG names
			List<String> selected = api.getSelectedNames();
			return createSuccessResponse(idFromParams(params), new HashMap<String, Object>() {
				{
					put("selected", selected);
				}
			});
		} catch (Exception e) {
			throw new RuntimeException("Failed to get selected items", e);
		}
	}

	private JsonObject handleSelect(JsonObject params) {
		try {
			// Select CSGs by names
			List<String> names = gson.fromJson(params.get("names"), List.class);
			Map<String, Object> result = api.selectCSGs(names);
			return createSuccessResponse(idFromParams(params), result);
		} catch (Exception e) {
			throw new RuntimeException("Failed to select CSGs", e);
		}
	}

	private JsonObject handleGetCSGs(JsonObject params) {
		try {
			// Get list of CSGs with details
			return createSuccessResponse(idFromParams(params), api.getCSGs());
		} catch (Exception e) {
			throw new RuntimeException("Failed to get CSGs", e);
		}
	}

	private JsonObject handleGetParameters(JsonObject params) {
		try {
			String csgName = params.get("csgName").getAsString();
			// Get parameters for a CSG
			return createSuccessResponse(idFromParams(params), api.getCSGParameters(csgName));
		} catch (Exception e) {
			throw new RuntimeException("Failed to get parameters", e);
		}
	}

	private JsonObject handleSetBounds(JsonObject params) {
		try {
			String csgName = params.get("csgName").getAsString();
			double minX = params.get("minX").getAsDouble();
			double minY = params.get("minY").getAsDouble();
			double minZ = params.get("minZ").getAsDouble();
			double maxX = params.get("maxX").getAsDouble();
			double maxY = params.get("maxY").getAsDouble();
			double maxZ = params.get("maxZ").getAsDouble();
			// Set bounds for a CSG
			return createSuccessResponse(idFromParams(params), new HashMap<String, Object>() {
				{
					put("success", true);
					put("message", "Bounds set for: " + csgName);
				}
			});
		} catch (Exception e) {
			throw new RuntimeException("Failed to set bounds", e);
		}
	}

	private JsonObject handleGetShapesPalette(JsonObject params) {
		try {
			// Get shapes palette as structured JSON
			ShapesPalette shapesPalette = new ShapesPalette();
			List<Map<String, Object>> categories = shapesPalette.getShapes();
			return createSuccessResponse(idFromParams(params), new HashMap<String, Object>() {
				{
					put("categories", categories);
				}
			});
		} catch (Exception e) {
			throw new RuntimeException("Failed to get shapes palette", e);
		}
	}

	private String idFromParams(JsonObject params) {
		return params.has("id") ? params.get("id").getAsString() : "unknown";
	}

	private JsonObject createResponse(String id, JsonObject result) {
		JsonObject response = new JsonObject();
		response.addProperty("jsonrpc", "2.0");
		response.addProperty("id", id);
		response.add("result", result);
		return response;
	}

	private JsonObject createSuccessResponse(String id, Object result) {
		JsonObject response = new JsonObject();
		response.addProperty("jsonrpc", "2.0");
		response.addProperty("id", id);
		JsonObject resultObj = new JsonObject();
		resultObj.addProperty("success", true);
		if (result instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> resultMap = (Map<String, Object>) result;
			for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
				resultObj.add(entry.getKey(), gson.toJsonTree(entry.getValue()));
			}
		} else {
			resultObj.add("data", gson.toJsonTree(result));
		}
		response.add("result", resultObj);
		return response;
	}

	private JsonObject createErrorResponse(String id, String code, String message) {
		JsonObject response = new JsonObject();
		response.addProperty("jsonrpc", "2.0");
		response.addProperty("id", id);
		JsonObject error = new JsonObject();
		error.addProperty("code", code);
		error.addProperty("message", message);
		response.add("error", error);
		return response;
	}
}
