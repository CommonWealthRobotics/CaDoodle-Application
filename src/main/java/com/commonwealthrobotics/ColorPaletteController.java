package com.commonwealthrobotics;

import java.io.IOException;
import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Popup;

public class ColorPaletteController {

	@FXML
	private Button colorButton;

	private Consumer<Color> onColorSelected;

	public void setCurrentColor(Color color) {
		String hexColor = String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255),
				(int) (color.getBlue() * 255));

		colorButton.setStyle("-fx-background-color: " + hexColor + ";");
	}

	public void setOnColorSelected(Consumer<Color> onColorSelected) {
		this.onColorSelected = onColorSelected;
	}

	@FXML
	private void initialize() {
		colorButton.setOnAction(e -> showPalettePopup());
	}

	private void showPalettePopup() {
		Popup popup = new Popup();
		popup.setAutoHide(true);

		try {
			FXMLLoader loader = new FXMLLoader(ColorPaletteController.class.getResource("ColorPalettePopup.fxml"),
					ActiveProject.getLangaugePack());

			StackPane content = loader.load();
			ActiveProject.setStyleSheet(content);

			ColorPalettePopupController popupController = loader.getController();
			popupController.setOnColorSelected(onColorSelected);
			popupController.setPopup(popup);

			popup.getContent().setAll(content);

			popup.show(colorButton, colorButton.localToScreen(0, 0).getX(),
					colorButton.localToScreen(0, 0).getY() + colorButton.getHeight());

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
