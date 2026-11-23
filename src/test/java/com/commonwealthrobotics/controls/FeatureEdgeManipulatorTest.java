package com.commonwealthrobotics.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;

class FeatureEdgeManipulatorTest {
	@Test
	void initialAndRebuiltLinesFollowTheLiveManipulatorInsideSelectionTransforms() {
		CSG part = new Cube(10).toCSG();
		Affine manipulator = new Affine();
		manipulator.setTx(30);
		part.setManipulator(manipulator);
		Group edges = new Group();
		edges.getTransforms().add(new Rotate(90));
		for (int rebuild = 0; rebuild < 3; rebuild++) {
			MeshView view = SelectionSession.createFeatureEdgeView(part, new TriangleMesh());
			edges.getChildren().setAll(view);
			for (double x : new double[]{30, 0, -15, 30}) {
				manipulator.setTx(x);
				Point3D point = view.localToScene(new Point3D(1, 2, 3));
				assertEquals(-2, point.getX(), 1e-9);
				assertEquals(x + 1, point.getY(), 1e-9);
				assertEquals(3, point.getZ(), 1e-9);
			}
		}
	}

	@Test
	void ordinaryComponentLinesRemainInLocalSpace() {
		CSG part = new Cube(10).toCSG();
		MeshView view = SelectionSession.createFeatureEdgeView(part, new TriangleMesh());
		assertTrue(view.getTransforms().isEmpty());
		assertEquals(new Point3D(1, 2, 3), view.localToParent(new Point3D(1, 2, 3)));
	}
}
