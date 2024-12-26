package com.commonwealthrobotics.mirror;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.controls.ControlSprites;
import com.commonwealthrobotics.rotate.RotationSessionManager;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleOpperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Mirror;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.MirrorOrentation;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.EulerAxis;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cylinder;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;

public class MirrorHandle implements ICaDoodleStateUpdate{
	private MirrorOrentation ax;
	private Affine translate;
	private Affine vr;
	private MirrorSessionManager rotationSessionManager;
	private ActiveProject ap;
	private ControlSprites cs;
	private Affine workplaneOffset;
	private double screenW;
	private double screenH;
	private double zoom;
	private double az;
	private double el;
	private double x;
	private double y;
	private double z;
	private List<String> selectedCSG;
	private Bounds b;
	private TransformNR cf;
	private static double height = 20;
	private static CSG doubbleArrow = null;
	private MeshView mesh = null;
	private BowlerStudio3dEngine engine;
	private List<CSG> ta;
	private List<String> selected;
	private HashMap<CSG, MeshView> meshes;
	private boolean active = false;
	private EventHandler<? super MouseEvent> entered;

	private EventHandler<? super MouseEvent> exited;
	private EventHandler<? super MouseEvent> onClickEvent;
	private PhongMaterial material;
	private HashMap<CSG, MeshView> visualizers = new HashMap<>();
	private Scale scaleTF = new Scale();
	private Affine location = new Affine();
	private Affine cameraOrent = new Affine();
	private double scale;
	private Mirror op;
	private List<CSG> visualizationObjects;

	public MirrorHandle(MirrorOrentation ax, Affine translate, Affine vr, MirrorSessionManager rotationSessionManager,
			ActiveProject ap, ControlSprites cs, Affine workplaneOffset) {
		this.ax = ax;
		this.translate = translate;
		this.vr = vr;
		this.rotationSessionManager = rotationSessionManager;
		this.ap = ap;
		this.cs = cs;
		this.workplaneOffset = workplaneOffset;
		CSG arrow = getDoubbleArrow();
		mesh = arrow.newMesh();
		material = new PhongMaterial();

		mesh.setCullFace(CullFace.NONE);
		mesh.setMaterial(material);
		exited = event -> {
			material.setDiffuseColor(Color.BLACK);
			for (CSG key : visualizers.keySet()) {
				visualizers.get(key).setVisible(false);
			}
		};
		entered = event -> {
			material.setDiffuseColor(new Color(1, 0, 0, 1));
			// com.neuronrobotics.sdk.common.Log.error("ENtered " + self + " " +
			// orentation);
			for (CSG key : visualizers.keySet()) {
				visualizers.get(key).setVisible(true);
			}
		};
		onClickEvent = event -> {
			material.setDiffuseColor(Color.BLACK);
			com.neuronrobotics.sdk.common.Log.error("Handle clicked ");
			setMyOperation();
		};
		// mesh.getTransforms().add(move);
		// mesh.getTransforms().add(allignLoc);
		mesh.getTransforms().add(workplaneOffset);
		mesh.getTransforms().add(location);
		mesh.getTransforms().add(cameraOrent);
		mesh.getTransforms().add(scaleTF);
		mesh.setVisible(false);
	}

	private void setMyOperation() {
		System.out.println("\n\nRun Mirror on " + ax);
	}

	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double xI, double yI,
			double zI, List<String> selectedCSG, Bounds b, TransformNR cf) {
		//System.out.println("Mirror Handle "+ax+" Updated");
		this.screenW = screenW;
		this.screenH = screenH;
		this.zoom = zoom;
		this.az = az;
		this.el = el;
		this.x = xI;
		this.y = yI;
		this.z = zI;
		this.selectedCSG = selectedCSG;
		this.b = b;
		this.cf = cf;
		double X = 0;
		double Y = 0;
		double Z = 0;
		switch (ax) {
		case x:
			X = b.getCenter().x;
			Y = b.getMin().y;
			Z = b.getMin().z;
			break;
		case y:
			X = b.getMax().x;
			Y = b.getCenter().y;
			Z = b.getMin().z;
			break;
		case z:
			X = b.getMax().x;
			Y = b.getMax().y;
			Z = b.getCenter().z;
			break;
		default:
			break;

		}

		TransformNR target = new TransformNR(X, Y, Z);
		double rx = 0;
		double ry = 0;
		double rz = 0;
		if (ax==MirrorOrentation.x) {
			rx = 0;
			ry = -90;
			rz = 90;
		}
		if (ax==MirrorOrentation.y) {
			rx = 0;
			ry = 90;
			rz = 0;
		}
		if (ax==MirrorOrentation.z) {
			rx = -90;
			ry = 0;
			rz = 0;
		}
		TransformNR pureRot = new TransformNR(new RotationNR(rx, rz, ry));

		// com.neuronrobotics.sdk.common.Log.error(cf.toSimpleString());
		// Calculate the vector from camera to target
		double x = target.getX() - cf.getX();
		double y = target.getY() - cf.getY();
		double z = target.getZ() - cf.getZ();

		// Calculate the distance between camera and target
		double distance = Math.sqrt(x * x + y * y + z * z);

		// Define a base scale and distance
		double baseScale = 0.75;
		double baseDistance = 1000.0;

		// Calculate the scale factor
		double scaleFactor = ((distance / baseDistance) * baseScale);

		// Clamp the scale factor to a reasonable range
		scaleFactor = Math.max(0.001, Math.min(90.0, scaleFactor));

		setScale(scaleFactor);

		BowlerStudio.runLater(() -> {
			scaleTF.setX(getScale());
			scaleTF.setY(getScale());
			scaleTF.setZ(getScale());
			TransformFactory.nrToAffine(pureRot, cameraOrent);
			TransformFactory.nrToAffine(target.setRotation(new RotationNR()), location);
		});
		if(op!=null)
			op.setWorkplane(ap.get().getWorkplane());
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}

	public void initialize(Bounds b, BowlerStudio3dEngine eng, List<CSG> t, List<String> sel,
			HashMap<CSG, MeshView> meshes) {
		this.b = b;
		this.engine = eng;
		this.ta = t;
		this.selected = sel;
		this.meshes = meshes;
		material.setDiffuseColor(Color.BLACK);
		BowlerStudio.runLater(() -> mesh.setVisible(true));
		mesh.addEventFilter(MouseEvent.MOUSE_EXITED, exited);
		mesh.addEventFilter(MouseEvent.MOUSE_ENTERED, entered);
		mesh.addEventFilter(MouseEvent.MOUSE_CLICKED, onClickEvent);
		updateState();
		ap.addListener(this);
	}

	private void updateState() {
		op= new Mirror().setNames(selected).setWorkplane(ap.get().getWorkplane()).setLocation(ax);
		if(visualizationObjects!=null) {
			for(CSG obj:visualizationObjects) {
				MeshView mv = visualizers.remove(obj);
				engine.removeUserNode(mv);
			}
		}
		visualizationObjects = op.process(ta);
		for(CSG indicator:visualizationObjects) {
			MeshView indicatorMesh = indicator.newMesh();
			indicatorMesh.setMouseTransparent(true);
			//indicatorMesh.getTransforms().addAll(workplaneOffset);
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
			indicatorMesh.setVisible(false);
			visualizers.put(indicator, indicatorMesh);
		}
	}

	public void hide() {
		BowlerStudio.runLater(() -> mesh.setVisible(false));
		mesh.removeEventFilter(MouseEvent.MOUSE_EXITED, exited);
		mesh.removeEventFilter(MouseEvent.MOUSE_ENTERED, entered);
		mesh.removeEventFilter(MouseEvent.MOUSE_CLICKED, onClickEvent);
		for(CSG key:visualizers.keySet()) {
			visualizers.get(key).setVisible(false);
		}
		ap.removeListener(this);
	}

	public List<Node> getElements() {
		ArrayList<Node> result = new ArrayList<Node>();
		result.add(mesh);
		return result;
	}

	public static CSG getDoubbleArrow() {
		if (doubbleArrow == null) {
			CSG cone = new Cylinder(height / 2, 0, height).toCSG().movez(height);
			CSG pin = new Cylinder(height / 4, height).toCSG();
			CSG arrow = cone.union(pin).rotx(90);
			doubbleArrow = arrow.union(arrow.rotx(180));
			// doubbleArrow.setColor(Color.BLACK);
		}
		return doubbleArrow;
	}

	public static void setDoubbleArrow(CSG doubbleArrow) {
		MirrorHandle.doubbleArrow = doubbleArrow;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Override
	public void onUpdate(List<CSG> currentState, ICaDoodleOpperation source, CaDoodleFile file) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSaveSuggestion() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInitializationDone() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInitializationStart() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRegenerateDone() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRegenerateStart() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onWorkplaneChange(TransformNR newWP) {
		updateState();
	}
}
