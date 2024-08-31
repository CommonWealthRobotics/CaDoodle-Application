package com.commonwealthrobotics.allign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Allign;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Allignment;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.scene.Node;
import javafx.scene.transform.Affine;

public class AllignRadioSet {
	AllignHandle positive=null;
	AllignHandle middle =null;
	AllignHandle negetive=null;
	private String name;
	private Vector3d vector3d;
	private List<AllignHandle> asList;
	public AllignRadioSet(String name,Affine move, Affine workplaneOffset, Vector3d vector3d){
		this.name = name;
		this.vector3d = vector3d;
		positive = new AllignHandle(Allignment.positive,move,workplaneOffset,vector3d);
		middle = new AllignHandle(Allignment.middle,move,workplaneOffset,vector3d);
		negetive = new AllignHandle(Allignment.negative,move,workplaneOffset,vector3d);
		asList = Arrays.asList(positive,middle,negetive);

	}
	public void threeDTarget(double screenW, double screenH, double zoom,Bounds b, TransformNR cf) {
		positive.threeDTarget(screenW, screenH, zoom, b, cf);
		middle.threeDTarget(screenW, screenH, zoom, b, cf);
		negetive.threeDTarget(screenW, screenH, zoom, b, cf);
	}
	public List<Node> getElements(){
		ArrayList<Node> result = new ArrayList<Node>(); 
		for(AllignHandle r: asList) {
			result.add(r.getHandle());
		}
		return result;
	}
	public void initialize(Allign opperation, BowlerStudio3dEngine engine, List<CSG> toAllign, List<String> selected) {
		for(AllignHandle r: asList) {
			r.initialize(opperation,engine,toAllign,selected);
		}
	}
	public void hide() {
		for(AllignHandle r: asList) {
			r.hide();
		}
	}
}
