package com.commonwealthrobotics.align;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.controls.SelectionSession;
import com.commonwealthrobotics.rotate.RotationHandle;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Align;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;

public class AlignManager {
	private static List<AlignRadioSet> AS_LIST = null;
	AlignRadioSet frontBack;
	AlignRadioSet leftRight;
	AlignRadioSet upDown;
	Align operation = null;
	private ArrayList<CSG> toAlign = new ArrayList<CSG>();
	private SelectionSession session;
	private boolean alignemntSelected = false;
	private HashMap<CSG, MeshView> meshes;
	private HashMap<CSG, EventHandler<? super MouseEvent>> events = new HashMap<CSG, EventHandler<? super MouseEvent>>();
	private double screenW;
	private double screenH;
	private double zoom;
	private TransformNR cf;
	private Bounds b;
	private ActiveProject ap;

	public AlignManager(SelectionSession session, Affine move, Affine workplaneOffset,ActiveProject ap) {
		this.session = session;
		this.ap = ap;
		frontBack = new AlignRadioSet("frontBack", move, workplaneOffset, new Vector3d(1, 0, 0),ap);
		leftRight = new AlignRadioSet("leftRight", move, workplaneOffset, new Vector3d(0, 1, 0),ap);
		upDown = new AlignRadioSet("upDown", move, workplaneOffset, new Vector3d(0, 0, 1),ap);
		AS_LIST = Arrays.asList(frontBack, leftRight, upDown);
		for (AlignRadioSet r : AS_LIST) {
			r.setOnClickCallback(() -> {
				
				setAlignemntSelected(true);
				recompute(() -> {
					CaDoodleOperation curOp = session.getCurrentOperation();
					if (curOp != operation && operation!=null)
						ap.addOp(operation);
					else
						ap.get().regenerateCurrent();
					com.neuronrobotics.sdk.common.Log.debug("AlignManager clicked "+operation);
					List<String> names = operation.getNamesAddedInThisOperation();
					session.selectAll(names);
				});

			});
		}
		hide();
	}
	public void clear() {
		for (AlignRadioSet r : AS_LIST) 
			r.clear();
	}
	public void threeDTarget(double w, double h, double z, Bounds bo, TransformNR c) {
		this.screenW = w;
		this.screenH = h;
		this.zoom = z;
		this.b = bo;
		this.cf = c;
		updateHandles();

	}

	private void updateHandles() {
		if(operation!=null)
			b = operation.getBounds(ap.get().getCurrentState());
		frontBack.threeDTarget(screenW, screenH, zoom, b, cf);
		leftRight.threeDTarget(screenW, screenH, zoom, b, cf);
		upDown.threeDTarget(screenW, screenH, zoom, b, cf);
	}

	public List<Node> getElements() {
		ArrayList<Node> result = new ArrayList<Node>();
		for (AlignRadioSet r : AS_LIST) {
			result.addAll(r.getElements());
		}
		return result;
	}

	public ArrayList<CSG> getToAlign() {
		return toAlign;
	}

	public void initialize(List<String> boundNames, BowlerStudio3dEngine engine, List<CSG> ta, List<String> selected,
			HashMap<CSG, MeshView> meshes) {
		this.meshes = meshes;
		for (Node n : getElements()) {
			BowlerStudio.runLater(()->n.setVisible(true));
			;
		}
		this.toAlign.clear();
		for (CSG c : ta)
			this.toAlign.add(c);
		operation = new Align().setNames(selected).setWorkplane(session.getWorkplane());
		operation.setBounds(boundNames);

		com.neuronrobotics.sdk.common.Log.error("Align manager reinitialized");
		setAlignemntSelected(false);
		for (AlignRadioSet r : AS_LIST) {
			r.initialize(operation, engine, toAlign, selected);
		}
		recompute(null);
		for (CSG c : toAlign) {
			MeshView mv = meshes.get(c);
			EventHandler<? super MouseEvent> eventFilter = event -> {
				if(operation==null)
					return;
				operation.setBounds(Arrays.asList(c.getName()));
				recompute(null);
				updateHandles();
			};
			mv.addEventFilter(MouseEvent.MOUSE_CLICKED, eventFilter);
			events.put(c, eventFilter);
		}
	}

	private void recompute(Runnable r) {
		new Thread(() -> {
			for (AlignRadioSet rs : AS_LIST) {
				rs.recomputeOps();
			}
			if (r != null)
				r.run();
		}).start();
	}

	public boolean isActive() {
		return toAlign.size() > 1;
	}

	public void cancel() {

		com.neuronrobotics.sdk.common.Log.debug("Align canceled here");
		if (isActive()) {
			this.toAlign.clear();
			if (isAlignemntSelected()) {
				com.neuronrobotics.sdk.common.Log.debug("Add op " + operation);
			}
			operation = null;
		}
		hide();
		for (CSG c : toAlign) {
			MeshView mv = meshes.get(c);
			EventHandler<? super MouseEvent> eventFilter = events.remove(c);
			mv.removeEventFilter(MouseEvent.MOUSE_CLICKED, eventFilter);
		}
	}

	public void hide() {
		for (AlignRadioSet r : AS_LIST) {
			r.hide();
		}
		for (Node n : getElements()) {
			BowlerStudio.runLater(()->n.setVisible(false));
		}
	}

	public boolean isAlignemntSelected() {
		return alignemntSelected;
	}

	public void setAlignemntSelected(boolean alignemntSelected) {
		// new Exception("Alignment selected set to
		// "+alignemntSelected).printStackTrace();
		this.alignemntSelected = alignemntSelected;
	}
}
