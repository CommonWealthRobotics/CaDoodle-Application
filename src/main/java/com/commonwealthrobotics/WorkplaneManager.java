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

	public WorkplaneManager(CaDoodleFile f, ImageView ground, BowlerStudio3dEngine engine ) {
		this.cadoodle = f;
		this.ground = ground;
		this.engine = engine;
		CSG indicator = new Cylinder(5,0,2.5,3).toCSG();
		
		setIndicator( indicator, new Affine());
		
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
		if(ev.getEventType()==MouseEvent.MOUSE_PRESSED) {
			cancle();
		}
		if(ev.getEventType()==MouseEvent.MOUSE_MOVED) {
			//System.out.println(ev);
			PickResult pickResult = ev.getPickResult();
			Point3D intersectedPoint = pickResult.getIntersectedPoint();
			double x = intersectedPoint.getX();
			double y = intersectedPoint.getY();
			double z = intersectedPoint.getZ();
			if(ev.getSource()==ground) {
				x*=2;
				y*=2;
				z*=2;
			}
			Node intersectedNode = pickResult.getIntersectedNode();
			double[] angles=new double[]{0, 0} ;
			if (intersectedNode instanceof MeshView) {
			    MeshView meshView = (MeshView) intersectedNode;
			    TriangleMesh mesh = (TriangleMesh) meshView.getMesh();
			    
			    int faceIndex = pickResult.getIntersectedFace();
			    angles= getFaceNormalAngles(mesh, faceIndex);
			    
			    //System.out.println("Face normal azimuth: " + angles[0] + "°, tilt: " + angles[1] + "°");
			}
			TransformNR pureRot = new TransformNR(new RotationNR(angles[1],angles[0],0));
			
			TransformNR t = new TransformNR(x,y,z);
			setCurrentAbsolutePose(t.times(pureRot));
			
		}
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
	    
	    // Ensure azimuth is in the range [-180, 180)
	    if (azimuth < -180) {
	        azimuth += 360;
	    }
	    if (azimuth > 180) {
	        azimuth -= 360;
	    }
	    return new double[]{azimuth, tilt};
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
}
