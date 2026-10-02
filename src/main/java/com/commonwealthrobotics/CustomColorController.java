package com.commonwealthrobotics;

import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Popup;

public class CustomColorController {

	@FXML
	private AnchorPane colorArea;

	@FXML
	private AnchorPane hueArea;

	@FXML
	private Region saturationLayer;

	@FXML
	private Region brightnessLayer;

	@FXML
	private Region colorMarker;

	@FXML
	private Region hueMarker;

	@FXML
	private Region preview;

	private Consumer<Color> onColorSelected;
	private Popup popup;

	private double hue = 0.0;
	private double saturation = 1.0;
	private double brightness = 1.0;

	@FXML
	private void initialize() {
		updateUI();
	}

	public void setOnColorSelected(Consumer<Color> onColorSelected) {
		this.onColorSelected = onColorSelected;
	}

	public void setPopup(Popup popup) {
		this.popup = popup;
	}

	public void setInitialColor(Color color) {
		if (color == null) {
			return;
		}

		hue = color.getHue();
		saturation = color.getSaturation();
		brightness = color.getBrightness();

		updateUI();
	}

	@FXML
	private void onColorArea(MouseEvent event) {
		double width = colorArea.getWidth();
		double height = colorArea.getHeight();

		if (width <= 0 || height <= 0) {
			return;
		}

		double x = clamp(event.getX(), 0, width);
		double y = clamp(event.getY(), 0, height);

		saturation = x / width;
		brightness = 1.0 - (y / height);

		updateUI();
	}

	@FXML
	private void onHueArea(MouseEvent event) {
		double width = hueArea.getWidth();

		if (width <= 0) {
			return;
		}

		double x = clamp(event.getX(), 0, width);
		hue = (x / width) * 360.0;

		updateUI();
	}

	@FXML
	private void onUseColor() {
		if (onColorSelected != null) {
			onColorSelected.accept(getCurrentColor());
		}

		if (popup != null) {
			popup.hide();
		}
	}

	@FXML
	private void onCancel() {
		if (popup != null) {
			popup.hide();
		}
	}

	private void updateUI() {
		Color hueColor = Color.hsb(hue, 1.0, 1.0);
		String hueHex = toHex(hueColor);

		saturationLayer.setStyle("-fx-background-color: linear-gradient(to right, white 0%, " + hueHex + " 100%);"
				+ "-fx-border-color: black;" + "-fx-border-width: 1;");

		Color current = getCurrentColor();

		preview.setStyle(
				"-fx-background-color: " + toHex(current) + ";" + "-fx-border-color: black;" + "-fx-border-width: 1;");

		double colorWidth = colorArea.getWidth() > 0 ? colorArea.getWidth() : colorArea.getPrefWidth();
		double colorHeight = colorArea.getHeight() > 0 ? colorArea.getHeight() : colorArea.getPrefHeight();

		colorMarker.setLayoutX((saturation * colorWidth) - (colorMarker.getPrefWidth() / 2.0));
		colorMarker.setLayoutY(((1.0 - brightness) * colorHeight) - (colorMarker.getPrefHeight() / 2.0));

		double hueWidth = hueArea.getWidth() > 0 ? hueArea.getWidth() : hueArea.getPrefWidth();

		hueMarker.setLayoutX((hue / 360.0) * hueWidth - (hueMarker.getPrefWidth() / 2.0));
		hueMarker.setLayoutY(0);
	}

	private Color getCurrentColor() {
		return Color.hsb(hue, saturation, brightness);
	}

	private String toHex(Color color) {
		return String.format("#%02X%02X%02X", (int) Math.round(color.getRed() * 255),
				(int) Math.round(color.getGreen() * 255), (int) Math.round(color.getBlue() * 255));
	}

	private double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
