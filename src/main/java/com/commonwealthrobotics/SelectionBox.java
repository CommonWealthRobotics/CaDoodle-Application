package com.commonwealthrobotics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.VirtualCameraMobileBase;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.event.EventHandler;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;

public class SelectionBox {

	private double xStart;
	private double yStart;
	private double xLeft;
	private double yTop;
	private Rectangle rect = new Rectangle();
	private AnchorPane view3d;
	private BowlerStudio3dEngine engine;
	private WorkplaneManager workplane;
	private SelectionSession session;
	private ActiveProject ap;
	private boolean active = false;
	private MeshView selectionPlane;
	private Affine wpPickPlacement = new Affine();
	private Affine dottedLine = new Affine();

	private ArrayList<Rectangle> show = new ArrayList<>();
	private EventHandler<? super MouseEvent> eventFilter;

	public SelectionBox(SelectionSession session, AnchorPane view3d, BowlerStudio3dEngine engine, ActiveProject ap) {
		this.session = session;
		this.view3d = view3d;
		this.engine = engine;
		this.ap = ap;
		rect.setVisible(false);
		setSelectionPlane(new Cube(20000, 20000, 0.001).toCSG().newMesh());
		PhongMaterial material = new PhongMaterial();

		// material.setDiffuseMap(texture);
		material.setDiffuseColor(new Color(0.25, 0.25, 0, 0));
		getSelectionPlane().setCullFace(CullFace.BACK);
		getSelectionPlane().setMaterial(material);
		getSelectionPlane().setOpacity(0.0025);
		getSelectionPlane().getTransforms().addAll(wpPickPlacement);

		getSelectionPlane().setViewOrder(-2);
		getSelectionPlane().addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
			if (event.isPrimaryButtonDown())
				dragged(event);
		});
		getSelectionPlane().addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
			released(event);
		});
		rect.getTransforms().addAll(dottedLine);
		eventFilter = event -> {
			// if (event.isPrimaryButtonDown())
			dragged(event);
		};
		engine.getSubScene().addEventFilter(MouseEvent.MOUSE_DRAGGED, eventFilter);

		engine.addUserNode(getSelectionPlane());
	}

	public void cancle() {
		//engine.removeUserNode(selectionPlane);
	}

	public void activate(MouseEvent event) {

	}
	public void onCameraChange(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z) {
		TransformFactory.nrToAffine(getCameraFrame(), wpPickPlacement);
		TransformFactory.nrToAffine(getBoxFrame(), dottedLine);

	}
	public void dragged(MouseEvent event) {
		if(!event.isPrimaryButtonDown())
			return;
		if (!engine.contains(rect)) {
			engine.addUserNode(rect);
			active = false;
		}
		Object source = event.getSource();
		if (source == engine.getSubScene())
			return;
		if (!active) {
			xStart = event.getX();
			yStart = event.getY();
			xLeft = xStart;
			yTop = yStart;
			System.out.println("Select Box Started x=" + xStart + " y=" + yStart);
			rect.setX(xStart);
			rect.setY(yStart);
			rect.setVisible(true);
			rect.setFill(Color.TRANSPARENT);
			rect.setStroke(Color.RED);
			rect.setStrokeWidth(2);
			rect.getStrokeDashArray().addAll(5.0, 5.0);
			rect.setWidth(0);
			rect.setHeight(0);
			rect.setViewOrder(-2);
			active = true;
		}
		for (Rectangle r : show) {
			view3d.getChildren().remove(r);
		}
		show.clear();
		// Auto-generated method stub
		double width = Math.abs(event.getX() - xStart);
		double height = Math.abs(event.getY() - yStart);
		// System.out.println("Select Box Dragging x="+xEnd+" y="+yEnd);
		if (event.getX() < xStart) {
			xLeft = event.getX();
			rect.setX(event.getX());

		}
		if (event.getY() < yStart) {
			yTop = event.getY();
			rect.setY(event.getY());
		}

		rect.setWidth(width);
		rect.setHeight(height);
	}

	public void released(MouseEvent event) {
//		rect.setVisible(false);
//		view3d.getChildren().remove(rect);	
		engine.removeUserNode(rect);
		//engine.removeUserNode(selectionPlane);
		new Thread(() -> {
			double width = Math.abs(event.getX() - xStart);
			double height = Math.abs(event.getY() - yStart);
			if (!active)
				return;
			if (width < 2)
				return;
			if (height < 2)
				return;

			active = false;
//			yTop=yTop-view3d.getHeight()/2;
//			xLeft=xLeft-(view3d.getWidth()/2);
			TransformNR cf = getCameraFrame();

			ArrayList<CSG> visable = session.getAllVisable();
			HashMap<CSG, Bounds> cache = new HashMap<>();
			for (CSG c : visable)
				session.getBounds(c, cf, cache);
			Bounds sel = new Bounds(new Vector3d(xLeft, yTop, 0), new Vector3d(xLeft + width, yTop + width));
			List<String> overlapping = checkOverlap(sel, cache);
			session.selectAll(overlapping);
		}).start();
	}

	private TransformNR getCameraFrame() {
		VirtualCameraMobileBase camera = engine.getFlyingCamera();
		double zoom = camera.getZoomDepth() /2;
		TransformNR cf = engine.getFlyingCamera().getCamerFrame().times(new TransformNR(0, 0, 500));
		return cf;
	}
	private TransformNR getBoxFrame() {
		VirtualCameraMobileBase camera = engine.getFlyingCamera();
		double zoom = camera.getZoomDepth() /2;
		TransformNR cf = engine.getFlyingCamera().getCamerFrame().times(new TransformNR(0, 0, 500));
		return cf;
	}

	public List<String> checkOverlap(Bounds sel, Map<CSG, Bounds> cache) {
		List<String> overlapping = new ArrayList<>();

		for (CSG key : cache.keySet()) {
			Bounds bounds = cache.get(key);

			// Get coordinates of selection box
			double selMaxX = sel.getMaxX();
			double selMaxY = sel.getMaxY();
			double selMinX = sel.getMinX();
			double selMinY = sel.getMinY();

			// Get coordinates of current box
			double boxMaxX = bounds.getMaxX();
			double boxMaxY = bounds.getMaxY();
			double boxMinX = bounds.getMinX();
			double boxMinY = bounds.getMinY();

			// Check if boxes overlap
			if (!(selMaxX < boxMinX || selMinX > boxMaxX || selMaxY < boxMinY || selMinY > boxMaxY)) {
				overlapping.add(key.getName());
			}
		}

		// Return true if any overlaps found
		return overlapping;
	}

	public void setWorkplaneManager(WorkplaneManager workplane2) {
		workplane = workplane2;
	}

	public void setPressEvent(EventHandler< MouseEvent> value) {
		getSelectionPlane().addEventFilter(MouseEvent.MOUSE_PRESSED,event->{
			System.out.println("Background Click ");
			value.handle(event);
		});
	}

	public MeshView getSelectionPlane() {
		return selectionPlane;
	}

	public void setSelectionPlane(MeshView selectionPlane) {
		this.selectionPlane = selectionPlane;
	}

}
