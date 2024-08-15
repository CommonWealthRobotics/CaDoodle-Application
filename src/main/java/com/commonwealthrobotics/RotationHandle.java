package com.commonwealthrobotics;

import java.util.List;

import com.commonwealthrobotics.SelectionSession.Quadrent;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.sdk.addons.kinematics.math.EulerAxis;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.Affine;

public class RotationHandle {
	private static final Image selectedImage = new Image(MainController.class.getResourceAsStream("rotateSelected.png"));
	private static final Image fullcircleImage = new Image(MainController.class.getResourceAsStream("fullCircle.png"));
	private static final Image rotateImage = new Image(MainController.class.getResourceAsStream("rotate.png"));
	private EulerAxis axis;
	ImageView handle = new ImageView();
	ImageView controlCircle = new ImageView();
	private Affine controlPin = new Affine();
	private Affine circelPlanerOffset = new Affine();
	private Affine handlePlanarOffset= new Affine();
	public enum Quadrent {
		first, second, third, fourth
	}

	Quadrent getQuad(double angle) {
		if (angle > 45 && angle < 135)
			return Quadrent.first;
		if (angle > 135 || angle < (-135))
			return Quadrent.second;
		if (angle > -135 && angle <= -45)
			return Quadrent.third;
		if (angle > -45 && angle < 45)
			return Quadrent.fourth;
		throw new RuntimeException("Impossible nummber! " + angle);
	}

	double QuadrentToAngle(Quadrent q) {
		switch (q) {
		case first:
			return 90;
		case second:
			return 180;
		case third:
			return -90;
		case fourth:
		default:
			return 0;
		}
	}

	public RotationHandle(EulerAxis axis, Affine translate) {
		this.axis = axis;
		handle.setImage(rotateImage);
		controlCircle.setImage(fullcircleImage);
		controlCircle.setVisible(false);
		handle.addEventFilter(MouseEvent.MOUSE_ENTERED, ev->{
			controlCircle.setVisible(true);

			handle.setImage(selectedImage);
			System.out.println("Entered "+axis);
		});
		handle.addEventFilter(MouseEvent.MOUSE_EXITED, ev->{
			handle.setImage(rotateImage);
			controlCircle.setVisible(false);
		});
		
		handle.getTransforms().addAll(translate,controlPin,handlePlanarOffset);
		controlCircle.getTransforms().addAll(translate,controlPin,circelPlanerOffset);
		
	}
	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z,List<CSG> selectedCSG, Bounds b) {
		double totx = b.getMax().x-b.getMin().x;
		double toty = b.getMax().y-b.getMin().y;
		double totz = b.getMax().z-b.getMin().z;
		double rA=1,rB=1;
		double pinLocx=0,pinLocy=0,pinLocz=0;
		Vector3d center = b.getCenter();
		Vector3d min = b.getMin();
		Vector3d max = b.getMax();
		Quadrent q= getQuad(-az);
		
		//System.out.println("Az camera in Rotation Handle "+az);
		RotationNR axisOrent = new RotationNR();
		switch(axis) {
		case azimuth:
			rA=totx;
			rB=toty;
			pinLocx = center.x;
			pinLocy = center.y;
			pinLocz=min.z;
			axisOrent = RotationNR.getRotationZ(QuadrentToAngle(q)+180);
			break;
		case elevation:
			rA=totx;
			rB=totz;
			pinLocx = center.x;
			pinLocy = (az<90&&az>-90)?min.y:max.y;
			pinLocz=  center.z;
			axisOrent = new RotationNR(-90,0,0);
			break;
		case tilt:
			rA=totz;
			rB=toty;
			pinLocx = (az>0&&az<180)?min.x:max.x;
			pinLocy = center.y;
			pinLocz=center.z;
			axisOrent = new RotationNR(-90,90,0);
			break;
		}
		double circleDiameter=Math.sqrt(Math.pow(rA, 2)+Math.pow(rB, 2));
		TransformNR pureRot = new TransformNR(axisOrent);
		TransformNR t = new TransformNR(pinLocx,pinLocy,pinLocz);
		TransformNR input = t;
		
		controlCircle.setFitHeight(circleDiameter);
		controlCircle.setFitWidth(circleDiameter);
		//controlCircle.setTranslateX(-circleDiameter/2);
		//controlCircle.setTranslateY(-circleDiameter/2);
		handle.setFitWidth(circleDiameter/4);
		handle.setFitHeight(circleDiameter/8);
		//handle.setTranslateX(-circleDiameter/8);
		//handle.setTranslateY(-circleDiameter/2-circleDiameter/8);
		TransformNR input2 = pureRot.times(new TransformNR(-circleDiameter/2,-circleDiameter/2,0));
		TransformNR input3 = pureRot.times(new TransformNR(-circleDiameter/8,-circleDiameter/2-circleDiameter/8,0));

		BowlerStudio.runLater(() -> {
			TransformFactory.nrToAffine(input3,handlePlanarOffset);
			TransformFactory.nrToAffine(input2,circelPlanerOffset);
			TransformFactory.nrToAffine(input,controlPin);
		});
	}
}
