package com.commonwealthrobotics;

import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;
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
import eu.mihosoft.vrl.v3d.Sphere;

public class ViewCube {
	private static BowlerStudio3dEngine engine;
	private static boolean focusTrig = false;
	private static float cubeSize = 100f;
	private static PhongMaterial phongMaterialCube = new PhongMaterial(Color.YELLOW);
	private static PhongMaterial phongMaterialText = new PhongMaterial(Color.BLACK);

	private static PhongMaterial phongMaterialEdge = new PhongMaterial(Color.ORANGE);
	private static PhongMaterial phongMaterialCorner = new PhongMaterial(Color.WHITE);
	private static PhongMaterial phongMaterialHover = new PhongMaterial(Color.RED);

	public static void setColors(Color surface, Color edge, Color corner, Color text, Color hover) {
		phongMaterialCube.setDiffuseColor(surface);
		phongMaterialEdge.setDiffuseColor(edge);
		phongMaterialCorner.setDiffuseColor(corner);
		phongMaterialText.setDiffuseColor(text);
		phongMaterialHover.setDiffuseColor(hover);
	}

	public static void createTexturedCube(BowlerStudio3dEngine e) {

		engine = e;
		addTexturedCube();

		CSG face = new Cube(2, cubeSize, cubeSize).toCSG();
		CSG corner = new Sphere(10).toCSG();
		CSG edge = new Cube(10, cubeSize, 10).toCSG();
		CSG edgeUp = new Cube(10, 10, cubeSize).toCSG();

		addSurface(face, new TransformNR(cubeSize / 2, 0, 0), phongMaterialCube, "Front");
		addSurface(face, new TransformNR(-cubeSize / 2, 0, 0), phongMaterialCube, "Back");
		addSurface(face, new TransformNR(0, cubeSize / 2, 0), phongMaterialCube, "Left");
		addSurface(face, new TransformNR(0, -cubeSize / 2, 0), phongMaterialCube, "Right");
		addSurface(face, new TransformNR(0, 0, cubeSize / 2), phongMaterialCube, "Top");
		addSurface(face, new TransformNR(0, 0, -cubeSize / 2), phongMaterialCube, "Bottom");

		for (double k = -cubeSize / 2; k <= cubeSize; k += cubeSize) {
			for (double j = -cubeSize / 2; j <= cubeSize; j += cubeSize)
				addSurface(edge, new TransformNR(0, k, j), phongMaterialEdge, null);
		}
		for (double k = -cubeSize / 2; k <= cubeSize; k += cubeSize) {
			for (double j = -cubeSize / 2; j <= cubeSize; j += cubeSize)
				addSurface(edge, new TransformNR(k, 0, j), phongMaterialEdge, null);
		}
		for (double k = -cubeSize / 2; k <= cubeSize; k += cubeSize) {
			for (double j = -cubeSize / 2; j <= cubeSize; j += cubeSize)
				addSurface(edgeUp, new TransformNR(k, j, 0), phongMaterialEdge, null);
		}
		for (double k = -cubeSize / 2; k <= cubeSize; k += cubeSize)

			for (double j = -cubeSize / 2; j <= cubeSize; j += cubeSize)

				for (double i = -cubeSize / 2; i <= cubeSize; i += cubeSize)
					try {
						addSurface(corner, new TransformNR(k, j, i), phongMaterialCorner, null);
					} catch (Exception ex) {
						Log.error(ex);
					}

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
			az = Math.toDegrees(Math.atan2(transformNR.getY(), transformNR.getX()));
		double el = 0;
		if (Math.abs(transformNR.getZ()) > 0.1) {
			TransformNR alligned = transformNR.times(new TransformNR(new RotationNR(0, -az, 0)));
			double horizontal = Math.hypot(transformNR.getX(), transformNR.getY());

			el = Math.toDegrees(Math.atan2(alligned.getZ(), horizontal));
			if (el > 90) {
				el = 180 - el;
				// az+=180;
			}
			if (el < -90) {
				el = -180 - el;
				// az+=180;
			}

		}
		TransformNR target = new TransformNR(0, 0, 0, new RotationNR(0, az, -el));
		MeshView label = null;
		if (string != null) {
			String translation = ActiveProject.getTranslation(string);

			label = CSG.textToSize(translation, cubeSize - 20, cubeSize / 2, 4).moveToCenter().toZMin().rotx(-90)
					.rotz(-90 - az).roty(el).transformed(TransformFactory.nrToCSG(transformNR))

					.newMesh();
			label.setMouseTransparent(true);
			label.setMaterial(phongMaterialText);
		}

		//		Label label = new Label(translation);
		//		label.setScaleX(100);
		//		label.setScaleY(100);
		////		label.getTransforms().addAll(
		////				TransformFactory.nrToAffine(target),
		////				TransformFactory.nrToAffine(transformNR)
		////				);
		CSG placed = csg.roty(el).rotz(az).transformed(TransformFactory.nrToCSG(transformNR));
		MeshView meshView = placed.newMesh();
		meshView.setMaterial(phongMaterialCube2);
		meshView.setOnMouseEntered(ev -> {
			meshView.setMaterial(phongMaterialHover);
		});
		meshView.setOnMouseExited(ev -> {
			meshView.setMaterial(phongMaterialCube2);
		});
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
		if (label != null)
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
