/**
 * Sample Skeleton for 'Settings.fxml' Controller Class
 */

package com.commonwealthrobotics;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.neuronrobotics.bowlerstudio.BowlerStudio;

import eu.mihosoft.vrl.v3d.JavaFXInitializer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

public class SettingsManager {

	private static Stage stage;

	@FXML // ResourceBundle that was given to the FXMLLoader
	private ResourceBundle resources;

	@FXML // URL location of the FXML file that was given to the FXMLLoader
	private URL location;

	@FXML // fx:id="advancedSelector"
	private CheckBox advancedSelector; // Value injected by FXMLLoader

	@FXML // fx:id="askOpt"
	private RadioButton askOpt; // Value injected by FXMLLoader

	@FXML // fx:id="eraseOpt"
	private RadioButton eraseOpt; // Value injected by FXMLLoader

	@FXML // fx:id="insertOpt"
	private RadioButton insertOpt; // Value injected by FXMLLoader

	@FXML // fx:id="insertStrat"
	private ToggleGroup insertStrat; // Value injected by FXMLLoader

	@FXML // fx:id="insertionExplanation"
	private TextField insertionExplanation; // Value injected by FXMLLoader

	@FXML // fx:id="workingDirPath"
	private TextField workingDirPath; // Value injected by FXMLLoader

	@FXML
	void onAdvancedMode(ActionEvent event) {
		System.out.println("Advanced Mode "+advancedSelector.isSelected());
	}

	@FXML
	void onAlwaysAsk(ActionEvent event) {
		System.out.println("Ask");
	}

	@FXML
	void onAlwaysContinue(ActionEvent event) {
		System.out.println("Continue");
	}

	@FXML
	void onAlwaysInsert(ActionEvent event) {
		System.out.println("Insert");
	}

	@FXML
	void onBrowse(ActionEvent event) {
		System.out.println("Browse For WOrking Location");
	}

	@FXML // This method is called by the FXMLLoader when initialization is complete
	void initialize() {
		assert advancedSelector != null
				: "fx:id=\"advancedSelector\" was not injected: check your FXML file 'Settings.fxml'.";
		assert askOpt != null : "fx:id=\"askOpt\" was not injected: check your FXML file 'Settings.fxml'.";
		assert eraseOpt != null : "fx:id=\"eraseOpt\" was not injected: check your FXML file 'Settings.fxml'.";
		assert insertOpt != null : "fx:id=\"insertOpt\" was not injected: check your FXML file 'Settings.fxml'.";
		assert insertStrat != null : "fx:id=\"insertStrat\" was not injected: check your FXML file 'Settings.fxml'.";
		assert insertionExplanation != null
				: "fx:id=\"insertionExplanation\" was not injected: check your FXML file 'Settings.fxml'.";
		assert workingDirPath != null
				: "fx:id=\"workingDirPath\" was not injected: check your FXML file 'Settings.fxml'.";

	}
	public static void main(String[] args) {
		JavaFXInitializer.go();
		BowlerStudio.runLater(()->launch());
		;
	}

	public static void launch() {
		try {
			// Load the FXML file
			System.out.println("Resource URL: " + ProjectManager.class.getResource("Settings.fxml"));
			FXMLLoader loader = new FXMLLoader(SettingsManager.class.getClassLoader().getResource("com/commonwealthrobotics/Settings.fxml"));
			//loader.setController(new SettingsManager());
			Parent root = loader.load();

			stage = new Stage();
			stage.setTitle("CaDoodle Settings");
			// Set the window to always be on top
			stage.setAlwaysOnTop(true);
			stage.setOnCloseRequest(event -> {
				
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
