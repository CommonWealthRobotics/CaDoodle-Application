package com.commonwealthrobotics.controls;

import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.ChamferedCube;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.Transform;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;

public class ResizingHandle {

	private BowlerStudio3dEngine engine;
	private PerspectiveCamera camera;
	private static final double size=20;
	private MeshView mesh;
	private Affine location = new Affine();
	private Affine cameraOrent = new Affine();
	private Scale scaleTF = new Scale();
	private double scale;
	Manipulation manipulator;
	private Affine resizeHandleLocation = new Affine();
	private String name;
	//private Tooltip hover = new Tooltip();
	/**
	 * Creates a new instance of Rectangle with the given size and fill.
	 * @param name 
	 * @param vector3d 
	 * @param workplaneOffset 
	 * 
	 * @param width  width of the rectangle
	 * @param height height of the rectangle
	 * @param fill   determines how to fill the interior of the rectangle
	 */
	public ResizingHandle(String name, BowlerStudio3dEngine engine,Affine move, Vector3d vector3d, Affine workplaneOffset) {
		this.name = name;
		manipulator=new Manipulation(resizeHandleLocation, vector3d, new TransformNR());
//		super(12.0, 12.0, Color.WHITE);
//		setStroke(Color.BLACK);
//		setStrokeWidth(3);
		this.engine = engine;
		camera = engine.getFlyingCamera().getCamera();
		CSG cube = new ChamferedCube(getSize(),getSize(),getSize(),getSize()/5).toCSG().toZMin();
		mesh=cube.getMesh();
		PhongMaterial material = new PhongMaterial();
		material.setDiffuseColor(new Color(1,1,1,1));
		 getMesh().setCullFace(CullFace.NONE);
	    //material.setSpecularColor(javafx.scene.paint.Color.WHITE);
	    getMesh().setMaterial(material);
		getMesh().addEventFilter(MouseEvent.MOUSE_EXITED,event -> {
			material.setDiffuseColor(new Color(1,1,1,1));
		});
		getMesh().addEventFilter(MouseEvent.MOUSE_ENTERED,event -> {
			material.setDiffuseColor(new Color(1,0,0,1));
			
		});
		mesh.getTransforms().add(move);
		mesh.getTransforms().add(resizeHandleLocation);
		mesh.getTransforms().add(workplaneOffset);
		mesh.getTransforms().add(location);
		mesh.getTransforms().add(cameraOrent);
		mesh.getTransforms().add(scaleTF);
		//Tooltip.install(mesh, hover);
		mesh.addEventFilter(MouseEvent.ANY, manipulator.getMouseEvents());

	}
	public TransformNR getParametric() {
		return new TransformNR(
				resizeHandleLocation.getTx(),
				resizeHandleLocation.getTy(),
				resizeHandleLocation.getTz()	
				);
	}
	public void setInReferenceFrame(double x, double y, double z) {
		TransformNR wp =  manipulator.getFrameOfReference().copy();
		TransformNR tmp = new TransformNR(x, y, z);
		
		TransformFactory.nrToAffine(tmp,location);
	}
	public TransformNR getCurrentInGlobalFrame() {
		TransformNR wp =  manipulator.getFrameOfReference().copy();
		TransformNR rsz = TransformFactory.affineToNr(resizeHandleLocation);
		TransformNR loc = TransformFactory.affineToNr(location);
		return rsz.times(wp.times(loc));
	}
	public TransformNR getCurrentInReferenceFrame() {
		TransformNR wp =  manipulator.getFrameOfReference().copy();
		return wp.inverse().times(getCurrentInGlobalFrame());
	}
	
	public TransformNR getCurrent() {
		
		return new TransformNR(
				resizeHandleLocation.getTx()+location.getTx(),
				resizeHandleLocation.getTy()+location.getTy(),
				resizeHandleLocation.getTz()+location.getTz()	
				);
	}

	public TransformNR screenToWorld(double screenW, double screenH, double zoom, double mouseX, double mouseY) {
		double wHalf = screenW / 2;
		double hHalf = screenH / 2;
		double scalex = calculatePixelToMmScale(screenW, screenH, -zoom);

		// Convert mouse coordinates to centered coordinates
		double xComp = mouseX - wHalf;
		double yComp = mouseY - hHalf;

		// Calculate angles
		double angleOfXpoint = Math.atan2(xComp * scalex, -zoom);
		double angleOfYPoint = Math.atan2(yComp * scalex, -zoom);

		// Calculate world X and Y
		double worldX = Math.tan(angleOfXpoint) * (-zoom);
		double worldY = Math.tan(angleOfYPoint) * (-zoom);

		// Create a TransformNR for the point in camera space
		TransformNR pointInCameraSpace = new TransformNR(worldX, worldY, 0, new RotationNR());

		// Get the camera frame and apply inverse rotation
		TransformNR cf = engine.getFlyingCamera().getCamerFrame().times(new TransformNR(RotationNR.getRotationZ(180)));

		// Transform the point from camera space to world space
		TransformNR worldPoint = cf.times(pointInCameraSpace);

		return worldPoint;
	}

	public double calculateEffectiveFov(double screenW, double screenH) {
		double aspectRatio = screenW / screenH;
		double verticalFov = Math.toRadians(camera.getFieldOfView());	

		if (aspectRatio >= 1) {
			// Landscape or square orientation
			return 2 * Math.atan(Math.tan(verticalFov / 2) * aspectRatio);
		} else {
			// Portrait orientation
			return verticalFov;
		}
	}

	public double calculatePixelToMmScale(double screenW, double screenH, double zoom) {
		double effectiveFov = calculateEffectiveFov(screenW, screenH);
		double distance = zoom; // Assuming -zoom is stored in camera's Z translation

		if (screenW >= screenH) {
			// Landscape or square orientation
			double realWorldWidth = 2 * distance * Math.tan(effectiveFov / 2);
			return realWorldWidth / screenW;
		} else {
			// Portrait orientation
			double realWorldHeight = 2 * distance * Math.tan(effectiveFov / 2);
			return realWorldHeight / screenH;
		}
	}

	public void threeDTarget(double screenW, double screenH, double zoom, TransformNR target,TransformNR cf) {
		cf=manipulator.getFrameOfReference().inverse().times( cf);

		//System.out.println(cf.toSimpleString());
		// Calculate the vector from camera to target
		double x = target.getX() - cf.getX();
		double y = target.getY() - cf.getY();
		double z = target.getZ() - cf.getZ();

		// Calculate the distance between camera and target
		double distance = Math.sqrt(x*x + y*y + z*z);

		// Define a base scale and distance
		double baseScale = 0.75;
		double baseDistance = 1000.0;

		// Calculate the scale factor
		double scaleFactor = ((distance / baseDistance) * baseScale);

		// Clamp the scale factor to a reasonable range
		scaleFactor = Math.max(0.001, Math.min(90.0, scaleFactor));

		setScale(scaleFactor);
		
		//System.out.println("Point From Cam distaance "+vect+" scale "+scale);
		//System.out.println("");
		BowlerStudio.runLater(() -> {
			scaleTF.setX(getScale());
			scaleTF.setY(getScale());
			scaleTF.setZ(getScale());
			//TransformFactory.nrToAffine(pureRot ,cameraOrent);
			TransformFactory.nrToAffine(target.setRotation(new RotationNR()), location);
		});

		//hover.setText(name +" "+getCurrentInReferenceFrame()) ;
	}

	private void setVisible(boolean b) {
		mesh.setVisible(b);
	}

	private double getHeight() {
		// TODO Auto-generated method stub
		return getSize();
	}

	private double getWidth() {
		// TODO Auto-generated method stub
		return getSize();
	}

	public MeshView getMesh() {
		return mesh;
	}

	public void setMesh(MeshView mesh) {
		this.mesh = mesh;
	}

	public static double getSize() {
		return size;
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}
	@Override 
	public String toString() {
		return name;
	}

}
