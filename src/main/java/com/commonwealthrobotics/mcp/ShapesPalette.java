package com.commonwealthrobotics.mcp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.neuronrobotics.sdk.common.Log;

/**
 * Loads and provides access to the CaDoodle shapes palette JSON.
 */
public class ShapesPalette {
    private static String paletteJSON = null;
    
    static {
        loadPalette();
    }
    
    private static void loadPalette() {
        try {
            // Load from the resource path
            String path = "/doodle/CSGdatabase.json";
            // Try multiple paths
            String[] possiblePaths = {
                "/doodle/CSGdatabase.json",
                "/com/neuronrobotics/bowlerstudio/scripting/cadoodle/doodle/CSGdatabase.json",
                "/doodle/csgDatabase.json"
            };
            
            for (String pathToTry : possiblePaths) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                ShapesPalette.class.getResourceAsStream(pathToTry),
                                StandardCharsets.UTF_8))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line);
                    }
                    paletteJSON = content.toString();
                    if (paletteJSON != null && !paletteJSON.isEmpty()) {
                        Log.info("Loaded shapes palette from " + pathToTry);
                        return;
                    }
                } catch (IOException e) {
                    // Try next path
                }
            }
            
            // If we can't load from resources, try to load from file system
            File cadoodleDir = new File(System.getProperty("user.home"), "bin/CaDoodle-ApplicationInstall/doodle");
            if (cadoodleDir.exists()) {
                File dbFile = new File(cadoodleDir, "CSGdatabase.json");
                if (dbFile.exists()) {
                    paletteJSON = new String(java.nio.file.Files.readAllBytes(dbFile.toPath()), StandardCharsets.UTF_8);
                    Log.info("Loaded shapes palette from " + dbFile.getAbsolutePath());
                    return;
                }
            }
            
            // Fallback to empty JSON
            paletteJSON = "{}";
            Log.warning("Could not load shapes palette, using empty JSON");
            
        } catch (Exception e) {
            Log.error("Error loading shapes palette");
            paletteJSON = "{}";
        }
    }
    
    /**
     * Get the shapes palette JSON as a string.
     * @return The palette JSON.
     */
    public static String getPaletteJSON() {
        return paletteJSON;
    }
}
