package com.commonwealthrobotics;

import com.commonwealthrobotics.controls.SelectionSession;

import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class SelectionBox {

	private double xStart;
	private double yStart;
	private Rectangle rect = new Rectangle();

	public SelectionBox(SelectionSession session, AnchorPane view3d) {
		rect.setVisible(false);
		view3d.getChildren().add(rect);
		
	}

	public void activate(MouseEvent event) {
		xStart = event.getX();
		yStart = event.getY();
		System.out.println("Select Box Started x="+xStart+" y="+yStart);
		AnchorPane.setLeftAnchor(rect, xStart);
        AnchorPane.setTopAnchor(rect, yStart);
        rect.setX(xStart);
        rect.setY(yStart);
		rect.setVisible(true);
		rect.setFill(Color.TRANSPARENT);
		rect.setStroke(Color.RED);
		rect.setStrokeWidth(2);
		rect.getStrokeDashArray().addAll(5.0, 5.0);
		rect.setWidth(0);
		rect.setHeight(0);
	}

	public void dragged(MouseEvent event) {
		//  Auto-generated method stub
		double width = Math.abs(event.getX()-xStart);
		double height = Math.abs(event.getY()-yStart);
		//System.out.println("Select Box Dragging x="+xEnd+" y="+yEnd);
		//if(event.getScreenX()<xStart)
		rect.setWidth(width);
		rect.setHeight(height);
	}

	public void released(MouseEvent event) {

		double width = Math.abs(event.getX()-xStart);
		double height = Math.abs(event.getY()-yStart);
		//System.out.println("Select Box Ended x="+xEnd+" y="+yEnd);
		rect.setVisible(false);
	}

}
