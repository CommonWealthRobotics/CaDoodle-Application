package com.commonwealthrobotics.controls;

import com.neuronrobotics.bowlerstudio.physics.TransformFactory;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.Node;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;

public class DottedLine extends Group {

	private double startX = 0.0;
	private double startY = 0.0;
	private double startZ = 0.0;
	private double endX = 0.0;
	private double endY = 0.0;
	private double endZ = 0.0;

	private double prevStartX = 0.0;
	private double prevStartY = 0.0;
	private double prevStartZ = 0.0;
	private double prevEndX = 0.0;
	private double prevEndY = 0.0;
	private double prevEndZ = 0.0;

	private static final double CHANGE_THRESHOLD = 0.001;

	private final double dotRadius;
	private final double dotSpacing;
	private Color dotColor = Color.BLACK;
	private Affine workplaneOffset;
	private Scale scale = new Scale(1, 1, 1);
	// Pooled Sphere nodes with one shared material; updateLine() only
	// repositions views instead of building a fresh sphere per dot per event
	private PhongMaterial dotMaterial = null;
	private final List<Node> dots = new ArrayList<>();
	private final List<Node> placedDots = new ArrayList<>();

	public DottedLine(double dotRadius, double dotSpacing, Affine workplaneOffset, Color dotColor) {
		this(dotRadius, dotSpacing, workplaneOffset);
		this.dotColor = dotColor;
	}

	public DottedLine(double dotRadius, double dotSpacing, Affine workplaneOffset) {
		this.dotRadius = dotRadius;
		this.dotSpacing = dotSpacing;
		this.workplaneOffset = workplaneOffset;
	}

	private boolean changed(double value, double prev) {
		return Math.abs(value - prev) > CHANGE_THRESHOLD;
	}

	public void setPoints(double startX, double startY, double startZ, double endX, double endY, double endZ) {
		boolean dirty = changed(startX, prevStartX) || changed(startY, prevStartY) || changed(startZ, prevStartZ)
				|| changed(endX, prevEndX) || changed(endY, prevEndY) || changed(endZ, prevEndZ);

		prevStartX = startX;
		this.startX = startX;
		prevStartY = startY;
		this.startY = startY;
		prevStartZ = startZ;
		this.startZ = startZ;
		prevEndX = endX;
		this.endX = endX;
		prevEndY = endY;
		this.endY = endY;
		prevEndZ = endZ;
		this.endZ = endZ;

		if (dirty)
			updateLine();
	}

	public void setScale(double d) {
		if (d < 0.1)
			d = 0.1;
		scale.setX(d * 2);
		scale.setY(d * 2);
		scale.setZ(d * 2);
	}

	public void updateLine() {
		// Take all dots out of the group first so the scene graph is not
		// notified once per removal
		getChildren().removeAll(placedDots);
		placedDots.clear();

		double dx = endX - startX;
		double dy = endY - startY;
		double dz = endZ - startZ;
		double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

		int numDots = (int) Math.floor(length / dotSpacing);
		if (numDots == 0)
			return;

		if (dotMaterial == null)
			dotMaterial = new PhongMaterial(dotColor);
		for (int i = 0; i <= numDots; i++) {
			double t = i / (double) numDots;
			double x = startX + t * dx;
			double y = startY + t * dy;
			double z = startZ + t * dz;

			Sphere dot;
			if (i < dots.size()) {
				dot = (Sphere) dots.get(i);
			} else {
				dot = new Sphere(dotRadius, 10);
				dot.setMaterial(dotMaterial);
				dots.add(dot);
			}
			Affine loc = TransformFactory.newAffine(x, y, z);
			// Per-dot location sits between the shared transforms
			dot.getTransforms().setAll(workplaneOffset, loc, scale);
			placedDots.add(dot);
		}
		getChildren().addAll(placedDots);
	}

	public double getStartX() {
		return startX;
	}

	public double getStartY() {
		return startY;
	}

	public double getStartZ() {
		return startZ;
	}

	public double getEndX() {
		return endX;
	}

	public double getEndY() {
		return endY;
	}

	public double getEndZ() {
		return endZ;
	}

	public void setStroke(Color color) {
		this.dotColor = color;
		updateLine();
	}
}
