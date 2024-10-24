package com.commonwealthrobotics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.creature.ThumbnailImage;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AddFromScript;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CadoodleConcurrencyException;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.MoveCenter;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.parametrics.CSGDatabase;
import eu.mihosoft.vrl.v3d.parametrics.Parameter;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.transform.Affine;

import com.commonwealthrobotics.controls.SelectionSession;
import com.commonwealthrobotics.controls.SpriteDisplayMode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;

public class ShapesPallet {

	private ComboBox<String> shapeCatagory;
	private GridPane objectPallet;
	private String gitULR = "https://github.com/madhephaestus/CaDoodle-Example-Objects.git";
	private HashMap<String, HashMap<String, HashMap<String, String>>> nameToFile = new HashMap<>();
	private Type TT = new TypeToken<HashMap<String, HashMap<String, String>>>() {
	}.getType();
	private Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	private HashMap<String, HashMap<String, String>> active = null;
	private SelectionSession session;
	private WorkplaneManager workplane;
	private HashMap<Button,List<CSG>> referenceParts = new HashMap<>();
	private ActiveProject ap;

	public ShapesPallet(ComboBox<String> shapeCatagory, GridPane objectPallet, SelectionSession session) {
		this.shapeCatagory = shapeCatagory;
		this.objectPallet = objectPallet;
		this.session = session;
		try {
			ScriptingEngine.cloneRepo(gitULR, null);
			ScriptingEngine.pull(gitULR);
			ArrayList<String> files = ScriptingEngine.filesInGit(gitULR);
			for (String f : files) {
				if (f.toLowerCase().endsWith(".json")) {
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
		String starting = ConfigurationDatabase.get("ShapesPallet", "selected", "BasicShapes").toString();
		shapeCatagory.getSelectionModel().select(starting);
		onSetCatagory();
	}

	public void onSetCatagory() {
		String current = shapeCatagory.getSelectionModel().getSelectedItem();
		System.out.println("Selecting shapes from " + current);
		ConfigurationDatabase.put("ShapesPallet", "selected", current).toString();
		active = nameToFile.get(current);
		if (active == null)
			return;
		ArrayList<HashMap<String, String>> orderedList = new ArrayList<HashMap<String, String>>();
		// store the name os the keys for labeling the hoverover later
		HashMap<Map, String> names = new HashMap<>();
		for (String key : active.keySet()) {
			HashMap<String, String> hashMap = active.get(key);
			String s = hashMap.get("order");
			if(s!=null) {
				int index = Integer.parseInt(s);
				System.out.println("Adding " + key + " at " + index);
				while (orderedList.size() <= index)
					orderedList.add(null);
				orderedList.set(index, hashMap);
			}else {
				orderedList.add( hashMap);
			}
			names.put(hashMap, key);
		}
		objectPallet.getChildren().clear();
		referenceParts.clear();
		for (int i = 0; i < orderedList.size(); i++) {
			int col = i % 3;
			int row = i / 3;
			HashMap<String, String> key = orderedList.get(i);
			if(key==null)
				continue;
			System.out.println("Placing " + names.get(key) + " at " + row + " , " + col);
			setupButton(names, key,col, row,objectPallet);
			//objectPallet.add(button, col, row);
		}

	}

	private Button setupButton(HashMap<Map, String> names, HashMap<String, String> key, int col, int row, GridPane objectPallet2) {
		String name = names.get(key);
		Tooltip hover = new Tooltip(name);
		Button button = new Button();
		button.setTooltip(hover);
		button.getStyleClass().add("image-button");
		
		new Thread(() -> {
			AddFromScript set = new AddFromScript().set(key.get("git"), key.get("file"));
			List<CSG> ScriptObjects = set.process(new ArrayList<>());
			referenceParts.put(button,ScriptObjects);
			BowlerStudio.runLater(()->{
				Image thumb = ThumbnailImage.get(ScriptObjects);
				ImageView tIv = new ImageView(thumb);
				tIv.setFitHeight(50);
				tIv.setFitWidth(50);
				button.setGraphic(tIv);
				objectPallet.add(button, col, row);
			});
		}).start();
		button.setOnMousePressed(ev -> {
			new Thread(() -> {
				List<CSG> ScriptObjects = referenceParts.get(button);
				CSG indicator = ScriptObjects.get(0);
				if(ScriptObjects.size()>1) {
					indicator=CSG.unionAll(ScriptObjects);
				}
				session.setMode(SpriteDisplayMode.PLACING);
				workplane.setIndicator(indicator, new Affine());
				workplane.setOnSelectEvent(() -> {
					session.setMode(SpriteDisplayMode.Default);
					if(workplane.isClicked())
					try {
						TransformNR currentAbsolutePose = workplane.getCurrentAbsolutePose();
						AddFromScript setAddFromScript = new AddFromScript().set(key.get("git"), key.get("file"))
								.setLocation(currentAbsolutePose);
						ap.addOp(setAddFromScript).join();
//						List<String> namesToMove = new ArrayList<>();
//						namesToMove.addAll(setAddFromScript.getNamesAdded());
//						cadoodle.addOpperation(new MoveCenter()
//								.setNames(namesToMove)
//								.setLocation(currentAbsolutePose)).join();
						HashSet<String> namesAdded = setAddFromScript.getNamesAdded();
						session.selectAll(namesAdded);
		
						if (!workplane.isClicked())
							return;
						if (workplane.isClickOnGround()) {
							// System.out.println("Ground plane click detected");
							ap.get().setWorkplane(new TransformNR());
						} else {
							ap.get().setWorkplane(workplane.getCurrentAbsolutePose());
						}
						workplane.placeWorkplaneVisualization();
						workplane.setTemporaryPlane();
					} catch (CadoodleConcurrencyException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
				workplane.activate();

			}).start();
			session.setKeyBindingFocus();
		});
		return button;
	}


	public void setCadoodle(ActiveProject ap) {
		this.ap = ap;
	}

	public void setWorkplaneManager(WorkplaneManager workplane) {
		this.workplane = workplane;
	}

}
