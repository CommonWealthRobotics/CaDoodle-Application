package com.commonwealthrobotics;

import java.util.ArrayList;
import java.util.Arrays;
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
import eu.mihosoft.vrl.v3d.CSGClient;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.Transform;
import eu.mihosoft.vrl.v3d.Vector3d;
import eu.mihosoft.vrl.v3d.ext.quickhull3d.HullUtil;
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
import javafx.scene.DepthTest;

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

	//private ArrayList<Rectangle> show = new ArrayList<>();
	private EventHandler<? super MouseEvent> eventFilter;
	private CSG selection;
	private boolean start = false;

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
		getSelectionPlane().getTransforms().addAll(dottedLine);

		getSelectionPlane().setViewOrder(-1);
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
		if(!start)
			return;
		if(!event.isPrimaryButtonDown())
			return;
		if(event.isControlDown()||event.isShiftDown())
			return;
		if(event.getSource()!=getSelectionPlane())
			return;
		if (!engine.contains(rect)) {
			engine.addUserNode(rect);
			active = false;
		}
		Object source = event.getSource();
		if (source == engine.getSubScene())
			return;
		if (!active) {
			TransformNR cf = getCameraFrame();
//			ap.get().setWorkplane(cf);
//			workplane.placeWorkplaneVisualization();
			xStart = event.getX();
			yStart = event.getY();
			xLeft = xStart;
			yTop = yStart;
			com.neuronrobotics.sdk.common.Log.debug("Select Box Started x=" + xStart + " y=" + yStart);
			rect.setX(xStart);
			rect.setY(yStart);
			rect.setVisible(true);
			rect.setFill(Color.TRANSPARENT);
			rect.setStroke(Color.RED);
			rect.setStrokeWidth(2);
			rect.getStrokeDashArray().setAll(5.0, 5.0);
			rect.setWidth(0);
			rect.setHeight(0);
			rect.setViewOrder(-2);
			rect.setDepthTest(DepthTest.DISABLE);

			//show.clear();
			HashMap<CSG, MeshView> meshes = session.getMeshes();
			for(CSG key:meshes.keySet()) {
				MeshView mv= meshes.get(key);
				mv.setMouseTransparent(true);
			}
			
			active = true;
		}
		
		// Auto-generated method stub
		double width = Math.abs(event.getX() - xStart);
		double height = Math.abs(event.getY() - yStart);
		// com.neuronrobotics.sdk.common.Log.debug("Select Box Dragging x="+xEnd+" y="+yEnd);
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
//		for (Rectangle r : show) {
//			engine.removeUserNode(r);
//		}
		HashMap<CSG, MeshView> meshes = session.getMeshes();
		for(CSG key:meshes.keySet()) {
			MeshView mv= meshes.get(key);
			mv.setMouseTransparent(false);
		}
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
			start=false;
//			yTop=yTop-view3d.getHeight()/2;
//			xLeft=xLeft-(view3d.getWidth()/2);
//			ap.get().setWorkplane(cf);
//			workplane.placeWorkplaneVisualization();

			Bounds sel = new Bounds(new Vector3d(xLeft, yTop, 0), new Vector3d(xLeft + width, yTop + height));
			Transform boxFrame = TransformFactory.nrToCSG(getBoxFrame());
//			if(selection!=null)
//				BowlerStudio.runLater(()-> engine.removeObject(selection));
			selection = HullUtil.hull(Arrays.asList(
					new Vector3d(sel.getMaxX(),sel.getMaxY(),0).transform(boxFrame),
					new Vector3d(sel.getMaxX(),sel.getMinY(),0).transform(boxFrame),
					new Vector3d(sel.getMinX(),sel.getMaxY(),0).transform(boxFrame),
					new Vector3d(sel.getMinX(),sel.getMinY(),0).transform(boxFrame),
					new Vector3d(0,0,0).transform(TransformFactory.nrToCSG(getCameraFrame()))
					));
			
			if(selection==null)
				throw new RuntimeException("Selection can not be null");
			//selection.setManipulator(dottedLine);
			selection.setColor(new Color(0.25, 0.25, 0, 0.25));
			//engine.addCsg(selection, null);
			List<String> overlapping = checkOverlap(selection);
			session.selectAll(overlapping);
		}).start();
	}

	private TransformNR getCameraFrame() {
		VirtualCameraMobileBase camera = engine.getFlyingCamera();
		double zoom = camera.getZoomDepth() ;
		TransformNR cf = engine.getFlyingCamera().getCamerFrame().times(new TransformNR(0, 0, zoom));
		return cf;
	}
	private TransformNR getBoxFrame() {
		VirtualCameraMobileBase camera = engine.getFlyingCamera();
		double zoom = camera.getZoomDepth() /2;
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
	public boolean touching(CSG selection2,Bounds incoming) {
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
		ArrayList<CSG> visable = session.getSelectable();
		CSGClient c= CSGClient.getClient();
		if(c!=null)
			CSGClient.setClient(null);
		for (CSG key : visable) {
			// Check if boxes overlap
			if(key.hasManipulator()) {
				key=key.transformed(TransformFactory.affineToCSG(key.getManipulator()));
			}
			if (key.touching(selection2)) {
				overlapping.add(key.getName());
			}
		}
		CSGClient.setClient(c);
		// Return true if any overlaps found
		return overlapping;
	}

	public void setWorkplaneManager(WorkplaneManager workplane2) {
		workplane = workplane2;
	}

	public void setPressEvent(EventHandler< MouseEvent> value) {
		getSelectionPlane().addEventFilter(MouseEvent.MOUSE_PRESSED,event->{
			if(event.getSource()!=getSelectionPlane())
				return;
			com.neuronrobotics.sdk.common.Log.debug("Selection Box Background Click ");
			start=true;
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
