package com.commonwealthrobotics.controls;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cylinder;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.MatrixType;
import javafx.geometry.Point3D;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR; 

import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;

public class MoveUpArrow {
	private MeshView mesh;
	private boolean selected;
	private PhongMaterial material = new PhongMaterial();
	private Color color = Color.DARKGRAY;
	private Color selectedColor = new Color(1, 0, 0, 1);
	private Point3D startingPosition3D;

	private final Affine selectionTransform;
	private final Affine workplaneOffsetTransform;
	private final Affine moveUpLocationTransform;
	private final Scale scaleTransform;

	public MoveUpArrow(Affine selection, Affine workplaneOffset, Affine moveUpLocation, Scale scaleTF,
			Manipulation zMoveManipulator /*EventHandler<MouseEvent> eventHandler*/, Runnable onSelect, Runnable onReset) {

		this.selectionTransform = selection;
		this.workplaneOffsetTransform = workplaneOffset;
		this.moveUpLocationTransform = moveUpLocation;
		this.scaleTransform = scaleTF;

		// Arrow on top of z-height handle. Parameters(radius1, radius2, height);
		double cSize = ResizingHandle.getSize();
		CSG cylinder = new Cylinder(ResizingHandle.getSize(), 0, ResizingHandle.getSize() * 2).toCSG()
			.setColor(getColor());

		mesh = cylinder.getMesh();
		mesh.getTransforms().add(selection);
		mesh.getTransforms().add(workplaneOffset);
		mesh.getTransforms().add(moveUpLocation);
		mesh.getTransforms().add(scaleTF);
		mesh.addEventFilter(MouseEvent.ANY, zMoveManipulator.getMouseEvents());
		material.setDiffuseColor(Color.GRAY);
		material.setSpecularColor(Color.LIGHTGRAY);
		mesh.setMaterial(material);

		mesh.addEventFilter(MouseEvent.MOUSE_EXITED, event -> {
			if (!selected)
				resetColor();
		});

		mesh.addEventFilter(MouseEvent.MOUSE_ENTERED, event -> {
			setSelectedColor();
		});

		mesh.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			com.neuronrobotics.sdk.common.Log.debug("MoveUp selected " + event);
			onReset.run();
			selected = true;
			setSelectedColor();
			onSelect.run();

		// Get the current arrow position (includes zoom-dependent offset)
		double arrowX = moveUpLocationTransform.getTx();
		double arrowY = moveUpLocationTransform.getTy();
		double arrowZ = moveUpLocationTransform.getTz();
		
		// Calculate the zoom-dependent offset that was added
		double arrowOffset = ResizingHandle.getSize() * scaleTransform.getZ();
		
		// Subtract to get back to the base position (top of bounds)
		double baseX = arrowX;
		double baseY = arrowY;
		double baseZ = arrowZ - arrowOffset;
		
		this.startingPosition3D = new Point3D(baseX, baseY, baseZ);
		
		zMoveManipulator.setStartingWorkplanePosition(startingPosition3D);

		});
	}

	public Point3D getStartingPoint3D() {
		return startingPosition3D;
	}

	public Point3D calculateWorldPosition() {
		// Bounds in parent coordinate space (includes all transforms)
		javafx.geometry.Bounds parentBounds = mesh.getBoundsInParent();
		System.out.println("BOUNDS CALC: " + parentBounds);
		return new Point3D( parentBounds.getCenterX(), parentBounds.getCenterY(), (parentBounds.getMinZ() + parentBounds.getMaxZ()) / 2 );
	}

	private void setSelectedColor() {
		material.setDiffuseColor(selectedColor);
	}

	private void resetColor() {
		material.setDiffuseColor(getColor());
	}

	private Color getColor() {	
		return color;
	}

	public void resetSelected() {
		resetColor();
		selected = false;
	}

	public boolean isSelected() {
		return selected;
	}

	public MeshView getMesh() {
		return mesh;
	}

	public void hide() {
		mesh.setVisible(false);
	}

	public void show() {
		mesh.setVisible(true);
	}

	/**
	 * @param color the color to set
	 */
	public void setColor(Color color) {
		this.color = color;
		resetColor() ;
	}

	public Color getSelectedColor() {
		return selectedColor;
	}

	public void setSelectedColor(Color selectedColor) {
		this.selectedColor = selectedColor;
	}

}
