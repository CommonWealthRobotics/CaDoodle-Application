package com.commonwealthrobotics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.SplashManager;
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
	private HashMap<Button, List<CSG>> referenceParts = new HashMap<>();
	private ActiveProject ap;
	private boolean threadRunning = false;
	private boolean threadComplete = true;

	public ShapesPallet(ComboBox<String> sc, GridPane objectPallet, SelectionSession session, ActiveProject active,
			WorkplaneManager workplane2) {
		this.shapeCatagory = sc;
		this.objectPallet = objectPallet;
		this.session = session;
		ap = active;
		workplane = workplane2;
		// new Thread(() -> {
		try {
			ScriptingEngine.cloneRepo(gitULR, null);
			ScriptingEngine.pull(gitULR);
			ArrayList<String> files = ScriptingEngine.filesInGit(gitULR);
			List<String> sortedList = new ArrayList<>(files);
			Collections.sort(sortedList);
			for (String f : sortedList) {
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
		BowlerStudio.runLater(() -> shapeCatagory.getSelectionModel().select(starting));
		onSetCatagory();
		// }).start();
	}

	public void onSetCatagory() {
		threadRunning = false;
		Thread t = new Thread(() -> {
			SplashManager.renderSplashFrame(50, "Loading Shapes");
			while (!threadComplete) {
				com.neuronrobotics.sdk.common.Log.error("Waiting for shapesThread to exit");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			threadComplete = false;
			threadRunning = true;
			ap.setDisableRegenerate(true);
			try {
				String current = shapeCatagory.getSelectionModel().getSelectedItem();
				com.neuronrobotics.sdk.common.Log.error("Selecting shapes from " + current);
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
					if (s != null) {
						int index = Integer.parseInt(s);
						com.neuronrobotics.sdk.common.Log.error("Adding " + key + " at " + index);
						while (orderedList.size() <= index)
							orderedList.add(null);
						orderedList.set(index, hashMap);
					} else {
						orderedList.add(hashMap);
					}
					names.put(hashMap, key);
				}
				BowlerStudio.runLater(() -> objectPallet.getChildren().clear());
				Thread.sleep(30);
				referenceParts.clear();
				for (int i = 0; i < orderedList.size() && threadRunning; i++) {
					int col = i % 3;
					int row = i / 3;
					HashMap<String, String> key = orderedList.get(i);
					if (key == null)
						continue;
					com.neuronrobotics.sdk.common.Log.error("Placing " + names.get(key) + " at " + row + " , " + col);
					try {
						setupButton(names, key, col, row);

					} catch (Throwable tx) {
						tx.printStackTrace();
					}
					// objectPallet.add(button, col, row);
				}
			} catch (Throwable tr) {
				tr.printStackTrace();
			}
			ap.setDisableRegenerate(false);
			threadComplete = true;
			SplashManager.closeSplash();
		});
		t.start();
	}

	private Button setupButton(HashMap<Map, String> names, HashMap<String, String> key, int col, int row) {
		// TODO cache images and STLs
		String typeOfShapes = ConfigurationDatabase.get("ShapesPallet", "selected", "BasicShapes").toString();

		String name = names.get(key);
		Tooltip hover = new Tooltip(name);
		Button button = new Button();
		button.setTooltip(hover);
		button.getStyleClass().add("image-button");
		// new Thread(() -> {
		AddFromScript set = new AddFromScript().set(key.get("git"), key.get("file")).setPreventBoM(true);
		List<CSG> so = set.process(new ArrayList<>());
		referenceParts.put(button, so);
		BowlerStudio.runLater(() -> {
			objectPallet.add(button, col, row);
			if (typeOfShapes.toLowerCase().contains("vitamin"))
				for (CSG c : so) {
					c.setIsHole(false);
				}
			Image thumb = ThumbnailImage.get(so);
			ImageView tIv = new ImageView(thumb);
			tIv.setFitHeight(50);
			tIv.setFitWidth(50);
			button.setGraphic(tIv);
			button.setOnMousePressed(ev -> {
				new Thread(() -> {
					List<CSG> ScriptObjects = referenceParts.get(button);
					CSG indicator = ScriptObjects.get(0);
					if (ScriptObjects.size() > 1) {
						indicator = CSG.unionAll(ScriptObjects);
					}
					session.setMode(SpriteDisplayMode.PLACING);
					workplane.setIndicator(indicator, new Affine());
					workplane.setOnSelectEvent(() -> {
						new Thread(() -> {
							session.setMode(SpriteDisplayMode.Default);
							if (workplane.isClicked())
								try {
									TransformNR currentAbsolutePose = workplane.getCurrentAbsolutePose();
									AddFromScript setAddFromScript = new AddFromScript()
											.set(key.get("git"), key.get("file")).setLocation(currentAbsolutePose);
									ap.addOp(setAddFromScript).join();
									HashSet<String> namesAdded = setAddFromScript.getNamesAdded();
									session.selectAll(namesAdded);

									if (!workplane.isClicked())
										return;
									if (workplane.isClickOnGround()) {
										// com.neuronrobotics.sdk.common.Log.error("Ground plane click detected");
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

						}).start();
					});
					workplane.activate();

				}).start();
				session.setKeyBindingFocus();
			});
		});
		try {
			Thread.sleep(30);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return button;
	}
}
