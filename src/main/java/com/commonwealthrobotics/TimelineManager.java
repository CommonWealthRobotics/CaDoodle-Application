package com.commonwealthrobotics;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.Log;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public class TimelineManager {

	private ScrollPane timelineScroll;
	private HBox baseBox;
	private GridPane timeline;
	private ActiveProject ap;
	private ArrayList<Button> buttons = new ArrayList<Button>();
	private boolean updating = false;
	private SelectionSession session;
	private boolean clear;

	private boolean updateNeeded = false;
	private BowlerStudio3dEngine engine;
	private boolean addrem;

	public TimelineManager(ActiveProject activeProject) {
		this.ap = activeProject;

		ap.addListener(new ICaDoodleStateUpdate() {
			long timeSinceGC = 0;

			@Override
			public void onWorkplaneChange(TransformNR newWP) {
			}

			@Override
			public void onUpdate(List<CSG> currentState, CaDoodleOperation source, CaDoodleFile file) {
				if (file.isRegenerating())
					return;
			}

			@Override
			public void onSaveSuggestion() {
			}

			@Override
			public void onRegenerateStart() {
			}

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
				if (num > 1)
					update(true);
				else if (num == 1)
					update(false);
			}
		});
	}

	public void set(ScrollPane timelineScroll, HBox timeline, SelectionSession session, BowlerStudio3dEngine engine) {
		this.timelineScroll = timelineScroll;
		this.baseBox = timeline;
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

	private boolean boundsSame(CSG one, CSG two) {
		if (one == null || two == null)
			return true;
		return boundsSame(one.getBounds(), two.getBounds());
	}

	private boolean boundsSame(Bounds one, Bounds two) {
		if (one.getMax().test(two.getMax(), 0.0001)) {
			return true;
		}
		if (one.getMin().test(two.getMin(), 0.0001)) {
			return true;
		}
		return false;
	}

	private CSG getSameName(CSG get, List<CSG> list) {
		for (CSG c : list)
			if (c.getName().contentEquals(get.getName()))
				return c;
		return null;
	}

	private void update(boolean clear) {
		// com.neuronrobotics.sdk.common.Log.debug("Timeline Update called");
		this.clear = clear;
		updateNeeded = true;
		if (updating)
			return;
		updating = true;
		updateNeeded = false;
		ArrayList<CaDoodleOperation> opperations = ap.get().getOpperations();
		BowlerStudio.runLater(() -> {
			if(timeline!=null) {
				if (clear)
					clear();
			}else {
				timeline=new GridPane();
		        baseBox.getChildren().add(timeline);
			}
		       // Configure columns (all same width)
			int buttonSize=80;
			int space =20;
			timeline.setHgap(space/2); // Horizontal gap between columns
			timeline.setVgap(space); // Vertical gap between rows
	        
	        // Center the entire GridPane content
			timeline.setAlignment(Pos.CENTER);
	        timeline.setGridLinesVisible(true); // Shows grid lines for debuggin

	       // baseBox.setMaxWidth(opperations.size() * (buttonSize+space));
	        timelineScroll.setHvalue(1.0);


			new Thread(() -> {
				while(ap.get().isRegenerating() || !ap.get().isInitialized()) {
					try {
						Thread.sleep(100);
						Log.debug("Waifting for timeline to update");
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					}
				}
				addrem = false;
				int s = opperations.size();
				for (int i = buttons.size(); i < Math.max(s, ap.get().getCurrentIndex()); i++) {
					try {
						CaDoodleOperation op = opperations.get(i);
						List<CSG> state = ap.get().getStateAtOperation(op);
						if (op == null)
							continue;
						int myIndex = i;
						BowlerStudio.runLater(() -> {
							String text = (myIndex + 1) + "\n" + op.getType();
							Button toAdd = new Button(text);
							buttons.add(toAdd);
							int my = myIndex;
							ContextMenu contextMenu = new ContextMenu();
							List<CSG> previous = (myIndex == 0) ? new ArrayList<CSG>()
									: ap.get().getStateAtOperation(opperations.get(myIndex - 1));
							toAdd.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
								session.setKeyBindingFocus();
								BowlerStudio.runLater(() -> {
									if (event.getButton() == MouseButton.PRIMARY) {
										int index = ap.get().getCurrentIndex() - 1;
										Button button = buttons.get(index < 0 ? 0 : index);
										contextMenu.hide();
										if (button == toAdd)
											return;
										for (CSG c : state)
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
												com.neuronrobotics.sdk.common.Log.error(e);
											}
											BowlerStudio.runLater(() -> contextMenu.hide());
										}).start();
									}
								});
							});
							int myButtonIndex = myIndex;
							toAdd.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
								int index = ap.get().getCurrentIndex() - 1;
								if (index != myButtonIndex)
									for (CSG c : state) {
										if (c.isInGroup())
											continue;
										if (c.isHide())
											continue;
										CSG prev = getSameName(c, previous);
										boolean b = !boundsSame(prev, c);
										if (prev != null) {
											if (prev.isHide() && !c.isHide())
												b = true;
											if (prev.isHole() != c.isHole())
												b = true;
										}
										if (b || prev == null)
											engine.addObject(c, null, 0.4);
									}
							});
							toAdd.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
								int index = ap.get().getCurrentIndex() - 1;
								if (index != myButtonIndex)
									for (CSG c : state)
										engine.removeObject(c);
							});
							File f = ap.get().getTimelineImageFile(myIndex - 1);
							Image image = new Image(f.toURI().toString());
							ImageView value = new ImageView(resizeImage(image, buttonSize, buttonSize));
							ImageView toolimage = new ImageView(image);
							toolimage.setFitHeight(300);
							toolimage.setFitWidth(300);
							toAdd.getStyleClass().add("image-button");
							toAdd.setContentDisplay(ContentDisplay.TOP);
							toAdd.setGraphic(value);
							Tooltip tooltip = new Tooltip(text);
							tooltip.setGraphic(toolimage);
							tooltip.setContentDisplay(ContentDisplay.TOP);
							// toAdd.setTooltip(tooltip);

							timeline.add(toAdd,myIndex,0);
							Separator verticalSeparator = new Separator();
							verticalSeparator.setOrientation(Orientation.VERTICAL);
							verticalSeparator.setPrefHeight(buttonSize); // Set height to 80 units
							//timeline.getChildren().add(verticalSeparator);
							addrem = true;
							// Create a delete menu item
							MenuItem deleteItem = new MenuItem("Delete");
							deleteItem.getStyleClass().add("image-button-focus");
							deleteItem.setOnAction(event -> {
								toAdd.setDisable(true);
								buttons.remove(toAdd);
								timeline.getChildren().remove(toAdd);
								ap.get().deleteOperation(op);
								for (CSG c : state)
									engine.removeObject(c);
							});
							deleteItem.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
								int index = ap.get().getCurrentIndex() - 1;
								if (index != myButtonIndex)
									for (CSG c : state)
										engine.removeObject(c);
							});
							// Add the delete item to the context menu
							contextMenu.getItems().add(deleteItem);
							
						});
						// Add event handler for right-click
					} catch (Exception ex) {
						com.neuronrobotics.sdk.common.Log.error(ex);
						;
					}
				}
				// com.neuronrobotics.sdk.common.Log.debug("Timeline updated");
				if (addrem)
					BowlerStudio.runLater(java.time.Duration.ofMillis(100), () -> {
						timelineScroll.setHvalue(1.0);
						updating = false;
						if (updateNeeded)
							update(clear);
						session.updateControlsDisplayOfSelected();
					});
				else {
					updating = false;
					if (updateNeeded)
						update(clear);
					BowlerStudio.runLater(() -> session.updateControlsDisplayOfSelected());
				}

			}).start();
		});
	}

	public void clear() {
		// com.neuronrobotics.sdk.common.Log.debug("Old Timeline buttons cleared");

		buttons.clear();
		timeline.getChildren().clear();
		
	}

	public void updateSelected(LinkedHashSet<String> selected) {
		ArrayList<CaDoodleOperation> opperations = ap.get().getOpperations();
		for (int i = 0; i < opperations.size() && i < buttons.size(); i++) {
			CaDoodleOperation op = opperations.get(i);
			if (op == null)
				continue;
			Button b = buttons.get(i);
			boolean applyToMe = false;
			int index = ap.get().getCurrentIndex() - 1;
			if (index >= buttons.size())
				continue;
			Button sel = buttons.get(index < 0 ? 0 : index);
			for (String s : op.getNamesAddedInThisOperation()) {
				for (String p : selected) {
					if (s.contentEquals(p))
						applyToMe = true;
				}
			}
			b.getStyleClass().clear();
			if (sel == b) {
				b.getStyleClass().add("image-button-focus");
			}
			if (applyToMe)
				b.getStyleClass().add("image-button-highlight");
			else
				b.getStyleClass().add("image-button");

		}
	}

	public void setOpenState(boolean timelineOpen) {
		ap.get().setTimelineVisable(timelineOpen);
		if (timelineOpen)
			session.save();
	}

}
