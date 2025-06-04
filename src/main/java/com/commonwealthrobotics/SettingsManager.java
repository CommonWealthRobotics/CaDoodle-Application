/**
 * Sample Skeleton for 'Settings.fxml' Controller Class
 */

package com.commonwealthrobotics;

import java.io.File;
import java.io.IOException;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.OperationResult;

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
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

public class SettingsManager {

	private static Stage stage;
	private static MainController mc;
	private static boolean changedDir = false;
	@FXML
	private CheckBox advancedSelector;

	@FXML
	private TextField apiKey;

	@FXML
	private RadioButton askOpt;

	@FXML
	private CheckBox connectServer;

	@FXML
	private RadioButton eraseOpt;

	@FXML
	private RadioButton insertOpt;

	@FXML
	private ToggleGroup insertStrat;

	@FXML
	private TextField insertionExplanation;

	@FXML
	private TextField ipaddressField;

	@FXML
	private TextField portField;

	@FXML
	private Label serverIPDisplay;

	@FXML
	private VBox serverStatusBox;

	@FXML
	private CheckBox startServerCheckbox;

	@FXML
	private TextField workingDirPath;

	@FXML
	void checkServerConfigs(KeyEvent event) {
		try {
			URL u= new URL(ser)
		}
	}

	@FXML
	void onConnectServer(ActionEvent event) {

	}

	@FXML
	void onStartServer(ActionEvent event) {

	}

	@FXML
	void onAdvancedMode(ActionEvent event) {
		boolean selected = advancedSelector.isSelected();
		ConfigurationDatabase.put("CaDoodle", "CaDoodleAdvancedMode", "" + selected);
		mc.setAdvancedMode(selected);
		ConfigurationDatabase.save();
	}

	@FXML
	void onAlwaysAsk(ActionEvent event) {
		System.out.println("Ask");
		setExplanationText(OperationResult.ASK);
	}

	@FXML
	void onAlwaysContinue(ActionEvent event) {
		System.out.println("Continue");
		setExplanationText(OperationResult.PRUNE);

	}

	@FXML
	void onAlwaysInsert(ActionEvent event) {
		System.out.println("Insert");
		setExplanationText(OperationResult.INSERT);
	}

	private void setExplanationText(OperationResult result) {
		switch (result) {
		case INSERT:
			insertionExplanation.setText(
					"Insert will add this operation at the current position while keeping subsequent operations.");
			break;
		case PRUNE:
			insertionExplanation.setText(
					"Replace subsequent work with this change.\nThis will remove any work you've done after this point.");
			break;
		case ASK:
			insertionExplanation
					.setText("Always ask what I want to do with a popup window every time something is edited.");
			break;
		}
		ConfigurationDatabase.put("CaDoodle", "Insertion Stratagy", result.name());
		ConfigurationDatabase.save();
	}

	@FXML
	void onBrowse(ActionEvent event) {
		System.out.println("Browse For Working Location");
		File start = new File(workingDirPath.getText());
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Select a Directory");

		// Set the initial directory
		if (start.exists()) {
			directoryChooser.setInitialDirectory(start);
		}

		// Show the dialog and get the selected directory
		File selectedDirectory = directoryChooser.showDialog(stage);

		if (selectedDirectory != null) {
			String absolutePath = selectedDirectory.getAbsolutePath();
			System.out.println("Selected directory: " + absolutePath);
			ConfigurationDatabase.put("CaDoodle", "CaDoodleWorkspace", absolutePath);
			if (!absolutePath.contentEquals(workingDirPath.getText())) {
				changedDir = true;
			}
			workingDirPath.setText(absolutePath);
			ConfigurationDatabase.save();
		}
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
		OperationResult insertionStrat = OperationResult.fromString(
				(String) ConfigurationDatabase.get("CaDoodle", "Insertion Stratagy", OperationResult.ASK.name()));
		if (insertionStrat == OperationResult.INSERT)
			insertOpt.setSelected(true);
		if (insertionStrat == OperationResult.PRUNE)
			eraseOpt.setSelected(true);
		setExplanationText(insertionStrat);
		String dir = (String) ConfigurationDatabase.get("CaDoodle", "CaDoodleWorkspace", ActiveProject.getWorkingDir());
		workingDirPath.setText(dir);
		boolean advanced = Boolean
				.parseBoolean(ConfigurationDatabase.get("CaDoodle", "CaDoodleAdvancedMode", "" + true).toString());
		mc.setAdvancedMode(advanced);
		advancedSelector.setSelected(advanced);
		changedDir = false;
	}

	public static void main(String[] args) {
		JavaFXInitializer.go();
		BowlerStudio.runLater(() -> launch(new MainController()));

	}

	public static void launch(MainController mc) {
		SettingsManager.mc = mc;
		try {
			// Load the FXML file
			System.out.println("Resource URL: " + ProjectManager.class.getResource("Settings.fxml"));
			FXMLLoader loader = new FXMLLoader(
					SettingsManager.class.getClassLoader().getResource("com/commonwealthrobotics/Settings.fxml"));
			// loader.setController(new SettingsManager());
			Parent root = loader.load();
			stage = new Stage();
			stage.setTitle("CaDoodle Settings");
			// Set the window to always be on top
			stage.setAlwaysOnTop(true);
			// Set the scene
			Scene scene = new Scene(root);
			stage.setScene(scene);
			stage.setOnCloseRequest(event -> {
				if (changedDir) {
					mc.onHome(null);
				}
			});
			// Show the new window
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
			// Handle the exception (e.g., show an error dialog)
		}
	}
}
