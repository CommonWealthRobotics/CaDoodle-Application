package com.commonwealthrobotics;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Transform;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class ControlRectangle extends Rectangle {

    private BowlerStudio3dEngine engine;
	/**
     * Creates a new instance of Rectangle with the given size and fill.
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param fill determines how to fill the interior of the rectangle
     */
    public ControlRectangle(BowlerStudio3dEngine engine) {
    	super(12.0,12.0, Color.WHITE);
		this.engine = engine;
		setStroke(Color.BLACK);

    }
    public TransformNR screenToWorld(double screenW,double screenH, double zoom ,double mouseX, double mouseY, Vector3d planePoint, Vector3d planeNormal) {
        double verticalFOV = engine.getFlyingCamera().getCamera().getFieldOfView(); // in degrees
        //double smallerDimension = Math.min(screenW, screenH);
        double scalex = screenW / (2 * Math.tan(Math.toRadians(verticalFOV / 2)));
        double scaley = screenH / (2 * Math.tan(Math.toRadians(verticalFOV / 2)));

        double d = screenW / 2;
        double e = screenH / 2;
        double fuge = 1;
        
        // Get the current camera frame
        TransformNR cf = engine.getFlyingCamera().getCamerFrame().times(new TransformNR(RotationNR.getRotationZ(180)));
      //double xValue = -(getWidth()/2)+((tp.getX()/-zoom)*(scale*fuge))+ d;

        // Convert screen coordinates to world coordinates
        double worldX = ((mouseX - d) / (scalex * fuge)) * -zoom;
        double worldY = ((mouseY - e) / (scaley * fuge)) * -zoom;
        TransformNR worldXY = new TransformNR(worldX,worldY,0);
        worldXY=cf.times(worldXY);
        
//        TransformNR rotOnly= new TransformNR(cf.getRotation());
//        Transform csgtimes = TransformFactory.nrToCSG(rotOnly);
//        // Create a ray from the camera through the mouse point
//        Vector3d rayOrigin = new Vector3d(cf.getX(), cf.getY(), zoom);
//        Vector3d rayDirection = new Vector3d(worldX, worldY, -zoom).normalized();
//        rayDirection = csgtimes.transform(rayDirection);
//        
//        // Calculate intersection with the plane
//        double denom = rayDirection.dot(planeNormal);
//        if (Math.abs(denom) > 1e-6) { // Check if ray is not parallel to plane
//            double t = (planePoint.minus(rayOrigin)).dot(planeNormal) / denom;
//            if (t >= 0) { // Check if intersection is in front of camera
//            	rayDirection.times(t);
//                rayOrigin.add(rayDirection);
//                
//                // Create and return TransformNR
//                return new TransformNR(rayDirection.x, rayDirection.y, rayDirection.z, 
//                                       new RotationNR(0, 0, 0)); // Assuming no rotation
//            }
//        }
        
        // Return null if no intersection found
        return worldXY;
    }
	public void threeDTarget(double screenW,double screenH, double zoom , TransformNR target) {
		double bigFov = Math.toRadians(engine.getFlyingCamera().getCamera().getFieldOfView());
		double scalex;
		double scaley;	

		double wHalf= screenW / 2;
		double hHalf = screenH/2;
		double fovX; //= Math.atan2(screenW/2, -zoom);
		double fovY;//=Math.atan2(screenH/2, -zoom);;
		
		double zoomInPix=wHalf/Math.tan(bigFov/2);
		fovY = Math.toDegrees(Math.atan2(hHalf,zoomInPix));

		//if(screenW>screenH) {
			fovX=bigFov;
			scalex = screenW / (2* Math.tan(fovX/2 ));
		//}
		
		
		scaley = screenH / (2 * Math.tan(Math.toRadians(fovY) ));

		TransformNR cf = engine.getFlyingCamera().getCamerFrame().times(new TransformNR(RotationNR.getRotationZ(180)));

		TransformNR tp = cf.inverse().times(target);
		double fuge =1;
		double yFuge=fuge*1;
		
		double xValue = -(getWidth()/2)+((tp.getX()/-zoom)*(scalex*fuge))+wHalf;
		double yValue = -(getHeight()/2)+((tp.getY()/-zoom)* (scaley*yFuge))+ hHalf;
		System.out.println("Targeting "+target.toSimpleString());
		System.out.println("In Frame  "+screenToWorld(screenW, screenH,  zoom,
				xValue+(getWidth()/2),
				yValue+(getHeight()/2),
				new Vector3d(0, 0,0),
				new Vector3d(0, 0,1)
				).toSimpleString());
		System.out.println("Fov X "+Math.toDegrees(fovX)+" Fov Y"+(fovY*2) );
		System.out.println(cf.toSimpleString());
		System.out.println("\tScalex= " + scalex);
		if(xValue>screenW || xValue<0 || yValue>screenH||yValue<0) {
			BowlerStudio.runLater(()->{
				setVisible(false);
			});
			return;
		}else
			BowlerStudio.runLater(()->{
				setVisible(true);
				setLayoutX(Math.max(0, Math.min(screenW, xValue)));
				setLayoutY(Math.max(0, Math.min(screenH, yValue)));	
			});

	}
}
