package com.commonwealthrobotics.controls;

import com.neuronrobotics.bowlerstudio.physics.TransformFactory;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;

public class DottedLine extends Group {
    private final DoubleProperty startX = new SimpleDoubleProperty();
    private final DoubleProperty startY = new SimpleDoubleProperty();
    private final DoubleProperty startZ = new SimpleDoubleProperty();
    private final DoubleProperty endX = new SimpleDoubleProperty();
    private final DoubleProperty endY = new SimpleDoubleProperty();
    private final DoubleProperty endZ = new SimpleDoubleProperty();
    
    private final double dotRadius;
    private final double dotSpacing;
    private Color dotColor = Color.BLACK;
	private Affine workplaneOffset;

    public DottedLine(double dotRadius, double dotSpacing, Affine workplaneOffset) {
        this.dotRadius = dotRadius;
        this.dotSpacing = dotSpacing;
		this.workplaneOffset = workplaneOffset;
        startX.addListener((obs, oldVal, newVal) -> updateLine());
        startY.addListener((obs, oldVal, newVal) -> updateLine());
        startZ.addListener((obs, oldVal, newVal) -> updateLine());
        endX.addListener((obs, oldVal, newVal) -> updateLine());
        endY.addListener((obs, oldVal, newVal) -> updateLine());
        endZ.addListener((obs, oldVal, newVal) -> updateLine());
    }

    private void updateLine() {
        getChildren().clear();

        double dx = endX.get() - startX.get();
        double dy = endY.get() - startY.get();
        double dz = endZ.get() - startZ.get();
        double length = Math.sqrt(dx*dx + dy*dy + dz*dz);

        int numDots = (int) Math.floor(length / dotSpacing);
        if(numDots==0)
        	return;

        for (int i = 0; i <= numDots; i++) {
            double t = i / (double) numDots;
            double x = startX.get() + t * dx;
            double y = startY.get() + t * dy;
            double z = startZ.get() + t * dz;

            Sphere dot = new Sphere(dotRadius);
            Affine loc= TransformFactory.newAffine(x, y, z);
            dot.getTransforms().add(workplaneOffset);
            dot.getTransforms().addAll(loc);
            
            PhongMaterial material = new PhongMaterial(dotColor);
            dot.setMaterial(material);

            getChildren().add(dot);
        }
      

    }

    // Mimic Line class API
    public void setStartX(double value) { startX.set(value); }
    public double getStartX() { return startX.get(); }
    public DoubleProperty startXProperty() { return startX; }

    public void setStartY(double value) { startY.set(value); }
    public double getStartY() { return startY.get(); }
    public DoubleProperty startYProperty() { return startY; }

    public void setStartZ(double value) { startZ.set(value); }
    public double getStartZ() { return startZ.get(); }
    public DoubleProperty startZProperty() { return startZ; }

    public void setEndX(double value) { endX.set(value); }
    public double getEndX() { return endX.get(); }
    public DoubleProperty endXProperty() { return endX; }

    public void setEndY(double value) { endY.set(value); }
    public double getEndY() { return endY.get(); }
    public DoubleProperty endYProperty() { return endY; }

    public void setEndZ(double value) { endZ.set(value); }
    public double getEndZ() { return endZ.get(); }
    public DoubleProperty endZProperty() { return endZ; }

    public void setStroke(Color color) {
        this.dotColor = color;
        updateLine();
    }
}