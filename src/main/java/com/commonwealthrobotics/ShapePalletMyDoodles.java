package com.commonwealthrobotics;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import com.commonwealthrobotics.controls.SelectionSession;
import com.commonwealthrobotics.controls.SpriteDisplayMode;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AbstractAddFrom;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AddFromFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AddFromScript;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CadoodleConcurrencyException;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Sweep;
import com.neuronrobotics.bowlerstudio.vitamins.Vitamins;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.parametrics.CSGDatabase;
import eu.mihosoft.vrl.v3d.parametrics.CSGDatabaseInstance;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.transform.Affine;

public class ShapePalletMyDoodles {

	private ComboBox<String> shapeCatagory;
	private GridPane objectPallet;
	private SelectionSession session;
	private ActiveProject ap;
	private WorkplaneManager workplane;

	public ShapePalletMyDoodles(ComboBox<String> shapeCatagory, GridPane objectPallet, SelectionSession session,
			ActiveProject ap, WorkplaneManager workplane) {
		this.shapeCatagory = shapeCatagory;
		this.objectPallet = objectPallet;
		this.session = session;
		this.ap = ap;
		this.workplane = workplane;
	}

	public String getName() {
		return "My Doodles";
	}

	public void activate() throws IOException {
		BowlerStudio.runLater(() -> objectPallet.getChildren().clear());
		do {
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				com.neuronrobotics.sdk.common.Log.error(e);
				return;
			}

		} while (!ap.get().isInitialized());
		com.neuronrobotics.sdk.common.Log.debug("Loading the MyDoodles panel after initialization");

		List<CaDoodleFile> proj = ap.getProjects();
		int i = 0;
		ArrayList<Button> buttons = new ArrayList<Button>();
		for (int j = 0; j < proj.size(); j++) {
			int col = i % 3;
			int row = i / 3;
			if (proj.get(j).getMyProjectName().contentEquals(ap.get().getMyProjectName()))
				continue;
			try {
				buttons.add(setupButton(proj.get(j), col, row));
				i++;
			} catch (Exception ex) {
				// com.neuronrobotics.sdk.common.Log.error(e);;
			}
		}
		BowlerStudio.runLater(() -> {
			for (Button b : buttons) {
				b.setDisable(false);
			}
		});
	}

	public Button setupButton(CaDoodleFile caDoodleFile, int col, int row) throws Exception {
		if (caDoodleFile.getMyProjectName().contentEquals(ap.get().getMyProjectName()))
			throw new RuntimeException("You can not reference yourself in a model");
		String name = caDoodleFile.getMyProjectName();

		Tooltip hover = new Tooltip(name);
		Button button = new Button();
		button.setTooltip(hover);
		button.getStyleClass().add("image-button");

		CSGDatabaseInstance instance =caDoodleFile.getCsgDBinstance();
		if (!caDoodleFile.getSTLThumbnailFile().exists()) {
//			Path tempFile = Files.createTempFile("CSGDatabase", ".tmp");
//			CSGDatabase.setInstance(new CSGDatabaseInstance(tempFile.toFile()));
//			caDoodleFile.initialize();
//			caDoodleFile.save();
//			CSGDatabase.setInstance(instance);
//			if (!caDoodleFile.getSTLThumbnailFile().exists())
				throw new Exception("Failed to initialize model " + caDoodleFile.getMyProjectName());
		}
		CSG indicator = Vitamins.get(instance,caDoodleFile.getSTLThumbnailFile());
		BowlerStudio.runLater(() -> {
			objectPallet.add(button, col, row);
			Image thumb = caDoodleFile.loadImageFromFile();
			ImageView tIv = new ImageView(TimelineManager.resizeImage(thumb, 50, 50));
			ImageView toolimage = new ImageView(thumb);

			toolimage.setFitHeight(300);
			toolimage.setFitWidth(300);
			hover.setGraphic(toolimage);
			hover.setContentDisplay(ContentDisplay.TOP);
//			tIv.setFitHeight(50);
//			tIv.setFitWidth(50);
			button.setGraphic(tIv);
			button.setDisable(true);
			button.setOnMousePressed(ev -> {
				new Thread(() -> {
					session.setMode(SpriteDisplayMode.PLACING);
					workplane.setIndicator(indicator, new Affine());
					boolean workplaneInOrigin = !workplane.isWorkplaneNotOrigin();
					com.neuronrobotics.sdk.common.Log.debug("Is Workplane set " + workplaneInOrigin);
					workplane.setOnSelectEvent(() -> {
						new Thread(() -> {
							session.setMode(SpriteDisplayMode.Default);
							if (workplane.isClicked())
								try {
									TransformNR currentAbsolutePose = workplane.getCurrentAbsolutePose();
									AddFromFile addFromFile = new AddFromFile();
									AbstractAddFrom setAddFromScript = addFromFile.set(caDoodleFile.getSelf(), ap.get())
											.setLocation(currentAbsolutePose);
									ap.addOp(setAddFromScript).join();
									HashSet<String> namesAdded = setAddFromScript.getNamesAdded();
									ArrayList<String> namesBack = new ArrayList<String>();
									namesBack.addAll(namesAdded);
									session.selectAll(namesAdded);
									if (!workplane.isClicked())
										return;
									if (workplane.isClickOnGround()) {
										// com.neuronrobotics.sdk.common.Log.error("Ground plane click detected");
										ap.get().setWorkplane(new TransformNR());
									} else {
										ap.get().setWorkplane(workplane.getCurrentAbsolutePose());
									}
									workplane.placeWorkplaneVisualization();
									if (workplaneInOrigin)
										workplane.setTemporaryPlane();
								} catch (CadoodleConcurrencyException e) {
									com.neuronrobotics.sdk.common.Log.error(e);
								} catch (InterruptedException e) {
									com.neuronrobotics.sdk.common.Log.error(e);
								}

						}).start();
					});
					workplane.activate();

				}).start();
				session.setKeyBindingFocus();
			});
		});
		try {
			Thread.sleep(30);
		} catch (InterruptedException e) {
			com.neuronrobotics.sdk.common.Log.error(e);
		}
		return button;
	}

}
