package com.commonwealthrobotics;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.creature.ThumbnailImage;
import com.neuronrobotics.bowlerstudio.scripting.DownloadManager;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AbstractAddFrom;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AddFromScript;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Sweep;
import com.neuronrobotics.bowlerstudio.vitamins.Vitamins;

import static com.neuronrobotics.bowlerstudio.scripting.DownloadManager.*;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.FileUtil;
import eu.mihosoft.vrl.v3d.parametrics.CSGDatabase;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class ShapePalletButtonResources {
	javafx.scene.image.Image image = null;
	CSG indicator = null;
	File imageFile = null;
	File stlFile = null;

	public ShapePalletButtonResources(HashMap<String, String> key, String typeOfShapes, String name,ActiveProject ap) {
		String string = key.get("plugin");

		boolean isPluginMissing = false;
		if(string!=null) {
			isPluginMissing=!DownloadManager.isDownloadedAlready(string);
		}
		String absolutePath = ScriptingEngine.getWorkspace().getAbsolutePath() + delim() + "uicache";
		File dir = new File(absolutePath);
		if (!dir.exists())
			dir.mkdirs();
		imageFile = new File(absolutePath + delim() + typeOfShapes + name + ".png");
		stlFile = new File(absolutePath + delim() + typeOfShapes + name + ".stl");
		if (imageFile.exists() && stlFile.exists()) {
			indicator = Vitamins.get(stlFile);
			indicator.setColor(Color.WHITE);
			image = new Image(imageFile.toURI().toString());
			return;
		}
		if(isPluginMissing) {
			indicator=new Cube(20).toCSG().toZMin();
			indicator.setColor(Color.WHITE);
			image = new Image(ShapePalletButtonResources.class.getResourceAsStream("pluginMissing.png"));
			return;
		}
		String sweep = key.get("sweep");
		boolean isSweep = (sweep != null)? Boolean.parseBoolean(sweep):false;
		
		// new Thread(() -> {
		AbstractAddFrom set = new AddFromScript().set(key.get("git"), key.get("file")).setPreventBoM(true);
		if (isSweep) {
			try {
				File f = ScriptingEngine.fileFromGit(key.get("git"), key.get("file"));
				Sweep s = new Sweep();
				String ZPer = key.get("ZPer");
				String Degrees = key.get("Degrees");
				String sprial = key.get("Spiral");
				if (ZPer != null) {
					s.setDefz(Double.parseDouble(ZPer));
				}
				if (Degrees != null)
					s.setDefangle(Double.parseDouble(Degrees));
				if(sprial!=null) {
					s.setDefSpiral(Double.parseDouble(sprial));
				}
				s.set(f).setPreventBoM(true);
				set = s;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		set.setCaDoodleFile(ap.get());
		List<CSG> so = set.process(new ArrayList<>());
		for (CSG c : so) {
			for (String s : c.getParameters()) {
				CSGDatabase.delete(s);
			}
		}
		if (isSweep)
			try {
				File file = set.getFile();
				file.delete();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		//referenceParts.put(button, so);
		BowlerStudio.runLater(() -> {
			if (typeOfShapes.toLowerCase().contains("vitamin"))
				for (CSG c : so) {
					c.setIsHole(false);
				}
			image = ThumbnailImage.get(so);
			
		});
		while(image==null) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
		try {
			ImageIO.write(bufferedImage, "png", imageFile);
			System.err.println("Thumbnail saved successfully to " + imageFile.getAbsolutePath());
		} catch (IOException e) {
			// com.neuronrobotics.sdk.common.Log.error("Error saving image: " +
			// e.getMessage());
			e.printStackTrace();
		}
		indicator = so.get(0);
		if (so.size() > 1) {
			for(int i=1;i<so.size();i++) {
				indicator=indicator.dumbUnion(so.get(i));
			}
		}
		indicator.setColor(Color.WHITE);
		try {
			FileUtil.write(Paths.get(stlFile.getAbsolutePath()),indicator.toStlString());
			System.err.println("Indicator STL saved successfully to " + stlFile.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public javafx.scene.image.Image getImage() {
		return image;
	}

	public CSG getIndicator() {

		return indicator;
	}
}
