package com.commonwealthrobotics;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.jgit.api.errors.GitAPIException;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import static com.neuronrobotics.bowlerstudio.scripting.DownloadManager.*;

import com.neuronrobotics.bowlerstudio.BowlerKernel;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.SplashManager;
import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.assets.FontSizeManager;
import com.neuronrobotics.bowlerstudio.assets.IFontSizeReciver;
import com.neuronrobotics.bowlerstudio.scripting.DownloadManager;
import com.neuronrobotics.bowlerstudio.scripting.IApprovalForDownload;
import com.neuronrobotics.bowlerstudio.scripting.IDownloadManagerEvents;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.FailedToApplyOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.IAcceptPruneForward;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICadoodleSaveStatusUpdate;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.OperationResult;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.RandomStringFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.SaveOverwriteException;
import com.neuronrobotics.bowlerstudio.util.FileChangeWatcher;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.Log;
import com.neuronrobotics.video.OSUtil;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

public class ActiveProject implements ICaDoodleStateUpdate {

	private static final String DEFAULT = "Default";
	private boolean isOpenValue = true;
	private boolean disableRegenerate = false;
	private CaDoodleFile fromFile = null;
	// private ICaDoodleStateUpdate listener;
	private ArrayList<ICaDoodleStateUpdate> listeners = new ArrayList<ICaDoodleStateUpdate>();
	// private boolean isAlwaysAccept=false;
	// private boolean isAlwaysInsert=false;
	private Thread autosaveThread = null;
	private boolean needsSave = false;
	private long timeOfLastUpdate = 0;
	private Thread lastUpdate = null;
	private boolean saving;
	private static HashSet<Region> panes = new HashSet<Region>();

	public ActiveProject() {
		// this.listener = listener;
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
		ActiveProject ap = this;
		DownloadManager.setApproval(new IApprovalForDownload() {
			private ButtonType buttonType = null;

			@Override
			public boolean get(String name, String url) {

				buttonType = null;
				boolean isVis = SplashManager.isVisibleSplash();
				BooleanSupplier p = SplashManager.getClosePreventer();
				SplashManager.setClosePreventer(() -> {
					return false;
				});

				while (SplashManager.isVisibleSplash()) {
					Log.debug("Closing splash");
					SplashManager.closeSplash();
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				SplashManager.setClosePreventer(p);
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
					setStyleSheet((Region) root);
					Optional<ButtonType> result = alert.showAndWait();
					buttonType = result.get();
					alert.close();
				});

				while (buttonType == null) {
					try {
						Thread.sleep(20);

						SplashManager.closeSplash();
					} catch (InterruptedException e) {
						// Auto-generated catch block
						com.neuronrobotics.sdk.common.Log.error(e);
					}

				}
				if (isVis)
					SplashManager.renderSplashFrame(0, "Downloading " + name);
				return buttonType.equals(ButtonType.OK);
			}

			@Override
			public void onInstallFail(String url) {
				if (!ap.get().isInitialized()) {
					return;
				}
				try {
					BowlerStudio.openExternalWebpage(new URL(url));
				} catch (MalformedURLException e) {
					// Auto-generated catch block
					com.neuronrobotics.sdk.common.Log.error(e);
				}
			}

			public void notifyOfFailure(String name) {
				BowlerKernel.runLater(() -> {
					Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
					alert.setTitle("Message");
					alert.setHeaderText("FAILED to install " + name + " plugin");
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
			}
		});
	}

	public Thread regenerateAll() throws FailedToApplyOperation {
		return regenerateFrom(get().getOperations().get(0));
	}

	public Thread regenerateFrom(CaDoodleOperation source) throws FailedToApplyOperation {
		if (disableRegenerate)
			return null;
		Thread t = get().regenerateFrom(source);
		if (t == null)
			return null;
		new Thread(() -> {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// Auto-generated catch block
				com.neuronrobotics.sdk.common.Log.error(e);
			}
			if (t.isAlive()) {
				while (!SplashManager.isVisibleSplash()) {
					SplashManager.renderSplashFrame((int) (get().getPercentInitialized() * 100), " Re-Generating");
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// Auto-generated catch block
						com.neuronrobotics.sdk.common.Log.error(e);
					}
				}
			} else {
				return;
			}
			try {
				t.join();
			} catch (InterruptedException e) {
				// Auto-generated catch block
				com.neuronrobotics.sdk.common.Log.error(e);
			}
			SplashManager.closeSplash();
		}).start();
		return t;
	}

	public Thread addOp(CaDoodleOperation h) {
		Thread t = get().addOperation(h);
		timeoutThread(h, t);
		return t;
	}

	private void timeoutThread(CaDoodleOperation h, Thread t) {
		new Thread(() -> {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// Auto-generated catch block
				com.neuronrobotics.sdk.common.Log.error(e);
			}
			if (t.isAlive()) {
				SplashManager.renderSplashFrame(50, h.getType() + " running");
			} else {
				return;
			}
			try {
				t.join();
			} catch (InterruptedException e) {
				// Auto-generated catch block
				com.neuronrobotics.sdk.common.Log.error(e);
			}

			SplashManager.closeSplash();
		}).start();
	}

	public ActiveProject clearListeners() {
		listeners.clear();
		return this;
	}

	public ActiveProject removeListener(ICaDoodleStateUpdate l) {
		if (listeners.contains(l))
			listeners.remove(l);
		return this;
	}

	public ActiveProject addListener(ICaDoodleStateUpdate l) {
		if (!listeners.contains(l))
			listeners.add(l);
		return this;
	}

	public CaDoodleFile setActiveProject(File f) throws Exception {
		if (fromFile != null) {
			fromFile.removeListener(this);
		}
		if (autosaveThread != null)
			autosaveThread.interrupt();
		autosaveThread = null;
		ConfigurationDatabase.put("CaDoodle", "CaDoodleActiveFile", f.getAbsolutePath());
		return loadActive();
	}

	public File getActiveProject() throws Exception {
		Object object = ConfigurationDatabase.get("CaDoodle", "CaDoodleActiveFile", null);
		if (object == null) // Fix legacy typo, try again with corrected spelling
			object = ConfigurationDatabase.get("CaDoodle", "CaDoodleActiveFile", null);

		if (object == null)
			return newProject();
		String string = object.toString();
		com.neuronrobotics.sdk.common.Log.debug("Loading file " + string);
		File file = new File(string);
		if (file.exists())
			return file;
		return newProject();
	}

	// Helper method to create styled option buttons with descriptions
	private HBox createOptionButton(CheckBox always, String buttonText, String description, String tooltipText,
			EventHandler<ActionEvent> value) {
		HBox buttonContainer = new HBox(5);
		buttonContainer.setAlignment(Pos.CENTER_LEFT);

		Button button = new Button(buttonText);
		button.setMaxWidth(Double.MAX_VALUE);
		button.setPrefHeight(40);
		button.setOnAction(value);
		// button.setStyle("-fx-font-weight: bold;");

		Label descriptionLabel = new Label(description);
		descriptionLabel.setWrapText(true);

		buttonContainer.getChildren().addAll(button, descriptionLabel);
		if (always != null)
			buttonContainer.getChildren().add(always);
		// Set tooltip
		Tooltip tooltip = new Tooltip(tooltipText);
		// tooltip.setShowDelay(Duration.millis(300));
		// button.setTooltip(tooltip);

		// Create the final button that contains both the button and description
		Button optionButton = new Button();
		optionButton.setMaxWidth(Double.MAX_VALUE);
		optionButton.setAlignment(Pos.CENTER_LEFT);
		optionButton.setPadding(new Insets(10));

		return buttonContainer;
	}

	public CaDoodleFile loadActive() throws Exception {
		if (fromFile != null) {
			fromFile.close();
			fromFile = null;
		}

		FileChangeWatcher.clearAll();
		try {
			fromFile = CaDoodleFile.fromFile(getActiveProject(), this, false);
			fromFile.setSaveUpdate(new ICadoodleSaveStatusUpdate() {

				@Override
				public void renderSplashFrame(int percent, String message) {
					SplashManager.renderSplashFrame(percent, message);
				}
			});
			fromFile.setAccept(new IAcceptPruneForward() {
				@Override
				public OperationResult accept() {
					if (!isAdvancedMode()) {
						return OperationResult.PRUNE;
					}
					OperationResult insertionStrat = OperationResult.fromString((String) ConfigurationDatabase
							.get("CaDoodle", "Insertion Stratagy", OperationResult.ASK.name()));
					if (insertionStrat == OperationResult.ASK)
						return ChangeOptionsController.launch();
					return insertionStrat;
				}
			});
			// fromFile.setImageEngine(new ThumbnailImage());
			return fromFile;
		} catch (Exception e) {
			newProject();
			return fromFile;
		}
	}

	private void save(CaDoodleFile cf) throws SaveOverwriteException {
		saving = true;
		try {
			cf.setSelf(getActiveProject());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			com.neuronrobotics.sdk.common.Log.error(e);
		}
		try {
			cf.save();
		} catch (IOException e) {
			// Auto-generated catch block
			com.neuronrobotics.sdk.common.Log.error(e);
		}
		saving = false;
	}

	public boolean isOpen() {
		// Auto-generated method stub
		return isOpenValue;
	}

	public WritableImage getImage() {
		return fromFile.getImage();
	}

	public CaDoodleFile get() {
		if (fromFile == null) {
			throw new RuntimeException("Can not access file before it is loaded");
		}
		return fromFile;
	}

	@Override
	public void onUpdate(List<CSG> currentState, CaDoodleOperation source, CaDoodleFile file) {
		if (lastUpdate != null)
			lastUpdate.interrupt();
		timeOfLastUpdate = System.currentTimeMillis();
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				l.onUpdate(currentState, source, file);
			} catch (Throwable e) {
				com.neuronrobotics.sdk.common.Log.error(e);
			}
		}
		//		try {
		//			Thread.sleep(16);
		//		} catch (InterruptedException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
	}

	@Override
	public void onSaveSuggestion() {
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				l.onSaveSuggestion();
			} catch (Throwable e) {
				com.neuronrobotics.sdk.common.Log.error(e);
			}
		}
	}

	@Override
	public void onInitializationDone() {
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				l.onInitializationDone();
			} catch (Throwable e) {
				com.neuronrobotics.sdk.common.Log.error(e);
			}
		}
	}

	public static File getWorkingDir() {
		String relative = ScriptingEngine.getWorkspace().getAbsolutePath();
		if (OSUtil.isWindows()) {
			relative = Paths.get(System.getProperty("user.home"), "Documents").toString();;
		}
		File defaultFile = new File(relative + delim() + "MyCaDoodleProjects" + delim());
		defaultFile.mkdirs();
		return new File(
				(String) ConfigurationDatabase.get("CaDoodle", "CaDoodleWorkspace", defaultFile.getAbsolutePath()));
	}

	public List<CaDoodleFile> getProjects() throws IOException {
		String directoryPath = getWorkingDir().getAbsolutePath();
		com.neuronrobotics.sdk.common.Log.debug("Loading workspace from " + directoryPath);
		File dir = new File(directoryPath);
		List<CaDoodleFile> list = new ArrayList<>();

		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory() && !file.equals(dir) && !file.getName().startsWith(".")) {
					File[] contents = file.listFiles();
					for (File f : contents) {
						if (f.getName().toLowerCase().endsWith(".doodle")) {
							try {
								list.add(CaDoodleFile.fromFile(f, null, false));
							} catch (Exception e) {
								// Auto-generated catch block
								com.neuronrobotics.sdk.common.Log.error(e);
							}
						}
					}
				}
			}
		}
		HashSet<String> externals = Main.getOptionalProjects();

		for (String s : externals) {
			File f = new File(s);
			if (f.exists() && f.getName().toLowerCase().endsWith(".doodle")) {
				try {
					list.add(CaDoodleFile.fromFile(f, null, false));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					com.neuronrobotics.sdk.common.Log.error(e);
				}
			}
		}
		Collections.sort(list, new Comparator<CaDoodleFile>() {
			@Override
			public int compare(CaDoodleFile c1, CaDoodleFile c2) {
				// Compare in reverse order for newest first
				return Long.compare(c2.getTimeCreated(), c1.getTimeCreated());
			}
		});
		return list;
	}

	public File newProject() throws IOException {
		List<CaDoodleFile> proj = getProjects();
		String nextRandomName = RandomStringFactory.getNextRandomName();
		String pathname = "Doodle-" + proj.size() + "-" + nextRandomName;
		File np = new File(getWorkingDir().getAbsolutePath() + delim() + pathname);
		np.mkdirs();
		com.neuronrobotics.sdk.common.Log.debug("New Doodle Directory " + np.getAbsolutePath());
		File nf = new File(np.getAbsolutePath() + delim() + pathname + ".doodle");
		nf.createNewFile();
		com.neuronrobotics.sdk.common.Log.debug("New Doodle File " + nf.getAbsolutePath());
		try {
			CaDoodleFile cf = setActiveProject(nf);
			cf.setProjectName(nextRandomName);
			cf.save(true);
		} catch (Exception e) {
			// Auto-generated catch block
			com.neuronrobotics.sdk.common.Log.error(e);
		}
		return nf;
	}

	public boolean isDisableRegenerate() {
		return disableRegenerate;
	}

	public void setDisableRegenerate(boolean disableRegenerate) {
		this.disableRegenerate = disableRegenerate;
	}

	@Override
	public void onWorkplaneChange(TransformNR newWP) {
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				l.onWorkplaneChange(newWP);
			} catch (Throwable e) {
				com.neuronrobotics.sdk.common.Log.error(e);
			}
		}
	}

	@Override
	public void onInitializationStart() {
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				l.onInitializationStart();
			} catch (Throwable e) {
				com.neuronrobotics.sdk.common.Log.error(e);
			}
		}
	}

	@Override
	public void onRegenerateDone() {
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				// TickToc.tic("Start "+l.getClass());
				l.onRegenerateDone();
				// TickToc.tic("End "+l.getClass());
			} catch (Throwable e) {
				com.neuronrobotics.sdk.common.Log.error(e);
			}
		}
	}

	@Override
	public void onRegenerateStart(CaDoodleOperation source) {
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				l.onRegenerateStart(source);
			} catch (Throwable e) {
				com.neuronrobotics.sdk.common.Log.error(e);
			}
		}
	}

	@Override
	public void onTimelineUpdate(int num, WritableImage image) {
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				l.onTimelineUpdate(num, image);
			} catch (Throwable e) {
				com.neuronrobotics.sdk.common.Log.error(e);
			}
		}
	}

	public static void unzip(String zipFilePath, String destDir) throws IOException {
		Path destPath = Paths.get(destDir);
		if (Files.exists(destPath)) {
			throw new IOException("Destination directory already exists: " + destDir);
		}
		Files.createDirectories(destPath);

		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
			ZipEntry zipEntry = zis.getNextEntry();
			while (zipEntry != null) {
				Path newPath = zipSlipProtect(zipEntry, destPath);
				if (zipEntry.isDirectory()) {
					Files.createDirectories(newPath);
				} else {
					if (newPath.getParent() != null) {
						if (Files.notExists(newPath.getParent())) {
							Files.createDirectories(newPath.getParent());
						}
					}
					Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
				}
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
		}
	}

	// Protects against Zip Slip vulnerability
	private static Path zipSlipProtect(ZipEntry zipEntry, Path targetDir) throws IOException {
		Path targetDirResolved = targetDir.resolve(zipEntry.getName());
		Path normalizedPath = targetDirResolved.normalize();
		if (!normalizedPath.startsWith(targetDir)) {
			throw new IOException("Bad zip entry: " + zipEntry.getName());
		}
		return normalizedPath;
	}

	public void loadFromZip(File file) {
		String name = file.getName().substring(0, file.getName().length() - 4);
		int index = 0;
		File targetDir = null;
		do {
			targetDir = new File(getWorkingDir() + delim() + name + "_" + index);
			index++;
			Log.debug("CHecking for file: " + targetDir);
		} while (targetDir.exists());
		try {
			unzip(file.getAbsolutePath(), targetDir.getAbsolutePath());
			File[] files = targetDir.listFiles();
			for (File f : files) {
				if (f.getName().toLowerCase().endsWith(".doodle")) {
					setActiveProject(f);
					System.out.println("Active file set to " + f.getAbsolutePath());
					new Thread(() -> get().initialize()).start();
					return;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			com.neuronrobotics.sdk.common.Log.error(e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			com.neuronrobotics.sdk.common.Log.error(e);
		}
		Log.debug("Extraction complete, NO DOODLE FOUND IN TL!");
	}

	public void save() {
		// com.neuronrobotics.sdk.common.Log.error("Save Requested");
		needsSave = true;
		// new Exception("Auto-save called here").printStackTrace();
		if (autosaveThread == null) {
			autosaveThread = new Thread(() -> {
				while (!get().isInitialized()) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
						return;
					}
				}

				while (isOpen()) {
					if (needsSave && (get().timeSinceLastUpdate() > 1000)
							&& get().getPercentInitialized() >= .9999999) {
						ICadoodleSaveStatusUpdate saveDisplay = get().getSaveUpdate();
						get().setSaveUpdate(null);

						Thread t = new Thread(() -> {
							com.neuronrobotics.sdk.common.Log.debug("Auto save " + get().getSelf().getAbsolutePath());
							try {
								save(get());
							} catch (Throwable e) {
								Log.error(e);
							}
						});
						t.start();

						needsSave = false;
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							com.neuronrobotics.sdk.common.Log.error(e);
							return;
						}

						get().setSaveUpdate(saveDisplay);
						if (saving) {
							do {
								try {
									Thread.sleep(20);
								} catch (InterruptedException e) {
									return;
								}
							} while (t.isAlive());
						}
						com.neuronrobotics.sdk.common.Log.debug("Save finished");
						SplashManager.closeSplash();
					}

					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// Auto-generated catch block
						// com.neuronrobotics.sdk.common.Log.error(e);
						return;
					}
				}
			});

			autosaveThread.setName("Auto-save thread");
			autosaveThread.start();
		}
	}

	public boolean isAdvancedMode() {
		return Boolean
				.parseBoolean(ConfigurationDatabase.get("CaDoodle", "CaDoodleAdvancedMode", "" + false).toString());

	}

	public void resetAllStyleSheets() {
		for (Iterator<Region> iterator = panes.iterator(); iterator.hasNext();) {
			Region p = iterator.next();
			setStyleSheet(p);
		}
	}

	public static ResourceBundle getLangaugePack() {

		Locale toUse = getCurrentLocale();
		try {
			List<Locale> locales = getAvailableLocales();
			if (toUse == null)
				for (Locale l : locales) {
					if (l.getLanguage().contentEquals(Locale.getDefault().getLanguage())) {
						toUse = l;
						break;
					}
				}
		} catch (Throwable t) {
			Log.error(t);
		}
		if (toUse == null)
			toUse = showLanguageSelectionPopup(null);
		if (toUse == null)
			toUse = Locale.of("en");
		String country = toUse.getLanguage().toLowerCase();
		ConfigurationDatabase.put("CaDoodle", "CaDoodleLangauge", country);
		return ResourceBundle.getBundle("lang.Messages", toUse);

	}

	public static Locale getCurrentLocale() {
		String stored = ConfigurationDatabase.get("CaDoodle", "CaDoodleLangauge", "").toString();
		Locale toUse = stored.length() == 0 ? null : Locale.of(stored);
		return toUse;
	}

	public static Locale showLanguageSelectionPopup(Locale starting) {

		List<Locale> locales = getAvailableLocales();

		ComboBox<Locale> comboBox = new ComboBox<Locale>();
		comboBox.getItems().addAll(locales);

		comboBox.setCellFactory(list -> new ListCell<Locale>() {
			@Override
			protected void updateItem(Locale item, boolean empty) {
				super.updateItem(item, empty);

				if (empty || item == null) {
					setText(null);
				} else {
					setText(item.getDisplayLanguage(item));
				}
			}
		});

		comboBox.setButtonCell(new ListCell<Locale>() {
			@Override
			protected void updateItem(Locale item, boolean empty) {
				super.updateItem(item, empty);

				if (empty || item == null) {
					setText(null);
				} else {
					setText(item.getDisplayLanguage(item));
				}
			}
		});
		comboBox.getSelectionModel().selectFirst();
		if (starting != null)
			for (Locale l : locales)
				if (l.getLanguage().contentEquals(starting.getLanguage()))
					comboBox.getSelectionModel().select(l);;

		Button ok = new Button("OK");
		ok.setDefaultButton(true);

		VBox myroot = new VBox(15, new Label("Select Language"), comboBox, ok);

		myroot.setPadding(new Insets(20));
		myroot.setAlignment(Pos.CENTER);
		AnchorPane root = new AnchorPane();
		root.getChildren().add(myroot);
		final Locale[] result = new Locale[1];
		myroot.getStyleClass().add("vbox");
		AnchorPane.setTopAnchor(myroot, 0.0);
		AnchorPane.setBottomAnchor(myroot, 0.0);
		AnchorPane.setLeftAnchor(myroot, 0.0);
		AnchorPane.setRightAnchor(myroot, 0.0);
		BowlerStudio.runLater(() -> {
			Stage stage = new Stage();
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.initStyle(StageStyle.UTILITY);
			stage.setTitle("Language Selection");
			stage.setScene(new Scene(root));
			stage.setResizable(false);
			setStyleSheet(root);
			ok.setOnAction(e -> {
				result[0] = comboBox.getValue();
				stage.close();
			});

			stage.setOnCloseRequest(e -> {
				result[0] = Locale.of("en");
			});

			stage.showAndWait();
			panes.remove(root);

		});
		while (result[0] == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		return result[0];
	}

	public static List<Locale> getAvailableLocales() {
		List<Locale> locales = new ArrayList<>();

		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();

			URL url = cl.getResource("lang");
			if (url == null)
				return locales;

			Path path;

			if (url.toURI().getScheme().equals("jar")) {

				String uri = url.toURI().toString();
				String jarUri = uri.substring(0, uri.indexOf("!"));

				FileSystem fs;

				try {
					fs = FileSystems.getFileSystem(URI.create(jarUri));
				} catch (FileSystemNotFoundException e) {
					fs = FileSystems.newFileSystem(URI.create(jarUri), Map.of());
				}

				path = fs.getPath("/lang");

			} else {
				path = Paths.get(url.toURI());
			}

			try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "Messages_*.properties")) {

				List<Path> files = new ArrayList<>();
				stream.forEach(files::add);
				files.sort(Comparator.comparing(p -> p.getFileName().toString()));

				for (Path file : files) {

					String name = file.getFileName().toString();

					String code = name.replace("Messages_", "").replace(".properties", "");

					locales.add(Locale.of(code));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return locales;
	}

	public static void setStyleSheet(Region node) {
		if (!panes.contains(node)) {
			FontSizeManager.addListener(new IFontSizeReciver() {
				@Override
				public void fontSizeChange(int tmp) {
					node.setStyle("-fx-font-size: " + tmp + "pt");
				}
			});
			panes.add(node);
		}

		String sheet = ConfigurationDatabase.get("CaDoodle", "CaDoodleStyle", DEFAULT).toString();

		String url = Main.class.getResource("/com/commonwealthrobotics/stylesheet.css").toExternalForm();
		if (!sheet.contentEquals(DEFAULT)) {
			try {
				File fileFromGit = ScriptingEngine
						.fileFromGit("https://github.com/CommonWealthRobotics/Style-Cadoodle.git", sheet + ".css");
				if (fileFromGit.exists())
					url = fileFromGit.toURI().toURL().toExternalForm();
			} catch (GitAPIException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		node.getStylesheets().setAll(url);
	}

	public static ArrayList<String> getStyleSheetOptions() {
		ArrayList<String> sheets = new ArrayList<String>();
		sheets.add(DEFAULT);
		try {
			ArrayList<String> filesInGit = ScriptingEngine
					.filesInGit("https://github.com/CommonWealthRobotics/Style-Cadoodle.git");
			for (String s : filesInGit) {
				if (s.endsWith(".css")) {
					sheets.add(s.substring(0, s.length() - 4));
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Collections.sort(sheets, String.CASE_INSENSITIVE_ORDER);
		return sheets;
	}
}
