package com.commonwealthrobotics.controls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.allign.AllignManager;
import com.commonwealthrobotics.numbers.ThreedNumber;
import com.commonwealthrobotics.rotate.RotationSessionManager;
import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;
import com.neuronrobotics.bowlerstudio.BowlerKernel;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleOpperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.MoveCenter;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.TickToc;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cylinder;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Line;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;

public class ControlSprites {
	private static final double SIZE_OF_DOT = 0.5;
	private static final double NUMBER_OF_MM_PER_DOT = 2;
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
	private ScaleSessionManager scaleSession;
	private RotationSessionManager rotationManager;
	private AllignManager allign;
	private Rectangle footprint = new Rectangle(100, 100, new Color(0, 0, 1, 0.25));
	// private Rectangle bottomDimentions = new Rectangle(100,100,new
	// Color(0,0,1,0.25));
	private Affine workplaneOffset = new Affine();
	private MoveUpArrow up;
	private DottedLine frontLine = new DottedLine(SIZE_OF_DOT, NUMBER_OF_MM_PER_DOT, workplaneOffset);
	private DottedLine backLine = new DottedLine(SIZE_OF_DOT, NUMBER_OF_MM_PER_DOT, workplaneOffset);
	private DottedLine leftLine = new DottedLine(SIZE_OF_DOT, NUMBER_OF_MM_PER_DOT, workplaneOffset);
	private DottedLine rightLine = new DottedLine(SIZE_OF_DOT, NUMBER_OF_MM_PER_DOT, workplaneOffset);
	private DottedLine heightLine = new DottedLine(SIZE_OF_DOT, NUMBER_OF_MM_PER_DOT, workplaneOffset);

	private ArrayList<Node> allElems = new ArrayList<Node>();
	private boolean selectionLive = false;
	private Bounds bounds;
	private List<DottedLine> lines;
	private Affine spriteFace = new Affine();
	private Affine moveUpLocation = new Affine();
	private Scale scaleTF = new Scale();
	private Affine selection;
	private Manipulation zMove;
	//private CaDoodleFile cadoodle;
	private Bounds b;
	private SpriteDisplayMode mode = SpriteDisplayMode.Default;
	private TransformNR cf;
	private ThreedNumber xdimen;
	private ThreedNumber ydimen;
	private ThreedNumber zdimen;
	private ThreedNumber xOffset;
	private ThreedNumber yOffset;
	private ThreedNumber zOffset;
	private List<ThreedNumber> numbers;
	private Manipulation manipulation;
	private boolean xymoving = false;
	private boolean zmoving = false;
	private ICaDoodleOpperation currentOp;
	private ActiveProject ap;

	public void setSnapGrid(double size) {
		zMove.setIncrement(size);
		scaleSession.setSnapGrid(size);
	}

	public ControlSprites(SelectionSession session, BowlerStudio3dEngine e, Affine sel, Manipulation manipulation,
			ActiveProject ap) {
		this.session = session;
		if(e==null)
			throw new NullPointerException();
		this.engine = e;
		this.selection = sel;
		this.manipulation = manipulation;
		this.ap = ap;
		// this.xyMove = mov;
		ap.addListener(new ICaDoodleStateUpdate() {
			@Override
			public void onUpdate(List<CSG> currentState, ICaDoodleOpperation source, CaDoodleFile file) {}			
			@Override
			public void onSaveSuggestion() {}			
			@Override
			public void onInitializationDone() {
				currentOp = ap.get().getCurrentOpperation();
			}
		});
		
		manipulation.addEventListener(() -> {
			xymoving = true;
			zmoving = false;
			up.resetSelected();
			scaleSession.resetSelected();
			updateLines();
		});
		manipulation.addSaveListener(() -> {
			xymoving = false;
		});
		Affine zMoveOffsetFootprint = new Affine();
		zMove = new Manipulation(selection, new Vector3d(0, 0, 1), new TransformNR());
		zMove.setFrameOfReference(() -> ap.get().getWorkplane());
		zMove.addSaveListener(() -> {
			TransformNR globalPose = zMove.getGlobalPoseInReferenceFrame();
			System.out.println("Z Moved! " + globalPose.toSimpleString());
			Thread t = ap.addOp(
					new MoveCenter().setLocation(globalPose.copy()).setNames(session.selectedSnapshot()));
			try {
				t.join();
			} catch (InterruptedException exx) {
				// TODO Auto-generated catch block
				exx.printStackTrace();
			}
			zMove.set(0, 0, 0);
			BowlerKernel.runLater(() -> {
				TransformFactory.nrToAffine(new TransformNR(), zMoveOffsetFootprint);
			});
			zmoving = false;
			setMode(SpriteDisplayMode.Default);
			updateLines();
		});
		zMove.addEventListener(() -> {
			zmoving = true;
			xymoving = false;
			setMode(SpriteDisplayMode.MoveZ);
			TransformNR globalPose = zMove.getCurrentPose();
			TransformNR wp = new TransformNR(ap.get().getWorkplane().getRotation());
			globalPose = wp.times(globalPose);
			globalPose.setRotation(new RotationNR());
			TransformNR inverse = globalPose.inverse();
			BowlerKernel.runLater(() -> TransformFactory.nrToAffine(inverse.translateZ(0.1), zMoveOffsetFootprint));
			updateLines();
		});

		up = new MoveUpArrow(selection, workplaneOffset, moveUpLocation, scaleTF, zMove.getMouseEvents(), () -> {
			updateCubes();
			updateLines();
		}, () -> scaleSession.resetSelected());
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
			// System.out.println("Lines updated from scale session");
		};
		scaleSession = new ScaleSessionManager(e, selection, updateLines, ap, session, workplaneOffset, up);
		List<Node> tmp = Arrays.asList(scaleSession.topCenter.getMesh(), scaleSession.rightFront.getMesh(),
				scaleSession.rightRear.getMesh(), scaleSession.leftFront.getMesh(), scaleSession.leftRear.getMesh(),
				footprint, frontLine, backLine, leftLine, rightLine, heightLine, up.getMesh());
		allElems.addAll(tmp);

		rotationManager = new RotationSessionManager(selection, ap, this, workplaneOffset);
		allign = new AllignManager(session, selection, workplaneOffset);
		allElems.addAll(allign.getElements());
		allElems.addAll(rotationManager.getElements());
		Runnable dimChange = () -> {
			//System.out.println("Typed position update");
			scaleSession.set(xdimen.getMostRecentValue(),ydimen.getMostRecentValue(),zdimen.getMostRecentValue());
			updateCubes();
			updateLines();
		};
		Runnable offsetxyChange = () -> {

			this.bounds = scaleSession.getBounds();
			Vector3d min = bounds.getMin();
			double xOff = xOffset.getMostRecentValue() - min.x ;
			double yOff = yOffset.getMostRecentValue() - min.y ;
			//System.out.println("Typed XY offset update x="+ xOff+" y="+yOff);
			manipulation.set(xOff, yOff, 0);
			manipulation.fireSave();
			updateCubes();
			updateLines();
		};
		Runnable offsetZChange = () -> {
			this.bounds = scaleSession.getBounds();
			Vector3d min = bounds.getMin();
			double manipDiff = zOffset.getMostRecentValue() -min.z; 
			//System.out.println("Typed Z offset ud "+manipDiff);
			zMove.set(0, 0, manipDiff);
			zMove.fireSave();
			updateCubes();
			updateLines();
		};
		xdimen = new ThreedNumber( selection, workplaneOffset, dimChange);
		ydimen = new ThreedNumber( selection, workplaneOffset, dimChange);
		zdimen = new ThreedNumber( selection, workplaneOffset, dimChange);
		xOffset = new ThreedNumber( selection, workplaneOffset, offsetxyChange);
		yOffset = new ThreedNumber( selection, workplaneOffset, offsetxyChange);
		zOffset = new ThreedNumber( selection, workplaneOffset, offsetZChange);
		numbers = Arrays.asList(xdimen, ydimen, zdimen, xOffset, yOffset, zOffset);
		for (ThreedNumber t : numbers)
			allElems.add(t.get());

		clearSelection();
		setUpUIComponennts();

	}

	private void setUpUIComponennts() {
		Group linesGroupp = new Group();
		linesGroupp.setDepthTest(DepthTest.DISABLE);
		linesGroupp.setViewOrder(-1); // Lower viewOrder renders on top
		Group controlsGroup = new Group();
		controlsGroup.setDepthTest(DepthTest.DISABLE);
		controlsGroup.setViewOrder(-2); // Lower viewOrder renders on top

		BowlerStudio.runLater(() -> {
			engine.addUserNode(footprint);
			for (Node r : allElems) {
				if (MeshView.class.isInstance(r)) {
					((MeshView) r).setCullFace(CullFace.BACK);
				}
				if (r == footprint)
					continue;
				if (DottedLine.class.isInstance(r))
					linesGroupp.getChildren().add(r);
				else
					controlsGroup.getChildren().add(r);
			}
			engine.addUserNode(linesGroupp);
			engine.addUserNode(controlsGroup);
		});
	}

	public void updateControls(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z, List<String> selectedCSG, Bounds b) {

		this.b = b;
		if (!selectionLive) {
			selectionLive = true;
			BowlerStudio.runLater(() -> {
				initialize();
			});
		}
		if (el < -90 || el > 90) {
			footprint.setVisible(false);
		} else {
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
		TickToc.tic("cam up");
		cf = engine.getFlyingCamera().getCamerFrame().times(new TransformNR(0, 0, zoom));
		TickToc.tic("rot update");
		rotationManager.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b,cf);
		TickToc.tic("alligned update");
		allign.threeDTarget(screenW, screenH, zoom, b, cf);
		TickToc.tic("cubes update");
		updateCubes();
		TickToc.tic("lines update");
		updateLines();
	}

	public void initializeAllign(List<CSG> toAllign, Bounds b, HashMap<CSG, MeshView> meshes) {
		allign.initialize(b, engine, toAllign, session.selectedSnapshot(), meshes);
	}

	public void cancleAllign() {
		allign.cancel();
	}

	private void initialize() {
		for (Node r : allElems)
			r.setVisible(true);
		rotationManager.initialize();
		allign.hide();
		for (ThreedNumber t : numbers) {
			t.hide();
		}
	}

	private void resetSelected() {
		scaleSession.resetSelected();
		up.resetSelected();
		rotationManager.resetSelected();
	}

	public boolean isFocused() {
		for (ThreedNumber t : numbers) {
			if (t.isFocused())
				return true;
		}
		return false;
	}

	private void updateLines() {
		BowlerStudio.runLater(() -> {
			TransformFactory.nrToAffine(ap.get().getWorkplane(), workplaneOffset);
			this.bounds = scaleSession.getBounds();
			Vector3d center = bounds.getCenter();
			Vector3d min = bounds.getMin();
			Vector3d max = bounds.getMax();
			footprint.setHeight(Math.abs(max.y - min.y));
			footprint.setWidth(Math.abs(max.x - min.x));
			footprint.setX(Math.min(min.x, max.x));
			footprint.setY(Math.min(min.y, max.y));

			double lineScale = 2 * (-zoom / 1000);
			double lineEndOffsetY = 0;// Math.min(5 * lineScale, max.y - min.y);
			double lineEndOffsetX = 0;// Math.min(5 * lineScale, max.x - min.x);
			double lineEndOffsetZ = 0;// Math.min(5, max.z - min.z);
			frontLine.setStartX(max.x);
			frontLine.setStartY(min.y + lineEndOffsetY);
			frontLine.setEndX(max.x);
			frontLine.setEndY(max.y - lineEndOffsetY);
			frontLine.setStartZ(min.z);
			frontLine.setEndZ(min.z);

			backLine.setStartX(min.x);
			backLine.setStartY(min.y + lineEndOffsetY);
			backLine.setEndX(min.x);
			backLine.setEndY(max.y - lineEndOffsetY);
			backLine.setStartZ(min.z);
			backLine.setEndZ(min.z);

			leftLine.setStartX(min.x + lineEndOffsetX);
			leftLine.setStartY(max.y);
			leftLine.setEndX(max.x - lineEndOffsetX);
			leftLine.setEndY(max.y);
			leftLine.setStartZ(min.z);
			leftLine.setEndZ(min.z);

			rightLine.setStartX(min.x + lineEndOffsetX);
			rightLine.setStartY(min.y);
			rightLine.setEndX(max.x - lineEndOffsetX);
			rightLine.setEndY(min.y);
			rightLine.setStartZ(min.z);
			rightLine.setEndZ(min.z);

			heightLine.setStartX(center.x);
			heightLine.setStartY(center.y);
			heightLine.setEndY(center.y);
			heightLine.setEndX(center.x);
			heightLine.setStartZ(min.z);
			heightLine.setEndZ(max.z - lineEndOffsetZ);
			double numberOffset = 20;
			TransformNR zHandleLoc = new TransformNR(center.x, center.y,
					5 + max.z + (ResizingHandle.getSize() * scaleSession.getViewScale()));
			xdimen.threeDTarget(screenW, screenH, zoom, new TransformNR(center.x,
					scaleSession.leftSelected() ? max.y + numberOffset : min.y - numberOffset, min.z), cf);
			ydimen.threeDTarget(screenW, screenH, zoom, new TransformNR(
					scaleSession.frontSelected() ? max.x + numberOffset : min.x - numberOffset, center.y, min.z), cf);
			zdimen.threeDTarget(screenW, screenH, zoom, new TransformNR(center.x, center.y, center.z), cf);
			xOffset.threeDTarget(screenW, screenH, zoom, new TransformNR(min.x - numberOffset, min.y, min.z), cf);
			yOffset.threeDTarget(screenW, screenH, zoom, new TransformNR(min.x, min.y - numberOffset, min.z), cf);
			zOffset.threeDTarget(screenW, screenH, zoom,
					new TransformNR(max.x + numberOffset / 5, max.y + numberOffset / 5, min.z), cf);
			xdimen.setValue(bounds.getTotalX());
			ydimen.setValue(bounds.getTotalY());
			zdimen.setValue(bounds.getTotalZ());
			TransformNR pose = manipulation.getCurrentPoseInReferenceFrame();
			xOffset.setValue(min.x + pose.getX());
			yOffset.setValue(min.y + pose.getY());

			zOffset.setValue(min.z + zMove.getCurrentPoseInReferenceFrame().getZ());

			if (up.isSelected() && mode == SpriteDisplayMode.Default || mode == SpriteDisplayMode.MoveZ) {
				zOffset.show();
			} else {
				zOffset.hide();
			}
			if (scaleSession.zScaleSelected() && mode == SpriteDisplayMode.Default
					|| mode == SpriteDisplayMode.ResizeZ) {
				zdimen.show();
			} else {
				zdimen.hide();
			}
			if (scaleSession.xySelected() && mode == SpriteDisplayMode.Default) {
				xdimen.show();
				ydimen.show();
			} else {
				xdimen.hide();
				ydimen.hide();
			}

			ICaDoodleOpperation currentOpperation = ap.get().getCurrentOpperation();
			boolean isThisADisplayMode = mode == SpriteDisplayMode.MoveZ || mode == SpriteDisplayMode.MoveXY
							|| (mode == SpriteDisplayMode.Default
									&& MoveCenter.class.isInstance(currentOpperation) && currentOp!=currentOpperation);
			if ( isThisADisplayMode && (!scaleSession.xySelected() && !scaleSession.zScaleSelected())) {
				if (!xymoving)
					zOffset.show();
				if (!zmoving) {
					xOffset.show();
					yOffset.show();
				}else {
					//System.out.println("Z is moving");
				}
			} else {
				xOffset.hide();
				yOffset.hide();
				zOffset.hide();
			}
			TransformFactory.nrToAffine(new TransformNR(RotationNR.getRotationZ(90 - az)), spriteFace);

			TransformFactory.nrToAffine(zHandleLoc, moveUpLocation);

			scaleTF.setX(scaleSession.getViewScale());
			scaleTF.setY(scaleSession.getViewScale());
			scaleTF.setZ(scaleSession.getViewScale());
		});

	}

	public Affine getViewRotation() {
		return rotationManager.getViewRotation();
	}

	private void updateCubes() {
		scaleSession.threeDTarget(screenW, screenH, zoom, b, cf);
	}

	public void clearSelection() {
		BowlerStudio.runLater(() -> {
			for (Node r : allElems)
				r.setVisible(false);
		});
		selectionLive = false;
		resetSelected();
		if(ap.get()!=null)
			currentOp = ap.get().getCurrentOpperation();
	}

	public SpriteDisplayMode getMode() {
		return mode;
	}

	public void setMode(SpriteDisplayMode mode) {
		if (mode == this.mode)
			return;
		this.mode = mode;
		System.out.println("Mode Set to " + mode);
		// new Exception().printStackTrace();
		BowlerStudio.runLater(() -> {
			for (Node r : allElems)
				r.setVisible(mode == SpriteDisplayMode.Default);

			switch (this.mode) {
			case Default:
				initialize();
				allign.hide();
				return;
			case MoveXY:
				for (DottedLine l : lines) {
					l.setVisible(true);
				}
				xOffset.show();
				yOffset.show();
				break;
			case MoveZ:
				for (DottedLine l : lines) {
					l.setVisible(true);
				}
				up.show();
				footprint.setVisible(true);
				zOffset.show();
				break;
			case ResizeX:
				break;
			case ResizeXY:
				break;
			case ResizeY:
				break;
			case ResizeZ:
				break;
			case Rotating:
				break;
			case Allign:
				for (DottedLine l : lines) {
					l.setVisible(true);
				}
				break;
			case PLACING:
				for (DottedLine l : lines) {
					l.setVisible(true);
				}
				break;

			}
			updateLines();
		});
	}

	public boolean allignIsActive() {
		return allign.isActive();
	}
}
