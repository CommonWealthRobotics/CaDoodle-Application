package com.commonwealthrobotics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.commonwealthrobotics.controls.SpriteDisplayMode;
import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.BowlerKernel;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.Log;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.ColinearPointsException;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.Cylinder;
import eu.mihosoft.vrl.v3d.MissingManipulatorException;
import eu.mihosoft.vrl.v3d.Polygon;
import eu.mihosoft.vrl.v3d.Transform;
import eu.mihosoft.vrl.v3d.Vector3d;
import eu.mihosoft.vrl.v3d.ext.org.poly2tri.PolygonUtil;
import eu.mihosoft.vrl.v3d.ext.quickhull3d.HullUtil;
import javafx.collections.ObservableFloatArray;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Translate;
import javafx.scene.image.WritableImage;

public class WorkplaneManager implements EventHandler<MouseEvent> {

	private MeshView ground;
	private Group wpPick;
	private HashMap<CSG, MeshView> meshes;
	private HashMap<MeshView,CSG> meshesReverseLookup;
	private BowlerStudio3dEngine engine;
	private Affine workplaneLocation = new Affine();
	private MeshView indicatorMesh;
	private TransformNR currentAbsolutePose;
	private Runnable onSelectEvent = () -> {
	};
	private boolean clickOnGround = false;
	private boolean clicked = false;
	private boolean active;
	private Affine wpPickPlacement = new Affine();
	private SelectionSession session;
	private boolean tempory;
	private ActiveProject ap;
	private double increment = 1.0;
	private IWorkplaneUpdate updater = null;
	private Runnable onCancel;

	// Not used anymore
	public Group createWireframeWorkplane(double xSizeMM, double ySizeMM) {

		final float LINE_SPACING = 5.0f;
		final float EXTENT  = (float) xSizeMM / 2.0f;
		final int   LINE_COUNT   = (int) (2 * EXTENT / LINE_SPACING) + 1;
		final float THICKNESS   = 0.1f;

		TriangleMesh mesh = new TriangleMesh();
		mesh.getTexCoords().addAll(0,0); // required dummy coords

		for (int i = 0; i < LINE_COUNT; i++) {
			float x = -EXTENT + i * LINE_SPACING;
			int base = mesh.getPoints().size() / 3;
			mesh.getPoints().addAll(
				x - THICKNESS, -EXTENT, -0.001f,  // 0
				x + THICKNESS, -EXTENT, -0.001f,  // 1
				x + THICKNESS,  EXTENT, -0.001f,  // 2
				x - THICKNESS,  EXTENT, -0.001f); // 3
			mesh.getFaces().addAll(
				base,0, base+1,0, base+2,0,
				base,0, base+2,0, base+3,0);
		}

		for (int i = 0; i < LINE_COUNT; i++) {
			float y = -EXTENT + i * LINE_SPACING;
			int base = mesh.getPoints().size() / 3;
			mesh.getPoints().addAll(
				-EXTENT, y - THICKNESS, -0.001f,  // 0
				 EXTENT, y - THICKNESS, -0.001f,  // 1
				 EXTENT, y + THICKNESS, -0.001f,  // 2
				-EXTENT, y + THICKNESS, -0.001f); // 3
			mesh.getFaces().addAll(
				base,0, base+1,0, base+2,0,
				base,0, base+2,0, base+3,0);
		}

		MeshView meshView = new MeshView(mesh);
		meshView.setCullFace(CullFace.NONE);
		meshView.setDrawMode(DrawMode.FILL);
		meshView.setMaterial(new PhongMaterial(Color.BLACK));

		return new Group(meshView);
	}

	// Create textured work-plane based on tiles of custom size
	public Group createTexturedWorkplane(double xSizeMM, double ySizeMM) {

		// Build square textured tile in MM
		final float TILE_SIZE_MM	 = 10.0f;
		final int TILE_BIG_GRID_PX   = 200;
		final int TILE_SMALL_GRID_PX =  20;

		// Build square textured tile in inches
		//final float TILE_SIZE_MM	   = 25.4f;
		//final int TILE_BIG_GRID_PX   = 200;
		//final int TILE_SMALL_GRID_PX =  20; // 1/10th inch

		// Build square textured tile in inches
		//final float TILE_SIZE_MM	   = 25.4f;
		//final int TILE_BIG_GRID_PX   = 256;
		//final int TILE_SMALL_GRID_PX =  16; // 1/16th inch

		// Build square textured tile in half inche
		//final float TILE_SIZE_MM	   = 12.7f;
		//final int TILE_BIG_GRID_PX   = 254;
		//final int TILE_SMALL_GRID_PX = 127;

		// Upscale work plane texture
		final int wpUpscale = 4;

		// Work plane noise in percentage [0-100%]
		int wpNoise = 25;

		// Work plane texture colors
		int wpColor	 = webColorToArgb(Color.web("#3838A8")); // Higher is lighter color
		int grid1Color  = webColorToArgb(Color.web("#202060"));
		int grid10Color = webColorToArgb(Color.web("#0000FF"));

		float workplaneX = (float)xSizeMM;
		float workplaneY = (float)ySizeMM;

		final float TILE_HALF_PIXEL_SIZE = TILE_SIZE_MM / (TILE_BIG_GRID_PX * 2);

		// Calculate texture offsets. Note X and Y are swapped in the 3D view
		float xTextureOffset = (float)((int)(ySizeMM / (TILE_SIZE_MM * 2)) - ySizeMM / (TILE_SIZE_MM * 2));
		float yTextureOffset = (float)((int)(xSizeMM / (TILE_SIZE_MM * 2)) - xSizeMM / (TILE_SIZE_MM * 2));

		int[] src = new int[TILE_BIG_GRID_PX * TILE_BIG_GRID_PX];

		// Set work plane background (done when adding noise)
		//Arrays.fill(src, wpColor);

		// Add some noise to make the work plane look real
		Random rnd = new Random();
		int r = (wpColor >> 16) & 0xFF;
		int g = (wpColor >>  8) & 0xFF;
		int b =  wpColor		& 0xFF;
		for (int i = 0; i < src.length; i++) {
			int n = 100 + rnd.nextInt(wpNoise + 1) - (wpNoise / 2);
			src[i] = 0xFF000000 |
					 (Math.min(255, (r * n) / 100) << 16) |
					 (Math.min(255, (g * n) / 100) <<  8) |
					 (Math.min(255, (b * n) / 100));
		}

		// Draw small grid, 1 line
		for (int x1 = 0; x1 < TILE_BIG_GRID_PX; x1 += TILE_SMALL_GRID_PX) {
			for (int y = 0; y < TILE_BIG_GRID_PX; y++) {
			src[y * TILE_BIG_GRID_PX + x1] = grid1Color;
			src[x1 * TILE_BIG_GRID_PX + y] = grid1Color;
			}
		}

		// Draw big grid, 3 lines
		int last = TILE_BIG_GRID_PX - 1;
		for (int i = 0; i < TILE_BIG_GRID_PX; i++) {
			src[i + TILE_BIG_GRID_PX	] = grid10Color;
			src[i * TILE_BIG_GRID_PX + 1] = grid10Color;

			src[i					] = grid10Color;
			src[i * TILE_BIG_GRID_PX] = grid10Color;

			src[i * TILE_BIG_GRID_PX + last] = grid10Color;
			src[last * TILE_BIG_GRID_PX + i] = grid10Color;
		}

		// Scale up with nearest neighbor algorithm
		int upscaledX = TILE_BIG_GRID_PX * wpUpscale;
		int upscaledY = TILE_BIG_GRID_PX * wpUpscale;
		WritableImage tile = new WritableImage(upscaledX, upscaledY);
		PixelWriter pw = tile.getPixelWriter();

		for (int y = 0; y < upscaledY; y++) {
			int sy = y / wpUpscale;
			for (int x = 0; x < upscaledX; x++) {
				int sx = x / wpUpscale;
				pw.setArgb(x, y, src[sy * TILE_BIG_GRID_PX + sx]);
			}
		}

		// Create the work plane material
		PhongMaterial material = new PhongMaterial();
// Sharp edges, edges with aliasing
//		material.setDiffuseMap(tile);
//		material.setDiffuseColor(new Color(1, 1, 0, 0.33));
//		material.setSpecularColor(Color.BLACK);
//		material.setSelfIlluminationMap(tile);

		// Set work plane texture
		material.setDiffuseMap(tile);

		// Control work plane transparency
		Color transWhite = new Color(1, 1, 1, 0.45);
		material.setDiffuseColor(transWhite); // Work plane color
		material.setSpecularColor(Color.BLACK); // No shiny spots

		// Create the work plane outline material
		PhongMaterial material2 = new PhongMaterial();
		WritableImage outlineImage = new WritableImage(1, 1);
		outlineImage.getPixelWriter().setColor(0, 0, argbToColor(grid10Color));
		material2.setDiffuseMap(outlineImage);
		material2.setDiffuseColor(transWhite); // Work plane color
		material2.setSpecularColor(Color.BLACK); // No shiny spots

		// Create the work plane mesh, draw at slight offset to align pixel to line centre
		TriangleMesh topMesh = new TriangleMesh();
		topMesh.getPoints().setAll(
		  	-workplaneX / 2 - TILE_HALF_PIXEL_SIZE, -workplaneY / 2 - TILE_HALF_PIXEL_SIZE, 0f,
  			 workplaneX / 2 - TILE_HALF_PIXEL_SIZE, -workplaneY / 2 - TILE_HALF_PIXEL_SIZE, 0f,
  			 workplaneX / 2 - TILE_HALF_PIXEL_SIZE,  workplaneY / 2 - TILE_HALF_PIXEL_SIZE, 0f,
		  	-workplaneX / 2 - TILE_HALF_PIXEL_SIZE,  workplaneY / 2 - TILE_HALF_PIXEL_SIZE, 0f);

		// Map texture to mesh
		topMesh.getTexCoords().setAll(
			xTextureOffset							, yTextureOffset,							// bottom-left
			xTextureOffset							, yTextureOffset + workplaneX/TILE_SIZE_MM, // top-left
			xTextureOffset + workplaneY/TILE_SIZE_MM, yTextureOffset + workplaneX/TILE_SIZE_MM, // top-right
			xTextureOffset + workplaneY/TILE_SIZE_MM, yTextureOffset);							// bottom-right

		topMesh.getFaces().setAll(0,0, 1,1, 2,2, 0,0, 2,2, 3,3); // Frontside
//								  0,0, 2,2, 1,1, 0,0, 3,3, 2,2); // Backside

		MeshView topView = new MeshView(topMesh);
		topView.setMaterial(material);
		topView.setBlendMode(BlendMode.SRC_OVER);
		topView.setCullFace(CullFace.NONE);
		//topView.setCache(false); // keeps JavaFX from scaling the image

		// Create the work plane outline mesh
		final float OUT = 2.0f; // outwards mm
		final float IN  = 0.0f; // inwards mm

		TriangleMesh outlineMesh = new TriangleMesh();
		outlineMesh.getPoints().setAll(
		// inside
			 IN - workplaneX / 2 - TILE_HALF_PIXEL_SIZE,   IN - workplaneY / 2 - TILE_HALF_PIXEL_SIZE, 0f,
			-IN + workplaneX / 2 - TILE_HALF_PIXEL_SIZE,   IN - workplaneY / 2 - TILE_HALF_PIXEL_SIZE, 0f,
			-IN + workplaneX / 2 - TILE_HALF_PIXEL_SIZE,  -IN + workplaneY / 2 - TILE_HALF_PIXEL_SIZE, 0f,
			 IN - workplaneX / 2 - TILE_HALF_PIXEL_SIZE,  -IN + workplaneY / 2 - TILE_HALF_PIXEL_SIZE, 0f,
		// outside
		   -OUT - workplaneX / 2 - TILE_HALF_PIXEL_SIZE, -OUT - workplaneY / 2 - TILE_HALF_PIXEL_SIZE, 0f,
			OUT + workplaneX / 2 - TILE_HALF_PIXEL_SIZE, -OUT - workplaneY / 2 - TILE_HALF_PIXEL_SIZE, 0f,
			OUT + workplaneX / 2 - TILE_HALF_PIXEL_SIZE,  OUT + workplaneY / 2 - TILE_HALF_PIXEL_SIZE, 0f,
		   -OUT - workplaneX / 2 - TILE_HALF_PIXEL_SIZE,  OUT + workplaneY / 2 - TILE_HALF_PIXEL_SIZE, 0f);

		outlineMesh.getTexCoords().setAll(
			0,0,  1,0,  1,1,  0,1,   // inside
			0,0,  1,0,  1,1,  0,1);  // outide

		// 8 triangles (4 quads)
		outlineMesh.getFaces().setAll(
			0,0, 4,4, 5,5,	0,0, 5,5, 1,1,   // bottom
			1,1, 5,5, 6,6,	1,1, 6,6, 2,2,   // right
			2,2, 6,6, 7,7,	2,2, 7,7, 3,3,   // top
			3,3, 7,7, 4,4,	3,3, 4,4, 0,0 ); // left

		MeshView outlineView = new MeshView(outlineMesh);
		outlineView.setMaterial(material2);
		outlineView.setBlendMode(BlendMode.SRC_OVER);
		outlineView.setCullFace(CullFace.NONE);

		Group wp = new Group(topView, outlineView);

		wp.setMouseTransparent(true);

		return wp;
	}

	private static int webColorToArgb(Color color) {
		return (int) (color.getOpacity() * 255) << 24 |
			   (int) (color.getRed()	 * 255) << 16 |
			   (int) (color.getGreen()	 * 255) <<  8 |
			   (int) (color.getBlue()	 * 255);
	}

	private static Color argbToColor(int argb) {

	return Color.color(((argb >> 16) & 0xFF) / 255.0,
					   ((argb >>  8) & 0xFF) / 255.0,
					   ( argb		 & 0xFF) / 255.0,
					   ((argb >> 24) & 0xFF) / 255.0);
	}

	public WorkplaneManager(ActiveProject ap, MeshView ground, BowlerStudio3dEngine engine, SelectionSession session) {

		this.ap = ap;
		this.ground = ground; // Not used anymore
		this.engine = engine;
		this.session = session;

		ground.setVisible(false); // Not used anymore

		wpPick = createTexturedWorkplane(200, 200);
		wpPick.getTransforms().addAll(wpPickPlacement);
		wpPick.setMouseTransparent(true);

		wpPick.addEventFilter(MouseEvent.MOUSE_PRESSED, ev -> {
			setClickOnGround(true);
		});

		engine.getWorkplaneGroup().addEventFilter(MouseEvent.MOUSE_PRESSED, ev -> {
			setClickOnGround(true);
		});

		engine.addCustomWorkplaneNode(wpPick);
		engine.getWorkplaneGroup().setMouseTransparent(true);
	}

	public void setIndicator(CSG indicator, Affine centerOffset) {

		if (indicatorMesh != null)
			engine.removeUserNode(indicatorMesh);

		indicatorMesh = indicator.newMesh();
		indicatorMesh.getTransforms().addAll(getWorkplaneLocation(), centerOffset);
		indicatorMesh.setMouseTransparent(true);

		PhongMaterial material = new PhongMaterial();

		if (indicator.isHole()) {
			material.setDiffuseColor(new Color(0.25, 0.25, 0.25, 0.75));
			material.setSpecularColor(javafx.scene.paint.Color.WHITE);
		} else {
			Color c = indicator.getColor();
			material.setDiffuseColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 0.75));
			material.setSpecularColor(javafx.scene.paint.Color.WHITE);
		}
		indicatorMesh.setMaterial(material);
		engine.addUserNode(indicatorMesh);
	}

	public void updateMeshes(HashMap<CSG, MeshView> meshes) {

		this.meshes = meshes;
		meshesReverseLookup = new HashMap<MeshView, CSG>();

		for (CSG c:meshes.keySet())
			meshesReverseLookup.put(meshes.get(c), c);
	}

	public void cancel() {

		if (!active)
			return;

		updater = null;
		engine.getWorkplaneGroup().removeEventFilter(MouseEvent.ANY, this);
		wpPick.setVisible(isWorkplaneNotOrigin());

		if (meshes != null)
			for (CSG key : meshes.keySet()) {
				MeshView mv = meshes.get(key);
				mv.removeEventFilter(MouseEvent.ANY, this);
			}

		if (indicatorMesh != null)
			indicatorMesh.setVisible(false);

		indicatorMesh = null;

		if (onSelectEvent != null)
			onSelectEvent.run();

		onSelectEvent = null;
		active = false;
		engine.getWorkplaneGroup().setVisible(true);
		engine.getWorkplaneGroup().setMouseTransparent(true);
		session.setMode(SpriteDisplayMode.Default);

		if (onCancel != null) {
			onCancel.run();
			onCancel = null;
		}
	}

	public void activate() {
		activate(true);
	}

	public void activate(boolean enableGroundPick) {
		active = true;
		tempory = false;
		setClickOnGround(false);
		clicked = false;

		// com.neuronrobotics.sdk.common.Log.debug("Starting workplane listeners");
		wpPick.addEventFilter(MouseEvent.ANY, this);
		wpPick.setMouseTransparent(false);
		wpPick.setVisible(isWorkplaneNotOrigin());

		engine.getWorkplaneGroup().addEventFilter(MouseEvent.ANY, this);
		engine.getWorkplaneGroup().setMouseTransparent(false);

		// Make user meshes pickable
		if (meshes != null)
			for (CSG key : meshes.keySet()) {
				MeshView mv = meshes.get(key);
				mv.addEventFilter(MouseEvent.ANY, this);
			}

		if (indicatorMesh != null)
			indicatorMesh.setVisible(true);
	}

	@Override
	public void handle(MouseEvent ev) {
		PickResult pickResult = ev.getPickResult();
		Node intersectedNode = pickResult.getIntersectedNode();

		if (ev.getEventType() == MouseEvent.MOUSE_PRESSED) {
			clicked = true;
			onCancel = null;// non cancel but instead completed
			cancel();
			ev.consume();
			session.updateControls();
			engine.getWorkplaneGroup().setMouseTransparent(true);

			wpPick.setMouseTransparent(true);

		} else if ((ev.getEventType() == MouseEvent.MOUSE_MOVED) || (ev.getEventType() == MouseEvent.MOUSE_DRAGGED)) {
			// com.neuronrobotics.sdk.common.Log.error(ev);
			Point3D intersectedPoint = pickResult.getIntersectedPoint();
			double x = intersectedPoint.getX();
			double y = intersectedPoint.getY();
			double z = intersectedPoint.getZ();

			if (ev.getSource() == wpPick) {
				x *= MainController.groundScale();
				y *= MainController.groundScale();
				z *= MainController.groundScale();
			}

			TransformNR screenLocation;
			TransformNR pureRot = null;
			Affine manipulator = new Affine();
			CSG source = null;

			if (intersectedNode instanceof MeshView) {
				MeshView meshView = (MeshView) intersectedNode;

				if (meshesReverseLookup != null) {
					source = meshesReverseLookup.get(meshView);

					if ((source != null) && (source.hasManipulator()))
						try {
							manipulator = source.getManipulator();
						} catch (MissingManipulatorException e) {
							e.printStackTrace();
						}
				}

				TriangleMesh mesh = (TriangleMesh) meshView.getMesh();

				int faceIndex = pickResult.getIntersectedFace();

				if (faceIndex >= 0) {

					if (source != null) {
						ArrayList<Polygon> polygons = source.getPolygons();
						Polygon p =  getPolygonFromFaceIndex(faceIndex,polygons);

						if (p != null) {
							try {
								pureRot = TransformFactory.csgToNR(PolygonUtil.calculateNormalTransform(p)).inverse();
								// an in-plane snapping here by transforming the points
								// into the plane orentation, then snapping in plane, then transforming the points back.
								TransformNR t = new TransformNR(x, y, z);
								TransformNR screenLocationtmp = t;//manipulatorNR.times(t);
								Polygon np = p; //p.transformed(TransformFactory.affineToCSG(manipulator));
								Transform npTF = PolygonUtil.calculateNormalTransform(np);
								TransformNR npTFNR = TransformFactory.csgToNR(npTF);
								Polygon flattened = np.transformed(npTF);
								TransformNR flattenedTouch = npTFNR.times(screenLocationtmp);
								//Log.debug("Polygon " + flattened);
								//Log.debug("Point " + flattenedTouch.toSimpleString());
								TransformNR adjusted = new TransformNR(
										SelectionSession.roundToNearest(flattenedTouch.getX(), snapgridValue),// snap in plane
										SelectionSession.roundToNearest(flattenedTouch.getY(), snapgridValue),
										flattened.getPoints().get(0).z);// adhere to the plane of the polygon
								TransformNR adjustedBack = npTFNR.inverse().times(adjusted);// flip the point back to its original orentaation in the plane post snap
								x = adjustedBack.getX();
								y = adjustedBack.getY();
								z = adjustedBack.getZ();

								//Log.debug("Polygon snapped " + adjusted);
							} catch (Exception e) {
								e.printStackTrace();
							}

						} else
							Log.error("Polygon not found " + faceIndex);

					} else {
						x = SelectionSession.roundToNearest(x, snapgridValue);
						y = SelectionSession.roundToNearest(y, snapgridValue);
						z = SelectionSession.roundToNearest(z, snapgridValue);
					}

					if (pureRot == null)
						pureRot = getFaceNormalAngles(mesh, faceIndex).inverse();

				} else
					Log.error("Error face index came back: " + faceIndex);

			}

			TransformNR manipulatorNR = TransformFactory.affineToNr(manipulator);
			TransformNR t = new TransformNR(x, y, z);
			screenLocation = manipulatorNR.times(t.times(pureRot));

			if ((intersectedNode == wpPick) || (intersectedNode.getParent() == wpPick)) {
				if (updater != null)
					updater.setWorkplaneLocation(screenLocation);

				screenLocation = ap.get().getWorkplane().times(screenLocation);
			} else
				if (updater != null)
					updater.setWorkplaneLocation(ap.get().getWorkplane().inverse().times(screenLocation));

			setCurrentAbsolutePose(screenLocation);
		}
	}

	public static Polygon getPolygonFromFaceIndex(int faceIndex, List<Polygon> polygons) {

		if (faceIndex < 0)
			return null;

		int currentFaceCount = 0;

		// We need to iterate because some polygons might have < 3 vertices (0 faces)
		// If you're CERTAIN all polygons have >= 3 vertices, this could be optimized
		for (Polygon p : polygons) {
			int vertexCount = p.getVertices().size();
			int facesInThisPolygon = vertexCount - 2;
			// Check if the face index falls within this polygon's range

			if (faceIndex < (currentFaceCount + facesInThisPolygon))
				return p;

			currentFaceCount += facesInThisPolygon;
		}

		return null;
	}

	private Vector3d toV(javafx.geometry.Point3D p) {
		return new Vector3d(p.getX(), p.getY(), p.getZ());
	}

	private TransformNR getFaceNormalAngles(TriangleMesh mesh, int faceIndex) {
		ObservableFaceArray faces = mesh.getFaces();
		ObservableFloatArray points = mesh.getPoints();

		int p1Index = faces.get(faceIndex * 6    ) * 3;
		int p2Index = faces.get(faceIndex * 6 + 2) * 3;
		int p3Index = faces.get(faceIndex * 6 + 4) * 3;

		Point3D p1 = new Point3D(points.get(p1Index), points.get(p1Index + 1), points.get(p1Index + 2));
		Point3D p2 = new Point3D(points.get(p2Index), points.get(p2Index + 1), points.get(p2Index + 2));
		Point3D p3 = new Point3D(points.get(p3Index), points.get(p3Index + 1), points.get(p3Index + 2));

		try {
			Polygon p = Polygon.fromPoints(Arrays.asList(toV(p1),toV(p2),toV(p3)));
			return TransformFactory.csgToNR(PolygonUtil.calculateNormalTransform(p));

		} catch (Exception e) {
			e.printStackTrace();
		}

		return new TransformNR();
	}

	public TransformNR getCurrentAbsolutePose() {
		return currentAbsolutePose;
	}

	public void setCurrentAbsolutePose(TransformNR currentAbsolutePose) {
		this.currentAbsolutePose = currentAbsolutePose;
		TransformFactory.nrToAffine(getCurrentAbsolutePose(), getWorkplaneLocation());
	}

	public Runnable getOnSelectEvent() {
		return onSelectEvent;
	}

	public void setOnSelectEvent(Runnable onSelectEvent) {
		this.onSelectEvent = onSelectEvent;
	}

	public boolean isClickOnGround() {
		return clickOnGround;
	}

	public void setClickOnGround(boolean clickOnGround) {
		this.clickOnGround = clickOnGround;
	}

	public boolean isClicked() {
		return clicked;
	}

	public void pickPlane(Runnable r, Runnable always, RulerManager ruler) {

		// Create work plane placement indicator
		double pointerLen = 10;
		double pointerWidth = 2;
		double pointerHeight = 20;

		CSG indicator = HullUtil.hull(Arrays.asList(new Vector3d(0, 0, 0), new Vector3d(pointerLen, 0, 0),
			new Vector3d(pointerWidth, pointerWidth, 0), new Vector3d(0, 0, pointerHeight)))
			.union(HullUtil.hull(Arrays.asList(new Vector3d(0, 0, 0), new Vector3d(0, pointerLen, 0),
			new Vector3d(pointerWidth, pointerWidth, 0), new Vector3d(0, 0, pointerHeight))))
			.setColor(Color.YELLOWGREEN);

		this.setIndicator(indicator, new Affine());

		ap.get().setWorkplane(new TransformNR());
		placeWorkplaneVisualization();

		this.setOnSelectEvent(() -> {

			if (this.isClicked()) {

				if (this.isClickOnGround()) {
					// com.neuronrobotics.sdk.common.Log.debug("Ground plane click detected");
					ap.get().setWorkplane(new TransformNR());
					ruler.cancel();
				} else
					ap.get().setWorkplane(this.getCurrentAbsolutePose());

				placeWorkplaneVisualization();

				r.run();
			}
			always.run();
		});

		this.activate(true);
	}

	public void placeWorkplaneVisualization() {
		Log.debug("Placing workplane visualization");
		engine.placeGrid(ap.get().getWorkplane());

		BowlerKernel.runLater(() -> {
			wpPick.setVisible(isWorkplaneNotOrigin());
			TransformFactory.nrToAffine(ap.get().getWorkplane(), wpPickPlacement);
		});
	}

	public boolean isWorkplaneNotOrigin() {
		TransformNR w = ap.get().getWorkplane();
		double epsilon = 0.00001;
		RotationNR r = w.getRotation();
		double abst = Math.abs(w.getX());
		double abs2t = Math.abs(w.getY());
		double abs3t = Math.abs(w.getZ());

		if ((abst > epsilon) || (abs2t > epsilon) || (abs3t > epsilon))
			return true;

		double abs = Math.abs(r.getRotationAzimuthDegrees());
		double abs2 = Math.abs(r.getRotationElevationDegrees());
		double abs3 = Math.abs(r.getRotationTiltDegrees());

		return ((abs > epsilon) || (abs2 > epsilon) || (abs3 > epsilon));
	}

	public void setTemporaryPlane() {
		tempory = true;
	}

	public void clearTemporaryPlane() {
		tempory = false;
	}

	public boolean isTemporaryPlane() {
		return tempory;
	}

	public double getIncrement() {
		return increment;
	}

	public void setIncrement(double increment) {
		this.increment = increment;
	}

	public Group getPlacementPlane() {
		return wpPick;
	}

	public Affine getWorkplaneLocation() {
		return workplaneLocation;
	}

	public void setWorkplaneLocation(Affine workplaneLocation) {
		this.workplaneLocation = workplaneLocation;
	}

	public void setUpdater(IWorkplaneUpdate updater) {
		this.updater = updater;
	}

	public void onCancel(Runnable onCancel) {
		this.onCancel = onCancel;
	}
}
