package com.commonwealthrobotics.mirror;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.controls.ControlSprites;

import javafx.scene.transform.Affine;

public class MirrorSessionManager {
	private Affine selection;
	private ControlSprites controlSprites;
	
	public MirrorSessionManager(Affine selection, ActiveProject ap, ControlSprites controlSprites, Affine workplaneOffset) {
		this.selection = selection;
		this.controlSprites = controlSprites;
		
	}
}
