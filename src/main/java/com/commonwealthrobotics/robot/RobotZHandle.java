package com.commonwealthrobotics.robot;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.controls.MoveUpArrow;
import com.commonwealthrobotics.controls.ResizingHandle;
import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.SceneStyleConfig;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;

final class RobotZHandle {
	final Manipulation manipulator;
	final MoveUpArrow arrow;
	private final Affine location = new Affine();
	private final Scale scale = new Scale();
	private final BowlerStudio3dEngine engine;
	private final Affine workplaneOffset;

	RobotZHandle(Runnable saveListener, EventHandler<MouseEvent> moveListener, ActiveProject ap,
			BowlerStudio3dEngine engine, Affine workplaneOffset, Runnable onSelect, Runnable onReset) {
		this.engine = engine;
		this.workplaneOffset = workplaneOffset;
		Affine selection = new Affine();
		manipulator = new Manipulation(selection, new Vector3d(0, 0, 1), new TransformNR(), this::projectMouse, true,
				false);
		manipulator.setFrameOfReference(() -> ap.get().getWorkplane());
		manipulator.addEventListener(moveListener);
		manipulator.addSaveListener(saveListener);
		arrow = new MoveUpArrow(selection, workplaneOffset, location, scale, manipulator, onSelect, onReset);
	}

	Point3D projectMouse(double screenX, double screenY, double increment) {
		Point2D local = engine.getOverlayPane().sceneToLocal(new Point2D(screenX, screenY));
		Point3D start = arrow.getStartingPoint3D();
		double z = engine.sceneToWorldFixedXY_WP(local, start.getX(), start.getY());
		return new Point3D(start.getX(), start.getY(), manipulator.snapToGrid(z));
	}

	void update(TransformNR target, boolean locked) {
		BowlerStudio.runLater(() -> {
			Point3D world = workplaneOffset.transform(target.getX(), target.getY(), target.getZ());
			double size = engine.screenToSceneMMscale(world) * SceneStyleConfig.getDouble("handle.scale", 0.75);
			scale.setX(size);
			scale.setY(size);
			scale.setZ(size);
			location.setTx(target.getX());
			location.setTy(target.getY());
			location.setTz(target.getZ() + 5 + ResizingHandle.getSize() * size);
			arrow.getMesh().setVisible(!locked);
		});
	}

	void reset() {
		arrow.resetSelected();
		manipulator.reset();
	}
}
