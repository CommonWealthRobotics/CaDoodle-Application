package com.commonwealthrobotics.align;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.commonwealthrobotics.ActiveProject;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Align;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Alignment;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;

public class AlignRadioSet {
	AlignHandle positive = null;
	AlignHandle middle = null;
	AlignHandle negetive = null;
	private String name;
	private Vector3d vector3d;
	private List<AlignHandle> asList;
	private MeshView mesh = new Cube(1, 1, 1).toCSG().setColor(Color.GREY).getMesh();
	private Affine location = new Affine();
	private Affine cameraOrient = new Affine();
	private Scale scaleTF = new Scale();
	private Affine alignLoc = new Affine();

	private Affine move;
	private Affine workplaneOffset;
	private PhongMaterial material;

	public AlignRadioSet(String name, Affine move, Affine workplaneOffset, Vector3d vector3d, ActiveProject ap) {
		this.name = name;
		this.move = move;
		this.workplaneOffset = workplaneOffset;
		this.vector3d = vector3d;

		positive = new AlignHandle(Alignment.positive, move, workplaneOffset, vector3d, ap);
		middle = new AlignHandle(Alignment.middle, move, workplaneOffset, vector3d, ap);
		negetive = new AlignHandle(Alignment.negative, move, workplaneOffset, vector3d, ap);
		asList = Arrays.asList(positive, middle, negetive);
		mesh.getTransforms().add(move);
		mesh.getTransforms().add(alignLoc);
		mesh.getTransforms().add(workplaneOffset);
		mesh.getTransforms().add(location);
		mesh.getTransforms().add(cameraOrient);
		mesh.getTransforms().add(scaleTF);
		mesh.setVisible(false);
		material = new PhongMaterial();
		material.setDiffuseColor(new Color(vector3d.x, vector3d.y, vector3d.z, 0.75));
		material.setSpecularColor(new Color(vector3d.x, vector3d.y, vector3d.z, 0.75));

		mesh.setCullFace(CullFace.BACK);
		mesh.setMaterial(material);
	}

	public void threeDTarget(double screenW, double screenH, double zoom, Bounds b, TransformNR cf) {
		positive.threeDTarget(screenW, screenH, zoom, b, cf);
		middle.threeDTarget(screenW, screenH, zoom, b, cf);
		negetive.threeDTarget(screenW, screenH, zoom, b, cf);

		BowlerStudio.runLater(() -> {
			boolean isX = isXvector();
			boolean isY = isYvector();
			boolean isZ = isZvector();
			double X = 0;
			double Y = 0;
			double Z = 0;
			double length = 1;
			double x = b.getCenter().x - cf.getX();
			double y = b.getCenter().y - cf.getY();
			double z = b.getCenter().z - cf.getZ();

			// Calculate the distance between camera and target
			double distance = Math.sqrt(x * x + y * y + z * z);

			// Define a base scale and distance
			double baseScale = 0.75;
			double baseDistance = 1000.0;

			// Calculate the scale factor
			double scaleFactor = ((distance / baseDistance) * baseScale);

			// Clamp the scale factor to a reasonable range
			scaleFactor = Math.max(0.001, Math.min(90.0, scaleFactor));
			double barsize = scaleFactor * 5;
			scaleTF.setX(barsize);
			scaleTF.setY(barsize);
			scaleTF.setZ(barsize);

			if (isX) {
				X = b.getCenter().x;
				Y = b.getMin().y;
				Z = b.getMin().z;
				length = b.getTotalX();
				scaleTF.setX(length);
			}
			if (isY) {
				X = b.getMax().x;
				Y = b.getCenter().y;
				Z = b.getMin().z;
				length = b.getTotalY();
				scaleTF.setY(length);
			}
			if (isZ) {
				X = b.getMax().x;
				Y = b.getMax().y;
				Z = b.getCenter().z;
				length = b.getTotalZ();
				scaleTF.setZ(length);
			}
			TransformNR target = new TransformNR(X, Y, Z);
			//TransformFactory.nrToAffine(pureRot, cameraOrient);
			TransformFactory.nrToAffine(target.setRotation(new RotationNR()), location);
		});

	}

	private boolean isZvector() {
		return vector3d.z > 0;
	}

	private boolean isYvector() {
		return vector3d.y > 0;
	}

	private boolean isXvector() {
		return vector3d.x > 0;
	}

	public List<Node> getElements() {
		ArrayList<Node> result = new ArrayList<Node>();
		for (AlignHandle r : asList) {
			result.add(r.getHandle());
		}
		result.add(mesh);
		return result;
	}

	public void initialize(Align operation, BowlerStudio3dEngine engine, List<CSG> toAlign, List<String> selected) {
		for (AlignHandle r : asList) {
			r.initialize(operation, engine, toAlign, selected);
		}
		BowlerStudio.runLater(() -> mesh.setVisible(true));
	}

	public void hide() {
		for (AlignHandle r : asList) {
			r.hide();
		}
		BowlerStudio.runLater(() -> mesh.setVisible(false));
	}

	public void setOnClickCallback(Runnable onClick) {
		for (AlignHandle r : asList) {
			r.setOnClickCallback(() -> {
				com.neuronrobotics.sdk.common.Log.error("Radio group click ");
				for (AlignHandle ah : asList) {
					ah.reset();
				}
				onClick.run();
			});
		}
	}

	public void recomputeOps(HashMap<String, Bounds> cache) {
		for (AlignHandle ah : asList) {
			ah.recomputeOps(cache);
		}
	}

	public void clear(HashMap<String, Bounds> cache) {
		for (AlignHandle ah : asList) {
			ah.clear(cache);
		}
	}
}
