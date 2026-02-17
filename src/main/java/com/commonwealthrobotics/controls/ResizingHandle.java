package com.commonwealthrobotics.controls;

import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.ChamferedCube;
import eu.mihosoft.vrl.v3d.Cube;
//import eu.mihosoft.vrl.v3d.Transform;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.scene.layout.Pane;
import javafx.beans.value.ChangeListener;
import javafx.scene.transform.NonInvertibleTransformException;

public class ResizingHandle {

	private final Pane overlay; // Overlay pane for 2D objects

	private double baseScale = 0.75; // Base scale factor for the corner handle cubes
	private static final double size = 10; // Handle size in pixels
	private double scaleFactor;

	private BowlerStudio3dEngine engine;
	private PerspectiveCamera camera;
	private MeshView mesh;

	private Affine location = new Affine();
	private Affine cameraOrent = new Affine();
	private Scale scaleTF = new Scale();
	private Affine resizeHandleLocation = new Affine();
	private Affine baseMove;

	public Manipulation manipulator;
	private String name;
	private PhongMaterial material;
	private boolean selected = false;
	private Affine workplaneOffset;
	private boolean uniform = false;
	private boolean resizeAllowed;
	private boolean moveLock;
	private Color myColor = null;
	private Color highlightColor = new Color(1, 0, 0, 1);
	private Point3D startingPosition3D;
	private boolean zMove = false; // Is the move a XY or a Z-move
	private double snapGridValue = 0;

	// private Tooltip hover = new Tooltip();
	public ResizingHandle(String name, BowlerStudio3dEngine engine, Affine move, Vector3d vector3d,
			Affine workplaneOffset, Runnable onSelect, Runnable onReset) {
		this(name, engine, move, vector3d, workplaneOffset, onSelect, onReset, new ChamferedCube(getSize(), getSize(), getSize() / 2.4, getSize() / 5).toCSG().toZMin());
	}

	public ResizingHandle(String name, BowlerStudio3dEngine engine, Affine move, Vector3d vector3d,
			Affine workplaneOffset, Runnable onSelect, Runnable onReset, CSG shape) {
		this.name = name;
		this.workplaneOffset = workplaneOffset;
		this.baseMove = move;
		// Different behavior for different moves
		zMove = this.name.equals("topCenter"); // XY-move or Z-move?

		manipulator = new Manipulation(resizeHandleLocation, vector3d, new TransformNR(), this::sendNewWorldPosition, zMove);
//		super(12.0, 12.0, Color.WHITE);
//		setStroke(Color.BLACK);
//		setStrokeWidth(3);
		if (engine == null)
			throw new NullPointerException();

		this.engine = engine;
		overlay = engine.getOverlayPane();
		camera = engine.getFlyingCamera().getCamera();

		mesh = shape.getMesh();
		material = new PhongMaterial();
		resetColor();
		mesh.setCullFace(CullFace.BACK);
		// material.setSpecularColor(javafx.scene.paint.Color.WHITE);
		mesh.setMaterial(material);
		mesh.addEventFilter(MouseEvent.MOUSE_EXITED, event -> {
			if (!selected)
				resetColor();
		});

		mesh.addEventFilter(MouseEvent.MOUSE_ENTERED, event -> {
			setSelectedColor();
		});

		mesh.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			com.neuronrobotics.sdk.common.Log.debug("Corner selected");
			onReset.run();
			selected = true;
			setUniform(event.isShiftDown());
			onSelect.run();
			setSelectedColor();

			this.startingPosition3D = getAbsolutePositionInWorkplane();
			manipulator.setStartingWorkplanePosition(startingPosition3D);

		});

		mesh.getTransforms().add(move);
		mesh.getTransforms().add(resizeHandleLocation);
		mesh.getTransforms().add(workplaneOffset);
		mesh.getTransforms().add(location);
		mesh.getTransforms().add(cameraOrent);
		mesh.getTransforms().add(scaleTF);
		// Tooltip.install(mesh, hover);
		mesh.addEventFilter(MouseEvent.ANY, manipulator.getMouseEvents());

	}

	public Point3D getAbsolutePosition() {
		Affine combined = new Affine();
		combined.prepend(scaleTF);
		combined.prepend(cameraOrent);
		combined.prepend(location);
		combined.prepend(workplaneOffset);
		combined.prepend(resizeHandleLocation);
		combined.prepend(baseMove);

		return combined.transform(new Point3D(0, 0, 0));
	}

	public Point3D getAbsolutePositionInWorkplane() {
		Point3D world = getAbsolutePosition();
		try {
			return workplaneOffset.createInverse().transform(world);
		} catch (NonInvertibleTransformException e) {
			return world;
		}
	}

	public Point3D sendNewWorldPosition(double screenX, double screenY, double snapGridValue) {

		// Convert to subscene coordinates to remove offsets
		Point2D overlayCoords = overlay.sceneToLocal(new Point2D(screenX, screenY));

		if (zMove) {
			// Z-move: fixed XY in local space, varying Z
			double foundZ = engine.sceneToWorldFixedXY_WP(overlayCoords, startingPosition3D.getX(), startingPosition3D.getY());
			updateCubeSize();
			return new Point3D(startingPosition3D.getX(), startingPosition3D.getY(), this.manipulator.snapToGrid(foundZ));
		} else {
			// XY-move: fixed Z in local space, varying XY
			Point3D wp3d = engine.sceneToWorldFixedZ_WP(overlayCoords, startingPosition3D.getZ());
			updateCubeSize();
			return new Point3D(this.manipulator.snapToGrid(wp3d.getX()), this.manipulator.snapToGrid(wp3d.getY()), wp3d.getZ());
		}
	}

	private void setSelectedColor() {
		material.setDiffuseColor(highlightColor);
	}

	private Color getSelectedColor() {
		return highlightColor;
	}

	private void resetColor() {
		material.setDiffuseColor(currentColor());
	}

	private Color currentColor() {
		Color tmpColor = (myColor != null) ? myColor : new Color(isResizeAllowed()?1:0, moveLock?0:1, 1, 1);

		// Make top handle a bit darker to distinguish it from the corner handles
		if (zMove)
			tmpColor = Color.color((tmpColor.getRed() * 0.8), (tmpColor.getGreen() * 0.8), (tmpColor.getBlue() * 0.8));

		return tmpColor;
	}

	public TransformNR getParametric() {
		return new TransformNR(resizeHandleLocation.getTx(), resizeHandleLocation.getTy(),
				resizeHandleLocation.getTz());
	}

	public void setInReferenceFrame(double x, double y, double z) {
		TransformNR wp = manipulator.getFrameOfReference().copy();
		TransformNR tmp = new TransformNR(x, y, z);

		TransformFactory.nrToAffine(tmp, location);
	}

	public TransformNR getCurrentInGlobalFrame() {
		TransformNR wp = manipulator.getFrameOfReference().copy();
		TransformNR rsz = TransformFactory.affineToNr(resizeHandleLocation);
		TransformNR loc = TransformFactory.affineToNr(location);
		return rsz.times(wp.times(loc));
	}

	public TransformNR getCurrentInReferenceFrame() {
		TransformNR wp = manipulator.getFrameOfReference().copy();
		return wp.inverse().times(getCurrentInGlobalFrame());
	}

	public TransformNR getCurrent() {

		return new TransformNR(resizeHandleLocation.getTx() + location.getTx(),
				resizeHandleLocation.getTy() + location.getTy(), resizeHandleLocation.getTz() + location.getTz());
	}

	public TransformNR screenToWorld(double screenW, double screenH, double zoom, double mouseX, double mouseY) {
		double wHalf = screenW / 2;
		double hHalf = screenH / 2;
		double scalex = calculatePixelToMmScale(screenW, screenH, -zoom);

		// Convert mouse coordinates to centered coordinates
		double xComp = mouseX - wHalf;
		double yComp = mouseY - hHalf;

		// Calculate angles
		double angleOfXpoint = Math.atan2(xComp * scalex, -zoom);
		double angleOfYPoint = Math.atan2(yComp * scalex, -zoom);

		// Calculate world X and Y
		double worldX = Math.tan(angleOfXpoint) * (-zoom);
		double worldY = Math.tan(angleOfYPoint) * (-zoom);

		// Create a TransformNR for the point in camera space
		TransformNR pointInCameraSpace = new TransformNR(worldX, worldY, 0, new RotationNR());

		// Get the camera frame and apply inverse rotation
		TransformNR cf = engine.getFlyingCamera().getCamerFrame().times(new TransformNR(RotationNR.getRotationZ(180)));

		// Transform the point from camera space to world space
		TransformNR worldPoint = cf.times(pointInCameraSpace);

		return worldPoint;
	}

	public double calculateEffectiveFov(double screenW, double screenH) {
		double aspectRatio = screenW / screenH;
		double verticalFov = Math.toRadians(camera.getFieldOfView());

		if (aspectRatio >= 1) {
			// Landscape or square orientation
			return 2 * Math.atan(Math.tan(verticalFov / 2) * aspectRatio);
		} else {
			// Portrait orientation
			return verticalFov;
		}
	}

	public double calculatePixelToMmScale(double screenW, double screenH, double zoom) {
		double effectiveFov = calculateEffectiveFov(screenW, screenH);
		double distance = zoom; // Assuming -zoom is stored in camera's Z translation

		if (screenW >= screenH) {
			// Landscape or square orientation
			double realWorldWidth = 2 * distance * Math.tan(effectiveFov / 2);
			return realWorldWidth / screenW;
		} else {
			// Portrait orientation
			double realWorldHeight = 2 * distance * Math.tan(effectiveFov / 2);
			return realWorldHeight / screenH;
		}
	}

	public void updateCubeSize() {

		Point3D world3Dpos = getAbsolutePosition();

		double calculatedScaleFactor = engine.screenToSceneMMscale(world3Dpos);
	   
		setScale(calculatedScaleFactor);
		
		BowlerStudio.runLater(() -> {
			scaleTF.setX(calculatedScaleFactor);
			scaleTF.setY(calculatedScaleFactor);
			scaleTF.setZ(calculatedScaleFactor);
		});
	}

	public void threeDTarget(double screenW, double screenH, double zoom, TransformNR target, TransformNR cf, boolean locked) {

		updateCubeSize();

		TransformNR pureRot = new TransformNR(cf.getRotation());


		BowlerStudio.runLater(() -> {
			setVisible(!locked);

			TransformFactory.nrToAffine(pureRot, cameraOrent);
			TransformFactory.nrToAffine(target.copy().setRotation(new RotationNR()), location);
		});

		// hover.setText(name + " " + getCurrentInReferenceFrame()) ;
	}

	public void hide() {
		setVisible(false);
	}

	public void show() {
		setVisible(true);
	}

	public void setVisible(boolean b) {
		mesh.setVisible(b);
	}

	public static double getSize() {
		return size;
	}

	private double getHeight() {
		return getSize();
	}

	private double getWidth() {
		return getSize();
	}

	public MeshView getMesh() {
		return mesh;
	}

	public void setMesh(MeshView mesh) {
		this.mesh = mesh;
	}

	public void setBaseScale(double baseScale) {
		this.baseScale = baseScale;
	}

	private double getBaseScale() {
		return baseScale;
	}

	private void setScale(double scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	public double getScale() {
		return baseScale * scaleFactor;
	}

	@Override
	public String toString() {
		return name;
	}

	public boolean isSelected() {
		return selected;
	}

	public void resetSelected() {
		resetColor();
		selected = false;
		setUniform(false);
	}

	public void setUniform(boolean uniform) {
		this.uniform = uniform;
	}

	public boolean isUniform() {
		return uniform;
	}

	public void setResizeAllowed(boolean resizeAllowed, boolean moveLock) {
		this.resizeAllowed = resizeAllowed;
		this.moveLock = moveLock;
		manipulator.setUnlocked( resizeAllowed) ;
		resetColor();
	}

	public boolean isResizeAllowed() {
		return resizeAllowed;
	}

	/**
	 * @param myColor the myColor to set
	 */
	public void setMyColor(Color myColor, Color highlightColor) {
		this.myColor = myColor;
		this.highlightColor = highlightColor;
		resetColor();
	}

	/**
	 * @return the myColor
	 */
	public Color getMyColor() {
		return myColor;
	}

}
