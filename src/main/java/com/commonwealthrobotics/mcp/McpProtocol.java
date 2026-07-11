package com.commonwealthrobotics.mcp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.neuronrobotics.sdk.common.Log;

/**
 * Open MCP JSON-RPC protocol (tools) backed by {@link CaDoodleAPI}.
 */
public class McpProtocol {
	private static final String PROTOCOL_VERSION = "2024-11-05";
	private final Gson gson = new Gson();
	private volatile CaDoodleAPI api;

	public void setApi(CaDoodleAPI api) {
		this.api = api;
	}

	public JsonObject handleMessage(JsonObject message) {
		if (message == null) {
			return error(null, -32600, "Invalid Request");
		}

		JsonElement idEl = message.has("id") ? message.get("id") : null;
		boolean isNotification = idEl == null || idEl.isJsonNull();
		String method = message.has("method") ? message.get("method").getAsString() : null;
		JsonObject params = message.has("params") && message.get("params").isJsonObject()
				? message.get("params").getAsJsonObject()
				: new JsonObject();

		if (method == null) {
			return isNotification ? null : error(idEl, -32600, "Missing method");
		}

		try {
			switch (method) {
				case "initialize" :
					return result(idEl, handleInitialize(params));
				case "notifications/initialized" :
				case "notifications/cancelled" :
					return null;
				case "ping" :
					return result(idEl, new JsonObject());
				case "tools/list" :
					return result(idEl, toolsList());
				case "tools/call" :
					return result(idEl, toolsCall(params));
				default :
					if (isNotification) {
						return null;
					}
					return error(idEl, -32601, "Method not found: " + method);
			}
		} catch (Exception e) {
			Log.error("MCP method failed: " + method + " — " + e.getMessage());
			e.printStackTrace();
			if (isNotification) {
				return null;
			}
			return error(idEl, -32603, e.getMessage());
		}
	}

	private JsonObject handleInitialize(JsonObject params) {
		String requested = params.has("protocolVersion")
				? params.get("protocolVersion").getAsString()
				: PROTOCOL_VERSION;
		JsonObject result = new JsonObject();
		result.addProperty("protocolVersion",
				"2025-03-26".equals(requested) || "2024-11-05".equals(requested) ? requested : PROTOCOL_VERSION);
		JsonObject capabilities = new JsonObject();
		capabilities.add("tools", new JsonObject());
		result.add("capabilities", capabilities);
		JsonObject serverInfo = new JsonObject();
		serverInfo.addProperty("name", "cadoodle");
		serverInfo.addProperty("version", "1.0.0");
		result.add("serverInfo", serverInfo);
		result.addProperty("instructions",
				"CaDoodle 3D modeling tools. Prefer take_screenshot after visual changes. "
						+ "Use move_camera presets (home/front/left/back/right/top/bottom/fit). "
						+ "operationId values are timeline indexes as strings.");
		return result;
	}

	private JsonObject toolsList() {
		JsonObject result = new JsonObject();
		result.add("tools", gson.toJsonTree(toolDefinitions()));
		return result;
	}

	private JsonObject toolsCall(JsonObject params) throws Exception {
		if (api == null) {
			throw new IllegalStateException("CaDoodle API not ready yet");
		}
		String name = params.has("name") ? params.get("name").getAsString() : null;
		JsonObject args = params.has("arguments") && params.get("arguments").isJsonObject()
				? params.get("arguments").getAsJsonObject()
				: new JsonObject();
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Tool name is required");
		}

		Map<String, Object> argMap = jsonObjectToMap(args);
		List<Map<String, Object>> content = new ArrayList<>();
		boolean isError = false;

		try {
			switch (name) {
				case "get_current_state" :
					content.add(textContent(api.getCurrentState()));
					break;
				case "get_selected" :
					Map<String, Object> selected = new HashMap<>();
					selected.put("selected", api.getSelectedNames());
					content.add(textContent(selected));
					break;
				case "get_csgs" :
					content.add(textContent(api.getCSGs()));
					break;
				case "select_csgs" :
					@SuppressWarnings("unchecked")
					List<String> names = (List<String>) argMap.get("names");
					content.add(textContent(api.selectCSGs(names)));
					break;
				case "add_operation" : {
					Map<String, Object> opParams = new HashMap<>(argMap);
					Object nested = argMap.get("params");
					if (nested instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String, Object> nestedMap = (Map<String, Object>) nested;
						opParams.putAll(nestedMap);
					}
					String operationType = stringArg(argMap, "operationType");
					content.add(textContent(api.addOperation(operationType, opParams)));
					break;
				}
				case "remove_operation" :
					content.add(textContent(api.removeOperation(stringArg(argMap, "operationId"))));
					break;
				case "regenerate" :
					content.add(textContent(api.regenerate(stringArgOrNull(argMap, "sourceOperationId"))));
					break;
				case "get_parameters" :
					content.add(textContent(api.getCSGParameters(stringArg(argMap, "csgName"))));
					break;
				case "set_parameter" :
					content.add(textContent(api.setCSGParameter(stringArg(argMap, "csgName"),
							stringArg(argMap, "parameterName"), argMap.get("value"))));
					break;
				case "set_bounds" : {
					Map<String, Double> bounds = new HashMap<>();
					bounds.put("minX", numberArg(argMap, "minX"));
					bounds.put("minY", numberArg(argMap, "minY"));
					bounds.put("minZ", numberArg(argMap, "minZ"));
					bounds.put("maxX", numberArg(argMap, "maxX"));
					bounds.put("maxY", numberArg(argMap, "maxY"));
					bounds.put("maxZ", numberArg(argMap, "maxZ"));
					content.add(textContent(api.setCSGBounds(stringArg(argMap, "csgName"), bounds)));
					break;
				}
				case "get_shapes_palette" :
					content.add(textContent(api.getShapesPalette()));
					break;
				case "add_shape_by_name" :
					content.add(textContent(api.addShapeByName(stringArg(argMap, "name"))));
					break;
				case "take_screenshot" : {
					Integer width = argMap.containsKey("width") ? ((Number) argMap.get("width")).intValue() : null;
					Integer height = argMap.containsKey("height") ? ((Number) argMap.get("height")).intValue() : null;
					String path = stringArgOrNull(argMap, "path");
					Map<String, Object> shot = api.takeScreenshot(width, height, path);
					content.add(textContent(shot));
					if (Boolean.TRUE.equals(shot.get("success")) && shot.get("path") != null) {
						Path png = Path.of(shot.get("path").toString());
						if (Files.isRegularFile(png)) {
							Map<String, Object> image = new HashMap<>();
							image.put("type", "image");
							image.put("mimeType", "image/png");
							image.put("data", Base64.getEncoder().encodeToString(Files.readAllBytes(png)));
							content.add(0, image);
						}
					} else {
						isError = true;
					}
					break;
				}
				case "move_camera" :
					content.add(textContent(api.moveCamera(argMap)));
					break;
				case "get_camera_state" : {
					Map<String, Object> cam = new HashMap<>();
					cam.put("success", true);
					cam.put("camera", api.getCameraState());
					content.add(textContent(cam));
					break;
				}
				default :
					throw new IllegalArgumentException("Unknown tool: " + name);
			}
		} catch (Exception e) {
			isError = true;
			Map<String, Object> err = new HashMap<>();
			err.put("type", "text");
			err.put("text", "Error: " + e.getMessage());
			content.clear();
			content.add(err);
		}

		// Mark API-level failures
		if (!isError && !content.isEmpty()) {
			Object first = content.get(content.size() - 1);
			if (first instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> textBlock = (Map<String, Object>) first;
				if ("text".equals(textBlock.get("type"))) {
					try {
						@SuppressWarnings("unchecked")
						Map<String, Object> parsed = gson.fromJson(textBlock.get("text").toString(), Map.class);
						if (parsed != null && Boolean.FALSE.equals(parsed.get("success"))) {
							isError = true;
						}
					} catch (Exception ignored) {
					}
				}
			}
		}

		JsonObject result = new JsonObject();
		result.add("content", gson.toJsonTree(content));
		if (isError) {
			result.addProperty("isError", true);
		}
		return result;
	}

	private Map<String, Object> textContent(Object value) {
		Map<String, Object> block = new HashMap<>();
		block.put("type", "text");
		block.put("text", gson.toJson(value));
		return block;
	}

	private static String stringArg(Map<String, Object> args, String key) {
		Object value = args.get(key);
		if (value == null) {
			throw new IllegalArgumentException("Missing argument: " + key);
		}
		return value.toString();
	}

	private static String stringArgOrNull(Map<String, Object> args, String key) {
		Object value = args.get(key);
		return value == null ? null : value.toString();
	}

	private static double numberArg(Map<String, Object> args, String key) {
		Object value = args.get(key);
		if (!(value instanceof Number)) {
			throw new IllegalArgumentException("Missing numeric argument: " + key);
		}
		return ((Number) value).doubleValue();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> jsonObjectToMap(JsonObject obj) {
		return gson.fromJson(obj, Map.class);
	}

	private JsonObject result(JsonElement id, JsonObject result) {
		JsonObject response = new JsonObject();
		response.addProperty("jsonrpc", "2.0");
		setId(response, id);
		response.add("result", result);
		return response;
	}

	private JsonObject error(JsonElement id, int code, String message) {
		JsonObject response = new JsonObject();
		response.addProperty("jsonrpc", "2.0");
		setId(response, id);
		JsonObject error = new JsonObject();
		error.addProperty("code", code);
		error.addProperty("message", message == null ? "Error" : message);
		response.add("error", error);
		return response;
	}

	private static void setId(JsonObject response, JsonElement id) {
		if (id == null || id.isJsonNull()) {
			response.add("id", null);
		} else if (id.isJsonPrimitive()) {
			JsonPrimitive p = id.getAsJsonPrimitive();
			if (p.isNumber()) {
				response.addProperty("id", p.getAsNumber());
			} else if (p.isBoolean()) {
				response.addProperty("id", p.getAsBoolean());
			} else {
				response.addProperty("id", p.getAsString());
			}
		} else {
			response.add("id", id);
		}
	}

	private static List<Map<String, Object>> toolDefinitions() {
		List<Map<String, Object>> tools = new ArrayList<>();
		tools.add(tool("get_current_state", "Get current scene state", objectSchema()));
		tools.add(tool("get_selected", "Get selected CSG names", objectSchema()));
		tools.add(tool("get_csgs", "Get all CSGs with details", objectSchema()));
		tools.add(tool("select_csgs", "Select CSGs by names",
				objectSchema(prop("names", arrayOfStrings("Names of CSGs to select"), true))));
		tools.add(tool("add_operation",
				"Add a new operation. operationType is one of: Align, Delete, ExtrudeSurface, Fillet, Group, Hide, "
						+ "LinearDistribution, Lock, Mirror, MoveCenter, Paste, RadialDistribution, Resize, SetMaterial, Show, "
						+ "ToHole, ToSolid, UnGroup, UnLock, WireMeshView",
				objectSchema(prop("operationType", stringProp("Type of operation"), true),
						prop("params", mapProp("Parameters for the operation"), false))));
		tools.add(tool("remove_operation", "Remove an operation by timeline index or name",
				objectSchema(prop("operationId", stringProp("Timeline index or operation name"), true))));
		tools.add(tool("regenerate", "Regenerate from a source operation; omit sourceOperationId to use current",
				objectSchema(prop("sourceOperationId", stringProp("Timeline index or operation name"), false))));
		tools.add(tool("get_parameters", "Get parameters for a specific CSG",
				objectSchema(prop("csgName", stringProp("Name of CSG"), true))));
		tools.add(tool("set_parameter",
				"Set a CSG parameter (string or numeric mm). parameterName may be a full key or unique suffix like Value",
				objectSchema(prop("csgName", stringProp("Name of CSG"), true),
						prop("parameterName", stringProp("Parameter key or unique suffix"), true),
						prop("value", stringProp("String value, or numeric mm as a string/number"), true))));
		tools.add(tool("set_bounds", "Resize a CSG to the given AABB in the current workplane frame",
				objectSchema(prop("csgName", stringProp("CSG name"), true), prop("minX", numberProp(), true),
						prop("minY", numberProp(), true), prop("minZ", numberProp(), true),
						prop("maxX", numberProp(), true), prop("maxY", numberProp(), true),
						prop("maxZ", numberProp(), true))));
		tools.add(tool("get_shapes_palette", "Get available shapes and primitives", objectSchema()));
		tools.add(tool("add_shape_by_name", "Add a shape from the palette by name or description",
				objectSchema(prop("name", stringProp("Shape name or description"), true))));
		tools.add(tool("take_screenshot", "Capture a PNG screenshot of the live 3D scene view",
				objectSchema(prop("width", integerProp("Output width"), false),
						prop("height", integerProp("Output height"), false),
						prop("path", stringProp("Optional absolute PNG path"), false))));
		tools.add(tool("move_camera",
				"Move/orient the 3D camera and wait for the focus animation to finish. "
						+ "Use preset (home, front, left, back, right, top, bottom, fit) and/or "
						+ "x/y/z, tilt/azimuth/elevation, zoom.",
				objectSchema(prop("preset", stringProp("Named view preset"), false), prop("x", numberProp(), false),
						prop("y", numberProp(), false), prop("z", numberProp(), false),
						prop("tilt", numberProp(), false), prop("azimuth", numberProp(), false),
						prop("elevation", numberProp(), false), prop("zoom", numberProp(), false),
						prop("timeoutMs", integerProp("Max wait for animation"), false))));
		tools.add(tool("get_camera_state", "Get the current 3D camera position, angles, and zoom", objectSchema()));
		return tools;
	}

	private static Map<String, Object> tool(String name, String description, Map<String, Object> inputSchema) {
		Map<String, Object> tool = new HashMap<>();
		tool.put("name", name);
		tool.put("description", description);
		tool.put("inputSchema", inputSchema);
		return tool;
	}

	@SafeVarargs
	private static Map<String, Object> objectSchema(Map<String, Object>... props) {
		Map<String, Object> schema = new HashMap<>();
		schema.put("type", "object");
		Map<String, Object> properties = new HashMap<>();
		List<String> required = new ArrayList<>();
		for (Map<String, Object> prop : props) {
			String key = (String) prop.get("_key");
			Boolean req = (Boolean) prop.get("_required");
			Map<String, Object> def = new HashMap<>(prop);
			def.remove("_key");
			def.remove("_required");
			properties.put(key, def);
			if (Boolean.TRUE.equals(req)) {
				required.add(key);
			}
		}
		schema.put("properties", properties);
		if (!required.isEmpty()) {
			schema.put("required", required);
		}
		return schema;
	}

	private static Map<String, Object> prop(String key, Map<String, Object> def, boolean required) {
		Map<String, Object> out = new HashMap<>(def);
		out.put("_key", key);
		out.put("_required", required);
		return out;
	}

	private static Map<String, Object> stringProp(String description) {
		Map<String, Object> p = new HashMap<>();
		p.put("type", "string");
		if (description != null) {
			p.put("description", description);
		}
		return p;
	}

	private static Map<String, Object> numberProp() {
		Map<String, Object> p = new HashMap<>();
		p.put("type", "number");
		return p;
	}

	private static Map<String, Object> integerProp(String description) {
		Map<String, Object> p = new HashMap<>();
		p.put("type", "integer");
		if (description != null) {
			p.put("description", description);
		}
		return p;
	}

	private static Map<String, Object> mapProp(String description) {
		Map<String, Object> p = new HashMap<>();
		p.put("type", "object");
		p.put("additionalProperties", true);
		if (description != null) {
			p.put("description", description);
		}
		return p;
	}

	private static Map<String, Object> arrayOfStrings(String description) {
		Map<String, Object> p = new HashMap<>();
		p.put("type", "array");
		Map<String, Object> items = new HashMap<>();
		items.put("type", "string");
		p.put("items", items);
		if (description != null) {
			p.put("description", description);
		}
		return p;
	}
}
