/**
 * Sample Skeleton for 'Settings.fxml' Controller Class
 */

package com.commonwealthrobotics;

import static com.neuronrobotics.bowlerstudio.scripting.DownloadManager.delim;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;

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
import com.neuronrobotics.sdk.common.Log;

import eu.mihosoft.vrl.v3d.CSGClient;
import eu.mihosoft.vrl.v3d.CSGRequest;
import eu.mihosoft.vrl.v3d.CSGResponse;
import eu.mihosoft.vrl.v3d.CSGServer;
import eu.mihosoft.vrl.v3d.ICSGClientEvent;
import eu.mihosoft.vrl.v3d.JavaFXInitializer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

public class SettingsManager implements ICSGClientEvent {
	private static CSGServer server = null;
	private static Stage stage;
	private static MainController mc;
	private static boolean changedDir = false;
	private HashMap<CSGRequest, Label> active = new HashMap<>();

	private Label clientDisplay = new Label("No client");

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
	private TextField numberOfSides;
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
	private RadioButton checkOnLaunch;

	@FXML
	private ToggleGroup checkUpdate;

	@FXML
	private RadioButton pinToVersion;
	@FXML
	private ComboBox<String> versionOptions;
	private File pinFile;
	private String myVersionFileString;
	private String bindir;

	@FXML
	void onPinVersion(ActionEvent event) {
		Log.debug("onPinVersion");
		versionOptions.setDisable(false);
		try {
			pinFile.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@FXML
	void onPinVersionSelect(ActionEvent event) {
		String selectedItem = versionOptions.getSelectionModel().getSelectedItem();
		Log.debug("onPinVersionSelect to " + selectedItem);
		File f = new File(myVersionFileString);
		try {
			Files.write(f.toPath(), selectedItem.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	void onSetCheck(ActionEvent event) {
		Log.debug("onSetCheck");
		versionOptions.setDisable(true);
		pinFile.delete();
	}

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
			// com.neuronrobotics.sdk.common.Log.error(e);;
			connectServer.setDisable(true);
			return;
		}
	}

	@FXML
	void onConnectServer(ActionEvent event) {
		connectServer.setDisable(false);
		if (connectServer.isSelected()) {

			String key = apiKey.getText();
			Path tempFile;
			try {

				com.neuronrobotics.sdk.common.Log.debug("Opening Server Connection");
				String text = ipaddressField.getText();
				ConfigurationDatabase.put("CaDoodle", "CSGClientConnect", "" + true);
				ConfigurationDatabase.put("CaDoodle", "CSGClientKey", key);
				ConfigurationDatabase.put("CaDoodle", "CSGClientHost", text);
				ConfigurationDatabase.put("CaDoodle", "CSGClientPort", portField.getText());
			} catch (Exception e) {
				com.neuronrobotics.sdk.common.Log.error(e);
			}
		} else {
			com.neuronrobotics.sdk.common.Log.debug("Closing Server Connection");
			ConfigurationDatabase.put("CaDoodle", "CSGClientConnect", "" + false);
		}
		if (clientStateSet()) {
			clientDisplay.setText("Client is Connected!");
			CSGClient.getClient().addListener(this);
		} else {
			connectServer.setSelected(false);
			clientDisplay.setText("Server is missing");
		}

	}

	public static boolean clientStateSet() {
		boolean connect = Boolean
				.parseBoolean(ConfigurationDatabase.get("CaDoodle", "CSGClientConnect", "" + false).toString());
		String key = ConfigurationDatabase.get("CaDoodle", "CSGClientKey", "").toString();
		String host = ConfigurationDatabase.get("CaDoodle", "CSGClientHost", "").toString();
		String port = ConfigurationDatabase.get("CaDoodle", "CSGClientPort", 3742).toString();
		if (connect) {
			try {
				Path tempFile = Files.createTempFile("mytemp", ".txt");
				Files.write(tempFile, key.getBytes(StandardCharsets.UTF_8));
				if (!CSGClient.isRunning())
					CSGClient.start(host, Integer.parseInt(port), tempFile.toFile());
				return true;
			} catch (Exception ex) {
				ConfigurationDatabase.put("CaDoodle", "CSGClientConnect", "" + false);
			}
		} else {
			CSGClient.close();
			com.neuronrobotics.sdk.common.Log.debug("Closing server connection");
		}
		return false;
	}

	public static boolean setServerState() {
		boolean s = Boolean
				.parseBoolean(ConfigurationDatabase.get("CaDoodle", "CSGServerStart", "" + false).toString());
		if (s) {
			if (server == null) {
				String key = ConfigurationDatabase.get("CaDoodle", "CSGClientKey", "").toString();
				String port = ConfigurationDatabase.get("CaDoodle", "CSGClientPort", 3742).toString();
				Path tempFile;
				try {
					tempFile = Files.createTempFile("mytemp", ".txt");
					Files.write(tempFile, key.getBytes(StandardCharsets.UTF_8));
					server = new CSGServer(Integer.parseInt(port), tempFile.toFile());
					new Thread(() -> {
						try {
							com.neuronrobotics.sdk.common.Log.debug("\n\nStarting CSG server\n\n");
							server.start();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							com.neuronrobotics.sdk.common.Log.error(e);
						}
					}).start();
					return true;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					com.neuronrobotics.sdk.common.Log.error(e);
				}

			}
		} else {
			if (server != null)
				try {
					com.neuronrobotics.sdk.common.Log.debug("\n\nStopping CSG server\n\n");
					server.stop();
					server = null;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					com.neuronrobotics.sdk.common.Log.error(e);
				}
		}
		return false;
	}

	public String getLocalIP() {
		String hostAddress = "127.0.0.1";
		try (DatagramSocket socket = new DatagramSocket()) {
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			hostAddress = socket.getLocalAddress().getHostAddress();
			socket.close();
		} catch (Exception e) {
			try {
				hostAddress = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				com.neuronrobotics.sdk.common.Log.error(e1);
			}
		}

		return hostAddress;
	}

	@FXML
	void onStartServer(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.debug("Start a server event");
		ConfigurationDatabase.put("CaDoodle", "CSGServerStart", "" + startServerCheckbox.isSelected());
		String key = apiKey.getText();
		ConfigurationDatabase.put("CaDoodle", "CSGClientKey", key);
		ConfigurationDatabase.put("CaDoodle", "CSGClientPort", portField.getText());
		if (setServerState()) {
			serverIPDisplay.setText("Server started " + getLocalIP());
		} else {
			serverIPDisplay.setText("Server closed");
		}
	}

	@FXML
	void onAdvancedMode(ActionEvent event) {
		boolean selected = advancedSelector.isSelected();
		ConfigurationDatabase.put("CaDoodle", "CaDoodleAdvancedMode", "" + selected);
		mc.setAdvancedMode(selected);
		ConfigurationDatabase.save();
		updateVersionOptions();
	}

	@FXML
	void onAlwaysAsk(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.debug("Ask");
		setExplanationText(OperationResult.ASK);
	}

	@FXML
	void onAlwaysContinue(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.debug("Continue");
		setExplanationText(OperationResult.PRUNE);

	}

	@FXML
	void onAlwaysInsert(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.debug("Insert");
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
	public void onNumberOfSides(ActionEvent event) {
		String text = numberOfSides.getText();
		try {
			int int1 = Integer.parseInt(text);
			if (int1 > 200) {
				Log.error("Fault can not set that number " + int1);
				numberOfSides.setText("200");
				int1 = 200;
			}
			if (int1 < 3) {
				Log.error("Fault can not set that number " + int1);
				numberOfSides.setText("3");
				int1 = 3;
			}
			Log.debug("Setting Default Number of sides to " + int1);
			ConfigurationDatabase.put("CaDoodle", "DefaultNumberOfSides", text);
			ConfigurationDatabase.save();

		} catch (NumberFormatException ex) {
			Log.error(ex);
			numberOfSides.setText("16");
		}
	}

	@FXML
	void onBrowse(ActionEvent event) {
		com.neuronrobotics.sdk.common.Log.debug("Browse For Working Location");
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
			com.neuronrobotics.sdk.common.Log.debug("Selected directory: " + absolutePath);
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
		boolean connect = Boolean
				.parseBoolean(ConfigurationDatabase.get("CaDoodle", "CSGClientConnect", "" + false).toString());
		String key = ConfigurationDatabase.get("CaDoodle", "CSGClientKey", "").toString();
		String host = ConfigurationDatabase.get("CaDoodle", "CSGClientHost", "").toString();
		String port = ConfigurationDatabase.get("CaDoodle", "CSGClientPort", 3742).toString();
		ipaddressField.setText(host);
		portField.setText(port);
		apiKey.setText(key);
		connectServer.setSelected(connect);
		if (connect) {
			onConnectServer(null);
		}
		boolean server = Boolean
				.parseBoolean(ConfigurationDatabase.get("CaDoodle", "CSGServerStart", "" + false).toString());
		startServerCheckbox.setSelected(server);
		connectServer.setDisable(false);
		if (server)
			serverIPDisplay.setText("Server started " + getLocalIP());
		serverStatusBox.getChildren().add(clientDisplay);
		String string = ConfigurationDatabase.get("CaDoodle", "DefaultNumberOfSides", "16").toString();

		try {
			int numberOfSidesInt = Integer.parseInt(string);
			numberOfSides.setText(string);
		} catch (Exception ex) {
			Log.error(ex);
			numberOfSides.setText("16");
			ConfigurationDatabase.put("CaDoodle", "DefaultNumberOfSides", "16");
		}
		bindir = System.getProperty("user.home") + delim() + "bin" + delim() + "CaDoodle-ApplicationInstall"
				+ delim();
		myVersionFileString = bindir + "currentversion.txt";
		String pinFileName = bindir + "pinVersion";
		pinFile = new File(pinFileName);
		boolean toPin = pinFile.exists();
		versionOptions.setDisable(!toPin);
		if (!toPin)
			checkOnLaunch.setSelected(true);
		else
			pinToVersion.setSelected(true);
		updateVersionOptions();
	}

	private void updateVersionOptions() {
		versionOptions.getItems().clear();
		File[] listFiles = new File(bindir).listFiles();
		if (listFiles != null) {
			Arrays.sort(listFiles, (f1, f2) -> {
				java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
				Matcher m1 = p.matcher(f1.getName());
				Matcher m2 = p.matcher(f2.getName());

				if (m1.find() && m2.find()) {
					int cmp = Integer.compare(Integer.parseInt(m2.group(1)), Integer.parseInt(m1.group(1)));
					if (cmp != 0)
						return cmp;

					cmp = Integer.compare(Integer.parseInt(m2.group(2)), Integer.parseInt(m1.group(2)));
					if (cmp != 0)
						return cmp;

					return Integer.compare(Integer.parseInt(m2.group(3)), Integer.parseInt(m1.group(3)));
				}
				return f2.getName().compareTo(f1.getName());
			});
		}
		for (File f : listFiles) {
			if (!f.isDirectory())
				continue;
			String name = f.getName();
			String[] fnames = name.split("\\.");
			if (fnames.length != 3)
				continue;
			try {
				int major=Integer.parseInt(fnames[0]);
				int minor=Integer.parseInt(fnames[1]);
				int bugfix=Integer.parseInt(fnames[2]);
				if((major==0 && minor<26) && !advancedSelector.isSelected())
					continue;
			} catch (NumberFormatException ex) {
				continue;
			}
			versionOptions.getItems().add(name);
		}
		try {
			String myVersionString = new String(Files.readAllBytes(Paths.get(myVersionFileString))).trim();
			versionOptions.getSelectionModel().select(myVersionString);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		JavaFXInitializer.go();
		BowlerStudio.runLater(() -> launch(new MainController(null)));

	}

	public static void launch(MainController mc) {
		SettingsManager.mc = mc;
		if (stage != null && stage.isShowing()) {
			stage.toFront();
			return;
		}
		try {
			// Load the FXML file

			com.neuronrobotics.sdk.common.Log
					.debug("Resource URL: " + ProjectManager.class.getResource("Settings.fxml"));
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
				if (CSGClient.isRunning()) {
					CSGClient.getClient().removeListener(loader.getController());
				}
			});
			// Show the new window
			stage.show();
		} catch (IOException e) {
			com.neuronrobotics.sdk.common.Log.error(e);
			// Handle the exception (e.g., show an error dialog)
		}
	}

	@Override
	public void toSend(CSGRequest request) {
		Label l = new Label("Request " + request.getOperation());
		active.put(request, l);
		com.neuronrobotics.sdk.common.Log.debug(l.getText());
		BowlerStudio.runLater(() -> {
			serverStatusBox.getChildren().add(l);
		});
	}

	@Override
	public void response(CSGResponse response, CSGRequest request) {
		Label label = active.get(request);
		String value = "Request " + request.getOperation() + " result " + response.getOperation();
		com.neuronrobotics.sdk.common.Log.debug(value);
		BowlerStudio.runLater(() -> {
			label.setText(value);
		});
		BowlerStudio.runLater(2000, () -> {
			BowlerStudio.runLater(() -> {
				active.remove(request);
				serverStatusBox.getChildren().remove(label);
			});
		});
	}
}
