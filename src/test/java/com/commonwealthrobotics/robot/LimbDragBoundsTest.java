package com.commonwealthrobotics.robot;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.RulerManager;
import com.commonwealthrobotics.WorkplaneManager;
import com.commonwealthrobotics.controls.ControlSprites;
import com.commonwealthrobotics.controls.SelectionSession;
import com.commonwealthrobotics.rotate.RotationSessionManager;
import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;
import com.neuronrobotics.bowlerstudio.creature.MobileBaseBuilder;
import com.neuronrobotics.bowlerstudio.creature.MobileBaseCadManager;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.robot.ModifyLimb;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.VirtualCameraMobileBase;
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import javafx.application.Platform;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

class LimbDragBoundsTest {
	@BeforeAll
	static void startFx() {
		com.sun.javafx.application.PlatformImpl.startup(() -> {
		});
	}

	@ParameterizedTest
	@CsvSource({"base,xy", "base,z", "tip,xy", "tip,z"})
	void movementSuspendsBoundsBeforeChangingKinematicsAndKeepsRendering(String point, String axis) throws Exception {
		onFx(() -> {
			Fixture f = new Fixture();
			boolean[] suspended = {false};
			doAnswer(invocation -> {
				suspended[0] = true;
				return null;
			}).when(f.session).beginLimbMove();
			doAnswer(invocation -> {
				assertTrue(suspended[0]);
				return null;
			}).when(f.limb).setRobotToFiducialTransform(any(TransformNR.class));
			doAnswer(invocation -> {
				assertTrue(suspended[0]);
				return new double[0];
			}).when(f.limb).setDesiredTaskSpaceTransform(any(TransformNR.class), anyDouble());
			Manipulation manipulator = f.manipulator(point, axis);
			Method move = Manipulation.class.getDeclaredMethod("fireMove", TransformNR.class, MouseEvent.class);
			move.setAccessible(true);
			for (int i = 0; i < 3; i++)
				move.invoke(manipulator, new TransformNR(), null);
			verify(f.session, times(3)).beginLimbMove();
			verify(f.cad, times(3)).render();
			if ("base".equals(point))
				verify(f.limb, times(3)).setRobotToFiducialTransform(any(TransformNR.class));
			else
				verify(f.limb, times(3)).setDesiredTaskSpaceTransform(any(TransformNR.class), anyDouble());
			verify(f.session, never()).getSellectedBounds();
			verify(f.session, never()).getBounds(any(DHParameterKinematics.class));
			verify(f.session, never()).finishLimbMove();
		});
	}

	@ParameterizedTest
	@CsvSource({"base,xy", "base,z", "tip,xy", "tip,z"})
	void savingRefreshesOnlyAfterTheOperationCompletes(String point, String axis) throws Exception {
		onFx(() -> {
			Fixture f = new Fixture();
			when(f.project.addOp(f.mod)).thenAnswer(invocation -> {
				verify(f.session, never()).finishLimbMove();
				verify(f.session, never()).getCurrentStateSelected();
				return new Thread();
			});
			f.manipulator(point, axis).fireSaveSynchronous();
			InOrder order = inOrder(f.project, f.session);
			order.verify(f.project).addOp(f.mod);
			order.verify(f.session).getCurrentStateSelected();
			order.verify(f.session).finishLimbMove();
		});
	}

	@Test
	void failedSaveStillReleasesBoundsSuspension() throws Exception {
		onFx(() -> {
			Fixture f = new Fixture();
			when(f.project.addOp(f.mod)).thenThrow(new IllegalStateException("save failed"));
			f.manipulator("base", "z").fireSaveSynchronous();
			verify(f.session).finishLimbMove();
		});
	}

	@Test
	void hidingAnActiveLimbFinishesTheDeferredRefresh() throws Exception {
		onFx(() -> {
			Fixture f = new Fixture();
			verify(f.session, never()).beginLimbMove();
			verify(f.session, never()).finishLimbMove();
			when(f.session.isLimbMoveActive()).thenReturn(true);
			f.manager.hide();
			verify(f.session).finishLimbMove();
		});
	}

	private static final class Fixture {
		final SelectionSession session = mock(SelectionSession.class);
		final ActiveProject project = mock(ActiveProject.class);
		final DHParameterKinematics limb = mock(DHParameterKinematics.class);
		final ModifyLimb mod = mock(ModifyLimb.class);
		final MobileBaseCadManager cad = mock(MobileBaseCadManager.class);
		final LimbControlManager manager;

		Fixture() throws Exception {
			BowlerStudio3dEngine engine = mock(BowlerStudio3dEngine.class);
			VirtualCameraMobileBase camera = mock(VirtualCameraMobileBase.class);
			when(camera.getCamera()).thenReturn(new PerspectiveCamera());
			when(camera.getCamerFrame()).thenReturn(new TransformNR());
			when(camera.getZoomScale()).thenReturn(1.0);
			when(engine.getFlyingCamera()).thenReturn(camera);
			when(engine.getOverlayPane()).thenReturn(new Pane());
			CaDoodleFile file = mock(CaDoodleFile.class);
			when(project.get()).thenReturn(file);
			when(file.getWorkplane()).thenReturn(new TransformNR());
			when(session.getCurrentStateSelected()).thenReturn(Collections.emptyList());
			when(limb.getRobotToFiducialTransform()).thenReturn(new TransformNR());
			when(limb.getCurrentTaskSpaceTransform()).thenReturn(new TransformNR());
			when(mod.getBase()).thenReturn(new TransformNR());
			when(mod.getTipTF()).thenReturn(new TransformNR());
			MobileBaseBuilder builder = mock(MobileBaseBuilder.class);
			when(builder.getCadManager()).thenReturn(cad);
			manager = new LimbControlManager(engine, session, project, mock(RulerManager.class),
					mock(WorkplaneManager.class), mock(ControlSprites.class));
			manager.rotationManager = mock(RotationSessionManager.class);
			set("limb", limb);
			set("mod", mod);
			set("builder", builder);
		}

		Manipulation manipulator(String point, String axis) throws Exception {
			ArmPointManipulator arm = "base".equals(point) ? manager.baseManipulator : manager.tipManipulator;
			if ("xy".equals(axis))
				return arm.tip.manipulator;
			Field field = ArmPointManipulator.class.getDeclaredField("up");
			field.setAccessible(true);
			return ((RobotZHandle) field.get(arm)).manipulator;
		}

		void set(String name, Object value) throws Exception {
			Field field = LimbControlManager.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(manager, value);
		}
	}

	private static void onFx(CheckedRunnable action) throws Exception {
		FutureTask<Void> work = new FutureTask<>(() -> {
			action.run();
			return null;
		});
		Platform.runLater(work);
		work.get(30, TimeUnit.SECONDS);
	}

	private interface CheckedRunnable {
		void run() throws Exception;
	}
}
