package com.commonwealthrobotics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.neuronrobotics.sdk.addons.kinematics.math.EulerAxis;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Affine;

public class RotationSessionManager {
	RotationHandle az;
	RotationHandle el;
	RotationHandle tlt;

	private Affine selection;

	public RotationSessionManager(Affine selection) {
		this.selection = selection;
		 az= new RotationHandle(EulerAxis.azimuth,selection);
		 el= new RotationHandle(EulerAxis.elevation,selection);
		 tlt= new RotationHandle(EulerAxis.tilt,selection);
	}
	
	public List<Node> getElements(){
		ArrayList<Node> result = new ArrayList<Node>(); 
		for(RotationHandle r: Arrays.asList(az,el,tlt)) {
			result.add(r.handle);
			result.add(r.controlCircle);
		}
		return result;
	}
	public void initialize() {
		for(RotationHandle r: Arrays.asList(az,el,tlt)) {
			r.handle.setVisible(true);
			r.controlCircle.setVisible(false);
		}
	}
	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z,List<CSG> selectedCSG, Bounds b) {
		this.az.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b);
		this.el.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b);
		this.tlt.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b);
	}

}
