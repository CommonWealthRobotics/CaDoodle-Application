package com.commonwealthrobotics.mirror;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.controls.ControlSprites;
import com.commonwealthrobotics.rotate.RotationHandle;
import com.neuronrobotics.sdk.addons.kinematics.math.EulerAxis;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import javafx.scene.Node;
import javafx.scene.transform.Affine;

public class MirrorSessionManager {
	private MirrorHandle x;
	private MirrorHandle y;
	private MirrorHandle z;
	private Affine selection;
	private ControlSprites controlSprites;
	private List<MirrorHandle> handles;
	public MirrorSessionManager(Affine selection, ActiveProject ap, ControlSprites controlSprites, Affine workplaneOffset) {
		this.selection = selection;
		this.controlSprites = controlSprites;
		x=new MirrorHandle(EulerAxis.tilt, workplaneOffset, selection, null, ap, controlSprites, workplaneOffset);
		y=new MirrorHandle(EulerAxis.elevation, workplaneOffset, selection, null, ap, controlSprites, workplaneOffset);
		z=new MirrorHandle(EulerAxis.azimuth, workplaneOffset, selection, null, ap, controlSprites, workplaneOffset);
		handles=Arrays.asList(x,y,z);
	}
	public List<Node> getElements(){
		ArrayList<Node> result = new ArrayList<Node>(); 
		for(MirrorHandle r: handles) {
			result.addAll(r.getElements());
		}
		return result;
	}
	public void initialize() {
		for(MirrorHandle r: handles) {
			r.initialize();

		}
	}
	public void hide() {
		for(MirrorHandle r: handles) {
			r.hide();
		}
	}
	public void show() {
		initialize();
	}
	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z,List<String> selectedCSG, Bounds b, TransformNR cf) {
		this.x.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b,cf);
		this.y.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b,cf);
		this.z.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b,cf);
	}

}
