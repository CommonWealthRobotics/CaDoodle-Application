package com.commonwealthrobotics;

import com.commonwealthrobotics.numbers.TextFieldDimention;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cylinder;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.transform.Affine;

public class RulerManager {

	private Affine wp = new Affine();
	private Affine buttonLoc = new Affine();
	private boolean active = false;
	private TransformNR newWP;
	private Button cancel = new Button("X");
	private ActiveProject ap;
	private Affine rulerOffset;
	private WorkplaneManager workplane;
	
	public RulerManager(ActiveProject ap) {
		this.ap=ap;
		
	}

//	public Group getRulerGroup() {
//		return rulerGroup;
//	}
	
	public double getOffset(TextFieldDimention dim) {
		switch(dim) {
		default:
			return 0;
		case X:
			return ap.get().getRulerLocation().getX();
		case Y:
			return ap.get().getRulerLocation().getY();
		case Z:
			return ap.get().getRulerLocation().getZ();		
		}
	}

	public void initialize(Group rulerGroup,Affine wpUpstream,Affine ro, Runnable OnCancle) {
		rulerOffset=ro;
		wp=wpUpstream;
		rulerGroup.getChildren().add(cancel);
		cancel.getTransforms().addAll(wp,ro);
		BowlerStudio.runLater(()->cancel.setVisible(false));
		cancel.setOnAction(ev->{
			setActive(false);
			OnCancle.run();
		});
		cancel.getTransforms().addAll(buttonLoc);
		BowlerStudio.runLater(()->TransformFactory.nrToAffine(
				new TransformNR(-10,-30,1,RotationNR.getRotationY(180)),
				buttonLoc));
		
		TransformNR rulerLocation = ap.get().getRulerLocation();
		BowlerStudio.runLater(()->{
			TransformFactory.nrToAffine(rulerLocation,rulerOffset);
		});
		if(Math.abs(rulerLocation.getX())>0.01 ||
				Math.abs(rulerLocation.getY())>0.01 ||
				Math.abs(rulerLocation.getZ())>0.01
				) {
			setActive(true);
		}
	}

	public void setWP(TransformNR n) {
		if (n==null)
			return;
		this.newWP = n.copy();
//		newWP.setX(0);
//		newWP.setY(0);
//		newWP.setZ(0);
		if(isActive())
			BowlerStudio.runLater(()->TransformFactory.nrToAffine(newWP, wp));
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
		if(!active) {
			BowlerStudio.runLater(()->TransformFactory.nrToAffine(new TransformNR(), wp));
			BowlerStudio.runLater(()->TransformFactory.nrToAffine(new TransformNR(), rulerOffset));
			ap.get().setRulerLocation(new TransformNR());
		}else {
			setWP(newWP);
		}
		BowlerStudio.runLater(()->cancel.setVisible(active));
	}

	public void startPick(Runnable onFinish) {
		com.neuronrobotics.sdk.common.Log.debug("Start Pick for Ruler");
		CSG csg = new Cylinder(0, 5,10,20).toCSG();
		CSG csg2 = new Cylinder(2, 2,10,20).toCSG().movez(10);
		workplane.setIndicator(csg.union(csg2), new Affine());
		workplane.setUpdater(tf->{
			TransformNR tmp = tf.copy();
			tmp.setRotation(new RotationNR());
			TransformFactory.nrToAffine(tmp, rulerOffset);
		});
		workplane.onCancle(()->{
			com.neuronrobotics.sdk.common.Log.debug("Canceling active ruler pick session");
			cancel();
			onFinish.run();
		});
		
		workplane.setOnSelectEvent(()->{
			if (workplane.isClicked()) {
				com.neuronrobotics.sdk.common.Log.debug("Placing ruler");
				ap.get().setRulerLocation(TransformFactory.affineToNr(rulerOffset));
			}else {
				cancel();
			}
			onFinish.run();
		});
		boolean workplaneInOrigin = workplane.isWorkplaneNotOrigin();
		workplane.activate(!workplaneInOrigin);
	}

	public void setWorkplane(WorkplaneManager workplane) {
		this.workplane = workplane;
		
	}

	public void cancel() {
		setActive(false);
	}

}
