package com.commonwealthrobotics.mirror;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.controls.ControlSprites;
import com.commonwealthrobotics.rotate.RotationSessionManager;
import com.neuronrobotics.sdk.addons.kinematics.math.EulerAxis;

import javafx.scene.transform.Affine;

public class MirrorHandle {
	private EulerAxis ax;
	private Affine translate;
	private Affine vr;
	private MirrorSessionManager rotationSessionManager;
	private ActiveProject ap;
	private ControlSprites cs;
	private Affine workplaneOffset;

	public MirrorHandle(EulerAxis ax, Affine translate, Affine vr,
			MirrorSessionManager rotationSessionManager, ActiveProject ap, ControlSprites cs, Affine workplaneOffset) {
				this.ax = ax;
				this.translate = translate;
				this.vr = vr;
				this.rotationSessionManager = rotationSessionManager;
				this.ap = ap;
				this.cs = cs;
				this.workplaneOffset = workplaneOffset;
		
	}
}
