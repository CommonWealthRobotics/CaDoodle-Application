package com.commonwealthrobotics.fillet;

import java.util.LinkedHashSet;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.RulerManager;
import com.commonwealthrobotics.WorkplaneManager;
import com.commonwealthrobotics.controls.SelectionSession;
import com.commonwealthrobotics.controls.SpriteDisplayMode;

import eu.mihosoft.vrl.v3d.CSG;

public class FilletUIManager {

	public void run(LinkedHashSet<CSG> selected, ActiveProject ap, SelectionSession session, WorkplaneManager workplane,
			RulerManager ruler) {
		session.setMode(SpriteDisplayMode.PLACING);
		workplane.pickPlane(() -> {
			ruler.disableRulerMode();
			session.save();
			// session.setMode(SpriteDisplayMode.Default);
			// session.updateControls();
		}, () -> { // Run always
			session.setMode(SpriteDisplayMode.Default);
			session.updateControls();
		}, ruler);
		session.setKeyBindingFocus();
	}

}
