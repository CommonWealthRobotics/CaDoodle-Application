package com.commonwealthrobotics;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.FailedToApplyOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AddFromFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.AddFromScript;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.MoveCenter;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Sweep;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.Log;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
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
	private boolean firstTime;
	private boolean timelineOpen;
	private int buttonSize = 80;
	private CheckBox timelineShowAll;
	private CheckBox timelineAddOpShow;
	private CheckBox timelineResizeShow;
	private CheckBox timelineAllignShow;
	private CheckBox timelineGroupShow;
	private CheckBox timelineHideShow;
	private CheckBox timelineMirrorShow;
	private CheckBox timelineFilletShow;
	private CheckBox timelineExtrudeShow;
	private CheckBox timelineRadialShow;
	private CheckBox timelineLinearShow;
	private CheckBox timelineDeleteShow;
	private ArrayList<CheckBox> boxes;
	private EventHandler<ActionEvent> showAllAction;
	private HBox timelineShowButtons;
	private CheckBox timelineMoveObjectShow;
	private ArrayList<Button> addButtons = new ArrayList<Button>();
	private ArrayList<Button> moveButtons = new ArrayList<Button>();
	private ArrayList<Button> otherButtons = new ArrayList<Button>();

	private CheckBox timelineOtherShow;

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
			public void onRegenerateStart(CaDoodleOperation source) {
				ArrayList<CaDoodleOperation> ops = ap.get().getOperations();
				ArrayList<Button> toRem = new ArrayList<Button>();
				for (int i = Math.max(0, ap.get().getCurrentIndex()); i < buttons.size(); i++) {
					Button b = buttons.get(i);
					toRem.add(b);
					BowlerStudio.runLater(() -> timeline.getChildren().remove(b));
				}
				buttons.removeAll(toRem);
			}

			@Override
			public void onRegenerateDone() {
				// update(false);
			}

			@Override
			public void onInitializationStart() {
				buttons.clear();
				if (timeline != null)
					BowlerStudio.runLater(() -> timeline.getChildren().clear());
			}

			@Override
			public void onInitializationDone() {

				firstTime = true;
				update(true);
			}

			@Override
			public void onTimelineUpdate(int num, File imageFile) {
				if (buttons.size() > num) {
					Button b = buttons.get(num);
					ImageView img = (ImageView) b.getGraphic();
					Image image = new Image(imageFile.toURI().toString());
					BowlerStudio.runLater(() -> {
						img.setImage(resizeImage(image, buttonSize, buttonSize));
					});
					Log.debug("Updating " + imageFile);
				} else
					update(false);
			}
		});
	}

	public void set(ScrollPane timelineScroll, HBox timeline, SelectionSession session, BowlerStudio3dEngine engine,
			HBox timelineShowButtons, CheckBox timelineShowAll, CheckBox timelineAddOpShow, CheckBox timelineResizeShow,
			CheckBox timelineAllignShow, CheckBox timelineGroupShow, CheckBox timelineHideShow,
			CheckBox timelineMirrorShow, CheckBox timelineFilletShow, CheckBox timelineExtrudeShow,
			CheckBox timelineRadialShow, CheckBox timelineLinearShow, CheckBox timelineDeleteShow,
			CheckBox timelineMoveObjectShow, CheckBox timelineOtherShow) {
		this.timelineScroll = timelineScroll;
		this.baseBox = timeline;
		this.session = session;
		this.engine = engine;
		this.timelineShowButtons = timelineShowButtons;
		this.timelineShowAll = timelineShowAll;
		this.timelineAddOpShow = timelineAddOpShow;
		this.timelineResizeShow = timelineResizeShow;
		this.timelineAllignShow = timelineAllignShow;
		this.timelineGroupShow = timelineGroupShow;
		this.timelineHideShow = timelineHideShow;
		this.timelineMirrorShow = timelineMirrorShow;
		this.timelineFilletShow = timelineFilletShow;
		this.timelineExtrudeShow = timelineExtrudeShow;
		this.timelineRadialShow = timelineRadialShow;
		this.timelineLinearShow = timelineLinearShow;
		this.timelineDeleteShow = timelineDeleteShow;
		this.timelineMoveObjectShow = timelineMoveObjectShow;
		this.timelineOtherShow = timelineOtherShow;
		boxes = new ArrayList<CheckBox>(Arrays.asList(timelineAddOpShow, timelineResizeShow, timelineAllignShow,
				timelineGroupShow, timelineHideShow, timelineMirrorShow, timelineFilletShow, timelineExtrudeShow,
				timelineRadialShow, timelineLinearShow, timelineDeleteShow, timelineMoveObjectShow, timelineOtherShow));

		timelineShowButtons.getChildren().clear();
		clear();
	}

	public static Image resizeImage(Image originalImage, int targetWidth, int targetHeight) {

		// // Read pixels from the original image
		// PixelReader pixelReader = originalImage.getPixelReader();
		// int originalWidth = (int) originalImage.getWidth();
		// int originalHeight = (int) originalImage.getHeight();
		//
		// // Create a new WritableImage with target dimensions
		// WritableImage resizedImage = new WritableImage(targetWidth, targetHeight);
		// PixelWriter pixelWriter = resizedImage.getPixelWriter();
		//
		// // Calculate scale factors
		// double scaleX = (double) originalWidth / targetWidth;
		// double scaleY = (double) originalHeight / targetHeight;
		//
		// // Perform nearest-neighbor scaling
		// for (int y = 0; y < targetHeight; y++) {
		// for (int x = 0; x < targetWidth; x++) {
		// // Map target coordinates to source coordinates
		// int srcX = (int) (x * scaleX);
		// int srcY = (int) (y * scaleY);
		//
		// // Clamp to valid bounds
		// srcX = Math.min(srcX, originalWidth - 1);
		// srcY = Math.min(srcY, originalHeight - 1);
		//
		// // Copy the pixel
		// int argb = pixelReader.getArgb(srcX, srcY);
		// pixelWriter.setArgb(x, y, argb);
		// }
		// }

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
		if ((one == null) || (two == null))
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
		ap.get().setTimelineVisible(timelineOpen);
		if (baseBox == null)
			return;
		// com.neuronrobotics.sdk.common.Log.debug("Timeline Update called");
		this.clear = clear;
		updateNeeded = true;
		if (updating)
			return;
		updating = true;
		updateNeeded = false;

		ArrayList<CaDoodleOperation> operations = ap.get().getOperations();

		BowlerStudio.runLater(() -> {

			if (timeline != null) {
				if (clear)
					clear();
			} else {
				timeline = new GridPane();
				baseBox.getChildren().add(timeline);
			}

			int space = 20;
			timeline.setHgap(space / 2); // Horizontal gap between columns
			timeline.setVgap(space); // Vertical gap between rows

			// Center the entire GridPane content
			timeline.setAlignment(Pos.CENTER);
			// timeline.setGridLinesVisible(true); // Shows grid lines for debugging

			// Don't control the prefWidth, use padding on the sides
			// baseBox.setPrefWidth((operations.size() + 2) * (buttonSize + space / 2 +
			// 4));

			// No auto scrolling of the timeline scroll pane
			// timelineScroll.setHvalue(((double)ap.get().getCurrentIndex())/((double)operations.size()));

			new Thread(() -> {
				while (ap.get().isRegenerating() || !ap.get().isInitialized()) {
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
				int s = operations.size();

				for (int i = buttons.size(); i < Math.max(s, ap.get().getCurrentIndex()); i++) {
					try {
						CaDoodleOperation op = operations.get(i);

						List<CSG> state = ap.get().getStateAtOperation(op);
						if (op == null)
							continue;
						int myIndex = i;
						addrem = true;
						List<CSG> previous = (myIndex == 0) ? new ArrayList<CSG>()
								: ap.get().getStateAtOperation(operations.get(myIndex - 1));
						File f = ap.get().getTimelineImageFile(myIndex - 1);
						Image image = new Image(f.toURI().toString());

						BowlerStudio.runLater(() -> {
							ImageView value = new ImageView(resizeImage(image, buttonSize, buttonSize));
							String text = (myIndex + 1) + "\n" + op.getType();
							Button toAdd = new Button(text);
							if (AddFromScript.class.isInstance(op) || AddFromFile.class.isInstance(op)
									|| Sweep.class.isInstance(op)) {
								addButtons.add(toAdd);
								setupCheckBox(addButtons, timelineAddOpShow);
							} else if (MoveCenter.class.isInstance(op)) {
								moveButtons.add(toAdd);
								setupCheckBox(moveButtons, timelineMoveObjectShow);
							} else {
								otherButtons.add(toAdd);
								setupCheckBox(otherButtons, timelineOtherShow);
							}
							buttons.add(toAdd);
							BowlerStudio.runLater(() -> timeline.add(toAdd, myIndex, 0));

							int my = myIndex;
							ContextMenu contextMenu = new ContextMenu();
							toAdd.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
								session.setKeyBindingFocus();
								if (ap.get().isRegenerating() || !ap.get().isInitialized())
									return;
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
									if (event.getButton() == MouseButton.SECONDARY && myIndex > 0) {
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
											engine.addObject(c, null, 0.4, ap.get().getCsgDBinstance());
									}
							});
							toAdd.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
								int index = ap.get().getCurrentIndex() - 1;
								if (index != myButtonIndex)
									for (CSG c : state)
										engine.removeObject(c);
							});

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
							Separator verticalSeparator = new Separator();
							verticalSeparator.setOrientation(Orientation.VERTICAL);
							verticalSeparator.setPrefHeight(buttonSize); // Set height to 80 units
							// timeline.getChildren().add(verticalSeparator);

							// Create a delete menu item
							MenuItem deleteItem = new MenuItem("Delete");
							deleteItem.getStyleClass().add("image-button-focus");
							deleteItem.setOnAction(event -> {
								if (ap.get().isRegenerating() || !ap.get().isInitialized())
									return;
								toAdd.setDisable(true);
								buttons.remove(toAdd);
								timeline.getChildren().remove(toAdd);
								try {
									ap.get().deleteOperation(op);
									for (CSG c : state)
										engine.removeObject(c);
								} catch (FailedToApplyOperation t) {
									Log.error(t);
								}
							});

							deleteItem.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
								int index = ap.get().getCurrentIndex() - 1;
								if (index != myButtonIndex)
									for (CSG c : state)
										engine.removeObject(c);
							});
							// Add the delete item to the context menu
							contextMenu.getItems().add(deleteItem);

							// Add "Delete all after" to the timeline context menu
							MenuItem deleteAfterItem = new MenuItem("Delete all after");
							deleteAfterItem.setOnAction(event -> {
								contextMenu.hide();
								if (ap.get().isRegenerating() || !ap.get().isInitialized())
									return;
								contextMenu.hide();
								for (CSG c : state)
									engine.removeObject(c);
								new Thread(() -> {
									if (my != ap.get().getCurrentIndex())
										ap.get().moveToOpIndex(my);
									ap.get().deleteTailFromCurrent();
								}).start();

							});
							contextMenu.getItems().add(deleteAfterItem);
						});
						// Add event handler for right-click
					} catch (Exception ex) {
						com.neuronrobotics.sdk.common.Log.error(ex);
					}
				}

				com.neuronrobotics.sdk.common.Log.debug("Timeline updated");
				if (addrem || firstTime) {
					BowlerStudio.runLater(java.time.Duration.ofMillis(200), () -> {

						// Scroll only to end if not inserting operation
						if (ap.get().getCurrentIndex() == operations.size()) {
							timelineScroll.applyCss();
							timelineScroll.layout();
							timelineScroll.setHvalue(1.0);
						}
						// Don't auto scroll
						// timelineScroll.setHvalue(((double) ap.get().getCurrentIndex()) / ((double)
						// operations.size()));
						// }
						updating = false;
						if (updateNeeded)
							update(clear);
						session.updateControlsDisplayOfSelected();
					});
				} else {
					updating = false;
					if (updateNeeded)
						update(clear);
					BowlerStudio.runLater(() -> session.updateControlsDisplayOfSelected());
				}
				session.setKeyBindingFocus();

			}).start();
		});
	}

	private void setupCheckBox(ArrayList<Button> moveButtons, CheckBox tmp) {
		if (!timelineShowButtons.getChildren().contains(tmp)) {
			timelineShowButtons.getChildren().add(tmp);
			tmp.setOnAction((ev) -> {
				setupCHeckboxEvent(moveButtons, tmp);
			});
		}
	}

	private void setupCHeckboxEvent(ArrayList<Button> moveButtons, CheckBox cb) {
		boolean value = !cb.isSelected();
		for (Button b : moveButtons) {
			b.setVisible(!value);
			b.getStyleClass().clear();
			if (value)
				b.getStyleClass().add("image-button-highlight");
			else
				b.getStyleClass().add("image-button");
		}
		if (!cb.isSelected())
			timelineShowAll.setSelected(false);
	}

	public void clear() {
		// com.neuronrobotics.sdk.common.Log.debug("Old Timeline buttons cleared");
		if (buttons != null)
			buttons.clear();
		if (timeline != null)
			timeline.getChildren().clear();

		showAllAction = e -> {
			for (CheckBox cb : boxes) {
				cb.setSelected(timelineShowAll.isSelected());
				EventHandler<ActionEvent> onAction = cb.getOnAction();
				if (onAction != null)
					onAction.handle(e);
			}
		};
		timelineShowAll.setSelected(true);
		timelineShowAll.setOnAction(showAllAction);
		timelineShowButtons.getChildren().clear();
	}

	public void updateSelected(LinkedHashSet<CSG> selected) {

		CaDoodleFile caDoodleFile = ap.get();
		if (caDoodleFile == null)
			return;
		ArrayList<CaDoodleOperation> operations = caDoodleFile.getOperations();
		for (int i = 0; ((i < operations.size()) && (i < buttons.size())); i++) {
			CaDoodleOperation op = operations.get(i);
			if (op == null)
				continue;
			Button b = buttons.get(i);
			boolean applyToMe = false;
			int index = caDoodleFile.getCurrentIndex() - 1;
			if (index >= buttons.size())
				continue;
			Button sel = buttons.get(index < 0 ? 0 : index);
			for (String s : op.getNamesAddedInThisOperation()) {
				for (CSG p : selected) {
					if (s.contentEquals(p.getName()))
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
		this.timelineOpen = timelineOpen;
		if (timelineOpen)
			session.save();
	}

} // TimelineManager
