package com.commonwealthrobotics.mcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Align;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Delete;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ExtrudeSurface;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.FilletChamfer;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Group;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Hide;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.LinearDistribution;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Lock;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Mirror;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.MirrorOrientation;
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
			@SuppressWarnings("unchecked")
			List<String> names = (List<String>) params.get("names");
			// Use reflection to call setNames if available
			try {
				op.getClass().getMethod("setNames", List.class).invoke(op, names);
			} catch (Exception e) {
				Log.error("Failed to set names on operation: " + type);
			}
		}

		// Common: set workplane if present
		if (params.containsKey("workplane")) {
			@SuppressWarnings("unchecked")
			Map<String, Double> wpMap = (Map<String, Double>) params.get("workplane");
			// Try to set workplane via reflection if the method exists
			try {
				com.neuronrobotics.sdk.addons.kinematics.math.TransformNR wp = new com.neuronrobotics.sdk.addons.kinematics.math.TransformNR();
				if (wpMap.containsKey("translateX"))
					wp.setX(wpMap.get("translateX"));
				if (wpMap.containsKey("translateY"))
					wp.setY(wpMap.get("translateY"));
				if (wpMap.containsKey("translateZ"))
					wp.setZ(wpMap.get("translateZ"));

				// Handle rotation - TransformNR uses RotationNR
				if (wpMap.containsKey("rotateAzimuth") || wpMap.containsKey("rotateX")) {
					double az = wpMap.getOrDefault("rotateAzimuth", wpMap.getOrDefault("rotateX", 0.0));
					double el = wpMap.getOrDefault("rotateY", 0.0);
					double tl = wpMap.getOrDefault("rotateZ", 0.0);
					wp.setRotation(new com.neuronrobotics.sdk.addons.kinematics.math.RotationNR(az, el, tl));
				}

				// Check if op has setWorkplane method
				try {
					java.lang.reflect.Method setWorkplane = op.getClass().getMethod("setWorkplane",
							com.neuronrobotics.sdk.addons.kinematics.math.TransformNR.class);
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
						@SuppressWarnings("unchecked")
						List<String> boundNames = (List<String>) params.get("bounds");
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
				if (op instanceof Delete) {
					// Delete already configured via setNames
				}
				break;
			case "ExtrudeSurface" :
				if (op instanceof ExtrudeSurface) {
					ExtrudeSurface ext = (ExtrudeSurface) op;
					if (params.containsKey("toFillet")) {
						@SuppressWarnings("unchecked")
						Set<String> toFillet = (Set<String>) params.get("toFillet");
						ext.setToExtrude(toFillet); // Use public setter
					}
				}
				break;
			case "Fillet" :
				if (op instanceof FilletChamfer) {
					FilletChamfer fillet = (FilletChamfer) op;
					if (params.containsKey("toFillet")) {
						@SuppressWarnings("unchecked")
						Set<String> toFillet = (Set<String>) params.get("toFillet");
						fillet.setToFillet(toFillet); // Use public setter
					}
				}
				break;
			case "Group" :
				if (op instanceof Group) {
					// Group already configured via setNames
				}
				break;
			case "Hide" :
			case "Show" :
				if (op instanceof Hide || op instanceof Show) {
					// Already configured via setNames
				}
				break;
			case "LinearDistribution" :
				if (op instanceof LinearDistribution) {
					// LinearDistribution doesn't have setName() - it uses getName() from INamedOperation
					// Configuration is done via parameters in the process method
				}
				break;
			case "Lock" :
			case "UnLock" :
				if (op instanceof Lock || op instanceof UnLock) {
					// Already configured via setNames
				}
				break;
			case "Mirror" :
				if (op instanceof Mirror) {
					Mirror mirror = (Mirror) op;
					if (params.containsKey("mirrorPlane")) {
						try {
							MirrorOrientation plane = MirrorOrientation.valueOf(params.get("mirrorPlane").toString());
							mirror.setLocation(plane);
						} catch (Exception e) {
							Log.error("Invalid mirror plane: " + params.get("mirrorPlane"));
						}
					}
				}
				break;
			case "Paste" :
				if (op instanceof Paste) {
					// Paste already configured via setNames
				}
				break;
			case "RadialDistribution" :
				if (op instanceof RadialDistribution) {
					// RadialDistribution doesn't have setName() - it uses getName() from INamedOperation
				}
				break;
			case "Resize" :
				if (op instanceof Resize) {
					// Resize parameters would need to be configured based on the specific resize type
				}
				break;
			case "SetMaterial" :
				if (op instanceof SetMaterial) {
					SetMaterial setMat = (SetMaterial) op;
					if (params.containsKey("materialType")) {
						setMat.setMaterialType(params.get("materialType").toString());
					}
				}
				break;
			case "ToHole" :
			case "ToSolid" :
				if (op instanceof ToHole || op instanceof ToSolid) {
					// Already configured via setNames
				}
				break;
			case "UnGroup" :
				if (op instanceof UnGroup) {
					// UnGroup already configured via setNames
				}
				break;
			case "WireMeshView" :
				if (op instanceof WireMeshView) {
					// WireMeshView already configured via setNames
				}
				break;
		}
	}

	/**
	 * Remove an operation from the activeProject.get().
	 * @param operationId The ID of the operation to remove.
	 * @return Result as JSON-serializable map.
	 */
	public Map<String, Object> removeOperation(String operationId) {
		Map<String, Object> result = new HashMap<>();
		try {
			// Implementation would call activeProject.get().deleteOperation()
			// This is complex because operations are stored in a list and deletion requires
			// regenerating from a previous operation. For now, return error.
			result.put("success", false);
			result.put("error", "Operation removal not yet implemented");
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", e.getMessage());
		}
		return result;
	}

	/**
	 * Regenerate operations from a source.
	 * @param sourceOperationId The source operation ID.
	 * @return Result as JSON-serializable map.
	 */
	public Map<String, Object> regenerate(String sourceOperationId) {
		Map<String, Object> result = new HashMap<>();
		try {
			// Find the operation by ID (simplified)
			List<com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation> operations = activeProject.get()
					.getOperations();
			com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation sourceOp = null;
			for (com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation op : operations) {
				// CaDoodleOperation doesn't have getName(), but AbstractAddFrom does
				// Use reflection to call getName() if available
				try {
					String name = (String) op.getClass().getMethod("getName").invoke(op);
					if (name != null && name.equals(sourceOperationId)) {
						sourceOp = op;
						break;
					}
				} catch (Exception e) {
					// Operation doesn't have getName()
				}
			}

			if (sourceOp == null) {
				result.put("success", false);
				result.put("error", "Source operation not found: " + sourceOperationId);
				return result;
			}

			Thread thread = activeProject.regenerateFrom(sourceOp);
			if (thread != null) {
				try {
					thread.join(30000);
					if (thread.isAlive()) {
						result.put("error", "Regeneration timed out");
						thread.interrupt();
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					result.put("error", "Regeneration interrupted");
				}
			}

			result.put("success", true);
			result.put("message", "Regeneration completed");
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
	 * Set bounds for a specific CSG.
	 * @param csgName The name of the CSG.
	 * @param boundsMap Map containing minX, minY, minZ, maxX, maxY, maxZ.
	 * @return Result as JSON-serializable map.
	 */
	public Map<String, Object> setCSGBounds(String csgName, Map<String, Double> boundsMap) {
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
				result.put("success", false);
				result.put("error", "CSG not found: " + csgName);
				return result;
			}

			// Bounds setting is not directly supported in CaDoodle
			// This would require complex CSG transformation
			result.put("success", false);
			result.put("error", "Setting bounds not yet implemented");

		} catch (Exception e) {
			result.put("success", false);
			result.put("error", e.getMessage());
		}
		return result;
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

	// Helper methods
	private Bounds getCSGBounds(CSG csg) {
		try {
			return csg.getBounds();
		} catch (Exception e) {
			return null;
		}
	}
}
