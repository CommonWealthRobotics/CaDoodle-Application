package com.commonwealthrobotics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.VirtualCameraMobileBase;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;


import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.CSGClient;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.MissingManipulatorException;
import eu.mihosoft.vrl.v3d.Transform;
import eu.mihosoft.vrl.v3d.Vector3d;
import eu.mihosoft.vrl.v3d.ext.quickhull3d.HullUtil;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.layout.Pane;
import javafx.geometry.Point2D;

public class SelectionBox {

	private double xStart;
	private double yStart;
	private double xStart2;
	private double yStart2;
	private double xLeft;
	private double yTop;
	private Rectangle rect1 = new Rectangle();
	private Rectangle rect2 = new Rectangle();
	private AnchorPane view3d;
	private BowlerStudio3dEngine engine;
	private WorkplaneManager workplane;
	private SelectionSession session;
	private ActiveProject ap;
	private boolean active = false;
	private MeshView selectionPlane;
	private Affine wpPickPlacement = new Affine();
	private Affine dottedLine = new Affine();

	// private ArrayList<Rectangle> show = new ArrayList<>();
	private EventHandler<? super MouseEvent> eventFilter;
	private CSG selection;
	private boolean start = false;
	private Pane overlayPane2D; // 2D overlay pane

	public SelectionBox(SelectionSession session, AnchorPane view3d, BowlerStudio3dEngine engine, ActiveProject ap,
			Pane paneOverlay2D) {
		this.session = session;
		this.view3d = view3d;
		this.engine = engine;
		this.ap = ap;
		this.overlayPane2D = paneOverlay2D; // keep reference to 2D pane
		rect1.setVisible(false);
		rect2.setVisible(false);
		setSelectionPlane(new Cube(20000, 20000, 0.001).toCSG().newMesh());
		PhongMaterial material = new PhongMaterial();

		// material.setDiffuseMap(texture);
		material.setDiffuseColor(new Color(0.25, 0.25, 0, 0));
		getSelectionPlane().setCullFace(CullFace.BACK);
		getSelectionPlane().setMaterial(material);
		getSelectionPlane().setOpacity(0.0025);
		getSelectionPlane().getTransforms().addAll(dottedLine);

		getSelectionPlane().setViewOrder(-1);

		getSelectionPlane().addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
			if (event.isPrimaryButtonDown())
				dragged(event);
		});

		getSelectionPlane().addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
			released(event);
		});

		eventFilter = event -> {
			// if (event.isPrimaryButtonDown())
			dragged(event);
		};
		engine.getSubScene().addEventFilter(MouseEvent.MOUSE_DRAGGED, eventFilter);

		engine.addUserNode(getSelectionPlane());
	}

	public void cancle() {
		// engine.removeUserNode(selectionPlane);
	}

	public void activate(MouseEvent event) {

	}

	public void onCameraChange(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z) {
		TransformFactory.nrToAffine(getCameraFrame(), wpPickPlacement);
		TransformFactory.nrToAffine(getBoxFrame(), dottedLine);
	}

	public void dragged(MouseEvent event) {
		if (!start || !event.isPrimaryButtonDown() || event.isControlDown() || event.isShiftDown()
				|| (event.getSource() != getSelectionPlane()))
			return;

		if (!overlayPane2D.getChildren().contains(rect1)) {
			overlayPane2D.getChildren().add(rect1);
			overlayPane2D.getChildren().add(rect2);
			active = false;
		}

		Object source = event.getSource();
		if (source == engine.getSubScene())
			return;

		if (!active) {
			active = true;
			TransformNR cf = getCameraFrame();
//			ap.get().setWorkplane(cf);
//			workplane.placeWorkplaneVisualization();

			xLeft = event.getX();
			yTop = event.getY();
			xStart2 = event.getX();
			yStart2 = event.getY();

			com.neuronrobotics.sdk.common.Log.debug("Selection Box Started at x=" + (int) xLeft + " y=" + (int) yTop);

			Point2D scenePt = event.getPickResult().getIntersectedNode().localToScene(event.getX(), event.getY());
			Point2D p = overlayPane2D.screenToLocal(event.getScreenX(), event.getScreenY());
			if (p == null)
				return;

			xStart = p.getX();
			yStart = p.getY();

			double strokeWidth = 4.0;
			rect1.setFill(Color.TRANSPARENT); // Only outline
			rect1.setStroke(Color.BLACK);
			rect1.setStrokeWidth(strokeWidth);
			rect1.getStrokeDashArray().setAll(strokeWidth, 5 * strokeWidth);
			rect1.setStrokeLineCap(StrokeLineCap.BUTT);
			rect1.setStrokeLineJoin(StrokeLineJoin.MITER);
			rect1.setSmooth(false);

			rect2.setFill(Color.TRANSPARENT); // Only outline
			rect2.setStroke(Color.RED);
			rect2.setStrokeWidth(strokeWidth);
			rect2.getStrokeDashArray().setAll(strokeWidth, 5 * strokeWidth);
			rect2.setStrokeDashOffset(3 * strokeWidth); // Misalign dash pattern
			rect2.setStrokeLineCap(StrokeLineCap.BUTT);
			rect2.setStrokeLineJoin(StrokeLineJoin.MITER);
			rect2.setSmooth(false);

			// show.clear();
			HashMap<CSG, MeshView> meshes = session.getMeshes();
			for (CSG key : meshes.keySet()) {
				MeshView mv = meshes.get(key);
				mv.setMouseTransparent(true);
			}
		}

		Point2D cur = overlayPane2D.screenToLocal(event.getScreenX(), event.getScreenY());
		if (cur == null)
			return;

		double width = Math.abs(cur.getX() - xStart);
		double height = Math.abs(cur.getY() - yStart);

		if (event.getX() < xStart)
			xLeft = event.getX();

		if (event.getY() < yStart)
			yTop = event.getY();

		rect1.setX(Math.min(xStart, cur.getX()));
		rect1.setY(Math.min(yStart, cur.getY()));
		rect1.setWidth(width);
		rect1.setHeight(height);
		rect1.setVisible(true);

		rect2.setX(Math.min(xStart, cur.getX()));
		rect2.setY(Math.min(yStart, cur.getY()));
		rect2.setWidth(width);
		rect2.setHeight(height);
		rect2.setVisible(true);
	}

	public void released(MouseEvent event) {
//		rect.setVisible(false);
//		view3d.getChildren().remove(rect);	
//		engine.removeUserNode(rect);
		overlayPane2D.getChildren().remove(rect1);
		overlayPane2D.getChildren().remove(rect2);
//		for (Rectangle r : show) {
//			engine.removeUserNode(r);
//		}
		HashMap<CSG, MeshView> meshes = session.getMeshes();
		for (CSG key : meshes.keySet()) {
			MeshView mv = meshes.get(key);
			mv.setMouseTransparent(false);
		}

		new Thread(() -> {
			double width = Math.abs(event.getX() - xStart2);
			double height = Math.abs(event.getY() - yStart2);

			if (xLeft > xStart2)
				xLeft = xStart2;

			if (yTop < yStart2)
				yTop = yStart2;

			if ((!active) || (width < 2) || (height < 2))
				return;

			active = false;
			start = false;
//			yTop=yTop-view3d.getHeight()/2;
//			xLeft=xLeft-(view3d.getWidth()/2);
//			ap.get().setWorkplane(cf);
//			workplane.placeWorkplaneVisualization();

			Bounds sel = new Bounds(new Vector3d(xLeft, yTop, 0), new Vector3d(xLeft + width, yTop - height));
			Transform boxFrame = TransformFactory.nrToCSG(getBoxFrame());
//			if (selection != null)
//				BowlerStudio.runLater(()-> engine.removeObject(selection));
			selection = HullUtil.hull(Arrays.asList(new Vector3d(sel.getMaxX(), sel.getMaxY(), 0).transform(boxFrame),
					new Vector3d(sel.getMaxX(), sel.getMinY(), 0).transform(boxFrame),
					new Vector3d(sel.getMinX(), sel.getMaxY(), 0).transform(boxFrame),
					new Vector3d(sel.getMinX(), sel.getMinY(), 0).transform(boxFrame),
					new Vector3d(0, 0, 0).transform(TransformFactory.nrToCSG(getCameraFrame()))));

			if (selection == null)
				throw new RuntimeException("Selection can not be null");

			// selection.setManipulator(dottedLine);
			selection.setColor(new Color(0.25, 0.25, 0, 0.25));
			// engine.addCsg(selection, null);
			List<String> overlapping = checkOverlap(selection);
			session.selectAll(overlapping);
		}).start();
	}

	private TransformNR getCameraFrame() {
		VirtualCameraMobileBase camera = engine.getFlyingCamera();
		double zoom = camera.getZoomDepth();
		TransformNR cf = engine.getFlyingCamera().getCamerFrame().times(new TransformNR(0, 0, zoom));
		return cf;
	}

	private TransformNR getBoxFrame() {
		VirtualCameraMobileBase camera = engine.getFlyingCamera();
		double zoom = camera.getZoomDepth() / 2;
		TransformNR cf = engine.getFlyingCamera().getCamerFrame().times(new TransformNR(0, 0, 500));
		return cf;
	}

	/**
	 * A test to see if 2 CSG's are touching. The fast-return is a bounding box
	 * check If bounding boxes overlap, then an intersection is performed and the
	 * existance of an interscting object is returned
	 * 
	 * @param incoming
	 * @return
	 */
	public boolean touching(CSG selection2, Bounds incoming) {
		// Fast bounding box overlap check, quick fail if not intersecting
		// bounding boxes
		if (selection2.getMaxX() > incoming.getMinX() && selection2.getMinX() < incoming.getMaxX()
				&& selection2.getMaxY() > incoming.getMinY() && selection2.getMinY() < incoming.getMaxY()
				&& selection2.getMaxZ() > incoming.getMinZ() && selection2.getMinZ() < incoming.getMaxZ()) {
			return true;
		}
		return false;
	}

	public List<String> checkOverlap(CSG selection2) {
		List<String> overlapping = new ArrayList<>();
		if (selection2.getPolygons().size() < 5)
			return overlapping;
		ArrayList<CSG> visible = session.getSelectable();
		CSGClient c = CSGClient.getClient();
		if (c != null)
			CSGClient.setClient(null);

		for (CSG key : visible) {
			// Check if boxes overlap
			if (key.hasManipulator()) {
				try {
					key = key.transformed(TransformFactory.affineToCSG(key.getManipulator()));
				} catch (MissingManipulatorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (key.getPolygons().size() > 4) {
				try {
					boolean touching = key.touching(selection2);
					if (touching)
						overlapping.add(key.getName());
				} catch (Exception ex) {
					com.neuronrobotics.sdk.common.Log.error(ex);
				}
			}

		}
		CSGClient.setClient(c);
		// Return true if any overlaps found
		return overlapping;
	}

	public void setWorkplaneManager(WorkplaneManager workplane2) {
		workplane = workplane2;
	}

	public void setPressEvent(EventHandler<MouseEvent> value) {
		getSelectionPlane().addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			if (event.getSource() != getSelectionPlane())
				return;
			com.neuronrobotics.sdk.common.Log.debug("Selection Box Background Click ");
			start = true;
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
