package com.commonwealthrobotics.fillet;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.RulerManager;
import com.commonwealthrobotics.WorkplaneManager;
import com.commonwealthrobotics.controls.SelectionSession;
import com.commonwealthrobotics.controls.SpriteDisplayMode;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.FilletChamfer;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Fillet;
import javafx.scene.transform.Affine;

public class FilletUIManager {

	public void run(LinkedHashSet<CSG> selected, ActiveProject ap, SelectionSession session, WorkplaneManager workplane,
			RulerManager ruler) {
		session.setMode(SpriteDisplayMode.PLACING);


		CSG indicator = new Fillet(2, 2).toCSG().rotz(90);

		workplane.setIndicator(indicator, new Affine());

		ap.get().setWorkplane(new TransformNR());
		session.updateHandleOrientations(session.engine.getFlyingCamera().getCamerFrame());
		workplane.placeWorkplaneVisualization();

		workplane.setOnSelectEvent(() -> {

			if (workplane.isClicked()) {
				ruler.disableRulerMode();
				if (workplane.isClickOnGround()) {
					// com.neuronrobotics.sdk.common.Log.debug("Ground plane click detected");
					ap.get().setWorkplane(new TransformNR());
				} else {
					ap.get().setWorkplane(workplane.getCurrentAbsolutePose());
				}
				Set<String> selectedSet = new HashSet<String>();
				for (CSG c : selected)
					selectedSet.add(c.getName());
				FilletChamfer op = new FilletChamfer().setWorkplane(ap.get().getWorkplane()).setToFillet(selectedSet);
				Thread thread = ap.addOp(op);
				session.clearSelection();
				session.getExecutor().execute(() -> {
					try {
						thread.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					HashSet<String> added = op.getNamesAdded();
					session.selectAll(added);
				});
				workplane.placeWorkplaneVisualization();
				ruler.disableRulerMode();
				session.save();
			}
			session.setMode(SpriteDisplayMode.Default);
			session.updateControls();

		});

		workplane.activate(true);
		session.setKeyBindingFocus();
	}

}
