package com.commonwealthrobotics.controls;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.commonwealthrobotics.ActiveProject;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.VirtualCameraMobileBase;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.Cube;
import javafx.application.Platform;
import javafx.scene.PerspectiveCamera;
import javafx.scene.transform.Affine;

class LimbBoundsRefreshTest {
	@BeforeAll
	static void startFx() throws Exception {
		CountDownLatch started = new CountDownLatch(1);
		com.sun.javafx.application.PlatformImpl.startup(started::countDown);
		assertTrue(started.await(15, TimeUnit.SECONDS));
	}

	@Test
	void queuedPreviewsAndDragCameraChangesSkipBoundsThenReleaseComputesOnce() throws Exception {
		FutureTask<Void> work = new FutureTask<>(() -> {
			SelectionSession session = mock(SelectionSession.class, CALLS_REAL_METHODS);
			ControlSprites controls = mock(ControlSprites.class);
			ActiveProject project = mock(ActiveProject.class);
			CaDoodleFile file = new CaDoodleFile();
			when(project.isOpen()).thenReturn(true);
			when(project.get()).thenReturn(file);
			BowlerStudio3dEngine engine = mock(BowlerStudio3dEngine.class);
			VirtualCameraMobileBase camera = mock(VirtualCameraMobileBase.class);
			when(engine.getFlyingCamera()).thenReturn(camera);
			when(camera.getCamera()).thenReturn(new PerspectiveCamera());
			when(camera.getZoomScale()).thenReturn(1.0);
			set(session, "limbBounds", new LimbBoundsLifecycle());
			set(session, "ap", project);
			set(session, "engine", engine);
			doReturn(controls).when(session).getControls();
			CSG part = new Cube(10).toCSG().setName("link");
			Affine pose = new Affine();
			part.setManipulator(pose);
			assertEquals(0, file.getBounds(Arrays.asList(part)).getCenterX(), 1e-9);
			doReturn(new LinkedHashSet<>(Arrays.asList(part))).when(session).getSelected();
			doReturn(Arrays.asList(part.getName())).when(session).selectedSnapshot();
			doReturn(new TransformNR()).when(session).getWorkplane();
			List<Runnable> queued = new ArrayList<>();
			doAnswer(invocation -> {
				queued.add((Runnable) invocation.getArguments()[0]);
				return null;
			}).when(session).submit(any(Runnable.class));
			session.onCameraChange(800, 600, -400, 0, 0, 0, 0, 0, 60);
			assertEquals(1, queued.size());
			session.beginLimbMove();
			pose.setTx(40);
			queued.remove(0).run();
			for (int i = 0; i < 50; i++) {
				session.beginLimbMove();
				session.onCameraChange(800, 600, -400, 0, 0, 0, 0, 0, 60);
			}
			assertTrue(queued.isEmpty());
			verify(session, never()).getSellectedBounds();
			session.finishLimbMove();
			assertTrue(session.isLimbMoveActive());
			assertEquals(1, queued.size());
			queued.remove(0).run();
			verify(session, times(1)).getSellectedBounds();
			ArgumentCaptor<Bounds> displayed = ArgumentCaptor.forClass(Bounds.class);
			verify(controls).updateControls(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
					anyDouble(), anyDouble(), anyDouble(), anyList(), displayed.capture(), any(), anyDouble());
			assertEquals(40, displayed.getValue().getCenterX(), 1e-9);
			verify(controls).setMode(SpriteDisplayMode.Default);
			assertFalse(session.isLimbMoveActive());
			session.finishLimbMove();
			assertTrue(queued.isEmpty());
			return null;
		});
		Platform.runLater(work);
		work.get(15, TimeUnit.SECONDS);
	}

	private static void set(SelectionSession session, String name, Object value) throws Exception {
		Field field = SelectionSession.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(session, value);
	}
}
