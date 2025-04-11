package com.commonwealthrobotics;

import com.commonwealthrobotics.numbers.TextFieldDimention;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.transform.Affine;

public class RulerManager {

	private Group rulerGroup;
	private Affine wp = new Affine();
	private Affine buttonLoc = new Affine();
	private boolean active = false;
	private TransformNR newWP;
	private Button cancel = new Button("X");
	private ActiveProject ap;
	private Affine rulerOffset;
	
	public RulerManager(ActiveProject ap) {
		this.ap=ap;
		
	}

	public Group getRulerGroup() {
		return rulerGroup;
	}
	
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

	public void setRulerGroup(Group rulerGroup) {
		this.rulerGroup = rulerGroup;
		rulerGroup.getTransforms().add(wp);
		rulerGroup.getChildren().add(cancel);
		BowlerStudio.runLater(()->cancel.setVisible(false));
		cancel.setOnAction(ev->{
			setActive(false);
		});
		cancel.getTransforms().add(buttonLoc);
		BowlerStudio.runLater(()->TransformFactory.nrToAffine(
				new TransformNR(-10,-30,1,RotationNR.getRotationY(180)),
				buttonLoc));
	}

	public void setWP(TransformNR newWP) {
		this.newWP = newWP;
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
		}else {
			setWP(newWP);
		}
		BowlerStudio.runLater(()->cancel.setVisible(active));
	}


	public void initializeRulerOffset(Affine rulerOffset) {
		this.rulerOffset = rulerOffset;
		BowlerStudio.runLater(()->{
			TransformFactory.nrToAffine(ap.get().getRulerLocation(),rulerOffset);
		});
	}

}
