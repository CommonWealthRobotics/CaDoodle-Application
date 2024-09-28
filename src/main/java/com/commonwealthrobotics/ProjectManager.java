package com.commonwealthrobotics;

/**
 * Sample Skeleton for 'ProjectManager.fxml' Controller Class
 */

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;

public class ProjectManager {

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML // fx:id="projectGrid"
    private GridPane projectGrid; // Value injected by FXMLLoader

    @FXML
    void onNewProject(ActionEvent event) {

    }

    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert projectGrid != null : "fx:id=\"projectGrid\" was not injected: check your FXML file 'ProjectManager.fxml'.";

    }

}
