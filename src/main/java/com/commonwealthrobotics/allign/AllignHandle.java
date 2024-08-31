package com.commonwealthrobotics.allign;

import java.util.List;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Allign;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Allignment;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.*;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
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

	private Vector3d orentation;

	private Allign opperation;

	private BowlerStudio3dEngine engine;

	private List<CSG> toAllign;

	private List<String> selected;

	private PhongMaterial material;

	private EventHandler<? super MouseEvent> entered;

	private EventHandler<? super MouseEvent> exited;
	private EventHandler<? super MouseEvent> onClickEvent;

	private Runnable onClick;

	public AllignHandle(Allignment set, Affine move, Affine workplaneOffset, Vector3d vector3d) {
		self = set;
		this.move = move;
		this.workplaneOffset = workplaneOffset;
		this.orentation = vector3d;
	}

	public MeshView getHandle() {
		if (mesh == null) {
			double pointerH = 20;
			double rad = 15;
			CSG nub = new Cylinder(rad, 1).toCSG().roty(90).toZMin().movez(pointerH);
			CSG pointer = new Cylinder(rad / 3, pointerH + rad).toCSG();

			CSG h = pointer.union(nub);
			mesh = h.getMesh();
			material = new PhongMaterial();

			mesh.setCullFace(CullFace.NONE);
			mesh.setMaterial(material);
			exited = event -> {
				material.setDiffuseColor(Color.BLACK);
			};
			entered = event -> {
				material.setDiffuseColor(new Color(1, 0, 0, 1));
				System.out.println("ENtered " + self + " " + orentation);
			};
			onClickEvent = event -> {
				onClick.run();
				System.out.println("Handle clicked ");
				material.setDiffuseColor(Color.GRAY);
				getHandle().removeEventFilter(MouseEvent.MOUSE_EXITED, exited);
				getHandle().removeEventFilter(MouseEvent.MOUSE_ENTERED, entered);
				getHandle().removeEventFilter(MouseEvent.MOUSE_CLICKED, onClickEvent);
				if(isXvector())
					opperation.x=self;
				if(isYvector())
					opperation.y=self;
				if(isZvector())
					opperation.z=self;

			};
			mesh.getTransforms().add(move);
			mesh.getTransforms().add(allignLoc);
			mesh.getTransforms().add(workplaneOffset);
			mesh.getTransforms().add(location);
			mesh.getTransforms().add(cameraOrent);
			mesh.getTransforms().add(scaleTF);
			mesh.setVisible(false);
			mesh.visibleProperty().addListener((observable, oldValue, newValue) -> {
				// new Exception("Mesh visibilitr changed "+newValue).printStackTrace();
			});
		}

		return mesh;
	}

	public void threeDTarget(double screenW, double screenH, double zoom, Bounds b, TransformNR cf) {

		double X = 0;
		double Y = 0;
		double Z = 0;
		boolean isX = isXvector();
		boolean isY = isYvector();
		boolean isZ = isZvector();
		switch (self) {
		case middle:
			if (isX) {
				X = b.getCenter().x;
				Y = b.getMin().y;
				Z = b.getMin().z;
			}
			if (isY) {
				X = b.getMax().x;
				Y = b.getCenter().y;
				Z = b.getMin().z;
			}
			if (isZ) {
				X = b.getMax().x;
				Y = b.getMax().y;
				Z = b.getCenter().z;
			}
			break;
		case negative:
			if (isX) {
				X = b.getMin().x;
				Y = b.getMin().y;
				Z = b.getMin().z;
			}
			if (isY) {
				X = b.getMax().x;
				Y = b.getMin().y;
				Z = b.getMin().z;
			}
			if (isZ) {
				X = b.getMax().x;
				Y = b.getMax().y;
				Z = b.getMin().z;
			}
			break;
		case positive:
			if (isX) {
				X = b.getMax().x;
				Y = b.getMin().y;
				Z = b.getMin().z;
			}
			if (isY) {
				X = b.getMax().x;
				Y = b.getMax().y;
				Z = b.getMin().z;
			}
			if (isZ) {
				X = b.getMax().x;
				Y = b.getMax().y;
				Z = b.getMax().z;
			}
			break;
		default:
			break;

		}

		TransformNR target = new TransformNR(X, Y, Z);
		double rx = 0;
		double ry = 0;
		double rz = 0;
		if (isX) {
			rx = 0;
			ry = -90;
			rz = 90;
		}
		if (isY) {
			rx = 0;
			ry = 90;
			rz = 0;
		}
		if (isZ) {
			rx = -90;
			ry = 0;
			rz = 0;
		}
		TransformNR pureRot = new TransformNR(new RotationNR(rx, rz, ry));

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
			TransformFactory.nrToAffine(pureRot, cameraOrent);
			TransformFactory.nrToAffine(target.setRotation(new RotationNR()), location);
		});

		// hover.setText(name +" "+getCurrentInReferenceFrame()) ;
	}

	private boolean isZvector() {
		return orentation.z > 0;
	}

	private boolean isYvector() {
		return orentation.y > 0;
	}

	private boolean isXvector() {
		return orentation.x > 0;
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}

	public void initialize(Allign opperation, BowlerStudio3dEngine engine, List<CSG> toAllign, List<String> selected) {
		this.opperation = opperation;
		// TODO Auto-generated method stub
		this.engine = engine;
		this.toAllign = toAllign;
		this.selected = selected;
		reset();
	}

	public void hide() {
		getHandle().setVisible(false);
	}

	public void setOnClickCallback(Runnable onClick) {
		this.onClick = onClick;

	}

	public void reset() {
		getHandle().setVisible(true);
		getHandle().addEventFilter(MouseEvent.MOUSE_EXITED, exited);
		getHandle().addEventFilter(MouseEvent.MOUSE_ENTERED, entered);
		getHandle().addEventFilter(MouseEvent.MOUSE_CLICKED, onClickEvent);
		material.setDiffuseColor(Color.BLACK);
	}
}
