package com.commonwealthrobotics.controls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.RulerManager;
import com.commonwealthrobotics.WorkplaneManager;
import com.commonwealthrobotics.align.AlignManager;
import com.commonwealthrobotics.controls.SelectionSession.MeshHolder;
import com.commonwealthrobotics.mirror.MirrorSessionManager;
import com.commonwealthrobotics.numbers.TextFieldDimension;
import com.commonwealthrobotics.numbers.ThreedNumber;
import com.commonwealthrobotics.rotate.RotationSessionManager;
import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;
import com.neuronrobotics.bowlerstudio.BowlerKernel;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Align;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.InvalidLocationMove;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.MoveCenter;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.Log;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
//import eu.mihosoft.vrl.v3d.Cylinder;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.Node;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;

public class ControlSprites {
	private static final double SIZE_OF_DOT = 0.5;
	private static final double NUMBER_OF_MM_PER_DOT = 1;
	private double screenW;
	private double screenH;
	private double zoom;
	private double az;
	private double el;
	private double x;
	private double y;
	private double z;
	private SelectionSession session;
	private BowlerStudio3dEngine engine;
	// private BowlerStudio3dEngine spriteEngine;
	private ResizeSessionManager scaleSession;
	private RotationSessionManager rotationManager;
	private AlignManager align;
	private MirrorSessionManager mirror;
	private Rectangle footprint = new Rectangle(100, 100, new Color(0, 0, 1, 0.25));
	// private Rectangle bottomDimensions = new Rectangle(100,100,new
	// Color(0,0,1,0.25));
	private Affine workplaneOffset = new Affine();
	private MoveUpArrow upArrow;
	private DottedLine frontLine = new DottedLine(SIZE_OF_DOT, NUMBER_OF_MM_PER_DOT, workplaneOffset);
	private DottedLine backLine = new DottedLine(SIZE_OF_DOT, NUMBER_OF_MM_PER_DOT, workplaneOffset);
	private DottedLine leftLine = new DottedLine(SIZE_OF_DOT, NUMBER_OF_MM_PER_DOT, workplaneOffset);
	private DottedLine rightLine = new DottedLine(SIZE_OF_DOT, NUMBER_OF_MM_PER_DOT, workplaneOffset);
	private DottedLine heightLine = new DottedLine(SIZE_OF_DOT, NUMBER_OF_MM_PER_DOT, workplaneOffset);

	private ArrayList<Node> allElems = new ArrayList<Node>();
	private boolean selectionLive = false;
	private Bounds b;
	private Bounds bounds;
	private List<DottedLine> lines;
	private Affine spriteFace = new Affine();
	private Affine moveUpLocation = new Affine();
	private Scale scaleTF = new Scale();
	private Affine selection;
	private Manipulation zMoveManipulator;
	private Manipulation manipulation;
	// private CaDoodleFile cadoodle;
	private SpriteDisplayMode mode = SpriteDisplayMode.Default;
	private TransformNR cf;
	private ThreedNumber xdimen;
	private ThreedNumber ydimen;
	private ThreedNumber zdimen;
	private ThreedNumber xOffset;
	private ThreedNumber yOffset;
	private ThreedNumber zOffset;
	private List<ThreedNumber> numbers;
	private boolean xymoving = false;
	private boolean zmoving = false;
	private CaDoodleOperation currentOp;
	private ActiveProject ap;
	private RulerManager ruler;
	private final Pane overlayPane; // Overlay pane for 2D objects
	private Point3D startingPosition3D;
	private double objectHeight = 0;
	private double cameraFovDegrees;
	private Affine zMoveOffsetFootprint;

	public void setSnapGrid(double snapGridValue) {
		zMoveManipulator.setIncrement(snapGridValue);
		scaleSession.setSnapGrid(snapGridValue);
	}

	public void updateHandleOrientations(TransformNR cameraFrame) {
		BowlerKernel.runLater(() -> TransformFactory.nrToAffine(ap.get().getWorkplane(), workplaneOffset));

		scaleSession.updateOrientation(cameraFrame);
	}

	// Use the received mouse position to calculate the world position and send it
	// back
	public Point3D sendNewWorldPosition(double screenX, double screenY, double snapGridValue) {

		// Convert to subscene coordinates to remove offsets
		Point2D overlayCoords = overlayPane.sceneToLocal(new Point2D(screenX, screenY));

		this.startingPosition3D = upArrow.getStartingPoint3D();

		Point3D wp3d;
		double foundZ = engine.sceneToWorldFixedXY_WP(overlayCoords, startingPosition3D.getX(),
				startingPosition3D.getY());

		return new Point3D(startingPosition3D.getX(), startingPosition3D.getY(), manipulation.snapToGrid(foundZ));
	}

	public ControlSprites(SelectionSession session, BowlerStudio3dEngine e, Affine sel, Manipulation m,
			ActiveProject ap, RulerManager ruler, WorkplaneManager workplane) {

		this.session = session;
		this.ruler = ruler;

		if (e == null)
			throw new NullPointerException();

		this.engine = e;
		this.overlayPane = e.getOverlayPane();
		this.selection = sel;
		this.manipulation = m;
		this.ap = ap;

		// Prevent Z-fighting of footprint and work plane
		footprint.setDepthTest(DepthTest.DISABLE);

		// this.xyMove = mov;
		ap.addListener(new ICaDoodleStateUpdate() {
			@Override
			public void onUpdate(List<CSG> currentState, CaDoodleOperation source, CaDoodleFile file) {
			}

			@Override
			public void onSaveSuggestion() {
			}

			@Override
			public void onInitializationDone() {
				currentOp = ap.get().getCurrentOperation();
			}

			@Override
			public void onWorkplaneChange(TransformNR newWP) {
			}

			@Override
			public void onInitializationStart() {
			}

			@Override
			public void onRegenerateDone() {
			}

			@Override
			public void onRegenerateStart(CaDoodleOperation source) {
			}

			@Override
			public void onTimelineUpdate(int num, WritableImage image) {
				// TODO Auto-generated method stub
			}
		});

		manipulation.addEventListener(ev -> {
			xymoving = true;
			zmoving = false;
			upArrow.resetSelected();
			scaleSession.resetSelected();
			updateLines();
		});

		manipulation.addSaveListener(() -> {
			xymoving = false;
		});

		zMoveOffsetFootprint = new Affine();
		zMoveManipulator = new Manipulation(selection, new Vector3d(0, 0, 1), new TransformNR(),
				this::sendNewWorldPosition, true, false);
		zMoveManipulator.setFrameOfReference(() -> ap.get().getWorkplane());

		zMoveManipulator.addSaveListener(() -> {
			TransformNR globalPose = zMoveManipulator.getGlobalPoseInReferenceFrame();
			com.neuronrobotics.sdk.common.Log.error("Z Moved! " + globalPose.toSimpleString());
			Thread t;
			try {
				t = ap.addOp(
						new MoveCenter().setLocation(globalPose.copy()).setNames(session.selectedSnapshot(), ap.get()));
				try {
					t.join();
				} catch (InterruptedException exx) {
					// Auto-generated catch block
					com.neuronrobotics.sdk.common.Log.error(exx);
				}
			} catch (InvalidLocationMove e1) {
				Log.error(e1);
			}
			BowlerKernel.runLater(() -> {
				TransformFactory.nrToAffine(new TransformNR(), zMoveOffsetFootprint);
			});
			zmoving = false;
			setMode(SpriteDisplayMode.Default);
			updateLines();
		});

		zMoveManipulator.addEventListener(ev -> {
			zmoving = true;
			xymoving = false;
			if (getMode() != SpriteDisplayMode.Align)
				setMode(SpriteDisplayMode.MoveZ);
			TransformNR globalPose = zMoveManipulator.getCurrentPose();
			TransformNR wp = new TransformNR(ap.get().getWorkplane().getRotation());
			globalPose = wp.times(globalPose);
			globalPose.setRotation(new RotationNR());
			TransformNR inverse = globalPose.inverse();
			BowlerKernel.runLater(() -> TransformFactory.nrToAffine(inverse.translateZ(0.01), zMoveOffsetFootprint));
			updateLines();
		});

		upArrow = new MoveUpArrow(selection, workplaneOffset, moveUpLocation, scaleTF, zMoveManipulator,
				() -> updateLinesAndCubes(), // onSelect
				() -> scaleSession.resetSelected()); // onReset

		lines = Arrays.asList(frontLine, backLine, leftLine, rightLine, heightLine);
		for (DottedLine l : lines) {
			l.getTransforms().add(selection);
			l.setMouseTransparent(true);
		}

		footprint.getTransforms().add(zMoveOffsetFootprint);
		footprint.getTransforms().add(selection);
		footprint.getTransforms().add(workplaneOffset);
		footprint.setMouseTransparent(true);

		Runnable updateLines = () -> {
			updateLines();
		};

		scaleSession = new ResizeSessionManager(e, selection, updateLines, ap, session, workplaneOffset, upArrow, this,
				workplane);
		List<Node> tmp = Arrays.asList(footprint, frontLine, backLine, leftLine, rightLine, heightLine,
				upArrow.getMesh());

		allElems.addAll(tmp);
		allElems.addAll(scaleSession.getMeshes());

		setUpOperationManagers(session, ap, ruler);
		allElems.addAll(align.getElements());
		allElems.addAll(mirror.getElements());
		allElems.addAll(rotationManager.getElements());

		Runnable dimChange = () -> {
			if (xdimen.canceled || ydimen.canceled || zdimen.canceled) {
				xdimen.hide();
				ydimen.hide();
				zdimen.hide();
				return;
			}

			com.neuronrobotics.sdk.common.Log.error("Typed position update");
			BowlerKernel.runLater(() -> {
				try {
					scaleSession.set(xdimen.getMostRecentValue(), ydimen.getMostRecentValue(),
							zdimen.getMostRecentValue());
				} catch (NowFXThreadException e1) {
					Log.error(e1);
				}
			});;
			updateLinesAndCubes();
		};

		Runnable offsetxyChange = () -> {
			if (xOffset.canceled || yOffset.canceled) {
				xOffset.hide();
				yOffset.hide();
				return;
			}

			this.bounds = scaleSession.getBounds();
			Vector3d min = bounds.getMin();
			double xOff = xOffset.getMostRecentValue() - min.x;
			double yOff = yOffset.getMostRecentValue() - min.y;
			// com.neuronrobotics.sdk.common.Log.error("Typed XY offset update x=" + xOff +
			// y=" + yOff);
			manipulation.set(xOff, yOff, 0);
			manipulation.fireSave();
			updateLinesAndCubes();
		};

		Runnable offsetZChange = () -> {
			if (zOffset.canceled)
				return;

			this.bounds = scaleSession.getBounds();
			Vector3d min = bounds.getMin();
			double manipDiff = zOffset.getMostRecentValue() - min.z;

			// com.neuronrobotics.sdk.common.Log.error("Typed Z offset ud "+manipDiff);
			zMoveManipulator.set(0, 0, manipDiff);
			zMoveManipulator.fireSave();
			updateLinesAndCubes();
		};

		xdimen = new ThreedNumber(selection, workplaneOffset, dimChange, TextFieldDimension.None, ruler, 4,
				new Vector3d(1, 0, 0));
		ydimen = new ThreedNumber(selection, workplaneOffset, dimChange, TextFieldDimension.None, ruler, 4,
				new Vector3d(0, 1, 0));
		zdimen = new ThreedNumber(selection, workplaneOffset, dimChange, TextFieldDimension.None, ruler, 4,
				new Vector3d(0, 0, 1));
		xOffset = new ThreedNumber(selection, workplaneOffset, offsetxyChange, TextFieldDimension.X, ruler, 4,
				new Vector3d(1, 0, 0));
		yOffset = new ThreedNumber(selection, workplaneOffset, offsetxyChange, TextFieldDimension.Y, ruler, 4,
				new Vector3d(0, 1, 0));
		zOffset = new ThreedNumber(selection, workplaneOffset, offsetZChange, TextFieldDimension.Z, ruler, 4,
				new Vector3d(0, 0, 1));
		numbers = Arrays.asList(xdimen, ydimen, zdimen, xOffset, yOffset, zOffset);

		for (ThreedNumber t : numbers)
			allElems.addAll(t.getTextField());

		clearSelection();
		setUpUIComponents();
	}

	void resetManipulator(Bounds b2) {
		zMoveManipulator.set(0, 0, 0);
		// zMoveManipulator.reset();
		BowlerKernel.runLater(() -> {
			TransformFactory.nrToAffine(new TransformNR(), zMoveOffsetFootprint);
		});
	}

	private void updateLinesAndCubes() {
		List<CSG> cur = session.getCurrentStateSelected();
		BowlerKernel.runLater(() -> {
			updateCubes(session.getCurrentStateSelected(), cur);
			updateLines();
		});

	}

	private void setUpOperationManagers(SelectionSession session, ActiveProject ap, RulerManager ruler) {
		rotationManager = new RotationSessionManager(selection, ap, session, workplaneOffset, ruler, (tf) -> {
			try {
				ap.addOp(new MoveCenter().setLocation(tf).setNames(session.selectedSnapshot(), ap.get()));
			} catch (InvalidLocationMove e) {
				return;
			}
		});

		align = new AlignManager(session, selection, workplaneOffset, ap);
		mirror = new MirrorSessionManager(selection, ap, this, workplaneOffset);
	}

	public void clearAlign(HashMap<String, Bounds> cache) {
		align.clear(cache);
	}

	private void setUpUIComponents() {
		Group linesGroupp = new Group();
		// Draw line in front of other objects
		linesGroupp.setDepthTest(DepthTest.DISABLE);
		linesGroupp.setViewOrder(-1); // Lower viewOrder renders on top
		Group controlsGroup = new Group();

		// Draw controls in front of other objects
		controlsGroup.setDepthTest(DepthTest.DISABLE);
		controlsGroup.setViewOrder(-2); // Lower viewOrder renders on top

		BowlerStudio.runLater(() -> {
			engine.addUserNode(footprint);
			for (Node r : allElems) {
				if (MeshView.class.isInstance(r))
					((MeshView) r).setCullFace(CullFace.BACK);

				if (r == footprint)
					continue;

				if (DottedLine.class.isInstance(r))
					linesGroupp.getChildren().add(r);
				else
					controlsGroup.getChildren().add(r);
			}
			// engine.addUserNode(linesGroupp); // Commented out, use control node below
			// engine.addUserNode(controlsGroup); // Commented out, use control node below
			engine.addControlNode(linesGroupp);
			engine.addControlNode(controlsGroup);
		});
	}

	// Selection of object informs about the object height
	public void setObjectHeight(double objectHeight) {
		this.objectHeight = objectHeight;
		zMoveManipulator.setObjectHeight(objectHeight);
	}

	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z, List<String> selectedCSG, Bounds b, HashMap<String, Bounds> inWorkplaneBounds,
			double cameraFovDegrees) {

		this.b = b;
		this.cameraFovDegrees = cameraFovDegrees;
		if (!selectionLive) {
			// TickToc.tic("!selectionLive");
			selectionLive = true;
			BowlerStudio.runLater(() -> {
				initialize();
				for (ThreedNumber t : numbers)
					t.hide();
			});
		}
		if ((el < -90) || (el > 90)) {
			// TickToc.tic("footprint.setVisible(false)");
			footprint.setVisible(false);
		} else {
			// TickToc.tic("footprint.setVisible(true)");
			footprint.setVisible(true);
		}

		this.screenW = screenW;
		this.screenH = screenH;
		this.zoom = zoom;
		this.az = az;
		this.el = el;
		this.x = x;
		this.y = y;
		this.z = z;
		// TickToc.tic("cam up");
		cf = engine.getFlyingCamera().getCamerFrame().times(new TransformNR(0, 0, zoom));
		// TickToc.tic("rot update");
		updateOperationsManagers(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b, inWorkplaneBounds);
		updateLinesAndCubes();

		if (session.isLocked() || session.isInOperationMode()) {
			upArrow.hide();
			rotationManager.hide();
			if (session.isAlignActive())
				scaleSession.show();
			else
				scaleSession.hide();
		} else {
			if (!session.moveLock()) {
				upArrow.show();
				rotationManager.show(session.moveLock());
			} else {
				upArrow.hide();
				rotationManager.hide();
			}
			// scaleSession.show();
		}

		// TickToc.toc();
		// TickToc.setEnabled(false);
	}

	private void updateOperationsManagers(double screenW, double screenH, double zoom, double az, double el, double x,
			double y, double z, List<String> selectedCSG, Bounds b, HashMap<String, Bounds> inWorkplaneBounds) {
		rotationManager.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b, cf, engine.getFov());
		mirror.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b, cf);
		// TickToc.tic("aligned update");
		align.threeDTarget(screenW, screenH, zoom, b, cf, inWorkplaneBounds);
	}

	public void initializeAlign(List<CSG> toAlign, List<String> boundNames, HashMap<CSG, MeshHolder> meshes,
			HashMap<String, Bounds> inWorkplaneBounds) {
		if (toAlign.size() > 1)
			align.initialize(boundNames, engine, toAlign, session.selectedSnapshot(), meshes, inWorkplaneBounds);

	}

	public void initializeMirror(List<CSG> toAlign, Bounds b, HashMap<CSG, MeshHolder> meshes) {
		mirror.initialize(b, engine, toAlign, session.selectedSnapshot(), meshes);
	}

	public void cancelOperationMode() {
		align.cancel();
		mirror.cancel();
	}

	private void initialize() {

		// Initialize as hidden to prevent visual glitches
		for (Node r : allElems)
			r.setVisible(false);

		rotationManager.initialize(session.moveLock());
		mirror.hide();
		align.hide();
	}

	private void resetSelected() {
		scaleSession.resetSelected();
		upArrow.resetSelected();
		rotationManager.resetSelected();
	}

	public boolean isFocused() {

		for (ThreedNumber t : numbers)
			if (t.isFocused())
				return true;

		return rotationManager.isFocused();
	}

	public void updateLines() {

		if (session.getSelected().size() == 0) {
			Log.debug("Nothing selected, no line updates");
			return;
		}
		BowlerStudio.runLater(() -> {
			TransformFactory.nrToAffine(ap.get().getWorkplane(), workplaneOffset);
			if (session.getSelected().size() == 0) {
				runClear();
				return;
			}
			this.bounds = scaleSession.getBounds();
			Vector3d center = bounds.getCenter();
			Vector3d min = bounds.getMin();
			Vector3d max = bounds.getMax();

			// Don't draw anything for zero size objects
			if ((max.x - min.x) < 0.0001) {
				Log.debug(" Don't draw anything for zero size objects");
				return;
			}

			// Set footprint of shape
			footprint.setHeight(Math.abs(max.y - min.y));
			footprint.setWidth(Math.abs(max.x - min.x));
			footprint.setX(Math.min(min.x, max.x));
			footprint.setY(Math.min(min.y, max.y));

			// double lineScale = 2 * (-zoom / 1000);
			// double lineEndOffsetY = 0; // Math.min(5 * lineScale, max.y - min.y);
			// double lineEndOffsetX = 0; // Math.min(5 * lineScale, max.x - min.x);
			// double lineEndOffsetZ = 0; // Math.min(5, max.z - min.z);

			// Draw lines closest to work plane
			double linesZ = 0;
			if (min.z > 0) // Object is above the work plane
				linesZ = min.z;

			if (max.z < 0) // Object is below the work plane
				linesZ = max.z;

			// Draw dotted lines between handles
			frontLine.setPoints(max.x, min.y, linesZ, max.x, max.y, linesZ);
			frontLine.setVisible(true);

			backLine.setPoints(min.x, min.y, linesZ, min.x, max.y, linesZ);
			backLine.setVisible(true);

			leftLine.setPoints(min.x, max.y, linesZ, max.x, max.y, linesZ);
			leftLine.setVisible(true);

			rightLine.setPoints(min.x, min.y, linesZ, max.x, min.y, linesZ);
			rightLine.setVisible(true);

			// Draw Z-handle dotted line
			heightLine.setPoints(center.x, center.y, min.z, center.x, center.y, max.z);
			heightLine.setVisible(true);

			// Distance between handle and label
			double numberOffset = -zoom / 50;

			// Get view scale of 3D shapes (arrow/cone/dotted line)
			double viewScale = scaleSession.getViewScale();

			// Scale factor for Z-handle arrow
			double arrowScale = viewScale;
			// if (arrowScale > 0.3)
			// arrowScale = arrowScale - (arrowScale - 0.3) / 2;

			scaleTF.setX(arrowScale);
			scaleTF.setY(arrowScale);
			scaleTF.setZ(arrowScale);

			// Scale factor for dotted line
			double dottedLineScale = viewScale * 5;

			if (dottedLineScale > 0.3)
				dottedLineScale = 0.3 + (dottedLineScale - 0.3) / 5;
			// if (dottedLineScale < 0.1)
			// dottedLineScale = 0.1;

			for (DottedLine l : lines)
				l.setScale(dottedLineScale);

			// Draw Z-offset handle with arrow/cone
			double arrowDistance = 5;
			TransformNR zHandleLoc = new TransformNR(center.x, center.y,
					arrowDistance + max.z + (ResizingHandle.getSize() * viewScale));
			TransformFactory.nrToAffine(zHandleLoc, moveUpLocation);

			// Position value labels
			xdimen.threeDTarget(
					screenW, screenH, zoom, new TransformNR(center.x,
							scaleSession.leftSelected() ? max.y + numberOffset : min.y - numberOffset, linesZ),
					cf, cameraFovDegrees);

			ydimen.threeDTarget(screenW, screenH, zoom,
					new TransformNR(scaleSession.frontSelected() ? max.x + numberOffset : min.x - numberOffset,
							center.y, linesZ),
					cf, cameraFovDegrees);

			zdimen.threeDTarget(screenW, screenH, zoom,
					new TransformNR(center.x, center.y, (max.z - min.z) / 2 + min.z), cf, cameraFovDegrees);

			xOffset.threeDTarget(screenW, screenH, zoom,
					new TransformNR((min.x / 2.0) + xOffset.getMyOffset() / 2, min.y, linesZ), cf, cameraFovDegrees);

			yOffset.threeDTarget(screenW, screenH, zoom,
					new TransformNR(min.x, (min.y / 2) + yOffset.getMyOffset() / 2, linesZ), cf, cameraFovDegrees);

			zOffset.threeDTarget(screenW, screenH, zoom,
					new TransformNR(center.x, center.y, (min.z / 2) + zOffset.getMyOffset() / 2), cf, cameraFovDegrees);
			xdimen.setValue(bounds.getTotalX());
			ydimen.setValue(bounds.getTotalY());
			zdimen.setValue(bounds.getTotalZ());

			TransformNR pose = manipulation.getCurrentPoseInReferenceFrame();
			xOffset.setValue(min.x + pose.getX());
			yOffset.setValue(min.y + pose.getY());

			zOffset.setValue(min.z + zMoveManipulator.getCurrentPose().getZ());
			if (mode == SpriteDisplayMode.Resize) {
				if (scaleSession.zScaleSelected())
					zdimen.show();
				else
					zdimen.hide();
				if (scaleSession.rightSelected() || scaleSession.leftSelected())
					ydimen.show();
				else
					ydimen.hide();
				if (scaleSession.frontSelected() || scaleSession.rearSelected())
					xdimen.show();
				else
					xdimen.hide();
			}
			boolean isDefault = mode == SpriteDisplayMode.Default;
			if (isDefault) {
				if (scaleSession.zScaleSelected())
					zdimen.show();
				else
					zdimen.hide();
				if (scaleSession.rightSelected() || scaleSession.leftSelected())
					ydimen.show();
				else
					ydimen.hide();
				if (scaleSession.frontSelected() || scaleSession.rearSelected())
					xdimen.show();
				else
					xdimen.hide();
			}

			CaDoodleOperation currentOperation = ap.get().getCurrentOperation();
			boolean isMoveZ = mode == SpriteDisplayMode.MoveZ;
			boolean isThisADisplayMode = isMoveZ || (mode == SpriteDisplayMode.MoveXY)
					|| (isDefault && MoveCenter.class.isInstance(currentOperation) && (currentOp != currentOperation));

			if (!ruler.isActive()) {
				boolean selected = upArrow.isSelected();
				boolean isZMove = selected && (isDefault || isMoveZ);
				if (isZMove)
					zOffset.show();
				else
					zOffset.hide();
				if (isThisADisplayMode && !scaleSession.xySelected() && !scaleSession.zScaleSelected() && !isZMove) {
					xOffset.show();
					yOffset.show();
				} else {
					xOffset.hide();
					yOffset.hide();
					// zOffset.hide(); // Show the height when clicking the top center arrow
				}
			} else {
				if (session.selectedSnapshot().size() > 0) {
					zOffset.show();
					xOffset.show();
					yOffset.show();
				}
			}
			TransformFactory.nrToAffine(new TransformNR(RotationNR.getRotationZ(90 - az)), spriteFace);

		});

	}

	public Affine getViewRotation() {
		return rotationManager.getViewRotation();
	}

	private void updateCubes(List<CSG> selectedCSG, List<CSG> currentState) {
		boolean lockSize = false;
		boolean moveLock = session.moveLock();
		for (CSG sel : currentState)
			if (sel.isNoScale())
				lockSize = true;

		zMoveManipulator.setUnlocked(!moveLock);
		scaleSession.setResizeAllowed(!lockSize, moveLock);
		rotationManager.setLock(moveLock);
		scaleSession.threeDTarget(screenW, screenH, zoom, b, cf, session.isLocked());
		List<String> selectedSnapshot = session.selectedSnapshot();
		if (selectedCSG.size() == 0)
			return;

		TransformNR cf = engine.getFlyingCamera().getCamerFrame().times(new TransformNR(0, 0, zoom));
		session.getLimbs().threeDTarget(screenW, screenH, zoom, cf, session.isLocked(), engine.getFov());
	}

	public void clearSelection() {

		BowlerStudio.runLater(() -> {
			for (Node r : allElems)
				r.setVisible(false);
		});

		selectionLive = false;
		resetSelected();
		try {
			currentOp = ap.get().getCurrentOperation();
		} catch (RuntimeException ex) {
			// ignore during loading before the AP initialized
		}
	}

	public SpriteDisplayMode getMode() {
		return mode;
	}

	public void setMode(SpriteDisplayMode mode) {
		// new Exception("Mode Set to " + mode).printStackTrace();

		if (mode == this.mode)
			return;
		Log.debug("ControlSprites setMode: " + mode);

		this.mode = mode;

		// if (mode == SpriteDisplayMode.MoveZ)
		// {
		// Point3D startingPos = new Point3D(0, 0, zOffset.getMostRecentValue());
		// zMove.setStartingWorkplanePosition(startingPos);
		// }
		BowlerStudio.runLater(() -> {

			for (Node r : allElems)
				r.setVisible(mode == SpriteDisplayMode.Default && session.getSelected().size() > 0);

			switch (this.mode) {

				case Default :
					initialize();
					for (ThreedNumber t : numbers)
						t.hide();

					align.hide();
					mirror.hide();
					if (ruler.isActive()) {
						xOffset.show();
						yOffset.show();
						zOffset.show();
					}
					return;

				case MoveXY :
					for (DottedLine l : lines) {
						l.setVisible(true);
					}
					if (ruler.isActive()) {
						xOffset.show();
						yOffset.show();
					}
					session.hideHalos();
					break;

				case MoveZ :
					for (DottedLine l : lines) {
						l.setVisible(true);
					}
					upArrow.show();
					footprint.setVisible(true);
					zOffset.show();
					session.hideHalos();
					break;

				case Resize :
					for (DottedLine l : lines) {
						l.setVisible(true);
					}
					align.hide();
					mirror.hide();
					scaleSession.show();
					session.hideHalos();
					break;
				case Rotating :
					session.hideHalos();
					break;
				case Align :
					for (DottedLine l : lines)
						l.setVisible(true);

					break;
				case Mirror :
					for (DottedLine l : lines)
						l.setVisible(true);

					rotationManager.hide();
					break;
				case PLACING :
					if (session.getSelected().size() > 0) {
						for (DottedLine l : lines)
							l.setVisible(true);
						scaleSession.show();
					}
					session.hideHalos();
					break;
				case Clear :
					runClear();
					break;
			}

			//			if ((mode != SpriteDisplayMode.Clear) && (mode != SpriteDisplayMode.PLACING))
			//				updateLines();
		});
	}

	private void runClear() {
		for (ThreedNumber t : numbers)
			t.hide();

		align.hide();
		mirror.hide();
		for (DottedLine l : lines)
			l.setVisible(false);

		upArrow.hide();
		footprint.setVisible(false);
		zOffset.hide();
		scaleSession.hide();
		session.hideHalos();
	}

	public boolean mirrorIsActive() {
		return mirror.isActive();
	}

	public boolean alignIsActive() {
		return align.isActive();
	}

	public SelectionSession getSession() {
		return session;
	}

	public void hideRotationHandles() {
		BowlerStudio.runLater(() -> rotationManager.hide());
	}

}
