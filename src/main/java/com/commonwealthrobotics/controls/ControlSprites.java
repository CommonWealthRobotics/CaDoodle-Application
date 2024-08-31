package com.commonwealthrobotics.controls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.commonwealthrobotics.allign.AllignManager;
import com.commonwealthrobotics.rotate.RotationSessionManager;
import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;
import com.neuronrobotics.bowlerstudio.BowlerKernel;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.MoveCenter;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

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

	private DottedLine frontLine = new DottedLine(SIZE_OF_DOT, NUMBER_OF_MM_PER_DOT,workplaneOffset);
	private DottedLine backLine = new DottedLine(SIZE_OF_DOT, NUMBER_OF_MM_PER_DOT,workplaneOffset);
	private DottedLine leftLine = new DottedLine(SIZE_OF_DOT, NUMBER_OF_MM_PER_DOT,workplaneOffset);
	private DottedLine rightLine = new DottedLine(SIZE_OF_DOT, NUMBER_OF_MM_PER_DOT,workplaneOffset);
	private DottedLine heightLine = new DottedLine(SIZE_OF_DOT, NUMBER_OF_MM_PER_DOT,workplaneOffset);

	private ArrayList<Node> allElems = new ArrayList<Node>();
	private boolean selectionLive = false;
	private Bounds bounds;
	private List<DottedLine> lines;
	private Affine spriteFace = new Affine();
	private MeshView moveUpArrow;
	private Affine moveUpLocation = new Affine();
	private Scale scaleTF = new Scale();
	private Affine selection;
	private Manipulation xyMove;
	private Manipulation zMove;
	private CaDoodleFile cadoodle;
	private double size;
	private Bounds b;
	private SpriteDisplayMode mode = SpriteDisplayMode.Default;
	private TransformNR cf;

	public void setSnapGrid(double size) {
		this.size = size;
		zMove.setIncrement(size);
		scaleSession.setSnapGrid(size);
	}

	public ControlSprites(SelectionSession session, BowlerStudio3dEngine e, Affine selection, Manipulation xyMove,
			CaDoodleFile c) {
		this.session = session;

		this.engine = e;
		this.selection = selection;
		this.xyMove = xyMove;
		this.cadoodle = c;
		Affine zMoveOffsetFootprint = new Affine();
		zMove = new Manipulation(selection, new Vector3d(0, 0, 1), new TransformNR());
		zMove.setFrameOfReference(()->cadoodle.getWorkplane());
		zMove.addSaveListener(() -> {
			TransformNR globalPose = zMove.getGlobalPoseInReferenceFrame();
			System.out.println("Objects Moved! " + globalPose.toSimpleString());
			Thread t = cadoodle.addOpperation(
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
			setMode(SpriteDisplayMode.Default);

		});
		zMove.addEventListener(() -> {
			setMode(SpriteDisplayMode.MoveZ);
			TransformNR globalPose = zMove.getCurrentPose();
			TransformNR wp = new TransformNR(cadoodle.getWorkplane().getRotation());
			globalPose=wp.times(globalPose);
			globalPose.setRotation(new RotationNR());
			TransformNR inverse = globalPose.inverse();
			BowlerKernel.runLater(() -> TransformFactory.nrToAffine(inverse.translateZ(0.1), zMoveOffsetFootprint));
		});

		CSG setColor = new Cylinder(ResizingHandle.getSize() / 2, 0, ResizingHandle.getSize()).toCSG()
				.setColor(Color.BLACK);
		moveUpArrow = setColor.getMesh();
		moveUpArrow.getTransforms().add(selection);
		moveUpArrow.getTransforms().add(workplaneOffset);
		moveUpArrow.getTransforms().add(moveUpLocation);
		moveUpArrow.getTransforms().add(scaleTF);
		moveUpArrow.addEventFilter(MouseEvent.ANY, zMove.getMouseEvents());
		PhongMaterial material = new PhongMaterial();
		material.setDiffuseColor(Color.GRAY);
		material.setSpecularColor(Color.WHITE);
		moveUpArrow.setMaterial(material);
		moveUpArrow.addEventFilter(MouseEvent.MOUSE_EXITED,event -> {
			material.setDiffuseColor(Color.GRAY);
		});
		moveUpArrow.addEventFilter(MouseEvent.MOUSE_ENTERED,event -> {
			material.setDiffuseColor(new Color(1,0,0,1));
		});
//		Affine heightLineOrentation = new Affine();
//		BowlerStudio.runLater(
//				() -> TransformFactory.nrToAffine(new TransformNR(RotationNR.getRotationY(-90)), heightLineOrentation));
		
		//heightLine.getTransforms().add(selection);
		//heightLine.getTransforms().add(spriteFace);


		lines = Arrays.asList(frontLine, backLine, leftLine, rightLine, heightLine);
		for (DottedLine l : lines) {
			//if (l != heightLine) {
				l.getTransforms().add(selection);
				//l.getTransforms().add(workplaneOffset);
				//
			//}
			// l.setFill(null);
			// l.setStrokeWidth(2);
			// l.setStrokeLineCap(StrokeLineCap.BUTT);
			// l.setStrokeLineJoin(StrokeLineJoin.MITER);
			// l.getStrokeDashArray().addAll(2.0,2.0);
			l.setMouseTransparent(true);
		}
		footprint.getTransforms().add(zMoveOffsetFootprint);
		footprint.getTransforms().add(selection);
		footprint.getTransforms().add(workplaneOffset);
		footprint.setMouseTransparent(true);
		scaleSession = new ScaleSessionManager(e, selection, () -> updateLines(), cadoodle, session,workplaneOffset);
		List<Node> tmp = Arrays.asList(scaleSession.topCenter.getMesh(), scaleSession.rightFront.getMesh(),
				scaleSession.rightRear.getMesh(), scaleSession.leftFront.getMesh(), scaleSession.leftRear.getMesh(),
				footprint, frontLine, backLine, leftLine, rightLine, heightLine, moveUpArrow);
		allElems.addAll(tmp);

		rotationManager = new RotationSessionManager(selection, cadoodle, this,workplaneOffset);
		allign=new AllignManager(session,selection, workplaneOffset);
		allElems.addAll(allign.getElements());
		allElems.addAll(rotationManager.getElements());

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
				if(DottedLine.class.isInstance(r))
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
		cf = engine.getFlyingCamera().getCamerFrame().times(new TransformNR(0,0,zoom));
		rotationManager.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedCSG, b);
		allign.threeDTarget(screenW, screenH, zoom, b, cf);
		updateCubes();
		updateLines();
	}
	public void initializeAllign(List<CSG> toAllign) {
		allign.initialize(engine,toAllign,session.selectedSnapshot());
	}
	public void cancleAllign() {
		allign.cancel();
	}
	private void initialize() {
		for (Node r : allElems)
			r.setVisible(true);
		rotationManager.initialize();
		allign.hide();
	}

	private void updateLines() {
		BowlerStudio.runLater(() -> {
			TransformFactory.nrToAffine(cadoodle.getWorkplane(), workplaneOffset);
			this.bounds = scaleSession.getBounds();
			Vector3d center = bounds.getCenter();
			Vector3d min = bounds.getMin();
			Vector3d max = bounds.getMax();
			footprint.setHeight(Math.abs(max.y - min.y));
			footprint.setWidth(Math.abs(max.x - min.x));
			footprint.setX(Math.min( min.x,max.x));
			footprint.setY(Math.min( min.y,max.y));

			double lineScale = 2 * (-zoom / 1000);
			double lineEndOffsetY = Math.min(5 * lineScale, max.y - min.y);
			double lineEndOffsetX = Math.min(5 * lineScale, max.x - min.x);
			double lineEndOffsetZ = Math.min(5, max.z - min.z);
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
			heightLine.setEndZ(max.z  - lineEndOffsetZ);
			// moveUpLocation

			TransformFactory.nrToAffine(new TransformNR(RotationNR.getRotationZ(90 - az)), spriteFace);
			TransformFactory.nrToAffine(new TransformNR(center.x, center.y, 5+max.z + (ResizingHandle.getSize()*scaleSession.getViewScale())),
					moveUpLocation);
//			for (DottedLine l : lines) {
//				// l.setStrokeWidth(1+lineScale);
//				l.setTranslateZ(min.z);
//			}
			scaleTF.setX(scaleSession.getViewScale());
			scaleTF.setY(scaleSession.getViewScale());
			scaleTF.setZ(scaleSession.getViewScale());
		});

		// bottomDimentions.bl;

	}

	public Affine getViewRotation() {
		return rotationManager.getViewRotation();
	}

	private void updateCubes() {
		scaleSession.threeDTarget(screenW, screenH, zoom, b,cf);
	}

	public void clearSelection() {
		BowlerStudio.runLater(() -> {
			for (Node r : allElems)
				r.setVisible(false);
		});
		selectionLive = false;
	}

	public SpriteDisplayMode getMode() {
		return mode;
	}

	public void setMode(SpriteDisplayMode mode) {
		if (mode == this.mode)
			return;
		this.mode = mode;
		System.out.println("Mode Set to "+mode);
		//new Exception().printStackTrace();
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
				break;
			case MoveZ:
				for (DottedLine l : lines) {
					l.setVisible(true);
				}
				moveUpArrow.setVisible(true);
				footprint.setVisible(true);
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
			default:
				break;

			}

		});
	}

	public boolean allignIsActive() {
		return allign.isActive();
	}
}
