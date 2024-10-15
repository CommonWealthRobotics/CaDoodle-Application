package com.commonwealthrobotics;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
/**
 * Sample Skeleton for 'ProjectManager.fxml' Controller Class
 */

import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.BowlerKernel;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.SplashManager;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.nrconsole.util.FileSelectionFactory;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;

public class ExportManager {
	private static ActiveProject ap;

	private static Stage stage;

	private static Runnable onFinish;

	private static Runnable clearScreen;

	@FXML // ResourceBundle that was given to the FXMLLoader
	private ResourceBundle resources;

	@FXML // URL location of the FXML file that was given to the FXMLLoader
	private URL location;

	@FXML // fx:id="blender"
	private CheckBox blender; // Value injected by FXMLLoader

	@FXML // fx:id="freecad"
	private CheckBox freecad; // Value injected by FXMLLoader

	@FXML // fx:id="projectGrid"
	private GridPane projectGrid; // Value injected by FXMLLoader

	@FXML // fx:id="stl"
	private CheckBox stl; // Value injected by FXMLLoader

	@FXML // fx:id="svg"
	private CheckBox svg; // Value injected by FXMLLoader

	private static SelectionSession session;
	
	private File exportDir=null;

	@FXML
	void onExport(ActionEvent event) {
		stage.close();
		new Thread(()->{
			
			if(exportDir==null)
				exportDir = new File(System.getProperty("user.home") +"/Desktop/");
			ArrayList<CSG> back = session.getAllVisable();
			String name = ap.get().getProjectName();
			name=name.replace(' ', '_');
			int index=1;
			for(CSG c:back) {
				if(stl.isSelected()) {
					c.addExportFormat("stl");
				}
				if(svg.isSelected()) {
					c.addExportFormat("svg");
				}
				if(blender.isSelected()) {
					c.addExportFormat("blender");
				}
				if(freecad.isSelected()) {
					c.addExportFormat("freecad");
				}
				c.setName(name+"_"+index);
				index++;
			}
			exportDir=FileSelectionFactory.GetDirectory(exportDir);
			//SplashManager.renderSplashFrame(50, " Exporting...");
			
			if(!exportDir.getAbsolutePath().endsWith(name+"/")) {
				exportDir=new File(exportDir+"/"+name+"/");
			}
			BowlerKernel.processReturnedObjectsStart(back, exportDir);
			onFinish.run();
			SplashManager.closeSplash();
		}).start();
	}

	@FXML // This method is called by the FXMLLoader when initialization is complete
	void initialize() {
		assert blender != null : "fx:id=\"blender\" was not injected: check your FXML file 'ExportWindow.fxml'.";
		assert freecad != null : "fx:id=\"freecad\" was not injected: check your FXML file 'ExportWindow.fxml'.";
		assert projectGrid != null
				: "fx:id=\"projectGrid\" was not injected: check your FXML file 'ExportWindow.fxml'.";
		assert stl != null : "fx:id=\"stl\" was not injected: check your FXML file 'ExportWindow.fxml'.";
		assert svg != null : "fx:id=\"svg\" was not injected: check your FXML file 'ExportWindow.fxml'.";

	}

	public static void launch(SelectionSession session, ActiveProject ap, Runnable onFinish, Runnable clearScreen) {
		ExportManager.session = session;
		ExportManager.ap = ap;
		ExportManager.onFinish = onFinish;
		ExportManager.clearScreen = clearScreen;
		try {
			// Load the FXML file
			FXMLLoader loader = new FXMLLoader(ExportManager.class.getResource("ExportWindow.fxml"));
			loader.setController(new ExportManager());
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
