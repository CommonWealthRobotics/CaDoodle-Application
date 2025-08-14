package com.commonwealthrobotics.robot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.controls.ControlSprites;
import com.commonwealthrobotics.controls.ResizingHandle;
import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.creature.MobileBaseBuilder;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.robot.ModifyLimb;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics;
import com.neuronrobotics.sdk.addons.kinematics.math.ITransformNRChangeListener;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
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
	private ModifyLimb mod ;
//	private Manipulation tipManip = new Manipulation(baseSelection, new Vector3d(1, 1, 0), new TransformNR());
//	private EventHandler<MouseEvent> tipMouseMover = tipManip.getMouseEvents();
//	private Manipulation baseManip = new Manipulation(baseSelection, new Vector3d(1, 1, 0), new TransformNR());
//	private EventHandler<MouseEvent> baseMouseMover = baseManip.getMouseEvents();
	ResizingHandle base = null;
	List<Node> handles;
	public LimbControlManager(BowlerStudio3dEngine engine,SelectionSession session,ActiveProject ap) {
		this.engine = engine;
		this.session = session;
		sprites = session.getControls();
		base=new ResizingHandle("Limb Base", engine, baseSelection, new Vector3d(1, 1, 0), baseSelection, ()->{
			updateLines();
		}, ()->{
			onReset();
		});
		base.setMyColor(Color.PINK);
		base.setBaseSize(1.25);
		handles=Arrays.asList(base.getMesh());
		base.manipulator.setFrameOfReference(() -> ap.get().getWorkplane());
		base.manipulator.addEventListener(ev -> {
			TransformNR base2 = mod.getBase().copy();
			System.out.println("from "+base2.toSimpleString());
			RotationNR nr= base2.getRotation();
			TransformNR tf = new TransformNR(base2.getX(),base2.getY(),base2.getZ()).times( base.manipulator.getCurrentPose());
			tf.setRotation(nr);
			mod.setBase(tf.copy());
			System.out.println("Moving "+tf.toSimpleString());
			limb.setRobotToFiducialTransform(tf);
			mod.setTip(limb.getCurrentTaskSpaceTransform());
			builder.getCadManager().render();
		});
		base.manipulator.addSaveListener(() ->{
			// add the operation and reset\
			update(builder);
			mod.setUndo(false);
			ModifyLimb myMod = mod;
			show(limb);
			myMod.getBase().addChangeListener(new ITransformNRChangeListener() {
				
				@Override
				public void event(TransformNR changed) {
					new RuntimeException().printStackTrace();
				}
			});
			try {
				ap.addOp(myMod).join();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			session.clearBoundsCache();
			BowlerStudio.runLater(()->	session.updateControls());
		});
		hide();
		for(Node n:handles) {
			n.setViewOrder(-2); 
			n.setDepthTest(DepthTest.DISABLE);
			engine.addUserNode(n);
		}
		
	}
	
	private void onReset() {
		System.out.println("Reset Limb Controller");
		base.resetSelected();
		base.manipulator.set(0, 0, 0);

	}

	private void updateLines() {
		// TODO Auto-generated method stub
		
	}

	public void show(DHParameterKinematics limb) {
		this.limb = limb;
		mod=new ModifyLimb().setLimb(limb).setNames(session.selectedSnapshot());
		base.show();
		mod.setUndo(true);
		onReset();
	}
	public void hide() {
		base.hide();
	}
	public void threeDTarget(double screenW, double screenH, double zoom, TransformNR cf, boolean locked) {
		if(limb==null)
			return;
		TransformNR target=limb.getRobotToFiducialTransform();
		base.threeDTarget(screenW, screenH, zoom, target, cf, locked);
		
	}
	public void update(MobileBaseBuilder builder) {
		this.builder = builder;
		if(builder==null) {
			limb=null;
			hide();
			return;
		}
		for(CSG c: session.getCurrentStateSelected()) {
			if(c.getLimbName().isPresent()) {
				String name=c.getLimbName().get();
				DHParameterKinematics kin= builder.getMobileBase().getLimbByName(name);
				show(kin);
				return;
			}
			
		}
	}

}
