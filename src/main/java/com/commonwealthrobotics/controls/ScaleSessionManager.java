package com.commonwealthrobotics.controls;

import java.util.Arrays;
import java.util.List;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Resize;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.scene.transform.Affine;

public class ScaleSessionManager {
	ResizingHandle topCenter = null;
	ResizingHandle rightFront = null;
	ResizingHandle rightRear = null;
	ResizingHandle leftFront = null;
	ResizingHandle leftRear = null;
	private List<ResizingHandle> controls;
	private ResizingHandle beingUpdated = null;
	private Runnable updateLines;
	private Bounds bounds;
	private double screenW;
	private double screenH;
	private double zoom;
	private double size = 1;
	private BowlerStudio3dEngine engine;
	// private Affine workplaneOffset;
	private TransformNR cf;

	public ScaleSessionManager(BowlerStudio3dEngine engine, Affine selection, Runnable updateLines,
			CaDoodleFile cadoodle, SelectionSession sel, Affine workplaneOffset, MoveUpArrow up) {
		this.engine = engine;
		this.updateLines = updateLines;
		// this.workplaneOffset = workplaneOffset;
		Runnable onSelect = () -> {
			resetSelected();
			updateLines.run();
			up.resetSelected();
		};
		topCenter = new ResizingHandle("topCenter", engine, selection, new Vector3d(0, 0, 1), workplaneOffset,
				onSelect);
		rightFront = new ResizingHandle("rightFront", engine, selection, new Vector3d(1, 1, 0), workplaneOffset,
				onSelect);
		rightRear = new ResizingHandle("rightRear", engine, selection, new Vector3d(1, 1, 0), workplaneOffset,
				onSelect);
		leftFront = new ResizingHandle("leftFront", engine, selection, new Vector3d(1, 1, 0), workplaneOffset,
				onSelect);
		leftRear = new ResizingHandle("leftRear", engine, selection, new Vector3d(1, 1, 0), workplaneOffset, onSelect);

		rightFront.manipulator.addEventListener(() -> {
			if (beingUpdated == null)
				beingUpdated = rightFront;
			if (beingUpdated != rightFront) {
				// System.out.println("Motion from "+beingUpdated+" rejected by "+rightFront);
				return;
			}
			double x = rightRear.manipulator.getCurrentPose().getX();
			double y = rightFront.manipulator.getCurrentPose().getY();
			double z = rightRear.manipulator.getCurrentPose().getZ();
			System.out.println("rightRear Move x" + x + " y" + y + " z" + z);
			rightRear.manipulator.setInReferenceFrame(x, y, z);
			x = rightFront.manipulator.getCurrentPose().getX();
			y = leftFront.manipulator.getCurrentPose().getY();
			leftFront.manipulator.setInReferenceFrame(x, y, z);
			BowlerStudio.runLater(() -> update());
			// System.out.println("rightFront");
		});
		rightRear.manipulator.addEventListener(() -> {
			if (beingUpdated == null)
				beingUpdated = rightRear;
			if (beingUpdated != rightRear) {
				// System.out.println("Motion from "+beingUpdated+" rejected by "+rightRear);
				return;
			}
			double x = rightFront.manipulator.getCurrentPose().getX();
			double y = rightRear.manipulator.getCurrentPose().getY();
			double z = rightFront.manipulator.getCurrentPose().getZ();
			rightFront.manipulator.setInReferenceFrame(x, y, z);
			x = rightRear.manipulator.getCurrentPose().getX();
			y = leftRear.manipulator.getCurrentPose().getY();
			leftRear.manipulator.setInReferenceFrame(x, y, z);
			BowlerStudio.runLater(() -> update());
			// System.out.println("rightRear");
		});
		leftFront.manipulator.addEventListener(() -> {
			if (beingUpdated == null)
				beingUpdated = leftFront;
			if (beingUpdated != leftFront) {
				// System.out.println("Motion from "+beingUpdated+" rejected by "+leftFront);
				return;
			}
			double x = leftRear.manipulator.getCurrentPose().getX();
			double y = leftFront.manipulator.getCurrentPose().getY();
			double z = leftFront.manipulator.getCurrentPose().getZ();
			leftRear.manipulator.setInReferenceFrame(x, y, z);
			x = leftFront.manipulator.getCurrentPose().getX();
			y = rightFront.manipulator.getCurrentPose().getY();
			rightFront.manipulator.setInReferenceFrame(x, y, z);
			BowlerStudio.runLater(() -> update()); // System.out.println("leftFront");
		});
		leftRear.manipulator.addEventListener(() -> {
			if (beingUpdated == null)
				beingUpdated = leftRear;
			if (beingUpdated != leftRear) {
				// System.out.println("Motion from "+beingUpdated+" rejected by "+leftRear);
				return;
			}

			double x = leftFront.manipulator.getCurrentPose().getX();
			double y = leftRear.manipulator.getCurrentPose().getY();
			double z = leftRear.manipulator.getCurrentPose().getZ();
			leftFront.manipulator.setInReferenceFrame(x, y, z);
			x = leftRear.manipulator.getCurrentPose().getX();
			y = rightRear.manipulator.getCurrentPose().getY();
			rightRear.manipulator.setInReferenceFrame(x, y, z);
			BowlerStudio.runLater(() -> update()); // System.out.println("leftRear");
		});
		topCenter.manipulator.addEventListener(() -> {
			if (beingUpdated == null)
				beingUpdated = topCenter;
			if (beingUpdated != topCenter) {
				// System.out.println("Motion from "+beingUpdated+" rejected by "+topCenter);
				return;
			}
			BowlerStudio.runLater(() -> update());

			// System.out.println("topCenter");
		});
		controls = Arrays.asList(topCenter, rightFront, rightRear, leftFront, leftRear);
		for (ResizingHandle c : controls) {
			c.manipulator.setFrameOfReference(() -> cadoodle.getWorkplane());
			c.manipulator.addSaveListener(() -> {
				try {
					Thread.sleep(32);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// System.out.println("Saving from "+c);
				TransformNR wp = cadoodle.getWorkplane().copy();
				TransformNR rrC = rightRear.getCurrentInReferenceFrame();
				TransformNR lfC = leftFront.getCurrentInReferenceFrame();
				TransformNR tcC = topCenter.getCurrentInReferenceFrame();

//				rrC=wp.inverse().times(rrC);
//				lfC=wp.inverse().times(lfC);
//				tcC=wp.inverse().times(tcC);

				bounds = getBounds();
				for (ResizingHandle ctrl : controls) {
					ctrl.manipulator.set(0, 0, 0);
				}
				beingUpdated = null;
				if (Math.abs(lfC.getZ() - rrC.getZ()) > 0.00001) {
					throw new RuntimeException("The control points of the corners must be at the same Z value \n"
							+ lfC.toSimpleString() + "\n" + rrC.toSimpleString());
				}
				Resize setResize = new Resize().setNames(sel.selectedSnapshot())
						// .setDebugger(engine)
						.setWorkplane(wp).setResize(tcC, lfC, rrC);
				Thread t = cadoodle.addOpperation(setResize);
				try {
					t.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					Thread.sleep(32);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				BowlerStudio.runLater(() -> threeDTarget());
			});
		}
	}

	private void update() {
		// if(beingUpdated!=null)
		ResizingHandle beingUpdated2 = beingUpdated;
		if (beingUpdated2 != topCenter || beingUpdated2 == null) {
			TransformNR rrC = rightRear.getCurrentInReferenceFrame();
			TransformNR lfC = leftFront.getCurrentInReferenceFrame();
			TransformNR tcC = topCenter.getCurrentInReferenceFrame();
			double x = (lfC.getX() - rrC.getX()) / 2 + rrC.getX();
			double y = (lfC.getY() - rrC.getY()) / 2 + rrC.getY();
			double z = tcC.getZ();
			topCenter.setInReferenceFrame(x, y, z);
			beingUpdated = beingUpdated2;
		} else {
			// System.out.println("Not updating center cube "+beingUpdated2);
		}
		updateLines.run();
	}

	public double getViewScale() {
		return topCenter.getScale();
	}

	public void threeDTarget(double w, double h, double z, Bounds b, TransformNR cameraFrame) {
		cf = cameraFrame;

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

		topCenter.threeDTarget(screenW, screenH, zoom, new TransformNR(center.x, center.y, max.z), cf);
		leftFront.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, max.y, min.z), cf);
		leftRear.threeDTarget(screenW, screenH, zoom, new TransformNR(min.x, max.y, min.z), cf);
		rightFront.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, min.y, min.z), cf);
		rightRear.threeDTarget(screenW, screenH, zoom, new TransformNR(min.x, min.y, min.z), cf);
		update();
	}

	boolean leftSelected() {
		return leftFront.isSelected() || leftRear.isSelected();
	}

	boolean rightSelected() {
		return rightFront.isSelected() || rightRear.isSelected();
	}

	boolean frontSelected() {
		return rightFront.isSelected() || leftFront.isSelected();
	}

	boolean rearSelected() {
		return rightRear.isSelected() || leftRear.isSelected();
	}

	public Bounds getBounds() {
		TransformNR lr = rightRear.getCurrentInReferenceFrame();
		TransformNR rf = leftFront.getCurrentInReferenceFrame();
		TransformNR tc = topCenter.getCurrentInReferenceFrame();
		Vector3d min = new Vector3d(lr.getX(), lr.getY(), lr.getZ());
		Vector3d max = new Vector3d(rf.getX(), rf.getY(), tc.getZ());
		return new Bounds(min, max);
	}

	public void setSnapGrid(double size) {
		this.size = size;
		for (ResizingHandle ctrl : controls) {
			ctrl.manipulator.setIncrement(size);
		}
	}

	public void resetSelected() {
		for (ResizingHandle c : controls) {
			c.resetSelected();
		}
	}

}
