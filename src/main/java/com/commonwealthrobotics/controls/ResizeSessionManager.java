package com.commonwealthrobotics.controls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.commonwealthrobotics.ActiveProject;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Resize;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.Log;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Plane;
import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.Vector3d;
import eu.mihosoft.vrl.v3d.Transform;

import javafx.scene.transform.Affine;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;

public class ResizeSessionManager {
	private ResizingHandle topCenter = null;
	private ResizingHandle rightFront = null;
	private ResizingHandle rightRear = null;
	private ResizingHandle leftFront = null;
	private ResizingHandle leftRear = null;

	// Edge-midpoint handles — each rescales one axis by default; Shift+drag gives
	// uniform resize
	//
	// leftRear --[leftMid]-- leftFront
	// | |
	// [rearMid] [frontMid]
	// | |
	// rightRear --[rightMid]-- rightFront
	//
	// frontMid / rearMid → X-only rescale (axis vector 1,0,0)
	// leftMid / rightMid → Y-only rescale (axis vector 0,1,0)
	private ResizingHandle frontMid = null;
	private ResizingHandle rearMid = null;
	private ResizingHandle leftMid = null;
	private ResizingHandle rightMid = null;
	private ArrayList<MeshView> meshControls = new ArrayList<MeshView>();

	private List<ResizingHandle> controls;
	private ResizingHandle beingUpdated = null;
	private Runnable updateLines;
	private Bounds bounds;
	private double screenW; // Screen width
	private double screenH; // Screen height
	private double zoom;
	private double snapGrid = 1;
	private BowlerStudio3dEngine engine;
	private TransformNR cf;
	private boolean scalingFlag = false;
	private boolean locked;
	private boolean resizeAllowed;
	private volatile Bounds originalBounds = null;
	private final Map<CSG, MeshView> meshes;
	private final SelectionSession session;
	private volatile double objectBottomZ = 0;

	public void rescaleMeshes(Affine workplaneOffset, Transform xyzScale) {

		TransformNR workplaneNR = TransformFactory.affineToNr(workplaneOffset);
		Transform workplaneInverse = TransformFactory.nrToCSG(workplaneNR.inverse());
		Transform workplaneTransform = TransformFactory.affineToCSG(workplaneOffset);

		for (CSG c : session.getCurrentStateSelected()) {
			// Transform the CSG to workplane coordinates
			CSG workplaneCSG = c.transformed(workplaneInverse);
			// Scale in workplane coordinates
			CSG scaledCSG = workplaneCSG.transformed(xyzScale);
			// Transform back to global coordinates
			CSG globalCSG = scaledCSG.transformed(workplaneTransform);
			regenerateMesh(c, globalCSG);
		}
	}

	private void regenerateMesh(CSG key, CSG scaledCSG) {
		MeshView old = meshes.get(key); // lookup with original key
		if (old == null)
			return;

		MeshView fresh = scaledCSG.newMesh();
		fresh.setMaterial(old.getMaterial());
		fresh.getTransforms().setAll(old.getTransforms());
		fresh.setMouseTransparent(old.isMouseTransparent());

		engine.removeUserNode(old);
		engine.addUserNode(fresh);
		meshes.put(key, fresh);
	}

	// Prevent 0-value for scaling
	public double notZero(double in) {
		if (Math.abs(in) < 0.001)
			return 0.001;

		return in;
	}

	public ResizeSessionManager(BowlerStudio3dEngine engine, Affine selection, Runnable updateLines, ActiveProject ap,
			SelectionSession session, Affine workplaneOffset, MoveUpArrow upArrow, ControlSprites controlSprites) {

		this.session = session;
		this.engine = engine;
		this.meshes = session.getMeshes();

		if (engine == null)
			throw new NullPointerException();

		this.updateLines = updateLines;

		Runnable onReset = () -> {
			resetSelected();
			upArrow.resetSelected();
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

		// Edge-midpoint handles: single-axis constraint vectors
		frontMid = new ResizingHandle("frontMid", engine, selection, new Vector3d(1, 0, 0), workplaneOffset,
				updateLines, onReset);
		rearMid = new ResizingHandle("rearMid", engine, selection, new Vector3d(1, 0, 0), workplaneOffset, updateLines,
				onReset);
		leftMid = new ResizingHandle("leftMid", engine, selection, new Vector3d(0, 1, 0), workplaneOffset, updateLines,
				onReset);
		rightMid = new ResizingHandle("rightMid", engine, selection, new Vector3d(0, 1, 0), workplaneOffset,
				updateLines, onReset);

		objectBottomZ = 0; // Keep track of the object bottom position

		// -----------------------------------------------------------------------
		// CORNER HANDLES — unchanged from original
		// -----------------------------------------------------------------------

		rightFront.getMesh().setOnMousePressed(ev -> {
			originalBounds = getBounds();
			beingUpdated = rightFront;
		});

		rightFront.manipulator.addEventListener(ev -> {

			if (scalingFlag) {
				scalingFlag = false;
				return;
			}

			if ((beingUpdated != rightFront) || (originalBounds == null))
				return;

			controlSprites.hideRotationHandles();
			double sx = 0, sy = 0, sz = 0;

			// Uniform scaling with shift key
			if ((ev != null) && ev.isShiftDown()) {

				rightFront.manipulator.setSnapGridStatus(false); // disable snap grid for uniform scaling
				double original_tx = originalBounds.getTotalX();
				double original_ty = originalBounds.getTotalY();
				double original_diagonal = Math.hypot(original_tx, original_ty);

				double mouseX = rightFront.getCurrentInReferenceFrame().getX() - originalBounds.getMaxX(); // Front
				double mouseY = -rightFront.getCurrentInReferenceFrame().getY() + originalBounds.getMinY(); // Right
				double scale = (mouseX * original_tx + mouseY * original_ty) / (original_diagonal * original_diagonal);

				double rawNewX = original_tx * (1.0 + scale); // Snap X-dimension to grid
				double gridNewX = Math.round(rawNewX / snapGrid) * snapGrid;
				double rawNewY = original_ty * (1.0 + scale); // Snap Y-dimension to grid
				double gridNewY = Math.round(rawNewY / snapGrid) * snapGrid;

				// Let the XY-size of the scaled object follow the snap grid
				// It is usually not possible to let the corner land on the snap grid
				double gs = Math.abs(rawNewX - gridNewX) < Math.abs(rawNewY - gridNewY)
						? (gridNewX / original_tx) - 1.0
						: (gridNewY / original_ty) - 1.0;

				scalingFlag = true; // block recursive call
				rightFront.manipulator.setInReferenceFrame(original_tx * gs, -original_ty * gs, 0);
				rightRear.manipulator.setInReferenceFrame(0, -original_ty * gs, 0);
				leftFront.manipulator.setInReferenceFrame(original_tx * gs, 0, 0);

				gs = gs + 1;
				topCenter.setInReferenceFrame(0, 0, originalBounds.getMinZ() + originalBounds.getTotalZ() * gs);
				sx = gs;
				sy = gs;
				sz = gs;

			} else { // Unconstraint resizing path RIGHT FRONT

				double x = rightRear.manipulator.getCurrentPose().getX();
				double y = rightFront.manipulator.getCurrentPose().getY(); // rightFront leads rightRear Y
				double z = rightFront.manipulator.getCurrentPose().getZ();
				rightRear.manipulator.setInReferenceFrame(x, y, z);

				x = rightFront.manipulator.getCurrentPose().getX(); // rightFront leads leftFront X
				y = leftFront.manipulator.getCurrentPose().getY();
				leftFront.manipulator.setInReferenceFrame(x, y, z);

				sx = (rightFront.getCurrentInReferenceFrame().getX() - rightRear.getCurrentInReferenceFrame().getX())
						/ originalBounds.getTotalX();
				sy = (leftRear.getCurrentInReferenceFrame().getY() - rightFront.getCurrentInReferenceFrame().getY())
						/ originalBounds.getTotalY();
				sz = 1.0; // Height is unchanged
			}
			updateHandleCenters(rightFront);

			Transform scaleXYZ = null;
			try {
				scaleXYZ = new Transform()
						.translate(originalBounds.getMinX(), originalBounds.getMaxY(), originalBounds.getMinZ())
						.scale(notZero(sx), notZero(sy), notZero(sz))
						.translate(-originalBounds.getMinX(), -originalBounds.getMaxY(), -originalBounds.getMinZ());

			} catch (Exception ex) {
				Log.error(ex);
			}
			BowlerStudio.runLater(() -> updateTopCenter());
			if (scaleXYZ != null)
				rescaleMeshes(workplaneOffset, scaleXYZ);

		});

		rightRear.getMesh().setOnMousePressed(ev -> {
			originalBounds = getBounds();
			beingUpdated = rightRear;
		});

		rightRear.manipulator.addEventListener(ev -> {

			if (scalingFlag) {
				scalingFlag = false;
				return;
			}
			if ((beingUpdated != rightRear) || (originalBounds == null))
				return;

			controlSprites.hideRotationHandles();
			double sx = 0, sy = 0, sz = 0;

			if ((ev != null) && ev.isShiftDown()) {

				rightRear.manipulator.setSnapGridStatus(false); // disable snap grid for uniform scaling
				double original_x = originalBounds.getTotalX();
				double original_y = originalBounds.getTotalY();
				double original_diagonal = Math.hypot(original_x, original_y);

				double mouseX = -rightRear.getCurrentInReferenceFrame().getX() + originalBounds.getMinX();
				double mouseY = -rightRear.getCurrentInReferenceFrame().getY() + originalBounds.getMinY();
				double scale = (mouseX * original_x + mouseY * original_y) / (original_diagonal * original_diagonal);

				double rawNewX = original_x * (1.0 + scale);
				double gridNewX = Math.round(rawNewX / snapGrid) * snapGrid;
				double rawNewY = original_y * (1.0 + scale);
				double gridNewY = Math.round(rawNewY / snapGrid) * snapGrid;
				double gs = Math.abs(rawNewX - gridNewX) < Math.abs(rawNewY - gridNewY)
						? (gridNewX / original_x) - 1.0
						: (gridNewY / original_y) - 1.0;

				scalingFlag = true; // block recursive call
				rightRear.manipulator.setInReferenceFrame(-original_x * gs, -original_y * gs, 0);
				rightFront.manipulator.setInReferenceFrame(0, -original_y * gs, 0);
				leftRear.manipulator.setInReferenceFrame(-original_x * gs, 0, 0);
				gs = gs + 1;
				topCenter.setInReferenceFrame(0, 0, originalBounds.getMinZ() + originalBounds.getTotalZ() * gs);

				sx = gs;
				sy = gs;
				sz = gs;

			} else { // Unconstraint resizing path RIGHT REAR

				double x = rightFront.manipulator.getCurrentPose().getX();
				double y = rightRear.manipulator.getCurrentPose().getY(); // rightRear leads rightFront Y
				double z = rightRear.manipulator.getCurrentPose().getZ();
				rightFront.manipulator.setInReferenceFrame(x, y, z);
				x = rightRear.manipulator.getCurrentPose().getX(); // rightRear leads leftRear X
				y = leftRear.manipulator.getCurrentPose().getY();
				leftRear.manipulator.setInReferenceFrame(x, y, z);

				sx = (rightFront.getCurrentInReferenceFrame().getX() - rightRear.getCurrentInReferenceFrame().getX())
						/ originalBounds.getTotalX();
				sy = (leftRear.getCurrentInReferenceFrame().getY() - rightRear.getCurrentInReferenceFrame().getY())
						/ originalBounds.getTotalY();
				sz = 1.0;
			}

			// Update mid handles to their new midpoint positions
			TransformNR rr = rightRear.getCurrentInReferenceFrame();
			TransformNR rf = rightFront.getCurrentInReferenceFrame();
			TransformNR lr = leftRear.getCurrentInReferenceFrame();
			double cornerZ = rr.getZ();
			rightMid.manipulator.setInReferenceFrame((rr.getX() + rf.getX()) / 2.0, (rr.getY() + rf.getY()) / 2.0,
					cornerZ);
			rearMid.manipulator.setInReferenceFrame((rr.getX() + lr.getX()) / 2.0, (rr.getY() + lr.getY()) / 2.0,
					cornerZ);
			updateHandleCenters(rightRear);
			Transform scaleXYZ = null;
			try {
				scaleXYZ = new Transform()
						.translate(originalBounds.getMaxX(), originalBounds.getMaxY(), originalBounds.getMinZ())
						.scale(notZero(sx), notZero(sy), notZero(sz))
						.translate(-originalBounds.getMaxX(), -originalBounds.getMaxY(), -originalBounds.getMinZ());

			} catch (Exception ex) {
				Log.error(ex);
			}

			BowlerStudio.runLater(() -> updateTopCenter());
			if (scaleXYZ != null)
				rescaleMeshes(workplaneOffset, scaleXYZ);

		});

		leftFront.getMesh().setOnMousePressed(ev -> {
			originalBounds = getBounds();
			beingUpdated = leftFront;
		});

		leftFront.manipulator.addEventListener(ev -> {

			if (scalingFlag) {
				scalingFlag = false;
				return;
			}
			if ((beingUpdated != leftFront) || (originalBounds == null))
				return;

			controlSprites.hideRotationHandles();
			double sx = 0, sy = 0, sz = 0;

			if ((ev != null) && ev.isShiftDown()) {

				leftFront.manipulator.setSnapGridStatus(false); // disable snap grid for uniform scaling
				double original_x = originalBounds.getTotalX();
				double original_y = originalBounds.getTotalY();
				double original_diagonal = Math.hypot(original_x, original_y);

				double mouseX = leftFront.getCurrentInReferenceFrame().getX() - originalBounds.getMaxX();
				double mouseY = leftFront.getCurrentInReferenceFrame().getY() - originalBounds.getMaxY();
				double scale = (mouseX * original_x + mouseY * original_y) / (original_diagonal * original_diagonal);

				double rawNewX = original_x * (1.0 + scale);
				double gridNewX = Math.round(rawNewX / snapGrid) * snapGrid;
				double rawNewY = original_y * (1.0 + scale);
				double gridNewY = Math.round(rawNewY / snapGrid) * snapGrid;
				double gs = Math.abs(rawNewX - gridNewX) < Math.abs(rawNewY - gridNewY)
						? (gridNewX / original_x) - 1.0
						: (gridNewY / original_y) - 1.0;

				scalingFlag = true; // block recursive call
				leftFront.manipulator.setInReferenceFrame(original_x * gs, original_y * gs, 0);
				rightFront.manipulator.setInReferenceFrame(original_x * gs, 0, 0);
				leftRear.manipulator.setInReferenceFrame(0, original_y * gs, 0);
				gs = gs + 1;
				topCenter.setInReferenceFrame(0, 0, originalBounds.getMinZ() + originalBounds.getTotalZ() * gs);

				sx = gs;
				sy = gs;
				sz = gs;

			} else { // Unconstraint resizing path LEFT FRONT

				double x = leftRear.manipulator.getCurrentPose().getX();
				double y = leftFront.manipulator.getCurrentPose().getY();
				double z = leftFront.manipulator.getCurrentPose().getZ();

				leftRear.manipulator.setInReferenceFrame(x, y, z);
				x = leftFront.manipulator.getCurrentPose().getX();
				y = rightFront.manipulator.getCurrentPose().getY();
				rightFront.manipulator.setInReferenceFrame(x, y, z);

				sx = (leftFront.getCurrentInReferenceFrame().getX() - leftRear.getCurrentInReferenceFrame().getX())
						/ originalBounds.getTotalX();
				sy = (leftFront.getCurrentInReferenceFrame().getY() - rightFront.getCurrentInReferenceFrame().getY())
						/ originalBounds.getTotalY();
				sz = 1.0;
			}

			// Update mid handles to their new midpoint positions
			TransformNR lf = leftFront.getCurrentInReferenceFrame();
			TransformNR rf = rightFront.getCurrentInReferenceFrame();
			TransformNR lr = leftRear.getCurrentInReferenceFrame();
			double cornerZ = lf.getZ();
			frontMid.manipulator.setInReferenceFrame((lf.getX() + rf.getX()) / 2.0, (lf.getY() + rf.getY()) / 2.0,
					cornerZ);
			leftMid.manipulator.setInReferenceFrame((lf.getX() + lr.getX()) / 2.0, (lf.getY() + lr.getY()) / 2.0,
					cornerZ);
			updateHandleCenters(leftFront);
			Transform scaleXYZ = null;
			try {
				scaleXYZ = new Transform()
						.translate(originalBounds.getMinX(), originalBounds.getMinY(), originalBounds.getMinZ())
						.scale(notZero(sx), notZero(sy), notZero(sz))
						.translate(-originalBounds.getMinX(), -originalBounds.getMinY(), -originalBounds.getMinZ());

			} catch (Exception ex) {
				Log.error(ex);
			}

			BowlerStudio.runLater(() -> updateTopCenter());
			if (scaleXYZ != null)
				rescaleMeshes(workplaneOffset, scaleXYZ);

		});

		leftRear.getMesh().setOnMousePressed(ev -> {
			originalBounds = getBounds();
			beingUpdated = leftRear;
		});

		leftRear.manipulator.addEventListener(ev -> {

			if (scalingFlag) {
				scalingFlag = false;
				return;
			}
			if ((beingUpdated != leftRear) || (originalBounds == null))
				return;

			controlSprites.hideRotationHandles();
			double sx = 0, sy = 0, sz = 0;

			if ((ev != null) && ev.isShiftDown()) {

				leftRear.manipulator.setSnapGridStatus(false); // disable snap grid for uniform scaling
				double original_x = originalBounds.getTotalX();
				double original_y = originalBounds.getTotalY();
				double original_diagonal = Math.hypot(original_x, original_y);

				double mouseX = -leftRear.getCurrentInReferenceFrame().getX() + originalBounds.getMinX();
				double mouseY = leftRear.getCurrentInReferenceFrame().getY() - originalBounds.getMaxY();
				double scale = (mouseX * original_x + mouseY * original_y) / (original_diagonal * original_diagonal);

				double rawNewX = original_x * (1.0 + scale);
				double gridNewX = Math.round(rawNewX / snapGrid) * snapGrid;
				double rawNewY = original_y * (1.0 + scale);
				double gridNewY = Math.round(rawNewY / snapGrid) * snapGrid;
				double gs = Math.abs(rawNewX - gridNewX) < Math.abs(rawNewY - gridNewY)
						? (gridNewX / original_x) - 1.0
						: (gridNewY / original_y) - 1.0;

				scalingFlag = true; // block recursive call
				leftRear.manipulator.setInReferenceFrame(-original_x * gs, original_y * gs, 0);
				leftFront.manipulator.setInReferenceFrame(0, original_y * gs, 0);
				rightRear.manipulator.setInReferenceFrame(-original_x * gs, 0, 0);
				gs = gs + 1;
				topCenter.setInReferenceFrame(0, 0, originalBounds.getMinZ() + originalBounds.getTotalZ() * gs);

				sx = gs;
				sy = gs;
				sz = gs;

			} else { // Unconstraint resizing path LEFT REAR
				double x = leftFront.manipulator.getCurrentPose().getX();
				double y = leftRear.manipulator.getCurrentPose().getY();
				double z = leftRear.manipulator.getCurrentPose().getZ();
				leftFront.manipulator.setInReferenceFrame(x, y, z);
				x = leftRear.manipulator.getCurrentPose().getX();
				y = rightRear.manipulator.getCurrentPose().getY();
				rightRear.manipulator.setInReferenceFrame(x, y, z);

				sx = (leftFront.getCurrentInReferenceFrame().getX() - leftRear.getCurrentInReferenceFrame().getX())
						/ originalBounds.getTotalX();

				sy = (leftRear.getCurrentInReferenceFrame().getY() - rightRear.getCurrentInReferenceFrame().getY())
						/ originalBounds.getTotalY();

				sz = 1.0;
			}

			// Update mid handles to their new midpoint positions
			TransformNR lr = leftRear.getCurrentInReferenceFrame();
			TransformNR rr = rightRear.getCurrentInReferenceFrame();
			TransformNR lf = leftFront.getCurrentInReferenceFrame();
			double cornerZ = lr.getZ();
			rearMid.manipulator.setInReferenceFrame((lr.getX() + rr.getX()) / 2.0, (lr.getY() + rr.getY()) / 2.0,
					cornerZ);
			leftMid.manipulator.setInReferenceFrame((lr.getX() + lf.getX()) / 2.0, (lr.getY() + lf.getY()) / 2.0,
					cornerZ);
			updateHandleCenters(leftRear);
			Transform scaleXYZ = new Transform()
					.translate(originalBounds.getMaxX(), originalBounds.getMinY(), originalBounds.getMinZ())
					.scale(notZero(sx), notZero(sy), notZero(sz))
					.translate(-originalBounds.getMaxX(), -originalBounds.getMinY(), -originalBounds.getMinZ());

			BowlerStudio.runLater(() -> updateTopCenter());
			if (scaleXYZ != null)
				rescaleMeshes(workplaneOffset, scaleXYZ);

		});

		topCenter.getMesh().setOnMousePressed(ev -> {
			originalBounds = getBounds();
			beingUpdated = topCenter;
		});

		topCenter.manipulator.addEventListener(ev -> {

			if (scalingFlag) {
				scalingFlag = false;
				return;
			}
			if ((beingUpdated != topCenter) || (originalBounds == null))
				return;

			controlSprites.hideRotationHandles();

			if ((ev != null) && (ev.isShiftDown())) {

				// Uniform scaling with shift key
				TransformNR tcC = topCenter.getCurrentInReferenceFrame();
				uniformScalingZ(tcC);

				// Live preview for uniform scaling
				double startZ = originalBounds.getTotalZ();
				double nowZ = tcC.getZ() - originalBounds.getMinZ();

				double scale = notZero(nowZ / startZ);

				// Create scaling in workplane coordinates
				Transform scaleXYZ = new Transform()
						.translate(originalBounds.getCenterX(), originalBounds.getCenterY(), originalBounds.getMinZ())
						.scale(scale, scale, scale).translate(-originalBounds.getCenterX(),
								-originalBounds.getCenterY(), -originalBounds.getMinZ());

				BowlerStudio.runLater(() -> updateTopCenter());
				rescaleMeshes(workplaneOffset, scaleXYZ);

			} else { // Unconstraint resizing path

				// Create scaling transform
				Transform scaleXYZ = null;
				try {
					scaleXYZ = new Transform()
							.translate(originalBounds.getMinX(), originalBounds.getMinY(), originalBounds.getMinZ())
							.scale(1.0, 1.0,
									notZero((topCenter.getCurrentInReferenceFrame().getZ() - originalBounds.getMinZ())
											/ originalBounds.getTotalZ()))
							.translate(-originalBounds.getMinX(), -originalBounds.getMinY(), -originalBounds.getMinZ());

				} catch (Exception ex) {
					Log.error(ex);
				}
				BowlerStudio.runLater(() -> updateTopCenter());
				if (scaleXYZ != null)
					rescaleMeshes(workplaneOffset, scaleXYZ);
			}

		});

		// -----------------------------------------------------------------------
		// EDGE-MIDPOINT HANDLES — single-axis rescale (Shift+drag: uniform resize)
		// -----------------------------------------------------------------------

		// --- frontMid: sits at (maxX, midY, cornerZ) — rescales X only ---
		// Dragging frontMid outward/inward grows/shrinks the X dimension while
		// keeping the rear edge (minX) fixed. rightFront and leftFront corners
		// are kept in sync so their X coordinate mirrors the handle.

		frontMid.getMesh().setOnMousePressed(ev -> {
			originalBounds = getBounds();
			beingUpdated = frontMid;
		});

		frontMid.manipulator.addEventListener(ev -> {

			if (scalingFlag) {
				scalingFlag = false;
				return;
			}
			if ((beingUpdated != frontMid) || (originalBounds == null))
				return;

			controlSprites.hideRotationHandles();

			// Uniform scaling with shift key
			if ((ev != null) && ev.isShiftDown()) {

				// frontMid.manipulator.setSnapGridStatus(false);
				double original_tx = originalBounds.getTotalX();
				double original_ty = originalBounds.getTotalY();

				double mouseX = frontMid.getCurrentInReferenceFrame().getX() - originalBounds.getMaxX();
				double scale = mouseX / original_tx;

				double rawNewX = original_tx * (1.0 + scale);
				double gridNewX = Math.round(rawNewX / snapGrid) * snapGrid;
				double rawNewY = original_ty * (1.0 + scale);
				double gridNewY = Math.round(rawNewY / snapGrid) * snapGrid;

				double gs = Math.abs(rawNewX - gridNewX) < Math.abs(rawNewY - gridNewY)
						? (gridNewX / original_tx) - 1.0
						: (gridNewY / original_ty) - 1.0;

				scalingFlag = true;

				// Anchor: rear midpoint (minX, midY). Each corner's delta =
				// its vector from the anchor * gs.
				// rightFront (maxX, minY): rel=(tx, -ty/2) → delta=(tx*gs, -ty/2*gs)
				// rightRear (minX, minY): rel=(0, -ty/2) → delta=(0, -ty/2*gs)
				// leftFront (maxX, maxY): rel=(tx, ty/2) → delta=(tx*gs, ty/2*gs)
				// leftRear (minX, maxY): rel=(0, ty/2) → delta=(0, ty/2*gs)
				rightFront.manipulator.setInReferenceFrame(original_tx * gs, -original_ty / 2.0 * gs, 0);
				rightRear.manipulator.setInReferenceFrame(0, -original_ty / 2.0 * gs, 0);
				leftFront.manipulator.setInReferenceFrame(original_tx * gs, original_ty / 2.0 * gs, 0);
				leftRear.manipulator.setInReferenceFrame(0, original_ty / 2.0 * gs, 0);

				// frontMid (maxX, midY): rel=(tx, 0) → delta=(tx*gs, 0)
				// rearMid (minX, midY): anchor → stays (0, 0, 0)
				// leftMid (midX, maxY): rel=(tx/2, ty/2) → delta=(tx/2*gs, ty/2*gs)
				// rightMid (midX, minY): rel=(tx/2, -ty/2) → delta=(tx/2*gs, -ty/2*gs)
				// frontMid.manipulator.setInReferenceFrame(original_tx * gs, 0, 0);
				// rearMid.manipulator.setInReferenceFrame(0, 0, 0);
				// leftMid.manipulator.setInReferenceFrame(original_tx / 2.0 * gs, original_ty /
				// 2.0 * gs, 0);
				// rightMid.manipulator.setInReferenceFrame(original_tx / 2.0 * gs, -original_ty
				// / 2.0 * gs, 0);

				updateHandleCenters(frontMid);
				gs = gs + 1;
				topCenter.setInReferenceFrame(0, 0, originalBounds.getMinZ() + originalBounds.getTotalZ() * gs);

				Transform scaleXYZ = null;
				try {
					// Pivot = anchor = (minX, midY, minZ)
					double midY = (originalBounds.getMinY() + originalBounds.getMaxY()) / 2.0;
					scaleXYZ = new Transform().translate(originalBounds.getMinX(), midY, originalBounds.getMinZ())
							.scale(notZero(gs), notZero(gs), notZero(gs))
							.translate(-originalBounds.getMinX(), -midY, -originalBounds.getMinZ());
				} catch (Exception ex) {
					Log.error(ex);
				}
				BowlerStudio.runLater(() -> updateTopCenter());
				if (scaleXYZ != null)
					rescaleMeshes(workplaneOffset, scaleXYZ);
				return;
			}

			// frontMid moves in X only; derive sx from its current X position
			// relative to the fixed rear edge (originalBounds.getMinX()).
			double newMaxX = frontMid.getCurrentInReferenceFrame().getX();
			double sx = (newMaxX - originalBounds.getMinX()) / originalBounds.getTotalX();

			// Keep the two front corners aligned to the handle's X position
			double z = rightFront.manipulator.getCurrentPose().getZ();
			rightFront.manipulator.setInReferenceFrame(frontMid.manipulator.getCurrentPose().getX(),
					rightFront.manipulator.getCurrentPose().getY(), z);
			leftFront.manipulator.setInReferenceFrame(frontMid.manipulator.getCurrentPose().getX(),
					leftFront.manipulator.getCurrentPose().getY(), z);

			Transform scaleXYZ = null;
			try {
				// Anchor: the rear edge stays fixed at originalBounds.getMinX()
				scaleXYZ = new Transform()
						.translate(originalBounds.getMinX(), originalBounds.getMinY(), originalBounds.getMinZ())
						.scale(notZero(sx), 1.0, 1.0)
						.translate(-originalBounds.getMinX(), -originalBounds.getMinY(), -originalBounds.getMinZ());
			} catch (Exception ex) {
				Log.error(ex);
			}
			updateHandleCenters(frontMid);
			BowlerStudio.runLater(() -> updateTopCenter());
			if (scaleXYZ != null)
				rescaleMeshes(workplaneOffset, scaleXYZ);
		});

		// --- rearMid: sits at (minX, midY, cornerZ) — rescales X only ---
		// Dragging rearMid grows/shrinks the X dimension while keeping the
		// front edge (maxX) fixed. rightRear and leftRear corners follow.

		rearMid.getMesh().setOnMousePressed(ev -> {
			originalBounds = getBounds();
			beingUpdated = rearMid;
		});

		rearMid.manipulator.addEventListener(ev -> {

			if (scalingFlag) {
				scalingFlag = false;
				return;
			}
			if ((beingUpdated != rearMid) || (originalBounds == null))
				return;

			controlSprites.hideRotationHandles();

			// Uniform scaling with shift key
			if ((ev != null) && ev.isShiftDown()) {

				// rearMid.manipulator.setSnapGridStatus(false);
				double original_tx = originalBounds.getTotalX();
				double original_ty = originalBounds.getTotalY();

				double mouseX = -rearMid.getCurrentInReferenceFrame().getX() + originalBounds.getMinX();
				double scale = mouseX / original_tx;

				double rawNewX = original_tx * (1.0 + scale);
				double gridNewX = Math.round(rawNewX / snapGrid) * snapGrid;
				double rawNewY = original_ty * (1.0 + scale);
				double gridNewY = Math.round(rawNewY / snapGrid) * snapGrid;

				double gs = Math.abs(rawNewX - gridNewX) < Math.abs(rawNewY - gridNewY)
						? (gridNewX / original_tx) - 1.0
						: (gridNewY / original_ty) - 1.0;

				scalingFlag = true;

				// Anchor: front midpoint (maxX, midY). Each corner's delta =
				// its vector from the anchor * gs.
				// rightFront (maxX, minY): rel=(0, -ty/2) → delta=(0, -ty/2*gs)
				// rightRear (minX, minY): rel=(-tx,-ty/2) → delta=(-tx*gs, -ty/2*gs)
				// leftFront (maxX, maxY): rel=(0, ty/2) → delta=(0, ty/2*gs)
				// leftRear (minX, maxY): rel=(-tx, ty/2)→ delta=(-tx*gs, ty/2*gs)
				rightFront.manipulator.setInReferenceFrame(0, -original_ty / 2.0 * gs, 0);
				rightRear.manipulator.setInReferenceFrame(-original_tx * gs, -original_ty / 2.0 * gs, 0);
				leftFront.manipulator.setInReferenceFrame(0, original_ty / 2.0 * gs, 0);
				leftRear.manipulator.setInReferenceFrame(-original_tx * gs, original_ty / 2.0 * gs, 0);

				// rearMid (minX, midY): rel=(-tx, 0) → delta=(-tx*gs, 0)
				// frontMid (maxX, midY): anchor → stays (0, 0, 0)
				// leftMid (midX, maxY): rel=(-tx/2, ty/2) → delta=(-tx/2*gs, ty/2*gs)
				// rightMid (midX, minY): rel=(-tx/2,-ty/2) → delta=(-tx/2*gs,-ty/2*gs)
				// rearMid.manipulator.setInReferenceFrame(-original_tx * gs, 0, 0);
				// frontMid.manipulator.setInReferenceFrame(0, 0, 0);
				// leftMid.manipulator.setInReferenceFrame(-original_tx / 2.0 * gs, original_ty
				// / 2.0 * gs, 0);
				// rightMid.manipulator.setInReferenceFrame(-original_tx / 2.0 * gs,
				// -original_ty / 2.0 * gs, 0);

				updateHandleCenters(rearMid);
				gs = gs + 1;
				topCenter.setInReferenceFrame(0, 0, originalBounds.getMinZ() + originalBounds.getTotalZ() * gs);

				Transform scaleXYZ = null;
				try {
					// Pivot = anchor = (maxX, midY, minZ)
					double midY = (originalBounds.getMinY() + originalBounds.getMaxY()) / 2.0;
					scaleXYZ = new Transform().translate(originalBounds.getMaxX(), midY, originalBounds.getMinZ())
							.scale(notZero(gs), notZero(gs), notZero(gs))
							.translate(-originalBounds.getMaxX(), -midY, -originalBounds.getMinZ());
				} catch (Exception ex) {
					Log.error(ex);
				}
				BowlerStudio.runLater(() -> updateTopCenter());
				if (scaleXYZ != null)
					rescaleMeshes(workplaneOffset, scaleXYZ);
				return;
			}

			// rearMid moves in X only; derive sx relative to the fixed front edge.
			double newMinX = rearMid.getCurrentInReferenceFrame().getX();
			double sx = (originalBounds.getMaxX() - newMinX) / originalBounds.getTotalX();

			// Keep the two rear corners aligned to the handle's X position
			double z = rightRear.manipulator.getCurrentPose().getZ();
			rightRear.manipulator.setInReferenceFrame(rearMid.manipulator.getCurrentPose().getX(),
					rightRear.manipulator.getCurrentPose().getY(), z);
			leftRear.manipulator.setInReferenceFrame(rearMid.manipulator.getCurrentPose().getX(),
					leftRear.manipulator.getCurrentPose().getY(), z);

			Transform scaleXYZ = null;
			try {
				// Anchor: the front edge stays fixed at originalBounds.getMaxX()
				scaleXYZ = new Transform()
						.translate(originalBounds.getMaxX(), originalBounds.getMinY(), originalBounds.getMinZ())
						.scale(notZero(sx), 1.0, 1.0)
						.translate(-originalBounds.getMaxX(), -originalBounds.getMinY(), -originalBounds.getMinZ());
			} catch (Exception ex) {
				Log.error(ex);
			}
			updateHandleCenters(rearMid);

			BowlerStudio.runLater(() -> updateTopCenter());
			if (scaleXYZ != null)
				rescaleMeshes(workplaneOffset, scaleXYZ);
		});

		// --- leftMid: sits at (midX, maxY, cornerZ) — rescales Y only ---
		// Dragging leftMid grows/shrinks the Y dimension while keeping the
		// right edge (minY) fixed. leftFront and leftRear corners follow.

		leftMid.getMesh().setOnMousePressed(ev -> {
			originalBounds = getBounds();
			beingUpdated = leftMid;
		});

		leftMid.manipulator.addEventListener(ev -> {

			if (scalingFlag) {
				scalingFlag = false;
				return;
			}
			if ((beingUpdated != leftMid) || (originalBounds == null))
				return;

			controlSprites.hideRotationHandles();

			// Uniform scaling with shift key
			if ((ev != null) && ev.isShiftDown()) {

				// leftMid.manipulator.setSnapGridStatus(false);
				double original_tx = originalBounds.getTotalX();
				double original_ty = originalBounds.getTotalY();

				double mouseY = leftMid.getCurrentInReferenceFrame().getY() - originalBounds.getMaxY();
				double scale = mouseY / original_ty;

				double rawNewY = original_ty * (1.0 + scale);
				double gridNewY = Math.round(rawNewY / snapGrid) * snapGrid;
				double rawNewX = original_tx * (1.0 + scale);
				double gridNewX = Math.round(rawNewX / snapGrid) * snapGrid;

				double gs = Math.abs(rawNewY - gridNewY) < Math.abs(rawNewX - gridNewX)
						? (gridNewY / original_ty) - 1.0
						: (gridNewX / original_tx) - 1.0;

				scalingFlag = true;

				// Anchor: right midpoint (midX, minY). Each corner's delta =
				// its vector from the anchor * gs.
				// rightFront (maxX, minY): rel=( tx/2, 0) → delta=( tx/2*gs, 0)
				// rightRear (minX, minY): rel=(-tx/2, 0) → delta=(-tx/2*gs, 0)
				// leftFront (maxX, maxY): rel=( tx/2, ty) → delta=( tx/2*gs, ty*gs)
				// leftRear (minX, maxY): rel=(-tx/2, ty) → delta=(-tx/2*gs, ty*gs)
				rightFront.manipulator.setInReferenceFrame(original_tx / 2.0 * gs, 0, 0);
				rightRear.manipulator.setInReferenceFrame(-original_tx / 2.0 * gs, 0, 0);
				leftFront.manipulator.setInReferenceFrame(original_tx / 2.0 * gs, original_ty * gs, 0);
				leftRear.manipulator.setInReferenceFrame(-original_tx / 2.0 * gs, original_ty * gs, 0);

				// leftMid (midX, maxY): rel=(0, ty) → delta=(0, ty*gs)
				// rightMid (midX, minY): anchor → stays (0, 0, 0)
				// frontMid (maxX, midY): rel=(tx/2, ty/2) → delta=(tx/2*gs, ty/2*gs)
				// rearMid (minX, midY): rel=(-tx/2,ty/2) → delta=(-tx/2*gs,ty/2*gs)
				// leftMid.manipulator.setInReferenceFrame(0, original_ty * gs, 0);
				// rightMid.manipulator.setInReferenceFrame(0, 0, 0);
				// frontMid.manipulator.setInReferenceFrame(original_tx / 2.0 * gs, original_ty
				// / 2.0 * gs, 0);
				// rearMid.manipulator.setInReferenceFrame(-original_tx / 2.0 * gs, original_ty
				// / 2.0 * gs, 0);

				updateHandleCenters(leftMid);
				gs = gs + 1;
				topCenter.setInReferenceFrame(0, 0, originalBounds.getMinZ() + originalBounds.getTotalZ() * gs);

				Transform scaleXYZ = null;
				try {
					// Pivot = anchor = (midX, minY, minZ)
					double midX = (originalBounds.getMinX() + originalBounds.getMaxX()) / 2.0;
					scaleXYZ = new Transform().translate(midX, originalBounds.getMinY(), originalBounds.getMinZ())
							.scale(notZero(gs), notZero(gs), notZero(gs))
							.translate(-midX, -originalBounds.getMinY(), -originalBounds.getMinZ());
				} catch (Exception ex) {
					Log.error(ex);
				}
				BowlerStudio.runLater(() -> updateTopCenter());
				if (scaleXYZ != null)
					rescaleMeshes(workplaneOffset, scaleXYZ);
				return;
			}

			// leftMid moves in Y only; derive sy relative to the fixed right edge.
			double newMaxY = leftMid.getCurrentInReferenceFrame().getY();
			double sy = (newMaxY - originalBounds.getMinY()) / originalBounds.getTotalY();

			// Keep the two left corners aligned to the handle's Y position
			double z = leftFront.manipulator.getCurrentPose().getZ();
			leftFront.manipulator.setInReferenceFrame(leftFront.manipulator.getCurrentPose().getX(),
					leftMid.manipulator.getCurrentPose().getY(), z);
			leftRear.manipulator.setInReferenceFrame(leftRear.manipulator.getCurrentPose().getX(),
					leftMid.manipulator.getCurrentPose().getY(), z);

			Transform scaleXYZ = null;
			try {
				// Anchor: the right edge stays fixed at originalBounds.getMinY()
				scaleXYZ = new Transform()
						.translate(originalBounds.getMinX(), originalBounds.getMinY(), originalBounds.getMinZ())
						.scale(1.0, notZero(sy), 1.0)
						.translate(-originalBounds.getMinX(), -originalBounds.getMinY(), -originalBounds.getMinZ());
			} catch (Exception ex) {
				Log.error(ex);
			}
			updateHandleCenters(leftMid);
			BowlerStudio.runLater(() -> updateTopCenter());
			if (scaleXYZ != null)
				rescaleMeshes(workplaneOffset, scaleXYZ);
		});

		// --- rightMid: sits at (midX, minY, cornerZ) — rescales Y only ---
		// Dragging rightMid grows/shrinks the Y dimension while keeping the
		// left edge (maxY) fixed. rightFront and rightRear corners follow.

		rightMid.getMesh().setOnMousePressed(ev -> {
			originalBounds = getBounds();
			beingUpdated = rightMid;
		});

		rightMid.manipulator.addEventListener(ev -> {

			if (scalingFlag) {
				scalingFlag = false;
				return;
			}
			if ((beingUpdated != rightMid) || (originalBounds == null))
				return;

			controlSprites.hideRotationHandles();

			// Uniform scaling with shift key
			if ((ev != null) && ev.isShiftDown()) {

				// rightMid.manipulator.setSnapGridStatus(false);
				double original_tx = originalBounds.getTotalX();
				double original_ty = originalBounds.getTotalY();

				double mouseY = -rightMid.getCurrentInReferenceFrame().getY() + originalBounds.getMinY();
				double scale = mouseY / original_ty;

				double rawNewY = original_ty * (1.0 + scale);
				double gridNewY = Math.round(rawNewY / snapGrid) * snapGrid;
				double rawNewX = original_tx * (1.0 + scale);
				double gridNewX = Math.round(rawNewX / snapGrid) * snapGrid;

				double gs = Math.abs(rawNewY - gridNewY) < Math.abs(rawNewX - gridNewX)
						? (gridNewY / original_ty) - 1.0
						: (gridNewX / original_tx) - 1.0;

				scalingFlag = true;

				// Anchor: left midpoint (midX, maxY). Each corner's delta =
				// its vector from the anchor * gs.
				// rightFront (maxX, minY): rel=( tx/2, -ty) → delta=( tx/2*gs, -ty*gs)
				// rightRear (minX, minY): rel=(-tx/2, -ty) → delta=(-tx/2*gs, -ty*gs)
				// leftFront (maxX, maxY): rel=( tx/2, 0) → delta=( tx/2*gs, 0)
				// leftRear (minX, maxY): rel=(-tx/2, 0) → delta=(-tx/2*gs, 0)
				rightFront.manipulator.setInReferenceFrame(original_tx / 2.0 * gs, -original_ty * gs, 0);
				rightRear.manipulator.setInReferenceFrame(-original_tx / 2.0 * gs, -original_ty * gs, 0);
				leftFront.manipulator.setInReferenceFrame(original_tx / 2.0 * gs, 0, 0);
				leftRear.manipulator.setInReferenceFrame(-original_tx / 2.0 * gs, 0, 0);

				// rightMid (midX, minY): rel=(0, -ty) → delta=(0, -ty*gs)
				// leftMid (midX, maxY): anchor → stays (0, 0, 0)
				// frontMid (maxX, midY): rel=(tx/2, -ty/2) → delta=(tx/2*gs, -ty/2*gs)
				// rearMid (minX, midY): rel=(-tx/2,-ty/2) → delta=(-tx/2*gs, -ty/2*gs)
				// rightMid.manipulator.setInReferenceFrame(0, -original_ty * gs, 0);
				// leftMid.manipulator.setInReferenceFrame(0, 0, 0);
				// frontMid.manipulator.setInReferenceFrame(original_tx / 2.0 * gs, -original_ty
				// / 2.0 * gs, 0);
				// rearMid.manipulator.setInReferenceFrame(-original_tx / 2.0 * gs, -original_ty
				// / 2.0 * gs, 0);

				updateHandleCenters(rightMid);
				gs = gs + 1;
				topCenter.setInReferenceFrame(0, 0, originalBounds.getMinZ() + originalBounds.getTotalZ() * gs);

				Transform scaleXYZ = null;
				try {
					// Pivot = anchor = (midX, maxY, minZ)
					double midX = (originalBounds.getMinX() + originalBounds.getMaxX()) / 2.0;
					scaleXYZ = new Transform().translate(midX, originalBounds.getMaxY(), originalBounds.getMinZ())
							.scale(notZero(gs), notZero(gs), notZero(gs))
							.translate(-midX, -originalBounds.getMaxY(), -originalBounds.getMinZ());
				} catch (Exception ex) {
					Log.error(ex);
				}
				BowlerStudio.runLater(() -> updateTopCenter());
				if (scaleXYZ != null)
					rescaleMeshes(workplaneOffset, scaleXYZ);
				return;
			}

			// rightMid moves in Y only; derive sy relative to the fixed left edge.
			double newMinY = rightMid.getCurrentInReferenceFrame().getY();
			double sy = (originalBounds.getMaxY() - newMinY) / originalBounds.getTotalY();

			// Keep the two right corners aligned to the handle's Y position
			double z = rightFront.manipulator.getCurrentPose().getZ();
			rightFront.manipulator.setInReferenceFrame(rightFront.manipulator.getCurrentPose().getX(),
					rightMid.manipulator.getCurrentPose().getY(), z);
			rightRear.manipulator.setInReferenceFrame(rightRear.manipulator.getCurrentPose().getX(),
					rightMid.manipulator.getCurrentPose().getY(), z);

			Transform scaleXYZ = null;
			try {
				// Anchor: the left edge stays fixed at originalBounds.getMaxY()
				scaleXYZ = new Transform()
						.translate(originalBounds.getMinX(), originalBounds.getMaxY(), originalBounds.getMinZ())
						.scale(1.0, notZero(sy), 1.0)
						.translate(-originalBounds.getMinX(), -originalBounds.getMaxY(), -originalBounds.getMinZ());
			} catch (Exception ex) {
				Log.error(ex);
			}
			updateHandleCenters(rightMid);
			BowlerStudio.runLater(() -> updateTopCenter());
			if (scaleXYZ != null)
				rescaleMeshes(workplaneOffset, scaleXYZ);
		});

		// -----------------------------------------------------------------------
		// Save listeners for all controls (corners + edges + top)
		// -----------------------------------------------------------------------

		controls = Arrays.asList(topCenter, rightFront, rightRear, leftFront, leftRear, frontMid, rearMid, leftMid,
				rightMid);
		for (ResizingHandle c : Arrays.asList(frontMid, rearMid, leftMid, rightMid)) {
			c.setMyColor(Color.BLACK, Color.RED);
		}
		topCenter.setMyColor(new Color(0.5, 0.5, 0.5, 1), Color.RED);
		for (ResizingHandle c : controls) {
			meshControls.add(c.getMesh());
			c.manipulator.setFrameOfReference(() -> ap.get().getWorkplane());
			c.manipulator.addSaveListener(() -> {
				if (beingUpdated != c)
					return;
				try {
					Thread.sleep(32);
				} catch (InterruptedException e) {
					com.neuronrobotics.sdk.common.Log.error(e);
				}
				TransformNR wp = ap.get().getWorkplane().copy();
				TransformNR rrC = rightRear.getCurrentInReferenceFrame();
				TransformNR lfC = leftFront.getCurrentInReferenceFrame();
				TransformNR tcC = topCenter.getCurrentInReferenceFrame();

				bounds = getBounds();
				for (ResizingHandle ctrl : controls)
					ctrl.manipulator.set(0, 0, 0);
				try {
					Resize setResize = new Resize().setNames(session.selectedSnapshot()).setWorkplane(wp).setResize(tcC,
							lfC, rrC);

					if (resizeAllowed) {
						Thread t = ap.addOp(setResize);
						try {
							t.join();
						} catch (InterruptedException e) {
							com.neuronrobotics.sdk.common.Log.error(e);
						}
					}
					beingUpdated = null;
					originalBounds = null;
				} catch (Exception ex) {
					Log.error(ex);
				}
				BowlerStudio.runLater(() -> threeDTarget());
			});
		}
	}

	private void updateHandleCenters(ResizingHandle source) {

		double nY = (rightFront.manipulator.getCurrentPose().getY() - leftFront.manipulator.getCurrentPose().getY())
				/ 2;
		double nX = (rightFront.manipulator.getCurrentPose().getX() - rightRear.manipulator.getCurrentPose().getX())
				/ 2;
		double rmx = leftRear.manipulator.getCurrentPose().getX();
		double ry = rightFront.manipulator.getCurrentPose().getY();
		double ly = leftFront.manipulator.getCurrentPose().getY();
		double z = leftFront.manipulator.getCurrentPose().getZ();
		double fx = (rightFront.manipulator.getCurrentPose().getX());
		if (source == rightRear) {
			nX = (-rightFront.manipulator.getCurrentPose().getX() + rightRear.manipulator.getCurrentPose().getX()) / 2;
			rmx = rightRear.manipulator.getCurrentPose().getX();
			ry = rightRear.manipulator.getCurrentPose().getY();
			nY = (rightRear.manipulator.getCurrentPose().getY() - leftRear.manipulator.getCurrentPose().getY()) / 2;
		}
		if (source == leftFront) {
			nY = (-rightFront.manipulator.getCurrentPose().getY() + leftFront.manipulator.getCurrentPose().getY()) / 2;
			fx = (leftFront.manipulator.getCurrentPose().getX());
			nX = (leftFront.manipulator.getCurrentPose().getX() - leftRear.manipulator.getCurrentPose().getX()) / 2;
		}
		if (source == leftRear) {
			nY = (-rightRear.manipulator.getCurrentPose().getY() + leftRear.manipulator.getCurrentPose().getY()) / 2;
			nX = (-leftFront.manipulator.getCurrentPose().getX() + leftRear.manipulator.getCurrentPose().getX()) / 2;
			ly = leftRear.manipulator.getCurrentPose().getY();
		}
		if (source == frontMid) {
			nX = (frontMid.manipulator.getCurrentPose().getX() - rearMid.manipulator.getCurrentPose().getX()) / 2;
			nY = frontMid.manipulator.getCurrentPose().getY();
			ry = rightRear.manipulator.getinLocalInReferenceFrame().getY();
			ly = leftRear.manipulator.getinLocalInReferenceFrame().getY();
		}
		if (source == rearMid) {
			nX = (-frontMid.manipulator.getCurrentPose().getX() + rearMid.manipulator.getCurrentPose().getX()) / 2;
			nY = rearMid.manipulator.getCurrentPose().getY();
			ry = rightRear.manipulator.getinLocalInReferenceFrame().getY();
			ly = leftRear.manipulator.getinLocalInReferenceFrame().getY();
		}
		if (source == rightMid) {
			nY = (rightMid.manipulator.getCurrentPose().getY()
					- leftMid.manipulator.getinLocalInReferenceFrame().getY()) / 2;
			nX = rightMid.manipulator.getCurrentPose().getX();
			fx = leftFront.manipulator.getinLocalInReferenceFrame().getX();
			rmx = rightRear.manipulator.getinLocalInReferenceFrame().getX();
			com.neuronrobotics.sdk.common.Log.debug("Mid Right update x " + fx);
		}
		if (source == leftMid) {
			nY = (-rightMid.manipulator.getCurrentPose().getY() + leftMid.manipulator.getCurrentPose().getY()) / 2;
			nX = leftMid.manipulator.getCurrentPose().getX();

			fx = leftFront.manipulator.getinLocalInReferenceFrame().getX();
			rmx = rightRear.manipulator.getinLocalInReferenceFrame().getX();
		}
		if (source != frontMid)
			frontMid.manipulator.setInReferenceFrame(fx, nY, z);
		if (source != rearMid)
			rearMid.manipulator.setInReferenceFrame(rmx, nY, z);
		if (source != rightMid)
			rightMid.manipulator.setInReferenceFrame(nX, ry, z);
		if (source != leftMid)
			leftMid.manipulator.setInReferenceFrame(nX, ly, z);
	}

	public void setResizeAllowed(boolean resizeAllowed, boolean moveLock) {
		this.resizeAllowed = resizeAllowed;
		for (ResizingHandle c : controls)
			c.setResizeAllowed(resizeAllowed, moveLock);
	}

	private void uniformScalingZ(TransformNR tcC) {
		double startZ = bounds.getTotalZ();
		double startX = bounds.getTotalX();
		double startY = bounds.getTotalY();

		double nowZ = tcC.getZ() - bounds.getMinZ();
		double scale = nowZ / startZ;
		double newXComp = (startX * scale - startX) / 2;
		double newYComp = (startY * scale - startY) / 2;

		double z = leftRear.manipulator.getCurrentPose().getZ();

		double newX1 = -newXComp;
		double newY1 = -newYComp;
		double newX2 = newXComp;
		double newY2 = newYComp;
		scalingFlag = true;
		rightRear.manipulator.setInReferenceFrame(newX1, newY1, z);
		leftFront.manipulator.setInReferenceFrame(newX2, newY2, z);
		rightFront.manipulator.setInReferenceFrame(newX2, newY1, z);
		leftRear.manipulator.setInReferenceFrame(newX1, newY2, z);
	}

	private void updateTopCenter() {
		ResizingHandle beingUpdated2 = beingUpdated;
		if ((beingUpdated2 != topCenter) || (beingUpdated2 == null)) {
			TransformNR rrC = rightRear.getCurrentInReferenceFrame();
			TransformNR lfC = leftFront.getCurrentInReferenceFrame();
			TransformNR tcC = topCenter.getCurrentInReferenceFrame();
			double x = (lfC.getX() - rrC.getX()) / 2 + rrC.getX();
			double y = (lfC.getY() - rrC.getY()) / 2 + rrC.getY();
			double z = tcC.getZ();
			topCenter.setInReferenceFrame(x, y, z);
			beingUpdated = beingUpdated2;
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

	private void threeDTarget() { // New way for control handles, always closest to the work plane
		Vector3d center = bounds.getCenter();
		Vector3d min = bounds.getMin(); // relative to work plane
		Vector3d max = bounds.getMax(); // relative to work plane

		objectBottomZ = min.z; // Store correct object Z-min

		double cornerZ = 0; // Object is cut by work plane

		// choose the face that is closest to the work-plane
		if (min.z > 0) // object is above the work plane
			cornerZ = min.z;
		else if (max.z < 0) // object is below the work plane
			cornerZ = max.z;

		// Corner handles
		topCenter.threeDTarget(screenW, screenH, zoom, new TransformNR(center.x, center.y, max.z), cf, locked);
		leftFront.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, max.y, cornerZ), cf, locked);
		leftRear.threeDTarget(screenW, screenH, zoom, new TransformNR(min.x, max.y, cornerZ), cf, locked);
		rightFront.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, min.y, cornerZ), cf, locked);
		rightRear.threeDTarget(screenW, screenH, zoom, new TransformNR(min.x, min.y, cornerZ), cf, locked);

		// Edge-midpoint handles — positioned at the midpoint of each edge
		frontMid.threeDTarget(screenW, screenH, zoom, new TransformNR(max.x, (min.y + max.y) / 2.0, cornerZ), cf,
				locked);
		rearMid.threeDTarget(screenW, screenH, zoom, new TransformNR(min.x, (min.y + max.y) / 2.0, cornerZ), cf,
				locked);
		leftMid.threeDTarget(screenW, screenH, zoom, new TransformNR((min.x + max.x) / 2.0, max.y, cornerZ), cf,
				locked);
		rightMid.threeDTarget(screenW, screenH, zoom, new TransformNR((min.x + max.x) / 2.0, min.y, cornerZ), cf,
				locked);

		updateTopCenter();
	}

	boolean leftSelected() {
		return leftFront.isSelected() || leftRear.isSelected() || frontMid.isSelected() || rearMid.isSelected();
	}

	boolean rightSelected() {
		return rightFront.isSelected() || rightRear.isSelected();
	}

	boolean frontSelected() {
		return rightFront.isSelected() || leftFront.isSelected() || leftMid.isSelected() || rightMid.isSelected();
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
		Vector3d min = new Vector3d(lr.getX(), lr.getY(), objectBottomZ);
		Vector3d max = new Vector3d(rf.getX(), rf.getY(), tc.getZ());

		return new Bounds(min, max);
	}

	public void setSnapGrid(double snapGrid) {
		this.snapGrid = snapGrid;

		for (ResizingHandle ctrl : controls)
			ctrl.manipulator.setIncrement(snapGrid);
	}

	public void resetSelected() {
		originalBounds = null; // reset var that exists to help maintain aspect ratios

		for (ResizingHandle c : controls)
			c.resetSelected();
	}

	public void set(double x, double y, double z) throws NowFXThreadException {
		if (!Platform.isFxApplicationThread()) {
			throw new NowFXThreadException();
		}
		Bounds b = getBounds();
		com.neuronrobotics.sdk.common.Log.error("Resizing to " + x + " " + y + " " + z);
		// BowlerStudio.runLater(() -> {

		if (topCenter.isSelected()) {
			topCenter.manipulator.setInReferenceFrame(0, 0, z - b.getTotalZ());
			if (topCenter.isUniform()) {
				TransformNR tcC = topCenter.getCurrentInReferenceFrame();
				uniformScalingZ(tcC);
			}
			topCenter.manipulator.fireSave();
			return;
		}
		boolean deltaX = Math.abs(b.getTotalX() - x) > Plane.getEPSILON();
		boolean deltaY = Math.abs(b.getTotalY() - y) > Plane.getEPSILON();
		boolean deltaZ = Math.abs(b.getTotalZ() - z) > Plane.getEPSILON();
		boolean isAnyCornerUniform = leftFront.isUniform() || leftRear.isUniform() || rightFront.isUniform()
				|| rightRear.isUniform();
		boolean isUniformX = deltaX && (isAnyCornerUniform || frontMid.isUniform() || rearMid.isUniform());
		boolean isUniformY = deltaY && (isAnyCornerUniform || leftMid.isUniform() || rightMid.isUniform());

		boolean isFrontSelected = frontMid.isSelected() || leftFront.isSelected() || rightFront.isSelected();
		boolean isRearSelected = rearMid.isSelected() || leftRear.isSelected() || rightRear.isSelected();
		boolean isRightSelected = rightMid.isSelected() || rightFront.isSelected() || rightRear.isSelected();
		boolean isLeftSelected = leftMid.isSelected() || leftFront.isSelected() || leftRear.isSelected();
		if (isLeftSelected && isUniformY) {
			double s = y / b.getTotalY();
			double gs = s - 1.0;
			if (Double.isFinite(gs)) {
				double original_tx = b.getTotalX();
				double original_ty = b.getTotalY();
				scalingFlag = true;
				// Anchor: right midpoint (midX, minY).
				// rightFront (maxX, minY): rel=( tx/2,   0) → delta=( tx/2*gs,       0)
				// rightRear  (minX, minY): rel=(-tx/2,   0) → delta=(-tx/2*gs,       0)
				// leftFront  (maxX, maxY): rel=( tx/2,  ty) → delta=( tx/2*gs,  ty*gs)
				// leftRear   (minX, maxY): rel=(-tx/2,  ty) → delta=(-tx/2*gs,  ty*gs)
				rightFront.manipulator.setInReferenceFrame(original_tx / 2.0 * gs, 0, 0);
				rightRear.manipulator.setInReferenceFrame(-original_tx / 2.0 * gs, 0, 0);
				leftFront.manipulator.setInReferenceFrame(original_tx / 2.0 * gs, original_ty * gs, 0);
				leftRear.manipulator.setInReferenceFrame(-original_tx / 2.0 * gs, original_ty * gs, 0);
				double gsFinal = gs + 1.0;
				topCenter.setInReferenceFrame(0, 0, b.getMinZ() + b.getTotalZ() * gsFinal);
				if (leftFront.isSelected())
					leftFront.manipulator.fireSaveSynchronous();
				else if (leftRear.isSelected())
					leftRear.manipulator.fireSaveSynchronous();
				else if (leftMid.isSelected())
					leftMid.manipulator.fireSaveSynchronous();
			}
			return;
		}
		if (isRightSelected && isUniformY) {
			double s = y / b.getTotalY();
			double gs = s - 1.0;
			if (Double.isFinite(gs)) {
				double original_tx = b.getTotalX();
				double original_ty = b.getTotalY();
				scalingFlag = true;
				// Anchor: left midpoint (midX, maxY).
				// rightFront (maxX, minY): rel=( tx/2, -ty) → delta=( tx/2*gs, -ty*gs)
				// rightRear  (minX, minY): rel=(-tx/2, -ty) → delta=(-tx/2*gs, -ty*gs)
				// leftFront  (maxX, maxY): rel=( tx/2,   0) → delta=( tx/2*gs,       0)
				// leftRear   (minX, maxY): rel=(-tx/2,   0) → delta=(-tx/2*gs,       0)
				rightFront.manipulator.setInReferenceFrame(original_tx / 2.0 * gs, -original_ty * gs, 0);
				rightRear.manipulator.setInReferenceFrame(-original_tx / 2.0 * gs, -original_ty * gs, 0);
				leftFront.manipulator.setInReferenceFrame(original_tx / 2.0 * gs, 0, 0);
				leftRear.manipulator.setInReferenceFrame(-original_tx / 2.0 * gs, 0, 0);
				double gsFinal = gs + 1.0;
				topCenter.setInReferenceFrame(0, 0, b.getMinZ() + b.getTotalZ() * gsFinal);
				if (rightFront.isSelected())
					rightFront.manipulator.fireSaveSynchronous();
				else if (rightRear.isSelected())
					rightRear.manipulator.fireSaveSynchronous();
				else if (rightMid.isSelected())
					rightMid.manipulator.fireSaveSynchronous();
			}
			return;
		}
		if (isRearSelected && isUniformX) {
			double s = x / b.getTotalX();
			double gs = s - 1.0;
			if (Double.isFinite(gs)) {
				double original_tx = b.getTotalX();
				double original_ty = b.getTotalY();
				scalingFlag = true;
				rightFront.manipulator.setInReferenceFrame(0, -original_ty / 2.0 * gs, 0);
				rightRear.manipulator.setInReferenceFrame(-original_tx * gs, -original_ty / 2.0 * gs, 0);
				leftFront.manipulator.setInReferenceFrame(0, original_ty / 2.0 * gs, 0);
				leftRear.manipulator.setInReferenceFrame(-original_tx * gs, original_ty / 2.0 * gs, 0);
				double gsFinal = gs + 1.0;
				topCenter.setInReferenceFrame(0, 0, b.getMinZ() + b.getTotalZ() * gsFinal);
				if (leftRear.isSelected())
					leftRear.manipulator.fireSaveSynchronous();
				else if (rightRear.isSelected())
					rightRear.manipulator.fireSaveSynchronous();
				else if (rearMid.isSelected())
					rearMid.manipulator.fireSaveSynchronous();

			}
			return;
		}
		if (isFrontSelected && isUniformX) {
			double s = x / b.getTotalX();
			double gs = s - 1.0;
			if (Double.isFinite(gs)) {
				double original_tx = b.getTotalX();
				double original_ty = b.getTotalY();
				scalingFlag = true;
				rightFront.manipulator.setInReferenceFrame(original_tx * gs, -original_ty / 2.0 * gs, 0);
				rightRear.manipulator.setInReferenceFrame(0, -original_ty / 2.0 * gs, 0);
				leftFront.manipulator.setInReferenceFrame(original_tx * gs, original_ty / 2.0 * gs, 0);
				leftRear.manipulator.setInReferenceFrame(0, original_ty / 2.0 * gs, 0);
				double gsFinal = gs + 1.0;
				topCenter.setInReferenceFrame(0, 0, b.getMinZ() + b.getTotalZ() * gsFinal);
				if (leftFront.isSelected())
					leftFront.manipulator.fireSaveSynchronous();
				else if (rightFront.isSelected())
					rightFront.manipulator.fireSaveSynchronous();
				else if (frontMid.isSelected())
					frontMid.manipulator.fireSaveSynchronous();

			}
			return;
		}
		// if (isFrontSelected && isUniformX) {
		//			double s = x / b.getTotalX();
		//			double gs = s - 1.0;
		//			if (Double.isFinite(gs)) {
		//				BowlerStudio.runLater(() -> {
		//					leftFront.manipulator.setInReferenceFrame(x - b.getTotalX(), y - b.getTotalY() * gs, 0);
		//					rightFront.manipulator.setInReferenceFrame(x - b.getTotalX(), -(y - b.getTotalY() * gs), 0);
		//					frontMid.setInReferenceFrame(x - b.getTotalX(), 0, 0);
		//					// rightRear.manipulator.setInReferenceFrame(b.getTotalX() * gs, 0, 0);
		//					topCenter.setInReferenceFrame(0, 0, b.getMinZ() + b.getTotalZ() * s);
		//					leftFront.manipulator.fireSave();
		//				});
		//			}
		//			return;
		//		}

		// original method continues below...
		if (leftFront.isSelected()) {
			com.neuronrobotics.sdk.common.Log.error("leftFront resize");
			leftFront.manipulator.setInReferenceFrame(x - b.getTotalX(), (y - b.getTotalY()), 0);
			leftFront.manipulator.fireSave();
			return;
		}

		if (leftRear.isSelected()) {
			double lr_x = -(x - b.getTotalX());
			double lr_y = (y - b.getTotalY());
			scalingFlag = false;
			leftRear.manipulator.setInReferenceFrame(lr_x, lr_y, 0);
			leftFront.manipulator.setInReferenceFrame(leftFront.manipulator.getCurrentPose().getX(), lr_y, 0);
			rightRear.manipulator.setInReferenceFrame(lr_x, rightRear.manipulator.getCurrentPose().getY(), 0);
			leftRear.manipulator.fireSave();
			return;
		}

		if (rightFront.isSelected()) {
			double rf_x = x - b.getTotalX();
			double rf_y = -(y - b.getTotalY());
			scalingFlag = false;
			rightFront.manipulator.setInReferenceFrame(rf_x, rf_y, 0);
			rightRear.manipulator.setInReferenceFrame(rightRear.manipulator.getCurrentPose().getX(), rf_y, 0);
			leftFront.manipulator.setInReferenceFrame(rf_x, leftFront.manipulator.getCurrentPose().getY(), 0);
			rightFront.manipulator.fireSave();
			return;
		}

		if (rightRear.isSelected()) {
			rightRear.manipulator.setInReferenceFrame(-(x - b.getTotalX()), -(y - b.getTotalY()), 0);
			rightRear.manipulator.fireSave();
			return;
		}

		// Edge-midpoint programmatic resize
		if (frontMid.isSelected()) {
			com.neuronrobotics.sdk.common.Log.error("frontMid resize (X only)");
			frontMid.manipulator.setInReferenceFrame(x - b.getTotalX(), 0, 0);
			leftFront.manipulator.setInReferenceFrame(x - b.getTotalX(), (y - b.getTotalY()), 0);
			frontMid.manipulator.fireSave();
			return;
		}

		if (rearMid.isSelected()) {
			com.neuronrobotics.sdk.common.Log.error("rearMid resize (X only)");
			rearMid.manipulator.setInReferenceFrame(-(x - b.getTotalX()), 0, 0);
			rightRear.manipulator.setInReferenceFrame(-(x - b.getTotalX()), -(y - b.getTotalY()), 0);
			rearMid.manipulator.fireSave();
			return;
		}

		if (leftMid.isSelected()) {
			com.neuronrobotics.sdk.common.Log.error("leftMid resize (Y only)");
			leftMid.manipulator.setInReferenceFrame(0, y - b.getTotalY(), 0);
			leftFront.manipulator.setInReferenceFrame(x - b.getTotalX(), (y - b.getTotalY()), 0);
			leftMid.manipulator.fireSave();
			return;
		}

		if (rightMid.isSelected()) {
			com.neuronrobotics.sdk.common.Log.error("rightMid resize (Y only)");
			rightMid.manipulator.setInReferenceFrame(0, -(y - b.getTotalY()), 0);
			rightRear.manipulator.setInReferenceFrame(-(x - b.getTotalX()), -(y - b.getTotalY()), 0);
			rightMid.manipulator.fireSave();
			return;
		}
		// });
	}

	public void hide() {
		topCenter.hide();
		leftFront.hide();
		leftRear.hide();
		rightFront.hide();
		rightRear.hide();
		frontMid.hide();
		rearMid.hide();
		leftMid.hide();
		rightMid.hide();
	}

	public void updateOrientation(TransformNR cameraFrame) {
		for (ResizingHandle c : controls)
			c.updateOrientation(cameraFrame);
	}

	public ArrayList<MeshView> getMeshes() {
		return meshControls;
	}

}
