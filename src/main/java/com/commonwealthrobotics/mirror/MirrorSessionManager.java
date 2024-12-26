package com.commonwealthrobotics.mirror;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.controls.ControlSprites;
import com.commonwealthrobotics.rotate.RotationHandle;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.MirrorOrentation;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.EulerAxis;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;

public class MirrorSessionManager {
	private MirrorHandle x;
	private MirrorHandle y;
	private MirrorHandle z;
	private Affine selection;
	private ControlSprites controlSprites;
	private List<MirrorHandle> handles;
	private Bounds b;
	private BowlerStudio3dEngine engine;
	private List<CSG> ta;
	private List<String> selected;
	private HashMap<CSG, MeshView> meshes;
	private boolean active = false;

	public MirrorSessionManager(Affine selection, ActiveProject ap, ControlSprites controlSprites,
			Affine workplaneOffset) {
		this.selection = selection;
		this.controlSprites = controlSprites;
		x = new MirrorHandle(MirrorOrentation.x, workplaneOffset, selection, null, ap, controlSprites, workplaneOffset);
		y = new MirrorHandle(MirrorOrentation.y, workplaneOffset, selection, null, ap, controlSprites,
				workplaneOffset);
		z = new MirrorHandle(MirrorOrentation.z, workplaneOffset, selection, null, ap, controlSprites, workplaneOffset);
		handles = Arrays.asList(x, y, z);
		hide();
	}

	public List<Node> getElements() {
		ArrayList<Node> result = new ArrayList<Node>();
		for (MirrorHandle r : handles) {
			result.addAll(r.getElements());
		}
		return result;
	}

	public void hide() {
		for (MirrorHandle r : handles) {
			r.hide();
		}
		
	}

	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z, List<String> selectedCSG, Bounds b, TransformNR cf) {
		this.x.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b, cf);
		this.y.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b, cf);
		this.z.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b, cf);
	}

	public void initialize(Bounds b, BowlerStudio3dEngine engine, List<CSG> ta, List<String> selected,
			HashMap<CSG, MeshView> meshes) {
		this.b = b;
		this.engine = engine;
		this.ta = ta;
		this.selected = selected;
		this.meshes = meshes;
		for (MirrorHandle r : handles) {
			r.initialize(b, engine, ta, selected, meshes);
		}
		setActive(true);
	}

	public void cancel() {
		hide();
		setActive(false);
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
		for (MirrorHandle r : handles) {
			r.setActive(active);
		}
	}

}
