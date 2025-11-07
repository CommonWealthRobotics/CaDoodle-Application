package com.commonwealthrobotics.controls;

import java.util.Arrays;
import java.util.List;

import com.commonwealthrobotics.ActiveProject;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Resize;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.scene.transform.Affine;

public class ResizeSessionManager {
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
	private ActiveProject ap;
	private boolean scalingFlag = false;
	private boolean locked;
	private boolean resizeAllowed;
	private boolean moveLock;
	private Bounds originalBounds = null;

	public ResizeSessionManager(BowlerStudio3dEngine engine, Affine selection, Runnable updateLines, ActiveProject ap,
			SelectionSession s, Affine workplaneOffset, MoveUpArrow up) {
		this.engine = engine;
		if (engine == null)
			throw new NullPointerException();
		this.updateLines = updateLines;
		// this.workplaneOffset = workplaneOffset;
		this.ap = ap;

		Runnable onReset = () -> {
			resetSelected();
			up.resetSelected();
		};
		topCenter = new ResizingHandle("topCenter", engine, selection, new Vector3d(0, 0, 1), workplaneOffset,
				updateLines, onReset);
		rightFront = new ResizingHandle("rightFront", engine, selection, new Vector3d(1, 1, 0), workplaneOffset,
				updateLines, onReset);
		rightRear = new ResizingHandle("rightRear", engine, selection, new Vector3d(1, 1, 0), workplaneOffset,
				updateLines, onReset);
		leftFront = new ResizingHandle("leftFront", engine, selection, new Vector3d(1, 1, 0), workplaneOffset,
				updateLines, onReset);
		leftRear = new ResizingHandle("leftRear", engine, selection, new Vector3d(1, 1, 0), workplaneOffset,
				updateLines, onReset);

		rightFront.manipulator.addEventListener(ev -> {
			if (scalingFlag)
				return;

			scalingFlag = false;
			if (beingUpdated == null)
				beingUpdated = rightFront;
			if (beingUpdated != rightFront) {
				// com.neuronrobotics.sdk.common.Log.error("Motion from "+beingUpdated+"
				// rejected by "+rightFront);
				return;
			}
			double x = rightRear.manipulator.getCurrentPose().getX();
			double y = rightFront.manipulator.getCurrentPose().getY();
			double z = rightRear.manipulator.getCurrentPose().getZ();
			// com.neuronrobotics.sdk.common.Log.error("rightRear Move x" + x + " y" + y + "
			// z" + z);
			rightRear.manipulator.setInReferenceFrame(x, y, z);
			x = rightFront.manipulator.getCurrentPose().getX();
			y = leftFront.manipulator.getCurrentPose().getY();
			leftFront.manipulator.setInReferenceFrame(x, y, z);
			BowlerStudio.runLater(() -> update());
			// com.neuronrobotics.sdk.common.Log.error("rightFront");
		});
		rightRear.manipulator.addEventListener(ev -> {
			if (scalingFlag)
				return;

			scalingFlag = false;

			if (beingUpdated == null)
				beingUpdated = rightRear;
			if (beingUpdated != rightRear) {
				// com.neuronrobotics.sdk.common.Log.error("Motion from "+beingUpdated+"
				// rejected by "+rightRear);
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
			// com.neuronrobotics.sdk.common.Log.error("rightRear");
		});
		leftFront.manipulator.addEventListener(ev -> {
			if (scalingFlag)
				return;

			scalingFlag = false;

			if (beingUpdated == null)
				beingUpdated = leftFront;
			if (beingUpdated != leftFront) {
				// com.neuronrobotics.sdk.common.Log.error("Motion from "+beingUpdated+"
				// rejected by "+leftFront);
				return;
			}
			double x = leftRear.manipulator.getCurrentPose().getX();
			double y = leftFront.manipulator.getCurrentPose().getY();
			double z = leftFront.manipulator.getCurrentPose().getZ();
			leftRear.manipulator.setInReferenceFrame(x, y, z);
			x = leftFront.manipulator.getCurrentPose().getX();
			y = rightFront.manipulator.getCurrentPose().getY();
			rightFront.manipulator.setInReferenceFrame(x, y, z);
			BowlerStudio.runLater(() -> update()); // com.neuronrobotics.sdk.common.Log.error("leftFront");
		});
		leftRear.manipulator.addEventListener(ev -> {
			if (scalingFlag)
				return;
			scalingFlag = false;

			if (beingUpdated == null)
				beingUpdated = leftRear;
			if (beingUpdated != leftRear) {
				// com.neuronrobotics.sdk.common.Log.error("Motion from "+beingUpdated+"
				// rejected by "+leftRear);
				return;
			}

			double x = leftFront.manipulator.getCurrentPose().getX();
			double y = leftRear.manipulator.getCurrentPose().getY();
			double z = leftRear.manipulator.getCurrentPose().getZ();
			leftFront.manipulator.setInReferenceFrame(x, y, z);
			x = leftRear.manipulator.getCurrentPose().getX();
			y = rightRear.manipulator.getCurrentPose().getY();
			rightRear.manipulator.setInReferenceFrame(x, y, z);
			BowlerStudio.runLater(() -> update()); // com.neuronrobotics.sdk.common.Log.error("leftRear");
		});
		topCenter.manipulator.addEventListener(ev -> {
			scalingFlag = false;

			if (beingUpdated == null)
				beingUpdated = topCenter;
			if (beingUpdated != topCenter) {
				// com.neuronrobotics.sdk.common.Log.error("Motion from "+beingUpdated+"
				// rejected by "+topCenter);
				return;
			}
			if (ev != null)
				if (ev.isShiftDown()) {
					TransformNR tcC = topCenter.getCurrentInReferenceFrame();
					uniformScalingZ(tcC);
					// com.neuronrobotics.sdk.common.Log.debug("RE-Scaling whole object! "+scale);
				}
			BowlerStudio.runLater(() -> update());

			// com.neuronrobotics.sdk.common.Log.error("topCenter");
		});
		controls = Arrays.asList(topCenter, rightFront, rightRear, leftFront, leftRear);
		for (ResizingHandle c : controls) {
			c.manipulator.setFrameOfReference(() -> ap.get().getWorkplane());
			c.manipulator.addSaveListener(() -> {
				try {
					Thread.sleep(32);
				} catch (InterruptedException e) {
					// Auto-generated catch block
					com.neuronrobotics.sdk.common.Log.error(e);
				}
				// com.neuronrobotics.sdk.common.Log.error("Saving from "+c);
				TransformNR wp = ap.get().getWorkplane().copy();
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
//				if (Math.abs(lfC.getZ() - rrC.getZ()) > 0.00001) {
//					throw new RuntimeException("The control points of the corners must be at the same Z value \n"
//							+ lfC.toSimpleString() + "\n" + rrC.toSimpleString());
//				}

				Resize setResize = new Resize().setNames(s.selectedSnapshot())
						// .setDebugger(engine)
						.setWorkplane(wp).setResize(tcC, lfC, rrC);
				if (resizeAllowed) {
					Thread t = ap.addOp(setResize);
					try {
						t.join();
					} catch (InterruptedException e) {
						// Auto-generated catch block
						com.neuronrobotics.sdk.common.Log.error(e);
					}
				}
				BowlerStudio.runLater(() -> threeDTarget());
			});
		}
	}

	public void setResizeAllowed(boolean resizeAllowed, boolean moveLock) {
		this.resizeAllowed = resizeAllowed;
		this.moveLock = moveLock;
		for (ResizingHandle c : controls)
			c.setResizeAllowed(resizeAllowed,moveLock);
	}

	private void uniformScalingZ(TransformNR tcC) {
		scalingFlag = true;
		double startZ = bounds.getTotalZ();
		double startX = bounds.getTotalX();
		double startY = bounds.getTotalY();

		double nowZ = tcC.getZ() - bounds.getMinZ();
		double scale = nowZ / startZ;
		double newXComp = (startX * scale - startX) / 2;
		double newYComp = (startY * scale - startY) / 2;

		double centerX = bounds.getCenterX();
		double centerY = bounds.getCenterY();
		// com.neuronrobotics.sdk.common.Log.debug("Center x:"+centerX+" centerY:"+centerY);
		double z = leftRear.manipulator.getCurrentPose().getZ();
		TransformNR rrC = rightRear.getCurrentInReferenceFrame();
		TransformNR lfC = leftFront.getCurrentInReferenceFrame();
		double x = (lfC.getX() - rrC.getX()) / 2 + rrC.getX();
		double y = (lfC.getY() - rrC.getY()) / 2 + rrC.getY();

		double newX = -newXComp;
		double newY = -newYComp;
		double newX2 = +newXComp;
		double newY2 = +newYComp;
		rightRear.manipulator.setInReferenceFrame(newX, newY, z);
		leftFront.manipulator.setInReferenceFrame(newX2, newY2, z);
		rightFront.manipulator.setInReferenceFrame(newX2, newY, z);
		leftRear.manipulator.setInReferenceFrame(newX, newY2, z);
	}
	
	private void uniformScalingXY(ResizingHandle draggedHandle, ResizingHandle anchorHandle, TransformNR draggedPos) {
	    scalingFlag = true;
	    
	    // Get current bounds from handle positions
	    bounds = getBounds();
	    
	    // Store original bounds on first call
	    if (originalBounds == null) {
	        originalBounds = new Bounds(bounds.getMin(), bounds.getMax());
	    }
	    
	    double originalX = originalBounds.getTotalX();
	    double originalY = originalBounds.getTotalY();
	    double originalZ = originalBounds.getTotalZ();
	    
	    TransformNR anchorPos = anchorHandle.getCurrentInReferenceFrame();
	    
	    // Calculate new dimensions from anchor to dragged position
	    double newX = Math.abs(draggedPos.getX() - anchorPos.getX());
	    double newY = Math.abs(draggedPos.getY() - anchorPos.getY());
	    
	    // Calculate scale factors
	    double scaleX = newX / originalX;
	    double scaleY = newY / originalY;
	    
	    // Use maximum scale to maintain one dimension precisely
	    double scale = Math.max(scaleX, scaleY);
	    
	    // Calculate new Z based on uniform scale
	    double newZ = originalZ * scale;
	    double zOffset = newZ - bounds.getTotalZ();
	    
	    // Apply Z scaling
	    topCenter.manipulator.set(0, 0, zOffset);
	    
	    // Reposition the other two corners to maintain rectangular shape
	    // The dragged corner and anchor corner are already positioned correctly
	    // Just need to update the other two corners
	    updateNonDraggedCorners(draggedHandle, anchorHandle);
	}
	
	private ResizingHandle getOppositeCorner(ResizingHandle handle) {
	    if (handle == leftRear) return rightFront;
	    if (handle == rightFront) return leftRear;
	    if (handle == leftFront) return rightRear;
	    if (handle == rightRear) return leftFront;
	    return null; // Should never happen
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
			// com.neuronrobotics.sdk.common.Log.error("Not updating center cube
			// "+beingUpdated2);
		}
		updateLines.run();
	}

	public double getViewScale() {
		return topCenter.getScale();
	}

	public void threeDTarget(double w, double h, double z, Bounds b, TransformNR cameraFrame, boolean locked) {
		cf = cameraFrame;

		this.screenW = w;
		this.screenH = h;
		this.zoom = z;
		this.bounds = b;
		this.locked = locked;
		threeDTarget();
	}

	private void threeDTarget() {

		Vector3d center = bounds.getCenter();

		Vector3d min = bounds.getMin();
		Vector3d max = bounds.getMax();

		topCenter.threeDTarget(screenW, screenH, zoom, new TransformNR(center.x, center.y, max.z), cf, locked);
		leftFront.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, max.y, min.z), cf, locked);
		leftRear.threeDTarget(screenW, screenH, zoom, new TransformNR(min.x, max.y, min.z), cf, locked);
		rightFront.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, min.y, min.z), cf, locked);
		rightRear.threeDTarget(screenW, screenH, zoom, new TransformNR(min.x, min.y, min.z), cf, locked);
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

	public boolean zScaleSelected() {
		return topCenter.isSelected();
	}

	public boolean xySelected() {
		return leftSelected() || rightSelected();
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

	public void set(double x, double y, double z) {
		Bounds b = getBounds();
		Vector3d c = b.getCenter();
		com.neuronrobotics.sdk.common.Log.error("Resizing to " + x + " " + y + " " + z);
		if (topCenter.isSelected()) {
			com.neuronrobotics.sdk.common.Log.error("Z resize");
			topCenter.manipulator.set(0, 0, z - b.getTotalZ());
			if (topCenter.isUniform()) {
				TransformNR tcC = topCenter.getCurrentInReferenceFrame();
				uniformScalingZ(tcC);
			}
			topCenter.manipulator.fireSave();
		}
		if (leftFront.isSelected()) {
			com.neuronrobotics.sdk.common.Log.error("leftFront resize");
			leftFront.manipulator.set(x - b.getTotalX(), (y - b.getTotalY()), 0);
			leftFront.manipulator.fireSave();
		}
		if (leftRear.isSelected()) {
			com.neuronrobotics.sdk.common.Log.error("leftRear resize");
			leftRear.manipulator.set(-(x - b.getTotalX()), (y - b.getTotalY()), 0);
			leftRear.manipulator.fireSave();
		}
		if (rightFront.isSelected()) {
			com.neuronrobotics.sdk.common.Log.error("rightFront resize");
			rightFront.manipulator.set(x - b.getTotalX(), -(y - b.getTotalY()), 0);
			rightFront.manipulator.fireSave();
		}
		if (rightRear.isSelected()) {
			com.neuronrobotics.sdk.common.Log.error("rightRear resize");
			rightRear.manipulator.set(-(x - b.getTotalX()), -(y - b.getTotalY()), 0);
			rightRear.manipulator.fireSave();
		}
	}

	public void hide() {
		topCenter.hide();
		leftFront.hide();
		leftRear.hide();
		rightFront.hide();
		rightRear.hide();
	}

}
