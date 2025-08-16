package com.commonwealthrobotics.rotate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.RulerManager;
import com.commonwealthrobotics.controls.ControlSprites;
import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.sdk.addons.kinematics.math.EulerAxis;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import javafx.scene.Node;
import javafx.scene.transform.Affine;

public class RotationSessionManager {
	private List<RotationHandle> handles;
	private Affine selection;
	private Affine viewRotation = new Affine();
	private SelectionSession controlSprites;
	private boolean moveLock;

	public RotationSessionManager(Affine selection, ActiveProject ap, SelectionSession controlSprites,
			Affine workplaneOffset,RulerManager ruler,IOnRotateDone done) {
		this.selection = selection;
		this.controlSprites = controlSprites;
		RotationHandle az;
		RotationHandle el;
		RotationHandle tlt;
		az = new RotationHandle(EulerAxis.azimuth, selection, getViewRotation(), this, ap, controlSprites,
				workplaneOffset,ruler,done);
		el = new RotationHandle(EulerAxis.elevation, selection, getViewRotation(), this, ap, controlSprites,
				workplaneOffset,ruler,done);
		tlt = new RotationHandle(EulerAxis.tilt, selection, getViewRotation(), this, ap, controlSprites,
				workplaneOffset,ruler,done);
		handles = Arrays.asList(az, el, tlt);
	}

	public List<Node> getElements() {
		ArrayList<Node> result = new ArrayList<Node>();
		for (RotationHandle r : handles) {
			result.add(r.handle);
			result.add(r.controlCircle);
			result.add(r.arc);
			result.add(r.text.get());
		}
		return result;
	}

	public void initialize(boolean b) {
		for (RotationHandle r : handles) {
			r.handle.setVisible(!b);
			r.controlCircle.setVisible(false);
			r.arc.setVisible(false);
			r.text.hide();

		}

	}

	public void hide() {
		for (RotationHandle r : handles) {
			r.handle.setVisible(false);
			r.controlCircle.setVisible(false);
			r.arc.setVisible(false);
			r.text.hide();
		}
	}

	public void show(boolean lock) {
		initialize(lock);
		// new RuntimeException().printStackTrace();
	}

	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z, List<String> selectedCSG, Bounds b, TransformNR cf) {
		for (RotationHandle r : handles)
			r.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b, cf);
	}

	public Affine getViewRotation() {
		return viewRotation;
	}

	public void setViewRotation(Affine viewRotation) {
		this.viewRotation = viewRotation;
	}

	public void resetSelected() {
		for (RotationHandle r : handles) {
			r.setSelected(false);
		}
	}

	public boolean isFocused() {
		for (RotationHandle r : handles) {
			if (r.isFocused())
				return true;
		}
		return false;
	}

	public void setLock(boolean moveLock) {
		this.moveLock = moveLock;
		for (RotationHandle r : handles) {
			r.setLock(moveLock);
		}
	}
	public void setMoving(IOnRotateMoving moving) {
		for (RotationHandle r : handles) {
			r.setMoving(moving);
		}
	}
}
