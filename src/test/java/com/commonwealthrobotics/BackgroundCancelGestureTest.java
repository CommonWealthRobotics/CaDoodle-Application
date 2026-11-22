package com.commonwealthrobotics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Background-click cancel. A press that reaches the selection plane must clear
 * the selection; modifier gestures must be left to the camera controls.
 *
 * <p>
 * Regression note: the decision used to read
 * {@code event.getPickResult().getIntersectedNode()}, which is null while the
 * event is still on the capture phase, so the comparison never matched and
 * cancel stopped firing once the camera controls were installed on the host
 * container.
 */
public class BackgroundCancelGestureTest {

	private static MouseEvent press(boolean primary, boolean control, boolean shift) {
		return new MouseEvent(MouseEvent.MOUSE_PRESSED, 10, 10, 10, 10, MouseButton.PRIMARY, 1, shift, control, false,
				false, primary, false, false, true, false, false, null);
	}

	@Test
	public void plainPrimaryPressOnThePlaneCancels() {
		assertTrue(MainController.isBackgroundCancelGesture(press(true, false, false)));
	}

	@Test
	public void modifierGesturesAreLeftToTheCamera() {
		// ctrl+primary and shift+primary drive the camera, so they must not cancel
		assertFalse(MainController.isBackgroundCancelGesture(press(true, true, false)));
		assertFalse(MainController.isBackgroundCancelGesture(press(true, false, true)));
	}

	@Test
	public void aReleaseOrSecondaryPressDoesNotCancel() {
		assertFalse(MainController.isBackgroundCancelGesture(press(false, false, false)));
	}
}
