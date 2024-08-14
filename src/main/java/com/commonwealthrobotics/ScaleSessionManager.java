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
	private ControlRectangle beingUpdated=null;
	private Runnable updateLines;
	private Bounds bounds;
	private double screenW;
	private double screenH;
	private double zoom;
	public ScaleSessionManager(BowlerStudio3dEngine engine,Affine selection, Runnable updateLines, CaDoodleFile cadoodle,SelectionSession sel) {
		this.updateLines = updateLines;
		topCenter= new ControlRectangle("topCenter",engine,selection,new Vector3d(0, 0, 1));
		rightFront= new ControlRectangle("rightFront",engine,selection,new Vector3d(1, 1, 0));
		rightRear =new ControlRectangle("rightRear",engine,selection,new Vector3d(1, 1, 0));
		leftFront =new ControlRectangle("leftFront",engine,selection,new Vector3d(1, 1, 0));
		leftRear =new ControlRectangle("leftRear",engine,selection,new Vector3d(1, 1, 0));

		rightFront.manipulator.addEventListener(()->{
			if(beingUpdated==null)
				beingUpdated=rightFront;
			if(beingUpdated!=rightFront) {
				//System.out.println("Motion from "+beingUpdated+" rejected by "+rightFront);
				return;
			}
			double x=rightRear.manipulator.getCurrentPose().getX();
			double y=rightFront.manipulator.getCurrentPose().getY();
			double z=rightRear.manipulator.getCurrentPose().getZ();
			rightRear.manipulator.set(x, y,z);
			x=rightFront.manipulator.getCurrentPose().getX();
			y=leftFront.manipulator.getCurrentPose().getY();
			leftFront.manipulator.set(x, y,z);
			update();
			//System.out.println("rightFront");
		});
		rightRear.manipulator.addEventListener(()->{
			if(beingUpdated==null)
				beingUpdated=rightRear;
			if(beingUpdated!=rightRear) {
				//System.out.println("Motion from "+beingUpdated+" rejected by "+rightRear);
				return;
			}
			double x=rightFront.manipulator.getCurrentPose().getX();
			double y=rightRear.manipulator.getCurrentPose().getY();
			double z=rightFront.manipulator.getCurrentPose().getZ();
			rightFront.manipulator.set(x, y,z);
			x=rightRear.manipulator.getCurrentPose().getX();
			y=leftRear.manipulator.getCurrentPose().getY();
			leftRear.manipulator.set(x, y,z);
			update();
			//System.out.println("rightRear");
		});
		leftFront.manipulator.addEventListener(()->{
			if(beingUpdated==null)
				beingUpdated=leftFront;
			if(beingUpdated!=leftFront) {
				//System.out.println("Motion from "+beingUpdated+" rejected by "+leftFront);
				return;
			}
			double x=leftRear.manipulator.getCurrentPose().getX();
			double y=leftFront.manipulator.getCurrentPose().getY();
			double z=leftFront.manipulator.getCurrentPose().getZ();
			leftRear.manipulator.set(x, y,z);
			x=leftFront.manipulator.getCurrentPose().getX();
			y=rightFront.manipulator.getCurrentPose().getY();
			rightFront.manipulator.set(x, y,z);
			update();
			//System.out.println("leftFront");
		});
		leftRear.manipulator.addEventListener(()->{
			if(beingUpdated==null)
				beingUpdated=leftRear;
			if(beingUpdated!=leftRear) {
				//System.out.println("Motion from "+beingUpdated+" rejected by "+leftRear);
				return;
			}
			double x=leftFront.manipulator.getCurrentPose().getX();
			double y=leftRear.manipulator.getCurrentPose().getY();
			double z=leftRear.manipulator.getCurrentPose().getZ();
			leftFront.manipulator.set(x, y,z);
			x=leftRear.manipulator.getCurrentPose().getX();
			y=rightRear.manipulator.getCurrentPose().getY();
			rightRear.manipulator.set(x, y,z);
			update();
			//System.out.println("leftRear");
		});
		topCenter.manipulator.addEventListener(()->{
			if(beingUpdated==null)
				beingUpdated=topCenter;
			if(beingUpdated!=topCenter) {
				//System.out.println("Motion from "+beingUpdated+" rejected by "+topCenter);
				return;
			}
			update();
			//System.out.println("topCenter");
		});
		controls = Arrays.asList(topCenter,rightFront ,rightRear ,leftFront,leftRear);
		for(ControlRectangle c:controls) {
			c.manipulator.addSaveListener(()->{
				//System.out.println("Saving from "+c);
				Resize setResize = new Resize()
						.setNames(sel.selectedSnapshot())
						.setResize(topCenter.getCurrent(), leftFront.getCurrent(), rightRear.getCurrent());
				bounds=getBounds();
				for(ControlRectangle ctrl:controls) {
					ctrl.manipulator.set(0, 0, 0);
				}
				
				Thread t=cadoodle.addOpperation(setResize);
				try {
					t.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				beingUpdated=null;
				threeDTarget();
			});
		}
	}
	
	private void update() {
		updateLines.run();
		//if(beingUpdated!=null)
			ControlRectangle beingUpdated2 = beingUpdated;
			if(beingUpdated2!=topCenter || beingUpdated2==null) {
				Bounds b=getBounds();
				double x = b.getCenter().x - bounds.getCenter().x;
				double y = b.getCenter().y - bounds.getCenter().y;
				
				topCenter.manipulator.set(x, y,0);
				beingUpdated=beingUpdated2;
			}else {
				//System.out.println("Not updating center cube "+beingUpdated2);
			}
	}
	
	public double getScale(){
		return topCenter.getScale();
	}
	public void threeDTarget(double w, double h, double z, Bounds b){
	
		this.screenW = w;
		this.screenH = h;
		this.zoom = z;
		this.bounds = b;
		threeDTarget();
	}

	private void threeDTarget() {
		
		Vector3d center = bounds.getCenter();
		
		Vector3d min = bounds.getMin();
		Vector3d max = bounds.getMax();
	
		topCenter.threeDTarget(screenW,screenH,zoom, new TransformNR(center.x,center.y,max.z));
		leftFront.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, max.y, min.z));
		leftRear.threeDTarget(screenW, screenH, zoom, new TransformNR(min.x, max.y,  min.z));
		rightFront.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, min.y,  min.z));
		rightRear.threeDTarget(screenW,screenH,zoom, new TransformNR(min.x,min.y, min.z));
		update();
	}
	public Bounds getBounds() {
		TransformNR lr= rightRear.getCurrent();
		TransformNR rf = leftFront.getCurrent();
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
