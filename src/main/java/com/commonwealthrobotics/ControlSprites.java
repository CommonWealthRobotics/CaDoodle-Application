package com.commonwealthrobotics;

import java.util.Arrays;
import java.util.List;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

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
	
	private Rectangle footprint = new Rectangle(100,100,new Color(0,0,1,0.25));
	private Rectangle bottomDimentions = new Rectangle(100,100,new Color(0,0,1,0.25));
	private List<Rectangle> allElems;
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
		bottomDimentions.setFill(null);
		bottomDimentions.setStroke(Color.BLACK);
		bottomDimentions.setStrokeWidth(2);
		bottomDimentions.setStrokeLineCap(StrokeLineCap.BUTT);
		bottomDimentions.setStrokeLineJoin(StrokeLineJoin.MITER);
		bottomDimentions.getStrokeDashArray().addAll(10.0, 5.0, 2.0, 5.0);
		allElems = Arrays.asList(topCenter,rightFront,rightRear,leftFront,leftRear,footprint,bottomDimentions);
		clearSelection();
		BowlerStudio.runLater(() -> {
			engine.addUserNode(footprint);
			engine.addUserNode(bottomDimentions);
			controls.getChildren().add(topCenter);
			controls.getChildren().add(rightFront);
			controls.getChildren().add(rightRear);
			controls.getChildren().add(leftFront);
			controls.getChildren().add(leftRear);
		});
//		AnchorPane.setTopAnchor(topCenter, screenH/2);
//        AnchorPane.setLeftAnchor(topCenter, screenW/2);
	}
	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z,List<CSG> selectedCSG, Bounds bounds) {
		this.bounds = bounds;
		if(!selectionLive) {
			selectionLive=true;
			BowlerStudio.runLater(() -> {
				for(Rectangle r:allElems)
					r.setVisible(true);
			});
		}
		if(el<-90 ||el>90) {
			footprint.setVisible(false);
		}else {
			footprint.setVisible(true);
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
		footprint.setHeight(Math.abs(max.y-min.y));
		footprint.setWidth(Math.abs(max.x-min.x));
		footprint.setX(min.x);
		footprint.setY(min.y);
		
		bottomDimentions.setHeight(Math.abs(max.y-min.y));
		bottomDimentions.setWidth(Math.abs(max.x-min.x));
		bottomDimentions.setX(min.x);
		bottomDimentions.setY(min.y);
		bottomDimentions.setTranslateZ(min.z);
		bottomDimentions.setStrokeWidth(1+2*(-zoom/1000));
		
		topCenter.threeDTarget(screenW,screenH,zoom, new TransformNR(center.x,center.y,max.z));
		rightFront.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, max.y, min.z));
		rightRear.threeDTarget(screenW, screenH, zoom, new TransformNR(min.x, max.y,  min.z));
		leftFront.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, min.y,  min.z));
		leftRear.threeDTarget(screenW,screenH,zoom, new TransformNR(min.x,min.y, min.z));

//		topCenter.threeDTarget(screenW,screenH,zoom, new TransformNR(0,0,0));
//		rightFront.threeDTarget(screenW, screenH, zoom, new TransformNR(40, 0,0));
//		leftFront.threeDTarget(screenW, screenH, zoom, new TransformNR(0,25.4*2,0));


        //System.out.println("ScrW:"+screenW+"ScrH:"+screenH+" zoom:"+zoom+" Pan:"+az+" Tilt:"+el+" X:"+x+" Y:"+y+" Z:"+z);
		
	}
	public void clearSelection() {
		BowlerStudio.runLater(() -> {
			for(Rectangle r:allElems)
				r.setVisible(false);
		});
		selectionLive = false;
	}
}
