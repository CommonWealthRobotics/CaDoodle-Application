package com.commonwealthrobotics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

class SelectionPlaneTest {
	@Test
	void backgroundSurfaceRemainsUsableWithEitherGeometryBackend() {
		CSG.OptType previous = CSG.getDefaultOptionType();
		try {
			for (CSG.OptType backend : new CSG.OptType[]{CSG.OptType.CSG_BOUND, CSG.OptType.Manifold3d}) {
				CSG.setDefaultOptType(backend);
				assertEquals(backend, CSG.getDefaultOptionType());
				MeshView plane = SelectionPlane.create();
				TriangleMesh mesh = (TriangleMesh) plane.getMesh();
				assertFalse(plane.getBoundsInLocal().isEmpty(), backend.toString());
				assertEquals(20000, plane.getBoundsInLocal().getWidth(), 0.001);
				assertEquals(20000, plane.getBoundsInLocal().getHeight(), 0.001);
				assertTrue(mesh.getFaces().size() >= 12);
			}
		} finally {
			CSG.setDefaultOptType(previous);
		}
	}
}
