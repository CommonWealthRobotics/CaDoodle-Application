package com.commonwealthrobotics.mcp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.sdk.common.Log;

/**
 * Loads and provides access to the CaDoodle shapes palette JSON.
 * Loads from the local git cache.
 */
public class ShapesPalette {
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    
    // Cache of shapes: category -> List of shape items
    private static List<Map<String, Object>> shapesCache = null;
    
    static {
        loadShapes();
    }
    
    private static void loadShapes() {
        shapesCache = new ArrayList<>();
        String gitULR = "https://github.com/CommonWealthRobotics/CaDoodle-ShapesPalet-Content.git";
        File shapesDir = ScriptingEngine.getRepositoryCloneDirectory(gitULR);
        if (!shapesDir.exists() || !shapesDir.isDirectory()) {
            Log.error("Shapes palette directory not found: " + gitULR);
            return;
        }
        
        File[] jsonFiles = shapesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            Log.error("No JSON files found in shapes palette directory");
            return;
        }
        
        // Sort files alphabetically
        List<File> sortedFiles = new ArrayList<>();
        for (File f : jsonFiles) {
            sortedFiles.add(f);
        }
        sortedFiles.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        
        for (File jsonFile : sortedFiles) {
            try {
                // Parse JSON file
                String content = new String(Files.readAllBytes(jsonFile.toPath()), StandardCharsets.UTF_8);
                @SuppressWarnings("unchecked")
                Map<String, Object> shapeMap = gson.fromJson(content, Map.class);
                
                if (shapeMap != null) {
                    String category = jsonFile.getName().replace(".json", "");
                    
                    // Convert shapeMap to a list of items with order
                    List<Map<String, Object>> items = new ArrayList<>();
                    
                    for (Map.Entry<String, Object> entry : shapeMap.entrySet()) {
                        String shapeName = entry.getKey();
                        Map<String, Object> shapeInfo = new TreeMap<>();
                        
                        Object value = entry.getValue();
                        if (value instanceof Map) {
                            // Full metadata: git, file, order, etc.
                            shapeInfo.putAll((Map<String, Object>) value);
                        } else {
                            // Simple name reference
                            shapeInfo.put("name", value);
                            shapeInfo.put("order", 0);
                        }
                        
                        // Ensure order is set (default to 0 if not present)
                        if (!shapeInfo.containsKey("order")) {
                            shapeInfo.put("order", 0);
                        }
                        
                        items.add(shapeInfo);
                    }
                    
                    Map<String, Object> categoryEntry = new TreeMap<>();
                    categoryEntry.put("category", category);
                    categoryEntry.put("items", items);
                    shapesCache.add(categoryEntry);
                    
                    Log.info("Loaded shapes category: " + category + " with " + items.size() + " items");
                }
                
            } catch (IOException e) {
                Log.error("Failed to load shapes file: " + jsonFile.getName());
                Log.error(e);
            }
        }
        
        Log.info("Loaded " + shapesCache.size() + " shapes categories");
    }
    
    /**
     * Get the shapes palette as a list of categories with items.
     * @return List of categories, each containing "category" and "items".
     */
    public List<Map<String, Object>> getShapes() {
        return shapesCache;
    }
    
    /**
     * Get a specific category's shapes.
     * @param categoryName The category name.
     * @return List of shape items in the category, or empty list if not found.
     */
    public List<Map<String, Object>> getCategoryShapes(String categoryName) {
        for (Map<String, Object> category : shapesCache) {
            if (category.get("category").equals(categoryName)) {
                return (List<Map<String, Object>>) category.get("items");
            }
        }
        return new ArrayList<>();
    }
}
