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
		double heightBoundOffset=0;
		screenH-=heightBoundOffset;
		double scalex = 1/calculatePixelToMmScale( screenW,  screenH,-zoom);
		double zoomInPix = zoom*scalex;
		System.out.println(" Zoom "+zoom+" zoom in pix "+zoomInPix);
		
		TransformNR zTf = new TransformNR(0,0,-zoom);
		TransformNR camerFrame2 = engine.getFlyingCamera().getCamerFrame();
		TransformNR cameraTranslation = camerFrame2.copy().setRotation(new RotationNR());
		RotationNR camRot=camerFrame2.getRotation();
		TransformNR cf = 
				new TransformNR(camRot)
						.times(zTf);
		
		TransformNR inverse = cf.inverse();
		System.out.println("Camera fraame: "+inverse.toSimpleString());

	    // Get the projection transform from the camera
//	    Affine input = new Affine(camera.getLocalToSceneTransform());
//		TransformNR projection = TransformFactory.affineToNr(input)
//				.times(new TransformNR(RotationNR.getRotationY(0)))
//				.times(new TransformNR(RotationNR.getRotationZ(0)))
//				.times(new TransformNR(RotationNR.getRotationX(0)));
//		System.out.println("Projection frame: "+projection.toSimpleString());

	    // Project the point
		System.out.println("Camera PureTrans: "+cameraTranslation.toSimpleString());
	    TransformNR times = target.inverse().times(cameraTranslation);
	    
		TransformNR simpleProjection = inverse.times(times);
	    
	
		
		double pointAngle = Math.toDegrees(Math.atan2(simpleProjection.getY(), simpleProjection.getX()));
		TransformNR t = new TransformNR(new RotationNR(0,-pointAngle,0));
		System.out.println("Rotaation Angle : "+pointAngle);
		System.out.println("Projection point: "+simpleProjection.toSimpleString());
		TransformNR projectedPoint=t.times(simpleProjection);
		System.out.println("Rotated    point: "+projectedPoint.toSimpleString());

	    // Perform viewport transformation
//	    double scaledVectorRotated = (-projectedPoint.getX() )*scalex;
//	    TransformNR backRotatedScaled=t.inverse().times(new TransformNR(scaledVectorRotated,0,0));
//	    double d= backRotatedScaled.getX();
//	    double e = backRotatedScaled.getY();
	    double d= simpleProjection.getX()*scalex;;
	    double e = simpleProjection.getY()*scalex;;
		double xValue = d + (screenW / 2)-getWidth()/2;
		double yValue = e + (screenH / 2)-getHeight()/2 +heightBoundOffset/2;
	    
		if (xValue > screenW || xValue < 0 || yValue > screenH || yValue < 0) {
			//BowlerStudio.runLater(() -> {
				setVisible(false);
			//});
			return;
		} else
			//BowlerStudio.runLater(() -> {
				setVisible(true);
				setLayoutX(Math.max(getWidth()/2+20, Math.min(screenW-getWidth()/2-20, xValue)));
				setLayoutY(Math.max(getWidth()/2+20, Math.min(screenH-getWidth()/2-20, yValue)));
			//});

	}
}
