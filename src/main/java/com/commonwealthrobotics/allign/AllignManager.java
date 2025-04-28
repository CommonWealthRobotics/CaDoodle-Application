package com.commonwealthrobotics.allign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.commonwealthrobotics.controls.SelectionSession;
import com.commonwealthrobotics.rotate.RotationHandle;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Allign;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleOpperation;
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

public class AllignManager {
	private static List<AllignRadioSet> AS_LIST = null;
	AllignRadioSet frontBack;
	AllignRadioSet leftRight;
	AllignRadioSet upDown;
	Allign opperation = null;
	private ArrayList<CSG> toAllign = new ArrayList<CSG>();
	private SelectionSession session;
	private boolean allignemntSelected = false;
	private HashMap<CSG, MeshView> meshes;
	private HashMap<CSG, EventHandler<? super MouseEvent>> events = new HashMap<CSG, EventHandler<? super MouseEvent>>();
	private double screenW;
	private double screenH;
	private double zoom;
	private TransformNR cf;
	private Bounds b;

	public AllignManager(SelectionSession session, Affine move, Affine workplaneOffset) {
		this.session = session;
		frontBack = new AllignRadioSet("frontBack", move, workplaneOffset, new Vector3d(1, 0, 0));
		leftRight = new AllignRadioSet("leftRight", move, workplaneOffset, new Vector3d(0, 1, 0));
		upDown = new AllignRadioSet("upDown", move, workplaneOffset, new Vector3d(0, 0, 1));
		AS_LIST = Arrays.asList(frontBack, leftRight, upDown);
		for (AllignRadioSet r : AS_LIST) {
			r.setOnClickCallback(() -> {
				com.neuronrobotics.sdk.common.Log.error("AllignManager clicked");
				setAllignemntSelected(true);
				recompute(() -> {
					ICaDoodleOpperation curOp = session.getCurrentOpperation();
					if (curOp != opperation)
						session.addOp(opperation);
					else
						session.regenerateCurrent();

					List<String> names = opperation.getNames();
					session.selectAll(names);
				});

			});
		}
		hide();
	}
	public void clear() {
		for (AllignRadioSet r : AS_LIST) 
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
		if(opperation!=null)
			b = opperation.getBounds();
		frontBack.threeDTarget(screenW, screenH, zoom, b, cf);
		leftRight.threeDTarget(screenW, screenH, zoom, b, cf);
		upDown.threeDTarget(screenW, screenH, zoom, b, cf);
	}

	public List<Node> getElements() {
		ArrayList<Node> result = new ArrayList<Node>();
		for (AllignRadioSet r : AS_LIST) {
			result.addAll(r.getElements());
		}
		return result;
	}

	public ArrayList<CSG> getToAllign() {
		return toAllign;
	}

	public void initialize(Bounds b, BowlerStudio3dEngine engine, List<CSG> ta, List<String> selected,
			HashMap<CSG, MeshView> meshes) {
		this.meshes = meshes;
		for (Node n : getElements()) {
			BowlerStudio.runLater(()->n.setVisible(true));
			;
		}
		this.toAllign.clear();
		for (CSG c : ta)
			this.toAllign.add(c);
		opperation = new Allign().setNames(selected).setWorkplane(session.getWorkplane());
		opperation.setBounds(b);

		com.neuronrobotics.sdk.common.Log.error("Allign manager reinitialized");
		setAllignemntSelected(false);
		for (AllignRadioSet r : AS_LIST) {
			r.initialize(opperation, engine, toAllign, selected);
		}
		recompute(null);
		for (CSG c : toAllign) {
			MeshView mv = meshes.get(c);
			EventHandler<? super MouseEvent> eventFilter = event -> {
				if(opperation==null)
					return;
				
				Bounds bounds = session.getSellectedBounds(Arrays.asList(c));
				opperation.setBounds(bounds);
				recompute(null);
				updateHandles();
			};
			mv.addEventFilter(MouseEvent.MOUSE_CLICKED, eventFilter);
			events.put(c, eventFilter);
		}
	}

	private void recompute(Runnable r) {
		new Thread(() -> {
			for (AllignRadioSet rs : AS_LIST) {
				rs.recomputeOps();
			}
			if (r != null)
				r.run();
		}).start();
	}

	public boolean isActive() {
		return toAllign.size() > 1;
	}

	public void cancel() {

		com.neuronrobotics.sdk.common.Log.error("Allign canceled here");
		if (isActive()) {
			this.toAllign.clear();
			if (isAllignemntSelected()) {
				com.neuronrobotics.sdk.common.Log.error("Add op " + opperation);
			}
			opperation = null;
		}
		hide();
		for (CSG c : toAllign) {
			MeshView mv = meshes.get(c);
			EventHandler<? super MouseEvent> eventFilter = events.remove(c);
			mv.removeEventFilter(MouseEvent.MOUSE_CLICKED, eventFilter);
		}
	}

	public void hide() {
		for (AllignRadioSet r : AS_LIST) {
			r.hide();
		}
		for (Node n : getElements()) {
			BowlerStudio.runLater(()->n.setVisible(false));
		}
	}

	public boolean isAllignemntSelected() {
		return allignemntSelected;
	}

	public void setAllignemntSelected(boolean allignemntSelected) {
		// new Exception("Allignment selected set to
		// "+allignemntSelected).printStackTrace();
		this.allignemntSelected = allignemntSelected;
	}
}
