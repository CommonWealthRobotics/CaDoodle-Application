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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.SplashManager;
import com.neuronrobotics.bowlerstudio.scripting.DownloadManager;
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

	@FXML
	private Button copyDoodle;

	@FXML
	private Button newDoodle;

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
	private boolean copy = false;

	private Button currentFileButton=null;

	@FXML
	void onCopyProject(ActionEvent event) {
		copy = true;
		copyDoodle.setDisable(true);
		newDoodle.setDisable(true);
		if(currentFileButton!=null)
			BowlerStudio.runLater(() -> currentFileButton.requestFocus());
	}

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
			int offsetFromStartingButton =2;
			for (int i = 0; i < proj.size(); i++) {
				CaDoodleFile c = proj.get(i);
				long time = c.getTimeCreated();
				Instant instant = Instant.ofEpochMilli(time);
				String formattedDateTime = formatter.format(instant);
				int row = (i + offsetFromStartingButton) / 4;
				int col = (i + offsetFromStartingButton) % 4;
				System.out.println(
						"File " + c.getMyProjectName() + " on " + formattedDateTime + " row=" + row + " col=" + col);

				BowlerStudio.runLater(() -> {
					Button b = new Button(c.getMyProjectName());
					b.setOnAction(ev -> {
						openProject(c);
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
					if(c.getMyProjectName().contentEquals(ap.get().getMyProjectName())) {
						b.requestFocus();
						currentFileButton = b;
					}
					
				});

			}
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void openProject(CaDoodleFile c) {
		new Thread(() -> {
			try {
				BowlerStudio.runLater(() -> stage.close());
				SplashManager.renderSplashFrame(50, "Initialize");
				if (!copy) {
					ap.setActiveProject(c.getSelf());
				} else {
					File sourceDir = c.getSelf().getParentFile();
					File target = new File(sourceDir + "_copy");
					copyDirectory(sourceDir.getAbsolutePath(),target.getAbsolutePath());
					File doodle = new File(target.getAbsolutePath()+DownloadManager.delim()+c.getSelf().getName());
					CaDoodleFile nf = CaDoodleFile.fromFile(doodle, null, false);
					nf.setProjectName(c.getMyProjectName()+"_copy");
					nf.setTimeCreated(System.currentTimeMillis());
					nf.save();
					ap.setActiveProject(doodle);
				}
				ap.get().initialize();
				SplashManager.closeSplash();
				onFinish.run();
			} catch (Exception e) {
				// Auto-generated catch block
				e.printStackTrace();
			}
		}).start();
	}

	/**
	 * Copies a source directory to a target directory that doesn't exist yet.
	 * Creates the target directory and copies all contents including
	 * subdirectories.
	 * 
	 * @param sourceDirectoryPath Path to the source directory
	 * @param targetDirectoryPath Path to the target directory (which will be
	 *                            created)
	 * @throws IOException If an I/O error occurs during copying
	 */
	public static void copyDirectory(String sourceDirectoryPath, String targetDirectoryPath) throws IOException {
		// Convert string paths to Path objects
		Path sourceDir = Paths.get(sourceDirectoryPath);
		Path targetDir = Paths.get(targetDirectoryPath);

		// Verify source directory exists and is a directory
		if (!Files.exists(sourceDir)) {
			throw new IOException("Source directory doesn't exist: " + sourceDirectoryPath);
		}

		if (!Files.isDirectory(sourceDir)) {
			throw new IOException("Source is not a directory: " + sourceDirectoryPath);
		}

		// Create the target directory if it doesn't exist
		Files.createDirectories(targetDir);

		// Walk through source directory and copy all files and subdirectories
		Files.walk(sourceDir).forEach(sourcePath -> {
			try {
				// Get the relative path from source directory
				Path relativePath = sourceDir.relativize(sourcePath);

				// Create the corresponding path in target directory
				Path targetPath = targetDir.resolve(relativePath);

				// Copy the file/directory
				if (Files.isDirectory(sourcePath)) {
					// Create directory if it doesn't exist
					if (!Files.exists(targetPath)) {
						Files.createDirectory(targetPath);
					}
				} else {
					// Copy the file, replacing if it exists
					Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (IOException e) {
				System.err.println("Error copying " + sourcePath + ": " + e.getMessage());
			}
		});

		System.out.println("Directory copied successfully from " + sourceDirectoryPath + " to " + targetDirectoryPath);
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
