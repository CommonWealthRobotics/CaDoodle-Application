package com.commonwealthrobotics;

import java.io.File;
import java.util.ArrayList;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleOpperation;

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

	}

	public void set(ScrollPane timelineScroll, HBox timeline) {
		this.timelineScroll = timelineScroll;
		this.timeline = timeline;

	}

	public void update() {
		int ci = ap.get().getCurrentIndex();
//		if (buttons.size() == ci) {
//			// no update
//			return;
//		}
		BowlerStudio.runLater(() -> {
			while (ci < buttons.size() && !ap.get().isForwardAvailible()) {
				Button toRem = buttons.remove(buttons.size() - 1);
				timeline.getChildren().remove(toRem);
			}
			for (int i = buttons.size(); i < ci; i++) {
				ICaDoodleOpperation opp = ap.get().getOpperations().get(i);
				String text = i + "\n" + opp.getType();
				Button toAdd = new Button(text);
				int my=i;
				toAdd.setOnAction(ev->{
					new Thread(()->{
						ap.get().moveToOpIndex(my);
					}).start();
				});
				File f = ap.get().getTimelineImageFile(i);
				Image image =new Image(f.toURI().toString());
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
			}
			for(int i=0;i<buttons.size();i++)
				buttons.get(i).setDisable(false);
			buttons.get(ci-1).setDisable(true);
			Platform.runLater(()->timelineScroll.setHvalue(1.0));
		});

	}

}
