package com.commonwealthrobotics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.BowlerKernel;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.Log;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.ColinearPointsException;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.Cylinder;
import eu.mihosoft.vrl.v3d.Polygon;
import eu.mihosoft.vrl.v3d.Transform;
import eu.mihosoft.vrl.v3d.Vector3d;
import eu.mihosoft.vrl.v3d.ext.org.poly2tri.PolygonUtil;
import eu.mihosoft.vrl.v3d.ext.quickhull3d.HullUtil;
import javafx.collections.ObservableFloatArray;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;

public class WorkplaneManager implements EventHandler<MouseEvent> {

	private MeshView ground;
	private MeshView wpPick;
	private HashMap<CSG, MeshView> meshes;
	private HashMap<MeshView,CSG> meshesReverseLookup;
	private BowlerStudio3dEngine engine;
	private Affine workplaneLocation = new Affine();
	private MeshView indicatorMesh;
	private TransformNR currentAbsolutePose;
	private Runnable onSelectEvent = () -> {
	};
	private boolean clickOnGround = false;
	private boolean clicked = false;
	private boolean active;
	private Affine wpPickPlacement = new Affine();
	private SelectionSession session;
	private boolean tempory;
	private ActiveProject ap;
	private double increment = 1.0;
	private IWorkplaneUpdate updater = null;
	private Runnable onCancel;

	public WorkplaneManager(ActiveProject ap, MeshView ground, BowlerStudio3dEngine engine, SelectionSession session) {

		this.ap = ap;
		this.ground = ground;
		this.engine = engine;
		this.session = session;
		wpPick = new Cube(200, 200, 0.001).toCSG().newMesh();
		PhongMaterial material = new PhongMaterial();

		// material.setDiffuseMap(texture);
		material.setDiffuseColor(new Color(0.25, 0.25, 0, 0.0025));
		wpPick.setCullFace(CullFace.BACK);
		wpPick.setMaterial(material);
		wpPick.setOpacity(0.25);
		wpPick.getTransforms().addAll(wpPickPlacement);
		Group linesGroupp = new Group();
		linesGroupp.setDepthTest(DepthTest.ENABLE);
		linesGroupp.setViewOrder(-1); // Lower viewOrder renders on top
		linesGroupp.getChildren().add(wpPick);

		ground.addEventFilter(MouseEvent.MOUSE_PRESSED, ev -> {
			// com.neuronrobotics.sdk.common.Log.error("Ground Click!");
			setClickOnGround(true);
		});

		engine.addUserNode(linesGroupp);
		ground.setMouseTransparent(true);
		wpPick.setMouseTransparent(true);
		ground.setVisible(false);
	}

	public void setIndicator(CSG indicator, Affine centerOffset) {
		if (indicatorMesh != null) {
			engine.removeUserNode(indicatorMesh);
		}
		indicatorMesh = indicator.newMesh();
		indicatorMesh.getTransforms().addAll(getWorkplaneLocation(), centerOffset);
		indicatorMesh.setMouseTransparent(true);

		PhongMaterial material = new PhongMaterial();

		if (indicator.isHole()) {
			// material.setDiffuseMap(texture);
			material.setDiffuseColor(new Color(0.25, 0.25, 0.25, 0.75));
			material.setSpecularColor(javafx.scene.paint.Color.WHITE);
		} else {
			Color c = indicator.getColor();
			material.setDiffuseColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 0.75));
			material.setSpecularColor(javafx.scene.paint.Color.WHITE);
		}
		indicatorMesh.setMaterial(material);
		engine.addUserNode(indicatorMesh);
	}

	public void updateMeshes(HashMap<CSG, MeshView> meshes) {
		this.meshes = meshes;
		meshesReverseLookup=new HashMap<MeshView, CSG>();
		for(CSG c:meshes.keySet()) {
			meshesReverseLookup.put(meshes.get(c), c);
		}
	}

	public void cancel() {
		if (!active)
			return;
		updater=null;
		ground.removeEventFilter(MouseEvent.ANY, this);
		wpPick.removeEventFilter(MouseEvent.ANY, this);
		wpPick.setVisible(isWorkplaneNotOrigin());
		if(meshes!=null)
		for (CSG key : meshes.keySet()) {
			MeshView mv = meshes.get(key);
			mv.removeEventFilter(MouseEvent.ANY, this);
		}
		if (indicatorMesh != null)
			indicatorMesh.setVisible(false);
		indicatorMesh = null;
		if (onSelectEvent != null)
			onSelectEvent.run();
		onSelectEvent = null;
		active = false;
		ground.setMouseTransparent(true);
		ground.setVisible(false);
		if(onCancel!=null) {
			onCancel.run();
			onCancel=null;
		}
	}
	public void activate() {
		activate(true);
	}
	public void activate(boolean enableGroundPick) {
		active = true;
		tempory=false;
		setClickOnGround(false);
		clicked = false;
		com.neuronrobotics.sdk.common.Log.error("Starting workplane listeners");
		ground.addEventFilter(MouseEvent.ANY, this);
		ground.setMouseTransparent(false);
		wpPick.setMouseTransparent(false);
		ground.setVisible(enableGroundPick);
		wpPick.addEventFilter(MouseEvent.ANY, this);
		wpPick.setVisible(isWorkplaneNotOrigin());
		if(meshes!=null)
		for (CSG key : meshes.keySet()) {
			MeshView mv = meshes.get(key);
			mv.addEventFilter(MouseEvent.ANY, this);
		}
		if (indicatorMesh != null)
			indicatorMesh.setVisible(true);
	}

	@Override
	public void handle(MouseEvent ev) {
		PickResult pickResult = ev.getPickResult();
		Node intersectedNode = pickResult.getIntersectedNode();
		if (ev.getEventType() == MouseEvent.MOUSE_PRESSED) {
			clicked = true;
			onCancel=null;// non cancles but instead completed
			cancel();
			ev.consume();
			session.updateControls();
			ground.setMouseTransparent(true);
			wpPick.setMouseTransparent(true);
			ground.setVisible(false);
			return;
		}
		if (ev.getEventType() == MouseEvent.MOUSE_MOVED || ev.getEventType() == MouseEvent.MOUSE_DRAGGED) {
			// com.neuronrobotics.sdk.common.Log.error(ev);
			Point3D intersectedPoint = pickResult.getIntersectedPoint();
			double x = intersectedPoint.getX();
			double y = intersectedPoint.getY();
			double z = intersectedPoint.getZ();
			if (ev.getSource() == ground) {
				x *= MainController.groundScale();
				y *=  MainController.groundScale();
				z *=  MainController.groundScale();
			}

			
			TransformNR screenLocation;
			TransformNR pureRot=null;
			Affine manipulator =new Affine();
			CSG source=null;
			if (intersectedNode instanceof MeshView) {
				MeshView meshView = (MeshView) intersectedNode;
				if(meshesReverseLookup!=null) {
					source = meshesReverseLookup.get(meshView);
					if(source!=null)
						if(source.getManipulator()!=null)
							manipulator=source.getManipulator();
				}
				
				TriangleMesh mesh = (TriangleMesh) meshView.getMesh();

				int faceIndex = pickResult.getIntersectedFace();

				if (faceIndex >= 0) {
					if(source!=null) {
						ArrayList<Polygon> polygons = source.getPolygons();
						Polygon p =  getPolygonFromFaceIndex(faceIndex,polygons);
						if(p!=null) {
							try {
								pureRot=TransformFactory.csgToNR(PolygonUtil.calculateNormalTransform(p)).inverse();
								// an in-plane snapping here by transforming the points
								// into the plane orentation, then snapping in plane, then transforming the points back. 
								TransformNR manipulatorNR=TransformFactory.affineToNr(manipulator);
								TransformNR t = new TransformNR(x, y, z);
								TransformNR screenLocationtmp = manipulatorNR.times(t);
								Polygon np = p.transformed(TransformFactory.affineToCSG(manipulator));
								Transform npTF =PolygonUtil.calculateNormalTransform(np);
								TransformNR npTFNR = TransformFactory.csgToNR(npTF);
								Polygon flattened = np.transformed(npTF);
								TransformNR flattenedTouch = npTFNR.times(screenLocationtmp);
								//Log.debug("Polygon "+flattened);
								//Log.debug("Point "+flattenedTouch.toSimpleString());
								TransformNR adjusted = new TransformNR(
										SelectionSession.roundToNearist(flattenedTouch.getX(),increment),// snap in plane
										SelectionSession.roundToNearist(flattenedTouch.getY(),increment),
										flattened.getPoints().get(0).z);// adhere to the plane of the polygon
								TransformNR adjustedBack = npTFNR.inverse().times(adjusted);// flip the point back to its original orentaation in the plane post snap
								x=adjustedBack.getX();
								y=adjustedBack.getY();
								z=adjustedBack.getZ();
								
								//Log.debug("Polygon snapped "+adjusted);
							} catch (Exception e) {
								Log.error(e);
							}

							
						}else
							Log.error("Polygon not found " + faceIndex);
					}else {
						x=SelectionSession.roundToNearist(x,increment);
						y=SelectionSession.roundToNearist(y,increment);
						z=SelectionSession.roundToNearist(z,increment);
					}
					if(pureRot==null) {
						pureRot = getFaceNormalAngles(mesh, faceIndex).inverse();
					}
				}else
					Log.error("Error face index came back: " + faceIndex);
			}
			TransformNR manipulatorNR=TransformFactory.affineToNr(manipulator);
			TransformNR t = new TransformNR(x, y, z);
			screenLocation = manipulatorNR.times(t.times(pureRot));
			
			if (intersectedNode == wpPick) {
				if(updater!=null) {
					updater.setWorkplaneLocation(screenLocation);
				}
				screenLocation = ap.get().getWorkplane().times(screenLocation);
			}else{
				if(updater!=null) {
					updater.setWorkplaneLocation(ap.get().getWorkplane().inverse().times(screenLocation));
				}
			}
			setCurrentAbsolutePose(screenLocation);
			
		}
	}
	public static Polygon getPolygonFromFaceIndex(int faceIndex, List<Polygon> polygons) {
		if (faceIndex < 0) {
			return null;
		}
		
		int currentFaceCount = 0;
		
		// We need to iterate because some polygons might have < 3 vertices (0 faces)
		// If you're CERTAIN all polygons have >= 3 vertices, this could be optimized
		for (Polygon p : polygons) {
			int vertexCount = p.getVertices().size();
			if (vertexCount >= 3) {
				int facesInThisPolygon = vertexCount - 2;
				
				// Check if the face index falls within this polygon's range
				if (faceIndex < currentFaceCount + facesInThisPolygon) {
					return p;
				}
				
				currentFaceCount += facesInThisPolygon;
			}
		}
		
		return null;
	}
	private Vector3d toV(javafx.geometry.Point3D p) {
		return new Vector3d(p.getX(),p.getY(),p.getZ());
	}
	private TransformNR getFaceNormalAngles(TriangleMesh mesh, int faceIndex) {
		ObservableFaceArray faces = mesh.getFaces();
		ObservableFloatArray points = mesh.getPoints();
		//mesh.get

		int p1Index = faces.get(faceIndex * 6) * 3;
		int p2Index = faces.get(faceIndex * 6 + 2) * 3;
		int p3Index = faces.get(faceIndex * 6 + 4) * 3;

		Point3D p1 = new Point3D(points.get(p1Index), points.get(p1Index + 1), points.get(p1Index + 2));
		Point3D p2 = new Point3D(points.get(p2Index), points.get(p2Index + 1), points.get(p2Index + 2));
		Point3D p3 = new Point3D(points.get(p3Index), points.get(p3Index + 1), points.get(p3Index + 2));
		try {
			Polygon p = Polygon.fromPoints(Arrays.asList(toV(p1),toV(p2),toV(p3)));
			return TransformFactory.csgToNR(PolygonUtil.calculateNormalTransform(p));
		} catch (ColinearPointsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new TransformNR();
	}

	public TransformNR getCurrentAbsolutePose() {
		return currentAbsolutePose;
	}

	public void setCurrentAbsolutePose(TransformNR currentAbsolutePose) {
		this.currentAbsolutePose = currentAbsolutePose;

		TransformFactory.nrToAffine(getCurrentAbsolutePose(), getWorkplaneLocation());
	}

	public Runnable getOnSelectEvent() {
		return onSelectEvent;
	}

	public void setOnSelectEvent(Runnable onSelectEvent) {
		this.onSelectEvent = onSelectEvent;
	}

	public boolean isClickOnGround() {
		return clickOnGround;
	}

	public void setClickOnGround(boolean clickOnGround) {
		this.clickOnGround = clickOnGround;
	}

	public boolean isClicked() {
		return clicked;
	}

	public void pickPlane(Runnable r,Runnable always,RulerManager ruler) {
		double pointerLen = 10;
		double pointerWidth = 2;
		double pointerHeight = 20;
		CSG indicator = HullUtil
				.hull(Arrays.asList(new Vector3d(0, 0, 0), new Vector3d(pointerLen, 0, 0),
						new Vector3d(pointerWidth, pointerWidth, 0), new Vector3d(0, 0, pointerHeight)))
				.union(HullUtil.hull(Arrays.asList(new Vector3d(0, 0, 0), new Vector3d(0, pointerLen, 0),
						new Vector3d(pointerWidth, pointerWidth, 0), new Vector3d(0, 0, pointerHeight))))
				.setColor(Color.YELLOWGREEN);
		this.setIndicator(indicator, new Affine());
		ap.get().setWorkplane(new TransformNR());
		placeWorkplaneVisualization();
		this.setOnSelectEvent(() -> {
			if (this.isClicked()) {
				if (this.isClickOnGround()) {
					// com.neuronrobotics.sdk.common.Log.error("Ground plane click detected");
					ap.get().setWorkplane(new TransformNR());
					ruler.cancel();
				} else {
					ap.get().setWorkplane(this.getCurrentAbsolutePose());
				}
				placeWorkplaneVisualization();

				r.run();
			}
			always.run();
		});
		this.activate();
	}

	public void placeWorkplaneVisualization() {
		engine.placeGrid(ap.get().getWorkplane());
		BowlerKernel.runLater(() -> {
			wpPick.setVisible(isWorkplaneNotOrigin());
			TransformFactory.nrToAffine(ap.get().getWorkplane(), wpPickPlacement);
		});
	}

	public boolean isWorkplaneNotOrigin() {
		TransformNR w = ap.get().getWorkplane();
		double epsilon = 0.00001;
		RotationNR r = w.getRotation();
		double abst = Math.abs(w.getX());
		double abs2t = Math.abs(w.getY());
		double abs3t = Math.abs(w.getZ());
		if (abst > epsilon || abs2t > epsilon || abs3t > epsilon) {
			return true;
		}
		double abs = Math.abs(r.getRotationAzimuthDegrees());
		double abs2 = Math.abs(r.getRotationElevationDegrees());
		double abs3 = Math.abs(r.getRotationTiltDegrees());
		if (abs > epsilon ||
				abs2 > epsilon || abs3 > epsilon) {
			return true;
		}
		return false;
	}

	public void setTemporaryPlane() {
		tempory=true;
	}
	public void clearTemporaryPlane() {
		tempory=false;
	}
	public boolean isTemporaryPlane() {
		return tempory;
	}

	public double getIncrement() {
		return increment;
	}

	public void setIncrement(double increment) {
		this.increment = increment;
	}

	public MeshView getPlacementPlane() {
		// Auto-generated method stub
		return wpPick;
	}

	public Affine getWorkplaneLocation() {
		return workplaneLocation;
	}

	public void setWorkplaneLocation(Affine workplaneLocation) {
		this.workplaneLocation = workplaneLocation;

	}

	public void setUpdater(IWorkplaneUpdate updater) {
		this.updater = updater;
	}

	public void onCancle(Runnable onCancel) {
		this.onCancel = onCancel;	
	}
}
