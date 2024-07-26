package com.commonwealthrobotics;

/**
 * CSGMesh.java
 *
 * Copyright (c) 2013-2016, F(X)yz
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of F(X)yz, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL F(X)yz BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;
import javafx.scene.DepthTest;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

import org.fxyz3d.geometry.Face3;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.shapes.primitives.TexturedMesh;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Polygon;
import eu.mihosoft.vrl.v3d.Vector3d;
import eu.mihosoft.vrl.v3d.Vertex;
import javafx.scene.image.Image;

/**
 *
 * @author José Pereda Llamas Created on 01-may-2015 - 12:20:06
 */
public class TexturedCSG extends TexturedMesh {

	private final CSG primitive;
	private Image textureImage; // Assume this is provided

	public TexturedCSG(CSG primitive, Image texture) {
		this.primitive = primitive;
		primitive.triangulate();
		updateMesh();
		setCullFace(CullFace.BACK);
		setDrawMode(DrawMode.FILL);
		setDepthTest(DepthTest.ENABLE);
		textureImage = texture;

		PhongMaterial material = new PhongMaterial();
		material.setDiffuseMap(texture);
		material.setDiffuseColor(javafx.scene.paint.Color.WHITE);
		// Apply the material to the meshview
		setMaterial(material);
	}

	@Override
	protected final void updateMesh() {
		setMesh(null);
		mesh = createCSGMesh();
		setMesh(mesh);
	}

	private TriangleMesh createCSGMesh() {
		List<Vertex> vertices = new ArrayList<>();
		List<List<Integer>> indices = new ArrayList<>();
		List<Point2D> texCoords = new ArrayList<>();
		listVertices.clear();
		listTextures.clear();
		listFaces.clear();

		// Process each polygon
		primitive.getPolygons().forEach(p -> {
			List<Integer> polyIndices = new ArrayList<>();

			// Compute polygon normal
			Vector3d normal = computePolygonNormal(p);

			// Create transformation matrix
			Affine transform = createTransform(normal, p.vertices.get(0).pos);

			// Invert transformation matrix
			Affine inverseTransform = new Affine(transform);
			try {
				inverseTransform.invert();
			} catch (NonInvertibleTransformException e) {
				e.printStackTrace();
				return; // Skip this polygon if the transform can't be inverted
			}

			// Calculate bounding box for texture mapping for this specific polygon
			double minXt = Double.MAX_VALUE, minYt = Double.MAX_VALUE;
			double maxXt = -Double.MAX_VALUE, maxYt = -Double.MAX_VALUE;

			// Transform vertices to 2D plane
			for (Vertex v : p.vertices) {
				javafx.geometry.Point3D transformed = inverseTransform.deltaTransform(v.pos.x, v.pos.y, v.pos.z);
				minXt = Math.min(minXt, transformed.getX());
				minYt = Math.min(minYt, transformed.getY());
				maxXt = Math.max(maxXt, transformed.getX());
				maxYt = Math.max(maxYt, transformed.getY());
			}
			double minX = minXt;
			double minY = minYt;
			double maxX = maxXt;
			double maxY = maxYt;

			// Add vertices and calculate texture coordinates for this polygon
			p.vertices.forEach(v -> {
				if (!vertices.contains(v)) {
					vertices.add(v);
					listVertices.add(new Point3D((float) v.pos.x, (float) v.pos.y, (float) v.pos.z));

					// Transform vertex to 2D plane
					// Point3D transformed = inverseTransform.transform(v.pos);
					javafx.geometry.Point3D transformed = inverseTransform.deltaTransform(v.pos.x, v.pos.y, v.pos.z);

					// Calculate texture coordinates, ensuring they fit within the image bounds
					float u = (float) ((transformed.getX() - minX) / (maxX - minX));
					float vf = (float) ((transformed.getY() - minY) / (maxY - minY));

					// Clamp coordinates to [0, 1] range
					u = Math.max(0, Math.min(1, u));
					vf = Math.max(0, Math.min(1, vf));

					texCoords.add(new Point2D(u, vf));

					polyIndices.add(vertices.size());
				} else {
					int index = vertices.indexOf(v);
					polyIndices.add(index + 1);
				}
			});

			// Add polygon indices to the list
			indices.add(polyIndices);
		});

		// Convert texture coordinates list to float array
		textureCoords = new float[texCoords.size() * 2];
		for (int i = 0; i < texCoords.size(); i++) {
			textureCoords[i * 2] = (float) texCoords.get(i).getX();
			textureCoords[i * 2 + 1] = (float) texCoords.get(i).getY();
		}

		// Convert polygons into triangles and create faces
		indices.forEach(pVerts -> {
			int index1 = pVerts.get(0);
			for (int i = 0; i < pVerts.size() - 2; i++) {
				int index2 = pVerts.get(i + 1);
				int index3 = pVerts.get(i + 2);
				listTextures.add(new Face3(index1 - 1, index2 - 1, index3 - 1));
				listFaces.add(new Face3(index1 - 1, index2 - 1, index3 - 1));
			}
		});

		// Set smoothing groups
		int[] faceSmoothingGroups = new int[listFaces.size()];
		smoothingGroups = faceSmoothingGroups;

		// Create and return the mesh
		return createMesh();
	}

	private Vector3d computePolygonNormal(Polygon p) {
		return p.plane.normal;
	}

	private Affine createTransform(Vector3d normal, Vector3d point) {
		// Create a transformation matrix based on the polygon normal and a point on the
		// polygon
		Affine transform = new Affine();

		// Translate the point to origin
		transform.prepend(new Translate(-point.x, -point.y, -point.z));

		// Rotate to align the normal with the Z-axis
		Vector3d zAxis = new Vector3d(0, 0, 1);
		Vector3d axis = zAxis.cross(normal);
		axis.normalize();
		double angle = Math.acos(zAxis.dot(normal));
		transform.prepend(new Rotate(Math.toDegrees(angle), axis.x, axis.y, axis.z));

		return transform;
	}
}
