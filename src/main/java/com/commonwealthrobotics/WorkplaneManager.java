package com.commonwealthrobotics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.commonwealthrobotics.controls.SpriteDisplayMode;
import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.BowlerKernel;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine.GridHolder;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.Log;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.ColinearPointsException;
import eu.mihosoft.vrl.v3d.MissingManipulatorException;
import eu.mihosoft.vrl.v3d.Plane;
import eu.mihosoft.vrl.v3d.Polygon;
import eu.mihosoft.vrl.v3d.Transform;
import eu.mihosoft.vrl.v3d.Vector3d;
import eu.mihosoft.vrl.v3d.ext.org.poly2tri.PolygonUtil;
import eu.mihosoft.vrl.v3d.ext.quickhull3d.HullUtil;
import javafx.collections.ObservableFloatArray;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;

public class WorkplaneManager implements EventHandler<MouseEvent> {

	private GridHolder wpPick;
	private BowlerStudio3dEngine engine;
	private Affine workplaneLocation = new Affine();
	private List<MeshView> indicatorMeshs;
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
	private double snapGridValue = 1.0;
	private IWorkplaneUpdate updater = null;
	private Runnable onCancel;
	private TransformNR pinedValue;

	// Create textured work-plane based on tiles of custom size
	public GridHolder createTexturedWorkplane(double xSizeMM, double ySizeMM) {
		return BowlerStudio3dEngine.createTexturedWorkplane(xSizeMM, ySizeMM);
	}

	private static int webColorToArgb(Color color) {
		return (int) (color.getOpacity() * 255) << 24 | (int) (color.getRed() * 255) << 16
				| (int) (color.getGreen() * 255) << 8 | (int) (color.getBlue() * 255);
	}

	private static Color argbToColor(int argb) {

		return Color.color(((argb >> 16) & 0xFF) / 255.0, ((argb >> 8) & 0xFF) / 255.0, (argb & 0xFF) / 255.0,
				((argb >> 24) & 0xFF) / 255.0);
	}

	public WorkplaneManager(ActiveProject ap, BowlerStudio3dEngine engine, SelectionSession session) {

		this.ap = ap;
		this.engine = engine;
		this.session = session;


		wpPick = createTexturedWorkplane(200, 200);
		wpPick.transformsAdd(wpPickPlacement);
		wpPick.setMouseTransparent(true);

		wpPick.addEventFilter(MouseEvent.MOUSE_PRESSED, ev -> {
			new Exception().printStackTrace();
			setClickOnGround(true);
		});

		engine.getWorkplaneGroup().addEventFilter(MouseEvent.MOUSE_PRESSED, ev -> {
			//new Exception().printStackTrace();
			setClickOnGround(true);
		});

		engine.addCustomWorkplaneNode(wpPick);
		engine.getWorkplaneGroup().setMouseTransparent(true);
		engine.groundToNormal();
	}

	public void setIndicator(CSG indicator, Affine centerOffset) {
		setIndicator(Arrays.asList(indicator), centerOffset);
	}

	public void setIndicator(List<CSG> indicators, Affine centerOffset) {

		if (indicatorMeshs != null) {
			for (MeshView indicatorMesh : indicatorMeshs) {
				engine.removeUserNode(indicatorMesh);
			}
			indicatorMeshs.clear();
		}
		indicatorMeshs = new ArrayList<MeshView>();
		for (CSG indicator : indicators) {
			MeshView indicatorMesh = indicator.newMesh();
			indicatorMesh.getTransforms().addAll(getWorkplaneLocation(), centerOffset);
			indicatorMesh.setMouseTransparent(true);

			PhongMaterial material = new PhongMaterial();

			if (indicator.isHole()) {
				material.setDiffuseColor(new Color(0.25, 0.25, 0.25, 0.75));
				material.setSpecularColor(javafx.scene.paint.Color.WHITE);
			} else {
				Color c = indicator.getColor();
				material.setDiffuseColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 0.75));
				material.setSpecularColor(javafx.scene.paint.Color.WHITE);
			}
			indicatorMesh.setMaterial(material);
			indicatorMeshs.add(indicatorMesh);
			engine.addUserNode(indicatorMesh);
		}
	}

	public void cancel() {

		if (!active)
			return;

		BowlerKernel.runLater(() -> {
			active = false;
			updater = null;
			engine.getWorkplaneGroup().removeEventFilter(MouseEvent.ANY, this);
			wpPick.setVisible(isWorkplaneNotOrigin());

			for (CSG key : session.getMeshes().keySet()) {
				MeshView mv = session.getMeshes().get(key).display;
				mv.removeEventFilter(MouseEvent.ANY, this);
			}

			if (indicatorMeshs != null)
				for (MeshView indicatorMesh : indicatorMeshs)
					indicatorMesh.setVisible(false);

			// indicatorMesh = null;

			if (onSelectEvent != null)
				onSelectEvent.run();

			onSelectEvent = null;

			engine.getWorkplaneGroup().setVisible(true);
			engine.getWorkplaneGroup().setMouseTransparent(true);
			engine.groundToNormal();
			session.setMode(SpriteDisplayMode.Default);

			if (onCancel != null) {
				onCancel.run();
				onCancel = null;
			}
			placeWorkplaneVisualization();
		});
	}

	public void activate() {
		activate(true);
	}

	public void activate(boolean enableGroundPick) {
		active = true;
		tempory = false;
		setClickOnGround(false);
		clicked = false;

		// com.neuronrobotics.sdk.common.Log.debug("Starting workplane listeners");
		wpPick.addEventFilter(MouseEvent.ANY, this);
		wpPick.setMouseTransparent(false);
		wpPick.setVisible(isWorkplaneNotOrigin());

		engine.getWorkplaneGroup().addEventFilter(MouseEvent.ANY, this);
		engine.getWorkplaneGroup().setMouseTransparent(false);
		engine.groundToPicking();
		// Make user meshes pickable

		for (CSG key : session.getMeshes().keySet()) {
			MeshView mv = session.getMeshes().get(key).display;
			mv.addEventFilter(MouseEvent.ANY, this);
		}

		if (indicatorMeshs != null)
			for (MeshView indicatorMesh : indicatorMeshs) {
				indicatorMesh.setVisible(true);
				indicatorMesh.setMouseTransparent(true);
			}
	}

	@Override
	public void handle(MouseEvent ev) {
		try {
			PickResult pickResult = ev.getPickResult();
			Node intersectedNode = pickResult.getIntersectedNode();

			if (ev.getEventType() == MouseEvent.MOUSE_PRESSED) {
				doClickEvent(ev);

			} else if ((ev.getEventType() == MouseEvent.MOUSE_MOVED)
					|| (ev.getEventType() == MouseEvent.MOUSE_DRAGGED)) {
				session.submit(() -> {
					// com.neuronrobotics.sdk.common.Log.error(ev);
					Point3D intersectedPoint = pickResult.getIntersectedPoint();
					double x = intersectedPoint.getX();
					double y = intersectedPoint.getY();
					double z = intersectedPoint.getZ();

					if (ev.getSource() == wpPick) {
						x *= MainController.groundScale();
						y *= MainController.groundScale();
						z *= MainController.groundScale();
					}

					TransformNR screenLocation;
					TransformNR pureRot = null;
					Affine manipulator = new Affine();
					CSG source = null;

					if (intersectedNode instanceof MeshView) {
						MeshView meshView = (MeshView) intersectedNode;

						for (CSG csg : session.getMeshes().keySet()) {
							if (meshView == session.getMeshes().get(csg).display) {
								source = csg;
								try {
									manipulator = source.getManipulator();
								} catch (MissingManipulatorException e) {

								}
								break;
							}
						}

						TriangleMesh mesh = (TriangleMesh) meshView.getMesh();

						int faceIndex = pickResult.getIntersectedFace();

						if (faceIndex >= 0) {
							Polygon fromMesh = getFaceNormalAngles(mesh, faceIndex);
							try {
								pureRot = TransformFactory
										.csgToNR(PolygonUtil.calculateNormalTransform(fromMesh.getPlane().getNormal()))
										.inverse();
							} catch (ColinearPointsException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (source != null) {
								Polygon p = fromMesh;
								Polygon sourcePoly = getPolygonFromFaceIndex(faceIndex, source);
								if (p.getBounds().isBoundsTouching(sourcePoly.getBounds())) {
									// use the more accurate polygon
									p = sourcePoly;
								}

								if (p != null) {
									try {
										Transform npTF = PolygonUtil.calculateNormalTransform(p.getPlane().getNormal());
										npTF.set(0, 0, 0);
										pureRot = TransformFactory.csgToNR(npTF).inverse();
										// an in-plane snapping here by transforming the points into the plane
										// orientation, then snapping in plane, then transforming the points back.
										TransformNR t = new TransformNR(x, y, z);
										TransformNR screenLocationtmp = t; // manipulatorNR.times(t);
										TransformNR npTFNR = TransformFactory.csgToNR(npTF);
										Polygon flattened = p.transformed(npTF);
										TransformNR flattenedTouch = npTFNR.times(screenLocationtmp);
										// Log.debug("Polygon " + flattened);
										// Log.debug("Point " + flattenedTouch.toSimpleString());
										TransformNR adjusted = new TransformNR( // Snap in plane
												SelectionSession.roundToNearest(flattenedTouch.getX(), snapGridValue),
												SelectionSession.roundToNearest(flattenedTouch.getY(), snapGridValue),
												flattened.getPoints().get(0).z); // adhere to the plane of the polygon
										// flip the point back to its original orientation in the plane post snap
										TransformNR adjustedBack = npTFNR.inverse().times(adjusted);
										x = adjustedBack.getX();
										y = adjustedBack.getY();
										z = adjustedBack.getZ();
										// Log.debug("Polygon snapped " + adjusted);
									} catch (Exception e) {
										e.printStackTrace();
									}

								}

							} else {
								x = SelectionSession.roundToNearest(x, snapGridValue);
								y = SelectionSession.roundToNearest(y, snapGridValue);
								z = SelectionSession.roundToNearest(z, snapGridValue);
							}

						} else
							Log.error("Error face index came back: " + faceIndex);

					}
					if (pureRot == null)
						pureRot = new TransformNR();
					TransformNR manipulatorNR = TransformFactory.affineToNr(manipulator);
					TransformNR t = new TransformNR(x, y, z);
					screenLocation = manipulatorNR.times(t.times(pureRot));

					if ((intersectedNode == wpPick.intersectionNode)) {
						if (updater != null)
							updater.setWorkplaneLocation(screenLocation);

						screenLocation = ap.get().getWorkplane().times(screenLocation);
					} else if (updater != null)
						updater.setWorkplaneLocation(ap.get().getWorkplane().inverse().times(screenLocation));
					TransformNR toSet = screenLocation;
					BowlerKernel.runLater(() -> setCurrentAbsolutePose(toSet));
				});
			}
		} catch (Throwable t) {
			Log.error(t);
		}
	}

	public void doClickEvent(MouseEvent ev) {
		clicked = true;
		session.submit(() -> {

			// onCancel = null;// non cancel but instead completed
			// this must not be in UI thread
			cancel();

			ev.consume();
			BowlerKernel.runLater(() -> {
				engine.getWorkplaneGroup().setMouseTransparent(true);
				engine.groundToNormal();
				session.getControls().hideRotationHandles();

				wpPick.setMouseTransparent(true);
				session.getControls().setMode(SpriteDisplayMode.Default);
			});
		});
	}

	private Polygon getPolygonFromFaceIndex(int faceIndex, CSG source) {
		long[] triangles = source.getTriangles();

		int i0 = (int) triangles[faceIndex * 3 + 0];
		int i1 = (int) triangles[faceIndex * 3 + 1];
		int i2 = (int) triangles[faceIndex * 3 + 2];

		Polygon p = null;
		try {
			p = Polygon.fromPoints(
					Arrays.asList(new Vector3d(source.getVertex_X(i0), source.getVertex_Y(i0), source.getVertex_Z(i0)),
							new Vector3d(source.getVertex_X(i1), source.getVertex_Y(i1), source.getVertex_Z(i1)),
							new Vector3d(source.getVertex_X(i2), source.getVertex_Y(i2), source.getVertex_Z(i2))));
		} catch (ColinearPointsException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
		}

		return p;
	}

	private TransformNR getFaceNormalAngles(CSG source, int faceIndex) throws ColinearPointsException {

		long[] triangles = source.getTriangles();

		int i0 = (int) triangles[faceIndex * 3 + 0];
		int i1 = (int) triangles[faceIndex * 3 + 1];
		int i2 = (int) triangles[faceIndex * 3 + 2];

		Vector3d p0 = new Vector3d(source.getVertex_X(i0), source.getVertex_Y(i0), source.getVertex_Z(i0));
		Vector3d p1 = new Vector3d(source.getVertex_X(i1), source.getVertex_Y(i1), source.getVertex_Z(i1));
		Vector3d p2 = new Vector3d(source.getVertex_X(i2), source.getVertex_Y(i2), source.getVertex_Z(i2));

		Vector3d normal = p1.minus(p0).cross(p2.minus(p0)).normalized();

		return TransformFactory.csgToNR(PolygonUtil.calculateNormalTransform(normal));
	}

	// public static Polygon getPolygonFromFaceIndex(int faceIndex, CSG polygons) {
	//
	// try {
	// return polygons.getPolygonByIndex(faceIndex);
	// } catch (Exception e) {
	// Log.error(e);
	// }
	// return null;
	// }

	private Vector3d toV(javafx.geometry.Point3D p) {
		return new Vector3d(p.getX(), p.getY(), p.getZ());
	}

	private Polygon getFaceNormalAngles(TriangleMesh mesh, int faceIndex) {
		ObservableFaceArray faces = mesh.getFaces();
		ObservableFloatArray points = mesh.getPoints();

		int p1Index = faces.get(faceIndex * 6) * 3;
		int p2Index = faces.get(faceIndex * 6 + 2) * 3;
		int p3Index = faces.get(faceIndex * 6 + 4) * 3;

		Point3D p1 = new Point3D(points.get(p1Index), points.get(p1Index + 1), points.get(p1Index + 2));
		Point3D p2 = new Point3D(points.get(p2Index), points.get(p2Index + 1), points.get(p2Index + 2));
		Point3D p3 = new Point3D(points.get(p3Index), points.get(p3Index + 1), points.get(p3Index + 2));

		try {
			// Polygon p =
			// Polygon.fromVector3d(Arrays.asList(toV(p1),toV(p2),toV(p3))).get(0);
			return Polygon.fromPoints(Arrays.asList(toV(p1), toV(p2), toV(p3)));

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
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

	public void pickPlane(Runnable r, Runnable always, RulerManager ruler) {

		// Create work plane placement indicator
		double pointerLen = 5;
		double pointerWidth = 2;
		double pointerHeight = 10;

		CSG indicator = HullUtil
				.hull(Arrays.asList(new Vector3d(0, 0, 0), new Vector3d(pointerLen, 0, 0),
						new Vector3d(pointerWidth, pointerWidth, 0), new Vector3d(0, 0, pointerHeight)))
				.union(HullUtil.hull(Arrays.asList(new Vector3d(0, 0, 0), new Vector3d(0, pointerLen, 0),
						new Vector3d(pointerWidth, pointerWidth, 0), new Vector3d(0, 0, pointerHeight))))
				.setColor(Color.YELLOWGREEN);

		this.setIndicator(Arrays.asList(indicator), new Affine());

		ap.get().setWorkplane(new TransformNR());
		session.updateHandleOrientations(engine.getFlyingCamera().getCamerFrame());
		placeWorkplaneVisualization();

		this.setOnSelectEvent(() -> {
			session.submit(() -> {
				if (this.isClicked()) {

					if (this.isClickOnGround()) {
						com.neuronrobotics.sdk.common.Log.debug("Ground plane click detected");
						ap.get().setWorkplane(new TransformNR());
						ruler.disableRulerMode();
					} else {
						// Move the workplane down from the surface to ensure a solid overlap between
						// the object and the surface

						TransformNR downset = new TransformNR(0, 0, -Plane.getEPSILON() * 100);
						TransformNR currentAbsolutePose = this.getCurrentAbsolutePose().times(downset);
						com.neuronrobotics.sdk.common.Log.debug("Workplane Placed " + currentAbsolutePose);

						ap.get().setWorkplane(currentAbsolutePose);
					}
					placeWorkplaneVisualization();
					r.run();
				} else {
					com.neuronrobotics.sdk.common.Log.debug("Click not regestered ");
				}

				always.run();
			});

		});

		this.activate(true);
	}

	public void placeWorkplaneVisualization() {

		engine.placeGrid(ap.get().getWorkplane());
		boolean workplaneNotOrigin = isWorkplaneNotOrigin();
		Log.debug("Placing workplane visualization " + workplaneNotOrigin);

		BowlerKernel.runLater(() -> {
			wpPick.setVisible(workplaneNotOrigin);
			TransformFactory.nrToAffine(ap.get().getWorkplane(), wpPickPlacement);
			//			wpPick.setVisible(workplaneNotOrigin);
			//			if (!workplaneNotOrigin)
			//				TransformFactory.nrToAffine(new TransformNR(), wpPickPlacement);
			//			else
			//				TransformFactory.nrToAffine(ap.get().getWorkplane(), wpPickPlacement);
		});
	}

	public boolean isWorkplaneNotOrigin() {
		TransformNR w = ap.get().getWorkplane();
		double epsilon = 0.01;
		RotationNR r = w.getRotation();
		double abs3t = Math.abs(w.getZ());

		if ((abs3t > epsilon))
			return true;

		double abs2 = Math.abs(r.getRotationElevationDegrees());
		double abs3 = Math.abs(r.getRotationTiltDegrees());

		boolean b = (abs2 > epsilon) || (abs3 > epsilon);
		if (b)
			return true;
		return false;
	}

	public void setTemporaryPlane() {
		tempory = true;
	}

	public void clearTemporaryPlane() {
		tempory = false;
	}

	public boolean isTemporaryPlane() {
		return tempory;
	}

	public double getIncrement() {
		return snapGridValue;
	}

	public void setIncrement(double snapGridValue) {
		this.snapGridValue = snapGridValue;
	}

	public GridHolder getPlacementPlane() {
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

	public void onCancel(Runnable onCancel) {
		this.onCancel = onCancel;
	}

	public boolean isActive() {
		return active;
	}

	public void setPin(TransformNR pinedValue) {
		if (pinedValue == null)
			return;
		pinedValue = ap.get().getWorkplane().times(pinedValue);
		this.pinedValue = pinedValue;

		if (updater != null)
			updater.setWorkplaneLocation(ap.get().getWorkplane().inverse().times(pinedValue));
		setCurrentAbsolutePose(pinedValue);
	}
}
