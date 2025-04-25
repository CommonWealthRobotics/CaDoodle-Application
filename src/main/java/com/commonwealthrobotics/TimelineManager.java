package com.commonwealthrobotics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleOpperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
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
				update(false);
			}

			@Override
			public void onInitializationStart() {
				update(false);
			}
			@Override
			public void onInitializationDone() {
				update(false);
			}
			@Override
			public void onTimelineUpdate() {
				new Exception().printStackTrace();
				update(true);
			}
		});
	}

	public void set(ScrollPane timelineScroll, HBox timeline, SelectionSession session) {
		this.timelineScroll = timelineScroll;
		this.timeline = timeline;
		this.session = session;

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
					String text = (i + 1) + "\n" + op.getType();
					Button toAdd = new Button(text);
					int my = i;
					ContextMenu contextMenu = new ContextMenu();
					toAdd.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
						BowlerStudio.runLater(() -> {
							if (event.getButton() == MouseButton.PRIMARY) {
								int index = ap.get().getCurrentIndex() - 1;
								Button button = buttons.get(index < 0 ? 0 : index);
								contextMenu.hide();
								if (button == toAdd)
									return;
								new Thread(() -> {
									ap.get().moveToOpIndex(my);
								}).start();
								session.setKeyBindingFocus();
							}
							
						});
		
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
					toAdd.setTooltip(tooltip);
					buttons.add(toAdd);
					timeline.getChildren().add(toAdd);
					addrem = true;
					// Create a delete menu item
					MenuItem deleteItem = new MenuItem("Delete");
					deleteItem.getStyleClass().add("image-button-focus");
					deleteItem.setOnAction(event -> {
						ap.get().deleteOperation(op);
					});
					// Add the delete item to the context menu
					contextMenu.getItems().add(deleteItem);
					// Add event handler for right-click
					toAdd.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
						BowlerStudio.runLater(() -> {
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
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			if (buttons.size() > 0) {
				int index = ap.get().getCurrentIndex() - 1;
				Button button = buttons.get(index < 0 ? 0 : index);
				for (int i = 0; i < buttons.size(); i++) {
					Button button2 = buttons.get(i);
					button2.getStyleClass().clear();
					if (button == button2)
						continue;
					button2.getStyleClass().add("image-button");
				}
				button.getStyleClass().add("image-button-focus");
				// Create a context menu
			}
			//System.out.println("Timeline updated");
			if (addrem)
				Platform.runLater(() -> {
					timelineScroll.setHvalue(1.0);
					updating = false;
					if(updateNeeded)
						update(clear);
				});
			else {
				updating = false;
				if(updateNeeded)
					update(clear);
			}
			
		});
	}

	public void clear() {
		System.out.println("Old Timeline buttons cleared");
		buttons.clear();
		timeline.getChildren().clear();
	}

}
