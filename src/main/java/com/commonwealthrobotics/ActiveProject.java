package com.commonwealthrobotics;
import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.RandomStringFactory;

import javafx.scene.image.WritableImage;

public class ActiveProject {


	
	private boolean isOpenValue=true;
	private CaDoodleFile fromFile;
	private ICaDoodleStateUpdate listener;
	public ActiveProject(ICaDoodleStateUpdate listener){
		this.listener = listener;
		
	}
	public CaDoodleFile setActiveProject(File f) throws Exception {
		if(fromFile!=null) {
			fromFile.removeListener(listener);
		}
		ConfigurationDatabase.put("CaDoodle", "CaDoodleacriveFile", f.getAbsolutePath());
		return loadActive();
	}
	private File getActiveProject() {
		try {
			if(!ConfigurationDatabase.containsKey("CaDoodle", 
					"CaDoodleacriveFile"))
				ScriptingEngine.pull("https://github.com/madhephaestus/TestRepo.git");
			return new File(ConfigurationDatabase.get(
					"CaDoodle", 
					"CaDoodleacriveFile", 
					ScriptingEngine.fileFromGit("https://github.com/madhephaestus/TestRepo.git", "TestRepo.doodle").getAbsolutePath()).toString());
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
		File random = new File(RandomStringFactory.getNextRandomName()+".doodle");
		ConfigurationDatabase.put("CaDoodle", "CaDoodleacriveFile", random.getAbsolutePath());
		return random;
	}
	public CaDoodleFile loadActive() throws Exception {
		fromFile = CaDoodleFile.fromFile(getActiveProject(),listener,false);
		return fromFile;
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
}
