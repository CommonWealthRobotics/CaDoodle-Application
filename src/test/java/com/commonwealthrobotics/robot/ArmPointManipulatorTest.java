package com.commonwealthrobotics.robot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.WorkplaneManager;
import com.commonwealthrobotics.controls.ControlSprites;
import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation.DragState;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.VirtualCameraMobileBase;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import javafx.scene.layout.Pane;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;

class ArmPointManipulatorTest {
	@BeforeAll
	static void startFx() throws Exception {
		CountDownLatch started = new CountDownLatch(1);
		com.sun.javafx.application.PlatformImpl.startup(started::countDown);
		assertTrue(started.await(15, TimeUnit.SECONDS));
	}

	@Test
	void repeatedTargetsReuseBothMeshesAndRespectVisibility() throws Exception {
		onFx(() -> {
			Fixture f = new Fixture();
			List<MeshView> meshes = f.arm.getMesh();
			assertEquals(2, meshes.size());
			for (int i = 0; i < 4; i++) {
				boolean locked = i % 2 == 0;
				f.arm.threeDTarget(800, 600, -400, new TransformNR(i, -i, i * 10), new TransformNR(), locked);
				for (int index = 0; index < meshes.size(); index++) {
					assertSame(meshes.get(index), f.arm.getMesh().get(index));
					assertEquals(!locked, meshes.get(index).isVisible());
				}
				f.arm.hide();
				for (MeshView mesh : meshes)
					assertFalse(mesh.isVisible());
				f.arm.show();
				for (MeshView mesh : meshes)
					assertTrue(mesh.isVisible());
			}
		});
	}

	@Test
	void zProjectionKeepsStartingXYAndSnapsOnlyZ() throws Exception {
		onFx(() -> {
			Fixture f = new Fixture();
			f.overlay.setTranslateX(40);
			f.overlay.setTranslateY(-20);
			set(f.up.arrow, "startingPosition3D", new Point3D(3.25, -7.75, 10));
			f.up.manipulator.setIncrement(2.5);
			f.up.manipulator.setSnapGridStatus(true);
			Point2D local = new Point2D(60, 70);
			for (double[] sample : new double[][]{{6.4, 7.5}, {-6.4, -7.5}, {0, 0}}) {
				when(f.engine.sceneToWorldFixedXY_WP(local, 3.25, -7.75)).thenReturn(sample[0]);
				Point3D projected = f.up.projectMouse(100, 50, 2.5);
				assertEquals(new Point3D(3.25, -7.75, sample[1]), projected);
			}
			verify(f.engine, times(3)).sceneToWorldFixedXY_WP(local, 3.25, -7.75);
		});
	}

	@Test
	void poseComesFromSelectedAxisAndResetClearsBothOffsets() throws Exception {
		onFx(() -> {
			Fixture f = new Fixture();
			f.arm.tip.manipulator.set(4, 8, 0);
			f.up.manipulator.set(0, 0, 12);
			assertPose(f.arm.getCurrentPoseInReferenceFrame(), 4, 8, 0);
			set(f.up.arrow, "selected", true);
			assertPose(f.arm.getCurrentPoseInReferenceFrame(), 0, 0, 12);
			set(f.arm.tip, "selected", true);
			f.arm.onReset();
			assertFalse(f.up.arrow.isSelected());
			assertFalse(f.arm.tip.isSelected());
			assertPose(f.up.manipulator.getCurrentPoseInReferenceFrame(), 0, 0, 0);
			assertPose(f.arm.getCurrentPoseInReferenceFrame(), 0, 0, 0);
			f.arm.onReset();
			assertPose(f.arm.getCurrentPoseInReferenceFrame(), 0, 0, 0);
		});
	}

	@Test
	void draggingEitherHandleMovesThePairByTheSameOffset() throws Exception {
		onFx(() -> {
			Fixture f = new Fixture();
			f.arm.threeDTarget(800, 600, -400, new TransformNR(20, -30, 40), new TransformNR(), false);
			MeshView xy = f.arm.tip.getMesh();
			MeshView z = f.up.arrow.getMesh();
			Point3D xyStart = xy.localToParent(Point3D.ZERO);
			Point3D zStart = z.localToParent(Point3D.ZERO);
			for (Point3D offset : new Point3D[]{new Point3D(12, -8, 0), new Point3D(-4, 9, 0)}) {
				f.arm.tip.manipulator.set(offset.getX(), offset.getY(), offset.getZ());
				assertEquals(offset, xy.localToParent(Point3D.ZERO).subtract(xyStart));
				assertEquals(offset, z.localToParent(Point3D.ZERO).subtract(zStart));
			}
			for (double height : new double[]{15, -7, 0}) {
				f.up.manipulator.set(0, 0, height);
				Point3D offset = new Point3D(0, 0, height);
				assertEquals(offset, xy.localToParent(Point3D.ZERO).subtract(xyStart));
				assertEquals(offset, z.localToParent(Point3D.ZERO).subtract(zStart));
			}
			f.arm.onReset();
			assertEquals(xyStart, xy.localToParent(Point3D.ZERO));
			assertEquals(zStart, z.localToParent(Point3D.ZERO));
		});
	}

	@Test
	void draggingEitherAxisIsReportedRegardlessOfSelection() throws Exception {
		onFx(() -> {
			Fixture f = new Fixture();
			for (DragState xy : DragState.values()) {
				for (DragState z : DragState.values()) {
					f.arm.tip.manipulator.setState(xy);
					f.up.manipulator.setState(z);
					assertEquals(z != DragState.IDLE ? z : xy, f.arm.getState());
				}
			}
		});
	}

	private static class Fixture {
		final Pane overlay = new Pane();
		final BowlerStudio3dEngine engine = mock(BowlerStudio3dEngine.class);
		final ArmPointManipulator arm;
		final RobotZHandle up;

		Fixture() throws Exception {
			ActiveProject project = mock(ActiveProject.class);
			CaDoodleFile file = mock(CaDoodleFile.class);
			when(project.get()).thenReturn(file);
			when(file.getWorkplane()).thenReturn(new TransformNR());
			VirtualCameraMobileBase camera = mock(VirtualCameraMobileBase.class);
			when(camera.getCamera()).thenReturn(new PerspectiveCamera());
			when(engine.getFlyingCamera()).thenReturn(camera);
			when(engine.getOverlayPane()).thenReturn(overlay);
			when(engine.screenToSceneMMscale(any(Point3D.class))).thenReturn(1.0);
			arm = new ArmPointManipulator(() -> {
			}, event -> {
			}, project, engine, new Affine(), () -> {
			}, () -> {
			}, mock(WorkplaneManager.class), mock(ControlSprites.class));
			Field field = ArmPointManipulator.class.getDeclaredField("up");
			field.setAccessible(true);
			up = (RobotZHandle) field.get(arm);
		}
	}

	// Seed selection and drag origin without synthesizing unrelated mouse gesture machinery;
	private static void set(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static void assertPose(TransformNR pose, double x, double y, double z) {
		assertEquals(x, pose.getX(), 1e-9);
		assertEquals(y, pose.getY(), 1e-9);
		assertEquals(z, pose.getZ(), 1e-9);
	}

	private static void onFx(CheckedRunnable action) throws Exception {
		FutureTask<Void> work = new FutureTask<Void>(() -> {
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
