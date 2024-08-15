/**
 * Sample Skeleton for 'MainWindow.fxml' Controller Class
 */

package com.commonwealthrobotics;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
import com.neuronrobotics.bowlerstudio.threed.ICameraChangeListener;
import com.neuronrobotics.bowlerstudio.threed.IControlsMap;
import com.neuronrobotics.bowlerstudio.threed.VirtualCameraMobileBase;
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
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

public class MainController implements ICaDoodleStateUpdate, ICameraChangeListener {
	private static final int ZOOM = -1000;
	private CaDoodleFile cadoodle;
	private boolean drawerOpen = true;
	private SelectionSession session = new SelectionSession();
	private ShapesPallet pallet;
	/**
	 * CaDoodle Model Classes
	 */
	private BowlerStudio3dEngine navigationCube;
	private BowlerStudio3dEngine engine;

	

    @FXML // fx:id="stackPane"
    private StackPane stackPane; // Value injected by FXMLLoader
	@FXML // fx:id="allignButton"
	private Button allignButton; // Value injected by FXMLLoader
	@FXML // ResourceBundle that was given to the FXMLLoader
	private ResourceBundle resources;
	@FXML // fx:id="lockImage"
	private ImageView lockImage; // Value injected by FXMLLoader
	@FXML // fx:id="showHideImage"
	private ImageView showHideImage; // Value injected by FXMLLoader
	@FXML // fx:id="showHideImage"
	private ImageView showAllImage;

	@FXML // URL location of the FXML file that was given to the FXMLLoader
	private URL location;

	@FXML // fx:id="anchorPanForConfiguration"
	private AnchorPane anchorPanForConfiguration; // Value injected by FXMLLoader

	@FXML // fx:id="buttonGrid"
	private GridPane buttonGrid; // Value injected by FXMLLoader

	@FXML // fx:id="colorPicker"
	private ColorPicker colorPicker; // Value injected by FXMLLoader

	@FXML // fx:id="configurationGrid"
	private GridPane configurationGrid; // Value injected by FXMLLoader

	@FXML // fx:id="controlBar"
	private GridPane controlBar; // Value injected by FXMLLoader

	@FXML // fx:id="copyButton"
	private Button copyButton; // Value injected by FXMLLoader

	@FXML // fx:id="cruseButton"
	private Button cruseButton; // Value injected by FXMLLoader

	@FXML // fx:id="deleteButton"
	private Button deleteButton; // Value injected by FXMLLoader

	@FXML // fx:id="drawerArea"
	private AnchorPane drawerArea; // Value injected by FXMLLoader

	@FXML // fx:id="drawerButton"
	private Button drawerButton; // Value injected by FXMLLoader

	@FXML // fx:id="drawerGrid"
	private GridPane drawerGrid; // Value injected by FXMLLoader

	@FXML // fx:id="drawerHolder"
	private HBox drawerHolder; // Value injected by FXMLLoader
	@FXML // fx:id="drawerHolder"
	private HBox buttonBar;
	@FXML // fx:id="drawrImage"
	private ImageView drawrImage; // Value injected by FXMLLoader

	@FXML // fx:id="export"
	private Button export; // Value injected by FXMLLoader

	@FXML // fx:id="fileNameBox"
	private TextField fileNameBox; // Value injected by FXMLLoader

	@FXML // fx:id="fitViewButton"
	private Button fitViewButton; // Value injected by FXMLLoader

	@FXML // fx:id="groupButton"
	private Button groupButton; // Value injected by FXMLLoader

	@FXML // fx:id="hideSHow"
	private Button hideSHow; // Value injected by FXMLLoader

	@FXML // fx:id="holeButton"
	private Button holeButton; // Value injected by FXMLLoader

	@FXML // fx:id="homeButton"
	private Button homeButton; // Value injected by FXMLLoader

	@FXML // fx:id="homeViewButton"
	private Button homeViewButton; // Value injected by FXMLLoader

	@FXML // fx:id="importButton"
	private Button importButton; // Value injected by FXMLLoader

	@FXML // fx:id="lockButton"
	private Button lockButton; // Value injected by FXMLLoader

	@FXML // fx:id="lockUnlockTooltip"
	private Tooltip lockUnlockTooltip; // Value injected by FXMLLoader

	@FXML // fx:id="mirronButton"
	private Button mirronButton; // Value injected by FXMLLoader

	@FXML // fx:id="model"
	private Button model; // Value injected by FXMLLoader

	@FXML // fx:id="notesButton"
	private Button notesButton; // Value injected by FXMLLoader

	@FXML // fx:id="objectPallet"
	private GridPane objectPallet; // Value injected by FXMLLoader

	@FXML // fx:id="pasteButton"
	private Button pasteButton; // Value injected by FXMLLoader

	@FXML // fx:id="physicsButton"
	private Button physicsButton; // Value injected by FXMLLoader

	@FXML // fx:id="redoButton"
	private Button redoButton; // Value injected by FXMLLoader

	@FXML // fx:id="rulerButton"
	private Button rulerButton; // Value injected by FXMLLoader

	@FXML // fx:id="settingsButton"
	private Button settingsButton; // Value injected by FXMLLoader

	@FXML // fx:id="shapeCatagory"
	private ComboBox<String> shapeCatagory; // Value injected by FXMLLoader

	@FXML // fx:id="shapeConfiguration"
	private TitledPane shapeConfiguration; // Value injected by FXMLLoader

	@FXML // fx:id="shapeConfigurationBox"
	private Accordion shapeConfigurationBox; // Value injected by FXMLLoader

	@FXML // fx:id="shapeConfigurationHolder"
	private AnchorPane shapeConfigurationHolder; // Value injected by FXMLLoader

	@FXML // fx:id="showAllButton"
	private Button showAllButton; // Value injected by FXMLLoader

	@FXML // fx:id="snapGrid"
	private ComboBox<String> snapGrid; // Value injected by FXMLLoader

	@FXML // fx:id="topBar"
	private GridPane topBar; // Value injected by FXMLLoader

	@FXML // fx:id="totalApplicationBackground"
	private AnchorPane totalApplicationBackground; // Value injected by FXMLLoader

	@FXML // fx:id="undoButton"
	private Button undoButton; // Value injected by FXMLLoader

	@FXML // fx:id="ungroupButton"
	private Button ungroupButton; // Value injected by FXMLLoader

	@FXML // fx:id="view3d"
	private AnchorPane view3d; // Value injected by FXMLLoader
	@FXML // fx:id="view3d"
	private AnchorPane layerHolder;
	@FXML // fx:id="viewControlCubeHolder"
	private AnchorPane viewControlCubeHolder; // Value injected by FXMLLoader

	@FXML // fx:id="visbilityButton"
	private MenuButton visbilityButton; // Value injected by FXMLLoader

	@FXML // fx:id="workplaneButton"
	private Button workplaneButton; // Value injected by FXMLLoader

	@FXML // fx:id="zoomInButton"
	private Button zoomInButton; // Value injected by FXMLLoader

	@FXML // fx:id="zoomOutButton"
	private Button zoomOutButton; // Value injected by FXMLLoader
	private ICaDoodleOpperation source;
	private boolean resetArmed;
	private long timeOfClick;
	private ImageView ground;


	@FXML
	void onAllign(ActionEvent event) {
		System.out.println("On Allign Clicked");
		session.onAllign();
		session.setKeyBindingFocus();
	}

	@FXML
	void onRedo(ActionEvent event) {
		System.out.println("On Redo");
		cadoodle.forward();
		session.setKeyBindingFocus();
	}

	@FXML
	void onUndo(ActionEvent event) {
		System.out.println("On Undo");
		cadoodle.back();
		session.setKeyBindingFocus();
	}

	@FXML
	void onPaste(ActionEvent event) {
		System.out.println("On Paste");
		session.onPaste();
		session.setKeyBindingFocus();
	}

	@FXML
	void onCopy(ActionEvent event) {
		System.out.println("On copy");
		session.onCopy();
		session.setKeyBindingFocus();
	}

	@FXML
	void onDelete(ActionEvent event) {
		System.out.println("On Delete");
		session.onDelete();
		session.setKeyBindingFocus();
	}

	@FXML
	void onColorPick(ActionEvent event) {

		Color value = colorPicker.getValue();

		session.setColor(value);
		session.setKeyBindingFocus();
	}

	@FXML
	void onCruse(ActionEvent event) {
		System.out.println("On Cruse");
		session.conCruse();
		session.setKeyBindingFocus();
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
		session.setKeyBindingFocus();
	}

	@FXML
	void onExport(ActionEvent event) {
		System.out.println("On Export");
		session.setKeyBindingFocus();
	}

	@FXML
	void onFitView(ActionEvent event) {
		engine.focusOrentation(new TransformNR(0, 0, 0, new RotationNR(0, 15, -45)), session.getFocusCenter(),
				engine.getFlyingCamera().getZoomDepth());
		session.setKeyBindingFocus();
	}

	@FXML
	void onGroup(ActionEvent event) {
		System.out.println("On Group");
		session.onGroup();
		session.setKeyBindingFocus();
	}

	@FXML
	void onHideConnections(ActionEvent event) {
		System.out.println(" on Hide Physics Connections");
		session.setKeyBindingFocus();
	}

	@FXML
	void onHideNotes(ActionEvent event) {
		System.out.println("On Hide Notes ");
		session.setKeyBindingFocus();
	}

	@FXML
	void onHideShow(ActionEvent event) {
		System.out.println("On Hide Show");
		session.onHideShowOpperation();

		session.setKeyBindingFocus();
	}

	@FXML
	void onHoleButton(ActionEvent event) {
		System.out.println("Set to Hole ");
		session.setToHole();
		session.setKeyBindingFocus();
	}

	@FXML
	void onHome(ActionEvent event) {
		System.out.println("Open the Project Select UI");
		session.setKeyBindingFocus();
	}

	@FXML
	void onHomeViewButton(ActionEvent event) {
		engine.focusOrentation(new TransformNR(0, 0, 0, new RotationNR(0, 15, -45)), new TransformNR(0, 0, 0), ZOOM);
		session.setKeyBindingFocus();
	}

	@FXML
	void onImport(ActionEvent event) {
		System.out.println("On Import");
		session.setKeyBindingFocus();
	}

	@FXML
	void onLock(ActionEvent event) {
		System.out.println("On Lock Selected");
		session.onLock();
		session.setKeyBindingFocus();
	}

	@FXML
	void onMirron(ActionEvent event) {
		System.out.println("On Mirron Object");
		session.setKeyBindingFocus();
	}

	@FXML
	void onModeling(ActionEvent event) {
		System.out.println("Select Modeling View");
		session.setKeyBindingFocus();
	}

	@FXML
	void onNotesClick(ActionEvent event) {
		System.out.println("On Notes");
		session.setKeyBindingFocus();
	}

	@FXML
	void onPhysics(ActionEvent event) {
		System.out.println("On Physics Mode Selected");
		session.setKeyBindingFocus();
	}

	@FXML
	void onRuler(ActionEvent event) {
		System.out.println("On Add Ruler");
		session.setKeyBindingFocus();
	}

	@FXML
	void onSetCatagory(ActionEvent event) {
		System.out.println("On Set Catagory, re-lod object pallet");
		pallet.onSetCatagory();
		session.setKeyBindingFocus();
	}

	@FXML
	void onSettings(ActionEvent event) {
		System.out.println("On Settings");
		session.setKeyBindingFocus();
	}

	@FXML
	void onShowHidden(ActionEvent event) {
		System.out.println("On Show Hidden");
		session.showAll();
		session.setKeyBindingFocus();
	}

	@FXML
	void onUngroup(ActionEvent event) {
		System.out.println("On Ungroup");

		session.onUngroup();
		session.setKeyBindingFocus();
	}

	@FXML
	void onVisibility(ActionEvent event) {
		System.out.println("On Visibility Menu opening");
		session.setKeyBindingFocus();
	}

	@FXML
	void onWOrkplane(ActionEvent event) {
		System.out.println("On Set Workplane");
		session.setKeyBindingFocus();
	}

	@FXML
	void onZoomIn(ActionEvent event) {

		System.out.println("Zoom In");
		engine.setZoom((int) engine.getFlyingCamera().getZoomDepth() + 40);
		session.setKeyBindingFocus();
	}

	@FXML
	void onZoomOut(ActionEvent event) {
		System.out.println("Zoom Out");

		engine.setZoom((int) engine.getFlyingCamera().getZoomDepth() - 40);
		session.setKeyBindingFocus();
	}

	@FXML
	void setName(ActionEvent event) {
		System.out.println("Set Project Name to " + fileNameBox.getText());
		cadoodle.setProjectName(fileNameBox.getText());
		session.setKeyBindingFocus();
		session.save();
	}

	@FXML
	void showAll(ActionEvent event) {
		System.out.println("On SHow All");
		session.showAll();
		session.setKeyBindingFocus();
	}

	@FXML // This method is called by the FXMLLoader when initialization is complete
	void initialize() {
		assert anchorPanForConfiguration != null
				: "fx:id=\"anchorPanForConfiguration\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert buttonGrid != null : "fx:id=\"buttonGrid\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert colorPicker != null : "fx:id=\"colorPicker\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert configurationGrid != null
				: "fx:id=\"configurationGrid\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert controlBar != null : "fx:id=\"controlBar\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert copyButton != null : "fx:id=\"copyButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert cruseButton != null : "fx:id=\"cruseButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert deleteButton != null
				: "fx:id=\"deleteButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert drawerArea != null : "fx:id=\"drawerArea\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert drawerButton != null
				: "fx:id=\"drawerButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert drawerGrid != null : "fx:id=\"drawerGrid\" was not injected: check your FXML file 'MainWindow.fxml'.";
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
		assert notesButton != null : "fx:id=\"notesButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert objectPallet != null
				: "fx:id=\"objectPallet\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert pasteButton != null : "fx:id=\"pasteButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert physicsButton != null
				: "fx:id=\"physicsButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert redoButton != null : "fx:id=\"redoButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
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
		assert topBar != null : "fx:id=\"topBar\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert totalApplicationBackground != null
				: "fx:id=\"totalApplicationBackground\" was not injected: check your FXML file 'MainWindow.fxml'.";
		assert undoButton != null : "fx:id=\"undoButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
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

		session.set(shapeConfiguration, shapeConfigurationBox, shapeConfigurationHolder, configurationGrid, null,
				engine, colorPicker, snapGrid);
		session.setButtons(copyButton, deleteButton, pasteButton, hideSHow, mirronButton, cruseButton);
		session.setGroup(groupButton);
		session.setUngroup(ungroupButton);
		session.setShowHideImage(showHideImage);
		session.setAllignButton(allignButton);
		// do this after setting up the session
		setupEngineControls();
		// Threaded load happens after UI opens
		setupFile();
		
		pallet=new ShapesPallet(shapeCatagory,objectPallet,session);
	}

	private void setupFile() {
		new Thread(() -> {
			try {
				// cadoodle varable set on the first instance of the listener fireing
				session.loadActive(this);
				session.save();
				BowlerStudio.runLater(() -> {
					fileNameBox.setText(cadoodle.getProjectName());
				});
				BowlerStudio.runLater(() -> shapeConfiguration.setExpanded(true));
				session.setKeyBindingFocus();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	private void setUpColorPicker() {
		colorPicker.setOnMousePressed(event -> {
			System.out.println("Set to Solid ");
			session.setToSolid();
		});

	}

	private void setUp3dEngine() {
		engine = new BowlerStudio3dEngine("CAD window");
		engine.rebuild(true);
		engine.hideHand();
		BowlerStudio.runLater(() -> {
			engine.getSubScene().setFocusTraversable(false);
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
		engine.addListener(this);

		layerHolder.widthProperty().addListener((observable, oldValue, newValue) -> {
			engine.getSubScene().setWidth(newValue.doubleValue());
			onChange(engine.getFlyingCamera());
		});

		layerHolder.heightProperty().addListener((observable, oldValue, newValue) -> {
			engine.getSubScene().setHeight(newValue.doubleValue());
			onChange(engine.getFlyingCamera());
		});
		Image image = new Image(Main.class.getResourceAsStream("BlueNoise470.png"));
		ground = new ImageView(image);
		ground.setOpacity(0.25);
		ground.setScaleX(2);
		ground.setScaleY(2);
		ground.setX(-image.getWidth()/2);
		ground.setY(-image.getHeight()/2);
		ground.setTranslateZ(-0.1);
		
		engine.addUserNode(ground);
		
	}

	private void setUpNavigationCube() {
		navigationCube = new BowlerStudio3dEngine("NaviCube");
		navigationCube.rebuild(false);
		navigationCube.setZoom(-400);
		navigationCube.lockZoom();
		navigationCube.lockMove();
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
		if (cadoodle == null) {
			cadoodle = f;
			pallet.setCadoodle(f);
		}
		System.out.println("Displaying result of " + source.getType());
		BowlerStudio.runLater(() -> {
			redoButton.setDisable(!cadoodle.isForwardAvailible());
			undoButton.setDisable(!cadoodle.isBackAvailible());
		});
		session.onUpdate(currentState, source, f);
		if (session.isAnyHidden()) {
			BowlerStudio.runLater(() -> {
				showAllImage.setImage(new Image(MainController.class.getResourceAsStream("litBulb.png")));
				showAllButton.setDisable(false);
			});
		} else {
			BowlerStudio.runLater(() -> {
				showAllImage.setImage(new Image(MainController.class.getResourceAsStream("darkBulb.png")));
				showAllButton.setDisable(true);
			});
		}
		if(this.source!=source) {
			session.save();
		}
		this.source = source;
		BowlerStudio.runLater(()->{
			onChange(engine.getFlyingCamera());
				
		});
	}



	private void setupEngineControls() {
		
		engine.getSubScene().setOnMousePressed(event -> {
			resetArmed=true;
			timeOfClick = System.currentTimeMillis();
		});
		engine.getSubScene().addEventFilter(MouseEvent.DRAG_DETECTED, event->{
			resetArmed=false;
		});
		engine.getSubScene().addEventFilter(MouseEvent.MOUSE_RELEASED, event->{
			if(!resetArmed)
				return;
			resetArmed=false;
			if(System.currentTimeMillis()-timeOfClick>200)
				return;
			session.clearSelection();
		});
		
		session.setKeyBindingFocus();
		SubScene subScene = engine.getSubScene();

		subScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.LEFT
					|| event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.TAB) {
				switch(event.getCode()) {
				case UP:
					if(event.isControlDown()) {
						session.moveInCameraFrame(new TransformNR(0,0,1));
					}else
						session.moveInCameraFrame(new TransformNR(1,0,0));
					break;
				case DOWN:
					if(event.isControlDown()) {
						session.moveInCameraFrame(new TransformNR(0,0,-1));
					}else
						session.moveInCameraFrame(new TransformNR(-1,0,0));
					break;
				case LEFT:
					session.moveInCameraFrame(new TransformNR(0,1,0));
					break;
				case RIGHT:
					session.moveInCameraFrame(new TransformNR(0,-1,0));
					break;
				}
				// System.out.println("Arrows " + event.getCode());
				// Consume the event to prevent default focus traversal
				event.consume();
			}
			if (event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.DELETE) {
				session.onDelete();
				// Handle the backspace or delete key press
				event.consume(); // Prevents the event from being processed further
			}
		});
		subScene.setOnKeyTyped(event -> {
			String character = event.getCharacter();

			// You can still use the key code for non-character keys
			// System.out.println("Key code: " + event.getCode());
			if (event.isControlDown()) {
				// System.out.println("CTRL + ");
				switch ((int) character.charAt(0)) {
				case 26:
					System.out.println("Undo");
					cadoodle.back();
					break;
				case 25:
					System.out.println("redo");
					cadoodle.forward();
					break;
				case 7:
					System.out.println("Group");
					session.onGroup();
					break;
				case 71:
					if (event.isShiftDown()) {
						System.out.println("Un-Group");
						session.onUngroup();
					}
					break;
				case 1:
					System.out.println("Select All");
					session.selectAll();
					break;
				case 3:
					session.onCopy();
					break;
				case 22:
					session.onPaste();
					break;
				case 4:
					session.Duplicate();
					break;
				case 8:
					session.onHideShowOpperation();
					break;
				case 72:
					session.showAll();
					break;
				default:
				    if (!character.isEmpty()) {
				        char rawChar = character.charAt(0);			        
				        System.out.println("CTRL+ Raw char value: " + (int) rawChar);
				    } else {
				        System.out.println("No character data available (probably a non-character key)");
				    }
					break;
				}
			} else {
				switch ((int) character.charAt(0)) {
				case 100:
					session.dropToWorkplane();
					break;
				default:
					if (!character.isEmpty()) {
						char rawChar = character.charAt(0);
						System.out.println("Raw char value: " + (int) rawChar);
					} else {
						System.out.println("No character data available (probably a non-character key)");
					}
					break;
				}
			}
		});
	}

	@Override
	public void onChange(VirtualCameraMobileBase camera) {
		double zoom = camera.getZoomDepth();
		double az = camera.getPanAngle();
		double el = camera.getTiltAngle();
		//System.out.println("Elevation "+el);
		if(el<-90 ||el>90) {
			ground.setVisible(false);
		}else {
			ground.setVisible(true);
		}
		double x= camera.getGlobalX();
		double y= camera.getGlobalY();
		double z= camera.getGlobalZ();
		double screenW = engine.getSubScene().getWidth();
		double screenH = engine.getSubScene().getHeight();
		session.onCameraChange(screenW,screenH,zoom,az,el,x,y,z);
	}
}
