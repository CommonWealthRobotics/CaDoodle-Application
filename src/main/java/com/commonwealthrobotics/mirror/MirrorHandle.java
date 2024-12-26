package com.commonwealthrobotics.mirror;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.controls.ControlSprites;
import com.commonwealthrobotics.rotate.RotationSessionManager;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.sdk.addons.kinematics.math.EulerAxis;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cylinder;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;

public class MirrorHandle {
	private EulerAxis ax;
	private Affine translate;
	private Affine vr;
	private MirrorSessionManager rotationSessionManager;
	private ActiveProject ap;
	private ControlSprites cs;
	private Affine workplaneOffset;
	private double screenW;
	private double screenH;
	private double zoom;
	private double az;
	private double el;
	private double x;
	private double y;
	private double z;
	private List<String> selectedCSG;
	private Bounds b;
	private TransformNR cf;
	private static double height = 5;
	private static CSG doubbleArrow = null;
	private MeshView mesh=null;
	public MirrorHandle(EulerAxis ax, Affine translate, Affine vr,
			MirrorSessionManager rotationSessionManager, ActiveProject ap, ControlSprites cs, Affine workplaneOffset) {
				this.ax = ax;
				this.translate = translate;
				this.vr = vr;
				this.rotationSessionManager = rotationSessionManager;
				this.ap = ap;
				this.cs = cs;
				this.workplaneOffset = workplaneOffset;
				CSG arrow = getDoubbleArrow();
				mesh=arrow.newMesh();
				
	}
	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z, List<String> selectedCSG, Bounds b, TransformNR cf) {
				this.screenW = screenW;
				this.screenH = screenH;
				this.zoom = zoom;
				this.az = az;
				this.el = el;
				this.x = x;
				this.y = y;
				this.z = z;
				this.selectedCSG = selectedCSG;
				this.b = b;
				this.cf = cf;
		
	}
	public void hide() {
		BowlerStudio.runLater(()->mesh.setVisible(false)) ;
	}
	public void initialize() {
		BowlerStudio.runLater(()->mesh.setVisible(true)) ;
	}
	public List<Node> getElements() {
		ArrayList<Node> result = new ArrayList<Node>(); 
		result.add(mesh);
		return result;
	}
	public static CSG getDoubbleArrow() {
		if(doubbleArrow==null) {
			CSG cone = new Cylinder(height/2,0,height).toCSG().movez(height);
			CSG pin = new Cylinder(height/4,height).toCSG();
			CSG arrow = cone.union(pin).rotx(90);
			doubbleArrow = arrow.union(arrow.rotx(180));
			doubbleArrow.setColor(Color.BLACK);
		}
		return doubbleArrow;
	}
	public static void setDoubbleArrow(CSG doubbleArrow) {
		MirrorHandle.doubbleArrow = doubbleArrow;
	}
}
