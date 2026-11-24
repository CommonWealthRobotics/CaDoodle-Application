package com.commonwealthrobotics.rotate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.RulerManager;
import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.sdk.addons.kinematics.math.EulerAxis;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Cube;
import javafx.application.Platform;
import javafx.event.EventType;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.transform.Affine;

class RotationHandleTest {
	@BeforeAll
	static void startFx() throws Exception {
		com.sun.javafx.application.PlatformImpl.startup(() -> {
		});
		onFx(() -> {
		});
	}

	private static void onFx(Runnable action) throws Exception {
		FutureTask<Void> task = new FutureTask<>(action, null);
		Platform.runLater(task);
		task.get(30, TimeUnit.SECONDS);
	}

	private static RotationHandle createHandle(boolean selectionLocked) {
		SelectionSession session = mock(SelectionSession.class);
		when(session.moveLock()).thenReturn(selectionLocked);
		return new RotationHandle(EulerAxis.azimuth, new Affine(), new Affine(), mock(RotationSessionManager.class),
				mock(ActiveProject.class), session, new Affine(), mock(RulerManager.class),
				tf -> fail("A click must not save a rotation"));
	}

	private static void fire(Node node, EventType<MouseEvent> type) {
		node.fireEvent(new MouseEvent(type, 0, 0, 0, 0, MouseButton.PRIMARY, 1, false, false, false, false,
				type == MouseEvent.MOUSE_PRESSED, false, false, false, false, true, null));
	}

	@Test
	void ordinaryObjectClickKeepsEditableAngleWithoutSaving() throws Exception {
		onFx(() -> {
			RotationHandle handle = createHandle(false);
			handle.setLock(false);
			fire(handle.getImageSet(), MouseEvent.MOUSE_PRESSED);
			fire(handle.getImageSet(), MouseEvent.MOUSE_RELEASED);
			fire(handle.handle, MouseEvent.MOUSE_EXITED);
			assertTrue(handle.isSelected());
			assertTrue(handle.controlCircle.isVisible());
			Node input = handle.TDnumber.getTextField().get(0);
			assertTrue(input.isVisible());
			assertFalse(input.isMouseTransparent());
		});
	}

	@ParameterizedTest
	@EnumSource(value = EulerAxis.class, names = {"azimuth", "elevation", "tilt"})
	void ordinaryObjectDragSavesOnceAndEndsRotation(EulerAxis axis) throws Exception {
		onFx(() -> {
			ActiveProject project = mock(ActiveProject.class);
			CaDoodleFile file = mock(CaDoodleFile.class);
			when(project.get()).thenReturn(file);
			when(file.getWorkplane()).thenReturn(new TransformNR());
			AtomicInteger saves = new AtomicInteger();
			RotationHandle handle = new RotationHandle(axis, new Affine(), new Affine(),
					mock(RotationSessionManager.class), project, mock(SelectionSession.class), new Affine(),
					mock(RulerManager.class), tf -> saves.incrementAndGet());
			handle.updateControls(800, 600, -500, 0, 0, 0, 0, 0, Collections.emptyList(),
					new Cube(20).toCSG().getBounds(), new TransformNR(0, 0, -500), 45, 1);
			fire(handle.getImageSet(), MouseEvent.MOUSE_PRESSED);
			double diameter = handle.controlCircle.getFitWidth();
			drag(handle, diameter * 0.95, diameter * 0.5);
			drag(handle, diameter * 0.5, diameter * 0.95);
			assertEquals(90, Math.abs(handle.TDnumber.getMostRecentValue()), 1e-6);
			assertTrue(handle.arc.isVisible());
			assertEquals(0, saves.get());
			fire(handle.getImageSet(), MouseEvent.MOUSE_RELEASED);
			assertEquals(1, saves.get());
			assertFalse(handle.isSelected());
			assertFalse(handle.controlCircle.isVisible());
			assertFalse(handle.arc.isVisible());
		});
	}

	private static void drag(RotationHandle handle, double x, double y) {
		PickResult pick = new PickResult(handle.controlCircle, new Point3D(x, y, 0), 1);
		handle.getImageSet().fireEvent(new MouseEvent(MouseEvent.MOUSE_DRAGGED, x, y, x, y, MouseButton.PRIMARY, 1,
				false, false, false, false, true, false, false, false, false, false, pick));
	}

	@Test
	void limbClickShowsAngleDespiteObjectMotionLock() throws Exception {
		onFx(() -> {
			RotationHandle handle = createHandle(true);
			handle.setLock(false);
			fire(handle.getImageSet(), MouseEvent.MOUSE_PRESSED);
			fire(handle.getImageSet(), MouseEvent.MOUSE_RELEASED);
			assertTrue(handle.isSelected());
			assertTrue(handle.controlCircle.isVisible());
			assertTrue(handle.TDnumber.getTextField().get(0).isVisible());
		});
	}

	@Test
	void refreshDoesNotDismissClickedCircleOnPointerExit() throws Exception {
		onFx(() -> {
			RotationHandle handle = createHandle(false);
			fire(handle.getImageSet(), MouseEvent.MOUSE_PRESSED);
			fire(handle.getImageSet(), MouseEvent.MOUSE_RELEASED);
			handle.updateControls(800, 600, -500, 0, 0, 0, 0, 0, Collections.emptyList(),
					new Cube(20).toCSG().getBounds(), new TransformNR(0, 0, -500), 45, 1);
			fire(handle.handle, MouseEvent.MOUSE_EXITED);
			assertTrue(handle.controlCircle.isVisible());
			assertTrue(handle.TDnumber.getTextField().get(0).isVisible());
		});
	}

	@Test
	void deselectionRestoresHoverOnlyCircle() throws Exception {
		onFx(() -> {
			RotationHandle handle = createHandle(false);
			fire(handle.getImageSet(), MouseEvent.MOUSE_PRESSED);
			handle.setSelected(false);
			fire(handle.handle, MouseEvent.MOUSE_EXITED);
			assertFalse(handle.controlCircle.isVisible());
		});
	}

	@Test
	void lockedHandleCannotStartRotation() throws Exception {
		onFx(() -> {
			RotationHandle handle = createHandle(false);
			handle.setLock(true);
			fire(handle.getImageSet(), MouseEvent.MOUSE_PRESSED);
			assertFalse(handle.isSelected());
			assertFalse(handle.controlCircle.isVisible());
			assertFalse(handle.TDnumber.getTextField().get(0).isVisible());
		});
	}
}
