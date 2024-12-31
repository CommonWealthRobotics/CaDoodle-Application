package com.commonwealthrobotics;

import java.util.Arrays;
import java.util.HashMap;

import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.BowlerKernel;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.Cylinder;
import eu.mihosoft.vrl.v3d.Vector3d;
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

	}

	public void setIndicator(CSG indicator, Affine centerOffset) {
		if (indicatorMesh != null) {
			engine.removeUserNode(indicatorMesh);
		}
		indicatorMesh = indicator.newMesh();
		indicatorMesh.getTransforms().addAll(workplaneLocation, centerOffset);
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

	}

	public void cancle() {
		if (!active)
			return;
		ground.removeEventFilter(MouseEvent.ANY, this);
		wpPick.removeEventFilter(MouseEvent.ANY, this);
		wpPick.setVisible(isWorkplaneInOrigin());
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

	}

	public void activate() {
		active = true;
		tempory=false;
		setClickOnGround(false);
		clicked = false;
		com.neuronrobotics.sdk.common.Log.error("Starting workplane listeners");
		ground.addEventFilter(MouseEvent.ANY, this);
		ground.setMouseTransparent(false);
		wpPick.setMouseTransparent(false);
		
		wpPick.addEventFilter(MouseEvent.ANY, this);
		wpPick.setVisible(isWorkplaneInOrigin());
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
			cancle();
			ev.consume();
			session.updateControls();
			ground.setMouseTransparent(true);
			wpPick.setMouseTransparent(true);
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
			x=SelectionSession.roundToNearist(x,increment);
			y=SelectionSession.roundToNearist(y,increment);
			z=SelectionSession.roundToNearist(z,increment);
			
			TransformNR screenLocation;
			double[] angles = new double[] { 0, 0, 0 };
			if (intersectedNode instanceof MeshView) {
				MeshView meshView = (MeshView) intersectedNode;
				TriangleMesh mesh = (TriangleMesh) meshView.getMesh();

				int faceIndex = pickResult.getIntersectedFace();
				if (faceIndex >= 0)
					angles = getFaceNormalAngles(mesh, faceIndex);
				else
					com.neuronrobotics.sdk.common.Log.error("Error face index came back: " + faceIndex);
			}
			TransformNR pureRot = new TransformNR(new RotationNR(angles[1], angles[0], angles[2]));
			TransformNR t = new TransformNR(x, y, z);
			screenLocation = t.times(pureRot);
			if (intersectedNode == wpPick) {
				screenLocation = ap.get().getWorkplane().times(screenLocation);
			}
			setCurrentAbsolutePose(screenLocation);

		}
	}

	private double[] getFaceNormalAngles(TriangleMesh mesh, int faceIndex) {
		ObservableFaceArray faces = mesh.getFaces();
		ObservableFloatArray points = mesh.getPoints();

		int p1Index = faces.get(faceIndex * 6) * 3;
		int p2Index = faces.get(faceIndex * 6 + 2) * 3;
		int p3Index = faces.get(faceIndex * 6 + 4) * 3;

		Point3D p1 = new Point3D(points.get(p1Index), points.get(p1Index + 1), points.get(p1Index + 2));
		Point3D p2 = new Point3D(points.get(p2Index), points.get(p2Index + 1), points.get(p2Index + 2));
		Point3D p3 = new Point3D(points.get(p3Index), points.get(p3Index + 1), points.get(p3Index + 2));

		Point3D v1 = p2.subtract(p1);
		Point3D v2 = p3.subtract(p1);

		Point3D normal = v1.crossProduct(v2).normalize();

		// Calculate azimuth and tilt
		double azimuth = Math.atan2(normal.getY(), normal.getX());
		double tilt = Math.asin(normal.getZ());

		// Convert to degrees
		azimuth = Math.toDegrees(azimuth) - 90;
		tilt = Math.toDegrees(tilt) - 90;

		Point3D globalX = new Point3D(1, 0, 0);
		Point3D localY = normal.crossProduct(globalX).normalize();
		Point3D localX = normal.crossProduct(localY).normalize();
		double roll =0;// Math.atan2(localY.getZ(), localX.getZ());

		// Ensure azimuth is in the range [-180, 180)
		if (azimuth < -180) {
			azimuth += 360;
		}
		if (azimuth > 180) {
			azimuth -= 360;
		}
		return new double[] { azimuth, tilt, roll };
	}

	public TransformNR getCurrentAbsolutePose() {
		return currentAbsolutePose;
	}

	public void setCurrentAbsolutePose(TransformNR currentAbsolutePose) {
		this.currentAbsolutePose = currentAbsolutePose;
		TransformFactory.nrToAffine(getCurrentAbsolutePose(), workplaneLocation);
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

	public void pickPlane(Runnable r) {
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
		this.setOnSelectEvent(() -> {
			if (!this.isClicked())
				return;
			if (this.isClickOnGround()) {
				// com.neuronrobotics.sdk.common.Log.error("Ground plane click detected");
				ap.get().setWorkplane(new TransformNR());
			} else {
				ap.get().setWorkplane(this.getCurrentAbsolutePose());
			}
			placeWorkplaneVisualization();
			r.run();
		});
		this.activate();
	}

	public void placeWorkplaneVisualization() {
		engine.placeGrid(ap.get().getWorkplane());
		BowlerKernel.runLater(() -> {
			wpPick.setVisible(isWorkplaneInOrigin());
			TransformFactory.nrToAffine(ap.get().getWorkplane(), wpPickPlacement);
		});
	}

	public boolean isWorkplaneInOrigin() {
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
}
