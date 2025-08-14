package com.commonwealthrobotics.rotate;

import java.util.List;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.MainController;
import com.commonwealthrobotics.RulerManager;
import com.commonwealthrobotics.controls.ControlSprites;
import com.commonwealthrobotics.controls.Quadrent;
import com.commonwealthrobotics.controls.SelectionSession;
import com.commonwealthrobotics.controls.SpriteDisplayMode;
import com.commonwealthrobotics.numbers.TextFieldDimention;
import com.commonwealthrobotics.numbers.ThreedNumber;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
//import com.neuronrobotics.bowlerstudio.scripting.cadoodle.MoveCenter;
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
	//private List<String> selectedCSG;
	private boolean startAngleFound;
	public ThreedNumber text;
	private boolean selected = false;
	private Affine viewRotation;
	private SelectionSession controlSprites;
	private ActiveProject ap;
	private boolean moveLock=false;
	private IOnRotateDone done;

	public RotationHandle(EulerAxis ax, Affine translate, Affine vr,
			RotationSessionManager rotationSessionManager, ActiveProject ap, 
			SelectionSession cs, Affine workplaneOffset,RulerManager ruler,IOnRotateDone done) {
		this.axis = ax;
		this.viewRotation = vr;
		this.ap = ap;
	
		this.controlSprites = cs;
		this.done = done;
		
		Runnable onSelect = ()->{
			double mostRecentValue = text.getMostRecentValue();

			com.neuronrobotics.sdk.common.Log.error("Number entered! = "+mostRecentValue);
			setSweepAngle(mostRecentValue);
			flagSaveChange=true;
			runSaveAndReset();
		};
		text=new ThreedNumber(translate,  workplaneOffset, onSelect, TextFieldDimention.None, ruler);
		text.get();
		text.hide();
		handle.setImage(rotateImage);
		controlCircle.setImage(fullcircleImage);
		controlCircle.setVisible(false);
		handle.addEventFilter(MouseEvent.MOUSE_ENTERED, ev -> {
			if(!moveLock)
				controlCircle.setVisible(true);

			handle.setImage(selectedImage);
			com.neuronrobotics.sdk.common.Log.error("Entered " + axis);
		});
		handle.addEventFilter(MouseEvent.MOUSE_EXITED, ev -> {
			handle.setImage(rotateImage);
			if (!rotationStarted)
				controlCircle.setVisible(false);
		});
		EventHandler<? super MouseEvent> eventFilter = ev -> {
			selected=true;
			rotationStarted = true;
			startAngleFound = false;
			com.neuronrobotics.sdk.common.Log.error("Handle clicked");
			rotationSessionManager.initialize(cs.moveLock());
			flagSaveChange = false;
			controlSprites.setMode(SpriteDisplayMode.Rotating);
			if(!moveLock)controlCircle.setVisible(true);
			arc.setVisible(true);
			handle.setVisible(true);
			text.setValue(0);
			text.show();
			ev.consume();
		};
		handle.addEventFilter(MouseEvent.MOUSE_PRESSED, eventFilter);
		controlCircle.addEventFilter(MouseEvent.MOUSE_PRESSED, eventFilter);
		controlCircle.setPickOnBounds(true);
		EventHandler<? super MouseEvent> released = event -> {
			if (!flagSaveChange)
				return;
			runSaveAndReset();
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
				// com.neuronrobotics.sdk.common.Log.error("Angle Is "+current);
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
				text.setValue(sweepAngle);
				
				setSweepAngle(current);
			}
		};
		handle.addEventFilter(MouseEvent.MOUSE_RELEASED, released);
		controlCircle.addEventFilter(MouseEvent.MOUSE_RELEASED, released);
		controlCircle.addEventFilter(MouseEvent.MOUSE_DRAGGED, dragged);
		arc.setFill(new Color(0.0, 0, 1, 0.5));

		arc.getTransforms().addAll(translate,workplaneOffset, controlPin, arcPlanerOffset);
		handle.getTransforms().addAll(translate, workplaneOffset,controlPin, handlePlanarOffset);
		controlCircle.getTransforms().addAll(translate, workplaneOffset,controlPin, circelPlanerOffset);
		controlCircle.setOpacity(0.5);
	}

	private void runSaveAndReset() {
		controlSprites.setMode(SpriteDisplayMode.Default);
		controlCircle.setVisible(false);
		arc.setVisible(false);
		selected=false;
		TransformNR toUpdate = rotAtCenter.copy();
		BowlerStudio.runLater(() -> {
			TransformFactory.nrToAffine(new TransformNR(), viewRotation);
		});
		if(!moveLock) {
			done.toUpdate(toUpdate);
		}
	}

	private void setSweepAngle(double c) {

		// Set the type to ROUND to create a wedge shape
		arc.setType(ArcType.ROUND);

		// divide the angle in 2 and aply it twice avaoids Euler singularities
		TransformNR update = new TransformNR(new RotationNR(axis, -c / 2));
		TransformNR pureRot = update.times(update);
		TransformNR wp = ap.get().getWorkplane();
		TransformNR center = wp.times(new TransformNR(bounds.getCenter().x, bounds.getCenter().y, bounds.getCenter().z));
		rotAtCenter = center.times(pureRot.times(center.inverse()));
		BowlerStudio.runLater(() -> {
			TransformFactory.nrToAffine(rotAtCenter, viewRotation);
		});
	}

	private double getAngle(MouseEvent event) {
		PickResult result = event.getPickResult();
		// com.neuronrobotics.sdk.common.Log.error("Draggin on circle");
		// com.neuronrobotics.sdk.common.Log.error(result);
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
		// com.neuronrobotics.sdk.common.Log.error("Unit Location "+angle);
		if (axis == EulerAxis.tilt)
			angle = -angle;
		return angle;

	}

	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z, List<String> selectedCSG, Bounds b, TransformNR cf) {
		this.bounds = b;
		Vector3d center = bounds.getCenter();
		Vector3d min = bounds.getMin();
		Vector3d max = bounds.getMax();
		double numberOffset = 20;

		this.az = az;
		//this.selectedCSG = selectedCSG;
		double totx = b.getMax().x - b.getMin().x;
		double toty = b.getMax().y - b.getMin().y;
		double totz = b.getMax().z - b.getMin().z;
		double rA = 1, rB = 1;
		double pinLocx = 0, pinLocy = 0, pinLocz = 0;
		TransformNR positionPin = new TransformNR();
		Quadrent q = Quadrent.getQuad(-az);
		rotationStarted = false;
		// com.neuronrobotics.sdk.common.Log.error("Az camera in Rotation Handle "+az);
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
		
		positionPin=input4.times( input.times( new TransformNR(0,0,0)));	
		text.threeDTarget(screenW, screenH, zoom, positionPin, cf);
		BowlerStudio.runLater(() -> {
			TransformFactory.nrToAffine(input4, arcPlanerOffset);
			TransformFactory.nrToAffine(input3, handlePlanarOffset);
			TransformFactory.nrToAffine(input2, circelPlanerOffset);
			TransformFactory.nrToAffine(input, controlPin);
		});
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isFocused() {
		return text.isFocused();
	}

	public void setLock(boolean moveLock) {
		this.moveLock = moveLock;
	}
}
