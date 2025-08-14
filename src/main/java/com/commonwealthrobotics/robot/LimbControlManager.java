package com.commonwealthrobotics.robot;

import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics;

public class LimbControlManager {

	private BowlerStudio3dEngine engine;
	private DHParameterKinematics limb;
	private SelectionSession session;

	public LimbControlManager(BowlerStudio3dEngine engine,SelectionSession session) {
		this.engine = engine;
		this.session = session;
	}
	
	public void show(DHParameterKinematics limb) {
		this.limb = limb;
		
	}
	public void hide() {
		
	}

	public void update() {
		
	}

}
