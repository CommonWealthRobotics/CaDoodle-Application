package com.commonwealthrobotics.controls;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.RulerManager;
import com.commonwealthrobotics.align.AlignManager;
import com.commonwealthrobotics.mirror.MirrorSessionManager;
import com.commonwealthrobotics.rotate.RotationSessionManager;
import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.VirtualCameraMobileBase;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;

class RemovedSelectionBoundsTest {
	@BeforeAll
	static void startFx() throws Exception {
		CountDownLatch started = new CountDownLatch(1);
		com.sun.javafx.application.PlatformImpl.startup(started::countDown);
		assertTrue(started.await(15, TimeUnit.SECONDS));
	}

	@Test
	void delayedBoundsUpdateCannotReshowControlsAfterLimbRemoval() throws Exception {
		onFx(() -> {
			Fixture f = new Fixture();
			CSG part = new Cube(10).toCSG().setName("removed-limb");
			f.selected.add(part);
			List<String> queuedSelection = List.of(part.getName());
			f.selected.clear();
			f.controls.clearSelection();
			assertFalse(f.footprint.isVisible());
			f.controls.updateControls(800, 600, -400, 0, 0, 0, 0, 0, queuedSelection, part.getBounds(), new HashMap<>(),
					60);
			for (Node node : f.root.getChildren())
				assertFalse(node.isVisible(), "late bounds work must not revive a removed selection");
		});
	}

	@Test
	void emptySelectionLineRefreshHidesExistingBounds() throws Exception {
		onFx(() -> {
			Fixture f = new Fixture();
			f.controls.updateLines();
			for (Node node : f.root.getChildren())
				assertFalse(node.isVisible(), "no selection must clear previously displayed bounds");
		});
	}

	@Test
	void boundsForRemovedLimbCannotOverwriteANewSelection() throws Exception {
		onFx(() -> {
			Fixture f = new Fixture();
			CSG replacement = new Cube(10).toCSG().setName("another-part");
			f.selected.add(replacement);
			for (Node node : f.root.getChildren())
				node.setVisible(false);
			f.controls.updateControls(800, 600, -400, 0, 0, 0, 0, 0, List.of("removed-limb"), replacement.getBounds(),
					new HashMap<>(), 60);
			for (Node node : f.root.getChildren())
				assertFalse(node.isVisible(), "stale bounds belong to a different selection");
		});
	}

	@Test
	void removedSelectionCannotKeepAlignBoundsVisible() throws Exception {
		onFx(() -> {
			Fixture f = new Fixture();
			f.state.setMode(SpriteDisplayMode.Align);
			f.controls.updateLines();
			for (Node node : f.root.getChildren())
				assertFalse(node.isVisible());
			assertTrue(f.controls.getMode() != SpriteDisplayMode.Align);
		});
	}

	@Test
	void currentSelectionStillDisplaysItsFootprint() throws Exception {
		onFx(() -> {
			Fixture f = new Fixture();
			CSG part = new Cube(10).toCSG().setName("current-part");
			f.selected.add(part);
			f.controls.clearSelection();
			doNothing().when(f.controls).updateLines();
			f.controls.updateControls(800, 600, -400, 0, 0, 0, 0, 0, List.of(part.getName()), part.getBounds(),
					new HashMap<>(), 60);
			assertTrue(f.footprint.isVisible());
		});
	}

	// Exercise real ControlSprites visibility methods with scene nodes; unrelated managers are inert
	private static class Fixture {
		final LinkedHashSet<CSG> selected = new LinkedHashSet<>();
		final ControlSpriteState state = new ControlSpriteState();
		final ControlSprites controls = mock(ControlSprites.class, CALLS_REAL_METHODS);
		final Rectangle footprint = new Rectangle(10, 10);
		final DottedLine line = new DottedLine(0.5, 1, new Affine());
		final Group root = new Group(footprint, line);

		Fixture() throws Exception {
			SelectionSession session = mock(SelectionSession.class);
			when(session.getSelected()).thenReturn(selected);
			when(session.getCurrentStateSelected()).thenReturn(List.of());
			when(session.selectedSnapshot()).thenAnswer(invocation -> selected.stream().map(CSG::getName).toList());
			when(session.isLocked()).thenReturn(true);
			VirtualCameraMobileBase camera = mock(VirtualCameraMobileBase.class);
			when(camera.getCamerFrame()).thenReturn(new TransformNR());
			when(camera.getZoomScale()).thenReturn(1.0);
			BowlerStudio3dEngine engine = mock(BowlerStudio3dEngine.class);
			when(engine.getFlyingCamera()).thenReturn(camera);
			set("session", session);
			set("engine", engine);
			set("ap", mock(ActiveProject.class));
			set("ruler", mock(RulerManager.class));
			set("displayState", state);
			set("footprint", footprint);
			set("allElems", new ArrayList<>(root.getChildren()));
			set("lines", List.of(line));
			set("numbers", List.of());
			set("scaleSession", mock(ResizeSessionManager.class));
			set("rotationManager", mock(RotationSessionManager.class));
			set("align", mock(AlignManager.class));
			set("mirror", mock(MirrorSessionManager.class));
			set("upArrow", mock(MoveUpArrow.class));
			set("zMoveManipulator", mock(Manipulation.class));
		}

		private void set(String name, Object value) throws Exception {
			Field field = ControlSprites.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(controls, value);
		}
	}

	private static void onFx(CheckedRunnable action) throws Exception {
		FutureTask<Void> work = new FutureTask<>(() -> {
			action.run();
			return null;
		});
		Platform.runLater(work);
		work.get(15, TimeUnit.SECONDS);
	}

	private interface CheckedRunnable {
		void run() throws Exception;
	}
}
