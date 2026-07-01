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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.*;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.Log;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
public class TimelineButton extends Button {

	private StackPane stack;
	private ImageView toolimage;
	private Image image;
	Separator separator = new Separator(Orientation.VERTICAL);
	private ImageView value;
	public HBox hbox;
	public static int buttonSize = 80;

	public TimelineButton(String text, Image image) {
		super(text);
		this.image = image;
		getStyleClass().add("image-button");
		setContentDisplay(ContentDisplay.TOP);
		separator.getStyleClass().clear();
		separator.getStyleClass().add("timeline-block");
	}

	public void setButtonImageType(ObservableList<String> styleClass) {
		value = new ImageView(TimelineManager.resizeImage(image, buttonSize, buttonSize));
		value.setFitWidth(buttonSize);
		value.setFitHeight(buttonSize);

		toolimage = new ImageView();
		toolimage.getStyleClass().addAll(styleClass);
		double overlaySize = buttonSize / 3.0;
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

	public void updatemainImage(Image resizeImage) {
		value.setImage(resizeImage);
	}
}
