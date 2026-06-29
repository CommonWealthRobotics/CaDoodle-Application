package com.commonwealthrobotics.fillet;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.RulerManager;
import com.commonwealthrobotics.WorkplaneManager;
import com.commonwealthrobotics.controls.SelectionSession;
import com.commonwealthrobotics.controls.SpriteDisplayMode;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ExtrudeSurface;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cylinder;
import javafx.scene.transform.Affine;

public class ExtrudeUIManager {

	public void run(LinkedHashSet<CSG> selected, ActiveProject ap, SelectionSession session, WorkplaneManager workplane,
			RulerManager ruler) {
		session.setMode(SpriteDisplayMode.PLACING);

		CSG indicator = new Cylinder(5 / 2.0, 0, 5, 20).toCSG();

		workplane.setIndicator(indicator, new Affine());

		session.updateHandleOrientations(session.engine.getFlyingCamera().getCamerFrame());
		workplane.placeWorkplaneVisualization();

		workplane.setOnSelectEvent(() -> {

			if (workplane.isClicked()) {

				session.getExecutor().execute(() -> {
					if (workplane.isClickOnGround()) {
						// com.neuronrobotics.sdk.common.Log.debug("Ground plane click detected");
						ap.get().setWorkplane(new TransformNR());
					} else {
						ap.get().setWorkplane(workplane.getCurrentAbsolutePose());
					}
					Set<String> selectedSet = new HashSet<String>();
					for (CSG c : selected)
						selectedSet.add(c.getName());
					ExtrudeSurface op = new ExtrudeSurface().setWorkplane(workplane.getCurrentAbsolutePose())
							.setToExtrude(selectedSet);
					Thread thread = ap.addOp(op);
					session.clearSelection();
					try {
						thread.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					List<String> added = op.getNamesAddedInThisOperation();
					session.selectAll(added);

					workplane.placeWorkplaneVisualization();
					ruler.disableRulerMode();
					session.save();
					session.setMode(SpriteDisplayMode.Default);
					session.updateControls();
				});
			}


		});

		workplane.activate(true);
		session.setKeyBindingFocus();
	}

}
