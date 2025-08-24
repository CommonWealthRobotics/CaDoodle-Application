package com.commonwealthrobotics.robot;

import java.util.ArrayList;
import java.util.Arrays;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.controls.MoveUpArrow;
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

	private Runnable saveListener;
	ResizingHandle tip = null;
	private Affine baseSelection = new Affine();
	private Affine moveUpLocation = new Affine();

	private MoveUpArrow up;
	public ArmPointManipulator(Runnable saveListener,EventHandler<MouseEvent> moveListener, ActiveProject ap,BowlerStudio3dEngine engine,
			Affine workplaneOffset, Runnable onSelect, Runnable onReset) {
		this.saveListener = saveListener;
		tip = new ResizingHandle("Limb Base", engine, baseSelection, new Vector3d(1, 1, 0), workplaneOffset,onSelect, onReset, new Cylinder(5, 1).toCSG());
		tip.setMyColor(Color.PINK, Color.TEAL);
		tip.setBaseSize(1.25);
		tip.manipulator.setFrameOfReference(() -> ap.get().getWorkplane());
		tip.manipulator.addEventListener(moveListener);
		tip.manipulator.addSaveListener(saveListener);
//		up = new MoveUpArrow(selection, workplaneOffset, moveUpLocation, scaleTF, zMove.getMouseEvents(), () -> {
//			updateCubes();
//			updateLines();
//		}, () -> scaleSession.resetSelected());
	}
	
	public TransformNR getCurrentPoseInReferenceFrame(){
		return tip.manipulator.getCurrentPoseInReferenceFrame();
	}
	public void onReset() {
		tip.resetSelected();
		tip.manipulator.reset();
	}
	public ArrayList<MeshView> getMesh() {
		
		return new ArrayList<MeshView>(Arrays.asList(tip.getMesh()));
	}

	public void show() {
		tip.show();
	}

	public void hide() {
		tip.hide();
	}

	public  DragState getState() {
		return tip.manipulator.getState();
	}
	public void threeDTarget(double screenW, double screenH, double zoom, TransformNR target, TransformNR cf, boolean locked) {
		tip.threeDTarget(screenW, screenH, zoom, target, cf, locked);
	}
}
