package com.commonwealthrobotics;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import com.neuronrobotics.bowlerstudio.scripting.CuratedColorPalette;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Popup;

public class ColorPalettePopupController {

	@FXML
	private GridPane paletteGrid;

	private Consumer<Color> onColorSelected;
	private Popup popup;

	@FXML
	private void initialize() {
		buildPalette();
	}

	public void setOnColorSelected(Consumer<Color> onColorSelected) {
		this.onColorSelected = onColorSelected;
	}

	public void setPopup(Popup popup) {
		this.popup = popup;
	}

	private void buildPalette() {
		paletteGrid.getChildren().clear();

		List<List<Color>> rows = CuratedColorPalette.getRows();

		for (int row = 0; row < rows.size(); row++) {
			List<Color> colors = rows.get(row);

			for (int column = 0; column < colors.size(); column++) {
				Color color = colors.get(column);

				Button button = new Button();
				button.setPrefWidth(32.0);
				button.setPrefHeight(32.0);
				button.setMinWidth(32.0);
				button.setMinHeight(32.0);

				button.setStyle("-fx-background-color: " + toHex(color) + ";");

				button.setOnAction(event -> selectColor(color));

				paletteGrid.add(button, column, row);
			}
		}
	}

	@FXML
	private void onCustomColor(ActionEvent event) {
		try {
			FXMLLoader loader = new FXMLLoader(ColorPalettePopupController.class.getResource("CustomColor.fxml"),
					ActiveProject.getLangaugePack());

			StackPane content = loader.load();
			ActiveProject.setStyleSheet(content);

			CustomColorController controller = loader.getController();
			controller.setInitialColor(Color.WHITE);
			controller.setOnColorSelected(this::selectColor);

			Popup customPopup = new Popup();
			customPopup.setAutoHide(true);

			controller.setPopup(customPopup);

			customPopup.getContent().setAll(content);

			Button anchor = (Button) event.getSource();

			customPopup.show(anchor, anchor.localToScreen(0, 0).getX(),
					anchor.localToScreen(0, 0).getY() + anchor.getHeight());

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void selectColor(Color color) {
		if (onColorSelected != null) {
			onColorSelected.accept(color);
		}

		if (popup != null) {
			popup.hide();
		}
	}

	private String toHex(Color color) {
		return String.format("#%02X%02X%02X", (int) Math.round(color.getRed() * 255),
				(int) Math.round(color.getGreen() * 255), (int) Math.round(color.getBlue() * 255));
	}
}
