package com.commonwealthrobotics;

import javafx.application.Application;
import javafx.collections.ObservableFloatArray;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.geometry.HPos;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import org.fxyz3d.shapes.primitives.CuboidMesh;
import org.fxyz3d.shapes.primitives.TexturedMesh;

import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.Polygon;
import eu.mihosoft.vrl.v3d.Vertex;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.stage.Stage;

public class ViewCube {
	private TexturedMesh meshView;
	private BowlerStudio3dEngine engine;
	private boolean focusTrig=false;
	public MeshView createTexturedCube(SubScene scene, BowlerStudio3dEngine engine) {
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
