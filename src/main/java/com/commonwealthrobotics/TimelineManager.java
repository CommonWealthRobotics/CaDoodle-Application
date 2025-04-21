package com.commonwealthrobotics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleOpperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class TimelineManager {

	private ScrollPane timelineScroll;
	private HBox timeline;
	private ActiveProject ap;
	private ArrayList<Button> buttons = new ArrayList<Button>();
	private boolean updating = false;

	public TimelineManager(ActiveProject activeProject) {
		this.ap = activeProject;
		ap.addListener(new ICaDoodleStateUpdate() {
			boolean init = false;

			@Override
			public void onWorkplaneChange(TransformNR newWP) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onUpdate(List<CSG> currentState, ICaDoodleOpperation source, CaDoodleFile file) {
//				if (init)
//					update(false);
			}

			@Override
			public void onSaveSuggestion() {

			}

			@Override
			public void onRegenerateStart() {

			}

			@Override
			public void onRegenerateDone() {
				update(true);
			}

			@Override
			public void onInitializationStart() {
				init = true;
			}

			@Override
			public void onInitializationDone() {
				update(false);
				init = false;
			}

			@Override
			public void onTimelineUpdate() {
				update(false);
			}
		});
	}

	public void set(ScrollPane timelineScroll, HBox timeline) {
		this.timelineScroll = timelineScroll;
		this.timeline = timeline;

	}

	public void update(boolean clear) {
		while (updating) {
			try {
				Thread.sleep(16);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			//System.out.println("Start Waiting for timeline to finish");
			//new Exception().printStackTrace();
		}
		updating = true;

		BowlerStudio.runLater(() -> {
			boolean addrem = false;
			if(clear)
				clear();
			while ( ap.get().getCurrentIndex() < (buttons.size() - 1) && !ap.get().isForwardAvailible()) {
				Button toRem = buttons.remove(buttons.size() - 1);
				timeline.getChildren().remove(toRem);
				addrem = true;
			}
			ArrayList<ICaDoodleOpperation> opperations = ap.get().getOpperations();
			int s=opperations.size();
			for (int i = buttons.size(); i < Math.max(s,  ap.get().getCurrentIndex()); i++) {
				try {
					ICaDoodleOpperation opp = opperations.get(i);
					String text = (i + 1) + "\n" + opp.getType();
					Button toAdd = new Button(text);
					int my = i;
					toAdd.setOnAction(ev -> {
						new Thread(() -> {
							ap.get().moveToOpIndex(my);
						}).start();
					});
					File f = ap.get().getTimelineImageFile(i - 1);
					Image image = new Image(f.toURI().toString());
					ImageView value = new ImageView(image);
					ImageView toolimage = new ImageView(image);
					value.setFitHeight(80);
					value.setFitWidth(80);
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
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			if (buttons.size() > 0) {
				for (int i = 0; i < buttons.size(); i++)
					buttons.get(i).setDisable(false);
				int index =  ap.get().getCurrentIndex() - 1;
				buttons.get(index < 0 ? 0 : index).setDisable(true);
			}
			if (addrem)
				Platform.runLater(() -> {
					timelineScroll.setHvalue(1.0);
					updating = false;
				});
			else
				updating = false;
		});
//		while (updating) {
//			try {
//				Thread.sleep(16);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//				return;
//			}
//		}
	}

	public void clear() {
		buttons.clear();
		timeline.getChildren().clear();
	}

}
