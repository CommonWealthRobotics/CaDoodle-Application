package com.commonwealthrobotics;

import java.io.IOException;

/**
 * Sample Skeleton for 'ProjectManager.fxml' Controller Class
 */

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class ProjectManager {

	@FXML // ResourceBundle that was given to the FXMLLoader
	private ResourceBundle resources;

	@FXML // URL location of the FXML file that was given to the FXMLLoader
	private URL location;

	@FXML // fx:id="projectGrid"
	private GridPane projectGrid; // Value injected by FXMLLoader

	private static ActiveProject ap;

	private static Stage stage;

	private static Runnable onFinish;

	private static Runnable clearScreen;


	@FXML
	void onNewProject(ActionEvent event) {
		clearScreen.run();
		try {
			ap.newProject();
			ap.get().initialize();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		stage.close();
		onFinish.run();
	}

	@FXML // This method is called by the FXMLLoader when initialization is complete
	void initialize() {
		assert projectGrid != null
				: "fx:id=\"projectGrid\" was not injected: check your FXML file 'ProjectManager.fxml'.";
		

	}

	public static void launch(ActiveProject ap, Runnable onFinish, Runnable clearScreen) {
		ProjectManager.ap = ap;
		ProjectManager.onFinish = onFinish;
		ProjectManager.clearScreen = clearScreen;
		try {
			// Load the FXML file
			FXMLLoader loader = new FXMLLoader(ProjectManager.class.getResource("ProjectManager.fxml"));
			loader.setController(new ProjectManager());
			Parent root = loader.load();

			stage = new Stage();
			stage.setTitle("Project Manager");
			// Set the window to always be on top
			stage.setAlwaysOnTop(true);
			stage.setOnCloseRequest(event -> {
				onFinish.run();
			});
			// Set the scene
			Scene scene = new Scene(root);
			stage.setScene(scene);
			// Show the new window
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
			// Handle the exception (e.g., show an error dialog)
		}
	}

}
