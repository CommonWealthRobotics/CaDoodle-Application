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

public class MoveUpArrow {
	private MeshView mesh;
	private boolean selected;
	private PhongMaterial material = new PhongMaterial();

	public MoveUpArrow(Affine selection, Affine workplaneOffset, Affine moveUpLocation, Scale scaleTF,
			EventHandler<MouseEvent> eventHandler, Runnable onSelect, Runnable onReset) {
		CSG setColor = new Cylinder(ResizingHandle.getSize() / 2, 0, ResizingHandle.getSize()).toCSG()
				.setColor(Color.BLACK);
		mesh = setColor.getMesh();
		mesh.getTransforms().add(selection);
		mesh.getTransforms().add(workplaneOffset);
		mesh.getTransforms().add(moveUpLocation);
		mesh.getTransforms().add(scaleTF);
		mesh.addEventFilter(MouseEvent.ANY, eventHandler);
		material.setDiffuseColor(Color.GRAY);
		material.setSpecularColor(Color.WHITE);
		mesh.setMaterial(material);
		mesh.addEventFilter(MouseEvent.MOUSE_EXITED, event -> {
			if(!selected)
				resetColor();
		});
		mesh.addEventFilter(MouseEvent.MOUSE_ENTERED, event -> {
			setSelectedColor();
		});
		mesh.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			com.neuronrobotics.sdk.common.Log.error("MoveUp selected");
			onReset.run();
			selected=true;
			setSelectedColor();
			onSelect.run();
		});
	}
	private void setSelectedColor() {
		material.setDiffuseColor(new Color(1, 0, 0, 1));
	}

	private void resetColor() {
		material.setDiffuseColor(Color.BLACK);
	}
	public void resetSelected() {
		resetColor();
		selected=false;
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

}
