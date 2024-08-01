package com.commonwealthrobotics;

import java.util.List;

import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
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
	ControlRectangle right =null;
	ControlRectangle forward =null;
    private boolean selectionLive = false;

	public ControlSprites(AnchorPane controls, SelectionSession session,BowlerStudio3dEngine engine) {
		this.session = session;
		if(controls==null)
			throw new NullPointerException();
		this.controls = controls;
		this.engine = engine;
		topCenter= new ControlRectangle(engine);
		right= new ControlRectangle(engine);
		forward= new ControlRectangle(engine);
//		AnchorPane.setTopAnchor(topCenter, screenH/2);
//        AnchorPane.setLeftAnchor(topCenter, screenW/2);
	}
	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z,List<CSG> selectedCSG) {
		if(!selectionLive) {
			selectionLive=true;
			controls.getChildren().add(topCenter);
			controls.getChildren().add(right);
			controls.getChildren().add(forward);
		}
		

		this.screenW = controls.getWidth();
		this.screenH = controls.getHeight();
		this.zoom = zoom;
		this.az = az;
		this.el = el;
		this.x = x;
		this.y = y;
		this.z = z;
		
		topCenter.threeDTarget(screenW,screenH,zoom, new TransformNR(50,0,0));
		right.threeDTarget(screenW,screenH,zoom, new TransformNR(0,0,0));
		forward.threeDTarget(screenW,screenH,zoom, new TransformNR(0,0,100));


        System.out.println("ScrW:"+screenW+"ScrH:"+screenH+" zoom:"+zoom+" Pan:"+az+" Tilt:"+el+" X:"+x+" Y:"+y+" Z:"+z);
		
	}
	public void clearSelection() {
		controls.getChildren().clear();
		selectionLive = false;
	}
}
