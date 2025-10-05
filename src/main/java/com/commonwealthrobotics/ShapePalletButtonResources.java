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
import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.creature.ThumbnailImage;
import com.neuronrobotics.bowlerstudio.scripting.DownloadManager;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AbstractAddFrom;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AddFromScript;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Sweep;
import com.neuronrobotics.bowlerstudio.vitamins.Vitamins;
import com.neuronrobotics.video.OSUtil;

import static com.neuronrobotics.bowlerstudio.scripting.DownloadManager.*;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.FileUtil;
import eu.mihosoft.vrl.v3d.parametrics.CSGDatabase;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ShapePalletButtonResources {
	javafx.scene.image.Image image = null;
	CSG indicator = null;
	File imageFile = null;
	File stlFile = null;

	
	public ShapePalletButtonResources(HashMap<String, String> key, String typeOfShapes, String name, ActiveProject ap) {
		String string = key.get("plugin");

		boolean isPluginMissing = false;
		if (string != null) {
			isPluginMissing = !DownloadManager.isDownloadedAlready(string);
		}
		String absolutePath = ConfigurationDatabase.getAppDataDirectory().toString();
		;
		File dir = new File(absolutePath);
		if (!dir.exists())
			dir.mkdirs();
		imageFile = new File(absolutePath + delim() + typeOfShapes + name + ".png");
		stlFile = new File(absolutePath + delim() + typeOfShapes + name + ".stl");
		// https://github.com/CommonWealthRobotics/CaDoodle-Application/issues/69
		// if(!OSUtil.isWindows())
		if (imageFile.exists() && stlFile.exists()) {
			try {
				indicator = Vitamins.get(stlFile);
				indicator.setColor(Color.WHITE);
				image = new Image(imageFile.toURI().toString());
				return;
			} catch (Throwable t) {
				com.neuronrobotics.sdk.common.Log.error(t);
			}
		}
		if (isPluginMissing) {
			indicator = new Cube(20).toCSG().toZMin();
			indicator.setColor(Color.WHITE);
			image = new Image(ShapePalletButtonResources.class.getResourceAsStream("pluginMissing.png"));
			return;
		}
		String sweep = key.get("sweep");
		boolean isSweep = (sweep != null) ? Boolean.parseBoolean(sweep) : false;

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
				if (sprial != null) {
					s.setDefSpiral(Double.parseDouble(sprial));
				}
				s.set(f,null).setPreventBoM(true);
				set = s;
			} catch (Exception ex) {
				com.neuronrobotics.sdk.common.Log.error(ex);;
			}
		}
		set.setCaDoodleFile(ap.get());
		List<CSG> so = set.process(new ArrayList<>());
		for (CSG c : so) {
			for (String s : c.getParameters(ap.get().getCsgDBinstance())) {
				ap.get().getCsgDBinstance().delete(s);
			}
		}
		if (isSweep)
			try {
				File file = set.getFile();
				file.delete();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				com.neuronrobotics.sdk.common.Log.error(e);
			}
		// referenceParts.put(button, so);
		BowlerStudio.runLater(() -> {
			if (typeOfShapes.toLowerCase().contains("vitamin"))
				for (CSG c : so) {
					c.setIsHole(false);
				}
			image = ThumbnailImage.get(ap.get().getCsgDBinstance(),so);

		});
		while (image == null) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				com.neuronrobotics.sdk.common.Log.error(e);
			}
		}
		BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
		try {
			ImageIO.write(bufferedImage, "png", imageFile);
			com.neuronrobotics.sdk.common.Log.error("Thumbnail saved successfully to " + imageFile.getAbsolutePath());
		} catch (IOException e) {
			// com.neuronrobotics.sdk.common.Log.error("Error saving image: " +
			// e.getMessage());
			com.neuronrobotics.sdk.common.Log.error(e);
		}
		indicator = so.get(0);
		if (so.size() > 1) {
			for (int i = 1; i < so.size(); i++) {
				indicator = indicator.dumbUnion(so.get(i));
			}
		}
		indicator.setColor(Color.WHITE);
		try {
			FileUtil.write(Paths.get(stlFile.getAbsolutePath()), indicator.toStlString());
			com.neuronrobotics.sdk.common.Log.error("Indicator STL saved successfully to " + stlFile.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			com.neuronrobotics.sdk.common.Log.error(e);
		}
	}

	public javafx.scene.image.Image getImage() {
		return image;
	}

	public CSG getIndicator() {

		return indicator;
	}
}
