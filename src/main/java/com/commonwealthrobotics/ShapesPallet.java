package com.commonwealthrobotics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.SplashManager;
import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.creature.ThumbnailImage;
import com.neuronrobotics.bowlerstudio.scripting.DownloadManager;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AbstractAddFrom;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AddFromFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AddFromScript;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CadoodleConcurrencyException;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.MoveCenter;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Sweep;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.parametrics.CSGDatabase;
import eu.mihosoft.vrl.v3d.parametrics.Parameter;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
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
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.NoSuchFileException;

public class ShapesPallet {
	private static final String SEARCH_MODE = "Search Mode";

	/**
	 * Statics
	 */

	private static String gitULR = "https://github.com/madhephaestus/CaDoodle-Example-Objects.git";

	/**
	 * Class variables
	 */

	private ComboBox<String> shapeCatagory;
	private GridPane objectPallet;
	private HashMap<String, HashMap<String, HashMap<String, String>>> nameToFile = new HashMap<>();
	private Type TT = new TypeToken<HashMap<String, HashMap<String, String>>>() {
	}.getType();
	private Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	private HashMap<String, HashMap<String, String>> active = null;
	private SelectionSession session;
	private WorkplaneManager workplane;
	// private HashMap<Button, List<CSG>> referenceParts = new HashMap<>();
	private ActiveProject ap;
	private boolean threadRunning = false;
	private boolean threadComplete = true;
	private boolean searchMode = false;

	private List<String> sortedList;
	private Thread searchThread = null;
	private ShapePalletMyDoodles mine;

	public ShapesPallet(ComboBox<String> sc, GridPane objectPallet, SelectionSession session, ActiveProject active,
			WorkplaneManager workplane2) {
		this.shapeCatagory = sc;
		this.objectPallet = objectPallet;
		this.session = session;
		ap = active;
		workplane = workplane2;
		mine = new ShapePalletMyDoodles(shapeCatagory, objectPallet, session, ap, workplane);
		// new Thread(() -> {
		try {
			ArrayList<String> files = ScriptingEngine.filesInGit(getGitULR());
			sortedList = new ArrayList<>(files);
			Collections.sort(sortedList);
			shapeCatagory.getItems().add(mine.getName());
			for (String f : sortedList) {
				if (f.toLowerCase().endsWith(".json")) {
					String contents = ScriptingEngine.codeFromGit(getGitULR(), f)[0];
					File fileFromGit = ScriptingEngine.fileFromGit(getGitULR(), f);
					String name = fileFromGit.getName();
					String[] split = name.split(".json");
					String filename = split[0];
					HashMap<String, HashMap<String, String>> tmp = gson.fromJson(contents, TT);
					nameToFile.put(filename, tmp);
					shapeCatagory.getItems().add(filename);
				}
			}
		} catch (Exception e) {
			//
			e.printStackTrace();
		}
		String starting = ConfigurationDatabase.get("ShapesPallet", "selected", "BasicShapes").toString();
		BowlerStudio.runLater(() -> shapeCatagory.getSelectionModel().select(starting));
		onSetCatagory();
		// }).start();
	}

	public void onSetCatagory() {
		if (searchMode)
			return;
		threadRunning = false;
		Thread t = new Thread(() -> {
			SplashManager.renderSplashFrame(50, "Loading Shapes");
			while (!threadComplete) {
				com.neuronrobotics.sdk.common.Log.error("Waiting for shapesThread to exit");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					//
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
				if (current.contentEquals(mine.getName())) {
					mine.activate();
				} else {
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
					// referenceParts.clear();
					for (int i = 0; i < orderedList.size() && threadRunning; i++) {
						int col = i % 3;
						int row = i / 3;
						HashMap<String, String> key = orderedList.get(i);
						if (key == null)
							continue;
						com.neuronrobotics.sdk.common.Log
								.error("Placing " + names.get(key) + " at " + row + " , " + col);
						try {
							setupButton(names.get(key), key, col, row, current);
						} catch (Throwable tx) {
							tx.printStackTrace();
						}
						// objectPallet.add(button, col, row);
					}
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

	private Button setupButton(String name, HashMap<String, String> key, int col, int row, String typeOfShapes) {
		String sweep = key.get("sweep");

		boolean isSweep = (sweep != null) ? Boolean.parseBoolean(sweep) : false;
		Tooltip hover = new Tooltip(name);
		Button button = new Button();
		button.setTooltip(hover);
		button.getStyleClass().add("image-button");
		ShapePalletButtonResources resources = new ShapePalletButtonResources(key, typeOfShapes, name);

		BowlerStudio.runLater(() -> {
			objectPallet.add(button, col, row);
			Image thumb = resources.getImage();
			ImageView tIv = new ImageView(TimelineManager.resizeImage(thumb, 50, 50));
			ImageView toolimage = new ImageView(thumb);

			toolimage.setFitHeight(300);
			toolimage.setFitWidth(300);
			hover.setGraphic(toolimage);
			hover.setContentDisplay(ContentDisplay.TOP);
//			tIv.setFitHeight(50);
//			tIv.setFitWidth(50);
			button.setGraphic(tIv);
			button.setOnMousePressed(ev -> {
				new Thread(() -> {
					CSG indicator = resources.getIndicator();
					session.setMode(SpriteDisplayMode.PLACING);
					workplane.setIndicator(indicator, new Affine());
					boolean workplaneInOrigin = !workplane.isWorkplaneNotOrigin();
					System.out.println("Is Workplane set " + workplaneInOrigin);
					workplane.setOnSelectEvent(() -> {
						new Thread(() -> {
							session.setMode(SpriteDisplayMode.Default);
							if (workplane.isClicked())
								try {
									TransformNR currentAbsolutePose = workplane.getCurrentAbsolutePose();
									AbstractAddFrom setAddFromScript = new AddFromScript()
											.set(key.get("git"), key.get("file")).setLocation(currentAbsolutePose);

									if (isSweep) {
										try {
											File f = ScriptingEngine.fileFromGit(key.get("git"), key.get("file"));
											Sweep s = new Sweep();
											String ZPer = key.get("ZPer");
											String Degrees = key.get("Degrees");
											String sprial = key.get("Spiral");
											if (ZPer != null) {
												s.setDefz(Double.parseDouble(ZPer));
											}
											if (Degrees != null)
												s.setDefangle(Double.parseDouble(Degrees));
											if (sprial != null) {
												s.setDefSpiral(Double.parseDouble(sprial));
											}
											s.set(f).setPreventBoM(true).setLocation(currentAbsolutePose);
											setAddFromScript = s;
										} catch (Exception e) {
											e.printStackTrace();
											return;
										}
									}

									String string = key.get("copyFile");
									if (string != null) {
										if (Boolean.parseBoolean(string)) {
											try {
												File f = ScriptingEngine.fileFromGit(key.get("git"), key.get("file"));
												setAddFromScript = new AddFromFile().set(f)
														.setLocation(currentAbsolutePose);
											} catch (InvalidRemoteException e) {
												//
												e.printStackTrace();
											} catch (TransportException e) {
												e.printStackTrace();
											} catch (GitAPIException e) {
												e.printStackTrace();
											} catch (IOException e) {
												e.printStackTrace();
											}
										}
									}
									ap.addOp(setAddFromScript).join();

									HashSet<String> namesAdded = setAddFromScript.getNamesAdded();
									ArrayList<String> namesBack = new ArrayList<String>();
									namesBack.addAll(namesAdded);
//
//									MoveCenter mc = new MoveCenter().setNames(namesBack)
//											.setLocation(currentAbsolutePose);
//									ap.addOp(mc).join();
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
									if (workplaneInOrigin)
										workplane.setTemporaryPlane();
								} catch (CadoodleConcurrencyException e) {
									e.printStackTrace();
								} catch (InterruptedException e) {
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
			e.printStackTrace();
		}

		return button;
	}

	public static String getGitULR() {
		return gitULR;
	}

	public boolean isSearchMode() {
		return searchMode;
	}

	public void setSearchMode(boolean searchMode, TextField searchField) {
		this.searchMode = searchMode;
		if (searchMode) {
			objectPallet.getChildren().clear();
			searchField.textProperty().addListener((observable, oldValue, newValue) -> {
				updateFromSearch(searchField.getText());
			});
			searchField.setOnAction(ev -> {
				updateFromSearch(searchField.getText());
			});
		}

		BowlerStudio.runLater(() -> shapeCatagory.setDisable(searchMode));
		onSetCatagory();
	}

	private void updateFromSearch(String newValue) {
		if (searchThread != null)
			return;
		objectPallet.getChildren().clear();
		if (newValue.length() < 2)
			return;
		searchThread = new Thread(() -> {
			System.out.println("Text changed to: " + newValue);
			int i = 0;
			HashSet<String> buttons = new HashSet<String>();
			for (String current : nameToFile.keySet()) {
				active = nameToFile.get(current);
				if (active == null)
					continue;
				for (String name : active.keySet()) {
					if (i > 15)
						break;
					HashMap<String, String> hashMap = active.get(name);
//					String string = hashMap.get("plugin");
//					if(string!=null) {
//						boolean b=DownloadManager.isDownloadedAlready(string);
//						if(!b)
//							continue;
//					}
					if (buttons.contains(name))
						continue;
					buttons.add(name);
					if (name.toLowerCase().contains(newValue.toLowerCase())) {
						System.out.println("Matching " + current + " value " + name);
						int col = i % 3;
						int row = i / 3;
						setupButton(name, hashMap, col, row, current);
						i++;
					}
				}
				if (i > 15)
					break;
			}
			
			try {
				List<CaDoodleFile> proj = ap.getProjects();
				for (int j=0; j < proj.size(); j++) {
					if(proj.get(j).getMyProjectName().toLowerCase().contains(newValue.toLowerCase())) {
						int col = i % 3;
						int row = i / 3;
						mine.setupButton(proj.get(j), col, row);
						i++;
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			searchThread = null;
		});
		searchThread.start();
	}

}
