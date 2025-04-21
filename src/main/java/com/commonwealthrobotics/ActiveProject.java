package com.commonwealthrobotics;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.IAcceptPruneForward;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleOpperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.RandomStringFactory;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

public class ActiveProject implements ICaDoodleStateUpdate {

	private boolean isOpenValue = true;
	private boolean disableRegenerate = false;
	private CaDoodleFile fromFile;
	// private ICaDoodleStateUpdate listener;
	private ArrayList<ICaDoodleStateUpdate> listeners = new ArrayList<ICaDoodleStateUpdate>();

	public ActiveProject() {
		// this.listener = listener;

	}
	public Thread regenerateFrom(ICaDoodleOpperation source) {
		if(disableRegenerate)
			return null;
		Thread t = get().regenerateFrom(source);
		if(t==null)
			return null;
		new Thread(()->{
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// Auto-generated catch block
				e.printStackTrace();
			}
			if(t.isAlive()) {
				while(!SplashManager.isVisableSplash()) {
					SplashManager.renderSplashFrame((int)(get().getPercentInitialized()*100), " Re-Generating");
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// Auto-generated catch block
						e.printStackTrace();
					}
				}
			}else {
				return;
			}
			try {
				t.join();
			} catch (InterruptedException e) {
				// Auto-generated catch block
				e.printStackTrace();
			}
			SplashManager.closeSplash();
		}).start();
		return t;
	}
	public Thread addOp(ICaDoodleOpperation h) {
		Thread t = get().addOpperation(h);
		timeoutThread(h, t);
		return t;
	}
	private void timeoutThread(ICaDoodleOpperation h, Thread t) {
		new Thread(()->{
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// Auto-generated catch block
				e.printStackTrace();
			}
			if(t.isAlive()) {
				SplashManager.renderSplashFrame(50, h.getType()+" running");
			}else {
				return;
			}
			try {
				t.join();
			} catch (InterruptedException e) {
				// Auto-generated catch block
				e.printStackTrace();
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

	private File getActiveProject() {
		try {
			Object object = ConfigurationDatabase.get("CaDoodle", "CaDoodleacriveFile",
					null);
			if (object==null)
				return newProject();
			return new File(object
					.toString());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} 

	}

	public CaDoodleFile loadActive() throws Exception  {
		try {
			fromFile = CaDoodleFile.fromFile(getActiveProject(), this, false);
			fromFile.setAccept(new IAcceptPruneForward() {
				private ButtonType buttonType = null;
				@Override
				public boolean accept() {
					buttonType = null;
					boolean isVis = SplashManager.isVisableSplash();
					SplashManager.closeSplash();			
					BowlerKernel.runLater(() -> {
						Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
						alert.setTitle("Message");
						alert.setHeaderText("You made a change, this will erase the work you had done after this\nWould you like to continue?");
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
						SplashManager.closeSplash();
						Optional<ButtonType> result = alert.showAndWait();
						buttonType = result.get();
						alert.close();
					});
					
					while (buttonType == null) {
						try {
							Thread.sleep(100);
							SplashManager.closeSplash();
						} catch (InterruptedException e) {
							// Auto-generated catch block
							e.printStackTrace();
						}

					}
					if (isVis)
						SplashManager.renderSplashFrame(0, "Processing " );
					return buttonType.equals(ButtonType.OK);
				}
			});
			return fromFile;
		}catch(Exception e) {
			newProject();
			return fromFile;
		}
	}

	public void save(CaDoodleFile cf) {
		cf.setSelf(getActiveProject());
		try {
			cf.save();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
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
	public void onUpdate(List<CSG> currentState, ICaDoodleOpperation source, CaDoodleFile file) {
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				l.onUpdate(currentState, source, file);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onSaveSuggestion() {
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				l.onSaveSuggestion();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onInitializationDone() {
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				l.onInitializationDone();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	public File getWorkingDir() {
		String relative =ScriptingEngine.getWorkspace().getAbsolutePath();
		File file = new File(relative + delim()  + "MyCaDoodleProjects" + delim());
		file.mkdirs();
		return new File((String) ConfigurationDatabase.get("CaDoodle", "CaDoodleWorkspace", file.getAbsolutePath()));
	}

	public List<CaDoodleFile> getProjects() throws IOException {
		String directoryPath = getWorkingDir().getAbsolutePath();
		File dir = new File(directoryPath);
		List<CaDoodleFile> list = new ArrayList<>();

		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory() && !file.equals(dir)) {
					File[] contents = file.listFiles();
					for (File f : contents) {
						if (f.getName().toLowerCase().endsWith(".doodle")) {
							try {
								list.add(CaDoodleFile.fromFile(f, null, false));
							} catch (Exception e) {
								// Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
			}
		}

		return list;
	}

	public File newProject() throws IOException {
		List<CaDoodleFile> proj = getProjects();
		String nextRandomName = RandomStringFactory.getNextRandomName();
		String pathname = "Doodle-" +proj.size()+"-"+ nextRandomName;
		File np = new File(getWorkingDir().getAbsolutePath() + delim() + pathname);
		np.mkdirs();
		System.out.println("New Doodle Directory "+np.getAbsolutePath());
		File nf = new File(np.getAbsolutePath() + delim() + pathname+".doodle");
		nf.createNewFile();
		System.out.println("New Doodle File "+nf.getAbsolutePath());
		try {
			CaDoodleFile cf =setActiveProject(nf);
			cf.setProjectName(nextRandomName);
			cf.save();
		} catch (Exception e) {
			// Auto-generated catch block
			e.printStackTrace();
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
				e.printStackTrace();
			}
		}
	}
	@Override
	public void onInitializationStart() {
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				l.onInitializationStart();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
	@Override
	public void onRegenerateDone() {
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				l.onRegenerateDone();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
	@Override
	public void onRegenerateStart() {
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				l.onRegenerateStart();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
	@Override
	public void onTimelineUpdate() {
		for (ICaDoodleStateUpdate l : listeners) {
			try {
				l.onTimelineUpdate();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
}
