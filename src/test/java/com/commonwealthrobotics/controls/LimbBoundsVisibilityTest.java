package com.commonwealthrobotics.controls;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;

class LimbBoundsVisibilityTest {
	@BeforeAll
	static void startFx() throws Exception {
		CountDownLatch started = new CountDownLatch(1);
		com.sun.javafx.application.PlatformImpl.startup(started::countDown);
		assertTrue(started.await(15, TimeUnit.SECONDS));
	}

	@Test
	void movingAndSavingSuppressEveryBoundsDisplayEntryPointButNotRobotHandles() throws Exception {
		FutureTask<Void> work = new FutureTask<>(() -> {
			SelectionSession session = mock(SelectionSession.class);
			when(session.isLimbMoveActive()).thenReturn(true);
			ControlSprites controls = mock(ControlSprites.class, CALLS_REAL_METHODS);
			Rectangle box = new Rectangle(10, 10);
			Rectangle robotHandle = new Rectangle(5, 5);
			set(controls, "session", session);
			set(controls, "allElems", new ArrayList<Node>(Arrays.asList(box)));
			set(controls, "displayState", new ControlSpriteState());
			controls.hideForLimbMove();
			assertFalse(box.isVisible());
			box.setVisible(true);
			controls.updateControls(800, 600, -400, 0, 0, 0, 0, 0, Collections.emptyList(), null, new HashMap<>(), 60);
			assertFalse(box.isVisible());
			box.setVisible(true);
			controls.updateLines();
			assertFalse(box.isVisible());
			box.setVisible(true);
			controls.setMode(SpriteDisplayMode.Clear);
			controls.setMode(SpriteDisplayMode.Default);
			assertFalse(box.isVisible());
			box.setVisible(true);
			controls.clearSelection();
			assertFalse(box.isVisible());
			assertTrue(robotHandle.isVisible());
			return null;
		});
		Platform.runLater(work);
		work.get(15, TimeUnit.SECONDS);
	}

	private static void set(ControlSprites controls, String name, Object value) throws Exception {
		Field field = ControlSprites.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(controls, value);
	}
}
