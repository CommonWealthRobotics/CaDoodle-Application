package com.commonwealthrobotics;

import java.io.File;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.SplashManager;
import com.neuronrobotics.bowlerstudio.scripting.ADMesh;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;

/**
 * Controller for StlRepairDialog.fxml.
 *
 * <p>Usage – call from any thread (blocks the caller until the user responds):
 * <pre>
 *   boolean fix = StlRepairDialogController.show(ownerWindow, file, throwable);
 * </pre>
 *
 * <p>The dialog is APPLICATION_MODAL and non-resizable. It reads all user-visible
 * strings from the application's {@link ResourceBundle} (Messages_*.properties),
 * so no hard-coded English text appears here.
 */
public class StlRepairDialogController implements Initializable {

	// ── FXML fields ──────────────────────────────────────────────────────────

	@FXML
	private VBox root;
	@FXML
	private Label headerLabel;
	@FXML
	private Label fileLabel;
	@FXML
	private Label errorLabel;
	@FXML
	private Label questionLabel;
	@FXML
	private CheckBox insideOutCheckBox;
	@FXML
	private Button yesButton;
	@FXML
	private Button noButton;

	// ── Internal state set before the dialog is shown ────────────────────────

	/** Latch released when the dialog is dismissed (any path). */
	private CountDownLatch latch;

	/** Written on the FX thread; read on the calling thread after latch. */
	private AtomicBoolean result;

	/** Stage that owns this dialog (set in {@link #show}). */
	private Stage stage;

	// ── Initializable ────────────────────────────────────────────────────────

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Styles that mirror the original inline CSS
		yesButton.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white;");
		noButton.setStyle("-fx-background-color: #c62828; -fx-text-fill: white;");
		ActiveProject.setStyleSheet(root);
	}

	// ── Package-private setters called by show() before stage.show() ─────────

	void configure(File file, Throwable throwable, AtomicBoolean result, CountDownLatch latch, Stage stage,
			ResourceBundle bundle) {

		this.result = result;
		this.latch = latch;
		this.stage = stage;

		// File label: "<bundle prefix>: <filename>"
		String filePfx = bundle.getString("stlrepair.file_label");
		fileLabel.setText(filePfx + " " + file.getName());

		// Error label: "<bundle prefix>: <message or fallback>"
		String errorPfx = bundle.getString("stlrepair.error_label");
		String errorMsg = (throwable != null && throwable.getMessage() != null)
				? throwable.getMessage()
				: bundle.getString("stlrepair.unknown_error");
		errorLabel.setText(errorPfx + " " + errorMsg);

		// Sync checkbox with current ADMesh setting
		insideOutCheckBox.setSelected(ADMesh.isReverseMesh());

		// Release the latch if the user clicks the window's X button
		stage.setOnCloseRequest(e -> {
			result.set(false);
			latch.countDown();
		});

		// Release the latch whenever the stage hides (covers yes/no paths too)
		stage.setOnHidden(e -> latch.countDown());
	}

	// ── FXML action handlers ──────────────────────────────────────────────────

	@FXML
	private void onYes() {
		result.set(true);
		stage.close();
	}

	@FXML
	private void onNo() {
		result.set(false);
		stage.close();
	}

	@FXML
	private void onInsideOutToggled() {
		ADMesh.setReverseMesh(insideOutCheckBox.isSelected());
	}

	// ── Static factory / entry point ─────────────────────────────────────────

	/**
	 * Show the STL-repair dialog and block the <em>calling</em> thread until
	 * the user has responded.
	 *
	 * <p>Safe to call from any thread (including the application main thread
	 * and background worker threads). Must NOT be called from the JavaFX
	 * Application Thread, because this method blocks.
	 *
	 * @param owner     optional owner window for the modal stage (may be null)
	 * @param file      the STL file that triggered the error
	 * @param throwable the exception/error that was caught (may be null)
	 * @return {@code true} if the user chose "Yes, Fix It"; {@code false} otherwise
	 */
	public static boolean show(Window owner, File file, Throwable throwable) {
		if (Platform.isFxApplicationThread()) {
			throw new IllegalStateException("StlRepairDialogController.show() must not be called on the FX thread "
					+ "because it blocks waiting for user input. " + "Call it from a background thread instead.");
		}

		AtomicBoolean result = new AtomicBoolean(false);
		CountDownLatch latch = new CountDownLatch(1);
		BooleanSupplier cp = SplashManager.getClosePreventer();
		SplashManager.setClosePreventer(new BooleanSupplier() {
			@Override
			public boolean getAsBoolean() {
				// TODO Auto-generated method stub
				return false;
			}
		});
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SplashManager.closeSplash();
		BowlerStudio.runLater(() -> {
			try {
				// Resolve the ResourceBundle the same way FXMLLoader does
				ResourceBundle bundle = ActiveProject.getLangaugePack();

				FXMLLoader loader = new FXMLLoader(
						StlRepairDialogController.class.getResource("/com/commonwealthrobotics/StlRepairDialog.fxml"),
						bundle);

				VBox dialogRoot = loader.load();

				Stage stage = new Stage();
				if (owner != null) {
					stage.initOwner(owner);
				}
				stage.initModality(Modality.APPLICATION_MODAL);
				stage.setTitle(bundle.getString("stlrepair.title"));
				stage.setResizable(false);

				// Apply application stylesheet if available
				// ActiveProject.setStyleSheet(dialogRoot);  // uncomment to re-enable

				Scene scene = new Scene(dialogRoot);
				stage.setScene(scene);

				// Wire up dynamic labels, checkbox state, and latch callbacks
				StlRepairDialogController controller = loader.getController();
				controller.configure(file, throwable, result, latch, stage, bundle);

				stage.show();

			} catch (IOException e) {
				// If the FXML cannot be loaded, treat it as "No" and unblock caller
				e.printStackTrace();
				latch.countDown();
			}
		});

		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		SplashManager.setClosePreventer(cp);
		return result.get();
	}

	// ── Convenience overload without an owner window ──────────────────────────

	/**
	 * Convenience overload — shows the dialog without an explicit owner window.
	 *
	 * @see #show(Window, File, Throwable)
	 */
	public static boolean show(File file, Throwable throwable) {
		return show(null, file, throwable);
	}
}
