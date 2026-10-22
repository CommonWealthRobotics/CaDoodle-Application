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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;

import com.neuronrobotics.bowlerstudio.threed.SceneStyleConfig;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.assets.FontSizeManager;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.OperationResult;
import com.neuronrobotics.sdk.common.Log;
//import com.sun.prism.GraphicsPipeline;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.CSG.OptType;
import eu.mihosoft.vrl.v3d.CSGClient;
import eu.mihosoft.vrl.v3d.CSGRequest;
import eu.mihosoft.vrl.v3d.CSGResponse;
import eu.mihosoft.vrl.v3d.CSGServer;
import eu.mihosoft.vrl.v3d.ICSGClientEvent;
import eu.mihosoft.vrl.v3d.JavaFXInitializer;
import eu.mihosoft.vrl.v3d.Plane;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
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
	private CheckBox manifoldCheck;

	@FXML
	private TextField apiKey;
	@FXML
	private TextField numPoints;

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
	private TextArea insertionExplanation;
	@FXML
	private TextArea prineExplanation;
	@FXML
	private TextArea askExplanation;
	@FXML
	private TextField fontSizeField;

	@FXML
	private TextField ipaddressField;
	@FXML
	private TextField numberOfSides;
	@FXML
	private TextField portField;

	@FXML
	private Label serverIPDisplay;
	@FXML
	private Label renderPipelineDisplay;
	@FXML
	private VBox serverStatusBox;
	@FXML
	private VBox styleButtons;

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
	private RadioButton bugfixOption;
	@FXML
	private ComboBox<String> versionOptions;
	private File pinFile;
	private File bugfixFile;
	private String myVersionFileString;
	private String bindir;
	@FXML
	private TabPane topPane;
	@FXML
	private ComboBox<String> styleOptions;

	// ---------------- UI Elements tab ----------------
	@FXML
	private ComboBox<String> uiLightingMode;
	@FXML
	private TextField uiLightingAmbient;

	@FXML
	private CheckBox uiLightingKeyEnabled;
	@FXML
	private TextField uiLightingKeyIntensity;
	@FXML
	private TextField uiLightingKeyX;
	@FXML
	private TextField uiLightingKeyY;
	@FXML
	private TextField uiLightingKeyZ;

	@FXML
	private CheckBox uiLightingFillEnabled;
	@FXML
	private TextField uiLightingFillIntensity;
	@FXML
	private TextField uiLightingFillX;
	@FXML
	private TextField uiLightingFillY;
	@FXML
	private TextField uiLightingFillZ;

	@FXML
	private CheckBox uiLightingRimEnabled;
	@FXML
	private TextField uiLightingRimIntensity;
	@FXML
	private TextField uiLightingRimX;
	@FXML
	private TextField uiLightingRimY;
	@FXML
	private TextField uiLightingRimZ;

	@FXML
	private CheckBox uiEdgesEnabled;
	@FXML
	private TextField uiEdgesAngleDegrees;
	@FXML
	private ComboBox<String> uiEdgesWidthMode;
	@FXML
	private TextField uiEdgesWidthPx;
	@FXML
	private TextField uiEdgesRadius;
	@FXML
	private TextField uiEdgesBrightness;

	@FXML
	private TextField uiGridSmallVisibleZoomDepth;
	@FXML
	private TextField uiGridSmallBrightnessMultiplier;
	@FXML
	private TextField uiGridSmallOpacityMultiplier;
	@FXML
	private TextField uiGridSmallLineWidthPx;
	@FXML
	private TextField uiGridSmallZOffsetMm;

	@FXML
	private TextField uiGridMajorBrightnessMultiplier;
	@FXML
	private TextField uiGridMajorOpacityMultiplier;
	@FXML
	private TextField uiGridMajorLineWidthPx;

	@FXML
	private ComboBox<String> uiGridMotionMode;
	@FXML
	private TextField uiGridGroundRenderOpacity;

	@FXML
	private TextField uiCameraOrbitMin;
	@FXML
	private TextField uiCameraOrbitMax;
	@FXML
	private TextField uiCameraOrbitHalf;
	@FXML
	private TextField uiCameraOrbitRiseMs;
	@FXML
	private TextField uiCameraOrbitFallMs;

	@FXML
	private TextField uiEdgesSceneSettleDelayMs;
	@FXML
	private TextField uiEdgesCameraSettleDelayMs;
	@FXML
	private TextField uiEdgesTopologyPrecision;
	@FXML
	private TextField uiEdgesStitchTolerance;

	private static final String PARAMS_KEY = "CaDoodle";

	@FXML
	void onFontSize(ActionEvent event) {
		try {
			int int1 = Integer.parseInt(fontSizeField.getText());
			if (int1 < 8)
				int1 = 8;
			if (int1 > 20)
				int1 = 20;

			FontSizeManager.setFontSize(int1);
		} catch (Throwable t) {
			Log.error(t);
		}
	}

	@FXML
	void onTest(ActionEvent event) {
		new Thread(() -> {
			com.neuronrobotics.sdk.common.Log.debug("Starting test");
		}).start();
	}

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
		bugfixFile.delete();
	}

	@FXML
	void onBugfix(ActionEvent event) {
		Log.debug("onPinVersion");
		versionOptions.setDisable(false);
		try {
			bugfixFile.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pinFile.delete();
	}

	@FXML
	void onPinVersionSelect(ActionEvent event) {
		String selectedItem = versionOptions.getSelectionModel().getSelectedItem();
		if (selectedItem == null)
			return;
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
		bugfixFile.delete();
	}

	@FXML
	void checkServerConfigs(KeyEvent event) {
		try {
			// Create a trust manager that ignores certificate errors
			TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			}};

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
		// Read all JavaFX control state on the UI thread before starting the worker.
		connectServer.setDisable(false);
		final boolean selected = connectServer.isSelected();
		final String key = apiKey.getText();
		final String host = ipaddressField.getText();
		final String port = portField.getText();

		new Thread(() -> {
			if (selected) {
				try {
					com.neuronrobotics.sdk.common.Log.debug("Opening Server Connection");
					ConfigurationDatabase.put("CaDoodle", "CSGClientConnect", "" + true);
					ConfigurationDatabase.put("CaDoodle", "CSGClientKey", key);
					ConfigurationDatabase.put("CaDoodle", "CSGClientHost", host);
					ConfigurationDatabase.put("CaDoodle", "CSGClientPort", port);
				} catch (Exception e) {
					com.neuronrobotics.sdk.common.Log.error(e);
				}
			} else {
				com.neuronrobotics.sdk.common.Log.debug("Closing Server Connection");
				ConfigurationDatabase.put("CaDoodle", "CSGClientConnect", "" + false);
			}
			final boolean connected = clientStateSet();
			BowlerStudio.runLater(() -> {
				if (connected) {
					clientDisplay.setText("Client is Connected!");
					CSGClient.getClient().addListener(this);
				} else {
					connectServer.setSelected(false);
					clientDisplay.setText("Server is missing");
				}
			});
		}).start();

	}

	public static boolean clientStateSet() {
		try {
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
		} catch (Exception e) {
			Log.error(e);
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
					server = new CSGServer((int) Double.parseDouble(port), tempFile.toFile());
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
	void onManifoldCheck(ActionEvent event) {
		boolean selected = manifoldCheck.isSelected();
		ConfigurationDatabase.put("CaDoodle", "CaDoodleAdvancedManifold", "" + selected);
		try {
			CSG.setDefaultOptType(selected ? OptType.Manifold3d : OptType.CSG_BOUND);
		} catch (Throwable t) {
			Log.error(t);
			CSG.setDefaultOptType(OptType.CSG_BOUND);
			ConfigurationDatabase.put("CaDoodle", "CaDoodleAdvancedManifold", "" + false).toString();
			manifoldCheck.setSelected(false);
		}
		ConfigurationDatabase.save();
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
			case INSERT :
				insertionExplanation.setVisible(true);
				askExplanation.setVisible(false);
				prineExplanation.setVisible(false);
				break;
			case PRUNE :
				insertionExplanation.setVisible(false);
				askExplanation.setVisible(false);
				prineExplanation.setVisible(true);
				break;
			case ASK :
				insertionExplanation.setVisible(false);
				askExplanation.setVisible(true);
				prineExplanation.setVisible(false);
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
		boolean manifold = Boolean
				.parseBoolean(ConfigurationDatabase.get("CaDoodle", "CaDoodleAdvancedManifold", "" + true).toString());
		manifoldCheck.setSelected(manifold);
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
		bindir = System.getProperty("user.home") + delim() + "bin" + delim() + "CaDoodle-ApplicationInstall" + delim();
		myVersionFileString = bindir + "currentversion.txt";
		String pinFileName = bindir + "pinVersion";
		pinFile = new File(pinFileName);
		bugfixFile = new File(bindir + "pinBugfixVersion");
		boolean toPin = pinFile.exists();
		versionOptions.setDisable(!toPin);
		if (!toPin && !bugfixFile.exists())
			checkOnLaunch.setSelected(true);
		else if (bugfixFile.exists())
			bugfixOption.setSelected(true);
		else
			pinToVersion.setSelected(true);
		ActiveProject ap = mc.getActiveProject();

		ArrayList<String> styleSheetOptions = ActiveProject.getStyleSheetOptions();
		String modeSelected = ConfigurationDatabase.get("CaDoodle", "CaDoodleStyle", styleSheetOptions.get(0))
				.toString();
		styleOptions.setOnAction(event -> {
			ConfigurationDatabase.put("CaDoodle", "CaDoodleStyle", styleOptions.getSelectionModel().getSelectedItem());
			ap.resetAllStyleSheets();
		});
		for (String s : styleSheetOptions) {
			styleOptions.getItems().add(s);
		}
		styleOptions.getSelectionModel().select(modeSelected);
		updateVersionOptions();
		loadUiElements();
		mc.getActiveProject();
		ActiveProject.setStyleSheet(topPane);

		if (ap.isOpen())
			numPoints.setText(ap.get().getTextResolutionPoints() + "");
		fontSizeField.setText(FontSizeManager.getDefaultSize() + "");
		// try {
		// // Optionally dig deeper with internal API (may break across JFX versions)
		// GraphicsPipeline pipe = GraphicsPipeline.getPipeline();
		// String name = pipe != null ? pipe.getClass().getSimpleName() : "unknown";
		// renderPipelineDisplay.setText(name);
		// } catch (Throwable ex) {
		// Log.error(ex);
		// renderPipelineDisplay.setText(ex.getMessage());
		// }

	}

	@FXML
	private void onUiElementChange(ActionEvent event) {
		Object source = event.getSource();
		if (source == null)
			return;
		try {
			if (source == uiLightingMode) {
				String value = uiLightingMode.getSelectionModel().getSelectedItem();
				ConfigurationDatabase.put(PARAMS_KEY, "lighting.mode", value);
				ConfigurationDatabase.save();
				return;
			}
			if (source == uiEdgesWidthMode) {
				String value = uiEdgesWidthMode.getSelectionModel().getSelectedItem();
				ConfigurationDatabase.put(PARAMS_KEY, "edges.widthMode", value);
				ConfigurationDatabase.save();
				return;
			}
			if (source == uiGridMotionMode) {
				String value = uiGridMotionMode.getSelectionModel().getSelectedItem();
				ConfigurationDatabase.put(PARAMS_KEY, "grid.motionMode", value);
				ConfigurationDatabase.save();
				return;
			}
			if (source == uiLightingKeyEnabled) {
				ConfigurationDatabase.put(PARAMS_KEY, "lighting.key.enabled", "" + uiLightingKeyEnabled.isSelected());
				ConfigurationDatabase.save();
				return;
			}
			if (source == uiLightingFillEnabled) {
				ConfigurationDatabase.put(PARAMS_KEY, "lighting.fill.enabled", "" + uiLightingFillEnabled.isSelected());
				ConfigurationDatabase.save();
				return;
			}
			if (source == uiLightingRimEnabled) {
				ConfigurationDatabase.put(PARAMS_KEY, "lighting.rim.enabled", "" + uiLightingRimEnabled.isSelected());
				ConfigurationDatabase.save();
				return;
			}
			if (source == uiEdgesEnabled) {
				ConfigurationDatabase.put(PARAMS_KEY, "edges.enabled", "" + uiEdgesEnabled.isSelected());
				ConfigurationDatabase.save();
				return;
			}
			// The rest are text fields with bounds applied to them
			TextField field = (TextField) source;
			String key = fieldToKey(field);
			if (key == null) {
				Log.error("Unknown control " + field);
				return;
			}
			double value = parseDouble(field.getText());
			value = bound(key, value);
			field.setText("" + value);
			ConfigurationDatabase.put(PARAMS_KEY, key, "" + value);
			ConfigurationDatabase.save();
		} catch (Throwable t) {
			Log.error(t);
		}
	}

	/**
	 * Map a text field to its scene-style.properties key.
	 */
	private String fieldToKey(TextField field) {
		if (field == uiLightingAmbient)
			return "lighting.ambient";
		if (field == uiLightingKeyIntensity)
			return "lighting.key.intensity";
		if (field == uiLightingKeyX)
			return "lighting.key.x";
		if (field == uiLightingKeyY)
			return "lighting.key.y";
		if (field == uiLightingKeyZ)
			return "lighting.key.z";
		if (field == uiLightingFillIntensity)
			return "lighting.fill.intensity";
		if (field == uiLightingFillX)
			return "lighting.fill.x";
		if (field == uiLightingFillY)
			return "lighting.fill.y";
		if (field == uiLightingFillZ)
			return "lighting.fill.z";
		if (field == uiLightingRimIntensity)
			return "lighting.rim.intensity";
		if (field == uiLightingRimX)
			return "lighting.rim.x";
		if (field == uiLightingRimY)
			return "lighting.rim.y";
		if (field == uiLightingRimZ)
			return "lighting.rim.z";
		if (field == uiEdgesAngleDegrees)
			return "edges.angleDegrees";
		if (field == uiEdgesWidthPx)
			return "edges.widthPx";
		if (field == uiEdgesRadius)
			return "edges.radius";
		if (field == uiEdgesBrightness)
			return "edges.brightness";
		if (field == uiGridSmallVisibleZoomDepth)
			return "grid.small.visibleZoomDepth";
		if (field == uiGridSmallBrightnessMultiplier)
			return "grid.small.brightnessMultiplier";
		if (field == uiGridSmallOpacityMultiplier)
			return "grid.small.opacityMultiplier";
		if (field == uiGridSmallLineWidthPx)
			return "grid.small.lineWidthPx";
		if (field == uiGridSmallZOffsetMm)
			return "grid.small.zOffsetMm";
		if (field == uiGridMajorBrightnessMultiplier)
			return "grid.major.brightnessMultiplier";
		if (field == uiGridMajorOpacityMultiplier)
			return "grid.major.opacityMultiplier";
		if (field == uiGridMajorLineWidthPx)
			return "grid.major.lineWidthPx";
		if (field == uiGridGroundRenderOpacity)
			return "grid.ground.renderOpacity";
		if (field == uiCameraOrbitMin)
			return "camera.orbit.min";
		if (field == uiCameraOrbitMax)
			return "camera.orbit.max";
		if (field == uiCameraOrbitHalf)
			return "camera.orbit.half";
		if (field == uiCameraOrbitRiseMs)
			return "camera.orbit.riseMs";
		if (field == uiCameraOrbitFallMs)
			return "camera.orbit.fallMs";
		if (field == uiEdgesSceneSettleDelayMs)
			return "edges.sceneSettleDelayMs";
		if (field == uiEdgesCameraSettleDelayMs)
			return "edges.cameraSettleDelayMs";
		if (field == uiEdgesTopologyPrecision)
			return "edges.topologyPrecision";
		if (field == uiEdgesStitchTolerance)
			return "edges.stitchTolerance";
		return null;
	}

	/**
	 * Bound a value to the ranges described in scene-style.properties.
	 */
	private double bound(String key, double value) {
		switch (key) {
			case "lighting.ambient" :
			case "lighting.key.intensity" :
			case "lighting.fill.intensity" :
			case "lighting.rim.intensity" :
			case "edges.brightness" :
			case "grid.small.brightnessMultiplier" :
			case "grid.major.brightnessMultiplier" :
				return SceneStyleConfig.clamp01(value);
			case "lighting.key.x" :
			case "lighting.key.y" :
			case "lighting.key.z" :
			case "lighting.fill.x" :
			case "lighting.fill.y" :
			case "lighting.fill.z" :
			case "lighting.rim.x" :
			case "lighting.rim.y" :
			case "lighting.rim.z" :
				return Math.max(-1.0, Math.min(1.0, value));
			case "edges.angleDegrees" :
				return Math.max(0.0, Math.min(360.0, value));
			case "edges.widthPx" :
				return Math.max(0.1, Math.min(10.0, value));
			case "edges.radius" :
				return Math.max(0.001, Math.min(100.0, value));
			case "grid.small.visibleZoomDepth" :
				return Math.max(1.0, Math.min(10000.0, value));
			case "grid.small.opacityMultiplier" :
			case "grid.major.opacityMultiplier" :
			case "grid.ground.renderOpacity" :
				return SceneStyleConfig.clamp01(value);
			case "grid.small.lineWidthPx" :
			case "grid.major.lineWidthPx" :
				return Math.max(1.0, Math.min(10.0, value));
			case "grid.small.zOffsetMm" :
				return Math.max(-1.0, Math.min(1.0, value));
			case "camera.orbit.min" :
			case "camera.orbit.max" :
				return Math.max(0.0, Math.min(1.0, value));
			case "camera.orbit.half" :
			case "camera.orbit.riseMs" :
			case "camera.orbit.fallMs" :
			case "edges.sceneSettleDelayMs" :
			case "edges.cameraSettleDelayMs" :
				return Math.max(0.0, Math.min(10000.0, value));
			case "edges.topologyPrecision" :
				return Math.max(0.0, Math.min(1.0, value));
			case "edges.stitchTolerance" :
				return Math.max(0.0, Math.min(1.0, value));
			default :
				return value;
		}
	}

	/**
	 * Load the scene style values into the UI elements tab. Values are read with
	 * the same access pattern used by SceneStyleConfig.
	 */
	private void loadUiElements() {
		// --- lighting.mode ---
		uiLightingMode.getItems().clear();
		uiLightingMode.getItems().addAll("stable", "kevin");
		uiLightingMode.getSelectionModel().select(SceneStyleConfig.getString("lighting.mode", "stable"));

		// --- lighting.ambient ---
		uiLightingAmbient.setText("" + SceneStyleConfig.clamp01(SceneStyleConfig.getDouble("lighting.ambient", 0.47)));

		// --- lighting.key ---
		uiLightingKeyEnabled.setSelected(SceneStyleConfig.getBoolean("lighting.key.enabled", true));
		uiLightingKeyIntensity
				.setText("" + SceneStyleConfig.clamp01(SceneStyleConfig.getDouble("lighting.key.intensity", 0.52)));
		uiLightingKeyX.setText("" + bound("lighting.key.x", SceneStyleConfig.getDouble("lighting.key.x", -0.80)));
		uiLightingKeyY.setText("" + bound("lighting.key.y", SceneStyleConfig.getDouble("lighting.key.y", -1.00)));
		uiLightingKeyZ.setText("" + bound("lighting.key.z", SceneStyleConfig.getDouble("lighting.key.z", -0.45)));

		// --- lighting.fill ---
		uiLightingFillEnabled.setSelected(SceneStyleConfig.getBoolean("lighting.fill.enabled", true));
		uiLightingFillIntensity
				.setText("" + SceneStyleConfig.clamp01(SceneStyleConfig.getDouble("lighting.fill.intensity", 0.16)));
		uiLightingFillX.setText("" + bound("lighting.fill.x", SceneStyleConfig.getDouble("lighting.fill.x", 0.65)));
		uiLightingFillY.setText("" + bound("lighting.fill.y", SceneStyleConfig.getDouble("lighting.fill.y", -0.10)));
		uiLightingFillZ.setText("" + bound("lighting.fill.z", SceneStyleConfig.getDouble("lighting.fill.z", -0.25)));

		// --- lighting.rim ---
		uiLightingRimEnabled.setSelected(SceneStyleConfig.getBoolean("lighting.rim.enabled", true));
		uiLightingRimIntensity
				.setText("" + SceneStyleConfig.clamp01(SceneStyleConfig.getDouble("lighting.rim.intensity", 0.08)));
		uiLightingRimX.setText("" + bound("lighting.rim.x", SceneStyleConfig.getDouble("lighting.rim.x", 0.20)));
		uiLightingRimY.setText("" + bound("lighting.rim.y", SceneStyleConfig.getDouble("lighting.rim.y", 0.90)));
		uiLightingRimZ.setText("" + bound("lighting.rim.z", SceneStyleConfig.getDouble("lighting.rim.z", -0.15)));

		// --- feature edges ---
		uiEdgesEnabled.setSelected(SceneStyleConfig.getBoolean("edges.enabled", true));
		uiEdgesAngleDegrees
				.setText("" + bound("edges.angleDegrees", SceneStyleConfig.getDouble("edges.angleDegrees", 35.0)));
		uiEdgesWidthMode.getItems().clear();
		uiEdgesWidthMode.getItems().addAll("screen", "world");
		uiEdgesWidthMode.getSelectionModel().select(SceneStyleConfig.getString("edges.widthMode", "screen"));
		uiEdgesWidthPx.setText("" + bound("edges.widthPx", SceneStyleConfig.getDouble("edges.widthPx", 1.6)));
		uiEdgesRadius.setText("" + bound("edges.radius", SceneStyleConfig.getDouble("edges.radius", 0.15)));
		uiEdgesBrightness.setText("" + SceneStyleConfig.clamp01(SceneStyleConfig.getDouble("edges.brightness", 0.0)));

		// --- workplane grid small ---
		uiGridSmallVisibleZoomDepth.setText("" + bound("grid.small.visibleZoomDepth",
				SceneStyleConfig.getDouble("grid.small.visibleZoomDepth", 350.0)));
		uiGridSmallBrightnessMultiplier.setText("" + bound("grid.small.brightnessMultiplier",
				SceneStyleConfig.getDouble("grid.small.brightnessMultiplier", 0.85)));
		uiGridSmallOpacityMultiplier.setText("" + bound("grid.small.opacityMultiplier",
				SceneStyleConfig.getDouble("grid.small.opacityMultiplier", 1.0)));
		uiGridSmallLineWidthPx
				.setText("" + bound("grid.small.lineWidthPx", SceneStyleConfig.getDouble("grid.small.lineWidthPx", 2)));
		uiGridSmallZOffsetMm.setText(
				"" + bound("grid.small.zOffsetMm", SceneStyleConfig.getDouble("grid.small.zOffsetMm", -0.005)));

		// --- workplane grid major ---
		uiGridMajorBrightnessMultiplier.setText("" + bound("grid.major.brightnessMultiplier",
				SceneStyleConfig.getDouble("grid.major.brightnessMultiplier", 1.0)));
		uiGridMajorOpacityMultiplier.setText("" + bound("grid.major.opacityMultiplier",
				SceneStyleConfig.getDouble("grid.major.opacityMultiplier", 1.0)));
		uiGridMajorLineWidthPx
				.setText("" + bound("grid.major.lineWidthPx", SceneStyleConfig.getDouble("grid.major.lineWidthPx", 4)));

		// --- grid motion mode ---
		uiGridMotionMode.getItems().clear();
		uiGridMotionMode.getItems().addAll("major", "all", "texture", "hidden");
		uiGridMotionMode.getSelectionModel().select(SceneStyleConfig.getString("grid.motionMode", "texture"));

		// --- grid ground ---
		uiGridGroundRenderOpacity.setText(
				"" + bound("grid.ground.renderOpacity", SceneStyleConfig.getDouble("grid.ground.renderOpacity", 1.0)));

		// --- camera orbit ---
		uiCameraOrbitMin.setText("" + bound("camera.orbit.min", SceneStyleConfig.getDouble("camera.orbit.min", 0.012)));
		uiCameraOrbitMax.setText("" + bound("camera.orbit.max", SceneStyleConfig.getDouble("camera.orbit.max", 0.080)));
		uiCameraOrbitHalf
				.setText("" + bound("camera.orbit.half", SceneStyleConfig.getDouble("camera.orbit.half", 500)));
		uiCameraOrbitRiseMs
				.setText("" + bound("camera.orbit.riseMs", SceneStyleConfig.getDouble("camera.orbit.riseMs", 250)));
		uiCameraOrbitFallMs
				.setText("" + bound("camera.orbit.fallMs", SceneStyleConfig.getDouble("camera.orbit.fallMs", 120)));

		// --- feature edge timing ---
		uiEdgesSceneSettleDelayMs.setText(
				"" + bound("edges.sceneSettleDelayMs", SceneStyleConfig.getDouble("edges.sceneSettleDelayMs", 500.0)));
		uiEdgesCameraSettleDelayMs.setText(""
				+ bound("edges.cameraSettleDelayMs", SceneStyleConfig.getDouble("edges.cameraSettleDelayMs", 120.0)));
		uiEdgesTopologyPrecision.setText(
				"" + bound("edges.topologyPrecision", SceneStyleConfig.getDouble("edges.topologyPrecision", 0.00001)));
		uiEdgesStitchTolerance.setText(
				"" + bound("edges.stitchTolerance", SceneStyleConfig.getDouble("edges.stitchTolerance", 0.002)));
	}

	/**
	 * Parse a text field, returning the fallback when the text is not a number.
	 */
	private double parseDouble(String text) {
		try {
			double value = Double.parseDouble(text);
			return Double.isFinite(value) ? value : 0.0;
		} catch (Throwable t) {
			return 0.0;
		}
	}

	@FXML
	private void onPointInCurve(ActionEvent event) {
		try {
			double int1 = Double.parseDouble(numPoints.getText());
			if (int1 > 2)
				int1 = 2;
			if (int1 < Plane.getEPSILON() * 100)
				int1 = Plane.getEPSILON() * 100;
			mc.getActiveProject().get().setTextResolutionPoints(int1);
			mc.getActiveProject().regenerateAll();
		} catch (Throwable t) {
			Log.error(t);
		}
	}

	@FXML
	private void onLang(ActionEvent event) {
		new Thread(() -> {
			Locale toUse = ActiveProject.showLanguageSelectionPopup(ActiveProject.getCurrentLocale());
			String country = toUse.getLanguage().toLowerCase();
			ConfigurationDatabase.put("CaDoodle", "CaDoodleLangauge", country);
		}).start();
	}

	private void updateVersionOptions() {
		try {
			versionOptions.getItems().clear();
		} catch (Exception ex) {
			Log.error(ex);
		}
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
				int major = Integer.parseInt(fnames[0]);
				int minor = Integer.parseInt(fnames[1]);
				int bugfix = Integer.parseInt(fnames[2]);
				if ((major == 0 && minor < 26) && !advancedSelector.isSelected())
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
					SettingsManager.class.getClassLoader().getResource("com/commonwealthrobotics/Settings.fxml"),
					ActiveProject.getLangaugePack());
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
			javafx.geometry.Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

			double width = Math.min(1500, screenBounds.getWidth());
			double height = Math.min(950, screenBounds.getHeight());

			stage.setWidth(width);
			stage.setHeight(height);
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
