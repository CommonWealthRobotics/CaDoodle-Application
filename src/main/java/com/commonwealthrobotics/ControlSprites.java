package com.commonwealthrobotics;

import java.util.List;

import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
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
	Rectangle topCenter = new Rectangle(10, 10, Color.WHITE);
    private boolean selectionLive = false;

	public ControlSprites(AnchorPane controls, SelectionSession session,BowlerStudio3dEngine engine) {
		this.session = session;
		if(controls==null)
			throw new NullPointerException();
		this.controls = controls;
		topCenter.setStroke(Color.BLACK);
		this.engine = engine;
//		AnchorPane.setTopAnchor(topCenter, screenH/2);
//        AnchorPane.setLeftAnchor(topCenter, screenW/2);
	}
	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z,List<CSG> selectedCSG) {
		if(!selectionLive) {
			selectionLive=true;
			controls.getChildren().add(topCenter);

		}
		
		TransformNR target= new TransformNR(50,0,0);

		this.screenW = controls.getWidth();
		this.screenH = controls.getHeight();
		this.zoom = zoom;
		this.az = az;
		this.el = el;
		this.x = x;
		this.y = y;
		this.z = z;
		//TransformNR cameraLocation =  new TransformNR(0,0,-zoom);
		TransformNR cf = engine.getFlyingCamera().getCamerFrame();

        
        topCenter.setLayoutX(screenW/2);
        topCenter.setLayoutY(screenH/2);
        System.out.println("\n"+cf.toSimpleString());
		System.out.println("ScrW:"+screenW+"ScrH:"+screenH+" zoom:"+zoom+" Pan:"+az+" Tilt:"+el+" X:"+x+" Y:"+y+" Z:"+z);
		
	}
	public void clearSelection() {
		controls.getChildren().clear();
		selectionLive = false;
	}
}
