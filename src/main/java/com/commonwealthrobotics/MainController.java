/**
 * Sample Skeleton for 'MainWindow.fxml' Controller Class
 */

package com.commonwealthrobotics;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class MainController {

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML // fx:id="buttonOverlay"
    private AnchorPane buttonOverlay; // Value injected by FXMLLoader

    @FXML // fx:id="colorPicker"
    private ColorPicker colorPicker; // Value injected by FXMLLoader

    @FXML // fx:id="control3d"
    private AnchorPane control3d; // Value injected by FXMLLoader

    @FXML // fx:id="cruseButton"
    private ImageView cruseButton; // Value injected by FXMLLoader

    @FXML // fx:id="drawerArea"
    private AnchorPane drawerArea; // Value injected by FXMLLoader

    @FXML // fx:id="drawerButton"
    private Button drawerButton; // Value injected by FXMLLoader

    @FXML // fx:id="drawerHolder"
    private HBox drawerHolder; // Value injected by FXMLLoader

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
    private ImageView holeButton; // Value injected by FXMLLoader

    @FXML // fx:id="home"
    private Button home; // Value injected by FXMLLoader

    @FXML // fx:id="homeButton"
    private ImageView homeButton; // Value injected by FXMLLoader

    @FXML // fx:id="homeViewButton"
    private Button homeViewButton; // Value injected by FXMLLoader

    @FXML // fx:id="importButton"
    private Button importButton; // Value injected by FXMLLoader

    @FXML // fx:id="lockButton"
    private Button lockButton; // Value injected by FXMLLoader

    @FXML // fx:id="lockUnlockTooltip"
    private Tooltip lockUnlockTooltip; // Value injected by FXMLLoader

    @FXML // fx:id="mirronButton"
    private ImageView mirronButton; // Value injected by FXMLLoader

    @FXML // fx:id="model"
    private Button model; // Value injected by FXMLLoader

    @FXML // fx:id="modeling"
    private ImageView modeling; // Value injected by FXMLLoader

    @FXML // fx:id="notesButton"
    private Button notesButton; // Value injected by FXMLLoader

    @FXML // fx:id="objectPallet"
    private GridPane objectPallet; // Value injected by FXMLLoader

    @FXML // fx:id="physics"
    private Button physics; // Value injected by FXMLLoader

    @FXML // fx:id="physicsButton"
    private ImageView physicsButton; // Value injected by FXMLLoader

    @FXML // fx:id="rulerButton"
    private Button rulerButton; // Value injected by FXMLLoader

    @FXML // fx:id="settingsButton"
    private Button settingsButton; // Value injected by FXMLLoader

    @FXML // fx:id="shapeCatagory"
    private ComboBox<?> shapeCatagory; // Value injected by FXMLLoader

    @FXML // fx:id="shapeConfiguration"
    private TitledPane shapeConfiguration; // Value injected by FXMLLoader

    @FXML // fx:id="shapeConfigurationBox"
    private Accordion shapeConfigurationBox; // Value injected by FXMLLoader

    @FXML // fx:id="shapeConfigurationHolder"
    private AnchorPane shapeConfigurationHolder; // Value injected by FXMLLoader

    @FXML // fx:id="showAllButton"
    private Button showAllButton; // Value injected by FXMLLoader

    @FXML // fx:id="snapGrid"
    private ComboBox<?> snapGrid; // Value injected by FXMLLoader

    @FXML // fx:id="ungroupButton"
    private ImageView ungroupButton; // Value injected by FXMLLoader

    @FXML // fx:id="view3d"
    private AnchorPane view3d; // Value injected by FXMLLoader

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
    void onColorPick(ActionEvent event) {

    }

    @FXML
    void onCruse(MouseEvent event) {

    }

    @FXML
    void onDrawer(ActionEvent event) {

    }

    @FXML
    void onExport(ActionEvent event) {

    }

    @FXML
    void onFitView(MouseEvent event) {

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
    void onHomeView(MouseEvent event) {

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
        assert buttonOverlay != null : "fx:id=\"buttonOverlay\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert colorPicker != null : "fx:id=\"colorPicker\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert control3d != null : "fx:id=\"control3d\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert cruseButton != null : "fx:id=\"cruseButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert drawerArea != null : "fx:id=\"drawerArea\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert drawerButton != null : "fx:id=\"drawerButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert drawerHolder != null : "fx:id=\"drawerHolder\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert drawrImage != null : "fx:id=\"drawrImage\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert export != null : "fx:id=\"export\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert fileNameBox != null : "fx:id=\"fileNameBox\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert fitViewButton != null : "fx:id=\"fitViewButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert groupButton != null : "fx:id=\"groupButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert hideSHow != null : "fx:id=\"hideSHow\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert holeButton != null : "fx:id=\"holeButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert home != null : "fx:id=\"home\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert homeButton != null : "fx:id=\"homeButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert homeViewButton != null : "fx:id=\"homeViewButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert importButton != null : "fx:id=\"importButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert lockButton != null : "fx:id=\"lockButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert lockUnlockTooltip != null : "fx:id=\"lockUnlockTooltip\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert mirronButton != null : "fx:id=\"mirronButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert model != null : "fx:id=\"model\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert modeling != null : "fx:id=\"modeling\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert notesButton != null : "fx:id=\"notesButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert objectPallet != null : "fx:id=\"objectPallet\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert physics != null : "fx:id=\"physics\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert physicsButton != null : "fx:id=\"physicsButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert rulerButton != null : "fx:id=\"rulerButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert settingsButton != null : "fx:id=\"settingsButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert shapeCatagory != null : "fx:id=\"shapeCatagory\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert shapeConfiguration != null : "fx:id=\"shapeConfiguration\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert shapeConfigurationBox != null : "fx:id=\"shapeConfigurationBox\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert shapeConfigurationHolder != null : "fx:id=\"shapeConfigurationHolder\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert showAllButton != null : "fx:id=\"showAllButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert snapGrid != null : "fx:id=\"snapGrid\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert ungroupButton != null : "fx:id=\"ungroupButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert view3d != null : "fx:id=\"view3d\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert viewControlCubeHolder != null : "fx:id=\"viewControlCubeHolder\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert visbilityButton != null : "fx:id=\"visbilityButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert workplaneButton != null : "fx:id=\"workplaneButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert zoomInButton != null : "fx:id=\"zoomInButton\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert zoomOutButton != null : "fx:id=\"zoomOutButton\" was not injected: check your FXML file 'MainWindow.fxml'.";

    }

}
