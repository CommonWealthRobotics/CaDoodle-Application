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

	public TimelineManager(ActiveProject activeProject) {
		this.ap = activeProject;
		ap.addListener(new ICaDoodleStateUpdate() {
			boolean init=false;
			@Override
			public void onWorkplaneChange(TransformNR newWP) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onUpdate(List<CSG> currentState, ICaDoodleOpperation source, CaDoodleFile file) {
				if(init)
					update();
			}

			@Override
			public void onSaveSuggestion() {

			}

			@Override
			public void onRegenerateStart() {

			}

			@Override
			public void onRegenerateDone() {
				update();
			}

			@Override
			public void onInitializationStart() {
				init=true;
			}

			@Override
			public void onInitializationDone() {
				update();
				init=false;
			}

			@Override
			public void onTimelineUpdate() {
				update();
			}
		});
	}

	public void set(ScrollPane timelineScroll, HBox timeline) {
		this.timelineScroll = timelineScroll;
		this.timeline = timeline;

	}

	public void update() {
		int ci = ap.get().getCurrentIndex();

		BowlerStudio.runLater(() -> {
			boolean addrem = false;
			while (ci < buttons.size() && !ap.get().isForwardAvailible()) {
				Button toRem = buttons.remove(buttons.size() - 1);
				timeline.getChildren().remove(toRem);
				addrem = true;
			}
			//clear();
			ArrayList<ICaDoodleOpperation> opperations = ap.get().getOpperations();
			for (int i = buttons.size(); i < ci; i++) {
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
					File f = ap.get().getTimelineImageFile(i);
					//System.out.println("Laoding Image to timeline " + f.getAbsolutePath());
					Image image = new Image(f.toURI().toString());
					ImageView value = new ImageView(image);
					value.setFitHeight(80);
					value.setFitWidth(80);
					toAdd.getStyleClass().add("image-button");
					toAdd.setContentDisplay(ContentDisplay.TOP);
					toAdd.setGraphic(value);
					Tooltip tooltip = new Tooltip(text);
					toAdd.setTooltip(tooltip);
					buttons.add(toAdd);
					timeline.getChildren().add(toAdd);
					addrem = true;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			for (int i = 0; i < buttons.size(); i++)
				buttons.get(i).setDisable(false);
			buttons.get(ci - 1).setDisable(true);
			if (addrem)
				Platform.runLater(() -> timelineScroll.setHvalue(1.0));
		});

	}

	public void clear() {
		buttons.clear();
		BowlerStudio.runLater(() -> timeline.getChildren().clear());
	}

}
