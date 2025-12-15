package com.commonwealthrobotics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.swing.filechooser.FileSystemView;

import static com.neuronrobotics.bowlerstudio.scripting.DownloadManager.*;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import com.neuronrobotics.bowlerstudio.BowlerKernel;
import com.neuronrobotics.bowlerstudio.SplashManager;
import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.assets.FontSizeManager;
import com.neuronrobotics.bowlerstudio.scripting.DownloadManager;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.IAcceptPruneForward;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICadoodleSaveStatusUpdate;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.OperationResult;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.RandomStringFactory;
import com.neuronrobotics.bowlerstudio.util.FileChangeWatcher;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.TickToc;
import com.neuronrobotics.video.OSUtil;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.stage.Stage;

public class ActiveProject implements ICaDoodleStateUpdate {

	private boolean isOpenValue = true;
	private boolean disableRegenerate = false;
	private CaDoodleFile fromFile=null;
	// private ICaDoodleStateUpdate listener;
	private ArrayList<ICaDoodleStateUpdate> listeners = new ArrayList<ICaDoodleStateUpdate>();
//	private boolean isAlwaysAccept=false;
//	private boolean isAlwaysInsert=false;
	
	public ActiveProject() {
		// this.listener = listener;

	}

	public Thread regenerateFrom(CaDoodleOperation source) {
		if (disableRegenerate)
			return null;
		Thread t = get().regenerateFrom(source);
		if (t == null)
			return null;
		new Thread(() -> {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// Auto-generated catch block
				com.neuronrobotics.sdk.common.Log.error(e);
			}
			if (t.isAlive()) {
				while (!SplashManager.isVisableSplash()) {
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
		Thread t = get().addOpperation(h);
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
		ConfigurationDatabase.put("CaDoodle", "CaDoodleacriveFile", f.getAbsolutePath());
		return loadActive();
	}

	private File getActiveProject() throws Exception {
		Object object = ConfigurationDatabase.get("CaDoodle", "CaDoodleacriveFile", null);
		if (object == null)
			return newProject();
		String string = object.toString();
		com.neuronrobotics.sdk.common.Log.debug("Loading file "+string);
		File file = new File(string);
		if(file.exists())
			return file;
		return newProject();
	}

	// Helper method to create styled option buttons with descriptions
	private HBox createOptionButton(CheckBox always,String buttonText, String description, String tooltipText, EventHandler<ActionEvent> value) {
		HBox buttonContainer = new HBox(5);
		buttonContainer.setAlignment(Pos.CENTER_LEFT);

		Button button = new Button(buttonText);
		button.setMaxWidth(Double.MAX_VALUE);
		button.setPrefHeight(40);
		button.setOnAction(value);
		//button.setStyle("-fx-font-weight: bold;");

		Label descriptionLabel = new Label(description);
		descriptionLabel.setWrapText(true);

		buttonContainer.getChildren().addAll(button, descriptionLabel);
		if(always!=null)
			buttonContainer.getChildren().add(always);
		// Set tooltip
		Tooltip tooltip = new Tooltip(tooltipText);
		//tooltip.setShowDelay(Duration.millis(300));
		//button.setTooltip(tooltip);

		// Create the final button that contains both the button and description
		Button optionButton = new Button();
		optionButton.setMaxWidth(Double.MAX_VALUE);
		optionButton.setAlignment(Pos.CENTER_LEFT);
		optionButton.setPadding(new Insets(10));

		return buttonContainer;
	}

	public CaDoodleFile loadActive() throws Exception {
		if(fromFile!=null) {
			fromFile.close();
			fromFile=null;
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
				private OperationResult operationResult = null;

				@Override
				public OperationResult accept() {
					OperationResult insertionStrat = OperationResult.fromString((String)ConfigurationDatabase.get("CaDoodle", "Insertion Stratagy",OperationResult.ASK.name()));
					if(insertionStrat!=OperationResult.ASK)
						return insertionStrat;
					operationResult = null;
					boolean isVis = SplashManager.isVisableSplash();
					SplashManager.closeSplash();
					BowlerKernel.runLater(() -> {
						Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
						alert.setTitle("Change Options");
						alert.setHeaderText("You made a change. How would you like to proceed?");
						alert.setContentText("Please select one of the following options:");

						// Create custom buttons with specific labels
						ButtonType eraseButton = new ButtonType("Erase");
						ButtonType insertButton = new ButtonType("Insert");
						ButtonType abortButton = new ButtonType("Abort change");

						// Set the buttons for the alert
						alert.getButtonTypes().setAll(eraseButton, insertButton, abortButton);

						// Get the dialog pane to customize
						DialogPane dialogPane = alert.getDialogPane();

						// Create a VBox to hold descriptions and stack buttons vertically
						VBox contentBox = new VBox(10);
						contentBox.setPadding(new Insets(10, 10, 10, 10));
						CheckBox alwaysPrune =new CheckBox("Always Continue");
						CheckBox alwaysInsert =new CheckBox("Always Insert");
						alwaysInsert.setOnAction(e->alwaysPrune.setSelected(false));
						alwaysPrune.setOnAction(e->alwaysInsert.setSelected(false));

						// Create labeled buttons with descriptions
						HBox eraseOptionBtn = createOptionButton(null,"Continue From Here",
								"Replace subsequent work with this change.\nThis will remove any work you've done after this point.",
								"Erase will prune the subsequent operations and replace them with this change.",
								e -> {
									this.operationResult= OperationResult.PRUNE;
									alert.close();
								});

						HBox insertOptionBtn = createOptionButton(null,"Insert",
								"Insert this change at the current position.\nYour subsequent work will be preserved.",
								"Insert will add this operation at the current position while keeping subsequent operations.",
								e -> {
									this.operationResult= OperationResult.INSERT;
									alert.close();
								});

						HBox abortOptionBtn = createOptionButton(null,"Abort change",
								"Cancel this change and keep your work as is.",
								"Abort will discard this change and maintain your current work.",
								e -> {
									this.operationResult= OperationResult.ABORT;
									alert.close();
								});

						// Add buttons to the VBox
						contentBox.getChildren().addAll(new Label("Choose how to handle your change: (Check the settings menu for default behavior)"), eraseOptionBtn,
								insertOptionBtn, abortOptionBtn);

						// Replace the default content with our custom content
						dialogPane.setContent(contentBox);

						// Get the root node and stage for styling
						Node root = dialogPane;
						Stage stage = (Stage) dialogPane.getScene().getWindow();

						// Handle close request properly
						stage.setOnCloseRequest(ev -> alert.hide());

						// Set up font size management
						FontSizeManager.addListener(fontNum -> {
							int tmp = fontNum - 10;
							if (tmp < 12)
								tmp = 12;
							root.setStyle("-fx-font-size: " + tmp + "pt");
							dialogPane.applyCss();
							dialogPane.layout();
							stage.sizeToScene();
						});

						// Hide the default buttons as we're using custom ones
						dialogPane.getButtonTypes().clear();
						dialogPane.getButtonTypes().add(ButtonType.CANCEL); // Add a hidden button to make dialog work
						Node buttonBar = dialogPane.lookup(".button-bar");
						if (buttonBar != null) {
							buttonBar.setVisible(false);
							buttonBar.setManaged(false);
						}


						SplashManager.closeSplash();

						// Show alert and wait for result
						alert.showAndWait();

						if(this.operationResult==null)
							this.operationResult= OperationResult.ABORT;
						alert.close();
					});

					while (operationResult == null) {
						try {
							Thread.sleep(100);
							SplashManager.closeSplash();
						} catch (InterruptedException e) {
							// Auto-generated catch block
							com.neuronrobotics.sdk.common.Log.error(e);
						}

					}
					if (isVis)
						SplashManager.renderSplashFrame(0, "Processing ");
					return operationResult;
				}
			});
			return fromFile;
		} catch (Exception e) {
			newProject();
			return fromFile;
		}
	}

	public void save(CaDoodleFile cf) {
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
	}

	public boolean isOpen() {
		// Auto-generated method stub
		return isOpenValue;
	}

	public WritableImage getImage() {
		return fromFile.getImage();
	}

	public CaDoodleFile get() {
		return fromFile;
	}

	@Override
	public void onUpdate(List<CSG> currentState, CaDoodleOperation source, CaDoodleFile file) {
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				l.onUpdate(currentState, source, file);
			} catch (Throwable e) {
				com.neuronrobotics.sdk.common.Log.error(e);
			}
		}
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
		if(OSUtil.isWindows()) {
			relative = Paths.get(System.getProperty("user.home"), "Documents").toString();;
		}
		File defaultFIle = new File(relative + delim() + "MyCaDoodleProjects" + delim());
		defaultFIle.mkdirs();
		return new File((String) ConfigurationDatabase.get("CaDoodle", "CaDoodleWorkspace", defaultFIle.getAbsolutePath()));
	}

	public List<CaDoodleFile> getProjects() throws IOException {
		String directoryPath = getWorkingDir().getAbsolutePath();
		com.neuronrobotics.sdk.common.Log.error("Loading workspace from "+directoryPath);
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
		HashSet<String> externals =  Main.getOptionalProjects();
		
		for(String s:externals) {
			File f = new File(s);
			if(f.exists()&&f.getName().toLowerCase().endsWith(".doodle")) {
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
			cf.save();
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
				//TickToc.tic("Start "+l.getClass());
				l.onRegenerateDone();
				//TickToc.tic("End "+l.getClass());
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
	public void onTimelineUpdate(int num) {
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				l.onTimelineUpdate( num);
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
		String name = file.getName().substring(0, file.getName().length()-4);
		int index=0;
		File targetDir = null;
		do {
			targetDir=new File(getWorkingDir()+delim()+name+"_"+index);
			index++;
		}while(targetDir.exists());
		try {
			unzip(file.getAbsolutePath(),targetDir.getAbsolutePath());
			File[] files= targetDir.listFiles();
			for(File f:files) {
				if(f.getName().toLowerCase().endsWith(".doodle")) {
					setActiveProject(f);
					new Thread(()->	get().initialize()).start();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			com.neuronrobotics.sdk.common.Log.error(e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			com.neuronrobotics.sdk.common.Log.error(e);
		}
		
	}
}
