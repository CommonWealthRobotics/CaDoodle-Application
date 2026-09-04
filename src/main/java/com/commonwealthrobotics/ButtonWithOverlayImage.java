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

import com.neuronrobotics.bowlerstudio.scripting.cadoodle.*;

public class ButtonWithOverlayImage extends Button {

	private StackPane stack;
	private ImageView toolimage;
	private Image image;
	Separator separator = new Separator(Orientation.VERTICAL);
	private ImageView value;
	public HBox hbox;

	public ButtonWithOverlayImage(String text, Image image, int buttonSize, double overlaySize, int insetDistance) {
		super(text);
		this.image = image;
		getStyleClass().add("image-button");
		if (text.length() > 0)
			setContentDisplay(ContentDisplay.TOP);
		else
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		separator.getStyleClass().clear();
		separator.getStyleClass().add("timeline-block");
		value = new ImageView(TimelineManager.resizeImage(image, buttonSize, buttonSize, insetDistance));
		value.setFitWidth(buttonSize);
		value.setFitHeight(buttonSize);

		toolimage = new ImageView();

		toolimage.setFitWidth(overlaySize);
		toolimage.setFitHeight(overlaySize);
		toolimage.setTranslateX(buttonSize / 2 - overlaySize / 2);
		toolimage.setTranslateY(buttonSize / 2 - overlaySize / 2);

		stack = new StackPane();
		// stack.setPrefSize(buttonSize, buttonSize);
		stack.getChildren().add(value);
		stack.getChildren().add(toolimage);

		hbox = new HBox(this, separator);
		hbox.setAlignment(Pos.CENTER);
		setMinSize(buttonSize, buttonSize);
		setGraphic(stack);
	}

	public void setButtonImageType(ObservableList<String> styleClass) {
		toolimage.getStyleClass().addAll(styleClass);
	}

	public void updatemainImage(Image resizeImage) {
		value.setImage(resizeImage);
	}
}
