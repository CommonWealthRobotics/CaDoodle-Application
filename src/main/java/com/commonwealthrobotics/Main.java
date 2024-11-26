/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.commonwealthrobotics;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;

import javax.swing.filechooser.FileSystemView;
import static com.neuronrobotics.bowlerstudio.scripting.DownloadManager.*;

import com.neuronrobotics.bowlerstudio.BowlerKernel;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.NameGetter;
import com.neuronrobotics.bowlerstudio.PsudoSplash;
import com.neuronrobotics.bowlerstudio.SplashManager;
import com.neuronrobotics.bowlerstudio.assets.AssetFactory;
import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.assets.FontSizeManager;
import com.neuronrobotics.bowlerstudio.assets.StudioBuildInfo;
import com.neuronrobotics.bowlerstudio.scripting.DownloadManager;
import com.neuronrobotics.bowlerstudio.scripting.GitHubWebFlow;
import com.neuronrobotics.bowlerstudio.scripting.IApprovalForDownload;
import com.neuronrobotics.bowlerstudio.scripting.IDownloadManagerEvents;
import com.neuronrobotics.bowlerstudio.scripting.PasswordManager;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.vitamins.Vitamins;
import com.neuronrobotics.nrconsole.util.FileSelectionFactory;
import com.neuronrobotics.sdk.common.Log;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
	private static Thread loadDeps;

	@Override
	public void start(Stage newStage) throws Exception {
		// SplashManager.renderSplashFrame(1, "Main Window Load");

		FXMLLoader loader = new FXMLLoader(Main.class.getResource("MainWindow.fxml"));
		loader.setController(new MainController());
		Parent root = loader.load();

		double sw = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode()
				.getWidth();
		double sh = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode()
				.getHeight();
		Rectangle2D primaryScreenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
		com.neuronrobotics.sdk.common.Log.error("Screen " + sw + "x" + sh);
		sw = primaryScreenBounds.getWidth();
		sh = primaryScreenBounds.getHeight();
		double w;
		double h;
		w = sw - 40;
		h = sh - 40;

		Scene scene = new Scene(root, w, h, true, SceneAntialiasing.BALANCED);
		newStage.setScene(scene);
		String title = StudioBuildInfo.getAppName() + " v " + StudioBuildInfo.getVersion();
		if (newStage != null)
			newStage.setTitle(title);
		newStage.setOnCloseRequest(event -> {
			Platform.exit();
			new Thread(() -> {
				Log.error("CaDoodle Exiting");
				System.exit(0);
			}).start();

		});

		FontSizeManager.addListener(fontNum ->

		{
			int tmp = fontNum - 10;
			if (tmp < 12)
				tmp = 12;
			root.setStyle("-fx-font-size: " + tmp + "pt");
		});
		BowlerStudio.runLater(() -> {
			try {

				Image loadAsset = new Image(PsudoSplash.getResource().toString());
				newStage.getIcons().add(loadAsset);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		newStage.setMinWidth(900);
		newStage.setMinHeight(600);
		// SplashManager.renderSplashFrame(1, "Main Window Show");
		FileSelectionFactory.setStage(newStage);
		newStage.show();

		// getLoadDeps().start();
	}

	public static void main(String[] args) {
		if (args != null) {
			if (args.length != 0) {
				File f = new File(args[0]);
				if (f.exists()) {
					ConfigurationDatabase.put("CaDoodle", "CaDoodleacriveFile", f.getAbsolutePath());
				}
			}
		}
		PsudoSplash.setResource(Main.class.getResource("SourceIcon.png"));
		SplashManager.renderSplashFrame(1, "Main Window Show");
		setUpApprovalWindow();
		ScriptingEngine.setAppName("CaDoodle");
		String relative = ScriptingEngine.getWorkingDirectory().getAbsolutePath();
		File file = new File(relative + delim() + "CaDoodle-workspace" + delim());
		file.mkdirs();
		ScriptingEngine.setWorkspace(file);
		NameGetter mykey = new NameGetter();
		GitHubWebFlow.setName(mykey);
		try {
			PasswordManager.setupAnyonmous();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ensureGitAssetsArePresent();
	
		launch();
	}

	private static void ensureGitAssetsArePresent() {
		Vitamins.loadAllScriptFiles();

		try {
			AssetFactory.loadAllAssets();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BowlerStudio.ensureUpdated("https://github.com/CommonWealthRobotics/ExternalEditorsBowlerStudio.git",
				"https://github.com/CommonWealthRobotics/freecad-bowler-cli.git",
				"https://github.com/CommonWealthRobotics/blender-bowler-cli.git",
				"https://github.com/kennetek/gridfinity-rebuilt-openscad.git");

//		try {
//			ScriptingEngine.gitScriptRun("https://github.com/madhephaestus/CaDoodle-Example-Objects.git",
//					"MakeVitamins.groovy");
//			System.exit(1);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	private static void setUpApprovalWindow() {
		DownloadManager.setDownloadEvents(new IDownloadManagerEvents() {

			@Override
			public void startDownload() {
				SplashManager.renderSplashFrame(0, "Downloading...");
			}

			@Override
			public void finishDownload() {
				SplashManager.closeSplash();
			}
		});
		DownloadManager.setApproval(new IApprovalForDownload() {
			private ButtonType buttonType = null;

			@Override
			public boolean get(String name, String url) {
				buttonType = null;

				BowlerKernel.runLater(() -> {
					Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
					alert.setTitle("Message");
					alert.setHeaderText("Would you like to add the: " + name + " Plugin?");
					Node root = alert.getDialogPane();
					Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
					stage.setOnCloseRequest(ev -> alert.hide());
					FontSizeManager.addListener(fontNum -> {
						int tmp = fontNum - 10;
						if (tmp < 12)
							tmp = 12;
						root.setStyle("-fx-font-size: " + tmp + "pt");
						alert.getDialogPane().applyCss();
						alert.getDialogPane().layout();
						stage.sizeToScene();
					});
					Optional<ButtonType> result = alert.showAndWait();
					buttonType = result.get();
					alert.close();
				});

				while (buttonType == null) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

				return buttonType.equals(ButtonType.OK);
			}

			@Override
			public void onInstallFail(String url) {
				try {
					BowlerStudio.openExternalWebpage(new URL(url));
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

//	public static Thread getLoadDeps() {
//		return loadDeps;
//	}
//
//	public static void setLoadDeps(Thread loadDeps) {
//		Main.loadDeps = loadDeps;
//	}

}
