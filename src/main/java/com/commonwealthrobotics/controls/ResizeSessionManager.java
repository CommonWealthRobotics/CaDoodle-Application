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
			if (beingUpdated == null)
				beingUpdated = rightFront;
			if (beingUpdated != rightFront)
				return;
			if (scalingFlag) { // cancel one recursive call
				scalingFlag = false;
				return;
			}

			// Uniform scaling with shift key
			if (ev != null && ev.isShiftDown()) {
				if (originalBounds == null)
					originalBounds = getBounds();

				double original_x = originalBounds.getTotalX();
				double original_y = originalBounds.getTotalY();
				double original_diagonal = Math.hypot(original_x, original_y);

				// Mouse offset from manipulator
				double mouseX =   rightFront.getCurrentInReferenceFrame().getX() - originalBounds.getMaxX(); // Front
				double mouseY = - rightFront.getCurrentInReferenceFrame().getY() + originalBounds.getMinY(); // Right
				double scale = (mouseX * original_x + mouseY * original_y) / (original_diagonal * original_diagonal);

				double rawNewX = original_x * (1.0 + scale); // Snap X-dimension to grid
				double gridNewX = Math.round(rawNewX / size) * size;
				double difference_x = Math.abs(rawNewX - gridNewX); 

				double rawNewY = original_y * (1.0 + scale); // Snap Y-dimension to grid
				double gridNewY = Math.round(rawNewY / size) * size;
				double difference_y = Math.abs(rawNewY - gridNewY); 
				double gridScale;
				
				if (difference_x < difference_y)
				  gridScale = (gridNewX / original_x) - 1.0;
				else
				  gridScale = (gridNewY / original_y) - 1.0;

				scalingFlag = true;
				rightFront.manipulator.setInReferenceFrame(original_x * gridScale, -original_y * gridScale, 0); 
				rightRear.manipulator.setInReferenceFrame (0,					   -original_y * gridScale, 0);
				leftFront.manipulator.setInReferenceFrame (original_x * gridScale, 0					  , 0);
				topCenter.setInReferenceFrame (0, 0, originalBounds.getMinZ() + originalBounds.getTotalZ() * (gridScale + 1));

			} else {
				// Unconstraint resizing path
				double x = rightRear.manipulator.getCurrentPose().getX();
				double y = rightFront.manipulator.getCurrentPose().getY();
				double z = rightRear.manipulator.getCurrentPose().getZ();
				rightRear.manipulator.setInReferenceFrame(x, y, z);
				x = rightFront.manipulator.getCurrentPose().getX();
				y = leftFront.manipulator.getCurrentPose().getY();
				leftFront.manipulator.setInReferenceFrame(x, y, z);
			}
			
			BowlerStudio.runLater(() -> update());
		});

		rightRear.manipulator.addEventListener(ev -> {
			if (beingUpdated == null)
				beingUpdated = rightRear;
			if (beingUpdated != rightRear)
				return;
			if (scalingFlag) {
				scalingFlag = false;
				return;
			}

			if (ev != null && ev.isShiftDown()) {
				// Uniform scaling
				if (originalBounds == null)
					originalBounds = getBounds();

				double original_x = originalBounds.getTotalX();
				double original_y = originalBounds.getTotalY();
				double original_diagonal = Math.hypot(original_x, original_y);

				// Mouse offset from manipulator
				double mouseX = -rightRear.getCurrentInReferenceFrame().getX() + originalBounds.getMinX(); // Rear
				double mouseY = -rightRear.getCurrentInReferenceFrame().getY() + originalBounds.getMinY(); // Right
				double scale = (mouseX * original_x + mouseY * original_y) / (original_diagonal * original_diagonal);

				double rawNewX = original_x * (1.0 + scale); // Snap X-dimension to grid
				double gridNewX = Math.round(rawNewX / size) * size;
				double difference_x = Math.abs(rawNewX - gridNewX); 

				double rawNewY = original_y * (1.0 + scale); // Snap Y-dimension to grid
				double gridNewY = Math.round(rawNewY / size) * size;
				double difference_y = Math.abs(rawNewY - gridNewY); 
				double gridScale;
				
				if (difference_x < difference_y)
				  gridScale = (gridNewX / original_x) - 1.0;
				else
				  gridScale = (gridNewY / original_y) - 1.0;

				scalingFlag = true; // Set to true to prevent recursive call
				rightRear.manipulator.setInReferenceFrame(-original_x * gridScale, -original_y * gridScale, 0); 
				rightFront.manipulator.setInReferenceFrame(0,					   -original_y * gridScale, 0);
				leftRear.manipulator.setInReferenceFrame  (-original_x * gridScale, 0					  , 0);
				topCenter.setInReferenceFrame (0, 0, originalBounds.getMinZ() + originalBounds.getTotalZ() * (gridScale + 1));
			} else {
				double x = rightFront.manipulator.getCurrentPose().getX();
				double y = rightRear.manipulator.getCurrentPose().getY();
				double z = rightFront.manipulator.getCurrentPose().getZ();
				rightFront.manipulator.setInReferenceFrame(x, y, z);
				x = rightRear.manipulator.getCurrentPose().getX();
				y = leftRear.manipulator.getCurrentPose().getY();
				leftRear.manipulator.setInReferenceFrame(x, y, z);
			}

			BowlerStudio.runLater(() -> update());
		});

		leftFront.manipulator.addEventListener(ev -> {
			if (beingUpdated == null)
				beingUpdated = leftFront;
			if (beingUpdated != leftFront)
				return;
			if (scalingFlag) {
				scalingFlag = false;
				return;
			}

			if (ev != null && ev.isShiftDown()) {
				// Uniform scaling
				if (originalBounds == null)
					originalBounds = getBounds();

				double original_x = originalBounds.getTotalX();
				double original_y = originalBounds.getTotalY();
				double original_diagonal = Math.hypot(original_x, original_y);

				// Mouse offset from manipulator
				double mouseX = leftFront.getCurrentInReferenceFrame().getX() - originalBounds.getMaxX(); // Front
				double mouseY = leftFront.getCurrentInReferenceFrame().getY() - originalBounds.getMaxY(); // Left
				double scale = (mouseX * original_x + mouseY * original_y) / (original_diagonal * original_diagonal);

				double rawNewX = original_x * (1.0 + scale); // Snap X-dimension to grid
				double gridNewX = Math.round(rawNewX / size) * size;
				double difference_x = Math.abs(rawNewX - gridNewX); 

				double rawNewY = original_y * (1.0 + scale); // Snap Y-dimension to grid
				double gridNewY = Math.round(rawNewY / size) * size;
				double difference_y = Math.abs(rawNewY - gridNewY); 
				double gridScale;
				
				if (difference_x < difference_y)
				  gridScale = (gridNewX / original_x) - 1.0;
				else
				  gridScale = (gridNewY / original_y) - 1.0;

				scalingFlag = true; // Set to true to prevent recursive call
				leftFront.manipulator.setInReferenceFrame (original_x * gridScale, original_y * gridScale, 0);
				rightFront.manipulator.setInReferenceFrame(original_x * gridScale, 0,					  0); 
				leftRear.manipulator.setInReferenceFrame (0,					   original_y * gridScale, 0);
				topCenter.setInReferenceFrame(0, 0, originalBounds.getMinZ() + originalBounds.getTotalZ() * (gridScale + 1));
			} else {
				double x = leftRear.manipulator.getCurrentPose().getX();
				double y = leftFront.manipulator.getCurrentPose().getY();
				double z = leftFront.manipulator.getCurrentPose().getZ();
				leftRear.manipulator.setInReferenceFrame(x, y, z);
				x = leftFront.manipulator.getCurrentPose().getX();
				y = rightFront.manipulator.getCurrentPose().getY();
				rightFront.manipulator.setInReferenceFrame(x, y, z);
			}
			
			BowlerStudio.runLater(() -> update());
		});

		leftRear.manipulator.addEventListener(ev -> {
			if (beingUpdated == null)
				beingUpdated = leftRear;
			if (beingUpdated != leftRear)
				return;
			if (scalingFlag) {
				scalingFlag = false;
				return;
			}

			if (ev != null && ev.isShiftDown()) {
				// Uniform scaling
				if (originalBounds == null)
					originalBounds = getBounds();

				double original_x = originalBounds.getTotalX();
				double original_y = originalBounds.getTotalY();
				double original_diagonal = Math.hypot(original_x, original_y);

				// Mouse offset from manipulator
				double mouseX = -leftRear.getCurrentInReferenceFrame().getX() + originalBounds.getMinX(); // Rear
				double mouseY =  leftRear.getCurrentInReferenceFrame().getY() - originalBounds.getMaxY(); // Left
				double scale = (mouseX * original_x + mouseY * original_y) / (original_diagonal * original_diagonal);

				double rawNewX = original_x * (1.0 + scale); // Snap X-dimension to grid
				double gridNewX = Math.round(rawNewX / size) * size;
				double difference_x = Math.abs(rawNewX - gridNewX); 

				double rawNewY = original_y * (1.0 + scale); // Snap Y-dimension to grid
				double gridNewY = Math.round(rawNewY / size) * size;
				double difference_y = Math.abs(rawNewY - gridNewY); 
				double gridScale;
				
				if (difference_x < difference_y)
				  gridScale = (gridNewX / original_x) - 1.0;
				else
				  gridScale = (gridNewY / original_y) - 1.0;

				scalingFlag = true; // Set to true to prevent recursive call
				leftRear.manipulator.setInReferenceFrame (-original_x * gridScale, original_y * gridScale, 0);
				leftFront.manipulator.setInReferenceFrame(0,					   original_y * gridScale, 0);			  
				rightRear.manipulator.setInReferenceFrame(-original_x * gridScale, 0					 , 0);
				topCenter.setInReferenceFrame(0, 0, originalBounds.getMinZ() + originalBounds.getTotalZ() * (gridScale + 1));
			} else {
				double x = leftFront.manipulator.getCurrentPose().getX();
				double y = leftRear.manipulator.getCurrentPose().getY();
				double z = leftRear.manipulator.getCurrentPose().getZ();
				leftFront.manipulator.setInReferenceFrame(x, y, z);
				x = leftRear.manipulator.getCurrentPose().getX();
				y = rightRear.manipulator.getCurrentPose().getY();
				rightRear.manipulator.setInReferenceFrame(x, y, z);
			}

			BowlerStudio.runLater(() -> update());
		});

		topCenter.manipulator.addEventListener(ev -> {
			if (beingUpdated == null)
				beingUpdated = topCenter;
			if (beingUpdated != topCenter)
				return;
			if (scalingFlag) {
				scalingFlag = false;
				return;
			}

			if (ev != null)
				if (ev.isShiftDown()) {
					TransformNR tcC = topCenter.getCurrentInReferenceFrame();
					uniformScalingZ(tcC);
				}
			BowlerStudio.runLater(() -> update());
		});

		controls = Arrays.asList(topCenter, rightFront, rightRear, leftFront, leftRear);
		for (ResizingHandle c : controls) {
			c.manipulator.setFrameOfReference(() -> ap.get().getWorkplane());
			c.manipulator.addSaveListener(() -> {
				if (beingUpdated != c)
					return;
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
				beingUpdated = null;
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
		scalingFlag = true;
		rightRear.manipulator.setInReferenceFrame(newX, newY, z);
        leftFront.manipulator.setInReferenceFrame(newX2, newY2, z);
        rightFront.manipulator.setInReferenceFrame(newX2, newY, z);
        leftRear.manipulator.setInReferenceFrame(newX, newY2, z);
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
		originalBounds = null; // reset var that exists to help maintain aspect ratios
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

			topCenter.manipulator.setInReferenceFrame(0, 0, z - b.getTotalZ());
			if (topCenter.isUniform()) {
				TransformNR tcC = topCenter.getCurrentInReferenceFrame();
				uniformScalingZ(tcC);
			}
			topCenter.manipulator.fireSave();
		}

		if (leftFront.isSelected()) {
			com.neuronrobotics.sdk.common.Log.error("leftFront resize");
			leftFront.manipulator.setInReferenceFrame(x - b.getTotalX(), (y - b.getTotalY()), 0);
			leftFront.manipulator.fireSave();
		}

		if (leftRear.isSelected()) {
			double lr_x = -(x - b.getTotalX());
			double lr_y = (y - b.getTotalY());
			scalingFlag = false;
			leftRear.manipulator.setInReferenceFrame(lr_x, lr_y, 0);
			leftFront.manipulator.setInReferenceFrame(leftFront.manipulator.getCurrentPose().getX(), lr_y, 0);
			rightRear.manipulator.setInReferenceFrame(lr_x, rightRear.manipulator.getCurrentPose().getY(), 0);
			leftRear.manipulator.fireSave();
		}

		if (rightFront.isSelected()) {
			double rf_x = x - b.getTotalX();
			double rf_y = -(y - b.getTotalY());
			scalingFlag = false;
			rightFront.manipulator.setInReferenceFrame(rf_x, rf_y, 0);
			rightRear.manipulator.setInReferenceFrame(rightRear.manipulator.getCurrentPose().getX(), rf_y, 0);
			leftFront.manipulator.setInReferenceFrame(rf_x, leftFront.manipulator.getCurrentPose().getY(), 0);
			rightFront.manipulator.fireSave();
		}

		if (rightRear.isSelected()) {
			rightRear.manipulator.setInReferenceFrame(-(x - b.getTotalX()), -(y - b.getTotalY()), 0);
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
