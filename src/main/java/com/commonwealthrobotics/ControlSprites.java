package com.commonwealthrobotics;

import java.util.List;

import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ControlSprites {
	private AnchorPane controls;
	private double screenW;
	private double screenH;
	private double zoom;
	private double az;
	private double el;
	private double x;
	private double y;
	private double z;
	private SelectionSession session;
	private BowlerStudio3dEngine engine;
	ControlRectangle topCenter = null;
	ControlRectangle rightFront =null;
	ControlRectangle rightRear =null;
	ControlRectangle leftFront =null;
	ControlRectangle leftRear =null;
    private boolean selectionLive = false;
	private Bounds bounds;

	public ControlSprites(AnchorPane controls, SelectionSession session,BowlerStudio3dEngine engine) {
		this.session = session;
		if(controls==null)
			throw new NullPointerException();
		this.controls = controls;
		this.engine = engine;
		topCenter= new ControlRectangle(engine);
		rightFront= new ControlRectangle(engine);
		rightRear =new ControlRectangle(engine);
		leftFront =new ControlRectangle(engine);
		leftRear =new ControlRectangle(engine);
//		AnchorPane.setTopAnchor(topCenter, screenH/2);
//        AnchorPane.setLeftAnchor(topCenter, screenW/2);
	}
	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z,List<CSG> selectedCSG, Bounds bounds) {
		this.bounds = bounds;
		if(!selectionLive) {
			selectionLive=true;
			controls.getChildren().add(topCenter);
			controls.getChildren().add(rightFront);
			controls.getChildren().add(rightRear);
			controls.getChildren().add(leftFront);
			controls.getChildren().add(leftRear);

		}
		

		this.screenW = controls.getWidth();
		this.screenH = controls.getHeight();
		this.zoom = zoom;
		this.az = az;
		this.el = el;
		this.x = x;
		this.y = y;
		this.z = z;
		Vector3d center = bounds.getCenter();
		Vector3d min = bounds.getMin();
		Vector3d max = bounds.getMax();
		topCenter.threeDTarget(screenW,screenH,zoom, new TransformNR(center.x,center.y,max.z));
		rightFront.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, max.y, 0));
		rightRear.threeDTarget(screenW, screenH, zoom, new TransformNR(min.x, max.y, 0));
		leftFront.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, min.y, 0));
		leftRear.threeDTarget(screenW,screenH,zoom, new TransformNR(min.x,min.y,0));



        System.out.println("ScrW:"+screenW+"ScrH:"+screenH+" zoom:"+zoom+" Pan:"+az+" Tilt:"+el+" X:"+x+" Y:"+y+" Z:"+z);
		
	}
	public void clearSelection() {
		controls.getChildren().clear();
		selectionLive = false;
	}
}
