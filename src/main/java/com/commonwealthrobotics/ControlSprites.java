package com.commonwealthrobotics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;
import com.neuronrobotics.bowlerstudio.BowlerKernel;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.MoveCenter;
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
import javafx.scene.input.MouseEvent;
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
	private ScaleSessionManager scaleSession;
	private RotationSessionManager rotationManager;
	private Rectangle footprint = new Rectangle(100,100,new Color(0,0,1,0.25));
	//private Rectangle bottomDimentions = new Rectangle(100,100,new Color(0,0,1,0.25));
	private Line frontLine = new Line(1, 1, 2, 2);
	private Line backLine = new Line(1, 1, 2, 2);
	private Line leftLine = new Line(1, 1, 2, 2);
	private Line rightLine = new Line(1, 1, 2, 2);
	private Line heightLine = new Line(1, 1, 2, 2);
	
	private ArrayList<Node> allElems=new ArrayList<Node>();
    private boolean selectionLive = false;
	private Bounds bounds;
	private List<Line> lines;
	private Affine spriteFace = new Affine();
	private MeshView moveUpArrow;
	private Affine moveUpLocation=new Affine();
	private Scale scaleTF = new Scale();
	private Affine selection;
	private Manipulation xyMove;
	private Manipulation zMove;
	private CaDoodleFile cadoodle;
	private double size;
	private Bounds b;
	public void setSnapGrid(double size) {
		this.size = size;
		zMove.setIncrement(size);
		scaleSession.setSnapGrid( size);
	}
	public ControlSprites(SelectionSession session,BowlerStudio3dEngine e, Affine selection,Manipulation xyMove,CaDoodleFile c) {
		this.session = session;

		this.engine = e;
		this.selection = selection;
		this.xyMove = xyMove;
		this.cadoodle = c;
		Affine zMoveOffsetFootprint =  new Affine();
		zMove = new Manipulation(selection, new Vector3d(0,0, 1), new TransformNR());
		zMove.addSaveListener(()->{
			System.out.println("Objects Moved! "+zMove.getGlobalPose().toSimpleString());
			Thread t= cadoodle.addOpperation(new MoveCenter()
					.setLocation(zMove.getGlobalPose().copy())
					.setNames(session.selectedSnapshot()));
			try {
				t.join();
			} catch (InterruptedException exx) {
				// TODO Auto-generated catch block
				exx.printStackTrace();
			}
			zMove.set(0, 0, 0);
			BowlerKernel.runLater(() -> {
				TransformFactory.nrToAffine(new TransformNR(), zMoveOffsetFootprint);
			});
			
		});
		zMove.addEventListener(()->{
			
			TransformNR inverse = zMove.getCurrentPose().copy().inverse();
			System.out.println("ApplyOffset "+inverse.toSimpleString());
			BowlerKernel.runLater(() -> TransformFactory.nrToAffine(inverse.translateZ(0.1), zMoveOffsetFootprint));
		});
		
		
		CSG setColor = new Cylinder(ResizingHandle.getSize()/2, 0,ResizingHandle.getSize() )
				.toCSG()
				.setColor(Color.BLACK);
		moveUpArrow = setColor.getMesh();
		moveUpArrow.getTransforms().add(selection);
		moveUpArrow.getTransforms().add(moveUpLocation);
		moveUpArrow.getTransforms().add(scaleTF);
		moveUpArrow.addEventFilter(MouseEvent.ANY, zMove.getMouseEvents());
		
		Affine heightLineOrentation = new Affine();
		BowlerStudio.runLater(() -> 		TransformFactory
				.nrToAffine(new TransformNR(RotationNR.getRotationY(-90)),heightLineOrentation));
		
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
			l.setMouseTransparent(true);
		}
		footprint.getTransforms().add(zMoveOffsetFootprint);
		footprint.getTransforms().add(selection);
		footprint.setMouseTransparent(true);
		scaleSession=new ScaleSessionManager(e, selection,()->updateLines(),cadoodle,session);
		List<Node> tmp = Arrays.asList(scaleSession.topCenter.getMesh(),scaleSession.rightFront.getMesh(),
				scaleSession.rightRear.getMesh(),
				scaleSession.leftFront.getMesh(),
				scaleSession.leftRear.getMesh(),footprint,frontLine,backLine,leftLine,rightLine,heightLine,moveUpArrow);
		allElems.addAll(tmp);
		
		rotationManager = new RotationSessionManager(selection,cadoodle);
		allElems.addAll(rotationManager.getElements());
		
		clearSelection();
		setUpUIComponennts();
		
	}

	private void setUpUIComponennts() {
		Group controlsGroup = new Group();
        controlsGroup.setDepthTest(DepthTest.DISABLE);
        controlsGroup.setViewOrder(-1);  // Lower viewOrder renders on top

		BowlerStudio.runLater(() -> {
			engine.addUserNode(footprint);
			for(Node r:allElems) {
				if(MeshView.class.isInstance(r)) {
					((MeshView)r).setCullFace(CullFace.BACK);
				}
				if(r==footprint)
					continue;
				controlsGroup.getChildren().add(r);
			}
			engine.addUserNode(controlsGroup);
		});
	}

	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z,List<String> selectedCSG, Bounds b) {
		
		
		this.b = b;
		if(!selectionLive) {
			selectionLive=true;
			BowlerStudio.runLater(() -> {
				initialize();
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
		rotationManager.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b);
		updateCubes();
		updateLines();
	}
	private void initialize() {
		for(Node r:allElems)
			r.setVisible(true);
		rotationManager.initialize();
	}
	private void updateLines() {
		
		this.bounds = scaleSession.getBounds();
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
		double lineEndOffsetZ = Math.min(5,max.z-min.z);
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
		BowlerStudio.runLater(() -> {
			TransformFactory.nrToAffine(new TransformNR(RotationNR.getRotationZ(90-az)),spriteFace);
			TransformFactory.nrToAffine(new TransformNR(center.x,center.y,max.z+ResizingHandle.getSize()),moveUpLocation);
		});
		for(Line l:lines) {
			l.setStrokeWidth(1+lineScale);
			l.setTranslateZ(min.z);
		}
		//bottomDimentions.bl;

		scaleTF.setX(scaleSession.getScale());
		scaleTF.setY(scaleSession.getScale());
		scaleTF.setZ(scaleSession.getScale());
	}
	public Affine getViewRotation() {
		return rotationManager.getViewRotation();
	}

	private void updateCubes() {
		scaleSession.threeDTarget(screenW,screenH,zoom,b);
	}
	public void clearSelection() {
		BowlerStudio.runLater(() -> {
			for(Node r:allElems)
				r.setVisible(false);
		});
		selectionLive = false;
	}
}
