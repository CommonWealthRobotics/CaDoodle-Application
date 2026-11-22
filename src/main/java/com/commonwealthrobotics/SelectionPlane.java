package com.commonwealthrobotics;

import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

final class SelectionPlane {
	private SelectionPlane() {
	}

	static MeshView create() {
		// A picking surface needs no solid geometry or CSG backend;
		// wind toward the camera, which sits on the negative-Z side
		TriangleMesh mesh = new TriangleMesh();
		mesh.getPoints().setAll(-10000, -10000, 0, -10000, 10000, 0, 10000, 10000, 0, 10000, -10000, 0);
		mesh.getTexCoords().setAll(0, 0);
		mesh.getFaces().setAll(0, 0, 1, 0, 2, 0, 0, 0, 2, 0, 3, 0);
		return new MeshView(mesh);
	}
}
