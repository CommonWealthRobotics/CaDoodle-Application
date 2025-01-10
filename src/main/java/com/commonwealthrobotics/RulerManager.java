package com.commonwealthrobotics;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import javafx.scene.Group;
import javafx.scene.transform.Affine;

public class RulerManager {

	private Group rulerGroup;
	private Affine wp = new Affine();

	public Group getRulerGroup() {
		return rulerGroup;
	}

	public void setRulerGroup(Group rulerGroup) {
		this.rulerGroup = rulerGroup;
		rulerGroup.getTransforms().add(wp);
		
	}

	public void setWP(TransformNR newWP) {
		BowlerStudio.runLater(()->TransformFactory.nrToAffine(newWP, wp));
	}

}
