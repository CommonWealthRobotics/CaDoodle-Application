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

import javax.vecmath.Matrix4d;

import javafx.geometry.Point2D;
import javafx.scene.DepthTest;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.TriangleMesh;

import org.fxyz3d.geometry.Face3;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.shapes.primitives.TexturedMesh;

import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Matrix3d;
import eu.mihosoft.vrl.v3d.Polygon;
import eu.mihosoft.vrl.v3d.Transform;
import eu.mihosoft.vrl.v3d.Vector3d;
import eu.mihosoft.vrl.v3d.Vertex;

public class TexturedCSG extends TexturedMesh {

    private final CSG primitive;
    private Image textureImage; // Assume this is provided

    public TexturedCSG(CSG primitive, Image texture) {
        this.primitive = primitive;
        this.textureImage = texture;
        primitive.triangulate();
        updateMesh();
        setCullFace(CullFace.BACK);
        setDrawMode(DrawMode.FILL);
        setDepthTest(DepthTest.ENABLE);
	    PhongMaterial material = new PhongMaterial();

	    //material.setDiffuseMap(texture);
        material.setDiffuseColor(javafx.scene.paint.Color.WHITE);
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
            //Vector3d normal = computePolygonNormal(p);
           
            // Create transformation matrix
            //Transform transform = createTransform(normal,  p.getBounds().getMin());
            Transform transform=createPolygonTransform(p);
            // Invert transformation matrix
            Transform inverseTransform = transform.inverse();

            double minXt = Double.MAX_VALUE, minYt = Double.MAX_VALUE;
            double maxXt = -Double.MAX_VALUE, maxYt = -Double.MAX_VALUE;

            // Transform vertices to 2D plane and calculate bounding box for texture mapping
            for (Vertex v : p.vertices) {
                Vector3d pos = v.pos;
                Vector3d transformed = pos.transformed(inverseTransform);

                minXt = Math.min(minXt, transformed.x);
                minYt = Math.min(minYt, transformed.y);
                maxXt = Math.max(maxXt, transformed.x);
                maxYt = Math.max(maxYt, transformed.y);
            }

            final double minX = minXt;
            final double minY = minYt;
            final double maxX = (minXt == maxXt) ? (maxXt + 1.0) : maxXt; // Avoid division by zero
            final double maxY = (minYt == maxYt) ? (maxYt + 1.0) : maxYt; // Avoid division by zero

            // Calculate texture coordinates for this polygon
            for (Vertex v : p.vertices) {
                if (!vertices.contains(v)) {
                    vertices.add(v);
                    listVertices.add(new Point3D((float) v.pos.x, (float) v.pos.y, (float) v.pos.z));

                    Vector3d pos = v.pos;
                    Vector3d transformed = pos.transform(inverseTransform);
//                    if(Math.abs(transformed.z)>0.000001) {
//                    	throw new RuntimeException("Failed to transform the point to the xy plane");
//                    }

                    // Normalize coordinates to fit within the image bounds
                    float u = (float) ((transformed.x - minX) / (maxX - minX));
                    float vf = (float) ((transformed.y - minY) / (maxY - minY));

                    // Clamp coordinates to [0, 1] range
                    u = Math.max(0, Math.min(1, u));
                    vf = Math.max(0, Math.min(1, vf));

                    texCoords.add(new Point2D(u, vf));
                    polyIndices.add(vertices.size());
                } else {
                    int index = vertices.indexOf(v);
                    polyIndices.add(index + 1);
                }
            }

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

    private Transform createPolygonTransform(Polygon polygon) {
    	Transform finalTransform = new Transform();
    	
    	Vector3d pos1 = polygon.vertices.get(0).pos;
    	Vector3d pos2 = polygon.vertices.get(1).pos;
    	Vector3d pos3 = polygon.vertices.get(2).pos;
		finalTransform= finalTransform.move(pos1);
		Transform inverse = finalTransform.inverse();
		pos1 = pos1.transformed(inverse);// should be at origin
    	pos2 = pos2.transformed(inverse);
    	pos3 = pos3.transformed(inverse);
    	double sqrt = Math.sqrt(Math.pow(pos2.x, 2) + Math.pow(pos2.y, 2));
		if(sqrt<0.000001) {
    		// this point is on the z axis, swap them
    		Vector3d tmp=pos3;
    		pos3=pos2;
    		pos2=tmp;
    	}
		double allignToX = Math.atan2(pos2.y, pos2.x);
		finalTransform=finalTransform.rotZ(Math.toDegrees(allignToX));

        return finalTransform;
    }


    private Polygon applyTransformToPolygon(Polygon polygon, Transform transform) {
        return polygon.transformed(transform);
    }


//    private Transform createTransform(Vector3d normal, Vector3d point) {
//        // Normalize the normal vector
//        Vector3d n = normal.normalized();
//        
//        // Calculate rotation angles
//        double rotX, rotY, rotZ;
//        
//        // Rotation around Y-axis
//        rotY = Math.atan2(n.x, n.z);
//        
//        // Rotation around X-axis
//        double lenXZ = Math.sqrt(n.x * n.x + n.z * n.z);
//        rotX = Math.atan2(-n.y, lenXZ);
//        
//        // We don't need rotation around Z-axis for aligning the normal
//        rotZ = 0;
//        
//        // Create the transform
//        Transform transform = new Transform()
//            .rotX(Math.toDegrees(-rotX))
//            .rotY(Math.toDegrees(-rotY))
//            .rotZ(Math.toDegrees(-rotZ))
//            .translate(point.x, point.y, point.z);
//        
//        return transform;
//    }
    private Transform createTransform(Vector3d normal, Vector3d point) {
       double rotz=Math.toDegrees(-Math.atan2(normal.y, normal.x));

		double x = Math.sqrt(Math.pow(normal.x, 2) +Math.pow(normal.y, 2) );
		double z = normal.z;
		double roty=Math.atan2(z,x);

        double degrees = 0;//Math.toDegrees(roty);
        if(degrees>90||degrees<-90) {
        	System.out.println("ERROR Angle impossible!");
        }
		return TransformFactory.nrToCSG(
        		new TransformNR(point.x,point.y,point.z,
        				new RotationNR(
        						0,
        						rotz,
        						degrees)));
    }
}
