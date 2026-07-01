package com.commonwealthrobotics;

import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;


import com.neuronrobotics.bowlerstudio.scripting.cadoodle.*;

public class ButtonWithOverlayImage extends Button {

	private StackPane stack;
	private ImageView toolimage;
	private Image image;
	Separator separator = new Separator(Orientation.VERTICAL);
	private ImageView value;
	public HBox hbox;


	public ButtonWithOverlayImage(String text, Image image, int buttonSize, double overlaySize) {
		super(text);
		this.image = image;
		getStyleClass().add("image-button");
		setContentDisplay(ContentDisplay.TOP);
		separator.getStyleClass().clear();
		separator.getStyleClass().add("timeline-block");
		value = new ImageView(TimelineManager.resizeImage(image, buttonSize, buttonSize));
		value.setFitWidth(buttonSize);
		value.setFitHeight(buttonSize);

		toolimage = new ImageView();


		toolimage.setFitWidth(overlaySize);
		toolimage.setFitHeight(overlaySize);

		double radius = overlaySize * Math.sqrt(2) / 2.0;
		Circle bg = new Circle(radius, Color.web("#263d8c"));

		StackPane badge = new StackPane(toolimage);
		badge.setAlignment(Pos.CENTER);
		badge.setMaxSize(radius * 2, radius * 2);

		stack = new StackPane();
		stack.setPrefSize(buttonSize, buttonSize);
		stack.getChildren().add(value);
		stack.getChildren().add(badge);
		StackPane.setAlignment(badge, Pos.BOTTOM_RIGHT);

		hbox = new HBox(this, separator);
		hbox.setAlignment(Pos.CENTER);

		setGraphic(stack);
	}

	public void setButtonImageType(ObservableList<String> styleClass) {
		toolimage.getStyleClass().addAll(styleClass);
	}

	public void updatemainImage(Image resizeImage) {
		value.setImage(resizeImage);
	}
}
