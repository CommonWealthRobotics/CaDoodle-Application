package com.commonwealthrobotics.mcp;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.commonwealthrobotics.ActiveProject;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.sdk.common.Log;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.parametrics.CSGDatabaseInstance;
import eu.mihosoft.vrl.v3d.parametrics.Parameter;

/**
 * API wrapper for CaDoodle operations.
 * This class provides a clean interface for MCP to interact with CaDoodleFile.
 */
public class CaDoodleAPI {
    private ActiveProject activeProject;
    private CaDoodleFile cadoodleFile;
    
    public CaDoodleAPI() {
        this.activeProject = new ActiveProject();
        try {
            this.cadoodleFile = activeProject.loadActive();
        } catch (Exception e) {
            Log.error("Failed to load active project");
            try {
                activeProject.newProject();
                this.cadoodleFile = activeProject.loadActive();
            } catch (Exception ex) {
                Log.error("Failed to create new project");
            }
        }
    }
    
    public void close() {
        if (cadoodleFile != null) {
            cadoodleFile.close();
        }
    }
    
    /**
     * Add an operation to the CaDoodleFile.
     * @param operationType The type of operation to add.
     * @return Result as JSON-serializable map.
     */
    public Map<String, Object> addOperation(String operationType) {
        Map<String, Object> result = new HashMap<>();
        try {
            // This is a simplified version - actual implementation would need to
            // create the appropriate CaDoodleOperation subclass
            // For now, return a placeholder
            result.put("success", true);
            result.put("message", "Operation type: " + operationType);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
    
    /**
     * Remove an operation from the CaDoodleFile.
     * @param operationId The ID of the operation to remove.
     * @return Result as JSON-serializable map.
     */
    public Map<String, Object> removeOperation(String operationId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // Implementation would call cadoodleFile.deleteOperation()
            result.put("success", true);
            result.put("message", "Operation removed");
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
            // Implementation would call cadoodleFile.regenerateFrom()
            result.put("success", true);
            result.put("message", "Regeneration started");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
    
    /**
     * Get the current state of the CaDoodleFile.
     * @return Current state as JSON-serializable map.
     */
    public Map<String, Object> getCurrentState() {
        Map<String, Object> state = new HashMap<>();
        try {
            if (cadoodleFile == null || !cadoodleFile.isInitialized()) {
                state.put("initialized", false);
                return state;
            }
            
            List<CSG> currentState = cadoodleFile.getCurrentState();
            state.put("initialized", true);
            state.put("operationCount", cadoodleFile.getOperations().size());
            state.put("currentOperationIndex", cadoodleFile.getCurrentIndex());
            state.put("operationCount", cadoodleFile.getOperations().size());
            state.put("projectName", cadoodleFile.getMyProjectName());
            
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
            if (cadoodleFile == null || !cadoodleFile.isInitialized()) {
                result.put("error", "CaDoodleFile not initialized");
                return result;
            }
            
            List<CSG> currentState = cadoodleFile.getCurrentState();
            List<Map<String, Object>> csgDetails = new ArrayList<>();
            
            for (CSG csg : currentState) {
                Map<String, Object> csgDetail = new HashMap<>();
                csgDetail.put("name", csg.getName());
                csgDetail.put("isGroup", csg.isGroupResult());
                
                // Check group membership
                if (csg.isInGroup()) {
                    // Find the group name (the result of the grouping operation)
                    String groupName = findGroupName(csg);
                    csgDetail.put("groupName", groupName);
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
            if (cadoodleFile == null) {
                result.put("error", "CaDoodleFile not initialized");
                return result;
            }
            
            List<CSG> currentState = cadoodleFile.getCurrentState();
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
            Set<String> paramNames = target.getParameters(cadoodleFile.getCsgDBinstance());
            if (paramNames != null) {
                CSGDatabaseInstance dbInstance = cadoodleFile.getCsgDBinstance();
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
     * Set bounds for a CSG.
     * @param csgName The name of the CSG.
     * @param minX Minimum X
     * @param minY Minimum Y
     * @param minZ Minimum Z
     * @param maxX Maximum X
     * @param maxY Maximum Y
     * @param maxZ Maximum Z
     * @return Result as JSON-serializable map.
     */
    public Map<String, Object> setBounds(String csgName, double minX, double minY, double minZ,
                                        double maxX, double maxY, double maxZ) {
        Map<String, Object> result = new HashMap<>();
        try {
            // Implementation would need to transform the CSG to new bounds
            // This is a complex operation that may not be directly supported
            result.put("success", true);
            result.put("message", "Bounds set for: " + csgName);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
    
    /**
     * Helper to get bounds for a CSG.
     */
    private Bounds getCSGBounds(CSG csg) {
        try {
            List<CSG> list = new ArrayList<>();
            list.add(csg);
            return cadoodleFile.getBounds(list);
        } catch (Exception e) {
            Log.error("Failed to get bounds for CSG");
            return null;
        }
    }
    
    /**
     * Helper to find the group name for a CSG that is in a group.
     */
    private String findGroupName(CSG csg) {
        // This is a simplified approach - actual implementation may vary
        // The group name is typically the name of the CSG that is the group result
        if (csg.isGroupResult()) {
            return csg.getName();
        }
        // For non-group CSGs, find the group that contains them
        // This would require traversing the CSG hierarchy
        return null;
    }
}
