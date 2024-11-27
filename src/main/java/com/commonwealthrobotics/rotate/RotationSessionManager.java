package com.commonwealthrobotics.rotate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.controls.ControlSprites;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.sdk.addons.kinematics.math.EulerAxis;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

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
	private Affine viewRotation = new Affine();
	private ControlSprites controlSprites;

	public RotationSessionManager(Affine selection, ActiveProject ap, ControlSprites controlSprites, Affine workplaneOffset) {
		this.selection = selection;
		this.controlSprites = controlSprites;
		 az= new RotationHandle(EulerAxis.azimuth,selection,getViewRotation(),this,ap,controlSprites,workplaneOffset);
		 el= new RotationHandle(EulerAxis.elevation,selection,getViewRotation(),this,ap,controlSprites,workplaneOffset);
		 tlt= new RotationHandle(EulerAxis.tilt,selection,getViewRotation(),this,ap,controlSprites,workplaneOffset);
	}
	
	public List<Node> getElements(){
		ArrayList<Node> result = new ArrayList<Node>(); 
		for(RotationHandle r: Arrays.asList(az,el,tlt)) {
			result.add(r.handle);
			result.add(r.controlCircle);
			result.add(r.arc);
			result.add(r.text.get());
		}
		return result;
	}
	public void initialize() {
		for(RotationHandle r: Arrays.asList(az,el,tlt)) {
			r.handle.setVisible(true);
			r.controlCircle.setVisible(false);
			r.arc.setVisible(false);
			r.text.hide();

		}

	}
	public void hide() {
		for(RotationHandle r: Arrays.asList(az,el,tlt)) {
			r.handle.setVisible(false);
			r.controlCircle.setVisible(false);
			r.arc.setVisible(false);
			r.text.hide();
		}
	}
	public void show() {
		initialize();
	}
	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z,List<String> selectedCSG, Bounds b, TransformNR cf) {
		this.az.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b,cf);
		this.el.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b,cf);
		this.tlt.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b,cf);
	}

	public Affine getViewRotation() {
		return viewRotation;
	}

	public void setViewRotation(Affine viewRotation) {
		this.viewRotation = viewRotation;
	}
	public void resetSelected() {
		for(RotationHandle r: Arrays.asList(az,el,tlt)) {
			r.setSelected(false);
		}
	}

}
