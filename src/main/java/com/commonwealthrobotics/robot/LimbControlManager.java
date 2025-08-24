package com.commonwealthrobotics.robot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.RulerManager;
import com.commonwealthrobotics.controls.ControlSprites;
import com.commonwealthrobotics.controls.ResizingHandle;
import com.commonwealthrobotics.controls.SelectionSession;
import com.commonwealthrobotics.controls.SpriteDisplayMode;
import com.commonwealthrobotics.rotate.RotationSessionManager;
import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;
import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation.DragState;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.creature.MobileBaseBuilder;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.MoveCenter;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.robot.ModifyLimb;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.VirtualCameraMobileBase;
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics;
import com.neuronrobotics.sdk.addons.kinematics.math.ITransformNRChangeListener;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cylinder;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.event.EventHandler;
import javafx.scene.DepthTest;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

public class LimbControlManager {

	private BowlerStudio3dEngine engine;
	private DHParameterKinematics limb;
	private SelectionSession session;
	private ControlSprites sprites;
	private MobileBaseBuilder builder;
	private ModifyLimb mod;
//	private Manipulation tipManip = new Manipulation(baseSelection, new Vector3d(1, 1, 0), new TransformNR());
//	private EventHandler<MouseEvent> tipMouseMover = tipManip.getMouseEvents();
//	private Manipulation baseManip = new Manipulation(baseSelection, new Vector3d(1, 1, 0), new TransformNR());
//	private EventHandler<MouseEvent> baseMouseMover = baseManip.getMouseEvents();
	//ResizingHandle base = null;
	//ResizingHandle tip = null;
	
	ArmPointManipulator baseManipulator;
	ArmPointManipulator tipManipulator;
	ResizingHandle elbow = null;
	RotationSessionManager rotationManager;
	Affine workplaneOffset = new Affine();
	ArrayList<Node> handles;
	private ActiveProject ap;
	private Bounds b;
	private List<String> selectedCSG;
	private VirtualCameraMobileBase camera;

	public LimbControlManager(BowlerStudio3dEngine engine, SelectionSession session, ActiveProject ap,
			RulerManager ruler) {
		this.engine = engine;
		camera = engine.getFlyingCamera();

		this.session = session;
		this.ap = ap;
		sprites = session.getControls();
		Runnable r = () -> {
			BowlerStudio.runLater(() -> session.setMode(SpriteDisplayMode.Default));
			// add the operation and reset\
			ModifyLimb myMod = mod;
			update(builder);
			myMod.setUndo(false);
			try {
				ap.addOp(myMod).join();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			session.clearBoundsCache();
			BowlerStudio.runLater(() -> session.updateControls());
		};

		Runnable onReset = () -> {
			onReset();
		};
		Runnable onSelect = () -> {
			updateControls();
		};
		tipManipulator = new ArmPointManipulator(r,ev -> {
			BowlerStudio.runLater(() -> {
				session.setMode(SpriteDisplayMode.Clear);
			});
			TransformNR base2 = mod.getTip().copy();
			// System.out.println("from "+base2.toSimpleString());
			RotationNR nr = base2.getRotation();
			TransformNR tf = new TransformNR(base2.getX(), base2.getY(), base2.getZ())
					.times(tipManipulator.getCurrentPoseInReferenceFrame());
			tf.setRotation(nr);
			mod.setTip(tf);
			// System.out.println("Moving "+tf.toSimpleString());
			try {
				limb.setDesiredTaskSpaceTransform(tf, 0);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			updateControls();
		},ap,engine,workplaneOffset,onSelect, onReset);
		
		baseManipulator = new ArmPointManipulator(r,ev -> {
			BowlerStudio.runLater(() -> {
				session.setMode(SpriteDisplayMode.Clear);
			});
			TransformNR baseAtStaartTF = mod.getBase().copy();
			// System.out.println("from "+base2.toSimpleString());
			RotationNR nr = baseAtStaartTF.getRotation();
			TransformNR translateOnly = new TransformNR(baseAtStaartTF.getX(), baseAtStaartTF.getY(),
					baseAtStaartTF.getZ());
			TransformNR tf = translateOnly.times(baseManipulator.getCurrentPoseInReferenceFrame());
			tf.setRotation(nr);
			mod.setBase(tf);
			// System.out.println("Moving "+tf.toSimpleString());
			limb.setRobotToFiducialTransform(tf);
			mod.setTip(limb.getCurrentTaskSpaceTransform());
			updateControls();
		},ap,engine,workplaneOffset,onSelect,onReset);
		


		rotationManager = new RotationSessionManager(new Affine(), ap, session, workplaneOffset, ruler, (tf) -> {
			r.run();
		});
		rotationManager.setMoving(toUpdate -> {
			try {

				BowlerStudio.runLater(() -> {
					session.setMode(SpriteDisplayMode.Clear);
				});
				TransformNR baseAtStartTF = mod.getBase().copy();
				// System.out.println("from "+base2.toSimpleString());
				RotationNR nr = baseAtStartTF.getRotation();
				TransformNR rotOnly = new TransformNR(nr);
				TransformNR tf = toUpdate.times(rotOnly);
				baseAtStartTF.setRotation(tf.getRotation());
				mod.setBase(baseAtStartTF);
				// System.out.println("Moving "+tf.toSimpleString());
				limb.setRobotToFiducialTransform(baseAtStartTF);
				mod.setTip(limb.getCurrentTaskSpaceTransform());
				updateControls();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		handles = new ArrayList<>();
		handles.addAll(tipManipulator.getMesh());
		handles.addAll(baseManipulator.getMesh());	
		handles.addAll(rotationManager.getElements());
		hide();
		for (Node n : handles) {
			n.setViewOrder(-2);
			n.setDepthTest(DepthTest.DISABLE);
			engine.addUserNode(n);
			n.setVisible(false);
		}

	}

	private void onReset() {
		System.out.println("Reset Limb Controller");

		baseManipulator.onReset();
		tipManipulator.onReset();
	}

	public void show(DHParameterKinematics limb) {
		this.limb = limb;
		b = session.getBounds(limb);
		if(b==null)
			throw new RuntimeException("Limb has no parts");
		selectedCSG = session.selectedSnapshot();
		mod = new ModifyLimb().setLimb(limb).setNames(session.selectedSnapshot());
		baseManipulator.show();
		tipManipulator.show();

		mod.setUndo(true);
		onReset();
		rotationManager.show(false);
		System.out.println("\n\nShowing Limb "+limb.getScriptingName());
	}

	public void hide() {
		BowlerStudio.runLater(() -> {
			baseManipulator.hide();
			tipManipulator.hide();
			rotationManager.hide();
		});
	}

	private void updateControls() {
		double zoom = camera.getZoomDepth();
		double screenW = engine.getSubScene().getWidth();
		double screenH = engine.getSubScene().getHeight();
		TransformNR cf = engine.getFlyingCamera().getCamerFrame().times(new TransformNR(0, 0, zoom));
		threeDTarget(screenW, screenH, zoom, cf, false);
		builder.getCadManager().render();
	}

	public void threeDTarget(double screenW, double screenH, double zoom, TransformNR cf, boolean locked) {
		if (limb == null) 
			return;
		double az = camera.getPanAngle();
		double el = camera.getTiltAngle();
		double x = camera.getGlobalX();
		double y = camera.getGlobalY();
		double z = camera.getGlobalZ();
		TransformNR workplane = ap.get().getWorkplane();
		if (baseManipulator.getState() == DragState.IDLE)
			baseManipulator.threeDTarget(screenW, screenH, zoom, workplane.inverse().times(limb.getRobotToFiducialTransform()), cf,
					locked);

		if (tipManipulator.getState() == DragState.IDLE)
			tipManipulator.threeDTarget(screenW, screenH, zoom, workplane.inverse().times(limb.getCurrentTaskSpaceTransform()), cf,
					locked);
		
		rotationManager.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b, cf);
		BowlerStudio.runLater(() -> {
			TransformFactory.nrToAffine(workplane, workplaneOffset);
		});

	}

	public void update(MobileBaseBuilder builder) {
		this.builder = builder;
		if (builder != null) 
			for (CSG c : session.getCurrentStateSelected()) {
				if (c.getLimbName().isPresent()) {
					String name = c.getLimbName().get();
					DHParameterKinematics kin = builder.getMobileBase().getLimbByName(name);
					show(kin);
					return;
				}

			}
		limb = null;
		hide();
	}

}
