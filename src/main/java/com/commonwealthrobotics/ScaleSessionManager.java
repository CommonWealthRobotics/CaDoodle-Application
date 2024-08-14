package com.commonwealthrobotics;

import java.util.Arrays;
import java.util.List;

import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Resize;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.scene.transform.Affine;

public class ScaleSessionManager {
	ControlRectangle topCenter = null;
	ControlRectangle rightFront =null;
	ControlRectangle rightRear =null;
	ControlRectangle leftFront =null;
	ControlRectangle leftRear =null;
	private List<ControlRectangle> controls;
	public ScaleSessionManager(BowlerStudio3dEngine engine,Affine selection, Runnable updateLines, CaDoodleFile cadoodle,SelectionSession sel) {
		topCenter= new ControlRectangle(engine,selection,new Vector3d(0, 0, 1));
		rightFront= new ControlRectangle(engine,selection,new Vector3d(1, 1, 0));
		rightRear =new ControlRectangle(engine,selection,new Vector3d(1, 1, 0));
		leftFront =new ControlRectangle(engine,selection,new Vector3d(1, 1, 0));
		leftRear =new ControlRectangle(engine,selection,new Vector3d(1, 1, 0));

		rightFront.manipulator.addEventListener(()->{
			double x=rightRear.manipulator.getCurrentPose().getX();
			double y=rightFront.manipulator.getCurrentPose().getY();
			double z=rightRear.manipulator.getCurrentPose().getZ();
			rightRear.manipulator.set(x, y,z);
			x=rightFront.manipulator.getCurrentPose().getX();
			y=leftFront.manipulator.getCurrentPose().getY();
			leftFront.manipulator.set(x, y,z);
			updateLines.run();
		});
		rightRear.manipulator.addEventListener(()->{
			updateLines.run();
		});
		leftFront.manipulator.addEventListener(()->{
			updateLines.run();
		});
		leftRear.manipulator.addEventListener(()->{
			updateLines.run();
		});
		topCenter.manipulator.addEventListener(()->{
			updateLines.run();
		});
		controls = Arrays.asList(topCenter,rightFront ,rightRear ,leftFront,leftRear);
		for(ControlRectangle c:controls) {
			c.manipulator.addSaveListener(()->{
				cadoodle.addOpperation(new Resize()
						.setNames(sel.selectedSnapshot())
						.setResize(topCenter.getCurrent(), rightFront.getCurrent(), leftRear.getCurrent())
						);
				for(ControlRectangle ctrl:controls) {
					ctrl.manipulator.set(0, 0, 0);
				}
			});
		}
	}
	
	public double getScale(){
		return topCenter.getScale();
	}
	public void threeDTarget(double screenW, double screenH, double zoom, Bounds bounds){
		Vector3d center = bounds.getCenter();
		Vector3d min = bounds.getMin();
		Vector3d max = bounds.getMax();
		topCenter.threeDTarget(screenW,screenH,zoom, new TransformNR(center.x,center.y,max.z));
		rightFront.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, max.y, min.z));
		rightRear.threeDTarget(screenW, screenH, zoom, new TransformNR(min.x, max.y,  min.z));
		leftFront.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, min.y,  min.z));
		leftRear.threeDTarget(screenW,screenH,zoom, new TransformNR(min.x,min.y, min.z));
	}
	public Bounds getBounds() {
		TransformNR lr= leftRear.getCurrent();
		TransformNR rf = rightFront.getCurrent();
		Vector3d min= new Vector3d(lr.getX(),lr.getY(), lr.getZ());
		Vector3d max=new Vector3d(rf.getX(),rf.getY(), topCenter.getCurrent().getZ());;
		
		return new Bounds(min,max);
	}

	public void setSnapGrid(double size) {
		for(ControlRectangle ctrl:controls) {
			ctrl.manipulator.setIncrement(size);
		}
	}
}
