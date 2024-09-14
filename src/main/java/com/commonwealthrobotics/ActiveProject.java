package com.commonwealthrobotics;
import static com.neuronrobotics.bowlerstudio.scripting.DownloadManager.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.creature.ThumbnailImage;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AddFromScript;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.RandomStringFactory;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

public class ActiveProject {


	
	private boolean isOpenValue=true;
	private WritableImage img;
	public void setActiveProject(File f) {
		ConfigurationDatabase.put("CaDoodle", "CaDoodleacriveFile", f.getAbsolutePath());
	}
	public File getActiveProject() {
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
	public CaDoodleFile loadActive(ICaDoodleStateUpdate listener) throws Exception {
		return CaDoodleFile.fromFile(getActiveProject(),listener,false);
	}
	public void save(CaDoodleFile cf) {
		cf.setSelf(getActiveProject());
		File parent = getActiveProject().getAbsoluteFile().getParentFile();
		File image = new File(parent.getAbsolutePath()+delim()+"snapshot.png");
		img=null;
		BowlerStudio.runLater(()->img=ThumbnailImage.get(cf.getCurrentState()));
		while(img==null)
			try {
				Thread.sleep(100);
				System.out.println("Waiting for image to write");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		BufferedImage bufferedImage = SwingFXUtils.fromFXImage(img, null);
        try {
            ImageIO.write(bufferedImage, "png", image);
            System.out.println("Image saved successfully to " + image.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving image: " + e.getMessage());
            e.printStackTrace();
        }
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
		return img;
	}
	public void setImage(WritableImage img) {
		this.img = img;
	}
}
