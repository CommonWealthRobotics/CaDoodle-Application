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
	private Affine baseSelection = new Affine();
	private ModifyLimb mod;
//	private Manipulation tipManip = new Manipulation(baseSelection, new Vector3d(1, 1, 0), new TransformNR());
//	private EventHandler<MouseEvent> tipMouseMover = tipManip.getMouseEvents();
//	private Manipulation baseManip = new Manipulation(baseSelection, new Vector3d(1, 1, 0), new TransformNR());
//	private EventHandler<MouseEvent> baseMouseMover = baseManip.getMouseEvents();
	ResizingHandle base = null;
	ResizingHandle tip = null;
	ResizingHandle elbow = null;
	RotationSessionManager rotationManager;
	Affine workplaneOffset = new Affine();
	ArrayList<Node> handles;
	private ActiveProject ap;
	private Bounds b;
	private List<String> selectedCSG;

	public LimbControlManager(BowlerStudio3dEngine engine, SelectionSession session, ActiveProject ap,
			RulerManager ruler) {
		this.engine = engine;
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

		tip = new ResizingHandle("Limb Base", engine, baseSelection, new Vector3d(1, 1, 0), workplaneOffset, () -> {
			updateLines();
		}, () -> {
			onReset();
		}, new Cylinder(5, 1).toCSG());
		tip.setMyColor(Color.PINK, Color.TEAL);
		tip.setBaseSize(1.25);
		tip.manipulator.setFrameOfReference(() -> ap.get().getWorkplane());
		tip.manipulator.addEventListener(ev -> {
			BowlerStudio.runLater(() -> {
				session.setMode(SpriteDisplayMode.Clear);
			});
			TransformNR base2 = mod.getTip().copy();
			// System.out.println("from "+base2.toSimpleString());
			RotationNR nr = base2.getRotation();
			TransformNR tf = new TransformNR(base2.getX(), base2.getY(), base2.getZ())
					.times(tip.manipulator.getCurrentPoseInReferenceFrame());
			tf.setRotation(nr);
			mod.setTip(tf);
			// System.out.println("Moving "+tf.toSimpleString());
			try {
				limb.setDesiredTaskSpaceTransform(tf, 0);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			builder.getCadManager().render();
		});
		tip.manipulator.addSaveListener(r);

		base = new ResizingHandle("Limb Base", engine, baseSelection, new Vector3d(1, 1, 0), workplaneOffset, () -> {
			updateLines();
		}, () -> {
			onReset();
		}, new Cylinder(5, 1).toCSG());
		base.setMyColor(Color.PINK, Color.TEAL);
		base.setBaseSize(1.25);
		base.manipulator.setFrameOfReference(() -> ap.get().getWorkplane());
		base.manipulator.addEventListener(ev -> {
			// new Exception().printStackTrace();
			BowlerStudio.runLater(() -> {
				session.setMode(SpriteDisplayMode.Clear);
			});
			TransformNR baseAtStaartTF = mod.getBase().copy();
			// System.out.println("from "+base2.toSimpleString());
			RotationNR nr = baseAtStaartTF.getRotation();
			TransformNR translateOnly = new TransformNR(baseAtStaartTF.getX(), baseAtStaartTF.getY(), baseAtStaartTF.getZ());
			TransformNR tf = translateOnly.times(base.manipulator.getCurrentPoseInReferenceFrame());
			tf.setRotation(nr);
			mod.setBase(tf);
			// System.out.println("Moving "+tf.toSimpleString());
			limb.setRobotToFiducialTransform(tf);
			mod.setTip(limb.getCurrentTaskSpaceTransform());
			builder.getCadManager().render();
		});
		base.manipulator.addSaveListener(r);

		rotationManager = new RotationSessionManager(baseSelection, ap, session, workplaneOffset, ruler, (tf) -> {
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
				builder.getCadManager().render();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		handles = new ArrayList<>(Arrays.asList(base.getMesh(), tip.getMesh()));
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
		base.resetSelected();
		base.manipulator.reset();
		tip.resetSelected();
		tip.manipulator.reset();
	}

	private void updateLines() {
		// TODO Auto-generated method stub

	}

	public void show(DHParameterKinematics limb) {
		this.limb = limb;
		b = session.getBounds(limb);
		selectedCSG = session.selectedSnapshot();
		mod = new ModifyLimb().setLimb(limb).setNames(session.selectedSnapshot());
		base.show();
		tip.show();
		mod.setUndo(true);
		onReset();
		rotationManager.show(false);
	}

	public void hide() {
		base.hide();
		tip.hide();
		rotationManager.hide();
	}

	public void threeDTarget(double screenW, double screenH, double zoom, TransformNR cf, boolean locked) {
		if (limb == null)
			return;
		VirtualCameraMobileBase camera = engine.getFlyingCamera();
		double az = camera.getPanAngle();
		double el = camera.getTiltAngle();
		double x = camera.getGlobalX();
		double y = camera.getGlobalY();
		double z = camera.getGlobalZ();
		TransformNR workplane = ap.get().getWorkplane();
		base.threeDTarget(screenW, screenH, zoom, workplane.inverse().times(limb.getRobotToFiducialTransform()), cf,
				locked);
		tip.threeDTarget(screenW, screenH, zoom, workplane.inverse().times(limb.getCurrentTaskSpaceTransform()), cf,
				locked);
		rotationManager.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b, cf);
		BowlerStudio.runLater(() -> {
			TransformFactory.nrToAffine(workplane, workplaneOffset);
		});

	}

	public void update(MobileBaseBuilder builder) {
		this.builder = builder;
		if (builder == null) {
			limb = null;
			hide();
			return;
		}
		for (CSG c : session.getCurrentStateSelected()) {
			if (c.getLimbName().isPresent()) {
				String name = c.getLimbName().get();
				DHParameterKinematics kin = builder.getMobileBase().getLimbByName(name);
				show(kin);
				return;
			}

		}
	}

}
