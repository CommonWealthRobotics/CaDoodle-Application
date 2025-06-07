/**
 * Sample Skeleton for 'Settings.fxml' Controller Class
 */

package com.commonwealthrobotics;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.OperationResult;

import eu.mihosoft.vrl.v3d.CSGClient;
import eu.mihosoft.vrl.v3d.CSGServer;
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
			// Create a trust manager that ignores certificate errors
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} };

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustAllCerts, null);

			// Also disable hostname verification
			HostnameVerifier allHostsValid = (hostname, session) -> true;
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

			SSLSocketFactory factory = sslContext.getSocketFactory();
			SSLSocket socket = (SSLSocket) factory.createSocket();
			socket.connect(new InetSocketAddress(ipaddressField.getText(), Integer.parseInt(portField.getText())), 100);
			socket.startHandshake(); // Verify SSL handshake works
			socket.close();
			connectServer.setDisable(false);
		} catch (Exception ex) {
			// ex.printStackTrace();
			connectServer.setDisable(true);
			return;
		}
	}

	@FXML
	void onConnectServer(ActionEvent event) {
		if (connectServer.isSelected()) {
			String key = apiKey.getText();
			Path tempFile;
			try {
				tempFile = Files.createTempFile("mytemp", ".txt");
				Files.write(tempFile, key.getBytes(StandardCharsets.UTF_8));
				System.out.println("Opening Server Connection");
				CSGClient.start(ipaddressField.getText(), Integer.parseInt(portField.getText()), tempFile.toFile());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Closing Server Connection");
			CSGClient.close();
		}
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
