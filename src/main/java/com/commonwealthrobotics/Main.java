/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.commonwealthrobotics;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;

import javax.swing.filechooser.FileSystemView;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.NameGetter;
import com.neuronrobotics.bowlerstudio.PsudoSplash;
import com.neuronrobotics.bowlerstudio.SplashManager;
import com.neuronrobotics.bowlerstudio.assets.FontSizeManager;
import com.neuronrobotics.bowlerstudio.assets.StudioBuildInfo;
import com.neuronrobotics.bowlerstudio.scripting.DownloadManager;
import com.neuronrobotics.bowlerstudio.scripting.GitHubWebFlow;
import com.neuronrobotics.bowlerstudio.scripting.PasswordManager;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main  extends Application {
	@Override
	public void start(Stage newStage) throws Exception {
		SplashManager.renderSplashFrame(1, "Main Window Load");

		FXMLLoader loader = new FXMLLoader(Main.class.getResource("MainWindow.fxml"));
		loader.setController(new MainController());
		Parent root = loader.load();
		
		double sw = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDisplayMode().getWidth();
		double sh = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDisplayMode().getHeight();
		Rectangle2D primaryScreenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
		System.out.println("Screen "+sw+"x"+sh);
		sw=primaryScreenBounds.getWidth();
		sh=primaryScreenBounds.getHeight();
		double w ;
		double h ;
		w=sw-40;
		h=sh-40;
		
		Scene scene = new Scene(root,  w, h,true,SceneAntialiasing.BALANCED);
		newStage.setScene(scene);
		String title=StudioBuildInfo.getAppName()+" v " + StudioBuildInfo.getVersion();
		if(newStage!=null)
			newStage.setTitle(title);
		newStage.setOnCloseRequest(event -> {
			System.out.println("CaDoodle Exiting");
			Platform.exit();
			System.exit(0);
		});

		FontSizeManager.addListener(fontNum -> {
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
		SplashManager.renderSplashFrame(1, "Main Window Show");
		newStage.show();
	}
	public static void main(String [] args) {
		String relative = FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
		if (!relative.endsWith("Documents")) {
			relative = relative + DownloadManager.delim()+"Documents";
		}
		File file = new File(relative + "/CaDoodle-workspace/");
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
		PsudoSplash.setResource(Main.class.getResource("SourceIcon.png"));
		SplashManager.renderSplashFrame(0, "Startup");
		launch(args);	
		
	}

}
	
