package com.commonwealthrobotics;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

import com.neuronrobotics.bowlerstudio.scripting.cadoodle.*;

public class ButtonWithOverlayImage extends Button {

	private StackPane stack;
	private ImageView toolimage;
	private StackPane editMarker;
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

	public void setEditableMarkerVisible(boolean visible) {
		if (visible && editMarker == null) {
			SVGPath wrench = new SVGPath();
			wrench.setContent("M12.9 1.1 C11.6 .7 10.1 1 9.1 2 C8.2 2.9 7.9 4.2 8.2 5.4 "
					+ "L2.4 11.2 C1.8 11.8 1.8 12.8 2.4 13.4 C3 14 4 14 4.6 13.4 "
					+ "L10.4 7.6 C11.6 7.9 12.9 7.6 13.8 6.7 C14.8 5.7 15.1 4.2 14.7 2.9 "
					+ "L12.4 5.2 L10.5 4.9 L10.2 3 Z");
			wrench.setStyle("-fx-fill: -fx-accent;");

			editMarker = new StackPane(wrench);
			editMarker.setMinSize(18, 18);
			editMarker.setPrefSize(18, 18);
			editMarker.setMaxSize(18, 18);
			editMarker.setStyle(
					"-fx-background-color: -fx-base; " + "-fx-background-radius: 5; " + "-fx-background-insets: 0;");
			editMarker.setMouseTransparent(true);

			stack.getChildren().add(editMarker);
			StackPane.setAlignment(editMarker, Pos.BOTTOM_RIGHT);
			StackPane.setMargin(editMarker, new Insets(0, 1, 1, 0));
		}
		if (editMarker != null)
			editMarker.setVisible(visible);
	}

	public void updatemainImage(Image resizeImage) {
		value.setImage(resizeImage);
	}
}
