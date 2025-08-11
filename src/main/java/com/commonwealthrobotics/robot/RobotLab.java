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
import com.neuronrobotics.bowlerstudio.creature.ControllerFeatures;
import com.neuronrobotics.bowlerstudio.creature.ControllerOption;
import com.neuronrobotics.bowlerstudio.creature.LimbOption;
import com.neuronrobotics.bowlerstudio.creature.MobileBaseBuilder;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AbstractAddFrom;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AddFromFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AddFromScript;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CadoodleConcurrencyException;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Sweep;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.robot.AddRobotController;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.robot.AddRobotLimb;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.robot.MakeRobot;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
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
import javafx.scene.layout.HBox;
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
	private Tab advancedTab;
	private GridPane robotBasePanel;
	private GridPane controllerGrid;
	private GridPane controllerFeaturesGrid;
	private ArrayList<ControllerOption> controllers = null;
	private ArrayList<LimbOption> limbOptions = null;
	private WorkplaneManager workplane;
	private VBox controllersVBox;
	private VBox capabilitiesVBox;
	private VBox optionProvide;
	private VBox optionsConsume;
	private GridPane wheelOptionGrid;
	private GridPane legsOptionGrid;
	private GridPane armsOptionGrid;
	private VBox controllerConsumedBox;
	private boolean controllersLoaded;
	private boolean limmbsLoaded;
	

	public RobotLab(SelectionSession session, ActiveProject ap, VBox baseRobotBox, Button makeRobotButton,
			TabPane robotLabTabPane, Tab bodyTab, Tab headTab, Tab advancedTab, GridPane robotBasePanel,
			GridPane controllerGrid, GridPane controllerFeaturesGrid, WorkplaneManager workplane, VBox controllersVBox,
			VBox controllerConsumedBox, VBox capabilitiesVBox, VBox optionProvide, VBox optionsConsume,
			GridPane wheelOptionGrid, GridPane legsOptionGrid, GridPane armsOptionGrid) {
		this.session = session;
		this.ap = ap;
		this.baseRobotBox = baseRobotBox;
		this.makeRobotButton = makeRobotButton;
		this.robotLabTabPane = robotLabTabPane;
		this.bodyTab = bodyTab;
		this.headTab = headTab;
		this.controllerConsumedBox = controllerConsumedBox;
		this.optionProvide = optionProvide;
		this.optionsConsume = optionsConsume;
		this.advancedTab = advancedTab;
		this.robotBasePanel = robotBasePanel;
		this.controllerGrid = controllerGrid;
		this.controllerFeaturesGrid = controllerFeaturesGrid;
		this.workplane = workplane;
		this.controllersVBox = controllersVBox;
		this.capabilitiesVBox = capabilitiesVBox;
		this.wheelOptionGrid = wheelOptionGrid;
		this.legsOptionGrid = legsOptionGrid;
		this.armsOptionGrid = armsOptionGrid;
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
			setupLimbsPanel();
			setupTabs();
			session.setupLimbManipulators(builder);
		}).start();
	}


	private void setupMainPanel() {
		BowlerStudio.runLater(() -> {
			controllersVBox.getChildren().clear();
			capabilitiesVBox.getChildren().clear();
			controllerConsumedBox.getChildren().clear();
			if (builder == null) {
				controllersVBox.getChildren().add(new Label("No Builder"));

				return;
			}

			ArrayList<AddRobotController> controllers2 = builder.getControllers();
			if (controllers2.size() == 0) {
				controllersVBox.getChildren().add(new Label("No controllers added yet"));

			} else {
				ControllerFeatures combined = new ControllerFeatures();
				ControllerFeatures consumed = new ControllerFeatures();
				int num = 1;
				for (AddRobotController ac : controllers2) {
					ControllerOption controller = ac.getController();
					combined.add(controller.getProvides());
					consumed.add(controller.getConsumes());
					controllersVBox.getChildren().add(new Label(num + " " + controller.getType()));
					num++;
				}
				for (AddRobotLimb c : builder.getLimmbs()) {
					LimbOption controller = c.getLimb();
					combined.add(controller.getProvides());
					consumed.add(controller.getConsumes());
				}

				setFunctionalityToList(combined, consumed, capabilitiesVBox, controllerConsumedBox);
			}

		});

	}

	private void setFunctionalityToList(ControllerFeatures provided, ControllerFeatures consumed, VBox provide,
			VBox consume) {
		if (provided == null)
			provided = new ControllerFeatures();
		if (consumed == null)
			consumed = new ControllerFeatures();

		// vexV5Motors += f.vexV5Motors;
		makeLine("vexV5Motors", provided.getVexV5Motors(), consumed.getVexV5Motors(), provide, consume);
		// hiwonderBus += f.hiwonderBus;
		makeLine("hiwonderBus", provided.getHiwonderBus(), consumed.getHiwonderBus(), provide, consume);
		// dynamixelBus += f.dynamixelBus;
		makeLine("dynamixelBus", provided.getDynamixelBus(), consumed.getDynamixelBus(), provide, consume);
		// steppers += f.steppers;
		makeLine("steppers", provided.getSteppers(), consumed.getSteppers(), provide, consume);
		// servoChannels+=f.servoChannels;
		makeLine("Servos", provided.getServoChannels(), consumed.getServoChannels(), provide, consume);
		// motorChannels+=f.motorChannels;
		makeLine("Motors", provided.getMotorChannels(), consumed.getMotorChannels(), provide, consume);
		// analogSensorChannels+=f.analogSensorChannels;
		makeLine("Analog", provided.getAnalogSensorChannels(), consumed.getAnalogSensorChannels(), provide, consume);
		// digitalSensorChannels+=f.digitalSensorChannels;
		makeLine("Digital", provided.getDigitalSensorChannels(), consumed.getDigitalSensorChannels(), provide, consume);
		// cameras+=f.cameras;
		makeLine("Camera", provided.getCameras(), consumed.getCameras(), provide, consume);
		// inertialSensors+=f.inertialSensors;
		makeLine("Inertial", provided.getInertialSensors(), consumed.getInertialSensors(), provide, consume);
		// distanceSensors+=f.distanceSensors;
		makeLine("Distance", provided.getDistanceSensors(), consumed.getDistanceSensors(), provide, consume);
		// pointCloudSensors+=f.pointCloudSensors;
		makeLine("Point Cloud", provided.getPointCloudSensors(), consumed.getPointCloudSensors(), provide, consume);
		// getBatteryVoltage().addAll(f.getBatteryVoltage());
		for (Double d : provided.getBatteryVoltage()) {
			makeVoltage("+Rail ", d, "volts", provide);
		}
		// batteryPeakWatt+=f.batteryPeakWatt;
		makeVoltage("+Peak W ", provided.getBatteryPeakWatts(), "W", provide);
		// batteryWattHour+=f.batteryWattHour;
		makeVoltage("+Capacity ", provided.getBatteryWattHours(), "W-H", provide);
		for (Double d : consumed.getBatteryVoltage()) {
			makeVoltage("-Rail ", d, "volts", consume);
		}
		// batteryPeakWatt+=f.batteryPeakWatt;
		makeVoltage("-Peak W ", consumed.getBatteryPeakWatts(), "W", consume);
		// batteryWattHour+=f.batteryWattHour;
		makeVoltage("-Capacity ", consumed.getBatteryWattHours(), "W-H", consume);
	}

	private void makeVoltage(String l, double has, String type, VBox provide) {
		if (has == 0)
			return;
		HBox line = new HBox(10);
		Label label = new Label(l);
		label.setPrefWidth(80);
		line.getChildren().add(label);
		Label numHave = new Label(String.format("%.2f", has));
		numHave.setPrefWidth(60);
		line.getChildren().add(numHave);
		Label used = new Label(type);
		used.setPrefWidth(60);
		line.getChildren().add(used);
		provide.getChildren().add(line);
	}

	private void makeLine(String l, int has, int usednum, VBox provide, VBox consume) {
		if (has == 0 && usednum == 0)
			return;
		HBox line = new HBox(10);
		Label label = new Label("+" + l);
		label.setPrefWidth(80);
		line.getChildren().add(label);
		Label numHave = new Label(has + "");
		numHave.setPrefWidth(60);
		line.getChildren().add(numHave);

		provide.getChildren().add(line);
		line = new HBox(10);
		Label used = new Label("-" + l);
		used.setPrefWidth(80);
		line.getChildren().add(used);
		Label num = new Label(usednum + "");
		num.setPrefWidth(60);
		line.getChildren().add(num);
		consume.getChildren().add(line);
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
				advancedTab.setDisable(false);
			}
		});
	}

	private void setupLimbsPanel() {
		if(limmbsLoaded)
			return;
		limmbsLoaded=true;
		try {
			if (limbOptions == null)
				limbOptions = LimbOption.getOptions();
			ArrayList<LimbOption> arms = new ArrayList<LimbOption>();
			ArrayList<LimbOption> legs = new ArrayList<LimbOption>();
			ArrayList<LimbOption> wheels = new ArrayList<LimbOption>();
			for (LimbOption o : limbOptions) {
				switch (o.getType()) {
				case arm:
				case flap:
				case hand:
				case head:
					arms.add(o);
					break;
				case leg:
					legs.add(o);
					break;
				case steerable:
				case wheel:
					wheels.add(o);
					break;
				}
				//break;
			}
			setupLimbOption(arms, armsOptionGrid);
			setupLimbOption(legs, legsOptionGrid);
			setupLimbOption(wheels, wheelOptionGrid);
		} catch (GitAPIException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setupLimbOption(ArrayList<LimbOption> arms, GridPane armsOptionGrid2) {
		
		BowlerStudio.runLater(() -> {
			armsOptionGrid2.getChildren().clear();
		});

		for (int i = 0; i < arms.size(); i++) {
			LimbOption o = arms.get(i);
			int col = i % 3;
			int row = i / 3;
			setupAddLimbButton(o, row, col,armsOptionGrid2);
		}

	}

	private void setupControllersPanel() {
		if(controllersLoaded)
			return;
		controllersLoaded=true;
		try {
			if (controllers == null)
				controllers = ControllerOption.getOptions();
			BowlerStudio.runLater(() -> controllerGrid.getChildren().clear());
			for (int i = 0; i < controllers.size(); i++) {
				ControllerOption o = controllers.get(i);
				int col = i % 3;
				int row = i / 3;
				setupAddControllerButton(o, row, col);
				// System.out.println(o);
			}
		} catch (GitAPIException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setupAddLimbButton(LimbOption o, int row, int col, GridPane armsOptionGrid2) {
		try {
			o.build(ap.get());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Tooltip hover = new Tooltip(o.getName() + " "+o.getType());
		Button button = new Button(o.getName());
		button.setTooltip(hover);
		button.getStyleClass().add("image-button");
		button.setOnMouseEntered(event -> {
			optionProvide.getChildren().clear();
			optionsConsume.getChildren().clear();
			setFunctionalityToList(o.getProvides(), o.getConsumes(), optionProvide, optionsConsume);
		});

		// Action when mouse exits the button
		button.setOnMouseExited(event -> {
			optionProvide.getChildren().clear();
			optionsConsume.getChildren().clear();
		});
		BowlerStudio.runLater(() -> {
			armsOptionGrid2.add(button, col, row);
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
									TransformNR currentAbsolutePose =workplane.getCurrentAbsolutePose().times( LimbOption.LimbRotationOffset);
									AddRobotLimb add = new AddRobotLimb().setLimb(o).setLocation(currentAbsolutePose)
											.setNames(session.selectedSnapshot());
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

	private void setupAddControllerButton(ControllerOption o, int col, int row) {
		o.build(ap.get());
		Tooltip hover = new Tooltip(o.getType());
		Button button = new Button();
		button.setTooltip(hover);
		button.getStyleClass().add("image-button");
		button.setOnMouseEntered(event -> {
			optionProvide.getChildren().clear();
			optionsConsume.getChildren().clear();
			setFunctionalityToList(o.getProvides(), o.getConsumes(), optionProvide, optionsConsume);
		});

		// Action when mouse exits the button
		button.setOnMouseExited(event -> {
			optionProvide.getChildren().clear();
			optionsConsume.getChildren().clear();
		});
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
