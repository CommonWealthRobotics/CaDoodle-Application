package com.commonwealthrobotics.mcp;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AbstractAddFrom;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Align;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Delete;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ExtrudeSurface;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.FailedToApplyOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.FilletChamfer;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Group;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Hide;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.INamedOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.LinearDistribution;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Lock;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Mirror;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.MirrorOrientation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.MoveCenter;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Paste;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.RadialDistribution;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Resize;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.SetMaterial;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Show;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ToHole;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ToSolid;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.UnGroup;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.UnLock;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.WireMeshView;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.Log;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.parametrics.CSGDatabaseInstance;
import eu.mihosoft.vrl.v3d.parametrics.Parameter;

/**
 * API wrapper for CaDoodle operations.
 * This class provides a clean interface for MCP to interact with activeProject.get().
 */
public class CaDoodleAPI {
	private final ActiveProject activeProject;
	private final SelectionSession selectionSession;


	// Operation type registry
	private static final Map<String, Class<? extends com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation>> OPERATION_TYPES = new HashMap<>();
	static {
		OPERATION_TYPES.put("Align", Align.class);
		OPERATION_TYPES.put("Delete", Delete.class);
		OPERATION_TYPES.put("ExtrudeSurface", ExtrudeSurface.class);
		OPERATION_TYPES.put("Fillet", FilletChamfer.class);
		OPERATION_TYPES.put("Group", Group.class);
		OPERATION_TYPES.put("Hide", Hide.class);
		OPERATION_TYPES.put("LinearDistribution", LinearDistribution.class);
		OPERATION_TYPES.put("Lock", Lock.class);
		OPERATION_TYPES.put("Mirror", Mirror.class);
		OPERATION_TYPES.put("MoveCenter", MoveCenter.class);
		OPERATION_TYPES.put("Paste", Paste.class);
		OPERATION_TYPES.put("RadialDistribution", RadialDistribution.class);
		OPERATION_TYPES.put("Resize", Resize.class);
		OPERATION_TYPES.put("SetMaterial", SetMaterial.class);
		OPERATION_TYPES.put("Show", Show.class);
		OPERATION_TYPES.put("ToHole", ToHole.class);
		OPERATION_TYPES.put("ToSolid", ToSolid.class);
		OPERATION_TYPES.put("UnGroup", UnGroup.class);
		OPERATION_TYPES.put("UnLock", UnLock.class);
		OPERATION_TYPES.put("WireMeshView", WireMeshView.class);
	}

	public CaDoodleAPI(ActiveProject activeProject, SelectionSession selectionSession) {
		this.activeProject = activeProject;
		this.selectionSession = selectionSession;
	}


	/**
	 * Add an operation to the activeProject.get().
	 * @param operationType The type of operation to add.
	 * @param params Parameters for the operation.
	 * @return Result as JSON-serializable map.
	 */
	public Map<String, Object> addOperation(String operationType, Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		try {
			Class<? extends com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation> opClass = OPERATION_TYPES
					.get(operationType);
			if (opClass == null) {
				result.put("success", false);
				result.put("error", "Unknown operation type: " + operationType);
				return result;
			}

			// Create operation instance
			com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation op = opClass.getDeclaredConstructor()
					.newInstance();

			// Configure operation based on type
			configureOperation(op, operationType, params);

			// Add operation to project
			Thread thread = activeProject.addOp(op);

			// Wait for operation to complete with timeout
			if (thread != null) {
				try {
					thread.join(30000);
					if (thread.isAlive()) {
						result.put("error", "Operation timed out");
						thread.interrupt();
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					result.put("error", "Operation interrupted");
				}
			}

			List<String> addedNames = op.getNamesAddedInThisOperation();
			result.put("success", true);
			result.put("addedNames", addedNames);
			result.put("operationType", operationType);

		} catch (Exception e) {
			result.put("success", false);
			result.put("error", e.getMessage());
		}
		return result;
	}

	private void configureOperation(com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation op,
			String type, Map<String, Object> params) {
		// Common: set names if present
		if (params.containsKey("names")) {
			List<String> names = toStringList(params.get("names"));
			try {
				op.getClass().getMethod("setNames", List.class).invoke(op, names);
			} catch (Exception e) {
				Log.error("Failed to set names on operation: " + type);
			}
		}

		// Common: set workplane if present
		if (params.containsKey("workplane")) {
			try {
				TransformNR wp = toTransformNR(params.get("workplane"));
				try {
					java.lang.reflect.Method setWorkplane = op.getClass().getMethod("setWorkplane", TransformNR.class);
					setWorkplane.invoke(op, wp);
				} catch (java.lang.reflect.InvocationTargetException | NoSuchMethodException e) {
					// Operation doesn't support workplane
				}
			} catch (Exception e) {
				Log.error("Failed to set workplane: " + e.getMessage());
			}
		}

		// Type-specific configuration
		switch (type) {
			case "Align" :
				if (op instanceof Align) {
					Align align = (Align) op;
					if (params.containsKey("bounds")) {
						List<String> boundNames = toStringList(params.get("bounds"));
						align.setBounds(boundNames);
					}
					// Set alignment parameters (x, y, z)
					if (params.containsKey("x")) {
						try {
							com.neuronrobotics.bowlerstudio.scripting.cadoodle.Alignment alignment = com.neuronrobotics.bowlerstudio.scripting.cadoodle.Alignment
									.valueOf(params.get("x").toString());
							align.x = alignment;
						} catch (Exception e) {
							Log.error("Invalid alignment value: " + params.get("x"));
						}
					}
					if (params.containsKey("y")) {
						try {
							com.neuronrobotics.bowlerstudio.scripting.cadoodle.Alignment alignment = com.neuronrobotics.bowlerstudio.scripting.cadoodle.Alignment
									.valueOf(params.get("y").toString());
							align.y = alignment;
						} catch (Exception e) {
							Log.error("Invalid alignment value: " + params.get("y"));
						}
					}
					if (params.containsKey("z")) {
						try {
							com.neuronrobotics.bowlerstudio.scripting.cadoodle.Alignment alignment = com.neuronrobotics.bowlerstudio.scripting.cadoodle.Alignment
									.valueOf(params.get("z").toString());
							align.z = alignment;
						} catch (Exception e) {
							Log.error("Invalid alignment value: " + params.get("z"));
						}
					}
				}
				break;
			case "Delete" :
			case "Hide" :
			case "Show" :
			case "Lock" :
			case "UnLock" :
			case "ToHole" :
			case "ToSolid" :
			case "UnGroup" :
			case "WireMeshView" :
				// Configured via setNames above
				break;
			case "Group" :
				if (op instanceof Group) {
					Group group = (Group) op;
					if (params.containsKey("intersect")) {
						group.setIntersect(Boolean.parseBoolean(params.get("intersect").toString()));
					}
					if (params.containsKey("hull")) {
						group.setHull(Boolean.parseBoolean(params.get("hull").toString()));
					}
				}
				break;
			case "ExtrudeSurface" :
				if (op instanceof ExtrudeSurface) {
					ExtrudeSurface ext = (ExtrudeSurface) op;
					Object targets = params.containsKey("toExtrude") ? params.get("toExtrude") : params.get("toFillet");
					Set<String> toExtrude = toStringSet(targets);
					if (toExtrude.isEmpty() && params.containsKey("names")) {
						toExtrude = toStringSet(params.get("names"));
					}
					if (!toExtrude.isEmpty()) {
						ext.setToExtrude(toExtrude);
					}
					if (params.containsKey("sweep")) {
						ext.setSweep(Boolean.parseBoolean(params.get("sweep").toString()));
					}
					if (params.containsKey("defz")) {
						ext.setDefz(((Number) params.get("defz")).doubleValue());
					}
					if (params.containsKey("defrad")) {
						ext.setDefrad(((Number) params.get("defrad")).doubleValue());
					}
					if (params.containsKey("defstep")) {
						ext.setDefstep(((Number) params.get("defstep")).doubleValue());
					}
					if (params.containsKey("defangle")) {
						ext.setDefangle(((Number) params.get("defangle")).doubleValue());
					}
					if (params.containsKey("defSpiral")) {
						ext.setDefSpiral(((Number) params.get("defSpiral")).doubleValue());
					}
				}
				break;
			case "Fillet" :
				if (op instanceof FilletChamfer) {
					FilletChamfer fillet = (FilletChamfer) op;
					Set<String> toFillet = toStringSet(params.get("toFillet"));
					if (toFillet.isEmpty() && params.containsKey("names")) {
						toFillet = toStringSet(params.get("names"));
					}
					if (!toFillet.isEmpty()) {
						fillet.setToFillet(toFillet);
					}
				}
				break;
			case "LinearDistribution" :
				if (op instanceof LinearDistribution) {
					LinearDistribution dist = (LinearDistribution) op;
					if (params.containsKey("names")) {
						dist.setNames(toStringList(params.get("names")));
					}
					if (params.containsKey("workplane")) {
						dist.setWorkplane(toTransformNR(params.get("workplane")));
					}
				}
				break;
			case "Mirror" :
				if (op instanceof Mirror) {
					Mirror mirror = (Mirror) op;
					if (params.containsKey("mirrorPlane") || params.containsKey("location")) {
						Object planeVal = params.containsKey("mirrorPlane")
								? params.get("mirrorPlane")
								: params.get("location");
						try {
							MirrorOrientation plane = MirrorOrientation.valueOf(planeVal.toString());
							mirror.setLocation(plane);
						} catch (Exception e) {
							Log.error("Invalid mirror plane: " + planeVal);
						}
					}
				}
				break;
			case "MoveCenter" :
				if (op instanceof MoveCenter) {
					MoveCenter move = (MoveCenter) op;
					List<String> moveNames = toStringList(params.get("names"));
					if (!moveNames.isEmpty()) {
						move.setNames(moveNames, activeProject.get());
					}
					try {
						if (params.containsKey("location")) {
							move.setLocation(toTransformNR(params.get("location")));
						} else {
							move.setLocation(toTransformNR(params));
						}
					} catch (Exception e) {
						throw new RuntimeException("Invalid MoveCenter location: " + e.getMessage(), e);
					}
				}
				break;
			case "Paste" :
				if (op instanceof Paste) {
					Paste paste = (Paste) op;
					if (params.containsKey("location")) {
						paste.setLocation(toTransformNR(params.get("location")));
					} else if (params.containsKey("translateX") || params.containsKey("translateY")
							|| params.containsKey("translateZ")) {
						paste.setLocation(toTransformNR(params));
					} else {
						// Default paste offset like the UI
						paste.setLocation(new TransformNR(20, 0, 0));
					}
				}
				break;
			case "RadialDistribution" :
				if (op instanceof RadialDistribution) {
					RadialDistribution dist = (RadialDistribution) op;
					if (params.containsKey("names")) {
						dist.setNames(toStringList(params.get("names")));
					}
					if (params.containsKey("workplane")) {
						dist.setWorkplane(toTransformNR(params.get("workplane")));
					}
				}
				break;
			case "Resize" :
				if (op instanceof Resize) {
					Resize resize = (Resize) op;
					if (params.containsKey("names")) {
						resize.setNames(toStringList(params.get("names")));
					}
					if (params.containsKey("workplane")) {
						resize.setWorkplane(toTransformNR(params.get("workplane")));
					} else {
						try {
							resize.setWorkplane(selectionSession.getWorkplane().copy());
						} catch (Exception e) {
							resize.setWorkplane(new TransformNR());
						}
					}
					if (params.containsKey("height") && params.containsKey("leftFront")
							&& params.containsKey("rightRear")) {
						resize.setResize(toTransformNR(params.get("height")), toTransformNR(params.get("leftFront")),
								toTransformNR(params.get("rightRear")));
					} else if (paramsContainsBounds(params)) {
						applyBoundsToResize(resize, params);
					}
				}
				break;
			case "SetMaterial" :
				if (op instanceof SetMaterial) {
					SetMaterial setMat = (SetMaterial) op;
					if (params.containsKey("materialType")) {
						setMat.setMaterialType(params.get("materialType").toString());
					}
					if (params.containsKey("material")) {
						setMat.setMaterial(params.get("material").toString());
					}
					if (params.containsKey("infillPercent")) {
						setMat.setInfillPercent(((Number) params.get("infillPercent")).doubleValue());
					}
					if (params.containsKey("density")) {
						setMat.setDensity(((Number) params.get("density")).doubleValue());
					}
				}
				break;
		}
	}

	/**
	 * Remove an operation from the project by timeline index or operation name.
	 * @param operationId Timeline index (preferred) or named-operation name.
	 * @return Result as JSON-serializable map.
	 */
	public Map<String, Object> removeOperation(String operationId) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (activeProject.get() == null || !activeProject.get().isInitialized()) {
				result.put("success", false);
				result.put("error", "Project not initialized");
				return result;
			}
			if (activeProject.get().isOperationRunning() || activeProject.get().isRegenerating()) {
				result.put("success", false);
				result.put("error", "Cannot remove operation while another operation is running");
				return result;
			}

			ResolvedOperation resolved = resolveOperation(operationId);
			if (resolved == null) {
				result.put("success", false);
				result.put("error", "Operation not found: " + operationId);
				return result;
			}
			if (resolved.index == 0 && activeProject.get().getOperations().size() == 1) {
				result.put("success", false);
				result.put("error", "Cannot remove the only operation in the timeline");
				return result;
			}

			Thread thread = activeProject.get().deleteOperation(resolved.operation);
			waitForThread(thread, 120000, result);
			if (result.containsKey("error")) {
				result.put("success", false);
				return result;
			}

			result.put("success", true);
			result.put("removedIndex", resolved.index);
			result.put("removedType", resolved.operation.getType());
			result.put("message", "Operation removed: " + operationId);
		} catch (FailedToApplyOperation e) {
			result.put("success", false);
			result.put("error", e.getMessage());
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", e.getMessage());
		}
		return result;
	}

	/**
	 * Regenerate operations from a source operation (timeline index or name).
	 * @param sourceOperationId Timeline index (preferred) or named-operation name.
	 * @return Result as JSON-serializable map.
	 */
	public Map<String, Object> regenerate(String sourceOperationId) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (activeProject.get() == null || !activeProject.get().isInitialized()) {
				result.put("success", false);
				result.put("error", "Project not initialized");
				return result;
			}
			if (activeProject.get().isOperationRunning() || activeProject.get().isRegenerating()) {
				result.put("success", false);
				result.put("error", "Cannot regenerate while another operation is running");
				return result;
			}

			CaDoodleOperation sourceOp;
			int sourceIndex;
			if (sourceOperationId == null || sourceOperationId.isEmpty()) {
				sourceOp = activeProject.get().getCurrentOperation();
				sourceIndex = activeProject.get().getCurrentIndex();
			} else {
				ResolvedOperation resolved = resolveOperation(sourceOperationId);
				if (resolved == null) {
					result.put("success", false);
					result.put("error", "Source operation not found: " + sourceOperationId);
					return result;
				}
				sourceOp = resolved.operation;
				sourceIndex = resolved.index;
			}

			Thread thread = activeProject.regenerateFrom(sourceOp);
			waitForThread(thread, 120000, result);
			if (result.containsKey("error")) {
				result.put("success", false);
				return result;
			}

			// regenerateFrom restores the prior timeline cursor; jump to the end so the
			// full regenerated scene is visible.
			int last = activeProject.get().getOperations().size() - 1;
			if (last >= 0) {
				activeProject.get().moveToOpIndex(last);
			}

			result.put("success", true);
			result.put("sourceIndex", sourceIndex);
			result.put("sourceType", sourceOp.getType());
			result.put("message", "Regeneration completed");
		} catch (FailedToApplyOperation e) {
			result.put("success", false);
			result.put("error", e.getMessage());
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", e.getMessage());
		}
		return result;
	}

	/**
	 * Get the current state of the activeProject.get().
	 * @return Current state as JSON-serializable map.
	 */
	public Map<String, Object> getCurrentState() {
		Map<String, Object> state = new HashMap<>();
		try {
			if (!activeProject.get().isInitialized()) {
				state.put("initialized", false);
				return state;
			}

			List<CSG> currentState = activeProject.get().getCurrentState();
			state.put("initialized", true);
			state.put("operationCount", activeProject.get().getOperations().size());
			state.put("currentOperationIndex", activeProject.get().getCurrentIndex());
			state.put("projectName", activeProject.get().getMyProjectName());

			List<Map<String, Object>> operations = new ArrayList<>();
			List<CaDoodleOperation> ops = activeProject.get().getOperations();
			for (int i = 0; i < ops.size(); i++) {
				operations.add(describeOperation(i, ops.get(i)));
			}
			state.put("operations", operations);

			// Add CSG list
			List<Map<String, Object>> csgList = new ArrayList<>();
			for (CSG csg : currentState) {
				Map<String, Object> csgInfo = new HashMap<>();
				csgInfo.put("name", csg.getName());
				csgInfo.put("isGroup", csg.isGroupResult());
				csgInfo.put("isInGroup", csg.isInGroup());
				csgInfo.put("isHidden", csg.isHide());
				csgInfo.put("isLocked", csg.isLock());
				csgList.add(csgInfo);
			}
			state.put("csgs", csgList);

		} catch (Exception e) {
			state.put("error", e.getMessage());
		}
		return state;
	}

	/**
	 * Get detailed information about all CSGs.
	 * @return List of CSG information as JSON-serializable map.
	 */
	public Map<String, Object> getCSGs() {
		Map<String, Object> result = new HashMap<>();
		try {
			if (activeProject.get() == null || !activeProject.get().isInitialized()) {
				result.put("error", "activeProject.get() not initialized");
				return result;
			}

			List<CSG> currentState = activeProject.get().getCurrentState();
			List<Map<String, Object>> csgDetails = new ArrayList<>();

			for (CSG csg : currentState) {
				Map<String, Object> csgDetail = new HashMap<>();
				csgDetail.put("name", csg.getName());
				csgDetail.put("isGroup", csg.isGroupResult());

				// Check group membership - for group result, the name is the group ID
				if (csg.isGroupResult()) {
					csgDetail.put("groupName", csg.getName());
				} else if (csg.isInGroup()) {
					// CSG is part of a group but not a group result itself
					// In CaDoodle, individual CSGs in a group don't have a direct parent reference
					// We can only indicate they are in a group
					csgDetail.put("inGroup", true);
				}

				// Get bounds
				Bounds bounds = getCSGBounds(csg);
				if (bounds != null) {
					Map<String, Double> boundsMap = new HashMap<>();
					boundsMap.put("minX", bounds.getMin().x);
					boundsMap.put("minY", bounds.getMin().y);
					boundsMap.put("minZ", bounds.getMin().z);
					boundsMap.put("maxX", bounds.getMax().x);
					boundsMap.put("maxY", bounds.getMax().y);
					boundsMap.put("maxZ", bounds.getMax().z);
					csgDetail.put("bounds", boundsMap);
				}

				csgDetail.put("vertexCount", csg.getVertCount());
				csgDetail.put("isHidden", csg.isHide());
				csgDetail.put("isLocked", csg.isLock());
				csgDetail.put("isHole", csg.isHole());

				csgDetails.add(csgDetail);
			}

			result.put("success", true);
			result.put("csgs", csgDetails);

		} catch (Exception e) {
			result.put("error", e.getMessage());
		}
		return result;
	}

	/**
	 * Get parameters for a specific CSG.
	 * @param csgName The name of the CSG.
	 * @return Parameters as JSON-serializable map.
	 */
	public Map<String, Object> getCSGParameters(String csgName) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (activeProject.get() == null) {
				result.put("error", "activeProject.get() not initialized");
				return result;
			}

			List<CSG> currentState = activeProject.get().getCurrentState();
			CSG target = null;
			for (CSG csg : currentState) {
				if (csg.getName().equals(csgName)) {
					target = csg;
					break;
				}
			}

			if (target == null) {
				result.put("error", "CSG not found: " + csgName);
				return result;
			}

			// Get parameters from CSG database
			Map<String, Object> parameters = new HashMap<>();
			Set<String> paramNames = target.getParameters(activeProject.get().getCsgDBinstance());
			if (paramNames != null) {
				CSGDatabaseInstance dbInstance = activeProject.get().getCsgDBinstance();
				for (String paramName : paramNames) {
					Parameter param = dbInstance.get(paramName);
					if (param != null) {
						// Try to get value - either numeric or string
						Long value = param.getValue();
						if (value != null) {
							parameters.put(paramName, value);
						} else {
							String strValue = param.getStrValue();
							if (strValue != null) {
								parameters.put(paramName, strValue);
							}
						}
					}
				}
			}

			result.put("success", true);
			result.put("parameters", parameters);

		} catch (Exception e) {
			result.put("error", e.getMessage());
		}
		return result;
	}

	/**
	 * Set a CSG parameter value (string or numeric mm). Triggers regeneration.
	 * @param csgName CSG name
	 * @param parameterName Full parameter key or a unique suffix (e.g. Value)
	 * @param value String or number (mm for length parameters)
	 */
	public Map<String, Object> setCSGParameter(String csgName, String parameterName, Object value) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (activeProject.get() == null || !activeProject.get().isInitialized()) {
				result.put("success", false);
				result.put("error", "activeProject.get() not initialized");
				return result;
			}
			if (activeProject.get().isOperationRunning() || activeProject.get().isRegenerating()) {
				result.put("success", false);
				result.put("error", "Cannot set parameter while another operation is running");
				return result;
			}
			if (csgName == null || csgName.isEmpty() || parameterName == null || parameterName.isEmpty()) {
				result.put("success", false);
				result.put("error", "csgName and parameterName are required");
				return result;
			}
			if (value == null) {
				result.put("success", false);
				result.put("error", "value is required");
				return result;
			}

			List<CSG> currentState = activeProject.get().getCurrentState();
			CSG target = null;
			for (CSG csg : currentState) {
				if (csg.getName().equals(csgName)) {
					target = csg;
					break;
				}
			}
			if (target == null) {
				result.put("success", false);
				result.put("error", "CSG not found: " + csgName);
				return result;
			}

			CSGDatabaseInstance dbInstance = activeProject.get().getCsgDBinstance();
			Set<String> paramNames = target.getParameters(dbInstance);
			String resolved = null;
			if (paramNames != null) {
				if (paramNames.contains(parameterName)) {
					resolved = parameterName;
				} else {
					List<String> matches = new ArrayList<>();
					for (String name : paramNames) {
						if (name.endsWith(parameterName) || name.endsWith("_" + parameterName)
								|| name.toLowerCase().endsWith(parameterName.toLowerCase())) {
							matches.add(name);
						}
					}
					if (matches.size() == 1) {
						resolved = matches.get(0);
					} else if (matches.size() > 1) {
						result.put("success", false);
						result.put("error", "Ambiguous parameterName; matches: " + matches);
						return result;
					}
				}
			}
			if (resolved == null) {
				result.put("success", false);
				result.put("error", "Parameter not found on CSG: " + parameterName);
				result.put("available", paramNames);
				return result;
			}

			Parameter param = dbInstance.get(resolved);
			if (param == null) {
				result.put("success", false);
				result.put("error", "Parameter missing from database: " + resolved);
				return result;
			}

			Object applied;
			boolean isLength = param.getValue() != null;
			if (isLength) {
				double mm = value instanceof Number
						? ((Number) value).doubleValue()
						: Double.parseDouble(value.toString());
				param.setMM(mm);
				applied = mm;
			} else {
				String str = value.toString();
				param.setStrValue(str);
				applied = str;
			}

			waitForIdle(120000, result);
			if (result.containsKey("error")) {
				result.put("success", false);
				return result;
			}

			result.put("success", true);
			result.put("csgName", csgName);
			result.put("parameterName", resolved);
			result.put("value", applied);
			result.put("message", "Parameter set: " + resolved);
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", e.getMessage());
		}
		return result;
	}

	/**
	 * Set bounds for a specific CSG by applying a Resize operation.
	 * Bounds are interpreted in the current workplane frame.
	 * @param csgName The name of the CSG.
	 * @param boundsMap Map containing minX, minY, minZ, maxX, maxY, maxZ.
	 * @return Result as JSON-serializable map.
	 */
	public Map<String, Object> setCSGBounds(String csgName, Map<String, Double> boundsMap) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (activeProject.get() == null || !activeProject.get().isInitialized()) {
				result.put("success", false);
				result.put("error", "activeProject.get() not initialized");
				return result;
			}
			if (activeProject.get().isOperationRunning() || activeProject.get().isRegenerating()) {
				result.put("success", false);
				result.put("error", "Cannot set bounds while another operation is running");
				return result;
			}

			List<CSG> currentState = activeProject.get().getCurrentState();
			CSG target = null;
			for (CSG csg : currentState) {
				if (csg.getName().equals(csgName)) {
					target = csg;
					break;
				}
			}

			if (target == null) {
				result.put("success", false);
				result.put("error", "CSG not found: " + csgName);
				return result;
			}
			if (target.isLock() || target.isNoScale()) {
				result.put("success", false);
				result.put("error", "CSG cannot be resized: " + csgName);
				return result;
			}

			double minX = boundsMap.get("minX");
			double minY = boundsMap.get("minY");
			double minZ = boundsMap.get("minZ");
			double maxX = boundsMap.get("maxX");
			double maxY = boundsMap.get("maxY");
			double maxZ = boundsMap.get("maxZ");
			if (maxX < minX || maxY < minY || maxZ < minZ) {
				result.put("success", false);
				result.put("error", "Invalid bounds: max must be >= min on each axis");
				return result;
			}

			TransformNR workplane;
			try {
				workplane = selectionSession.getWorkplane().copy();
			} catch (Exception e) {
				workplane = new TransformNR();
			}

			TransformNR rightRear = new TransformNR(minX, minY, minZ);
			TransformNR leftFront = new TransformNR(maxX, maxY, minZ);
			TransformNR height = new TransformNR(0, 0, maxZ);

			Resize resize = new Resize().setNames(List.of(csgName)).setWorkplane(workplane).setResize(height, leftFront,
					rightRear);
			Thread thread = activeProject.addOp(resize);
			waitForThread(thread, 60000, result);
			if (result.containsKey("error")) {
				result.put("success", false);
				return result;
			}

			result.put("success", true);
			result.put("csgName", csgName);
			result.put("bounds", boundsMap);
			result.put("message", "Bounds set for: " + csgName);
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", e.getMessage());
		}
		return result;
	}

	/**
	 * Capture a screenshot of the live 3D scene view.
	 * @param width Target image width (defaults to engine width or 1280).
	 * @param height Target image height (defaults to engine height or 720).
	 * @param outputPath Optional absolute path for the PNG; temp file if null/empty.
	 * @return Result map with path, width, height, and success.
	 */
	public Map<String, Object> takeScreenshot(Integer width, Integer height, String outputPath) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (selectionSession == null || selectionSession.engine == null) {
				result.put("success", false);
				result.put("error", "3D engine is not available");
				return result;
			}

			int w = width != null && width > 0
					? width
					: Math.max(1, (int) Math.round(selectionSession.engine.getWidth()));
			int h = height != null && height > 0
					? height
					: Math.max(1, (int) Math.round(selectionSession.engine.getHeight()));
			if (w < 10) {
				w = 1280;
			}
			if (h < 10) {
				h = 720;
			}

			File out;
			if (outputPath != null && !outputPath.isEmpty()) {
				out = new File(outputPath);
				File parent = out.getParentFile();
				if (parent != null) {
					parent.mkdirs();
				}
			} else {
				out = File.createTempFile("cadoodle-scene-", ".png");
				out.deleteOnExit();
			}

			selectionSession.engine.saveViewToPng(out, w, h);
			if (!out.exists() || out.length() < 100) {
				result.put("success", false);
				result.put("error", "Screenshot failed or produced an empty image");
				result.put("path", out.getAbsolutePath());
				return result;
			}

			result.put("success", true);
			result.put("path", out.getAbsolutePath());
			result.put("width", w);
			result.put("height", h);
			result.put("bytes", out.length());
			result.put("mimeType", "image/png");
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", e.getMessage());
		}
		return result;
	}

	/**
	 * Move/orient the 3D camera and wait for the focus animation to finish.
	 * Supports named presets (home, front, left, back, right, top, bottom, fit)
	 * and/or absolute position, orientation, and zoom.
	 */
	public Map<String, Object> moveCamera(Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (selectionSession == null || selectionSession.engine == null) {
				result.put("success", false);
				result.put("error", "3D engine is not available");
				return result;
			}

			String preset = params.containsKey("preset") && params.get("preset") != null
					? params.get("preset").toString().trim().toLowerCase()
					: null;

			TransformNR orient = null;
			TransformNR position = null;
			Double zoom = null;

			if (preset != null && !preset.isEmpty()) {
				switch (preset) {
					case "home" :
					case "ortho" :
						orient = new TransformNR(0, 0, 0, new RotationNR(0, 15, -45));
						position = focusCenterPosition();
						zoom = -700.0;
						break;
					case "front" :
						orient = new TransformNR(0, 0, 0, new RotationNR(0, 0, 0));
						position = focusCenterPosition();
						break;
					case "left" :
						orient = new TransformNR(0, 0, 0, new RotationNR(0, 90, 0));
						position = focusCenterPosition();
						break;
					case "back" :
						orient = new TransformNR(0, 0, 0, new RotationNR(0, 180, 0));
						position = focusCenterPosition();
						break;
					case "right" :
						orient = new TransformNR(0, 0, 0, new RotationNR(0, -90, 0));
						position = focusCenterPosition();
						break;
					case "bottom" :
						orient = new TransformNR(0, 0, 0, new RotationNR(0, 0, 90));
						position = focusCenterPosition();
						break;
					case "top" :
						orient = new TransformNR(0, 0, 0, new RotationNR(0, 0, -90));
						position = focusCenterPosition();
						break;
					case "fit" :
						position = focusCenterPosition();
						zoom = selectionSession.engine.getFlyingCamera().getZoomDepth();
						break;
					default :
						result.put("success", false);
						result.put("error", "Unknown camera preset: " + preset
								+ " (home, front, left, back, right, top, bottom, fit)");
						return result;
				}
			}

			boolean hasPosition = params.containsKey("x") || params.containsKey("y") || params.containsKey("z")
					|| params.containsKey("position");
			if (hasPosition) {
				if (params.containsKey("position")) {
					position = toTransformNR(params.get("position"));
				} else {
					TransformNR current = position != null
							? position
							: new TransformNR(selectionSession.engine.getFlyingCamera().getGlobalX(),
									selectionSession.engine.getFlyingCamera().getGlobalY(),
									selectionSession.engine.getFlyingCamera().getGlobalZ());
					double x = params.containsKey("x") ? ((Number) params.get("x")).doubleValue() : current.getX();
					double y = params.containsKey("y") ? ((Number) params.get("y")).doubleValue() : current.getY();
					double z = params.containsKey("z") ? ((Number) params.get("z")).doubleValue() : current.getZ();
					position = new TransformNR(x, y, z);
				}
			}

			boolean hasOrient = params.containsKey("tilt") || params.containsKey("azimuth")
					|| params.containsKey("elevation") || params.containsKey("orientation");
			if (hasOrient) {
				if (params.containsKey("orientation")) {
					orient = toTransformNR(params.get("orientation"));
				} else {
					double tilt = params.containsKey("tilt") ? ((Number) params.get("tilt")).doubleValue() : 0.0;
					double azimuth = params.containsKey("azimuth")
							? ((Number) params.get("azimuth")).doubleValue()
							: 0.0;
					double elevation = params.containsKey("elevation")
							? ((Number) params.get("elevation")).doubleValue()
							: 0.0;
					orient = new TransformNR(0, 0, 0, new RotationNR(tilt, azimuth, elevation));
				}
			}

			if (params.containsKey("zoom")) {
				zoom = ((Number) params.get("zoom")).doubleValue();
			}

			if (orient == null && position == null && zoom == null) {
				result.put("success", false);
				result.put("error", "Provide a preset and/or position/orientation/zoom");
				return result;
			}

			double zoomValue = zoom != null ? zoom : selectionSession.engine.getFlyingCamera().getZoomDepth();

			// fit preset intentionally passes null orientation so only position/zoom animate
			if ("fit".equals(preset) && !hasOrient) {
				orient = null;
			}

			long timeoutMs = params.containsKey("timeoutMs") ? ((Number) params.get("timeoutMs")).longValue() : 30000L;

			selectionSession.engine.focusOrientationAndWait(orient, position, zoomValue, timeoutMs);

			result.put("success", true);
			if (preset != null) {
				result.put("preset", preset);
			}
			result.put("camera", getCameraState());
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", e.getMessage());
		}
		return result;
	}

	/**
	 * Read the current camera pose/zoom.
	 */
	public Map<String, Object> getCameraState() {
		Map<String, Object> cam = new HashMap<>();
		try {
			com.neuronrobotics.bowlerstudio.threed.VirtualCameraMobileBase flying = selectionSession.engine
					.getFlyingCamera();
			cam.put("x", flying.getGlobalX());
			cam.put("y", flying.getGlobalY());
			cam.put("z", flying.getGlobalZ());
			cam.put("panAngle", flying.getPanAngle());
			cam.put("tiltAngle", flying.getTiltAngle());
			cam.put("zoom", flying.getZoomDepth());
		} catch (Exception e) {
			cam.put("error", e.getMessage());
		}
		return cam;
	}

	private TransformNR focusCenterPosition() {
		try {
			TransformNR scale = selectionSession.getFocusCenter();
			return new TransformNR(scale.getX(), -scale.getY(), -scale.getZ());
		} catch (Exception e) {
			return new TransformNR(0, 0, 0);
		}
	}

	/**
	 * Get the shapes palette.
	 * @return Shapes palette JSON content.
	 */
	public Map<String, Object> getShapesPalette() {
		Map<String, Object> result = new HashMap<>();
		try {
			ShapesPalette shapesPalette = new ShapesPalette();
			List<Map<String, Object>> categories = shapesPalette.getShapes();
			result.put("success", true);
			result.put("categories", categories);
		} catch (Exception e) {
			result.put("error", e.getMessage());
		}
		return result;
	}

	/**
	 * Get currently selected CSG names.
	 * @return List of selected CSG names.
	 */
	public List<String> getSelectedNames() {
		return selectionSession.selectedSnapshot();
	}

	/**
	 * Select CSGs by their names.
	 * @param names List of CSG names to select.
	 * @return Result as JSON-serializable map.
	 */
	public Map<String, Object> selectCSGs(List<String> names) {
		Map<String, Object> result = new HashMap<>();
		try {
			selectionSession.selectAll(names);
			result.put("success", true);
			result.put("selected", names);
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", e.getMessage());
		}
		return result;
	}

	/**
	 * Add a shape from the palette by name or description.
	 * @param nameOrDescription Shape name (e.g. "cube") or description to fuzzy-match.
	 * @return Result as JSON-serializable map.
	 */
	public Map<String, Object> addShapeByName(String nameOrDescription) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (nameOrDescription == null || nameOrDescription.isEmpty()) {
				result.put("success", false);
				result.put("error", "name parameter is required");
				return result;
			}

			ShapesPalette shapesPalette = new ShapesPalette();
			List<Map<String, Object>> categories = shapesPalette.getShapes();

			Map<String, Object> matchingShape = null;
			String bestMatch = "";
			double bestScore = 0.0;

			for (Map<String, Object> category : categories) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> items = (List<Map<String, Object>>) category.get("items");
				if (items == null) {
					continue;
				}
				for (Map<String, Object> shape : items) {
					String shapeName = "";
					if (shape.containsKey("file")) {
						shapeName = shape.get("file").toString();
						int slash = Math.max(shapeName.lastIndexOf('/'), shapeName.lastIndexOf('\\'));
						if (slash >= 0) {
							shapeName = shapeName.substring(slash + 1);
						}
						int dotIndex = shapeName.lastIndexOf('.');
						if (dotIndex > 0) {
							shapeName = shapeName.substring(0, dotIndex);
						}
					}

					double score = fuzzyMatch(nameOrDescription, shapeName);
					if (score > bestScore) {
						bestScore = score;
						bestMatch = shapeName;
						matchingShape = shape;
					}

					if (shape.containsKey("description")) {
						score = fuzzyMatch(nameOrDescription, shape.get("description").toString());
						if (score > bestScore) {
							bestScore = score;
							bestMatch = shapeName;
							matchingShape = shape;
						}
					}
				}
			}

			if (matchingShape == null || bestScore < 0.3) {
				result.put("success", false);
				result.put("error", "No matching shape found for: " + nameOrDescription);
				return result;
			}

			String git = matchingShape.containsKey("git") ? matchingShape.get("git").toString() : "";
			String file = matchingShape.containsKey("file") ? matchingShape.get("file").toString() : "";
			if (git.isEmpty() || file.isEmpty()) {
				result.put("success", false);
				result.put("error", "Matched shape is missing git/file metadata: " + bestMatch);
				return result;
			}

			TransformNR location = new TransformNR();
			try {
				TransformNR wp = selectionSession.getWorkplane();
				if (wp != null) {
					location = wp;
				}
			} catch (Exception e) {
				Log.debug("Using origin location for shape add: " + e.getMessage());
			}

			AbstractAddFrom op = shapesPalette.createAddOperation(matchingShape, activeProject.get(), location);

			Thread thread = activeProject.addOp(op);
			waitForThread(thread, 120000, result);
			if (result.containsKey("error")) {
				result.put("success", false);
				return result;
			}

			List<String> addedNames = op.getNamesAddedInThisOperation();
			try {
				selectionSession.selectAllFromCurrentState(addedNames);
			} catch (Exception e) {
				Log.debug("Could not select added shape: " + e.getMessage());
			}

			result.put("success", true);
			result.put("shapeName", bestMatch);
			result.put("git", git);
			result.put("file", file);
			result.put("addedNames", addedNames);
			return result;
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", e.getMessage());
			return result;
		}
	}

	private double fuzzyMatch(String query, String target) {
		if (query == null || target == null)
			return 0.0;
		String q = query.toLowerCase();
		String t = target.toLowerCase();

		if (q.equals(t))
			return 1.0;
		if (t.contains(q))
			return 0.9;
		if (q.length() >= 3 && t.contains(q.substring(0, Math.min(3, q.length()))))
			return 0.7;

		int matches = 0;
		for (char c : q.toCharArray()) {
			if (t.indexOf(c) >= 0)
				matches++;
		}
		return (double) matches / q.length();
	}

	// Helper methods
	private static class ResolvedOperation {
		final int index;
		final CaDoodleOperation operation;

		ResolvedOperation(int index, CaDoodleOperation operation) {
			this.index = index;
			this.operation = operation;
		}
	}

	private ResolvedOperation resolveOperation(String operationId) {
		if (operationId == null || operationId.isEmpty()) {
			return null;
		}
		List<CaDoodleOperation> ops = activeProject.get().getOperations();
		try {
			int index = Integer.parseInt(operationId.trim());
			if (index >= 0 && index < ops.size()) {
				return new ResolvedOperation(index, ops.get(index));
			}
		} catch (NumberFormatException ignored) {
			// Fall through to name lookup
		}

		for (int i = 0; i < ops.size(); i++) {
			CaDoodleOperation op = ops.get(i);
			String name = operationName(op);
			if (operationId.equals(name)) {
				return new ResolvedOperation(i, op);
			}
		}
		return null;
	}

	private String operationName(CaDoodleOperation op) {
		if (op instanceof INamedOperation) {
			return ((INamedOperation) op).getName();
		}
		try {
			return (String) op.getClass().getMethod("getName").invoke(op);
		} catch (Exception e) {
			return null;
		}
	}

	private Map<String, Object> describeOperation(int index, CaDoodleOperation op) {
		Map<String, Object> info = new HashMap<>();
		info.put("index", index);
		info.put("type", op.getType());
		String name = operationName(op);
		if (name != null) {
			info.put("name", name);
		}
		List<String> added = op.getNamesAddedInThisOperation();
		if (added != null) {
			info.put("addedNames", added);
		}
		return info;
	}

	private void waitForThread(Thread thread, long timeoutMs, Map<String, Object> result) {
		if (thread == null) {
			return;
		}
		try {
			thread.join(timeoutMs);
			if (thread.isAlive()) {
				result.put("error", "Operation timed out");
				thread.interrupt();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			result.put("error", "Operation interrupted");
		}
	}

	private void waitForIdle(long timeoutMs, Map<String, Object> result) {
		long deadline = System.currentTimeMillis() + timeoutMs;
		boolean sawBusy = false;
		try {
			while (System.currentTimeMillis() < deadline) {
				boolean busy = activeProject.get() != null
						&& (activeProject.get().isOperationRunning() || activeProject.get().isRegenerating());
				if (busy) {
					sawBusy = true;
				} else if (sawBusy) {
					return;
				} else {
					// Parameter listeners submit regen asynchronously; give them a moment to start.
					Thread.sleep(100);
					busy = activeProject.get() != null
							&& (activeProject.get().isOperationRunning() || activeProject.get().isRegenerating());
					if (!busy) {
						return;
					}
					sawBusy = true;
				}
				Thread.sleep(50);
			}
			result.put("error", "Timed out waiting for regeneration");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			result.put("error", "Interrupted while waiting for regeneration");
		}
	}

	@SuppressWarnings("unchecked")
	private TransformNR toTransformNR(Object value) {
		TransformNR tf = new TransformNR();
		if (value == null) {
			return tf;
		}
		if (value instanceof TransformNR) {
			return ((TransformNR) value).copy();
		}
		if (!(value instanceof Map)) {
			return tf;
		}
		Map<String, Object> map = (Map<String, Object>) value;
		double x = numberOr(map, "translateX", numberOr(map, "x", 0.0));
		double y = numberOr(map, "translateY", numberOr(map, "y", 0.0));
		double z = numberOr(map, "translateZ", numberOr(map, "z", 0.0));
		tf.setX(x);
		tf.setY(y);
		tf.setZ(z);

		if (map.containsKey("rotateAzimuth") || map.containsKey("rotateElevation") || map.containsKey("rotateTilt")
				|| map.containsKey("rotateX") || map.containsKey("rotateY") || map.containsKey("rotateZ")) {
			double az = numberOr(map, "rotateAzimuth", numberOr(map, "rotateX", 0.0));
			double el = numberOr(map, "rotateElevation", numberOr(map, "rotateY", 0.0));
			double tl = numberOr(map, "rotateTilt", numberOr(map, "rotateZ", 0.0));
			tf.setRotation(new RotationNR(az, el, tl));
		}
		return tf;
	}

	private double numberOr(Map<String, Object> map, String key, double fallback) {
		Object value = map.get(key);
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		if (value != null) {
			try {
				return Double.parseDouble(value.toString());
			} catch (NumberFormatException ignored) {
			}
		}
		return fallback;
	}

	private boolean paramsContainsBounds(Map<String, Object> params) {
		return params.containsKey("minX") && params.containsKey("minY") && params.containsKey("minZ")
				&& params.containsKey("maxX") && params.containsKey("maxY") && params.containsKey("maxZ");
	}

	private void applyBoundsToResize(Resize resize, Map<String, Object> params) {
		double minX = ((Number) params.get("minX")).doubleValue();
		double minY = ((Number) params.get("minY")).doubleValue();
		double minZ = ((Number) params.get("minZ")).doubleValue();
		double maxX = ((Number) params.get("maxX")).doubleValue();
		double maxY = ((Number) params.get("maxY")).doubleValue();
		double maxZ = ((Number) params.get("maxZ")).doubleValue();
		TransformNR rightRear = new TransformNR(minX, minY, minZ);
		TransformNR leftFront = new TransformNR(maxX, maxY, minZ);
		TransformNR height = new TransformNR(0, 0, maxZ);
		resize.setResize(height, leftFront, rightRear);
	}

	@SuppressWarnings("unchecked")
	private List<String> toStringList(Object value) {
		List<String> out = new ArrayList<>();
		if (value == null) {
			return out;
		}
		if (value instanceof List) {
			for (Object item : (List<?>) value) {
				if (item != null) {
					out.add(item.toString());
				}
			}
			return out;
		}
		if (value instanceof Set) {
			for (Object item : (Set<?>) value) {
				if (item != null) {
					out.add(item.toString());
				}
			}
			return out;
		}
		out.add(value.toString());
		return out;
	}

	private Set<String> toStringSet(Object value) {
		return new LinkedHashSet<>(toStringList(value));
	}

	private Bounds getCSGBounds(CSG csg) {
		try {
			return csg.getBounds();
		} catch (Exception e) {
			return null;
		}
	}
}
