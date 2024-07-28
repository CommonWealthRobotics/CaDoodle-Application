/**
 * Sample Skeleton for 'MainWindow.fxml' Controller Class
 */

package com.commonwealthrobotics;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.fxyz3d.scene.paint.Patterns;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.CaDoodleLoader;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleOpperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.IControlsMap;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.event.ActionEvent;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

public class MainController implements ICaDoodleStateUpdate {
	private static final int ZOOM = -1000;
	private ActiveProject ap = new ActiveProject();
	private CaDoodleFile cadoodle;
	private List<CSG> onDisplay = new ArrayList<>();
	private boolean drawerOpen = true;
	@FXML
	private AnchorPane anchorPanForConfiguration;

	@FXML
	private ColorPicker colorPicker;
	@FXML
	private GridPane buttonGrid;
	@FXML
	private GridPane configurationGrid;

	@FXML
	private AnchorPane control3d;

	@FXML
	private GridPane controlBar;

	@FXML
	private Button cruseButton;

	@FXML
	private AnchorPane drawerArea;

	@FXML
	private Button drawerButton;

	@FXML
	private GridPane drawerGrid;

	@FXML
	private HBox drawerHolder;

	@FXML
	private ImageView drawrImage;

	@FXML
	private Button export;

	@FXML
	private TextField fileNameBox;

	@FXML
	private Button fitViewButton;

	@FXML
	private Button groupButton;

	@FXML
	private Button hideSHow;

	@FXML
	private Button holeButton;

	@FXML
	private Button home;

	@FXML
	private Button homeButton;

	@FXML
	private Button homeViewButton;

	@FXML
	private Button importButton;

	@FXML
	private Button lockButton;

	@FXML
	private Tooltip lockUnlockTooltip;

	@FXML
	private Button mirronButton;

	@FXML
	private Button model;

	@FXML
	private Button modeling;

	@FXML
	private Button notesButton;

	@FXML
	private GridPane objectPallet;

	@FXML
	private Button physics;

	@FXML
	private Button physicsButton;

	@FXML
	private Button rulerButton;

	@FXML
	private Button settingsButton;

	@FXML
	private ComboBox<?> shapeCatagory;

	@FXML
	private TitledPane shapeConfiguration;

	@FXML
	private Accordion shapeConfigurationBox;

	@FXML
	private AnchorPane shapeConfigurationHolder;

	@FXML
	private Button showAllButton;

	@FXML
	private ComboBox<?> snapGrid;

	@FXML
	private GridPane topBar;

	@FXML
	private AnchorPane totalApplicationBackground;

	@FXML
	private Button ungroupButton;

	@FXML
	private AnchorPane view3d;

	@FXML
	private AnchorPane viewControlCubeHolder;

	@FXML
	private MenuButton visbilityButton;

	@FXML
	private Button workplaneButton;

	@FXML
	private Button zoomInButton;

	@FXML
	private Button zoomOutButton;
	@FXML
	private Button redoButton;
	@FXML
	private Button undoButton;
	/**
	 * CaDoodle Model Classes
	 */
	private BowlerStudio3dEngine navigationCube;
	private BowlerStudio3dEngine engine;

	@FXML
	void onRedo(ActionEvent event) {
		System.out.println("On Redo");
		cadoodle.forward();
	}

	@FXML
	void onUndo(ActionEvent event) {
		System.out.println("On Undo");
		cadoodle.back();
	}

	@FXML
	void onPaste(ActionEvent event) {
		System.out.println("On Paste");
	}

	@FXML
	void onCopy(ActionEvent event) {
		System.out.println("On copy");
	}

	@FXML
	void onDelete(ActionEvent event) {
		System.out.println("On Delete");
	}

	@FXML
	void onColorPick(ActionEvent event) {

		Color value = colorPicker.getValue();
		String hexColor = String.format("#%02X%02X%02X", (int) (value.getRed() * 255), (int) (value.getGreen() * 255),
				(int) (value.getBlue() * 255));

		String style = String.format(" -fx-background-color: %s;", hexColor);
		colorPicker.setStyle(style);
		System.out.println("Color set to " + value);
	}

	@FXML
	void onCruse(ActionEvent event) {
		System.out.println("On Cruse");
	}

	@FXML
	void onDrawer(ActionEvent event) {
		drawerOpen = !drawerOpen;
		if (drawerOpen) {
			drawrImage.setImage(new Image(MainController.class.getResourceAsStream("drawerClose.png")));
			drawerHolder.getChildren().add(drawerArea);
		} else {
			drawrImage.setImage(new Image(MainController.class.getResourceAsStream("drawerOpen.png")));
			drawerHolder.getChildren().remove(drawerArea);
		}
	}

	@FXML
	void onExport(ActionEvent event) {
		System.out.println("On Export");
	}

	@FXML
	void onFitView(ActionEvent event) {
		engine.focusOrentation(new TransformNR(0, 0, 0, new RotationNR(0, 45, -45)), new TransformNR(), ZOOM);
	}

	@FXML
	void onGroup(ActionEvent event) {
		System.out.println("On Group");
	}

	@FXML
	void onHideConnections(ActionEvent event) {
		System.out.println(" on Hide Physics Connections");
	}

	@FXML
	void onHideNotes(ActionEvent event) {
		System.out.println("On Hide Notes ");
	}

	@FXML
	void onHideShow(ActionEvent event) {
		System.out.println("On Hide Show");
	}

	@FXML
	void onHoleButton(ActionEvent event) {
		System.out.println("Set to Hole ");
	}

	@FXML
	void onHome(ActionEvent event) {
		System.out.println("On Home");
	}

	@FXML
	void onHomeViewButton(ActionEvent event) {
		engine.focusOrentation(new TransformNR(0, 0, 0, new RotationNR(0, 45, -45)), new TransformNR(), ZOOM);
	}

	@FXML
	void onImport(ActionEvent event) {
		System.out.println("On Import");
	}

	@FXML
	void onLock(ActionEvent event) {
		System.out.println("On Lock Selected");
	}

	@FXML
	void onMirron(ActionEvent event) {
		System.out.println("On Mirron Object");
	}

	@FXML
	void onModeling(ActionEvent event) {
		System.out.println("Select Modeling View");
	}

	@FXML
	void onNotesClick(ActionEvent event) {
		System.out.println("On Notes");
	}

	@FXML
	void onPhysics(ActionEvent event) {
		System.out.println("On Physics Mode Selected");
	}

	@FXML
	void onRuler(ActionEvent event) {
		System.out.println("On Add Ruler");
	}

	@FXML
	void onSetCatagory(ActionEvent event) {
		System.out.println("On Set Catagory");
	}

	@FXML
	void onSettings(ActionEvent event) {
		System.out.println("On Settings");
	}

	@FXML
	void onShowHidden(ActionEvent event) {
		System.out.println("On Show Hidden");
	}

	@FXML
	void onUngroup(ActionEvent event) {
		System.out.println("On Ungroup");
	}

	@FXML
	void onVisibility(ActionEvent event) {
		System.out.println("On Visibility Menu");
	}

	@FXML
	void onWOrkplane(ActionEvent event) {
		System.out.println("On Set Workplane");
	}

	@FXML
	void onZoomIn(ActionEvent event) {

		System.out.println("Zoom In");
		engine.setZoom((int) engine.getFlyingCamera().getZoomDepth() + 20);
	}

	@FXML
	void onZoomOut(ActionEvent event) {
		System.out.println("Zoom Out");

		engine.setZoom((int) engine.getFlyingCamera().getZoomDepth() - 20);
	}

	@FXML
	void setName(ActionEvent event) {
		System.out.println("Set Project Name to " + fileNameBox.getText());
		cadoodle.setProjectName(fileNameBox.getText());
		save();
	}

	@FXML
	void setSnapGrid(ActionEvent event) {
		System.out.println("Set Snap Grid");
	}

	@FXML
	void showAll(ActionEvent event) {
		System.out.println("On SHow All");
	}

	@FXML // This method is called by the FXMLLoader when initialization is complete
	void initialize() {
		assert colorPicker != null : "fx:id=\"colorPicker\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert control3d != null : "fx:id=\"control3d\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert cruseButton != null : "fx:id=\"cruseButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert drawerArea != null : "fx:id=\"drawerArea\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert drawerButton != null
				: "fx:id=\"drawerButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert drawerHolder != null
				: "fx:id=\"drawerHolder\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert drawrImage != null : "fx:id=\"drawrImage\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert export != null : "fx:id=\"export\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert fileNameBox != null : "fx:id=\"fileNameBox\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert fitViewButton != null
				: "fx:id=\"fitViewButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert groupButton != null : "fx:id=\"groupButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert hideSHow != null : "fx:id=\"hideSHow\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert holeButton != null : "fx:id=\"holeButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert home != null : "fx:id=\"home\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert homeButton != null : "fx:id=\"homeButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert homeViewButton != null
				: "fx:id=\"homeViewButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert importButton != null
				: "fx:id=\"importButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert lockButton != null : "fx:id=\"lockButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert lockUnlockTooltip != null
				: "fx:id=\"lockUnlockTooltip\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert mirronButton != null
				: "fx:id=\"mirronButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert model != null : "fx:id=\"model\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert modeling != null : "fx:id=\"modeling\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert notesButton != null : "fx:id=\"notesButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert objectPallet != null
				: "fx:id=\"objectPallet\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert physics != null : "fx:id=\"physics\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert physicsButton != null
				: "fx:id=\"physicsButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert rulerButton != null : "fx:id=\"rulerButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert settingsButton != null
				: "fx:id=\"settingsButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert shapeCatagory != null
				: "fx:id=\"shapeCatagory\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert shapeConfiguration != null
				: "fx:id=\"shapeConfiguration\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert shapeConfigurationBox != null
				: "fx:id=\"shapeConfigurationBox\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert shapeConfigurationHolder != null
				: "fx:id=\"shapeConfigurationHolder\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert showAllButton != null
				: "fx:id=\"showAllButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert snapGrid != null : "fx:id=\"snapGrid\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert ungroupButton != null
				: "fx:id=\"ungroupButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert view3d != null : "fx:id=\"view3d\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert viewControlCubeHolder != null
				: "fx:id=\"viewControlCubeHolder\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert visbilityButton != null
				: "fx:id=\"visbilityButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert workplaneButton != null
				: "fx:id=\"workplaneButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert zoomInButton != null
				: "fx:id=\"zoomInButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert zoomOutButton != null
				: "fx:id=\"zoomOutButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		setUpNavigationCube();
		setUp3dEngine();
		setUpColorPicker();
		
		// Threaded load happens after UI opens
		setupFile();
	}

	private void setupFile() {
		new Thread(() -> {
			try {
				// cadoodle varable set on the first instance of the listener fireing
				ap.loadActive(this);
				ap.save(cadoodle);
				BowlerStudio.runLater(() -> {
					fileNameBox.setText(cadoodle.getProjectName());
				});
				BowlerStudio.runLater(() -> shapeConfiguration.setExpanded(true));

			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	private void displayCurrent() {
		BowlerStudio.runLater(() -> {
			redoButton.setDisable(!cadoodle.isForwardAvailible());
			undoButton.setDisable(!cadoodle.isBackAvailible());

			// for(CSG c:onDisplay) {
			engine.clearUserNode();
			// }
			onDisplay.clear();
			for (CSG c : (List<CSG>) CaDoodleLoader.process(cadoodle)) {
				displayCSG(c);
			}
		});
	}

	private void displayCSG(CSG c) {
		MeshView meshView = c.getMesh();
		if (c.isHole()) {
			Image texture = new Image(getClass().getResourceAsStream("holeTexture.png"));

			meshView = new TexturedCSG(c, texture);
			// addTextureCoordinates(meshView);
			// Create a new PhongMaterial

			// Set opacity for semi-transparency
			meshView.setOpacity(0.75); // Adjust this value between 0.0 and 1.0 as needed
		}
		engine.addUserNode(meshView);
		onDisplay.add(c);
	}

	private void setUpColorPicker() {
		colorPicker.setValue(Color.RED);
		onColorPick(null);
		colorPicker.setOnMousePressed(event -> {
			System.out.println("Set to Solid ");
		});
	}

	private void setUp3dEngine() {
		engine = new BowlerStudio3dEngine();
		engine.rebuild(true);
		engine.hideHand();
		BowlerStudio.runLater(() -> {
			engine.getSubScene().setFocusTraversable(false);
			view3d.widthProperty().addListener((observable, oldValue, newValue) -> {
				engine.getSubScene().setWidth(newValue.doubleValue());
			});

			view3d.heightProperty().addListener((observable, oldValue, newValue) -> {
				engine.getSubScene().setHeight(newValue.doubleValue());
			});
			BowlerStudio.runLater(() -> {
				// Add the 3d environment
				view3d.getChildren().add(engine.getSubScene());
				// anchor it
				AnchorPane.setTopAnchor(engine.getSubScene(), 0.0);
				AnchorPane.setRightAnchor(engine.getSubScene(), 0.0);
				AnchorPane.setLeftAnchor(engine.getSubScene(), 0.0);
				AnchorPane.setBottomAnchor(engine.getSubScene(), 0.0);
			});

		});
		engine.setControlsMap(new IControlsMap() {

			@Override
			public boolean timeToCancel(MouseEvent event) {
				return false;
			}

			@Override
			public boolean isZoom(ScrollEvent t) {
				return ScrollEvent.SCROLL == t.getEventType();
			}

			@Override
			public boolean isSlowMove(MouseEvent event) {
				return false;
			}

			@Override
			public boolean isRotate(MouseEvent me) {
				boolean shiftDown = me.isShiftDown();
				boolean primaryButtonDown = me.isPrimaryButtonDown();
				boolean secondaryButtonDown = me.isSecondaryButtonDown();
				boolean ctrl = me.isControlDown();
				if (ctrl && primaryButtonDown && (!shiftDown))
					return true;
				if ((!shiftDown) && secondaryButtonDown)
					return true;
				return false;
			}

			@Override
			public boolean isMove(MouseEvent me) {
				boolean shiftDown = me.isShiftDown();
				boolean primaryButtonDown = me.isPrimaryButtonDown();
				boolean secondaryButtonDown = me.isSecondaryButtonDown();
				boolean ctrl = me.isControlDown();
				if ((shiftDown) && secondaryButtonDown)
					return true;
				if (ctrl && shiftDown && primaryButtonDown)
					return true;
				return false;
			}
		});
		engine.getFlyingCamera().bind(navigationCube.getFlyingCamera());
		navigationCube.getFlyingCamera().bind(engine.getFlyingCamera());
		onHomeViewButton(null);
	}

	private void setUpNavigationCube() {
		navigationCube = new BowlerStudio3dEngine();
		navigationCube.rebuild(false);
		navigationCube.setZoom(-400);
		navigationCube.lockZoom();
		navigationCube.setMouseScale(10);
		BowlerStudio.runLater(() -> {
			navigationCube.getSubScene().setFocusTraversable(false);
			viewControlCubeHolder.widthProperty().addListener((observable, oldValue, newValue) -> {
				navigationCube.getSubScene().setWidth(newValue.doubleValue());
			});

			viewControlCubeHolder.heightProperty().addListener((observable, oldValue, newValue) -> {
				navigationCube.getSubScene().setHeight(newValue.doubleValue());
			});
			BowlerStudio.runLater(() -> {
				viewControlCubeHolder.getChildren().add(navigationCube.getSubScene());
				AnchorPane.setTopAnchor(navigationCube.getSubScene(), 0.0);
				AnchorPane.setRightAnchor(navigationCube.getSubScene(), 0.0);
				AnchorPane.setLeftAnchor(navigationCube.getSubScene(), 0.0);
				AnchorPane.setBottomAnchor(navigationCube.getSubScene(), 0.0);
			});

		});
		ViewCube viewcube = new ViewCube();
		MeshView viewCubeMesh = viewcube.createTexturedCube(navigationCube);
		navigationCube.addUserNode(viewCubeMesh);

	}

	@Override
	public void onUpdate(List<CSG> currentState, ICaDoodleOpperation source, CaDoodleFile f) {
		if (cadoodle == null)
			cadoodle = f;
		System.out.println("Displaying result of " + source.getType());
		displayCurrent();
	}

	private void save() {
		new Thread(() -> ap.save(cadoodle));
	}
}
