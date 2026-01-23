package com.commonwealthrobotics.align;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.commonwealthrobotics.ActiveProject;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Align;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Alignment;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.scene.Node;
import javafx.scene.transform.Affine;

public class AlignRadioSet {
	AlignHandle positive=null;
	AlignHandle middle =null;
	AlignHandle negetive=null;
	private String name;
	private Vector3d vector3d;
	private List<AlignHandle> asList;
	public AlignRadioSet(String name,Affine move, Affine workplaneOffset, Vector3d vector3d,ActiveProject ap){
		this.name = name;
		this.vector3d = vector3d;
		positive = new AlignHandle(Alignment.positive,move,workplaneOffset,vector3d,ap);
		middle = new AlignHandle(Alignment.middle,move,workplaneOffset,vector3d,ap);
		negetive = new AlignHandle(Alignment.negative,move,workplaneOffset,vector3d,ap);
		asList = Arrays.asList(positive,middle,negetive);

	}
	public void threeDTarget(double screenW, double screenH, double zoom,Bounds b, TransformNR cf) {
		positive.threeDTarget(screenW, screenH, zoom, b, cf);
		middle.threeDTarget(screenW, screenH, zoom, b, cf);
		negetive.threeDTarget(screenW, screenH, zoom, b, cf);
	}
	public List<Node> getElements(){
		ArrayList<Node> result = new ArrayList<Node>(); 
		for(AlignHandle r: asList) {
			result.add(r.getHandle());
		}
		return result;
	}
	public void initialize(Align operation, BowlerStudio3dEngine engine, List<CSG> toAlign, List<String> selected) {
		for(AlignHandle r: asList) {
			r.initialize(operation,engine,toAlign,selected);
		}
	}
	public void hide() {
		for(AlignHandle r: asList) {
			r.hide();
		}
	}
	public void setOnClickCallback(Runnable onClick) {
		for(AlignHandle r: asList) {
			r.setOnClickCallback(()->{
				com.neuronrobotics.sdk.common.Log.error("Radio group click ");
				for(AlignHandle ah: asList) {
					ah.reset();
				}
				onClick.run();
			});			
		}
	}
	public void recomputeOps() {
		for(AlignHandle ah: asList) {
			ah.recomputeOps();
		}
	}
	public void clear() {
		for(AlignHandle ah: asList) {
			ah.clear();
		}
	}
}
