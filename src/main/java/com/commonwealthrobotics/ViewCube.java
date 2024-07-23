package com.commonwealthrobotics;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;

public class ViewCube {

	public MeshView createTexturedCube() {
		// Create a cube using JCSG
		CSG cube = new Cube(200).toCSG()
				.union(new Cube(200).toCSG().move(30,80,100))
				.hull()
				.toZMin();

		// Create texture with text
		Image texture = createTexturedImage();

		PhongMaterial material = new PhongMaterial();
		// material.setSpecularMap(texture);
		material.setDiffuseMap(texture);
		// material.setSpecularColor(Color.TRANSPARENT);
		material.setDiffuseColor(Color.rgb(255, 255, 255, 0.5)); // Semi-transparent
		CSGMesh meshResult = new CSGMesh(cube);
		
		MeshView meshView = meshResult;
		meshResult.setTextureModeVertices3D(1530,p->(double)p.x*p.y);
		// Load your original image
		material.setDiffuseMap(createTexturedImage());
		meshView.setMaterial(material);

		return meshView;
	}


	private Image createTexturedImage() {
		Image baseImage = new Image(MainController.class.getResourceAsStream("holes.png"));

		return baseImage;
	}

}
