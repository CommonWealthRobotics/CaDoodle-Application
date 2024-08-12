package com.commonwealthrobotics;

import java.util.Arrays;
import java.util.List;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cylinder;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Line;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;

public class ControlSprites {
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
	//private BowlerStudio3dEngine spriteEngine;
	ControlRectangle topCenter = null;
	ControlRectangle rightFront =null;
	ControlRectangle rightRear =null;
	ControlRectangle leftFront =null;
	ControlRectangle leftRear =null;
	
	private Rectangle footprint = new Rectangle(100,100,new Color(0,0,1,0.25));
	//private Rectangle bottomDimentions = new Rectangle(100,100,new Color(0,0,1,0.25));
	private Line frontLine = new Line(1, 1, 2, 2);
	private Line backLine = new Line(1, 1, 2, 2);
	private Line leftLine = new Line(1, 1, 2, 2);
	private Line rightLine = new Line(1, 1, 2, 2);
	private Line heightLine = new Line(1, 1, 2, 2);
	
	private List<Node> allElems;
    private boolean selectionLive = false;
	private Bounds bounds;
	private List<Line> lines;
	private Affine spriteFace = new Affine();
	private MeshView moveUpArrow;
	private Affine moveUpLocation=new Affine();
	private Scale scaleTF = new Scale();
	private Affine selection;

	public ControlSprites(SelectionSession session,BowlerStudio3dEngine e, Affine selection) {
		this.session = session;

		this.engine = e;
		this.selection = selection;
		topCenter= new ControlRectangle(engine,selection);
		rightFront= new ControlRectangle(engine,selection);
		rightRear =new ControlRectangle(engine,selection);
		leftFront =new ControlRectangle(engine,selection);
		leftRear =new ControlRectangle(engine,selection);
		CSG setColor = new Cylinder(ControlRectangle.getSize()/2, 0,ControlRectangle.getSize() )
				.toCSG()
				.setColor(Color.BLACK);
		moveUpArrow = setColor.getMesh();
		moveUpArrow.getTransforms().add(selection);
		moveUpArrow.getTransforms().add(moveUpLocation);
		moveUpArrow.getTransforms().add(scaleTF);
		Affine heightLineOrentation = TransformFactory.nrToAffine(new TransformNR(RotationNR.getRotationY(-90)));
		heightLine.getTransforms().add(selection);
		heightLine.getTransforms().add(spriteFace);
		heightLine.getTransforms().add(heightLineOrentation);
		lines = Arrays.asList(frontLine,backLine,leftLine,rightLine,heightLine);
		for(Line l:lines) {
			if(l!=heightLine)
				l.getTransforms().add(selection);
			l.setFill(null);
			l.setStroke(Color.BLACK);
			l.setStrokeWidth(2);
			l.setStrokeLineCap(StrokeLineCap.BUTT);
			l.setStrokeLineJoin(StrokeLineJoin.MITER);
			l.getStrokeDashArray().addAll(10.0, 5.0, 2.0, 5.0);
		}
		footprint.getTransforms().add(selection);
		allElems = Arrays.asList(topCenter.getMesh(),rightFront.getMesh(),
				rightRear.getMesh(),
				leftFront.getMesh(),
				leftRear.getMesh(),footprint,frontLine,backLine,leftLine,rightLine,heightLine,moveUpArrow);
		
		clearSelection();

		setUpUIComponennts();
	}
	private void setUpUIComponennts() {
		Group controlsGroup = new Group();
        controlsGroup.setDepthTest(DepthTest.DISABLE);
        controlsGroup.setViewOrder(-1);  // Lower viewOrder renders on top

		BowlerStudio.runLater(() -> {
			engine.addUserNode(footprint);
			for(Line l:lines)
				controlsGroup.getChildren().add(l);
			for(Node r:allElems) {
				if(MeshView.class.isInstance(r)) {
					
					controlsGroup.getChildren().add(r);
					((MeshView)r).setCullFace(CullFace.BACK);
				}
			}
			engine.addUserNode(controlsGroup);
		});
	}

	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z,List<CSG> selectedCSG, Bounds bounds) {
		this.bounds = bounds;
		if(!selectionLive) {
			selectionLive=true;
			BowlerStudio.runLater(() -> {
				for(Node r:allElems)
					r.setVisible(true);
			});
		}
		if(el<-90 ||el>90) {
			footprint.setVisible(false);
		}else {
			footprint.setVisible(true);
		}

		this.screenW = screenW;
		this.screenH = screenH;
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

		double lineScale = 2*(-zoom/1000);
		double lineEndOffsetY = Math.min(10*lineScale,max.y-min.y);
		double lineEndOffsetX = Math.min(10*lineScale,max.x-min.x);
		double lineEndOffsetZ = Math.min(10*lineScale,max.z-min.z);
		frontLine.setStartX(max.x);
		frontLine.setStartY(min.y+lineEndOffsetY);
		frontLine.setEndX(max.x);
		frontLine.setEndY(max.y-lineEndOffsetY);
		
		backLine.setStartX(min.x);
		backLine.setStartY(min.y+lineEndOffsetY);
		backLine.setEndX(min.x);
		backLine.setEndY(max.y-lineEndOffsetY);
		
		leftLine.setStartX(min.x+lineEndOffsetX);
		leftLine.setStartY(max.y);
		leftLine.setEndX(max.x-lineEndOffsetX);
		leftLine.setEndY(max.y);
		
		rightLine.setStartX(min.x+lineEndOffsetX);
		rightLine.setStartY(min.y);
		rightLine.setEndX(max.x-lineEndOffsetX);
		rightLine.setEndY(min.y);
		
		heightLine.setStartX(0);
		heightLine.setStartY(0);
		heightLine.setEndX(max.z-min.z-lineEndOffsetZ);
		heightLine.setEndY(0);
		heightLine.setTranslateX(center.x);
		heightLine.setTranslateY(center.y);
		//moveUpLocation
		TransformFactory.nrToAffine(new TransformNR(RotationNR.getRotationZ(90-az)),spriteFace);
		TransformFactory.nrToAffine(new TransformNR(center.x,center.y,max.z+ControlRectangle.getSize()),moveUpLocation);

		
		for(Line l:lines) {
			l.setStrokeWidth(1+lineScale);
			l.setTranslateZ(min.z);
		}
		//bottomDimentions.bl;
		
		topCenter.threeDTarget(screenW,screenH,zoom, new TransformNR(center.x,center.y,max.z));
		rightFront.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, max.y, min.z));
		rightRear.threeDTarget(screenW, screenH, zoom, new TransformNR(min.x, max.y,  min.z));
		leftFront.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, min.y,  min.z));
		leftRear.threeDTarget(screenW,screenH,zoom, new TransformNR(min.x,min.y, min.z));
		
		scaleTF.setX(topCenter.getScale());
		scaleTF.setY(topCenter.getScale());
		scaleTF.setZ(topCenter.getScale());

//		topCenter.threeDTarget(screenW,screenH,zoom, new TransformNR(0,0,0));
//		rightFront.threeDTarget(screenW, screenH, zoom, new TransformNR(40, 0,0));
//		leftFront.threeDTarget(screenW, screenH, zoom, new TransformNR(0,25.4*2,0));


        //System.out.println("ScrW:"+screenW+"ScrH:"+screenH+" zoom:"+zoom+" Pan:"+az+" Tilt:"+el+" X:"+x+" Y:"+y+" Z:"+z);
		
	}
	public void clearSelection() {
		BowlerStudio.runLater(() -> {
			for(Node r:allElems)
				r.setVisible(false);
		});
		selectionLive = false;
	}
}
