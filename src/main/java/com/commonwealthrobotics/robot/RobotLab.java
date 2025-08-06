package com.commonwealthrobotics.robot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.TimelineManager;
import com.commonwealthrobotics.WorkplaneManager;
import com.commonwealthrobotics.controls.SelectionSession;
import com.commonwealthrobotics.controls.SpriteDisplayMode;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.creature.ControllerOption;
import com.neuronrobotics.bowlerstudio.creature.MobileBaseBuilder;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AbstractAddFrom;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AddFromFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AddFromScript;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CadoodleConcurrencyException;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Sweep;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.robot.AddRobotController;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.robot.MakeRobot;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Affine;

public class RobotLab {

	private SelectionSession session;
	private ActiveProject ap;
	private MobileBaseBuilder builder = null;
	private VBox baseRobotBox;
	private Button makeRobotButton;
	private TabPane robotLabTabPane;
	private Tab bodyTab;
	private Tab headTab;
	private Tab limbTab;
	private Tab toollTab;
	private Tab advancedTab;
	private GridPane robotBasePanel;
	private GridPane controllerGrid;
	private GridPane controllerFeaturesGrid;
	private ArrayList<ControllerOption> controllers = null;
	private WorkplaneManager workplane;
	private VBox controllersVBox;
	private VBox capabilitiesVBox;

	public RobotLab(SelectionSession session, ActiveProject ap, VBox baseRobotBox, Button makeRobotButton,
			TabPane robotLabTabPane, Tab bodyTab, Tab headTab, Tab limbTab, Tab toollTab, Tab advancedTab,
			GridPane robotBasePanel, GridPane controllerGrid, GridPane controllerFeaturesGrid,
			WorkplaneManager workplane, VBox controllersVBox, VBox capabilitiesVBox) {
		this.session = session;
		this.ap = ap;
		this.baseRobotBox = baseRobotBox;
		this.makeRobotButton = makeRobotButton;
		this.robotLabTabPane = robotLabTabPane;
		this.bodyTab = bodyTab;
		this.headTab = headTab;
		this.limbTab = limbTab;
		this.toollTab = toollTab;
		this.advancedTab = advancedTab;
		this.robotBasePanel = robotBasePanel;
		this.controllerGrid = controllerGrid;
		this.controllerFeaturesGrid = controllerFeaturesGrid;
		this.workplane = workplane;
		this.controllersVBox = controllersVBox;
		this.capabilitiesVBox = capabilitiesVBox;
		session.setUpdateRobotLab(() -> {
			builder = null;
			updateDisplay();
		});
		updateDisplay();
	}

	public void setRobotLabOpenState(boolean isOpen) {
		if (isOpen) {
			updateDisplay();
		} else {
			builder = null;
		}
	}

	public void updateDisplay() {
		new Thread(() -> {
			searchForBuilder();
			setupMainPanel();
			setupControllersPanel();
			setupTabs();
		}).start();
	}

	private void setupMainPanel() {
		BowlerStudio.runLater(() -> {
			controllersVBox.getChildren().clear();
			capabilitiesVBox.getChildren().clear();
			if (builder == null) {
				controllersVBox.getChildren().add(new Label("No Builder"));

				return;
			}

			ArrayList<AddRobotController> controllers2 = builder.getControllers();
			if (controllers2.size() == 0) {
				controllersVBox.getChildren().add(new Label("No controllers added yet"));

			} else
				for (AddRobotController ac : controllers2) {
					controllersVBox.getChildren().add(new Label(ac.getController().getType()));
				}

		});

	}

	private void searchForBuilder() {
		CaDoodleFile caDoodleFile = ap.get();
		if (caDoodleFile != null) {
			HashMap<String, MobileBaseBuilder> robots = caDoodleFile.getRobots();
			if (builder == null) {
				for (String s : robots.keySet()) {
					if (builder != null)
						break;
					for (CSG c : session.getSelectedCSG(session.selectedSnapshot())) {
						Optional<String> mobileBaseName = c.getMobileBaseName();
						if (mobileBaseName.isPresent()) {
							if (mobileBaseName.get().contentEquals(s)) {
								builder = robots.get(s);
								break;
							}
						}
					}
				}
			}
		}
	}

	private void setupTabs() {
		BowlerStudio.runLater(() -> {
			boolean value = session.numberSelected() == 0;
			robotLabTabPane.setDisable(value);
			if (builder == null) {
				if (!baseRobotBox.getChildren().contains(makeRobotButton)) {
					baseRobotBox.getChildren().add(makeRobotButton);
				}

				if (baseRobotBox.getChildren().contains(robotBasePanel))
					baseRobotBox.getChildren().remove(robotBasePanel);
				headTab.setDisable(true);
				limbTab.setDisable(true);
				toollTab.setDisable(true);
				advancedTab.setDisable(true);
				if (!value)
					robotLabTabPane.getSelectionModel().select(bodyTab);
			} else {
				if (baseRobotBox.getChildren().contains(makeRobotButton)) {
					baseRobotBox.getChildren().remove(makeRobotButton);
				}

				if (!baseRobotBox.getChildren().contains(robotBasePanel))
					baseRobotBox.getChildren().add(robotBasePanel);
				headTab.setDisable(false);
				limbTab.setDisable(false);
				toollTab.setDisable(false);
				advancedTab.setDisable(false);
			}
		});
	}

	private void setupControllersPanel() {
		try {
			if (controllers == null)
				controllers = ControllerOption.getOptions();
			BowlerStudio.runLater(() -> controllerGrid.getChildren().clear());
			for (int i = 0; i < controllers.size(); i++) {
				ControllerOption o = controllers.get(i);
				int col = i % 3;
				int row = i / 3;
				setupButton(o, row, col);
				// System.out.println(o);
			}
		} catch (GitAPIException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setupButton(ControllerOption o, int col, int row) {
		o.build(ap.get());
		Tooltip hover = new Tooltip(o.getType());
		Button button = new Button();
		button.setTooltip(hover);
		button.getStyleClass().add("image-button");
		BowlerStudio.runLater(() -> {
			controllerGrid.add(button, col, row);
			Image thumb = o.getImage();
			ImageView tIv = new ImageView(TimelineManager.resizeImage(thumb, 60, 60));
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
					CSG indicator = o.getIndicator();
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
									AddRobotController add = new AddRobotController().setController(o)
											.setLocation(currentAbsolutePose).setNames(session.selectedSnapshot());
									ap.addOp(add).join();

									HashSet<String> namesAdded = add.getNamesAdded();
									ArrayList<String> namesBack = new ArrayList<String>();
									namesBack.addAll(namesAdded);
//
									session.selectAll(namesAdded);
									if (!workplane.isClicked())
										return;
									if (workplane.isClickOnGround()) {
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
	}

	public void makeRobot() {
		new Thread(() -> {
			MakeRobot mr = new MakeRobot();
			mr.setNames(session.selectedSnapshot());

			ap.addOp(mr);
			builder = ap.get().getRobots().get(mr.getName());
			if (builder == null)
				throw new RuntimeException("Failed to create robot!");
			updateDisplay();
		}).start();
	}

}
