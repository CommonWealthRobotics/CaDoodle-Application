package com.commonwealthrobotics;

import java.util.List;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.MoveCenter;
import com.neuronrobotics.sdk.addons.kinematics.math.EulerAxis;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.transform.Affine;

public class RotationHandle {
	private static final Image selectedImage = new Image(
			MainController.class.getResourceAsStream("rotateSelected.png"));
	private static final Image fullcircleImage = new Image(MainController.class.getResourceAsStream("fullCircle.png"));
	private static final Image rotateImage = new Image(MainController.class.getResourceAsStream("rotate.png"));
	private EulerAxis axis;
	ImageView handle = new ImageView();
	ImageView controlCircle = new ImageView();
	private Affine controlPin = new Affine();
	private Affine circelPlanerOffset = new Affine();
	private Affine handlePlanarOffset = new Affine();
	private Affine arcPlanerOffset = new Affine();

	private boolean rotationStarted = false;
	private double StartAngle = 0;
	private double current = 0;
	private Bounds bounds;
	Arc arc = new Arc();
	private double az;
	private TransformNR rotAtCenter;
	private boolean flagSaveChange = false;
	private List<String> selectedCSG;
	private boolean startAngleFound;

	public RotationHandle(EulerAxis axis, Affine translate, Affine viewRotation,
			RotationSessionManager rotationSessionManager, CaDoodleFile cadoodle, ControlSprites controlSprites) {
		this.axis = axis;
		handle.setImage(rotateImage);
		controlCircle.setImage(fullcircleImage);
		controlCircle.setVisible(false);
		handle.addEventFilter(MouseEvent.MOUSE_ENTERED, ev -> {
			controlCircle.setVisible(true);

			handle.setImage(selectedImage);
			System.out.println("Entered " + axis);
		});
		handle.addEventFilter(MouseEvent.MOUSE_EXITED, ev -> {
			handle.setImage(rotateImage);
			if (!rotationStarted)
				controlCircle.setVisible(false);
		});
		EventHandler<? super MouseEvent> eventFilter = ev -> {
			rotationStarted = true;
			startAngleFound = false;
			System.out.println("Handle clicked");
			rotationSessionManager.initialize();

			flagSaveChange = false;
			controlSprites.setMode(SpriteDisplayMode.Rotating);
			controlCircle.setVisible(true);
			arc.setVisible(true);
			handle.setVisible(true);
			ev.consume();
		};
		handle.addEventFilter(MouseEvent.MOUSE_PRESSED, eventFilter);
		controlCircle.addEventFilter(MouseEvent.MOUSE_PRESSED, eventFilter);
		controlCircle.setPickOnBounds(true);
		EventHandler<? super MouseEvent> released = event -> {
			controlSprites.setMode(SpriteDisplayMode.Default);
			controlCircle.setVisible(false);
			arc.setVisible(false);
			if (!flagSaveChange)
				return;
			TransformNR toUpdate = rotAtCenter.copy();
			BowlerStudio.runLater(() -> {
				TransformFactory.nrToAffine(new TransformNR(), viewRotation);
			});
			cadoodle.addOpperation(new MoveCenter().setLocation(toUpdate).setNames(selectedCSG));
		};

		EventHandler<? super MouseEvent> dragged = event -> {
			if (event.getPickResult().getIntersectedNode() != handle) {
				if (startAngleFound == false) {
					startAngleFound = true;
					StartAngle = 22.5 * Math.round(getAngle(event) / 22.5);
				}

				current = StartAngle - getAngle(event);
				while (current > 180)
					current -= 360;
				while (current < -180)
					current += 360;
				// System.out.println("Angle Is "+current);
				if (Math.abs(current) > 0.001) {
					arc.setVisible(true);
					flagSaveChange = true;
				} else
					flagSaveChange = false;
				// Calculate the sweep angle
				double sweepAngle = -getAngle(event) + StartAngle;
				if (sweepAngle < -180) {
					sweepAngle += 360;
				}
				if (sweepAngle > 180) {
					sweepAngle -= 360;
				}
				if (axis == EulerAxis.tilt)
					sweepAngle = -sweepAngle;
				// Update the Arc properties

				arc.setStartAngle(0);
				arc.setLength(sweepAngle);

				// Set the type to ROUND to create a wedge shape
				arc.setType(ArcType.ROUND);

				// divide the angle in 2 and aply it twice avaoids Euler singularities
				TransformNR update = new TransformNR(new RotationNR(axis, -current / 2));
				TransformNR pureRot = update.times(update);
				TransformNR center = new TransformNR(bounds.getCenter().x, bounds.getCenter().y, bounds.getCenter().z);
				rotAtCenter = center.times(pureRot.times(center.inverse()));
				BowlerStudio.runLater(() -> {
					TransformFactory.nrToAffine(rotAtCenter, viewRotation);
				});
			}
		};
		handle.addEventFilter(MouseEvent.MOUSE_RELEASED, released);
		controlCircle.addEventFilter(MouseEvent.MOUSE_RELEASED, released);
		handle.addEventFilter(MouseEvent.MOUSE_DRAGGED, dragged);
		controlCircle.addEventFilter(MouseEvent.MOUSE_DRAGGED, dragged);
		arc.setFill(new Color(0.0, 0, 1, 0.5));

		arc.getTransforms().addAll(translate, controlPin, arcPlanerOffset);
		handle.getTransforms().addAll(translate, controlPin, handlePlanarOffset);
		controlCircle.getTransforms().addAll(translate, controlPin, circelPlanerOffset);
		controlCircle.setOpacity(0.5);
	}

	private double getAngle(MouseEvent event) {
		PickResult result = event.getPickResult();
		// System.out.println("Draggin on circle");
		// System.out.println(result);
		double h = controlCircle.getFitHeight();
		double w = controlCircle.getFitWidth();
		double x = result.getIntersectedPoint().getX();
		double y = result.getIntersectedPoint().getY();
		double unitX = (x / h - 0.5) * 2;
		double unitY = (y / w - 0.5) * 2;
		double unitVect = Math.sqrt(Math.pow(unitX, 2) + Math.pow(unitY, 2));

		double angle = Math.toDegrees(Math.atan2(unitY, unitX));
		if (unitVect < 0.85) {
			angle = 22.5 * Math.round(angle / 22.5);
		}
		// System.out.println("Unit Location "+angle);
		if (axis == EulerAxis.tilt)
			angle = -angle;
		return angle;

	}

	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z, List<String> selectedCSG, Bounds b) {
		this.az = az;
		this.selectedCSG = selectedCSG;
		this.bounds = b;
		double totx = b.getMax().x - b.getMin().x;
		double toty = b.getMax().y - b.getMin().y;
		double totz = b.getMax().z - b.getMin().z;
		double rA = 1, rB = 1;
		double pinLocx = 0, pinLocy = 0, pinLocz = 0;
		Vector3d center = b.getCenter();
		Vector3d min = b.getMin();
		Vector3d max = b.getMax();
		Quadrent q = Quadrent.getQuad(-az);
		rotationStarted = false;
		// System.out.println("Az camera in Rotation Handle "+az);
		RotationNR axisOrent = new RotationNR();
		switch (axis) {
		case azimuth:
			rA = totx;
			rB = toty;
			pinLocx = center.x;
			pinLocy = center.y;
			pinLocz = min.z;
			axisOrent = RotationNR.getRotationZ(Quadrent.QuadrentToAngle(q) + 180);
			break;
		case elevation:
			rA = totx;
			rB = totz;
			pinLocx = center.x;
			pinLocy = (az < 90 && az > -90) ? min.y : max.y;
			pinLocz = center.z;
			axisOrent = new RotationNR(-90, 0, 0);
			break;
		case tilt:
			rA = totz;
			rB = toty;
			pinLocx = (az > 0 && az < 180) ? min.x : max.x;
			pinLocy = center.y;
			pinLocz = center.z;
			axisOrent = new RotationNR(-90, 90, 0);
			break;
		}
		double circleDiameter = Math.sqrt(Math.pow(rA, 2) + Math.pow(rB, 2));
		double radius = circleDiameter / 2;

		TransformNR pureRot = new TransformNR(axisOrent);
		TransformNR t = new TransformNR(pinLocx, pinLocy, pinLocz);
		TransformNR input = t;

		controlCircle.setFitHeight(circleDiameter);
		controlCircle.setFitWidth(circleDiameter);
		// controlCircle.setTranslateX(-circleDiameter/2);
		// controlCircle.setTranslateY(-circleDiameter/2);
		handle.setFitWidth(circleDiameter / 4);
		handle.setFitHeight(circleDiameter / 8);
		// handle.setTranslateX(-circleDiameter/8);
		// handle.setTranslateY(-circleDiameter/2-circleDiameter/8);
		TransformNR axisAngle = pureRot;
		if (axis == EulerAxis.azimuth)
			axisAngle = new TransformNR();
		TransformNR input4 = axisAngle.times(new TransformNR(0, 0, 0));

		TransformNR input2 = pureRot.times(new TransformNR(-circleDiameter / 2, -circleDiameter / 2, 0));
		TransformNR input3 = pureRot
				.times(new TransformNR(-circleDiameter / 8, -circleDiameter / 2 - circleDiameter / 8, 0));
		arc.setRadiusX(radius / 2);
		arc.setRadiusY(radius / 2);
		BowlerStudio.runLater(() -> {
			TransformFactory.nrToAffine(input4, arcPlanerOffset);
			TransformFactory.nrToAffine(input3, handlePlanarOffset);
			TransformFactory.nrToAffine(input2, circelPlanerOffset);
			TransformFactory.nrToAffine(input, controlPin);
		});
	}
}
