package com.commonwealthrobotics.allign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.commonwealthrobotics.controls.SelectionSession;
import com.commonwealthrobotics.rotate.RotationHandle;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Allign;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleOpperation;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.scene.Node;
import javafx.scene.transform.Affine;

public class AllignManager {
	private static List<AllignRadioSet> AS_LIST = null;
	AllignRadioSet frontBack;
	AllignRadioSet leftRight;
	AllignRadioSet upDown;
	Allign opperation =null;
	private ArrayList<CSG> toAllign = new ArrayList<CSG>();
	private SelectionSession session;
	private boolean allignemntSelected = false;

	public AllignManager(SelectionSession session, Affine move, Affine workplaneOffset) {
		this.session = session;
		frontBack = new AllignRadioSet("frontBack", move, workplaneOffset, new Vector3d(1,0,0));
		leftRight = new AllignRadioSet("leftRight", move, workplaneOffset, new Vector3d(0,1,0));
		upDown = new AllignRadioSet("upDown", move, workplaneOffset, new Vector3d(0,0,1));
		AS_LIST = Arrays.asList(frontBack,leftRight,upDown);
		for(AllignRadioSet r: AS_LIST) {
			r.setOnClickCallback(()->{
				System.out.println("AllignManager clicked");
				setAllignemntSelected(true);
				recompute(()->{
					ICaDoodleOpperation curOp = session.getCadoodle().currentOpperation();
					if(curOp!= opperation)
						session.addOp(opperation);
					else
						session.getCadoodle().regenerateCurrent();
					
					session.selectAll(opperation.getNames());
				});
				
				
			});
		}
		hide();
	}
	public void threeDTarget(double screenW, double screenH, double zoom,Bounds b, TransformNR cf) {
		frontBack.threeDTarget(screenW, screenH, zoom, b, cf);
		leftRight.threeDTarget(screenW, screenH, zoom, b, cf);
		upDown.threeDTarget(screenW, screenH, zoom, b, cf);
	}
	public List<Node> getElements(){
		ArrayList<Node> result = new ArrayList<Node>(); 
		for(AllignRadioSet r: AS_LIST) {
			result.addAll(r. getElements());
		}
		return result;
	}
	public ArrayList<CSG> getToAllign() {
		return toAllign;
	}
	public void initialize(BowlerStudio3dEngine engine,List<CSG> toAllign, List<String> selected) {
		for(Node n:getElements()) {
			n.setVisible(true);
		}
		this.toAllign.clear();
		for(CSG c:toAllign)
			this.toAllign.add( c);
		opperation=new Allign().setNames(selected).setWorkplane(session.getWorkplane());
		System.out.println("Allign manager reinitialized");
		setAllignemntSelected(false);
		for(AllignRadioSet r: AS_LIST) {
			r.initialize(opperation,engine,toAllign,selected);
		}
		recompute(null);
	
	}
	private void recompute(Runnable r) {
		new Thread(()->{
			for(AllignRadioSet rs: AS_LIST) {
				rs.recomputeOps();
			}
			if(r!=null)
				r.run();
		}).start();
	}
	public boolean isActive() {
		return toAllign.size()>1;
	}
	public void cancel() {
		System.out.println("Allign canceled here");
		if(isActive()) {
			this.toAllign.clear();
			if(isAllignemntSelected() ) {
				System.out.println("Add op "+opperation);
				
			}
			opperation=null;
		}
		hide();
	}
	public void hide() {
		for(AllignRadioSet r: AS_LIST) {
			r.hide();
		}
		for(Node n:getElements()) {
			n.setVisible(false);
		}
	}
	public boolean isAllignemntSelected() {
		return allignemntSelected;
	}
	public void setAllignemntSelected(boolean allignemntSelected) {
		//new Exception("Allignment selected set to "+allignemntSelected).printStackTrace();
		this.allignemntSelected = allignemntSelected;
	}
}
