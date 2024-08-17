package com.commonwealthrobotics;

import java.util.HashMap;

import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.Cylinder;
import javafx.collections.ObservableFloatArray;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;

public class WorkplaneManager implements EventHandler<MouseEvent>{

	private CaDoodleFile cadoodle;
	private ImageView ground;
	private HashMap<CSG, MeshView> meshes;
	private BowlerStudio3dEngine engine;
	private Affine workplaneLocation = new Affine();
	private MeshView indicatorMesh;
	private TransformNR currentAbsolutePose;
	private Runnable onSelectEvent=()->{};
	private boolean clickOnGround=false;
	private boolean clicked = false;

	public WorkplaneManager(CaDoodleFile f, ImageView ground, BowlerStudio3dEngine engine ) {
		this.cadoodle = f;
		this.ground = ground;
		this.engine = engine;

		ground.addEventFilter(MouseEvent.MOUSE_PRESSED, ev->{
			//System.out.println("Ground Click!");
			setClickOnGround(true);
		});
	}

	public void setIndicator(CSG indicator, Affine centerOffset) {
		if(indicatorMesh!=null) {
			engine.removeUserNode(indicatorMesh);
		}
		indicatorMesh = indicator.newMesh();
		indicatorMesh.getTransforms().addAll(workplaneLocation,centerOffset);
		indicatorMesh.setMouseTransparent(true);

		PhongMaterial material = new PhongMaterial();

		if(indicator.isHole()) {
		    //material.setDiffuseMap(texture);
	        material.setDiffuseColor(new Color(0.25,0.25,0.25,0.75));
	        material.setSpecularColor(javafx.scene.paint.Color.WHITE);        
		}else {
			Color c = indicator.getColor();
			material.setDiffuseColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),0.75));
	        material.setSpecularColor(javafx.scene.paint.Color.WHITE);  
		}
		indicatorMesh.setMaterial(material);
		engine.addUserNode(indicatorMesh);
	}

	public void updateMeshes(HashMap<CSG, MeshView> meshes) {
		this.meshes = meshes;
		
	}
	public void cancle() {
		ground.removeEventFilter(MouseEvent.ANY, this);
		for(CSG key:meshes.keySet()) {
			MeshView mv=meshes.get(key);
			mv.removeEventFilter(MouseEvent.ANY, this);
		}
		indicatorMesh.setVisible(false);
		onSelectEvent.run();
	}
	public void activate() {
		setClickOnGround(false);
		clicked = false;
		System.out.println("Starting workplane listeners");
		ground.addEventFilter(MouseEvent.ANY, this);
		for(CSG key:meshes.keySet()) {
			MeshView mv=meshes.get(key);
			mv.addEventFilter(MouseEvent.ANY, this);
		}
		indicatorMesh.setVisible(true);
	}
	@Override
	public void handle(MouseEvent ev) {
		PickResult pickResult = ev.getPickResult();
		Node intersectedNode = pickResult.getIntersectedNode();
		if(ev.getEventType()==MouseEvent.MOUSE_PRESSED) {
			clicked = true;
			cancle();
		}
		if(ev.getEventType()==MouseEvent.MOUSE_MOVED) {
			//System.out.println(ev);
			Point3D intersectedPoint = pickResult.getIntersectedPoint();
			double x = intersectedPoint.getX();
			double y = intersectedPoint.getY();
			double z = intersectedPoint.getZ();
			if(ev.getSource()==ground) {
				x*=2;
				y*=2;
				z*=2;
			}
			
			double[] angles=new double[]{0, 0,0} ;
			if (intersectedNode instanceof MeshView) {
			    MeshView meshView = (MeshView) intersectedNode;
			    TriangleMesh mesh = (TriangleMesh) meshView.getMesh();
			    
			    int faceIndex = pickResult.getIntersectedFace();
			    angles= getFaceNormalAngles(mesh, faceIndex);
			    
			    //System.out.println("Face normal azimuth: " + angles[0] + "°, tilt: " + angles[1] + "°");
			}
			TransformNR pureRot = new TransformNR(new RotationNR(angles[1],angles[0],angles[2]));
			
			TransformNR t = new TransformNR(x,y,z);
			setCurrentAbsolutePose(t.times(pureRot));
			
		}
	}
	private double[] getFaceEulerAngles(TriangleMesh mesh, int faceIndex) {
	    ObservableFaceArray faces = mesh.getFaces();
	    ObservableFloatArray points = mesh.getPoints();
	    
	    int p1Index = faces.get(faceIndex * 6) * 3;
	    int p2Index = faces.get(faceIndex * 6 + 2) * 3;
	    int p3Index = faces.get(faceIndex * 6 + 4) * 3;
	    
	    Point3D p1 = new Point3D(points.get(p1Index), points.get(p1Index + 1), points.get(p1Index + 2));
	    Point3D p2 = new Point3D(points.get(p2Index), points.get(p2Index + 1), points.get(p2Index + 2));
	    Point3D p3 = new Point3D(points.get(p3Index), points.get(p3Index + 1), points.get(p3Index + 2));
	    
	    Point3D v1 = p2.subtract(p1);
	    Point3D v2 = p3.subtract(p1);
	    
	    Point3D normal = v1.crossProduct(v2).normalize();
	    
	    // Calculate yaw (azimuth) and pitch (tilt)
	    double yaw = Math.atan2(normal.getY(), normal.getX());
	    double pitch = Math.asin(normal.getZ());
	    
	    // Calculate roll to align local X with global X
	    Point3D globalX = new Point3D(1, 0, 0);
	    Point3D localY = normal.crossProduct(globalX).normalize();
	    Point3D localX = normal.crossProduct(localY).normalize();
	    double roll = Math.atan2(localY.getZ(), localX.getZ());
	    
	    // Convert to degrees
	    yaw = Math.toDegrees(yaw);
	    pitch = Math.toDegrees(pitch);
	    roll = Math.toDegrees(roll);
	    
	    // Bound yaw and pitch to -180 to 180
	    yaw = boundAngle180(yaw);
	    pitch = boundAngle180(pitch);
	    
	    // Bound roll to -90 to 90
	    roll = boundAngle90(roll);
	    
	    return new double[]{yaw, pitch, roll};
	}

	private double boundAngle180(double angle) {
	    angle = angle % 360;
	    if (angle > 180) {
	        angle -= 360;
	    } else if (angle < -180) {
	        angle += 360;
	    }
	    return angle;
	}

	private double boundAngle90(double angle) {
	    angle = boundAngle180(angle);
	    if (angle > 90) {
	        angle = 180 - angle;
	    } else if (angle < -90) {
	        angle = -180 - angle;
	    }
	    return angle;
	}
	private double[] getFaceNormalAngles(TriangleMesh mesh, int faceIndex) {
	    ObservableFaceArray faces = mesh.getFaces();
	    ObservableFloatArray points = mesh.getPoints();
	    
	    int p1Index = faces.get(faceIndex * 6) * 3;
	    int p2Index = faces.get(faceIndex * 6 + 2) * 3;
	    int p3Index = faces.get(faceIndex * 6 + 4) * 3;
	    
	    Point3D p1 = new Point3D(points.get(p1Index), points.get(p1Index + 1), points.get(p1Index + 2));
	    Point3D p2 = new Point3D(points.get(p2Index), points.get(p2Index + 1), points.get(p2Index + 2));
	    Point3D p3 = new Point3D(points.get(p3Index), points.get(p3Index + 1), points.get(p3Index + 2));
	    
	    Point3D v1 = p2.subtract(p1);
	    Point3D v2 = p3.subtract(p1);
	    
	    Point3D normal = v1.crossProduct(v2).normalize();
	    
	    // Calculate azimuth and tilt
	    double azimuth = Math.atan2(normal.getY(), normal.getX());
	    double tilt = Math.asin(normal.getZ());
	    
	    // Convert to degrees
	    azimuth = Math.toDegrees(azimuth)-90;
	    tilt = Math.toDegrees(tilt)-90;
	    if(Math.abs(tilt)<0.01) {
	    	azimuth=0;
	    }
	    Point3D globalX = new Point3D(1, 0, 0);
	    Point3D localY = normal.crossProduct(globalX).normalize();
	    Point3D localX = normal.crossProduct(localY).normalize();
	    double roll = Math.atan2(localY.getZ(), localX.getZ());
	    
	    // Ensure azimuth is in the range [-180, 180)
	    if (azimuth < -180) {
	        azimuth += 360;
	    }
	    if (azimuth > 180) {
	        azimuth -= 360;
	    }
	    return new double[]{azimuth, tilt,roll};
	}

	public TransformNR getCurrentAbsolutePose() {
		return currentAbsolutePose;
	}

	public void setCurrentAbsolutePose(TransformNR currentAbsolutePose) {
		this.currentAbsolutePose = currentAbsolutePose;
		TransformFactory.nrToAffine(getCurrentAbsolutePose(), workplaneLocation);
	}

	public Runnable getOnSelectEvent() {
		return onSelectEvent;
	}

	public void setOnSelectEvent(Runnable onSelectEvent) {
		this.onSelectEvent = onSelectEvent;
	}

	public boolean isClickOnGround() {
		return clickOnGround;
	}

	public void setClickOnGround(boolean clickOnGround) {
		this.clickOnGround = clickOnGround;
	}

	public boolean isClicked() {
		return clicked;
	}
}
