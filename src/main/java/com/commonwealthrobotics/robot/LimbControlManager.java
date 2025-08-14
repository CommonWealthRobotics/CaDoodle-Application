package com.commonwealthrobotics.robot;

import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics;

public class LimbControlManager {

	private BowlerStudio3dEngine engine;
	private DHParameterKinematics limb;

	public LimbControlManager(BowlerStudio3dEngine engine) {
		this.engine = engine;
	}
	
	public void show(DHParameterKinematics limb) {
		this.limb = limb;
		
	}
	public void hide() {
		
	}

}
