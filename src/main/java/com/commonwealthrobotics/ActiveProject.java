package com.commonwealthrobotics;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.filechooser.FileSystemView;

import static com.neuronrobotics.bowlerstudio.scripting.DownloadManager.*;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.scripting.DownloadManager;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleOpperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.RandomStringFactory;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.scene.image.WritableImage;

public class ActiveProject implements ICaDoodleStateUpdate {

	private boolean isOpenValue = true;
	private CaDoodleFile fromFile;
	// private ICaDoodleStateUpdate listener;
	private ArrayList<ICaDoodleStateUpdate> listeners = new ArrayList<ICaDoodleStateUpdate>();

	public ActiveProject() {
		// this.listener = listener;

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
			if (!ConfigurationDatabase.containsKey("CaDoodle", "CaDoodleacriveFile"))
				ScriptingEngine.pull("https://github.com/madhephaestus/TestRepo.git");
			return new File(ConfigurationDatabase.get("CaDoodle", "CaDoodleacriveFile",
					ScriptingEngine
							.fileFromGit("https://github.com/madhephaestus/TestRepo.git", "Doodle1/TestRepo.doodle")
							.getAbsolutePath())
					.toString());
		} catch (InvalidRemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File random = new File(RandomStringFactory.getNextRandomName() + ".doodle");
		ConfigurationDatabase.put("CaDoodle", "CaDoodleacriveFile", random.getAbsolutePath());
		return random;
	}

	public CaDoodleFile loadActive() throws Exception  {
		try {
			fromFile = CaDoodleFile.fromFile(getActiveProject(), this, false);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isOpen() {
		// TODO Auto-generated method stub
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
		String relative = FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
		if (!relative.endsWith("Documents")) {
			relative = relative + delim() + "Documents";
		}
		File file = new File(relative + delim() + "CaDoodle-workspace" + delim() + "MyCaDoodleProjects" + delim());
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
	            	for(File f:contents) {
	            		if(f.getName().toLowerCase().endsWith(".doodle")) {
	            			try {
								list.add(CaDoodleFile.fromFile(f,null,false));
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
	            		}
	            	}
	            }
	        }
	    }
	    
	    return list;
	}

	public void newProject() throws IOException {
		List<CaDoodleFile> proj = getProjects();
		String pathname = "Doodle-" + proj.size();
		File np = new File(getWorkingDir().getAbsolutePath() + delim() + pathname);
		np.mkdirs();
		File nf = new File(np.getAbsolutePath() + delim() + pathname+".doodle");
		nf.createNewFile();
		try {
			setActiveProject(nf);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
