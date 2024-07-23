/**
 * Sample Skeleton for 'MainWindow.fxml' Controller Class
 */

package com.commonwealthrobotics;

import java.net.URL;
import java.util.ResourceBundle;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

public class MainController {

	private boolean drawerOpen=true;
	@FXML
	private AnchorPane anchorPanForConfiguration;

	@FXML
	private AnchorPane buttonOverlay;

	@FXML
	private ColorPicker colorPicker;

	@FXML
	private GridPane configurationGrid;

	@FXML
	private AnchorPane control3d;

	@FXML
	private GridPane controlBar;

	@FXML
	private ImageView cruseButton;

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
	private ImageView holeButton;

	@FXML
	private Button home;

	@FXML
	private ImageView homeButton;

	@FXML
	private Button homeViewButton;

	@FXML
	private Button importButton;

	@FXML
	private Button lockButton;

	@FXML
	private Tooltip lockUnlockTooltip;

	@FXML
	private ImageView mirronButton;

	@FXML
	private Button model;

	@FXML
	private ImageView modeling;

	@FXML
	private Button notesButton;

	@FXML
	private GridPane objectPallet;

	@FXML
	private Button physics;

	@FXML
	private ImageView physicsButton;

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
	private ImageView ungroupButton;

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
	void onColorPick(ActionEvent event) {
		Color value = colorPicker.getValue();
		System.out.println("Color set to " + value);
	}

	@FXML
	void onCruse(MouseEvent event) {

	}

	@FXML
	void onDrawer(ActionEvent event) {
		drawerOpen=!drawerOpen;
		if(drawerOpen) {
			drawrImage.setImage(new Image(MainController.class.getResourceAsStream("drawerClose.png")));
			drawerHolder.getChildren().add(drawerArea);
		}else {
			drawrImage.setImage(new Image(MainController.class.getResourceAsStream("drawerOpen.png")));
			drawerHolder.getChildren().remove(drawerArea);
		}
	}

	@FXML
	void onExport(ActionEvent event) {

	}

	@FXML
	void onFitView(ActionEvent event) {

	}

	@FXML
	void onGroup(ActionEvent event) {

	}

	@FXML
	void onHideConnections(ActionEvent event) {

	}

	@FXML
	void onHideNotes(ActionEvent event) {

	}

	@FXML
	void onHideShow(ActionEvent event) {

	}

	@FXML
	void onHoleButton(MouseEvent event) {

	}

	@FXML
	void onHome(MouseEvent event) {

	}

	@FXML
	void onHomeViewButton(ActionEvent event) {

	}

	@FXML
	void onImport(ActionEvent event) {

	}

	@FXML
	void onLock(ActionEvent event) {

	}

	@FXML
	void onMirron(MouseEvent event) {

	}

	@FXML
	void onModeling(MouseEvent event) {

	}

	@FXML
	void onNotesClick(ActionEvent event) {

	}

	@FXML
	void onPhysics(MouseEvent event) {

	}

	@FXML
	void onRuler(ActionEvent event) {

	}

	@FXML
	void onSetCatagory(ActionEvent event) {

	}

	@FXML
	void onSettings(ActionEvent event) {

	}

	@FXML
	void onShowHidden(ActionEvent event) {

	}

	@FXML
	void onUngroup(MouseEvent event) {

	}

	@FXML
	void onVisibility(ActionEvent event) {

	}

	@FXML
	void onWOrkplane(ActionEvent event) {

	}

	@FXML
	void onZoomIn(ActionEvent event) {

	}

	@FXML
	void onZoomOut(ActionEvent event) {

	}

	@FXML
	void setName(ActionEvent event) {

	}

	@FXML
	void setSnapGrid(ActionEvent event) {

	}

	@FXML
	void showAll(ActionEvent event) {

	}

	@FXML
	void zoomInView(MouseEvent event) {

	}

	@FXML
	void zoomOutViewButton(MouseEvent event) {

	}

	@FXML // This method is called by the FXMLLoader when initialization is complete
	void initialize() {
		assert buttonOverlay != null
				: "fx:id=\"buttonOverlay\" was not injected: check your FXML file 'MainWindow.fxml'.";
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
		BowlerStudio3dEngine engine = new BowlerStudio3dEngine();
		engine.rebuild();
		SubScene subScene = engine.getSubScene();
		BowlerStudio.runLater(() -> {
			subScene.setFocusTraversable(false);
			view3d.widthProperty().addListener((observable, oldValue, newValue) -> {
	            subScene.setWidth(newValue.doubleValue());
	        });

			view3d.heightProperty().addListener((observable, oldValue, newValue) -> {
	            subScene.setHeight(newValue.doubleValue());
	        });
			BowlerStudio.runLater(() -> {
				view3d.getChildren().add(subScene);
				AnchorPane.setTopAnchor(subScene, 0.0);
				AnchorPane.setRightAnchor(subScene, 0.0);
				AnchorPane.setLeftAnchor(subScene, 0.0);
				AnchorPane.setBottomAnchor(subScene, 0.0);
			});

		});

	}

}
