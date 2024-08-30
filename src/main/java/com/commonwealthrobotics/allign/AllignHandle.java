package com.commonwealthrobotics.allign;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Allignment;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.*;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;

public class AllignHandle {
	private double scale;

	public Allignment self;
	private MeshView mesh;
	private Affine location = new Affine();
	private Affine cameraOrent = new Affine();
	private Scale scaleTF = new Scale();
	private Affine move;
	private Affine workplaneOffset;
	private Affine allignLoc = new Affine();

	public AllignHandle(Allignment set, Affine move, Affine workplaneOffset) {
		self = set;
		this.move = move;
		this.workplaneOffset = workplaneOffset;
	}

	public MeshView getHandle() {
		if (mesh == null) {
			double pointerH = 5;
			double rad = 5;
			CSG nub = new Sphere(5).toCSG().toZMin().movez(pointerH);
			CSG pointer = new Cylinder(1, pointerH + rad).toCSG();

			CSG h = pointer.union(nub);
			mesh = h.getMesh();
			PhongMaterial material = new PhongMaterial();
			material.setDiffuseColor(Color.BLACK);
			mesh.setCullFace(CullFace.NONE);
			mesh.setMaterial(material);
			mesh.addEventFilter(MouseEvent.MOUSE_EXITED, event -> {
				material.setDiffuseColor(Color.BLACK);
			});
			mesh.addEventFilter(MouseEvent.MOUSE_ENTERED, event -> {
				material.setDiffuseColor(new Color(1, 0, 0, 1));

			});
			mesh.getTransforms().add(move);
			mesh.getTransforms().add(allignLoc);
			mesh.getTransforms().add(workplaneOffset);
			mesh.getTransforms().add(location);
			mesh.getTransforms().add(cameraOrent);
			mesh.getTransforms().add(scaleTF);
		}

		return mesh;
	}

	public void threeDTarget(double screenW, double screenH, double zoom, TransformNR target, TransformNR cf) {
		// TransformNR pureRot = new TransformNR(cf.getRotation());

		// System.out.println(cf.toSimpleString());
		// Calculate the vector from camera to target
		double x = target.getX() - cf.getX();
		double y = target.getY() - cf.getY();
		double z = target.getZ() - cf.getZ();

		// Calculate the distance between camera and target
		double distance = Math.sqrt(x * x + y * y + z * z);

		// Define a base scale and distance
		double baseScale = 0.75;
		double baseDistance = 1000.0;

		// Calculate the scale factor
		double scaleFactor = ((distance / baseDistance) * baseScale);

		// Clamp the scale factor to a reasonable range
		scaleFactor = Math.max(0.001, Math.min(90.0, scaleFactor));

		setScale(scaleFactor);

		// System.out.println("Point From Cam distaance "+vect+" scale "+scale);
		// System.out.println("");
		BowlerStudio.runLater(() -> {
			scaleTF.setX(getScale());
			scaleTF.setY(getScale());
			scaleTF.setZ(getScale());
			// TransformFactory.nrToAffine(pureRot ,cameraOrent);
			TransformFactory.nrToAffine(target.setRotation(new RotationNR()), location);
		});

		// hover.setText(name +" "+getCurrentInReferenceFrame()) ;
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}
}
