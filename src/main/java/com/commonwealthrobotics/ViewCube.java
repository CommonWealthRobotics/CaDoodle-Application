package com.commonwealthrobotics;

import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.geometry.Point3D;
import org.fxyz3d.shapes.primitives.CuboidMesh;
import org.fxyz3d.shapes.primitives.TexturedMesh;

import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.IControlsMap;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.Log;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;

public class ViewCube {
	private static BowlerStudio3dEngine engine;
	private static boolean focusTrig = false;
	private static float cubeSize = 100f;
	private static PhongMaterial phongMaterialCube = new PhongMaterial(Color.YELLOW);
	private static PhongMaterial phongMaterialEdge = new PhongMaterial(Color.ORANGE);
	private static PhongMaterial phongMaterialCorner = new PhongMaterial(Color.WHITE);

	public static void createTexturedCube(BowlerStudio3dEngine e) {

		engine = e;
		addTexturedCube();

		addSurface(new Cube(2, cubeSize, cubeSize).toCSG(), new TransformNR(cubeSize/2, 0, 0), phongMaterialCube,"Front");

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

				return (secondaryButtonDown || primaryButtonDown);
			}

			@Override
			public boolean isMove(MouseEvent ev) {
				return false;
			}
		});

		// meshView.setOnMouseClicked(this::handleMouseClick);
		// meshView.setOnMouseClicked(event -> handleMouseClick(event, meshView));

		return;
	}

	private static void addSurface(CSG csg, TransformNR transformNR, PhongMaterial phongMaterialCube2, String string) {
		double az = 0;
		if (Math.abs(transformNR.getX()) > 0.1 || Math.abs(transformNR.getY()) > 0.1)
			az=Math.toDegrees(Math.atan2(transformNR.getY(), transformNR.getX()));
		double el =0;
		if (Math.abs(transformNR.getZ()) > 0.1 ) {
			TransformNR alligned = transformNR.times(new TransformNR(new RotationNR(0, -az, 0)));
			el=Math.toDegrees(Math.atan2(alligned.getX(), alligned.getZ()));
		}
		TransformNR target= new TransformNR(0, 0, 0, new RotationNR(0, az, el));
		Log.debug("Targeting "+target.toSimpleString());
		
		String translation = ActiveProject.getTranslation(string);
		Label label = new Label(translation); 
		label.setScaleX(100);
		label.setScaleY(100);
//		label.getTransforms().addAll(
//				TransformFactory.nrToAffine(target),
//				TransformFactory.nrToAffine(transformNR)
//				);
		CSG placed = csg.transformed(TransformFactory.nrToCSG(transformNR));
		MeshView meshView = placed.newMesh();
		meshView.setMaterial(phongMaterialCube2);

		meshView.setOnMousePressed(event -> {
			focusTrig = true;
		});

		meshView.setOnMouseDragged(event -> {
			engine.focusOrientation(null, null, 0); // Send cancel
			focusTrig = false;
		});

		meshView.setOnMouseReleased(event -> {
			if (focusTrig) {
				if (event.getPickResult().getIntersectedNode() == meshView) {

					engine.focusOrientation(target);
				}
			}
		});

		engine.addUserNode(meshView);
		engine.addUserNode(label);
	}

	private static void addTexturedCube() {
		Affine rot = TransformFactory.nrToAffine(new TransformNR(new RotationNR(-90, 0, 0)));
		TexturedMesh meshView = new CuboidMesh(cubeSize, cubeSize, cubeSize);
		meshView.setTextureModeImage(MainController.class.getResource("navCube.png").toExternalForm());
		meshView.getTransforms().add(rot);

		meshView.setOnMousePressed(event -> {
			focusTrig = true;
		});

		meshView.setOnMouseDragged(event -> {
			engine.focusOrientation(null, null, 0); // Send cancel
			focusTrig = false;
		});

		meshView.setOnMouseReleased(event -> {
			if (focusTrig)
				handleMouseClick(event, meshView);
		});
		// engine.addUserNode(meshView);
	}

	private static void handleMouseClick(MouseEvent event, TexturedMesh meshView) {
		PickResult pickResult = event.getPickResult();

		if (pickResult.getIntersectedNode() == meshView) {
			Point3D intersectionPoint = pickResult.getIntersectedPoint();
			TransformNR faceOrientation = determineFaceOrientation(intersectionPoint);
			// com.neuronrobotics.sdk.common.Log.debug("Clicked face orientation: " +
			// faceOrientation);
			engine.focusOrientation(faceOrientation);
		} else
			com.neuronrobotics.sdk.common.Log.debug("Got NavigationCube event");
	}

	private static TransformNR determineFaceOrientation(Point3D point) {
		// Get the bounds of the MeshView
		double min = -cubeSize / 2;
		double max = cubeSize / 2;

		// Small epsilon value for float comparison
		double epsilon = 0.001;
		TransformNR frame = engine.getFlyingCamera().getCamerFrame();

		if (Math.abs(point.getX() - min) < epsilon) {
			com.neuronrobotics.sdk.common.Log.debug("Event NavigationCube: Back");
			return new TransformNR(0, 0, 0, new RotationNR(0, 180, 0));
		}

		if (Math.abs(point.getX() - max) < epsilon) {
			com.neuronrobotics.sdk.common.Log.debug("Event NavigationCube: Front");
			return new TransformNR(0, 0, 0, new RotationNR(0, 0, 0));
		}

		if (Math.abs(point.getY() - min) < epsilon) {
			com.neuronrobotics.sdk.common.Log.debug("Event NavigationCube: Top");
			return new TransformNR(0, 0, 0, new RotationNR(0, 0, -90));
		}

		if (Math.abs(point.getY() - max) < epsilon) {
			com.neuronrobotics.sdk.common.Log.debug("Event NavigationCube: Bottom");
			return new TransformNR(0, 0, 0, new RotationNR(0, 0, 90));
		}

		if (Math.abs(point.getZ() - min) < epsilon) {
			com.neuronrobotics.sdk.common.Log.debug("Event NavigationCube: Right");
			return new TransformNR(0, 0, 0, new RotationNR(0, -90, 0));
		}

		if (Math.abs(point.getZ() - max) < epsilon) {
			com.neuronrobotics.sdk.common.Log.debug("Event NavigationCube: Left");
			return new TransformNR(0, 0, 0, new RotationNR(0, 90, 0));
		}

		return new TransformNR();
	}
}
