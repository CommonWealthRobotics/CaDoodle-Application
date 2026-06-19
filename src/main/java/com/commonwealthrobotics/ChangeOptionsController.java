package com.commonwealthrobotics;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.SplashManager;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.OperationResult;


/**
 * Controller for ChangeOptionsDialog.fxml.
 *
 * Usage: OperationResult result = ChangeOptionsController.show(ownerStage);
 */
public class ChangeOptionsController {

	// ── FXML-injected controls ────────────────────────────────────────────────

	@FXML
	private Button continueButton;
	@FXML
	private Button insertButton;
	@FXML
	private Button abortButton;
	@FXML
	private GridPane topLevel;
	// ── Internal state ────────────────────────────────────────────────────────

	/** Result written by button handlers; read back by show() after close. */
	private static OperationResult result = OperationResult.ABORT;

	/** The Stage that owns this controller – set by show() after load. */
	private static Stage stage;

	@FXML // This method is called by the FXMLLoader when initialization is complete
	void initialize() {
		ActiveProject.setStyleSheet(topLevel);

	}
	// ── FXML action handlers ──────────────────────────────────────────────────

	@FXML
	private void onContinue() {
		result = OperationResult.PRUNE;
		stage.close();
	}

	@FXML
	private void onInsert() {
		result = OperationResult.INSERT;
		stage.close();
	}

	@FXML
	private void onAbort() {
		result = OperationResult.ABORT;
		stage.close();
	}

	public static OperationResult launch() {
		AtomicReference<OperationResult> result = new AtomicReference<>(OperationResult.ABORT);
		CountDownLatch latch = new CountDownLatch(1);

		BooleanSupplier ogcp = SplashManager.getClosePreventer();
		BooleanSupplier ogop = SplashManager.getOpenPreventer();

		SplashManager.setOpenPreventer(() -> true);
		SplashManager.setClosePreventer(() -> false);


		BowlerStudio.runLater(() -> {
			try {
				com.neuronrobotics.sdk.common.Log
						.debug("Resource URL: " + ProjectManager.class.getResource("ChangeOptionsDialog.fxml"));
				FXMLLoader loader = new FXMLLoader(SettingsManager.class.getClassLoader().getResource(
						"com/commonwealthrobotics/ChangeOptionsDialog.fxml"), ActiveProject.getLangaugePack());
				Parent root = loader.load();
				ChangeOptionsController controller = loader.getController();
				SplashManager.closeSplash();
				stage = new Stage();
				stage.setTitle("CaDoodle Settings");
				Scene scene = new Scene(root);
				stage.setScene(scene);;
				stage.initModality(Modality.APPLICATION_MODAL);
				// Fires when the user clicks the OS close button
				stage.setOnCloseRequest(event -> {
					result.set(ChangeOptionsController.result);
					// Don't countDown here — let onHidden handle it
				});
				// Fires whenever the stage is hidden, regardless of HOW it closed
				// (OS close button, stage.close(), stage.hide(), button in controller, etc.)
				stage.setOnHidden(event -> {
					result.set(ChangeOptionsController.result);
					latch.countDown();
				});
				scene.setOnKeyPressed(event -> {
					switch (event.getCode()) {
						case C :
							controller.onContinue();
							break;
						case I :
							controller.onInsert();
							break;
						case A :
							controller.onAbort();
							break;
						default :
							break;
					}
				});
				stage.setOnShown(event -> {
					BowlerStudio.runLater(200, () -> {
						stage.toFront();
						stage.requestFocus();
						controller.topLevel.requestFocus();
					});
				});
				stage.show();


			} catch (IOException e) {
				com.neuronrobotics.sdk.common.Log.error(e);
				latch.countDown(); // Unblock the waiting thread even on failure
			}
		});

		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		SplashManager.setOpenPreventer(ogop);
		SplashManager.setClosePreventer(ogcp);
		return result.get();
	}

}
