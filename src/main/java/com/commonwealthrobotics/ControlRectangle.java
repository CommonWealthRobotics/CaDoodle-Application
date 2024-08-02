package com.commonwealthrobotics;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Transform;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;

public class ControlRectangle extends Rectangle {

	private BowlerStudio3dEngine engine;
	private PerspectiveCamera camera;

	/**
	 * Creates a new instance of Rectangle with the given size and fill.
	 * 
	 * @param width  width of the rectangle
	 * @param height height of the rectangle
	 * @param fill   determines how to fill the interior of the rectangle
	 */
	public ControlRectangle(BowlerStudio3dEngine engine) {
		super(12.0, 12.0, Color.WHITE);
		this.engine = engine;
		setStroke(Color.BLACK);
		setStrokeWidth(3);
		camera = engine.getFlyingCamera().getCamera();
		setOnMouseEntered(event -> {
			setFill(Color.RED);
		});
		setOnMouseExited(event -> {
			setFill(Color.WHITE);
		});
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
		TransformNR zTf = new TransformNR(0,0,zoom);
		TransformNR cf = 
						engine.getFlyingCamera().getCamerFrame()
						.times(new TransformNR(RotationNR.getRotationZ(180)).times(zTf));
		
		System.out.println("Camera fraame: "+cf.toSimpleString());
		//target= cf.inverse().times(target);
//		Point3D worldPoint=new Point3D(target.getX(), target.getY(), target.getZ());
//		   // Transform the point to scene coordinates
//	    Point3D scenePoint = engine.getSubScene().getRoot().localToScene(worldPoint);

	    // Get the projection transform from the camera
	    Affine input = new Affine(camera.getLocalToSceneTransform());
		TransformNR projection = TransformFactory.affineToNr(input);
		System.out.println("Projection frame: "+projection.toSimpleString());

	    // Project the point
	    TransformNR projectedPoint = projection.times(cf.inverse().times(target));
		double scalex = 1;//1/calculatePixelToMmScale( screenW,  screenH,-zoom);

	    // Perform viewport transformation
	    double d = (projectedPoint.getX() + 1)*scalex;
	    double e = (-projectedPoint.getY() + 1)*scalex;

		double xValue = e + (screenW / 2)+getWidth()/2;
		double yValue = d + (screenH / 2)+getHeight()/2;
	    
		if (xValue > screenW || xValue < 0 || yValue > screenH || yValue < 0) {
			//BowlerStudio.runLater(() -> {
				setVisible(false);
			//});
			return;
		} else
			//BowlerStudio.runLater(() -> {
				setVisible(true);
				setLayoutX(Math.max(0, Math.min(screenW, xValue)));
				setLayoutY(Math.max(0, Math.min(screenH, yValue)));
			//});

	}
}
