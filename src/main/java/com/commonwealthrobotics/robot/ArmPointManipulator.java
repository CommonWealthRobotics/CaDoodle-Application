package com.commonwealthrobotics.robot;

import java.util.ArrayList;
import java.util.Arrays;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.WorkplaneManager;
import com.commonwealthrobotics.controls.ControlSprites;
import com.commonwealthrobotics.controls.ResizingHandle;
import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation.DragState;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Cylinder;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;

public class ArmPointManipulator {

	final ResizingHandle tip;
	private final Affine baseSelection = new Affine();
	private final RobotZHandle up;

	public ArmPointManipulator(Runnable saveListener, EventHandler<MouseEvent> moveListener, ActiveProject ap,
			BowlerStudio3dEngine engine, Affine workplaneOffset, Runnable onSelect, Runnable onReset,
			WorkplaneManager workplane, ControlSprites controlSprites) {
		tip = new ResizingHandle("Limb Base", workplane, engine, baseSelection, new Vector3d(1, 1, 0), workplaneOffset,
				onSelect, onReset, new Cylinder(15, 1).toCSG(), controlSprites);
		tip.setMyColor(Color.PINK, Color.TEAL);
		tip.setBaseScale(1.25);
		tip.manipulator.setFrameOfReference(() -> ap.get().getWorkplane());
		tip.manipulator.addEventListener(moveListener);
		tip.manipulator.addSaveListener(saveListener);
		up = new RobotZHandle(saveListener, moveListener, ap, engine, workplaneOffset, onSelect, onReset);
	}

	public TransformNR getCurrentPoseInReferenceFrame() {
		return up.arrow.isSelected()
				? up.manipulator.getCurrentPoseInReferenceFrame()
				: tip.manipulator.getCurrentPoseInReferenceFrame();
	}

	public void onReset() {
		tip.resetSelected();
		tip.manipulator.reset();
		up.reset();
	}

	public ArrayList<MeshView> getMesh() {
		return new ArrayList<MeshView>(Arrays.asList(tip.getMesh(), up.arrow.getMesh()));
	}

	public void show() {
		tip.show();
		up.arrow.show();
	}

	public void hide() {
		tip.hide();
		up.arrow.hide();
	}

	public DragState getState() {
		return up.manipulator.getState() != DragState.IDLE ? up.manipulator.getState() : tip.manipulator.getState();
	}

	public void threeDTarget(double screenW, double screenH, double zoom, TransformNR target, TransformNR cf,
			boolean locked) {
		tip.threeDTarget(screenW, screenH, zoom, target, cf, locked);
		up.update(target.copy(), locked);
	}
}
