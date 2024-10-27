package com.commonwealthrobotics;

import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.geometry.Point3D;
import org.fxyz3d.shapes.primitives.CuboidMesh;
import org.fxyz3d.shapes.primitives.TexturedMesh;

import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.IControlsMap;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

public class ViewCube {
	private TexturedMesh meshView;
	private BowlerStudio3dEngine engine;
	private boolean focusTrig=false;
	private float width;
	public MeshView createTexturedCube(BowlerStudio3dEngine engine) {
		this.engine = engine;
		Affine rot = TransformFactory.nrToAffine(new TransformNR(new RotationNR(-90,0,0)));
		width = 100f;
		meshView = new CuboidMesh(width, width, width);
		meshView.setTextureModeImage(MainController.class.getResource("navCube.png").toExternalForm());
		meshView.getTransforms().add(rot);
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
		com.neuronrobotics.sdk.common.Log.error("Got event");
		PickResult pickResult = event.getPickResult();
		if (pickResult.getIntersectedNode() == meshView) {
			Point3D intersectionPoint = pickResult.getIntersectedPoint();
			TransformNR faceOrientation = determineFaceOrientation(intersectionPoint);
			// com.neuronrobotics.sdk.common.Log.error("Clicked face orientation: " + faceOrientation);
			engine.focusOrentation(faceOrientation);
		}
	}

	private TransformNR determineFaceOrientation(Point3D point) {
		// Get the bounds of the MeshView
		double min = -width/2;
		double max = width/2;

		// Small epsilon value for float comparison
		double epsilon = 0.001;
		TransformNR frame = engine.getFlyingCamera().getCamerFrame();

		if (Math.abs(point.getX() - min) < epsilon) {
			com.neuronrobotics.sdk.common.Log.error("Back");
			return new TransformNR(0, 0, 0, new RotationNR(0, 180, 0));
		}
		if (Math.abs(point.getX() - max) < epsilon) {
			com.neuronrobotics.sdk.common.Log.error("Front");
			return new TransformNR(0, 0, 0, new RotationNR(0, 0, 0));
		}
		if (Math.abs(point.getY() - min) < epsilon) {
			com.neuronrobotics.sdk.common.Log.error("Top");
			return new TransformNR(0, 0, 0, new RotationNR(0,0 , -90));
		}
		if (Math.abs(point.getY() - max) < epsilon) {
			com.neuronrobotics.sdk.common.Log.error("Bottom");
			return new TransformNR(0, 0, 0, new RotationNR(0, 90, 90));
		}
		if (Math.abs(point.getZ() - min) < epsilon) {
			com.neuronrobotics.sdk.common.Log.error("Right");
			return new TransformNR(0, 0, 0, new RotationNR(0, -90, 0));
		}
		if (Math.abs(point.getZ() - max) < epsilon) {
			com.neuronrobotics.sdk.common.Log.error("Left");
			return new TransformNR(0, 0, 0, new RotationNR(0, 90, 0));
		}

		return new TransformNR();
	}
}
