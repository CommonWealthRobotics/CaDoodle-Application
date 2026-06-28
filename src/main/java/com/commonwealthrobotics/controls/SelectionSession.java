package com.commonwealthrobotics.controls;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.RulerManager;
import com.commonwealthrobotics.TimelineManager;
import com.commonwealthrobotics.WorkplaneManager;
import com.commonwealthrobotics.fillet.ExtrudeUIManager;
import com.commonwealthrobotics.fillet.FilletUIManager;
import com.commonwealthrobotics.robot.LimbControlManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.neuronrobotics.bowlerkernel.Bezier3d.IInteractiveUIElementProvider;
import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;
import com.neuronrobotics.bowlerstudio.BowlerKernel;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.SplashManager;
import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.assets.FontSizeManager;
import com.neuronrobotics.bowlerstudio.creature.LimbOption;
import com.neuronrobotics.bowlerstudio.creature.MobileBaseBuilder;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.CaDoodleLoader;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.*;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.robot.AddRobotLimb;
import com.neuronrobotics.bowlerstudio.scripting.external.ExternalEditorController;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.VirtualCameraMobileBase;
import com.neuronrobotics.bowlerstudio.util.FileChangeWatcher;
import com.neuronrobotics.bowlerstudio.util.IFileChangeListener;
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.Log;
import com.neuronrobotics.sdk.common.TickToc;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Plane;
import eu.mihosoft.vrl.v3d.Transform;
import eu.mihosoft.vrl.v3d.Vector3d;
import eu.mihosoft.vrl.v3d.parametrics.CSGDatabaseInstance;
import eu.mihosoft.vrl.v3d.parametrics.IParameterChanged;
import eu.mihosoft.vrl.v3d.parametrics.LengthParameter;
import eu.mihosoft.vrl.v3d.parametrics.Parameter;
import eu.mihosoft.vrl.v3d.parametrics.StringParameter;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.DepthTest;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.stage.Popup;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;

@SuppressWarnings("unused")
public class SelectionSession implements ICaDoodleStateUpdate {
	private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private ControlSprites controls;

	public class MeshHolder {
		public MeshView display;
		public MeshView halo;
		public Bounds bouds;

		public MeshHolder(MeshView display, MeshView halo, Bounds bouds) {
			this.display = display;
			this.halo = halo;
			this.bouds = bouds;
		}
	}

	private HashMap<CSG, MeshHolder> meshes = new HashMap<CSG, MeshHolder>();
	private final double MAX_NUMBER_FILED = 9999;
	private Label shapeConfiguration;
	private Accordion shapeConfigurationBox;
	private AnchorPane shapeConfigurationHolder;
	private GridPane configurationGrid;
	private AnchorPane control3d;
	public BowlerStudio3dEngine engine;
	private LinkedHashSet<CSG> selected = new LinkedHashSet<>();
	private ColorPicker colorPicker;
	private ComboBox<String> snapGrid;
	private List<Button> buttons;
	private Button ungroupButton;
	private Button groupButton;
	private ImageView showHideImage;
	private List<String> copySetinternal;
	private Button alignButton;
	private long timeSinceLastMove = System.currentTimeMillis();
	private double screenW;
	private double screenH;
	private double zoom;
	private double az;
	private double el;
	private double x;
	private double y;
	private double z;

	private Affine selection = new Affine();
	private Manipulation manipulation = new Manipulation(selection, new Vector3d(1, 1, 0), new TransformNR(),
			this::sendNewWorldPosition, false, false);
	private Point3D startingPosition3D;
	private EventHandler<MouseEvent> mouseMover = manipulation.getMouseEvents();


	public WorkplaneManager workplane;
	boolean intitialization = false;

	private VBox parametrics;
	private ActiveProject ap = null;
	private HashMap<FileChangeWatcher, CaDoodleOperation> myWatchers = new HashMap<>();
	private Button lockButton;
	private ImageView lockImage;
	// private boolean useButton = false;
	// private Button regenerate = new Button("Re-Generate");
	// private HashMap<String, EventHandler<ActionEvent>> regenEvents = new
	// HashMap<>();
	private boolean showConstituants = false;
	private MenuButton advancedGroupMenu;
	private TimelineManager timeline;
	private RulerManager ruler;
	private Button objectWorkplane;
	private Button dropToWorkplane;
	private boolean isObjectWorkplane = false;
	private TransformNR previousWP;
	private boolean advanced;
	private boolean robotLabOpen = true;
	private Button robotLabDrawer;
	private Runnable updateRobotLab = null;
	private LimbControlManager limbs;
	private boolean regenerating;
	private final List<Runnable> selectionListeners = new CopyOnWriteArrayList<>();
	private ProgressIndicator memUsage;
	private boolean resizeLiveMode = false;
	private final Pane overlayPane; // Overlay pane for 2D objects
	private FilletUIManager filletTool = new FilletUIManager();
	private ExtrudeUIManager extrudeManager = new ExtrudeUIManager();

	public boolean isResizeLiveMode() {
		return resizeLiveMode;
	}

	private final LockableHandler lockableMouseMover = new LockableHandler(manipulation.getMouseEvents());
	private Thread timeoutMoveThread = null;
	private boolean applyingMoveOperation;
	private Button renameBtn;
	private Button filletButton;
	private Button extrudeButton;
	private Button hexDistributeButton;
	private Button boltHoleButton;
	private CaDoodleOperation source;

	@SuppressWarnings("static-access")
	public SelectionSession(BowlerStudio3dEngine e, ActiveProject ap, RulerManager ruler, MeshView ground) {
		this.engine = e;
		this.ruler = ruler;
		workplane = new WorkplaneManager(ap, ground, engine, this);

		this.overlayPane = engine.getOverlayPane();
		//

		setActiveProject(ap, workplane);
		manipulation.addSaveListener(() -> {

			if (intitialization)
				return;

			TransformNR globalPose = manipulation.getGlobalPoseInReferenceFrame();
			// com.neuronrobotics.sdk.common.Log.error("Objects Moved! " +
			// globalPose.toSimpleString());
			Thread t;
			try {
				t = ap.addOp(new MoveCenter().setLocation(globalPose).setNames(selectedSnapshot(), ap.get()));
				try {
					t.join();
				} catch (InterruptedException ex) {
					// Auto-generated catch block
					com.neuronrobotics.sdk.common.Log.error(e);
				}
			} catch (InvalidLocationMove e1) {
				Log.error(e1);
			}

			getControls().setMode(SpriteDisplayMode.Default);

		});

		manipulation.addEventListener(ev -> {
			if (intitialization)
				return;

			getControls().setMode(SpriteDisplayMode.MoveXY);
			BowlerKernel.runLater(() -> updateControls());
		});

		Manipulation.setUi(new IInteractiveUIElementProvider() {

			public void runLater(Runnable r) {
				BowlerKernel.runLater(r);
			}

			public TransformNR getCamerFrame() {
				return engine.getFlyingCamera().getCamerFrame();
			}

			public double getCamerDepth() {
				return engine.getFlyingCamera().getZoomDepth();
			}

			@Override
			public PerspectiveCamera getCamera() {
				return engine.getFlyingCamera().getCamera();
			}

		});

		manipulation.setFrameOfReference(() -> ap.get().getWorkplane());
		ap.addListener(this);
	}

	public double clamp(double in, double min, double max) {
		return Math.max(min, Math.min(in, max));
	}

	public Point3D sendNewWorldPosition(double screenX, double screenY, double snapGridValue) {

		// Convert to subscene coordinates to remove offsets
		Point2D overlayPaneCoordinates = overlayPane.sceneToLocal(new Point2D(screenX, screenY));

		// Clamp object center for standard and custom work plane
		double clampValue = workplane.isWorkplaneNotOrigin() ? 300 : 600;

		// XY-move: fixed Z in local space
		Point3D wp3d = engine.sceneToWorldFixedZ_WP(overlayPaneCoordinates, startingPosition3D.getZ());
		wp3d = new Point3D(clamp(wp3d.getX(), -clampValue, clampValue), clamp(wp3d.getY(), -clampValue, clampValue),
				clamp(wp3d.getZ(), -clampValue, clampValue));

		return new Point3D(this.manipulation.snapToGrid(wp3d.getX()), this.manipulation.snapToGrid(wp3d.getY()),
				wp3d.getZ());
	}

	public boolean moveLock() {

		for (CSG sel : getSelected())
			if (sel.isMotionLock() || sel.isInGroup())
				return true;

		return false;
	}

	public List<String> selectedSnapshot() {
		ArrayList<String> s = new ArrayList<String>();
		for (CSG c : getSelected())
			s.add(c.getName());

		return s;
	}

	@Override
	public void onUpdate(List<CSG> currentState, CaDoodleOperation source, CaDoodleFile f) {
		if (source == this.source)
			return;
		this.source = source;
		// TickToc.setEnabled(true);
		TickToc.tic("Start On Update In Selected Session");
		ArrayList<CSG> toCleal = new ArrayList<CSG>();
		for (CSG c : currentState) {
			for (String s : source.getNamesAddedInThisOperation()) {
				if (c.getName().contentEquals(s))
					toCleal.add(c);
			}

		}
		clearBoundsCache(toCleal);
		toCleal.clear();
		// if (f.isInitialized())
		try {
			getSellectedBounds(currentState);
		} catch (BoundsComputFailure e) {
			Log.error(e);
		}
		// this.source = source;
		intitialization = true;
		// manipulation.set(0, 0, 0);

		intitialization = false;
		setUpParametrics(currentState, source);
		displayCurrent(source);
		TickToc.tic("Update memory display");
		updateMemoryDisplay();
		TickToc.tic("Finish On Update In Selected Session");
		TickToc.toc();
		// TickToc.setEnabled(false);
	}

	public void updateMemoryDisplay() {
		double value = ((double) CaDoodleFile.getFreeMemory()) / 100.0;
		Log.info("Mem=" + value);
		memUsage.setProgress(value);

		// if (value > 0.5 && value < 0.75) { }
	}

	private static class LockableHandler implements EventHandler<MouseEvent> {
		private final EventHandler<MouseEvent> delegate;
		private volatile boolean enabled = true;

		LockableHandler(EventHandler<MouseEvent> delegate) {
			this.delegate = delegate;
		}

		void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public void handle(MouseEvent event) {
			if (enabled)
				delegate.handle(event);
		}
	}

	private Thread myRegenerate(CaDoodleOperation source, IFileChangeListener l, File f) {
		if (regenerating)
			return null;

		regenerating = true;
		FileChangeWatcher fileChangeWatcher = null;
		for (FileChangeWatcher w : myWatchers.keySet())
			if (myWatchers.get(w) == source)
				fileChangeWatcher = w;

		if (fileChangeWatcher != null) {
			fileChangeWatcher.close();
			myWatchers.remove(fileChangeWatcher);
		}

		com.neuronrobotics.sdk.common.Log.debug("Regenerating from CaDoodle " + source);

		// new Exception().printStackTrace();
		Thread tr = new Thread(() -> {
			try {
				ap.get().getCsgDBinstance().saveDatabase();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Thread t;
			try {
				t = ap.regenerateFrom(source);
				if (t == null) {
					SplashManager.closeSplash();
					return;
				}
				try {
					t.join();
				} catch (InterruptedException e) {
					// Auto-generated catch block
					com.neuronrobotics.sdk.common.Log.error(e);
				}
			} catch (FailedToApplyOperation e) {
				Log.error(e);
			}

			if ((f != null) && (l != null)) {
				FileChangeWatcher w;
				try {
					w = FileChangeWatcher.watch(f);
					myWatchers.put(w, source);
					w.addIFileChangeListener(l);
				} catch (IOException e) {
					// Auto-generated catch block
					com.neuronrobotics.sdk.common.Log.error(e);
				}
			}
			regenerating = false;
		});

		tr.start();
		return tr;
	}

	private void setUpParametrics(List<CSG> currentState, CaDoodleOperation source) {
		if (AddRobotLimb.class.isInstance(source)) {
			AddRobotLimb op = (AddRobotLimb) source;
			MobileBaseBuilder builder = op.getMobilBaseBuilder();
			try {
				LimbOption limb = op.getLimb();
				DHParameterKinematics kin = op.getKinematics();
				File configFile = ScriptingEngine.fileFromGit(limb.getUrl(), limb.getSourceFile());
				File cadFile = ScriptingEngine.fileFromGit(kin.getGitCadEngine());

				FileChangeWatcher fileChangeWatcher = null;
				for (FileChangeWatcher w : myWatchers.keySet()) {
					if (myWatchers.get(w) == source)
						fileChangeWatcher = w;

					if (w.getFileToWatch().toPath().compareTo(configFile.toPath()) == 0)
						fileChangeWatcher = w;

					if (w.getFileToWatch().toPath().compareTo(cadFile.toPath()) == 0)
						fileChangeWatcher = w;
				}

				if (fileChangeWatcher == null) {

					IFileChangeListener l = new IFileChangeListener() {
						boolean active = false;

						@Override
						public void onFileDelete(File fileThatIsDeleted) {
						}

						@Override
						public void onFileChange(File fileThatChanged, @SuppressWarnings("rawtypes") WatchEvent event) {
							if (ap.get().isRegenerating() || active) {
								Log.error("Failed regenerate because still running");
								return;
							}

							active = true;
							try {
								Log.debug("File Change updating " + source.getType() + " because of "
										+ fileThatChanged.getAbsolutePath());
								builder.clear(op);
								Thread tr = myRegenerate(source, this, fileThatChanged);
								if (tr != null)
									tr.join();

							} catch (Exception ex) {
								Log.error(ex);
							}
							active = false;
						}

					};

					try {
						FileChangeWatcher w1 = FileChangeWatcher.watch(configFile);
						myWatchers.put(w1, source);
						w1.addIFileChangeListener(l);
						FileChangeWatcher w = FileChangeWatcher.watch(cadFile);
						w.removeIFileChangeListeners();
						myWatchers.put(w, source);
						w.addIFileChangeListener(l);
					} catch (IOException e) {
						Log.error(e);
					}
				}
			} catch (Exception e) {
				Log.error(e);
			}

		}

		if (AbstractAddFrom.class.isInstance(source)) {
			AbstractAddFrom s = (AbstractAddFrom) source;
			// com.neuronrobotics.sdk.common.Log.error("Adding A op for "+s.getClass());
			List<String> namesAdded = s.getNamesAddedInThisOperation();
			// com.neuronrobotics.sdk.common.Log.error(namesAdded.size());
			File f = null;
			IFileChangeListener l = null;
			try {
				f = s.getFile();
				if (f != null) {
					File myFile = f;
					l = new IFileChangeListener() {
						@Override
						public void onFileDelete(File fileThatIsDeleted) {
						}

						@Override
						public void onFileChange(File fileThatChanged, @SuppressWarnings("rawtypes") WatchEvent event) {
							com.neuronrobotics.sdk.common.Log.error("File Change updating " + source.getType());
							Thread tr = myRegenerate(source, this, myFile);
							if (tr != null) {
								try {
									tr.join();
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}

					};

					FileChangeWatcher fileChangeWatcher = null;
					for (FileChangeWatcher w : myWatchers.keySet()) {

						if (myWatchers.get(w) == source)
							fileChangeWatcher = w;

						if (w.getFileToWatch().toPath().compareTo(f.toPath()) == 0)
							fileChangeWatcher = w;

					}

					if (fileChangeWatcher == null) {
						try {
							FileChangeWatcher w = FileChangeWatcher.watch(f);
							myWatchers.put(w, source);
							w.addIFileChangeListener(l);
						} catch (IOException e) {
							// Auto-generated catch block
							com.neuronrobotics.sdk.common.Log.error(e);
						}
					}
				}
			} catch (NoSuchFileException ex) {
				// thats ok
			}

			for (String nameString : namesAdded) {

				CSG n = null;
				for (CSG c : currentState) {
					if (c.isHide() || c.isInGroup())
						continue;
					if (c.getName().contentEquals(nameString))
						n = c;
				}
				if (n == null)
					continue;

				String name = s.getName();
				// com.neuronrobotics.sdk.common.Log.error("Adding Listeners for " + name);
				// new Exception().printStackTrace();
				CSGDatabaseInstance db = ap.get().getCsgDBinstance();
				Set<String> parameters = n.getParameters(db);
				IFileChangeListener myL = l;
				File myFile = f;
				for (String k : parameters) {
					Parameter para = db.get(k);
					// com.neuronrobotics.sdk.common.Log.error("Adding listener to " + k + " on " +
					// nameString);
					db.clearParameterListeners(k);
					db.addParameterListener(k, (name1, p) -> {
						if (LengthParameter.class.isInstance(p)) {
							// new Exception().printStackTrace();
							com.neuronrobotics.sdk.common.Log
									.debug("Value Updating " + p.getName() + " to " + p.getMM());
						}
						CaDoodleFile caDoodleFile = ap.get();
						double percentInitialized = caDoodleFile.getPercentInitialized();
						boolean regenerating = caDoodleFile.isRegenerating();
						if (regenerating || (percentInitialized < 1))
							return;

						getExecutor().submit(() -> {
							// if (useButton)
							// BowlerStudio.runLater(() -> regenerate.setDisable(false));
							// else
							myRegenerate(source, myL, myFile);

						});
					});
				}
			}
		}
	}

	private void displayCurrent(CaDoodleOperation source) {

		@SuppressWarnings("unchecked")
		List<CSG> process = (List<CSG>) CaDoodleLoader.process(ap.get(), true);
		if (ap.get().isRegenerating())
			return;

		List<CSG> currentState = getCurrentState();
		HashMap<CSG, MeshHolder> transport = new HashMap<CSG, MeshHolder>();
		for (CSG c : process) {
			if (c.isHide() || c.isInGroup())
				continue;
			MeshView meshView = c.newMesh();
			MeshView halo = c.newMesh(true);
			transport.put(c, new MeshHolder(meshView, halo, ap.get().getBoundsCache().get(c)));
		}

		BowlerStudio.runLater(() -> {
			if (isAlignActive() && Align.class.isInstance(source))
				getControls().setMode(SpriteDisplayMode.Align);
			else if (isMirrorActive() && Mirror.class.isInstance(source)) {
				getControls().setMode(SpriteDisplayMode.Mirror);
				onMirror();
			} else
				getControls().setMode(SpriteDisplayMode.Default);

			clearScreen();
			resetManipulator();
			controls.resetManipulator();
			// if (isShowConstituants()) {
			// for (CSG c : ap.get().getCurrentState()) {
			// if (c.isInGroup() || c.isHide())
			// c.setIsWireFrame(true);
			// else
			// c.setIsWireFrame(false);
			//
			// displayCSG(c);
			// if ((c.isInGroup() && !c.isAlwaysShow()) || c.isHide())
			// getMeshes().get(c).setMouseTransparent(true);
			// }
			// } else
			for (CSG c : process)
				displayCSG(c, transport.get(c));
			transport.clear();
			ArrayList<CSG> toRemove = new ArrayList<>();
			for (CSG s : getSelected()) {
				boolean exists = false;
				for (CSG c : currentState) {
					if (c.getName().contentEquals(s.getName()) && !c.isInGroup()) {
						exists = true;
						break;
					}
				}

				if (!exists)
					toRemove.add(s);

			}
			if (getSelected().removeAll(toRemove))
				fireSelectionChanged();
			if (workplane != null)
				workplane.updateMeshes(getMeshes());

			updateControlsDisplayOfSelected();
			updateRobotLab.run();
			TickToc.toc();
			TickToc.setEnabled(false);
		});
	}

	public void clearScreen() {

		if (getMeshes() == null)
			return;

		for (CSG c : getMeshes().keySet()) {
			MeshHolder meshHolder = getMeshes().get(c);
			engine.removeUserNode(meshHolder.display);
			try {
				engine.removeUserNode(meshHolder.halo);
			} catch (Exception ex) {
				Log.error(ex);
			}
		}

		getMeshes().clear();
	}

	private void displayCSG(CSG c, MeshHolder holder) {

		MeshView meshView = holder.display;
		MeshView halo = holder.halo;
		Bounds b = holder.bouds;
		double haloDIstance = 1;
		double scalex = 1 - (b.getTotalX() / (b.getTotalX() + haloDIstance));
		double scaley = 1 - (b.getTotalY() / (b.getTotalY() + haloDIstance));
		double scalez = 1 - (b.getTotalZ() / (b.getTotalZ() + haloDIstance));

		TransformNR centerOffset = new TransformNR(b.getCenterX(), b.getCenterY(), b.getCenterZ());
		halo.getTransforms().add(TransformFactory.nrToAffine(centerOffset));
		halo.getTransforms().add(new Scale(1.0 + scalex, 1.0 + scaley, 1.0 + scalez));
		halo.getTransforms().add(TransformFactory.nrToAffine(centerOffset.inverse()));

		halo.setMouseTransparent(true);
		PhongMaterial haloMat = (PhongMaterial) halo.getMaterial();
		haloMat.setDiffuseColor(new Color(0, 0.95, 0.95, 0.45));
		haloMat.setSpecularColor(new Color(0.1, 0.5, 1, 0.));
		halo.setCullFace(CullFace.BACK);
		halo.setDrawMode(DrawMode.FILL);
		halo.setDepthTest(DepthTest.ENABLE);
		halo.setBlendMode(BlendMode.SRC_OVER);
		halo.setVisible(false);

		if (c.isHole() && !c.isWireFrame()) {
			PhongMaterial pm = (PhongMaterial) meshView.getMaterial();
			pm.setDiffuseColor(new Color(0.25, 0.25, 0.25, 0.55));
			pm.setSpecularColor(new Color(0.55, 0.55, 0.55, 1));
			meshView.setCullFace(CullFace.BACK);
			meshView.setDrawMode(DrawMode.FILL);
			meshView.setDepthTest(DepthTest.ENABLE);
			meshView.setBlendMode(BlendMode.SRC_OVER);
		} else {
			PhongMaterial phongMaterial = (PhongMaterial) meshView.getMaterial();
			Color diffuseColor = phongMaterial.getDiffuseColor();
			diffuseColor = Color.color(diffuseColor.getRed(), diffuseColor.getGreen(), diffuseColor.getBlue(),
					diffuseColor.getOpacity());
			phongMaterial.setDiffuseColor(diffuseColor);
			double red = diffuseColor.getRed() + 0.1;
			double green = diffuseColor.getGreen() + 0.1;
			double blue = diffuseColor.getBlue() + 0.1;
			phongMaterial.setSpecularColor(Color.color(red > 1 ? 1 : red, green > 1 ? 1 : green, blue > 1 ? 1 : blue,
					diffuseColor.getOpacity()));
			meshView.setCullFace(CullFace.BACK);
		}

		meshView.setViewOrder(0);
		engine.addUserNode(meshView);
		engine.addUserNode(halo);
		getMeshes().put(c, holder);

		// Allow move on first click for unlocked meshes only
		if (!c.isLock() && !c.isMotionLock() && !c.isInGroup())
			meshView.addEventFilter(MouseEvent.ANY, lockableMouseMover);

		setUpControls(meshView, c);
	}

	private void setUpControls(MeshView meshView, CSG name) {

		if (name == null)
			throw new RuntimeException("Name can not be null");

		meshView.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			if (event.getButton() == MouseButton.PRIMARY) {

				if (event.isShiftDown()) {
					if (isSelected(name))
						removeSelected(name);
					else
						addToSelected(name);
				} else if (!isSelected(name)) {
					getSelected().clear();
					addToSelected(name);
				}

				fireSelectionChanged();
				if (getMode() != SpriteDisplayMode.Align)
					setMode(SpriteDisplayMode.Default);

				updateRobotLab.run();
				updateControlsDisplayOfSelected();

				javafx.scene.Node pickedNode = event.getPickResult().getIntersectedNode();
				if (pickedNode == meshView) {
					Log.debug("Setup Move Starting point");
					Point3D localPoint = event.getPickResult().getIntersectedPoint();

					TransformNR wp = ap.get().getWorkplane();
					TransformNR scenePos = new TransformNR(localPoint.getX(), localPoint.getY(), localPoint.getZ());
					TransformNR wpLocal = wp.inverse().times(scenePos);
					startingPosition3D = new Point3D(wpLocal.getX(), wpLocal.getY(), wpLocal.getZ());
					manipulation.setStartingWorkplanePosition(
							new Point3D(manipulation.snapToGrid(startingPosition3D.getX()),
									manipulation.snapToGrid(startingPosition3D.getY()),
									manipulation.snapToGrid(startingPosition3D.getZ())));
				} else {
					Log.debug("NOT Setup Move Starting point because " + pickedNode + " is not " + meshView);

				}

				// Inform the controls about the total selected object(s) height
				try {
					getControls().setObjectHeight(getSellectedBounds().getTotalZ());
				} catch (Exception ex) {
					Log.error(ex);
				}
				event.consume();
			}
		});
	}

	private void addToSelected(CSG name) {
		if (name.isInGroup()) {
			Log.error(new Exception("Selected an object in a group"));
			return;
		}
		getSelected().add(name);
		meshes.get(name).halo.setVisible(true);
	}

	private boolean isSelected(CSG target) {
		if (target == null)
			return false;
		for (CSG selectedCsg : getSelected()) {
			if (selectedCsg.getName().contentEquals(target.getName()))
				return true;
		}
		return false;
	}

	private void removeSelected(CSG target) {
		if (target == null)
			return;
		Iterator<CSG> iterator = getSelected().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getName().contentEquals(target.getName()))
				iterator.remove();
		}
		meshes.get(target).halo.setVisible(false);
	}

	public void updateControlsDisplayOfSelected() {
		// getExecutor().submit(() -> {
		List<CSG> cs = getCurrentState();
		BowlerStudio.runLater(() -> UpdateUIControls(cs));
		// });
	}

	private void UpdateUIControls(List<CSG> cs) {
		hideHalos();
		resetSelectedCSGsFromCurrentState(cs);
		parametrics.getChildren().clear();
		timeline.updateSelected(getSelected());
		TickToc.tic("Start UpdateUIControls");
		GridPane gp = new GridPane(5, 5);
		int line = 0;
		parametrics.getChildren().add(gp);
		int width = 200;
		int c1Width = 100;
		ColumnConstraints col0 = new ColumnConstraints();
		col0.setHgrow(Priority.NEVER);
		col0.setMinWidth(c1Width);
		col0.setPrefWidth(c1Width);
		col0.setMaxWidth(c1Width);
		ColumnConstraints col1 = new ColumnConstraints();
		col1.setMinWidth(width);
		col1.setHgrow(Priority.ALWAYS);

		gp.getColumnConstraints().setAll(col0, col1);
		if (getSelected().size() > 0) {
			dropToWorkplane.setDisable(false);
			if (objectWorkplane != null)
				objectWorkplane.setDisable(getSelected().size() != 1);

			shapeConfigurationHolder.getChildren().clear();
			shapeConfigurationHolder.getChildren().add(shapeConfigurationBox);
			CSG set = ((CSG) getSelected().toArray()[0]);

			if (set == null)
				return;

			Color value = set.getColor();
			colorPicker.setValue(value);
			String hexColor = String.format(Locale.US, "#%02X%02X%02X", (int) (value.getRed() * 255),
					(int) (value.getGreen() * 255), (int) (value.getBlue() * 255));

			String style = String.format(" -fx-background-color: %s;", hexColor);
			colorPicker.setStyle(style);
			showButtons();
			updateShowHideButton();
			updateLockButton();

			for (CSG c : cs) {
				MeshHolder meshHolder = getMeshes().get(c);

				if (meshHolder != null) {
					MeshView meshView = meshHolder.display;
					meshView.getTransforms().remove(selection);
					meshView.getTransforms().remove(getControls().getViewRotation());
					meshView.removeEventFilter(MouseEvent.ANY, mouseMover);
				}
			}

			TransformFactory.nrToAffine(new TransformNR(), selection);
			TransformFactory.nrToAffine(new TransformNR(), getControls().getViewRotation());
			boolean lockMove = moveLock();
			List<String> selectedSnapshot = selectedSnapshot();
			for (CSG c : getSelectedCSG(selectedSnapshot)) {
				MeshHolder meshHolder = getMeshes().get(c);
				if (meshHolder != null) {
					MeshView meshView = meshHolder.display;
					meshView.getTransforms().addAll(getControls().getViewRotation(), selection);
					if (!c.isLock() && !c.isMotionLock() && !c.isInGroup())
						meshView.addEventFilter(MouseEvent.ANY, mouseMover);
				}
			}

			manipulation.setUnlocked(!lockMove);
			String name = "Shapes" + " (" + getSelected().size() + ")";
			List<CSG> csgs = getSelectedCSG(selectedSnapshot);

			if (csgs.size() == 1) {
				name = csgs.get(0).getUserDefinedName();
				renameBtn.setVisible(ap.isAdvancedMode());
			} else
				renameBtn.setVisible(false);
			shapeConfiguration.setText(name);

			if ((selectedSnapshot.size() == 1) && (csgs.size() > 0)) {
				CSG sel = csgs.get(0);
				List<String> sortedList = new ArrayList<>(sel.getParameters(ap.get().getCsgDBinstance()));
				Collections.sort(sortedList);
				int numCadParaams = 0;
				for (String key : sortedList) {
					if (key.contains("CaDoodle") && (key.contains(sel.getName().split("_")[0]))) {
						numCadParaams++;
						String[] parts = key.split("_");
						String text = parts[parts.length - 1];
						Label e = new Label(text);
						e.setMinWidth(50);
						gp.add(e, 0, line);

						Parameter para = ap.get().getCsgDBinstance().get(key);

						if (StringParameter.class.isInstance(para)) {
							ArrayList<String> opts = para.getOptions();
							if (opts.size() > 0)
								setUpComboBoxParametrics(gp, line, text, para, opts, width);
							else {
								File file = new File(para.getStrValue());
								boolean exists = file.exists();
								if (exists)
									setUpFileBox(gp, line, text, para, width, file);
								else
									setUpTextBoxEnterData(gp, line, text, para, width);
							}
						} else {
							if (para != null)
								setUpNumberChoices(gp, line, text, para, width);
							else
								Log.error("Failed to set parameter in UI " + key);
						}
					}
					line++;
				}

				// if (numCadParaams > 2) {
				// useButton = true;
				// // com.neuronrobotics.sdk.common.Log.error("Using button for regeneration " +
				// // sel.getName());
				// if (!parametrics.getChildren().contains(regenerate))
				// parametrics.getChildren().add(regenerate);
				//
				// EventHandler<ActionEvent> value2 = regenEvents.get(sel.getName());
				// if (value2 != null)
				// regenerate.setOnAction(value2);
				// else
				// com.neuronrobotics.sdk.common.Log.error("ERROR regenerate event is null");
				//
				// } else {
				// useButton = false;
				// parametrics.getChildren().remove(regenerate);
				// }
			}
		} else {
			for (CSG c : cs) {
				MeshHolder meshHolder = getMeshes().get(c);
				if (meshHolder != null) {
					MeshView meshView = meshHolder.display;
					meshView.getTransforms().remove(selection);
					meshView.getTransforms().remove(getControls().getViewRotation());
					meshView.removeEventFilter(MouseEvent.ANY, mouseMover);
				}
			}
			// com.neuronrobotics.sdk.common.Log.error("None selected");
			shapeConfigurationHolder.getChildren().clear();
			hideButtons();
			getControls().clearSelection();
		}
		double volume = 0;
		double sa = 0;
		for (CSG c : getSelected()) {
			volume += c.getVolume();
			sa += c.getSurfaceArea();
			meshes.get(c).halo.setVisible(true);
		}
		if (ap.isAdvancedMode()) {
			// if (getSelected().size() == 1) {
			gp.add(new Label("Material"), 0, line);
			Label massDisp = new Label("0.0");
			Button child = createPrintSettingsButton(getSelected(), massDisp);
			GridPane.setHalignment(child, HPos.RIGHT);
			gp.add(child, 1, line);
			line++;

			gp.add(new Label("Mass"), 0, line);
			GridPane.setHalignment(massDisp, HPos.RIGHT);
			gp.add(massDisp, 1, line);
			line++;
			// }
			setUpTextBox(gp, line++, "Volume", String.format(Locale.US, "%.4f cm^3", volume / 1000.0), width);
			if (getSelected().size() == 1) {
				setUpTextBox(gp, line++, "Area", String.format(Locale.US, "%.4f cm^2", sa / 100), width);
			}
		}
		updateControls();
	}

	private void resetSelectedCSGsFromCurrentState(List<CSG> cs) {
		Set<String> selectedBefore = new HashSet<>(selectedSnapshot());
		List<String> namesAddedInThisOperation = new ArrayList<String>();
		for (CSG c : getSelected()) {
			namesAddedInThisOperation.add(c.getName());
		}
		List<CSG> got = new ArrayList<CSG>();
		for (CSG c : cs) {
			for (String s : namesAddedInThisOperation) {
				if (c.getName().contentEquals(s))
					got.add(c);
			}
		}
		getSelected().clear();
		getSelected().addAll(got);
		if (!selectedBefore.equals(new HashSet<>(selectedSnapshot())))
			fireSelectionChanged();
	}

	public void clearBoundsCache(List<CSG> toclear) {
		Log.debug("Clearing bounds cache " + toclear);
		// Log.error(new Exception());
		if (toclear == null)
			ap.get().getBoundsCache().clear();
		else
			for (CSG c : toclear)
				ap.get().getBoundsCache().remove(c);
	}

	private void setUpNumberField(GridPane gp, int line, String text, Parameter para, int width) {
		ArrayList<String> options3 = para.getOptions();

		TextField options = new TextField();
		options.setEditable(true);
		options.setText(para.getMM() + "");
		options.setMinWidth(width);
		gp.add(options, 1, line);

		options.setOnAction(event -> {
			ArrayList<String> options2 = para.getOptions();
			String string = options.getText().toString();
			try {
				double parseDouble = Double.parseDouble(string);
				if (parseDouble > MAX_NUMBER_FILED) {
					parseDouble = MAX_NUMBER_FILED;
					options.setText(MAX_NUMBER_FILED + "");
				}

				if (parseDouble < -MAX_NUMBER_FILED) {
					parseDouble = -MAX_NUMBER_FILED;
					options.setText(-MAX_NUMBER_FILED + "");
				}
				com.neuronrobotics.sdk.common.Log.debug("Setting new value " + parseDouble);
				para.setMM(parseDouble);
				options2.clear();
			} catch (Throwable t) {
				com.neuronrobotics.sdk.common.Log.error(t);
				options.setText(para.getMM() + "");
			}
			BowlerStudio.runLater(() -> setKeyBindingFocus());

		});
	}

	private void setUpNumberChoices(GridPane gp, int line, String text, Parameter para, int width) {

		ArrayList<String> options2 = para.getOptions();
		boolean limited = false;
		if (options2.size() < 2) {
			setUpNumberField(gp, line, text, para, width);
			return;
		} else
			limited = true;

		ComboBox<String> options = new ComboBox<String>();
		for (String s : options2) {
			options.getItems().add(s);
		}

		options.getItems().add(para.getMM() + "");
		options.setEditable(true);
		options.getSelectionModel().select(para.getMM() + "");
		options.setMinWidth(width);
		gp.add(options, 1, line);

		boolean isLimit = limited;
		options.setOnAction(event -> {
			String string = options.getSelectionModel().getSelectedItem().toString();
			try {
				double parseDouble = Double.parseDouble(string);
				if (isLimit) {
					double top = Double.parseDouble(options2.get(options2.size() - 1));
					double bot = Double.parseDouble(options2.get(0));
					if (parseDouble > top)
						parseDouble = top;

					if (parseDouble < bot)
						parseDouble = bot;
				}
				com.neuronrobotics.sdk.common.Log.debug("Setting new value " + parseDouble);
				para.setMM(parseDouble);
			} catch (Throwable t) {
				com.neuronrobotics.sdk.common.Log.error(t);
				options.getSelectionModel().select(para.getMM() + "");
			}
			BowlerStudio.runLater(() -> setKeyBindingFocus());

		});
	}

	private void setUpTextBox(GridPane gp, int line, String label, String value, int width) {
		gp.add(new Label(label), 0, line);
		Label child = new Label(value);
		GridPane.setHalignment(child, HPos.RIGHT);
		gp.add(child, 1, line);
	}

	private Button createPrintSettingsButton(LinkedHashSet<CSG> linkedHashSet, Label massDisplay) {
		Button button = new Button("Print Settings");
		File f;
		try {
			f = ScriptingEngine.fileFromGit("https://github.com/madhephaestus/CaDoodle-Example-Objects.git",
					"materials_density.json");
		} catch (GitAPIException | IOException e) {
			Log.error(e);
			return button;
		}

		String defType = "FDM";
		String defMat = "PLA";
		String defInfil = 20 + "";
		// for (CSG c : linkedHashSet) {
		// Optional<String> materialType = c.getMaterialType();
		// if (materialType.isPresent())
		// defType = materialType.get();
		// Optional<String> material = c.getMaterial();
		// if (material.isPresent())
		// defMat = material.get();
		// Optional<Double> materiaInfillPercent = c.getMateriaInfillPercent();
		// if (materiaInfillPercent.isPresent())
		// defInfil = materiaInfillPercent.get() + "";
		// }

		// Mutable holders so the lambda can write back
		double[] density = {1.0};

		// --- Parse JSON with Gson ---
		Gson gson = new Gson();
		JsonObject root;
		try (FileReader reader = new FileReader(f)) {
			root = gson.fromJson(reader, JsonObject.class);
		} catch (Exception e) {
			e.printStackTrace();
			return new Button("Print Settings");
		}

		// --- Helper to build button label ---
		// Declared as an array so lambdas below can call it
		Runnable[] updateLabel = {null};
		// --- Label updater ---
		updateLabel[0] = () -> {
			double mass = 0;
			String label = "";
			double localDensity = 1.0;
			for (CSG c : linkedHashSet) {
				String type = c.getMaterialType().orElse("FDM"); // raw type, keep pristine
				String material = c.getMaterial().orElse("PLA");
				double infillPct = c.getMateriaInfillPercent().orElse(20.0);
				String infill = (int) infillPct + "";

				// Build display label separately — don't corrupt 'type'
				label = type + " / " + material;
				if ("FDM".equals(type))
					label += " / " + infill + "%";

				// Look up density using the original type key

				if (root.has(type)) {
					JsonObject section = root.get(type).getAsJsonObject();
					if (section.has(material)) {
						JsonObject matObj = section.get(material).getAsJsonObject();
						if (matObj.has("density_g_cm3")) {
							localDensity = matObj.get("density_g_cm3").getAsDouble();
							// Now 'type' is still "FDM", so this check works correctly
							if ("FDM".equals(type) && section.has(infill)) {
								JsonObject infillObj = section.get(infill).getAsJsonObject();
								if (infillObj.has("effective_density_multiplier"))
									localDensity *= infillObj.get("effective_density_multiplier").getAsDouble();
							}
						}
					}
				}
				mass += (c.getVolume() * localDensity / 1000.0);
			}
			if (linkedHashSet.size() == 1)
				button.setText(label + " \n " + String.format(Locale.US, "%.4f g/cm^3", localDensity));
			else
				button.setText("Asorted");
			massDisplay.setText(String.format(Locale.US, "%.4f g", mass));
		};
		if (linkedHashSet.size() == 1) {
			CSG singleCSG = linkedHashSet.iterator().next();

			button.setOnAction(e -> showStagedMaterialPopup(button, singleCSG, root, updateLabel));
		}
		// 4. Set initial label
		updateLabel[0].run();

		if (linkedHashSet.size() > 1)
			button.setDisable(true);
		return button;

	}

	private void showStagedMaterialPopup(Button anchor, CSG csg, JsonObject root, Runnable[] updateLabel) {
		Popup popup = new Popup();
		popup.setAutoHide(true);
		showTypeStep(popup, anchor, csg, root, updateLabel);
		popup.show(anchor, anchor.localToScreen(0, 0).getX(), anchor.localToScreen(0, 0).getY() + anchor.getHeight());
	}

	private static class PopupShell {
		final GridPane buttonBar;
		final GridPane content;

		PopupShell(GridPane buttonBar, GridPane content) {
			this.buttonBar = buttonBar;
			this.content = content;
		}
	}

	private PopupShell makePopupShell(String title, Popup popup) {
		Label header = new Label(title);
		Button cancel = new Button("Cancel");
		cancel.setOnAction(e -> popup.hide());

		GridPane buttonBar = new GridPane();
		buttonBar.setHgap(4);
		buttonBar.setVgap(4);
		buttonBar.add(header, 0, 0, 2, 1); // span 2 cols
		buttonBar.add(new Separator(), 0, 1, 2, 1);
		buttonBar.add(cancel, 0, 2);
		ActiveProject.setStyleSheet(buttonBar);

		GridPane content = new GridPane();
		content.setHgap(4);
		content.setVgap(4);
		ActiveProject.setStyleSheet(content);

		ScrollPane scroll = new ScrollPane(content);
		scroll.setFitToWidth(true);
		scroll.setPrefHeight(300);
		ActiveProject.setStyleSheet(scroll);

		GridPane root = new GridPane();
		root.setVgap(4);
		root.add(buttonBar, 0, 0);
		root.add(scroll, 0, 1);
		ActiveProject.setStyleSheet(root);
		// root.setBackground(new Background(new BackgroundFill(Color.DARKGRAY,
		// CornerRadii.EMPTY, Insets.EMPTY)));
		root.getStyleClass().add("anchor-pane");
		popup.getContent().setAll(root);
		return new PopupShell(buttonBar, content);
	}

	private void showTypeStep(Popup popup, Button anchor, CSG csg, JsonObject root, Runnable[] updateLabel) {
		PopupShell shell = makePopupShell("Step 1 of 3 — Select Type", popup);

		ToggleGroup group = new ToggleGroup();
		String currentType = csg.getMaterialType().orElse("FDM");

		Button continueBtn = new Button("Continue");
		continueBtn.setOnAction(e -> {
			Toggle selected = group.getSelectedToggle();
			if (selected == null)
				return;
			String typeName = ((RadioButton) selected).getText();
			csg.setMaterialType(typeName);
			popup.getContent().clear();
			showMaterialStep(popup, anchor, csg, root, updateLabel, typeName);
		});
		// Continue goes in col 1, row 2 (next to Cancel)
		shell.buttonBar.add(continueBtn, 1, 2);

		int row = 0;
		for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
			RadioButton btn = new RadioButton(entry.getKey());
			btn.setToggleGroup(group);
			if (entry.getKey().equals(currentType))
				btn.setSelected(true);
			btn.setOnAction(e -> {
				String typeName = btn.getText();
				csg.setMaterialType(typeName);
				popup.getContent().clear();
				showMaterialStep(popup, anchor, csg, root, updateLabel, typeName);
			});
			shell.content.add(btn, 0, row++);
			boolean typeIsDefault = !csg.getMaterialType().isPresent();
			// ...
			if (typeIsDefault && entry.getKey().equals(currentType))
				btn.setSelected(true);
		}

	}

	private void showMaterialStep(Popup popup, Button anchor, CSG csg, JsonObject root, Runnable[] updateLabel,
			String typeName) {
		boolean isFDM = "FDM".equals(typeName);
		String stepLabel = isFDM ? "Step 2 of 3 — Select Material" : "Step 2 of 2 — Select Material";
		PopupShell shell = makePopupShell(stepLabel, popup);

		ToggleGroup group = new ToggleGroup();
		JsonObject section = root.get(typeName).getAsJsonObject();
		String currentMat = csg.getMaterial().orElse("PLA");

		Button back = new Button("← Back");
		back.setOnAction(e -> {
			popup.getContent().clear();
			showTypeStep(popup, anchor, csg, root, updateLabel);
		});

		Button continueBtn = new Button("Continue");
		continueBtn.setOnAction(e -> {
			Toggle selected = group.getSelectedToggle();
			if (selected == null)
				return;
			String material = ((RadioButton) selected).getText();
			csg.setMaterial(material);
			if (isFDM) {
				popup.getContent().clear();
				showInfillStep(popup, anchor, csg, root, updateLabel, typeName, material);
			} else {
				ap.addOp(
						new SetMaterial().setNames(selectedSnapshot()).setMaterialType(typeName).setMaterial(material));
				if (updateLabel[0] != null)
					updateLabel[0].run();
				popup.hide();
			}

		});

		// row 2: Cancel(col0) Back(col1) Continue(col2)
		shell.buttonBar.add(back, 2, 2);
		shell.buttonBar.add(continueBtn, 1, 2);
		// widen header and separator to 3 cols now
		GridPane.setColumnSpan(shell.buttonBar.getChildren().get(0), 3); // header
		GridPane.setColumnSpan(shell.buttonBar.getChildren().get(1), 3); // separator

		int row = 0;
		for (Map.Entry<String, JsonElement> entry : section.entrySet()) {
			if (!entry.getValue().getAsJsonObject().has("density_g_cm3"))
				continue;
			RadioButton btn = new RadioButton(entry.getKey());
			btn.setToggleGroup(group);
			if (entry.getKey().equals(currentMat))
				btn.setSelected(true);
			btn.setOnAction(e -> {
				String material = btn.getText();
				csg.setMaterial(material);
				if (isFDM) {
					popup.getContent().clear();
					showInfillStep(popup, anchor, csg, root, updateLabel, typeName, material);
				} else {
					ap.addOp(new SetMaterial().setNames(selectedSnapshot()).setMaterialType(typeName)
							.setMaterial(material));
					if (updateLabel[0] != null)
						updateLabel[0].run();
					popup.hide();
				}
			});
			shell.content.add(btn, 0, row++);
			boolean matIsDefault = !csg.getMaterial().isPresent();
			// ...
			if (matIsDefault && entry.getKey().equals(currentMat))
				btn.setSelected(true);

		}
	}

	private void showInfillStep(Popup popup, Button anchor, CSG csg, JsonObject root, Runnable[] updateLabel,
			String typeName, String materialName) {
		PopupShell shell = makePopupShell("Step 3 of 3 — Select Infill %", popup);

		ToggleGroup group = new ToggleGroup();
		JsonObject section = root.get(typeName).getAsJsonObject();

		List<String> infillKeys = new ArrayList<>();
		for (Map.Entry<String, JsonElement> entry : section.entrySet()) {
			try {
				int val = Integer.parseInt(entry.getKey());
				if (val >= 10 && val <= 100)
					infillKeys.add(entry.getKey());
			} catch (NumberFormatException ignored) {
			}
		}
		Collections.sort(infillKeys, Comparator.comparingInt(Integer::parseInt));

		int currentInfill = csg.getMateriaInfillPercent().map(v -> (int) v.doubleValue()).orElse(20);

		Button back = new Button("← Back");
		back.setOnAction(e -> {
			popup.getContent().clear();
			showMaterialStep(popup, anchor, csg, root, updateLabel, typeName);
		});

		Button continueBtn = new Button("Continue");
		continueBtn.setOnAction(e -> {
			Toggle selected = group.getSelectedToggle();
			if (selected == null)
				return;
			int val = Integer.parseInt(((RadioButton) selected).getText().replace("%", ""));
			csg.setMaterialInfillPercent(val);
			ap.addOp(new SetMaterial().setNames(selectedSnapshot()).setMaterialType(typeName).setMaterial(materialName)
					.setInfillPercent(val));
			if (updateLabel[0] != null)
				updateLabel[0].run();
			popup.hide();
		});

		// row 2: Cancel(col0) Back(col1) Continue(col2)
		shell.buttonBar.add(back, 2, 2);
		shell.buttonBar.add(continueBtn, 1, 2);
		GridPane.setColumnSpan(shell.buttonBar.getChildren().get(0), 3); // header
		GridPane.setColumnSpan(shell.buttonBar.getChildren().get(1), 3); // separator

		int row = 0;
		for (String pct : infillKeys) {
			RadioButton btn = new RadioButton(pct + "%");
			btn.setToggleGroup(group);
			if (Integer.parseInt(pct) == currentInfill)
				btn.setSelected(true);
			btn.setOnAction(e -> {
				int val = Integer.parseInt(btn.getText().replace("%", ""));
				csg.setMaterialInfillPercent(val);
				ap.addOp(new SetMaterial().setNames(selectedSnapshot()).setMaterialType(typeName)
						.setMaterial(materialName).setInfillPercent(val));
				if (updateLabel[0] != null)
					updateLabel[0].run();
				popup.hide();
			});
			shell.content.add(btn, 0, row++);
			boolean infillIsDefault = !csg.getMateriaInfillPercent().isPresent();
			// int currentInfill = csg.getMateriaInfillPercent().orElse(20.0).intValue();
			// ...
			if (infillIsDefault && Integer.parseInt(pct) == currentInfill)
				btn.setSelected(true);
		}
	}

	/**
	 * setUpTextBoxEnterData
	 *
	 * @param gp
	 * @param line
	 * @param text
	 * @param para
	 * @param width
	 */
	private void setUpTextBoxEnterData(GridPane gp, int line, String text, Parameter para, int width) {
		TextField tf = new TextField(para.getStrValue());
		tf.setOnAction(event -> {
			para.setStrValue(tf.getText());
			BowlerStudio.runLater(() -> setKeyBindingFocus());
		});

		gp.add(tf, 1, line);
	}

	private void setUpFileBox(GridPane gp, int line, String text, Parameter para, int width, File file) {
		// Button tf = new Button(new File(para.getStrValue()).getName());
		ExternalEditorController ec = new ExternalEditorController(file, new CheckBox(), () -> {
			if (file.getName().endsWith("doodle")) {
				ConfigurationDatabase.put("CaDoodle", "CaDoodleActiveFile", ap.get().getSelf().getAbsolutePath());
				ConfigurationDatabase.save();
			} else {
				// Force an update when the parameters are loaded,
				// this should also switch from STL to blender file loading if the blend is
				// generated
				CopyOnWriteArrayList<IParameterChanged> listeners = para.getInstance()
						.getParamListeners(para.getName());
				for (IParameterChanged l : listeners)
					l.parameterChanged(para.getName(), para);

			}
			BowlerStudio.runLater(() -> setKeyBindingFocus());
		});

		Node tf = ec.getControl();
		gp.add(tf, 1, line);
	}

	private void setUpComboBoxParametrics(GridPane gp, int line, String text, Parameter para, ArrayList<String> opts,
			int width) {
		// ComboBox<String> options = new ComboBox<String>();
		// if (para.getName().toLowerCase().endsWith("font")) {
		// options.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
		// @Override
		// protected void updateItem(String item, boolean empty) {
		// super.updateItem(item, empty);
		// setFont(new Font(item,Font.getDefault().getSize()));
		// setText(item);
		// }
		// });
		//
		// options.setButtonCell(new javafx.scene.control.ListCell<String>() {
		// @Override
		// protected void updateItem(String item, boolean empty) {
		// super.updateItem(item, empty);
		// setFont(new Font(item,Font.getDefault().getSize()));
		// setText(item);
		// }
		// });
		// }

		ComboBox<String> options = new ComboBox<String>();

		if (para.getName().toLowerCase().endsWith("font")) {

			javafx.util.Callback<ListView<String>, ListCell<String>> cellFactory = lv -> new javafx.scene.control.ListCell<String>() {
				@Override
				protected void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setText(null);
						setFont(Font.getDefault());
					} else {
						setFont(new Font(item, FontSizeManager.getDefaultSize()));
						setText(item);
					}
				}
			};

			options.setCellFactory(cellFactory);

			options.setButtonCell(new javafx.scene.control.ListCell<String>() {
				@Override
				protected void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setText(null);
						setFont(Font.getDefault());
					} else {
						setFont(new Font(item, FontSizeManager.getDefaultSize()));
						setText(item);
					}
				}
			});

			if (ap.isAdvancedMode()) {
				opts = new ArrayList<String>(Font.getFontNames());
			}
		}

		for (String s : opts) {
			options.getItems().add(s);
		}
		BowlerStudio.runLater(() -> {
			options.getSelectionModel().select(para.getStrValue());
			gp.add(options, 1, line);
			options.setMinWidth(width);
			options.setOnAction(event -> {
				com.neuronrobotics.sdk.common.Log.debug(System.currentTimeMillis() + " Event " + event);
				para.setStrValue(options.getSelectionModel().getSelectedItem());
				BowlerStudio.runLater(() -> setKeyBindingFocus());
			});
		});
	}

	private CSG getSelectedCSG(String string) {
		for (CSG c : getSelectable())
			if (c.getName().contentEquals(string))
				return c;

		return null;
	}

	public void set(Label shapeConfiguration, Accordion shapeConfigurationBox, AnchorPane shapeConfigurationHolder,
			GridPane configurationGrid, AnchorPane control3d, BowlerStudio3dEngine engine, ColorPicker colorPicker,
			ComboBox<String> snapGrid, VBox parametrics, Button lockButton, ImageView lockImage,
			MenuButton advancedGroupMenu, TimelineManager tm, Button objectWorkplane, Button dropToWorkplane,
			ProgressIndicator memUsage, Button renameBtn) {
		this.shapeConfiguration = shapeConfiguration;
		this.shapeConfigurationBox = shapeConfigurationBox;
		this.shapeConfigurationHolder = shapeConfigurationHolder;
		this.configurationGrid = configurationGrid;
		this.control3d = control3d;
		this.engine = engine;
		this.colorPicker = colorPicker;
		this.snapGrid = snapGrid;
		this.parametrics = parametrics;
		this.lockButton = lockButton;
		this.lockImage = lockImage;
		this.advancedGroupMenu = advancedGroupMenu;
		this.timeline = tm;
		this.objectWorkplane = objectWorkplane;
		this.dropToWorkplane = dropToWorkplane;
		this.memUsage = memUsage;
		this.renameBtn = renameBtn;
		setupSnapGrid();

	}

	public void clearAlignObjectCache() {
		getControls().clearAlign(ap.get().getBoundsCache());
	}

	private void setupSnapGrid() {
		double inch = 25.4; // 1 inch is 25.4mm
		List<String> grids = Arrays.asList(String.format(Locale.US, "%.4f mm", 0.1000),
				String.format(Locale.US, "%.4f mm", 0.2500), String.format(Locale.US, "%.4f mm", 0.5000),
				String.format(Locale.US, "%.4f mm", 1.0000), String.format(Locale.US, "%.4f mm 1/16 inch", inch / 16.0),
				String.format(Locale.US, "%.4f mm", 2.0000), String.format(Locale.US, "%.4f mm Pi", 3.1416),
				String.format(Locale.US, "%.4f mm 1/8 inch", inch / 8.0), String.format(Locale.US, "%.4f mm", 5.0000),
				String.format(Locale.US, "%.4f mm 1/4 inch", inch / 4.0),
				String.format(Locale.US, "%.4f mm Brick", 8.0000), String.format(Locale.US, "%.4f mm", 10.000),
				String.format(Locale.US, "%.4f mm 1/2 inch", inch / 2.0), String.format(Locale.US, "%.4f mm", 20.000),
				String.format(Locale.US, "%.4f mm 1 inch", inch));

		HashMap<String, Double> map = new HashMap<>();
		map.put("Off", 0.001);
		this.snapGrid.getItems().add("Off");
		for (String s : grids) {
			Number n = Double.parseDouble(s.split(" ")[0]);
			String key = s;
			map.put(key, n.doubleValue());
			this.snapGrid.getItems().add(key);
		}

		for (String key : map.keySet()) {
			Double num = map.get(key);
			if (Math.abs(getSnapGridValue() - num.doubleValue()) < 0.001) {
				snapGrid.getSelectionModel().select(key);
				break;
			}
		}

		this.snapGrid.setOnAction(event -> {
			String selected = this.snapGrid.getSelectionModel().getSelectedItem();
			Double num = map.get(selected);
			if (num != null) {
				setSnapGrid(num);
				com.neuronrobotics.sdk.common.Log.debug("Snap Grid Set to " + num);
				setKeyBindingFocus();
			}
		});
	}

	public void addSelectionListener(Runnable listener) {
		selectionListeners.add(listener);
	}

	private void fireSelectionChanged() {
		for (Runnable r : selectionListeners)
			r.run();
	}

	public void clearSelection() {
		cancelOperationModes();
		getSelected().clear();

		fireSelectionChanged();
		updateControlsDisplayOfSelected();
		updateRobotLab.run();
		setKeyBindingFocus();
	}

	public void selectAll(Iterable<String> names) {
		getExecutor().submit(() -> {
			selectAllFromCurrentState(names);
			setKeyBindingFocus();
		});
	}

	public void selectAllFromCurrentState(Iterable<String> names) {
		getSelected().clear();
		for (CSG c : getCurrentState()) {

			if ((c.isInGroup() && !c.isAlwaysShow()))
				continue;

			if (c.isHide())
				continue;

			for (String s : names)
				if (s.contentEquals(c.getName())) {
					addToSelected(c);
					break;
				}
		}

		fireSelectionChanged();
		BowlerStudio.runLater(() -> {
			updateControlsDisplayOfSelected();
			setKeyBindingFocus();
		});

		updateRobotLab.run();
	}

	public void selectAll() {
		getExecutor().submit(() -> {
			getSelected().clear();

			for (CSG c : getCurrentState()) {

				if (c.isInGroup() && !c.isAlwaysShow())
					continue;

				if (c.isHide())
					continue;

				addToSelected(c);
			}

			fireSelectionChanged();
			BowlerStudio.runLater(() -> {
				updateControlsDisplayOfSelected();
			});
			updateRobotLab.run();
		});
	}

	public boolean setKeyBindingFocus() {
		if (!SplashManager.isVisibleSplash() && (engine != null)) {
			// new Exception("KB Focused here").printStackTrace();
			com.neuronrobotics.sdk.common.Log.debug("Setting KeyBindingFocus");
			BowlerStudio.runLater(() -> engine.requestFocus());
			return true;
		} else {
			com.neuronrobotics.sdk.common.Log.error("Rejecting focus KeyBindingFocus"
					+ (SplashManager.isVisibleSplash() ? " Splash open" : "") + (engine == null ? " Engine null" : ""));
		}
		return false;
	}

	public void setToHole() {
		getExecutor().submit(() -> {
			LinkedHashSet<CSG> selected2 = getSelected();
			if (selected2.size() == 0)
				return;

			boolean isSolid = false;
			for (CSG s : selected2) {
				if (!s.isHole()) {
					isSolid = true;
					break;
				}
			}

			if (isSolid) { // some solid
				ToHole h = new ToHole().setNames(selectedSnapshot());
				try {
					addOp(h).join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	public void setToSolid() {

		getExecutor().submit(() -> {

			if (getSelected().size() == 0)
				return;

			boolean isSolid = true;
			for (CSG s : getSelected()) {
				CSG selectedCSG = s;

				if ((selectedCSG != null) && (selectedCSG.isHole())) {
					isSolid = false;
					break;
				}
			}

			if (isSolid)
				return; // all solid

			ToSolid h = new ToSolid().setNames(selectedSnapshot());
			try {
				addOp(h).join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

	}

	public void toggleTransparent() {
		getExecutor().submit(() -> {
			com.neuronrobotics.sdk.common.Log.debug("Toggel transparent");
			ArrayList<ToSolid> toChange = new ArrayList<>();

			for (Iterator<CSG> iterator = getSelected().iterator(); iterator.hasNext();) {
				CSG s = iterator.next();
				CSG c = s;

				if (!c.isHole()) {
					MeshHolder meshHolder = getMeshes().get(c);
					if (meshHolder == null)
						continue;
					MeshView mesh = meshHolder.display;

					PhongMaterial phongMaterial = (PhongMaterial) mesh.getMaterial();
					Color diffuseColor = phongMaterial.getDiffuseColor();
					double opacity = diffuseColor.getOpacity();

					if (opacity < 1)
						opacity = 1;
					else
						opacity = 0.35;

					diffuseColor = Color.color(diffuseColor.getRed(), diffuseColor.getGreen(), diffuseColor.getBlue(),
							opacity);
					phongMaterial.setDiffuseColor(diffuseColor);
					mesh.setMaterial(phongMaterial);
					ToSolid solid = new ToSolid().setNames(Arrays.asList(c.getName())).setColor(diffuseColor);
					toChange.add(solid);
				}
			}

			for (ToSolid solid : toChange) {
				try {
					addOp(solid).join();
				} catch (InterruptedException e) {
					// Auto-generated catch block
					com.neuronrobotics.sdk.common.Log.error(e);
				}
			}
		});
	}

	public boolean isSelectedTransparent() {
		double opacity = 0;
		for (CSG c : getSelected())
			if (!c.isHole())
				opacity += ((PhongMaterial) c.getMesh().getMaterial()).getDiffuseColor().getOpacity();

		return (opacity / (double) getSelected().size()) < 0.999;
	}

	public void setColor(Color value) {
		ToSolid solid = new ToSolid().setNames(selectedSnapshot()).setColor(value);
		addOp(solid);
	}

	public TransformNR getFocusCenter() {
		if (getSelected().size() == 0)
			return new TransformNR();

		Bounds b;
		try {
			b = getSellectedBounds();
			TransformNR tf = new TransformNR(b.getCenterX(), b.getCenterY(), b.getCenterZ());
			TransformNR wp = ap.get().getWorkplane();
			return wp.times(tf);
		} catch (BoundsComputFailure e) {
			Log.error(e);
		}
		return new TransformNR();
	}

	public Thread addOp(CaDoodleOperation h) {
		if (ap.get() == null)
			return null;

		if (ap.get().isOperationRunning()) {
			com.neuronrobotics.sdk.common.Log.error("Ignoring operation because previous had not finished!");
			return null;
		}
		// com.neuronrobotics.sdk.common.Log.error("Adding " + h.getType());
		return ap.addOp(h);
	}

	public void setButtons(Button... buttonsList) {
		buttons = Arrays.asList(buttonsList);
		hideButtons();
	}

	private void hideButtons() {

		BowlerStudio.runLater(() -> {
			for (Button b : buttons)
				b.setDisable(true);

			if ((robotLabDrawer != null) && !robotLabOpen)
				robotLabDrawer.setDisable(true);

			if (ungroupButton != null)
				ungroupButton.setDisable(true);

			if (groupButton != null)
				groupButton.setDisable(true);

			if (alignButton != null)
				alignButton.setDisable(true);

			if (advancedGroupMenu != null)
				advancedGroupMenu.setDisable(true);

			if (dropToWorkplane != null)
				dropToWorkplane.setDisable(true);

			if (objectWorkplane != null)
				objectWorkplane.setDisable(true);
			if (hexDistributeButton != null)
				hexDistributeButton.setDisable(true);
			// if (filletButton != null)
			// filletButton.setDisable(true);
			// if (extrudeButton != null)
			// extrudeButton.setDisable(true);
			if (boltHoleButton != null)
				boltHoleButton.setDisable(true);
		});
	}

	private void showButtons() {

		BowlerStudio.runLater(() -> {

			for (Button b : buttons)
				b.setDisable(false);

			int unlockedSelected = 0;

			for (CSG c : getSelected())
				if (!c.isLock())
					unlockedSelected++;

			if (unlockedSelected > 1) {
				groupButton.setDisable(false);
				alignButton.setDisable(false);
			}

			if ((getSelected().size() > 0) && advanced) {
				advancedGroupMenu.setDisable(false);
				robotLabDrawer.setDisable(false);
				hexDistributeButton.setDisable(false);
				// filletButton.setDisable(false);
				// extrudeButton.setDisable(false);
				boltHoleButton.setDisable(false);
			}

			if (isAGroupSelected())
				ungroupButton.setDisable(false);

		});
	}

	private boolean isAGroupSelected() {

		for (CSG c : getSelected())
			if ((c != null) && c.isGroupResult())
				return true;

		return false;
	}

	public void setUngroup(Button ungroupButton) {
		this.ungroupButton = ungroupButton;
	}

	public void setGroup(Button groupButton) {
		this.groupButton = groupButton;
	}

	public void onDelete() {

		if (ap.get().isOperationRunning()) {
			com.neuronrobotics.sdk.common.Log.error("Ignoring operation because previous had not finished!");
			return;
		}

		com.neuronrobotics.sdk.common.Log.error("Delete");
		ap.addOp(new Delete().setNames(selectedSnapshot()));
	}

	public void setCopyListToCurrentSelected() {
		com.neuronrobotics.sdk.common.Log.debug("Copy Called");
		copySetinternal = selectedSnapshot();
	}

	public void Duplicate() {
		Paste paste = new Paste().setNames(selectedSnapshot());
		paste.setLocation(new TransformNR(0, 0, 0));
		com.neuronrobotics.sdk.common.Log.debug("Duplicate called ");
		getExecutor().submit(() -> performPaste(paste));
	}

	public void onPaste() {
		Paste paste = new Paste().setNames(copySetinternal);
		paste.setLocation(new TransformNR(20, 0, 0));
		com.neuronrobotics.sdk.common.Log.debug("Paste called");
		getExecutor().submit(() -> performPaste(paste));
	}

	public void runBoltHole() {
		RadialDistribution dist = new RadialDistribution().setWorkplane(getWorkplane()).setNames(selectedSnapshot());

		getExecutor().submit(() -> performPaste(dist));
	}

	public void runHexDistribute() {
		LinearDistribution dist = new LinearDistribution().setWorkplane(getWorkplane()).setNames(selectedSnapshot());

		getExecutor().submit(() -> performPaste(dist));
	}

	private void performPaste(AbstractAddFrom op) {
		if (ap.get().isOperationRunning()) {
			com.neuronrobotics.sdk.common.Log.error("Ignoring operation because previous had not finished!");
			return;
		}

		try {

			ap.addOp(op).join();
			List<String> namesAdded = op.getNamesAddedInThisOperation();
			ArrayList<String> namesBack = new ArrayList<String>();
			for (CSG c : getSelectedCSG(namesBack)) {
				if (c.isInGroup())
					continue;
				namesBack.add(c.getName());
			}

			selectAll(namesAdded);
			setCopyListToCurrentSelected();
			BowlerStudio.runLater(() -> updateControlsDisplayOfSelected());
			updateRobotLab.run();
		} catch (CadoodleConcurrencyException | InterruptedException e) {
			// Auto-generated catch block
			com.neuronrobotics.sdk.common.Log.error(e);
		}

	}

	public void onCruise() {
		TransformNR wp = ap.get().getWorkplane();
		List<String> selectedSnapshot = selectedSnapshot();
		if (selectedSnapshot.size() == 0) {
			// new RuntimeException("Cruise called with nothing
			// selected").printStackTrace();
			return;
		}

		getExecutor().submit(() -> {
			List<CSG> selectedCSG = getSelectedCSG(selectedSnapshot);
			List<CSG> cur = getCurrentStateSelected();
			Platform.runLater(() -> {

				if (!moveLock()) {
					com.neuronrobotics.sdk.common.Log.error("On Cruise");
					CSG indicator = selectedCSG.get(0);
					if (selectedCSG.size() > 1)
						indicator = CSG.unionAll(selectedCSG);

					List<String> seleectedNames = selectedSnapshot();
					TransformNR o = new TransformNR(RotationNR.getRotationZ(90));

					TransformNR g = o.times(wp.inverse());
					Transform tf = TransformFactory.nrToCSG(g);
					Bounds bounds = indicator.transformed(tf).getBounds();
					Vector3d center = bounds.getCenter();
					TransformNR b = new TransformNR(-center.x, -center.y, -bounds.getMin().z);
					g = b.times(g);

					Affine gemoAffine = new Affine();
					TransformNR copy = g.copy();
					BowlerKernel.runLater(() -> {
						TransformFactory.nrToAffine(copy, gemoAffine);
					});

					for (CSG c : selectedCSG) {
						MeshHolder meshHolder = getMeshes().get(c);

						if (meshHolder != null)
							BowlerKernel.runLater(() -> {
								MeshView meshView = meshHolder.display;
								if (!isResizeLiveMode())
									meshView.setVisible(false);
							});
					}

					workplane.setIndicator(indicator, gemoAffine);

					workplane.setOnSelectEvent(() -> {

						for (CSG c : selectedCSG) {
							MeshView meshView = getMeshes().get(c).display;

							if (meshView != null)
								BowlerKernel.runLater(() -> meshView.setVisible(true));

						}

						if (workplane.isClicked()) {
							// Move the workplane down from the surface to ensure a solid overlap between
							// the object and the surface
							TransformNR downset = new TransformNR(0, 0, -Plane.getEPSILON() * 100);
							TransformNR currentAbsolutePose = workplane.getCurrentAbsolutePose().times(downset);

							TransformNR finalLocation = currentAbsolutePose.times(copy);
							try {
								ap.addOp(
										new MoveCenter().setNames(seleectedNames, ap.get()).setLocation(finalLocation));
							} catch (InvalidLocationMove e) {
								Log.error(e);
							}
						}
					});

					workplane.setCurrentAbsolutePose(copy.inverse());
					workplane.activate();
				}
			});
		});

	}

	public void lockToggle() {
		if (ap.get().isOperationRunning()) {
			com.neuronrobotics.sdk.common.Log.error("Ignoring operation because previous had not finished!");
			return;
		}

		List<String> selectedSnapshot = selectedSnapshot();
		if (!isLocked(selectedSnapshot))
			ap.addOp(new Lock().setNames(selectedSnapshot));
		else
			ap.addOp(new UnLock().setNames(selectedSnapshot));

	}

	public void wireMeshModeToggle() {
		if (ap.get().isOperationRunning()) {
			com.neuronrobotics.sdk.common.Log.error("Ignoring operation because previous had not finished!");
			return;
		}

		List<String> selectedSnapshot = selectedSnapshot();

		ap.addOp(new WireMeshView().setNames(selectedSnapshot).setToMesh(!isWireMesh(selectedSnapshot)));

	}

	private boolean isLocked(List<String> s) {
		for (String name : s) {
			CSG c = getSelectedCSG(name);
			if ((c != null) && c.isLock())
				return true;
		}
		return false;
	}

	private boolean isWireMesh(List<String> s) {
		for (String name : s) {
			CSG c = getSelectedCSG(name);
			if ((c != null) && c.isWireFrame())
				return true;
		}
		return false;
	}

	public boolean isLocked() {
		return isLocked(selectedSnapshot());
	}

	public void showAll() {
		if (ap.get().isOperationRunning()) {
			com.neuronrobotics.sdk.common.Log.error("Ignoring operation because previous had not finished!");
			return;
		}

		ArrayList<String> toShow = new ArrayList<String>();
		for (CSG c : getCurrentState())
			if (c.isHide())
				toShow.add(c.getName());

		if (toShow.size() > 0)
			ap.addOp(new Show().setNames(toShow));

	}

	public void onXor() {
		if (ap.get().isOperationRunning()) {
			com.neuronrobotics.sdk.common.Log.error("Ignoring operation because previous had not finished!");
			return;
		}

		if (getSelected().size() > 1) {
			getExecutor().submit(() -> {
				try {
					List<String> selectedSnapshot = selectedSnapshot();
					Paste copy = new Paste().setNames(selectedSnapshot);
					ap.addOp(copy).join();
					ArrayList<String> n = new ArrayList<>(copy.getNamesAddedInThisOperation());
					Group groups = new Group().setNames(selectedSnapshot);
					groups.setHull(false);
					groups.setIntersect(true);
					ap.addOp(groups).join();
					List<CSG> results = ap.get().getCurrentState();
					String intersectName = groups.getGroupID();
					ArrayList<String> names = new ArrayList<>();
					names.add(intersectName);
					ToHole th = new ToHole().setNames(names);
					ap.addOp(th).join();

					for (int i = 0; i < n.size(); i++) {
						String e = n.get(i);
						ap.get();
						CSG g = null;

						try {
							g = CaDoodleFile.getByName(ap.get().getCurrentState(), e);
						} catch (NameMissingException e1) {
							continue;
						}

						if ((g == null) || g.isInGroup())
							continue;

						ArrayList<String> namesToDiff = new ArrayList<String>();
						namesToDiff.add(intersectName);
						namesToDiff.add(e);
						Group cutIntersect = new Group().setNames(namesToDiff);
						ap.addOp(cutIntersect).join();

					}

					getSelected().clear();
					getSelected().addAll(results);
					fireSelectionChanged();
					BowlerStudio.runLater(() -> updateControlsDisplayOfSelected());
					updateRobotLab.run();
				} catch (CadoodleConcurrencyException e) {
					// Auto-generated catch block
					com.neuronrobotics.sdk.common.Log.error(e);
				} catch (InterruptedException e) {
					// Auto-generated catch block
					com.neuronrobotics.sdk.common.Log.error(e);
				}
			});
		} else {
			updateControlsDisplayOfSelected();
			updateRobotLab.run();
		}

	}

	public void onGroup(boolean hull, boolean intersect) {
		if (ap.get().isOperationRunning()) {
			com.neuronrobotics.sdk.common.Log.error("Ignoring operation because previous has not finished!");
			return;
		}

		if ((getSelected().size() > 1) || hull) {

			getExecutor().submit(() -> {
				Group groups = new Group().setNames(selectedSnapshot());
				groups.setHull(hull);
				groups.setIntersect(intersect);
				try {
					ap.addOp(groups).join();
					List<String> namesAddedInThisOperation = groups.getNamesAddedInThisOperation();
					List<CSG> got = new ArrayList<CSG>();
					for (CSG c : ap.get().getCurrentState()) {
						for (String s : namesAddedInThisOperation) {
							if (c.getName().contentEquals(s) && !c.isInGroup())
								got.add(c);
						}
					}
					getSelected().clear();
					getSelected().addAll(got);
					fireSelectionChanged();
					BowlerStudio.runLater(java.time.Duration.ofMinutes(100), () -> {
						updateControlsDisplayOfSelected();
					});
					updateRobotLab.run();
				} catch (CadoodleConcurrencyException e) {
					// Auto-generated catch block
					com.neuronrobotics.sdk.common.Log.error(e);
				} catch (InterruptedException e) {
					// Auto-generated catch block
					com.neuronrobotics.sdk.common.Log.error(e);
				}
			});

		} else {
			updateControlsDisplayOfSelected();
			updateRobotLab.run();
		}

	}

	public void onUngroup() {
		if (ap.get().isOperationRunning()) {
			com.neuronrobotics.sdk.common.Log.error("Ignoring operation because previous has not finished!");
			return;
		}

		ArrayList<CSG> toSelect = new ArrayList<>();
		for (CSG c : getSelectedCSG(selectedSnapshot())) {
			if (c.isGroupResult()) {
				String name = c.getName();
				for (CSG inG : getCurrentState())
					if (inG.isInGroup() && inG.checkGroupMembership(name))
						toSelect.add(inG);

			}
		}

		List<String> selectedSnapshot = selectedSnapshot();

		if (isAGroupSelected()) {
			getSelected().clear();
			getSelected().addAll(toSelect);
			fireSelectionChanged();
			ap.addOp(new UnGroup().setNames(selectedSnapshot));
		}
		updateControlsDisplayOfSelected();
		updateRobotLab.run();

	}

	public void onHideShowOperation() {
		if (ap.get().isOperationRunning()) {
			com.neuronrobotics.sdk.common.Log.error("Ignoring operation because previous had not finished!");
			return;
		}

		CaDoodleOperation op;
		if (isSelectedHidden())
			op = new Show().setNames(selectedSnapshot());
		else
			op = new Hide().setNames(selectedSnapshot());

		ap.addOp(op);
		updateShowHideButton();
	}

	private void updateShowHideButton() {
		ObservableList<String> c = showHideImage.getStyleClass();
		c.clear();
		if (isSelectedHidden())
			c.add("lit-bulb-icon");
		else
			c.add("dark-bulb-icon");
	}

	private void updateLockButton() {
		ObservableList<String> c = lockImage.getStyleClass();
		c.clear();
		if (isLocked())
			c.add("lock-icon");
		else
			c.add("unlock-icon");
	}

	public boolean isAnyHidden() {
		for (CSG c : getCurrentState())
			if (c.isHide())
				return true;

		return false;
	}

	public boolean isSelectedHidden() {

		for (CSG c : getSelectedCSG(selectedSnapshot()))
			if (!c.isHide())
				return false;

		return true;
	}

	public List<CSG> getSelectedCSG(Iterable<String> sele) {

		ArrayList<CSG> back = new ArrayList<CSG>();
		for (String sel : sele) {
			CSG t = getSelectedCSG(sel);
			if (t != null)
				back.add(t);

		}
		return back;
	}

	public void setShowHideImage(ImageView showHideImage) {
		this.showHideImage = showHideImage;
	}

	public void onAlign() {
		if (getControls() == null)
			return;

		getExecutor().submit(() -> {
			getControls().setMode(SpriteDisplayMode.Align);
			// List<CSG> selectedCSG = getSelectedCSG(selectedSnapshot());
			// Bounds b = getSellectedBounds(selectedCSG);
			// getControls().initializeAlign(selectedCSG, b, getMeshes());
			List<String> selectedSnapshot = selectedSnapshot();
			List<CSG> selectedCSG = getSelectedCSG(selectedSnapshot);
			BowlerStudio.runLater(() -> {
				updateControlsDisplayOfSelected();
				getExecutor().submit(() -> {
					getControls().initializeAlign(selectedCSG, selectedSnapshot, getMeshes(),
							ap.get().getBoundsCache());
				});
			});
		});

	}

	public void onMirror() {
		if (getControls() == null)
			return;

		getExecutor().submit(() -> {
			getControls().setMode(SpriteDisplayMode.Mirror);
			List<CSG> selectedCSG = getSelectedCSG(selectedSnapshot());
			Bounds b;
			try {
				b = getSellectedBounds(selectedCSG);
				getControls().initializeMirror(selectedCSG, b, getMeshes());
			} catch (BoundsComputFailure e) {
				Log.error(e);
			}
		});
	}

	public boolean isFocused() {
		return getControls().isFocused();
	}

	public void cancelOperationModes() {
		if (getControls() == null)
			return;

		if (isAlignActive() || isMirrorActive())
			getControls().setMode(SpriteDisplayMode.Default);

		getControls().cancelOperationMode();
	}

	public void setAlignButton(Button alignButton) {
		this.alignButton = alignButton;
	}

	public List<CSG> getCurrentState() {
		CaDoodleFile caDoodleFile = ap.get();
		if (caDoodleFile == null)
			return new ArrayList<CSG>();

		return caDoodleFile.getCurrentState();
	}

	public List<CSG> getCurrentStateSelected() {
		ArrayList<CSG> back = new ArrayList<CSG>(getSelected());

		return back;
	}

	public Bounds getSellectedBounds() throws BoundsComputFailure {
		return getSellectedBounds(getCurrentStateSelected());
	}

	public Bounds getSellectedBounds(List<CSG> incoming) throws BoundsComputFailure {
		return Align.getBounds(incoming, ap.get().getWorkplane(), ap.get().getBoundsCache());
	}

	public Bounds getBounds(DHParameterKinematics limb) throws BoundsComputFailure {
		ArrayList<CSG> parts = getLimbParts(limb);

		return getSellectedBounds(parts);
	}

	public ArrayList<CSG> getLimbParts(DHParameterKinematics limb) {
		ArrayList<CSG> parts = new ArrayList<CSG>();
		for (CSG c : getCurrentState()) {
			if (c.getLimbName().isPresent() && c.getLimbName().get().contentEquals(limb.getScriptingName()))
				parts.add(c);

		}
		return parts;
	}

	public void objectWorkplane() {

		if ((getSelected().size() != 1) && !isObjectWorkplane)
			return;

		isObjectWorkplane = !isObjectWorkplane;
		if (objectWorkplane != null) {
			com.neuronrobotics.sdk.common.Log.debug("Setting Object Workplane " + isObjectWorkplane);
			objectWorkplane.getStyleClass().clear();
		}
		if (isObjectWorkplane) {
			CSG c = getSelectedCSG(selectedSnapshot().get(0));
			TransformNR nrToCSG = TransformFactory.csgToNR(MoveCenter.getTotalOffset(c));
			previousWP = ap.get().getWorkplane();
			ap.get().setWorkplane(nrToCSG);
			workplane.placeWorkplaneVisualization();
			if (objectWorkplane != null)
				objectWorkplane.getStyleClass().add("image-button-focus");
		} else {
			ap.get().setWorkplane(previousWP);
			workplane.placeWorkplaneVisualization();
			if (objectWorkplane != null)
				objectWorkplane.getStyleClass().add("image-button");
		}
		updateControls();
	}

	public void onDrop() {
		com.neuronrobotics.sdk.common.Log.error("Drop to Workplane");
		getExecutor().submit(() -> {
			List<CSG> sel = ap.get().getSelect(selectedSnapshot());

			if (moveLock())
				return;

			List<CSG> cur = getCurrentStateSelected();
			TransformNR wp = ap.get().getWorkplane();
			Transform t = TransformFactory.nrToCSG(wp);
			// Run a down move for each object, since each will move a different amount
			// based on its own bottom
			for (CSG c : sel) {
				double downMove = -c.transformed(t.inverse()).getMinZ();
				TransformNR location = wp.times(new TransformNR(0, 0, downMove)).times(wp.inverse());
				Thread op;
				try {
					op = ap.addOp(
							new MoveCenter().setLocation(location).setNames(Arrays.asList(c.getName()), ap.get()));
					try {
						op.join(); // wait for the move of this object to finish
					} catch (InterruptedException e) {
						// Auto-generated catch block
						com.neuronrobotics.sdk.common.Log.error(e);

					}
				} catch (InvalidLocationMove e) {
					Log.error(e);
				}

			}
		});

	}

	public void updateHandleOrientations(TransformNR cameraFrame) {
		if (controls != null)
			controls.updateHandleOrientations(cameraFrame);

	}

	public void moveInCameraFrame(TransformNR stateUnitVectorTmp) {

		TickToc.tic("Start Move Request");
		if (getSelected().size() == 0)
			return;
		if (applyingMoveOperation)
			return;
		getExecutor().submit(() -> {
			try {
				if (moveLock())
					return;

				if (ap.get().isOperationRunning()) {
					TickToc.tic("Process running, bailing on new update");
					return;
				}

				timeSinceLastMove = System.currentTimeMillis();
				// TickToc.setEnabled(true);
				TickToc.tic("Start");

				// Get camera orientation for screen-aligned movement
				TransformNR camerFrame = engine.getFlyingCamera().getCamerFrame();
				double camAz = Math.toDegrees(camerFrame.getRotation().getRotationAzimuthRadians());
				Quadrant quad = Quadrant.getQuad(camAz);
				double currentRotZ = Quadrant.QuadrantToAngle(quad);

				// Rotate input vector by camera yaw (screen-space to world-space)
				double yawRad = Math.toRadians(currentRotZ - 90);
				double cos = Math.cos(yawRad);
				double sin = Math.sin(yawRad);

				double inX = stateUnitVectorTmp.getX();
				double inY = stateUnitVectorTmp.getY();

				TransformNR stateUnitVector = new TransformNR(inX * cos - inY * sin, inX * sin + inY * cos,
						stateUnitVectorTmp.getZ());

				double incement = getSnapGridValue();

				boolean updateTrig = false;
				double bound = 0.5;
				if (Math.abs(stateUnitVector.getX()) > bound || Math.abs(stateUnitVector.getY()) > bound
						|| Math.abs(stateUnitVector.getZ()) > bound)
					updateTrig = true;

				if (!updateTrig) {
					TickToc.tic("No Update");
					TickToc.toc();
					TickToc.setEnabled(false);
					return;
				}

				stateUnitVector = new TransformNR(roundToNearest(stateUnitVector.getX() * incement, incement),
						roundToNearest(stateUnitVector.getY() * incement, incement),
						roundToNearest(stateUnitVector.getZ() * incement, incement));

				TransformNR current = manipulation.getGlobalPose().copy();
				TransformNR wp = ap.get().getWorkplane();

				// Convert to workplane-local coordinates
				TransformNR localCurrent = wp.inverse().times(current);

				// Convert world delta to workplane-local delta
				double wpAz = Math.toDegrees(wp.getRotation().getRotationAzimuthRadians());
				double rad = Math.toRadians(-wpAz);
				cos = Math.cos(rad);
				sin = Math.sin(rad);

				double deltaX = stateUnitVector.getX();
				double deltaY = stateUnitVector.getY();

				double localDeltaX = deltaX * cos - deltaY * sin;
				double localDeltaY = deltaX * sin + deltaY * cos;

				// Apply delta in workplane-local space
				TransformNR localNew = new TransformNR(localCurrent.getX() + localDeltaX,
						localCurrent.getY() + localDeltaY, localCurrent.getZ() + stateUnitVector.getZ());

				// Convert back to world coordinates
				TransformNR tf = wp.times(localNew);
				tf.setRotation(current.getRotation());

				List<String> selectedSnapshot = selectedSnapshot();

				CaDoodleOperation op = ap.get().getCurrentOperation();

				if (timeoutMoveThread == null) {
					timeoutMoveThread = new Thread(() -> {
						try {
							while ((System.currentTimeMillis() - timeSinceLastMove) < 800) {
								try {
									Thread.sleep(100);
									// com.neuronrobotics.sdk.common.Log.debug("Waiting to apply move...");
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							applyingMoveOperation = true;
							try {
								ap.addOp(new MoveCenter().setLocation(manipulation.getGlobalPose().copy())
										.setNames(selectedSnapshot(), ap.get())).join();
							} catch (InterruptedException e) {
								com.neuronrobotics.sdk.common.Log.error(e);
							} catch (InvalidLocationMove e) {
								com.neuronrobotics.sdk.common.Log.error(e);
							}

							// resetManipulator();
							TickToc.tic("save");
						} catch (Throwable t) {
							Log.error(t);
						}
						save();
						applyingMoveOperation = false;
						timeoutMoveThread = null;
					});
					timeoutMoveThread.start();
				}
				// Log.debug("Manipulator pose update \n" + tf.toSimpleString() + "\n" +
				// current.toSimpleString());
				// manipulation.setInReferenceFrame(tf);
				manipulation.set(tf);
				// Log.debug("New Manipulator pose update \n" + manipulation.getGlobalPose());

				TickToc.setEnabled(false);
			} catch (Throwable t) {
				Log.error(t);
			}
		});
	}

	private void resetManipulator() {
		manipulation.reset();
	}

	public void save() {
		ap.save();
	}

	public boolean compareLists(List<String> list1, List<String> list2) {
		if ((list1 == null) || (list2 == null))
			return (list1 == list2);

		if (list1.size() != list2.size())
			return false;

		HashSet<String> set1 = new HashSet<>(list1);
		HashSet<String> set2 = new HashSet<>(list2);

		return set1.equals(set2);
	}

	private MoveCenter getActiveMove() {
		CaDoodleOperation op = ap.get().getCurrentOperation();
		if (MoveCenter.class.isInstance(op)) {
			MoveCenter active = (MoveCenter) op;

			if (compareLists(selectedSnapshot(), active.getNamesAddedInThisOperation()))
				return active;

		}
		return null;
	}

	public static double roundToNearest(double incoming, double modulo) {
		return modulo * (Math.round(incoming / modulo));
	}

	public void updateControls() {
		onCameraChange(engine.getFlyingCamera());
	}

	public void onCameraChange(VirtualCameraMobileBase camera) {
		double zoom = camera.getZoomDepth();
		double az = camera.getPanAngle();
		double el = camera.getTiltAngle();
		double x = camera.getGlobalX();
		double y = camera.getGlobalY();
		double z = camera.getGlobalZ();
		double screenW = engine.getWidth();
		double screenH = engine.getHeight();
		onCameraChange(screenW, screenH, zoom, az, el, x, y, z, camera.getCamera().getFieldOfView());
	}

	public void onCameraChange(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z, double cameraFovDegrees) {

		this.screenW = screenW;
		this.screenH = screenH;
		this.zoom = zoom;
		this.az = az;
		this.el = el;
		this.x = x;
		this.y = y;
		this.z = z;

		if ((ap.get() == null) || (getControls() == null))
			return;

		List<String> selectedSnapshot = selectedSnapshot();

		// getExecutor().submit(() -> {
		List<CSG> selectedCSG = new ArrayList<CSG>(getSelected());

		if (selectedCSG.size() == 0)
			return;
		Bounds sellectedBounds;
		try {
			sellectedBounds = getSellectedBounds(selectedCSG);
			BowlerStudio.runLater(() -> {
				getControls().updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedSnapshot, sellectedBounds,
						ap.get().getBoundsCache(), cameraFovDegrees);
			});
		} catch (BoundsComputFailure e) {
			Log.error(e);
		}
	}

	// public void setCadoodle(ActiveProject ap) {
	//
	// }

	public void setSnapGrid(double snapGridValue) {
		this.setSnapGridValue(snapGridValue);
		manipulation.setIncrement(snapGridValue);

		if (getControls() != null)
			getControls().setSnapGrid(snapGridValue);

		workplane.setIncrement(snapGridValue);
	}

	@Override
	public void onSaveSuggestion() {
		// The Main Controller triggers this
	}

	public TransformNR getWorkplane() {
		return ap.get().getWorkplane();
	}

	public void setActiveProject(ActiveProject ap, WorkplaneManager workplane) {
		if (this.ap == null) {
			setControls(new ControlSprites(this, engine, selection, manipulation, ap, ruler, workplane));
			getControls().setSnapGrid(getSnapGridValue());
		}
		this.ap = ap;
	}

	public CaDoodleOperation getCurrentOperation() {
		return ap.get().getCurrentOperation();
	}

	public void regenerateCurrent() {
		try {
			ap.get().regenerateCurrent();
		} catch (FailedToApplyOperation e) {
			Log.error(e);
		}
	}

	@Override
	public void onInitializationDone() {
		// Auto-generated method stub
	}

	public SpriteDisplayMode getMode() {
		return getControls().getMode();
	}

	public void setMode(SpriteDisplayMode placing) {
		getControls().setMode(placing);
	}

	public ArrayList<CSG> getAllVisible() {
		ArrayList<CSG> back = new ArrayList<CSG>();

		for (CSG c : getCurrentState()) {

			if (c.isHide() || c.isHole() || (c.isInGroup() && !c.isAlwaysShow()))
				continue;

			back.add(c.clone().syncProperties(getDb(), c));
		}
		return back;
	}

	private CSGDatabaseInstance getDb() {
		return ap.get().getCsgDBinstance();
	}

	public ArrayList<CSG> getSelectable() {
		ArrayList<CSG> back = new ArrayList<CSG>();

		for (CSG c : getCurrentState()) {
			if (c.isHide() || (c.isInGroup() && !c.isAlwaysShow()))
				continue;

			back.add(c);
		}

		return back;
	}

	@Override
	public void onWorkplaneChange(TransformNR newWP) {
		clearBoundsCache(null);
		if (!workplane.isWorkplaneNotOrigin()) {
			if (objectWorkplane != null) {
				objectWorkplane.getStyleClass().clear();
				objectWorkplane.getStyleClass().add("image-button");
			}
			isObjectWorkplane = false;
		}

		BowlerStudio.runLater(() -> {
			updateControls();
		});
	}

	@Override
	public void onInitializationStart() {
		// Auto-generated method stub
	}

	@Override
	public void onRegenerateDone() {
		// com.neuronrobotics.sdk.common.Log.debug("Enable parametrics ");
		BowlerStudio.runLater(() -> parametrics.setDisable(false));
		onUpdate(ap.get().getCurrentState(), ap.get().getCurrentOperation(), ap.get());

	}

	@Override
	public void onRegenerateStart(CaDoodleOperation source) {
		// com.neuronrobotics.sdk.common.Log.debug("Disable parametrics ");
		BowlerStudio.runLater(() -> parametrics.setDisable(true));
	}

	public boolean isShowConstituants() {
		return showConstituants;
	}

	public void setShowConstituants(boolean showConstituants) {
		this.showConstituants = showConstituants;
	}

	public boolean isInOperationMode() {
		return isAlignActive() || isMirrorActive();
	}

	private boolean isMirrorActive() {
		return getControls().mirrorIsActive();
	}

	private boolean isAlignActive() {
		return getControls().alignIsActive();
	}

	public HashMap<CSG, MeshHolder> getMeshes() {
		return meshes;
	}

	public void setMeshes(HashMap<CSG, MeshHolder> meshes) {
		this.meshes = meshes;
	}

	@Override
	public void onTimelineUpdate(int num, File image) {
	}

	public void setAdvancedMode(boolean advanced) {
		this.advanced = advanced;
	}

	public boolean isRobotLabOpen() {
		return robotLabOpen;
	}

	public void setRobotLabOpen(boolean robotLabOpen) {
		this.robotLabOpen = robotLabOpen;
	}

	public void setRobotLabButton(Button robotLabDrawer) {
		this.robotLabDrawer = robotLabDrawer;
	}

	public int numberSelected() {
		return getSelected().size();
	}

	/**
	 * @return the updateRobotLab
	 */
	public Runnable getUpdateRobotLab() {
		return updateRobotLab;
	}

	/**
	 * @param updateRobotLab the updateRobotLab to set
	 */
	public void setUpdateRobotLab(Runnable updateRobotLab) {
		this.updateRobotLab = updateRobotLab;
	}

	/**
	 * @return the controls
	 */
	public ControlSprites getControls() {
		return controls;
	}

	/**
	 * @param controls the controls to set
	 */
	public void setControls(ControlSprites controls) {
		this.controls = controls;
	}

	/**
	 * @return the limbs
	 */
	public LimbControlManager getLimbs() {
		return limbs;
	}

	/**
	 * @param limbs the limbs to set
	 */
	public void setLimbs(LimbControlManager limbs) {
		this.limbs = limbs;
	}

	public void submit(Runnable r) {
		executor.submit(r);
	}

	public ExecutorService getExecutor() {
		return executor;
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	public LinkedHashSet<CSG> getSelected() {
		return selected;
	}

	public void setSelected(LinkedHashSet<CSG> selected) {
		this.selected = selected;
	}

	public void runFillet() {
		com.neuronrobotics.sdk.common.Log.debug("on Fillet");
		filletTool.run(selected, ap, this, workplane, ruler);
	}

	public void runExtrude() {
		com.neuronrobotics.sdk.common.Log.debug("on Extrude");
		extrudeManager.run(selected, ap, this, workplane, ruler);
	}

	public void setUserDefinedName(String newText) {
		if (ap.get().isOperationRunning())
			return;
		List<CSG> currentStateSelected = getCurrentStateSelected();
		if (currentStateSelected == null)
			return;
		if (currentStateSelected.size() == 0)
			return;
		CSG csg = currentStateSelected.get(0);
		if (csg.getUserDefinedName().contentEquals(newText))
			return;
		SetUserDefinedName ns = new SetUserDefinedName().setTarget(csg.getName()).setNewUserDefinedName(newText);
		ap.addOp(ns);
	}

	public void setAdvancedButtons(Button filletButton, Button extrudeButton, Button hexDistributeButton,
			Button boltHoleButton) {
		this.filletButton = filletButton;
		this.extrudeButton = extrudeButton;
		this.hexDistributeButton = hexDistributeButton;
		this.boltHoleButton = boltHoleButton;
	}

	public void hideHalos() {
		for (MeshHolder mh : meshes.values()) {
			mh.halo.setVisible(false);
		}
	}

	public double getSnapGridValue() {
		return Double.parseDouble(ConfigurationDatabase.get("CaDoodle", "SnapGridSize", "1.0").toString());
	}

	public void setSnapGridValue(double snapGridValue) {
		ConfigurationDatabase.put("CaDoodle", "SnapGridSize", "" + snapGridValue);
	}

}
