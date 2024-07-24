package com.commonwealthrobotics;

import javafx.scene.shape.MeshView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.geometry.Point3D;
import org.fxyz3d.shapes.primitives.CuboidMesh;
import org.fxyz3d.shapes.primitives.TexturedMesh;

import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.IControlsMap;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

public class ViewCube {
	private TexturedMesh meshView;
	private BowlerStudio3dEngine engine;
	private boolean focusTrig=false;
	public MeshView createTexturedCube(BowlerStudio3dEngine engine) {
		this.engine = engine;
		meshView = new CuboidMesh(100f, 100f, 100f);
		meshView.setTextureModeImage(MainController.class.getResource("navCube.png").toExternalForm());
		meshView.setOnMousePressed(event -> {
			focusTrig=true;
		});
		meshView.setOnMouseDragged(event -> {
			focusTrig=false;
		});
		meshView.setOnMouseReleased(event -> {
			if(focusTrig)
				handleMouseClick(event);
		});
		engine.setControlsMap(new IControlsMap() {
			
			@Override
			public boolean timeToCancel(MouseEvent event) {
				return false;
			}
			
			@Override
			public boolean isZoom(ScrollEvent e) {
				return false;
			}
			
			@Override
			public boolean isSlowMove(MouseEvent event) {
				return false;
			}
			
			@Override
			public boolean isRotate(MouseEvent me) {
				boolean primaryButtonDown = me.isPrimaryButtonDown();
				boolean secondaryButtonDown = me.isSecondaryButtonDown();
				return (secondaryButtonDown || (primaryButtonDown )) ;
			}
			
			@Override
			public boolean isMove(MouseEvent ev) {
				return false;
			}
		});
		// meshView.setOnMouseClicked(this::handleMouseClick);
		// meshView.setOnMouseClicked(event -> handleMouseClick(event, meshView));
		return meshView;
	}

	private void handleMouseClick(MouseEvent event) {
		System.out.println("Got event");
		PickResult pickResult = event.getPickResult();
		if (pickResult.getIntersectedNode() == meshView) {
			Point3D intersectionPoint = pickResult.getIntersectedPoint();
			TransformNR faceOrientation = determineFaceOrientation(intersectionPoint);
			// System.out.println("Clicked face orientation: " + faceOrientation);
			engine.focusOrentation(faceOrientation);
		}
	}

	private TransformNR determineFaceOrientation(Point3D point) {
		// Get the bounds of the MeshView
		double minX = meshView.getBoundsInLocal().getMinX();
		double maxX = meshView.getBoundsInLocal().getMaxX();
		double minY = meshView.getBoundsInLocal().getMinY();
		double maxY = meshView.getBoundsInLocal().getMaxY();
		double minZ = meshView.getBoundsInLocal().getMinZ();
		double maxZ = meshView.getBoundsInLocal().getMaxZ();

		// Small epsilon value for float comparison
		double epsilon = 0.001;
		TransformNR frame = engine.getFlyingCamera().getCamerFrame();

		if (Math.abs(point.getX() - minX) < epsilon)
			return new TransformNR(0, 0, 0, new RotationNR(0, 180, 0));
		if (Math.abs(point.getX() - maxX) < epsilon)
			return new TransformNR(0, 0, 0, new RotationNR(0, 0, 0));
		if (Math.abs(point.getY() - minY) < epsilon)
			return new TransformNR(0, 0, 0, new RotationNR(0, -90, 0));
		if (Math.abs(point.getY() - maxY) < epsilon)
			return new TransformNR(0, 0, 0, new RotationNR(0, 90, 0));
		if (Math.abs(point.getZ() - minZ) < epsilon)
			return new TransformNR(0, 0, 0, new RotationNR(0, 0, 90));
		if (Math.abs(point.getZ() - maxZ) < epsilon)
			return new TransformNR(0, 0, 0, new RotationNR(0, 0, -90));

		return new TransformNR();
	}
}
