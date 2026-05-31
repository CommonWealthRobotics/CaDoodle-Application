package com.commonwealthrobotics;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import com.neuronrobotics.bowlerstudio.scripting.cadoodle.OperationResult;

/**
 * Controller for ChangeOptionsDialog.fxml.
 *
 * Usage:
 *   OperationResult result = ChangeOptionsController.show(ownerStage);
 */
public class ChangeOptionsController {

    // ── FXML-injected controls ────────────────────────────────────────────────

    @FXML private Button continueButton;
    @FXML private Button insertButton;
    @FXML private Button abortButton;

    // ── Internal state ────────────────────────────────────────────────────────

    /** Result written by button handlers; read back by show() after close. */
    private OperationResult result = OperationResult.ABORT;

    /** The Stage that owns this controller – set by show() after load. */
    private Stage stage;

    // ── Static factory / launch method ───────────────────────────────────────

    /**
     * Loads and displays the Change Options dialog modally, then returns the
     * user's choice.  Must be called on the JavaFX Application Thread.
     *
     * @param owner  the owner {@link Stage}; may be {@code null}
     * @return the {@link OperationResult} chosen by the user, or
     *         {@link OperationResult#ABORT} if the window is closed without
     *         a selection
     */
    public static OperationResult show(Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ChangeOptionsController.class.getResource("/ChangeOptionsDialog.fxml"));
            Parent root = loader.load();

            ChangeOptionsController controller = loader.getController();

            Stage dialog = new Stage(StageStyle.DECORATED);
            dialog.initModality(Modality.WINDOW_MODAL);
            if (owner != null) {
                dialog.initOwner(owner);
            }
            dialog.setTitle("Change Options");
            dialog.setResizable(false);

            Scene scene = new Scene(root);

            // ── Keyboard shortcuts ────────────────────────────────────────────
            scene.setOnKeyPressed(ke -> {
                if (ke.getCode() == KeyCode.ESCAPE) {
                    controller.result = OperationResult.ABORT;
                    dialog.close();
                    ke.consume();
                }
            });
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.C), () -> {
                controller.result = OperationResult.PRUNE;
                dialog.close();
            });
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.I), () -> {
                controller.result = OperationResult.INSERT;
                dialog.close();
            });
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.A), () -> {
                controller.result = OperationResult.ABORT;
                dialog.close();
            });

            // Close-button (X) → ABORT
            dialog.setOnCloseRequest(e -> controller.result = OperationResult.ABORT);

            // Give the controller a reference so button handlers can close it
            controller.stage = dialog;

            dialog.setScene(scene);

            // Apply your application stylesheet if you have one
            // scene.getStylesheets().add(
            //     ChangeOptionsController.class.getResource("/css/app.css").toExternalForm());

            dialog.showAndWait();   // blocks until dialog.close() is called

            return controller.result;

        } catch (IOException e) {
            throw new RuntimeException("Failed to load ChangeOptionsDialog.fxml", e);
        }
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
}
