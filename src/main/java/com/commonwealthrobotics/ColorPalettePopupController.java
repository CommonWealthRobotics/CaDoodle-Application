package com.commonwealthrobotics;

import java.io.IOException;
import java.util.function.Consumer;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.stage.Popup;

public class ColorPalettePopupController {

	private Consumer<Color> onColorSelected;
	private Popup popup;

	public void setOnColorSelected(Consumer<Color> onColorSelected) {
		this.onColorSelected = onColorSelected;
	}

	public void setPopup(Popup popup) {
		this.popup = popup;
	}

	@FXML
	private void onColorButton(ActionEvent event) {
		Button button = (Button) event.getSource();
		String hex = button.getUserData().toString();

		selectColor(Color.web("#" + hex));
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
}
