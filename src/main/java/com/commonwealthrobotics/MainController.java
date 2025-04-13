/**
 * Sample Skeleton for 'MainWindow.fxml' Controller Class
 */

package com.commonwealthrobotics;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.sshd.common.util.OsUtils;

import com.commonwealthrobotics.controls.SelectionSession;
import com.commonwealthrobotics.controls.SpriteDisplayMode;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.SplashManager;
import com.neuronrobotics.bowlerstudio.scripting.BlenderLoader;
import com.neuronrobotics.bowlerstudio.scripting.CaDoodleLoader;
import com.neuronrobotics.bowlerstudio.scripting.FreecadLoader;
import com.neuronrobotics.bowlerstudio.scripting.GroovyHelper;
import com.neuronrobotics.bowlerstudio.scripting.OpenSCADLoader;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.StlLoader;
import com.neuronrobotics.bowlerstudio.scripting.SvgLoader;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AddFromFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleOpperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.ICameraChangeListener;
import com.neuronrobotics.bowlerstudio.threed.IControlsMap;
import com.neuronrobotics.bowlerstudio.threed.VirtualCameraMobileBase;
import com.neuronrobotics.nrconsole.util.FileSelectionFactory;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
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
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.stage.FileChooser.ExtensionFilter;

public class MainController implements ICaDoodleStateUpdate, ICameraChangeListener {
	private static final int ZOOM = -700;
	// private CaDoodleFile cadoodle;
	private boolean drawerOpen = true;
	private SelectionSession session = null;
	private WorkplaneManager workplane;
	private ShapesPallet pallet;
	private ActiveProject activeProject = new ActiveProject();
	private SelectionBox selectionBox = null;
	private RulerManager ruler = new RulerManager(activeProject);

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

	@FXML
	private VBox parametrics;

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
	@FXML
	private AnchorPane AdvancedBooleanOpsMenuHolder;
	@FXML
	private MenuButton advancedGroupMenu;
	
	
	
	
	private ICaDoodleOpperation source;
	private boolean resetArmed;
	private long timeOfClick;
	private MeshView ground;
	private int lastFrame = 0;
	private File currentFile = null;
	


	@FXML
	void onAllign(ActionEvent event) {

		session.onAllign();
		session.setKeyBindingFocus();
	}

	@FXML
	void onRedo(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Redo");
		activeProject.get().forward();
		session.setKeyBindingFocus();
	}

	@FXML
	void onUndo(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Undo");
		new Thread(() -> {
			activeProject.get().back();
			session.setKeyBindingFocus();
		}).start();
	}

	@FXML
	void onPaste(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Paste");
		session.onPaste();
		session.setKeyBindingFocus();
	}

	@FXML
	void onCopy(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On copy");
		session.setCopyListToCurrentSelected();
		session.setKeyBindingFocus();
	}

	@FXML
	void onDelete(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Delete");
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
		session.onCruse();
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
		com.neuronrobotics.sdk.common.Log.error("On Export");
		Runnable onFinish = () -> {
			session.setKeyBindingFocus();
			com.neuronrobotics.sdk.common.Log.error("ExportManager Close");
		};
		Runnable onClear = () -> {
			session.clearScreen();
			session.clearSelection();
		};
		ExportManager.launch(session, activeProject, onFinish, onClear);
		session.setKeyBindingFocus();
	}

	@FXML
	void onFitView(ActionEvent event) {
		engine.focusOrentation(null, session.getFocusCenter(),
				engine.getFlyingCamera().getZoomDepth());
		session.setKeyBindingFocus();
	}
//onXorOpperation
	@FXML
	void onXorOpperation(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Xor");
		session.onXor();
		session.setKeyBindingFocus();
	}
	@FXML
	void onGroup(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Group");
		session.onGroup(false,false);
		session.setKeyBindingFocus();
	}
	@FXML
	void onHullOpperation(ActionEvent e) {
		com.neuronrobotics.sdk.common.Log.error("On Hull");
		session.onGroup(true,false);
		session.setKeyBindingFocus();
	}
	@FXML
	void onIntersectOpperation(ActionEvent e) {
		com.neuronrobotics.sdk.common.Log.error("On Intersect");
		session.onGroup(false,true);
		session.setKeyBindingFocus();
	}
	
	@FXML
	void onHideConnections(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error(" on Hide Physics Connections");
		session.setKeyBindingFocus();
	}

	@FXML
	void onHideNotes(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Hide Notes ");
		session.setKeyBindingFocus();
	}

	@FXML
	void onHideShow(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Hide Show");
		session.onHideShowOpperation();

		session.setKeyBindingFocus();
	}

	@FXML
	void onHoleButton(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("Set to Hole ");
		session.setToHole();
		session.setKeyBindingFocus();
	}

	@FXML
	void onHome(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("Open the Project Select UI");
		// session.setKeyBindingFocus();
		homeButton.setDisable(true);
		Runnable onFinish = () -> {
			session.setKeyBindingFocus();
			com.neuronrobotics.sdk.common.Log.error("ProjectManager Close");
			BowlerStudio.runLater(() -> homeButton.setDisable(false));
		};
		Runnable onClear = () -> {
			session.clearScreen();
			session.clearSelection();
		};
		ProjectManager.launch(activeProject, onFinish, onClear);
	}

	@FXML
	void onHomeViewButton(ActionEvent event) {
		engine.focusOrentation(new TransformNR(0, 0, 0, new RotationNR(0, 15, -45)), new TransformNR(0, 0, 0), ZOOM);
		session.setKeyBindingFocus();
	}

	@FXML
	void onImport(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Import");
		new Thread(() -> {

			ArrayList<String> extentions = new ArrayList<>();
			// extentions.add("*");
			for (String s : new StlLoader().getFileExtenetion())
				extentions.add("*." + s);
			for (String s : new SvgLoader().getFileExtenetion())
				extentions.add("*." + s);
			for (String s : new GroovyHelper().getFileExtenetion())
				extentions.add("*." + s);
			for (String s : new BlenderLoader().getFileExtenetion())
				extentions.add("*." + s);
			for (String s : new FreecadLoader().getFileExtenetion())
				extentions.add("*." + s);
			for (String s : new OpenSCADLoader().getFileExtenetion())
				extentions.add("*." + s);
			for (String s : new CaDoodleLoader().getFileExtenetion())
				extentions.add("*." + s);

			ExtensionFilter stl = new ExtensionFilter("CaDoodle Compatible", extentions);
			if (currentFile == null)
				currentFile = new File(System.getProperty("user.home") + "/Desktop/");
			File last = FileSelectionFactory.GetFile(currentFile, false, stl);
			if (last != null) {
				currentFile = last;
				com.neuronrobotics.sdk.common.Log.error("Adding file " + last);
				AddFromFile toAdd = new AddFromFile().set(last);
				session.addOp(toAdd);
			}
			session.setKeyBindingFocus();
		}).start();
	}

	@FXML
	void onLock(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Lock Selected");
		session.lockToggle();
		session.setKeyBindingFocus();
	}

	@FXML
	void onMirror(ActionEvent event) {
		session.onMirror();
		session.setKeyBindingFocus();
	}

	@FXML
	void onModeling(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("Select Modeling View");
		session.setKeyBindingFocus();
	}

	@FXML
	void onNotesClick(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Notes");
		session.setKeyBindingFocus();
	}

	@FXML
	void onPhysics(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Physics Mode Selected");
		session.setKeyBindingFocus();
	}

	@FXML
	void onRuler(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Add Ruler");
		ruler.setActive(true);
		session.setMode(SpriteDisplayMode.PLACING);
		ruler.startPick(()->{
			if(session.selectedSnapshot().size()>0)
				session.setMode(SpriteDisplayMode.Default);
			else
				session.setMode(SpriteDisplayMode.Clear);
			session.updateControls();
		});
		session.setKeyBindingFocus();
	}

	@FXML
	void onSetCatagory(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Set Catagory, re-lod object pallet");
		pallet.onSetCatagory();
		session.setKeyBindingFocus();
	}

	@FXML
	void onSettings(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Settings");
		session.setKeyBindingFocus();
	}

	@FXML
	void onShowHidden(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Show Hidden");
		session.showAll();
		session.setKeyBindingFocus();
	}

	@FXML
	void onUngroup(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Ungroup");

		session.onUngroup();
		session.setKeyBindingFocus();
	}

	@FXML
	void onVisibility(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On Visibility Menu opening");
		session.setKeyBindingFocus();
	}

	@FXML
	void onWOrkplane(ActionEvent event) {
		session.setMode(SpriteDisplayMode.PLACING);
		workplane.pickPlane(() -> {
			ruler.cancle();
			session.save();
			session.setMode(SpriteDisplayMode.Default);
			session.updateControls();
		}, ()->{
			session.setMode(SpriteDisplayMode.Default);
			session.updateControls();
		},
		ruler);
		session.setKeyBindingFocus();
	}

	@FXML
	void onZoomIn(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("Zoom In");
		engine.setZoom((int) engine.getFlyingCamera().getZoomDepth() + 40);
		session.setKeyBindingFocus();
	}

	@FXML
	void onZoomOut(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("Zoom Out");

		engine.setZoom((int) engine.getFlyingCamera().getZoomDepth() - 40);
		session.setKeyBindingFocus();
	}

	@FXML
	void setName(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("Set Project Name to " + fileNameBox.getText());
		activeProject.get().setProjectName(fileNameBox.getText());
		session.setKeyBindingFocus();
		session.save();
	}

	@FXML
	void showAll(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.error("On SHow All");
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
		
		engine = new BowlerStudio3dEngine("CAD window");
		engine.rebuild(true);
		activeProject.addListener(this);
		session = new SelectionSession(engine, activeProject,ruler);
		selectionBox = new SelectionBox(session, view3d, engine, activeProject);
		try {
			activeProject.loadActive();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(2);
		}
		setUpNavigationCube();
		setUp3dEngine();
		setUpColorPicker();
		
		session.set(shapeConfiguration, shapeConfigurationBox, shapeConfigurationHolder, configurationGrid, null,
				engine, colorPicker, snapGrid, parametrics, lockButton, lockImage,advancedGroupMenu);
		session.setButtons(copyButton, deleteButton, pasteButton, hideSHow, mirronButton, cruseButton);
		session.setGroup(groupButton);
		session.setUngroup(ungroupButton);
		session.setShowHideImage(showHideImage);

		session.setAllignButton(allignButton);
		// do this after setting up the session
		setupEngineControls();
		try {
			setCadoodleFile();
			// Threaded load happens after UI opens
			setupFile();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		fileNameBox.setOnKeyTyped(ev -> {
			com.neuronrobotics.sdk.common.Log.error("Set Project Name to " + fileNameBox.getText());
			activeProject.get().setProjectName(fileNameBox.getText());
			session.save();
		});
		setupCSGEngine();
		SplashManager.setClosePreventer(() -> activeProject.get().getPercentInitialized() < 0.99);
	}

	private void setupCSGEngine() {
		CSG.setPreventNonManifoldTriangles(false);
		CSG.setProgressMoniter((currentIndex, finalIndex, type, intermediateShape) -> {
			int i = currentIndex + 1;
			double percent = ((double) i) / ((double) finalIndex) * 100;
			String name = "";
			if(intermediateShape!=null)
				name=intermediateShape.getName();
			String x = name + " " + type.trim() + " " + String.format("%.1f", percent)
					+ "% finished : " + i + " of " + finalIndex;
			System.out.println("MainController.setupCSGEngine():: "+x);
			if (isInitializing())
				return;
			try {
				if (finalIndex > 50) {
					if (percent > 95) {
						SplashManager.closeSplash();
					} else {
						SplashManager.renderSplashFrame((int) percent, x);
					}
				} else {
					SplashManager.closeSplash();
				}

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
	}

	public void loadActive(MainController mainController) throws Exception {

	}

	private void setupFile() {
		new Thread(() -> {
//			try {
//				Thread.sleep(200);
//			} catch (InterruptedException e) {
//				// Auto-generated catch block
//				e.printStackTrace();
//			}
			try {
				// cadoodle varable set on the first instance of the listener fireing
				SplashManager.renderSplashFrame(20, "Initialize Model");
				while (!SplashManager.isVisableSplash()) {
					Thread.sleep(100);
				}
				activeProject.get().initialize();
				session.save();
				BowlerStudio.runLater(() -> shapeConfiguration.setExpanded(true));
				while (SplashManager.isVisableSplash()) {
					SplashManager.closeSplash();
					Thread.sleep(500);
				}
				session.setKeyBindingFocus();
				BowlerStudio.runLater(() -> cancel());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();

	}

	private void setUpColorPicker() {
		colorPicker.setOnMousePressed(event -> {
			com.neuronrobotics.sdk.common.Log.error("Set to Solid ");
			session.setToSolid();
		});

	}

	private void setUp3dEngine() {
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
				boolean middle = me.isMiddleButtonDown();
 				boolean ctrl = me.isControlDown();
 				if(middle)
 					return true;
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
		createGroundPlane();
	}

	private void createGroundPlane() {
		ground = new Cube(1000, 1000, 0.001).toCSG().toZMax().newMesh();
		PhongMaterial material = new PhongMaterial();
		material.setDiffuseColor(new Color(0, 0, 0.25, 0.0025));
		ground.setCullFace(CullFace.BACK);
		ground.setMaterial(material);
		ground.setOpacity(0.25);
		Group linesGroupp = new Group();
		linesGroupp.setDepthTest(DepthTest.ENABLE);
		linesGroupp.setViewOrder(1); // Lower viewOrder renders on top
		linesGroupp.getChildren().add(ground);
		engine.addUserNode(linesGroupp);
		// rulerGroup.getTransforms().add(workplane.getWorkplaneLocation());
		ruler.initialize(engine.getRulerGroup(), 
				engine.getRulerInWorkplaneOffset(),
				engine.getRulerOffset(),()->{
					session.updateControls();
				});

	}

	public static double groundScale() {
		return 1;
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
	public void onUpdate(List<CSG> currentState, ICaDoodleOpperation source, CaDoodleFile fi) {
		if (isInitializing()) {
			int frame = (int) (100 * activeProject.get().getPercentInitialized());
			if (frame - lastFrame > 5) {
				lastFrame = frame;
				SplashManager.renderSplashFrame(frame, "Initialize Model");
			}
		}
		// com.neuronrobotics.sdk.common.Log.error("Displaying result of " +
		// source.getType());
		BowlerStudio.runLater(() -> {
			redoButton.setDisable(!activeProject.get().isForwardAvailible());
			undoButton.setDisable(!activeProject.get().isBackAvailible());
		});
		session.onUpdate(currentState, source, fi);
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
//		if (this.source != source) {
//			session.save();
//		}
		this.source = source;
		BowlerStudio.runLater(() -> {
			onChange(engine.getFlyingCamera());

		});
	}

	private boolean isInitializing() {
		return activeProject.get().getPercentInitialized() < 0.9;
	}

	private void setCadoodleFile() {

		workplane = new WorkplaneManager(activeProject, ground, engine, session);
		ruler.setWorkplane(workplane);
		ruler.setWP(activeProject.get().getWorkplane());
		session.setWorkplaneManager(workplane);
		pallet = new ShapesPallet(shapeCatagory, objectPallet, session, activeProject, workplane);
		workplane.placeWorkplaneVisualization();
		selectionBox.setWorkplaneManager(workplane);
		
	}

	private void setupEngineControls() {

		selectionBox.setPressEvent(event -> {
			resetArmed = true;
			timeOfClick = System.currentTimeMillis();
			if (isEventACancel(event)) {
				cancel();
			}

			// System.out.println("Releses MainController");
		});

//		engine.getSubScene().addEventFilter(MouseEvent.MOUSE_PRESSED,event->{
//			if(event.isPrimaryButtonDown())
//				sb.activate(event);
//		});

		session.setKeyBindingFocus();
		SubScene subScene = engine.getSubScene();

		subScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (session.isFocused()) {
				// com.neuronrobotics.sdk.common.Log.error("Key ignonred, session in focus");
				return;
			}

			if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.LEFT
					|| event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.TAB) {
				switch (event.getCode()) {
				case UP:
					if (event.isControlDown()) {
						session.moveInCameraFrame(new TransformNR(0, 0, 1));
					} else
						session.moveInCameraFrame(new TransformNR(1, 0, 0));
					break;
				case DOWN:
					if (event.isControlDown()) {
						session.moveInCameraFrame(new TransformNR(0, 0, -1));
					} else
						session.moveInCameraFrame(new TransformNR(-1, 0, 0));
					break;
				case LEFT:
					session.moveInCameraFrame(new TransformNR(0, 1, 0));
					break;
				case RIGHT:
					session.moveInCameraFrame(new TransformNR(0, -1, 0));
					break;
				}
				// com.neuronrobotics.sdk.common.Log.error("Arrows " + event.getCode());
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
			if (session.isFocused()) {
				// com.neuronrobotics.sdk.common.Log.error("Key ignonred, session in focus");
				return;
			}
			String character = event.getCharacter();

			// You can still use the key code for non-character keys
			// com.neuronrobotics.sdk.common.Log.error("Key code: " + event.getCode());
			if (event.isControlDown() || (OsUtils.isOSX() ? event.isMetaDown() : event.isControlDown())) {
				// com.neuronrobotics.sdk.common.Log.error("CTRL + ");
				switch ((int) character.charAt(0)) {
				case 90:
				case 122:
				case 26:
					com.neuronrobotics.sdk.common.Log.error("Undo");
					workplane.cancle();
					activeProject.get().back();
					break;
				case 121:
				case 25:
					com.neuronrobotics.sdk.common.Log.error("redo");
					activeProject.get().forward();
					break;
				case 103:
				case 7:
					onGroup(null);
					break;
				case 71:
					if (event.isShiftDown()) {
						com.neuronrobotics.sdk.common.Log.error("Un-Group");
						session.onUngroup();
					}
					break;
				case 1:
					com.neuronrobotics.sdk.common.Log.error("Select All");
					session.selectAll();
					break;
				case 3:
					session.setCopyListToCurrentSelected();
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
				case 12:
					session.lockToggle();
					break;
				default:
					if (!character.isEmpty()) {
						char rawChar = character.charAt(0);
						System.err.println("CTRL+ Raw char value: " + (int) rawChar);
					} else {
						System.err.println("No character data available (probably a non-character key)");
					}
					break;
				}
			} else {
				switch ((int) character.charAt(0)) {
				case 119:// w
					onWOrkplane(null);
					break;
				case 45:// -
					onZoomOut(null);
					break;
				case 43:// +
					onZoomIn(null);
					break;
				case 102:// f
					onFitView(null);
					break;
				case 104:// h
					session.setToHole();
					break;
				case 115:
					session.setToSolid();
					break;
				case 100:// d
					session.onDrop();
					break;
				case 108:// l
					session.onAllign();
					break;
				case 99:// c
					session.onCruse();
					break;
				case 27:// escape
					workplane.cancle();
				break;
				case 116:// t
				case 84:
					session.toggleTransparent();
					break;
				case 109:// m
					session.onMirror();
					break;
				default:
					if (!character.isEmpty()) {
						char rawChar = character.charAt(0);
						System.err.println("Raw char value: " + (int) rawChar + " : " + character);
					} else {
						System.err.println("No character data available (probably a non-character key)");
					}
					break;
				}
			}
		});
	}

	private void cancel() {
		System.out.println("Cancel event");
		if (workplane.isTemporaryPlane()) {
			activeProject.get().setWorkplane(new TransformNR());
			workplane.placeWorkplaneVisualization();
		}
		session.clearSelection();
	}

	public boolean isEventACancel(MouseEvent event) {
		Node in = event.getPickResult().getIntersectedNode();
		if (in != ground && in != engine.getSubScene() && in != workplane.getPlacementPlane()
				&& in != selectionBox.getSelectionPlane())
			return false;
		if (event.isControlDown())
			return false;
		if (!event.isPrimaryButtonDown())
			return false;
		if (event.isSecondaryButtonDown())
			return false;
		return true;
	}

	@Override
	public void onChange(VirtualCameraMobileBase camera) {
		double zoom = camera.getZoomDepth();
		double az = camera.getPanAngle();
		double el = camera.getTiltAngle();
		// com.neuronrobotics.sdk.common.Log.error("Elevation "+el);
//		if (el < -90 || el > 90) {
//			ground.setVisible(false);
//		} else {
//			ground.setVisible(true);
//		}
		double x = camera.getGlobalX();
		double y = camera.getGlobalY();
		double z = camera.getGlobalZ();
		double screenW = engine.getSubScene().getWidth();
		double screenH = engine.getSubScene().getHeight();
		session.onCameraChange(screenW, screenH, zoom, az, el, x, y, z);
		selectionBox.onCameraChange(screenW, screenH, zoom, az, el, x, y, z);
	}

	@Override
	public void onSaveSuggestion() {
		session.save();
	}

	@Override
	public void onInitializationDone() {
		BowlerStudio.runLater(() -> {
			fileNameBox.setText(activeProject.get().getMyProjectName());
		});
	}

	@Override
	public void onWorkplaneChange(TransformNR newWP) {
		ruler.setWP(newWP);
	}

	@Override
	public void onInitializationStart() {
		// Auto-generated method stub

	}

	@Override
	public void onRegenerateDone() {
		// Auto-generated method stub

	}

	@Override
	public void onRegenerateStart() {
		// Auto-generated method stub

	}
}
