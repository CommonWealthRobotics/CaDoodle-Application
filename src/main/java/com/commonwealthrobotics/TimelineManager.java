package com.commonwealthrobotics;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleOpperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public class TimelineManager {

	private ScrollPane timelineScroll;
	private HBox timeline;
	private ActiveProject ap;
	private ArrayList<Button> buttons = new ArrayList<Button>();
	private boolean updating = false;
	private SelectionSession session;
	private boolean clear;
	
	private boolean updateNeeded=false;
	private BowlerStudio3dEngine engine;

	public TimelineManager(ActiveProject activeProject) {
		this.ap = activeProject;
		
		ap.addListener(new ICaDoodleStateUpdate() {
			long timeSinceGC=0;
			@Override
			public void onWorkplaneChange(TransformNR newWP) {}
			@Override
			public void onUpdate(List<CSG> currentState, ICaDoodleOpperation source, CaDoodleFile file) {
				if(file.isRegenerating())
					return;
			}
			@Override
			public void onSaveSuggestion() {}
			@Override
			public void onRegenerateStart() {}
			@Override
			public void onRegenerateDone() {
			}

			@Override
			public void onInitializationStart() {

			}
			@Override
			public void onInitializationDone() {
				update(true);
			}
			@Override
			public void onTimelineUpdate(int num) {
				if(num>1)
					update(true);
				else if (num==1)
					update(false);
			}
		});
	}

	public void set(ScrollPane timelineScroll, HBox timeline, SelectionSession session,BowlerStudio3dEngine engine) {
		this.timelineScroll = timelineScroll;
		this.timeline = timeline;
		this.session = session;
		this.engine = engine;
	}

	public static Image resizeImage(Image originalImage, int targetWidth, int targetHeight) {
		// Create a canvas with the target dimensions
		Canvas canvas = new Canvas(targetWidth, targetHeight);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		// Clear the canvas with a transparent background
		gc.clearRect(0, 0, targetWidth, targetHeight);
		// Draw the original image scaled to the target size
		gc.drawImage(originalImage, 0, 0, targetWidth, targetHeight);
		// Create snapshot parameters to preserve transparency
		SnapshotParameters params = new SnapshotParameters();
		params.setFill(Color.TRANSPARENT);
		// Create a new WritableImage from the canvas
		WritableImage resizedImage = new WritableImage(targetWidth, targetHeight);
		canvas.snapshot(params, resizedImage);
		return resizedImage;
	}
	private boolean boundsSame(CSG one,CSG two) {
		if(one==null||two==null)
			return true;
		return boundsSame(one.getBounds(), two.getBounds());
	}
	private boolean boundsSame(Bounds one, Bounds two) {
		if(one.getMax().test(two.getMax(), 0.0001)) {
			return true;
		}
		if(one.getMin().test(two.getMin(), 0.0001)) {
			return true;
		}
		return false;
	}
	private CSG getSameName(CSG get, List<CSG> list) {
		for(CSG c:list)
			if(c.getName().contentEquals(get.getName()))
				return c;
		return null;
	}
	private void update(boolean clear) {
		//System.out.println("Timeline Update called");
		this.clear = clear;
		updateNeeded=true;
		if(updating)
			return;
		updateNeeded=false;
		updating = true;
		BowlerStudio.runLater(() -> {
			boolean addrem = false;
			if (clear)
				clear();
			while (ap.get().getCurrentIndex() < (buttons.size() - 1) && !ap.get().isForwardAvailible()) {
				Button toRem = buttons.remove(buttons.size() - 1);
				timeline.getChildren().remove(toRem);
				addrem = true;
			}
			ArrayList<ICaDoodleOpperation> opperations = ap.get().getOpperations();
			int s = opperations.size();
			for (int i = buttons.size(); i < Math.max(s, ap.get().getCurrentIndex()); i++) {
				try {
					ICaDoodleOpperation op = opperations.get(i);
					if(op==null)
						continue;
					String text = (i + 1) + "\n" + op.getType();
					Button toAdd = new Button(text);
					buttons.add(toAdd);
					int my = i;
					ContextMenu contextMenu = new ContextMenu();
					List<CSG> state=ap.get().getStateAtOperation(op);
					List<CSG> previous = (i==0)?new ArrayList<CSG>():ap.get().getStateAtOperation(opperations.get(i-1));
					toAdd.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
						session.setKeyBindingFocus();
						BowlerStudio.runLater(() -> {
							if (event.getButton() == MouseButton.PRIMARY) {
								int index = ap.get().getCurrentIndex() - 1;
								Button button = buttons.get(index < 0 ? 0 : index);
								contextMenu.hide();
								if (button == toAdd)
									return;
								for(CSG c:state)
									engine.removeObject(c);
								new Thread(() -> {
									ap.get().moveToOpIndex(my);
								}).start();
								
							}
							if (event.getButton() == MouseButton.SECONDARY) {
								// Show context menu where the mouse was clicked
								contextMenu.show(toAdd, event.getScreenX(), event.getScreenY());
								new Thread(() -> {
									try {
										Thread.sleep(3000);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									BowlerStudio.runLater(() -> contextMenu.hide());
								}).start();
							}
						});
					});
					int myButtonIndex=i;
					toAdd.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
						int index = ap.get().getCurrentIndex() - 1;
						if(index!=myButtonIndex)
							for(CSG c:state) {
								if (c.isInGroup())
									continue;
								if(c.isHide())
									continue;
								CSG prev=getSameName(c, previous);
								boolean b=!boundsSame(prev, c);
								if(prev!=null) {
									if(prev.isHide() && !c.isHide())
										b=true;
									if(prev.isHole()!=c.isHole())
										b=true;
								}
								if(b||prev==null)
									engine.addObject(c, null, 0.4);
							}
					});
					toAdd.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
						int index = ap.get().getCurrentIndex() - 1;
						if(index!=myButtonIndex)
							for(CSG c:state)
								engine.removeObject(c);
					});
					File f = ap.get().getTimelineImageFile(i - 1);
					Image image = new Image(f.toURI().toString());
					ImageView value = new ImageView(resizeImage(image, 80, 80));
					ImageView toolimage = new ImageView(image);
					toolimage.setFitHeight(300);
					toolimage.setFitWidth(300);
					toAdd.getStyleClass().add("image-button");
					toAdd.setContentDisplay(ContentDisplay.TOP);
					toAdd.setGraphic(value);
					Tooltip tooltip = new Tooltip(text);
					tooltip.setGraphic(toolimage);
					tooltip.setContentDisplay(ContentDisplay.TOP);
					//toAdd.setTooltip(tooltip);

					timeline.getChildren().add(toAdd);
					Separator verticalSeparator = new Separator();
					verticalSeparator.setOrientation(Orientation.VERTICAL);
					verticalSeparator.setPrefHeight(80); // Set height to 80 units
					timeline.getChildren().add(verticalSeparator);
					addrem = true;
					// Create a delete menu item
					MenuItem deleteItem = new MenuItem("Delete");
					deleteItem.getStyleClass().add("image-button-focus");
					deleteItem.setOnAction(event -> {
						toAdd.setDisable(true);
						buttons.remove(toAdd);
						timeline.getChildren().remove(toAdd);
						ap.get().deleteOperation(op);
						for(CSG c:state)
							engine.removeObject(c);
					});
					deleteItem.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
						int index = ap.get().getCurrentIndex() - 1;
						if(index!=myButtonIndex)
							for(CSG c:state)
								engine.removeObject(c);
					});
					// Add the delete item to the context menu
					contextMenu.getItems().add(deleteItem);
					// Add event handler for right-click
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			//System.out.println("Timeline updated");
			if (addrem)
				BowlerStudio.runLater(java.time.Duration.ofMillis(100),() -> {
					timelineScroll.setHvalue(1.0);
					updating = false;
					if(updateNeeded)
						update(clear);
					session.updateControlsDisplayOfSelected();
				});
			else {
				updating = false;
				if(updateNeeded)
					update(clear);
				session.updateControlsDisplayOfSelected();
			}
			
		});
	}

	public void clear() {
		//System.out.println("Old Timeline buttons cleared");
		buttons.clear();
		timeline.getChildren().clear();
	}

	public void updateSelected(LinkedHashSet<String> selected) {
		ArrayList<ICaDoodleOpperation> opperations = ap.get().getOpperations();
		for(int i=0;i<opperations.size()&&i<buttons.size();i++) {
			ICaDoodleOpperation op =opperations.get(i);
			if(op==null)
				continue;
			Button b=buttons.get(i);
			boolean applyToMe=false;
			int index = ap.get().getCurrentIndex() - 1;
			if(index>=buttons.size())
				continue;
			Button sel = buttons.get(index < 0 ? 0 : index);
			for(String s:op.getNames()) {
				for(String p:selected) {
					if(s.contentEquals(p))
						applyToMe=true;
				}
			}
			b.getStyleClass().clear();
			if(sel==b) {
				b.getStyleClass().add("image-button-focus");
			}
			if(applyToMe)
				b.getStyleClass().add("image-button-highlight");
			else
				b.getStyleClass().add("image-button");


		}
	}

	public void setOpenState(boolean timelineOpen) {
		ap.get().setTimelineVisable(timelineOpen);
		if(timelineOpen)
			session.save();
	}

}
