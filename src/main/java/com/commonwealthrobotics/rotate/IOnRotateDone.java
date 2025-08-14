package com.commonwealthrobotics.rotate;

import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

public interface IOnRotateDone {
	public void toUpdate(TransformNR toUpdate );
}
