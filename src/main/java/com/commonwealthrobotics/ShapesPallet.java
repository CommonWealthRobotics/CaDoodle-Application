package com.commonwealthrobotics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;

import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;

public class ShapesPallet {

	private ComboBox<String> shapeCatagory;
	private GridPane objectPallet;
	private String gitULR= "https://github.com/madhephaestus/CaDoodle-Example-Objects.git";
	private HashMap<String,HashMap<String, HashMap<String, String>>> nameToFile = new HashMap<>();
	private Type TT = new TypeToken<HashMap<String, HashMap<String, String>>>() {}.getType();
	private Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	private HashMap<String, HashMap<String, String>> active=null;
	public ShapesPallet(ComboBox<String> shapeCatagory, GridPane objectPallet) {
		this.shapeCatagory = shapeCatagory;
		this.objectPallet = objectPallet;
		try {
			ArrayList<String> files = ScriptingEngine.filesInGit(gitULR);
			for(String f:files) {
				if(f.toLowerCase().endsWith(".json")) {
					String contents = ScriptingEngine.codeFromGit(gitULR, f)[0];
					File fileFromGit = ScriptingEngine.fileFromGit(gitULR, f);
					String name = fileFromGit.getName();
					String[] split = name.split(".json");
					String filename = split[0];
					HashMap<String, HashMap<String, String>> tmp = gson.fromJson(contents, TT);
					nameToFile.put(filename, tmp);
					shapeCatagory.getItems().add(filename);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String starting = ConfigurationDatabase.get("ShapesPallet", "selected","BasicShapes").toString();
		shapeCatagory.getSelectionModel().select(starting);
		onSetCatagory();
	}
	public void onSetCatagory() {
		String current=shapeCatagory.getSelectionModel().getSelectedItem();
		System.out.println("Selecting shapes from "+current);
		ConfigurationDatabase.put("ShapesPallet", "selected",current).toString();
		active = nameToFile.get(current);
		if(active ==null)
			return;
		ArrayList<HashMap<String,String>> orderedList = new ArrayList<HashMap<String,String>>();
		// store the name os the keys for labeling the hoverover later
		HashMap<Map,String> names = new HashMap<>();
		for(String key:active.keySet()) {
			HashMap<String, String> hashMap = active.get(key);
			int index = Integer.parseInt(hashMap.get("order"));
			System.out.println("Adding "+key+" at "+index);
			while(orderedList.size()<=index)
				orderedList.add(null);
			orderedList.set(index, hashMap);
			names.put(hashMap, key);
		}
		
		
	}

}
