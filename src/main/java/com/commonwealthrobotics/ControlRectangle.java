package com.commonwealthrobotics;

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
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;

public class ControlRectangle {

	private BowlerStudio3dEngine engine;
	private PerspectiveCamera camera;
	private static final double size=20;
	private MeshView mesh;
	private Affine location = new Affine();
	private Affine cameraOrent = new Affine();
	private Scale scaleTF = new Scale();
	private double scale;
	Manipulation manipulator;
	private Affine scaleAffine = new Affine();
	private String name;
	/**
	 * Creates a new instance of Rectangle with the given size and fill.
	 * @param name 
	 * @param vector3d 
	 * 
	 * @param width  width of the rectangle
	 * @param height height of the rectangle
	 * @param fill   determines how to fill the interior of the rectangle
	 */
	public ControlRectangle(String name, BowlerStudio3dEngine engine,Affine move, Vector3d vector3d) {
		this.name = name;
		manipulator=new Manipulation(scaleAffine, vector3d, new TransformNR());
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
		mesh.getTransforms().add(scaleAffine);
		mesh.getTransforms().add(location);
		mesh.getTransforms().add(cameraOrent);
		mesh.getTransforms().add(scaleTF);
		mesh.addEventFilter(MouseEvent.ANY, manipulator.getMouseEvents());

	}
	
	public TransformNR getCurrent() {
		return new TransformNR(
				scaleAffine.getTx()+location.getTx(),
				scaleAffine.getTy()+location.getTy(),
				scaleAffine.getTz()+location.getTz()	
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

	public void threeDTarget(double screenW, double screenH, double zoom, TransformNR target) {
		TransformNR cf = engine.getFlyingCamera().getCamerFrame();//.times(new TransformNR(RotationNR.getRotationZ(180)));
		TransformNR pureRot = new TransformNR(cf.getRotation());
		
		TransformNR zTf = new TransformNR(0,0,-zoom);
		TransformNR camerFrame2 = engine.getFlyingCamera().getCamerFrame();
		//TransformNR cameraTranslation = camerFrame2.copy().setRotation(new RotationNR());
		RotationNR camRot=camerFrame2.getRotation();
		TransformNR cfimageFrame = 
				new TransformNR(camRot)
						.times(zTf);
		TransformNR inverse = cfimageFrame;
		double x = inverse.getX()+target.getX();
		double y = inverse.getY()+target.getY();
		double z = inverse.getZ()+target.getZ();
		double vect = Math.max(1, Math.sqrt(Math.pow(x, 2)+Math.pow(y, 2)+Math.pow(z, 2)));
		vect = Math.min(9000, vect);
		setScale(Math.max(0.1, ((vect/1000.0))*0.75));
		
		//System.out.println("Point From Cam distaance "+vect+" scale "+scale);
		//System.out.println("");
		BowlerStudio.runLater(() -> {
			scaleTF.setX(getScale());
			scaleTF.setY(getScale());
			scaleTF.setZ(getScale());
			TransformFactory.nrToAffine(pureRot ,cameraOrent);
			TransformFactory.nrToAffine(target.setRotation(new RotationNR()), location);
		});


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
