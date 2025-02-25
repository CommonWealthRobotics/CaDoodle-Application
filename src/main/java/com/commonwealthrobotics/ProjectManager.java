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

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.SplashManager;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;

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
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(" MMM d \n h:mm a")
			.withZone(ZoneId.systemDefault());

	@FXML
	void onNewProject(ActionEvent event) {
		clearScreen.run();
		try {
			ap.newProject();
			ap.get().initialize();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
		stage.close();
		onFinish.run();
	}

	@FXML // This method is called by the FXMLLoader when initialization is complete
	void initialize() {
		assert projectGrid != null
				: "fx:id=\"projectGrid\" was not injected: check your FXML file 'ProjectManager.fxml'.";
		new Thread(() -> {
			loadFiles();
		}).start();

	}

	private void loadFiles() {
		try {
			List<CaDoodleFile> proj = ap.getProjects();
			com.neuronrobotics.sdk.common.Log.error("Found " + proj.size() + " projects");
			Collections.sort(proj, new Comparator<CaDoodleFile>() {
				@Override
				public int compare(CaDoodleFile c1, CaDoodleFile c2) {
					// Compare in reverse order for newest first
					return Long.compare(c2.getTimeCreated(), c1.getTimeCreated());
				}
			});
			for (int i = 0; i < proj.size(); i++) {
				CaDoodleFile c = proj.get(i);
				long time = c.getTimeCreated();
				Instant instant = Instant.ofEpochMilli(time);
				String formattedDateTime = formatter.format(instant);
				int row = (i + 1) / 4;
				int col = (i + 1) % 4;
				System.out.println(
						"File " + c.getMyProjectName() + " on " + formattedDateTime + " row=" + row + " col=" + col);

				

				BowlerStudio.runLater(() -> {
					Button b = new Button(c.getMyProjectName());
					b.setOnAction(ev->{
						new Thread(()->{
							try {
								BowlerStudio.runLater(() ->stage.close());
								SplashManager.renderSplashFrame(50, "Initialize");
								ap.setActiveProject(c.getSelf());
								ap.get().initialize();
								SplashManager.closeSplash();
								onFinish.run();
							} catch (Exception e) {
								// Auto-generated catch block
								e.printStackTrace();
							}
						}).start();
					});
					b.getStyleClass().add("image-button");
					b.setContentDisplay(ContentDisplay.TOP);
					ImageView value = new ImageView(c.loadImageFromFile());
					value.setFitWidth(60);
					value.setFitHeight(60);
					b.setGraphic(value);
					VBox box = new VBox();

					box.getChildren().add(b);
					box.getChildren().add(new Label(formattedDateTime));
					
					box.setAlignment(Pos.CENTER); // This centers the contents of the HBox
					b.setAlignment(Pos.CENTER); // This centers the contents of the HBox
					projectGrid.add(box, col, row);
					GridPane.setHalignment(b, HPos.CENTER); // Horizontal center alignment
					GridPane.setValignment(b, VPos.CENTER); //
				});

			}
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
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
