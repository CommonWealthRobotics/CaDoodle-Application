package com.commonwealthrobotics.allign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.commonwealthrobotics.RotationHandle;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.scene.Node;
import javafx.scene.transform.Affine;

public class AllignManager {
	AllignRadioSet frontBack;
	AllignRadioSet leftRight;
	AllignRadioSet upDown;

	public AllignManager(Affine move, Affine workplaneOffset) {
		frontBack = new AllignRadioSet("frontBack", move, workplaneOffset, new Vector3d(1,0,0));
		leftRight = new AllignRadioSet("leftRight", move, workplaneOffset, new Vector3d(0,1,0));
		upDown = new AllignRadioSet("upDown", move, workplaneOffset, new Vector3d(0,0,1));
	}
	public void threeDTarget(double screenW, double screenH, double zoom,Bounds b, TransformNR cf) {
		frontBack.threeDTarget(screenW, screenH, zoom, b, cf);
		leftRight.threeDTarget(screenW, screenH, zoom, b, cf);
		upDown.threeDTarget(screenW, screenH, zoom, b, cf);
	}
	public List<Node> getElements(){
		ArrayList<Node> result = new ArrayList<Node>(); 
		for(AllignRadioSet r: Arrays.asList(frontBack,leftRight,upDown)) {
			result.addAll(r. getElements());
		}
		return result;
	}
}
