package com.commonwealthrobotics.controls;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.sshd.common.session.Session;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.Main;
import com.commonwealthrobotics.MainController;
import com.commonwealthrobotics.RulerManager;
import com.commonwealthrobotics.TexturedCSG;
import com.commonwealthrobotics.TimelineManager;
import com.commonwealthrobotics.WorkplaneManager;
import com.commonwealthrobotics.robot.LimbControlManager;
import com.neuronrobotics.bowlerkernel.Bezier3d.IInteractiveUIElementProvider;
import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;
import com.neuronrobotics.bowlerstudio.BowlerKernel;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.SplashManager;
import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.creature.MobileBaseBuilder;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.CaDoodleLoader;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.*;
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
import eu.mihosoft.vrl.v3d.Transform;
import eu.mihosoft.vrl.v3d.Vector3d;
import eu.mihosoft.vrl.v3d.parametrics.CSGDatabase;
import eu.mihosoft.vrl.v3d.parametrics.LengthParameter;
import eu.mihosoft.vrl.v3d.parametrics.Parameter;
import eu.mihosoft.vrl.v3d.parametrics.StringParameter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.DepthTest;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;

@SuppressWarnings("unused")
public class SelectionSession implements ICaDoodleStateUpdate {
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private ControlSprites controls;
	private HashMap<CSG, MeshView> meshes = new HashMap<CSG, MeshView>();
	// private CaDoodleOperation source;
	private TitledPane shapeConfiguration;
	private Accordion shapeConfigurationBox;
	private AnchorPane shapeConfigurationHolder;
	private GridPane configurationGrid;
	private AnchorPane control3d;
	private BowlerStudio3dEngine engine;
	private LinkedHashSet<CSG> selected = new LinkedHashSet<>();
	private ColorPicker colorPicker;
	private ComboBox<String> snapGrid;
	private double currentGrid = 1.0;
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
	private Thread autosaveThread = null;
	private boolean needsSave = false;
	private Affine selection = new Affine();
	private Manipulation manipulation = new Manipulation(selection, new Vector3d(1, 1, 0), new TransformNR());
	private EventHandler<MouseEvent> mouseMover = manipulation.getMouseEvents();
	private HashMap<CSG, Bounds> inWorkplaneBounds = new HashMap<CSG, Bounds>();
	private double size;

	private WorkplaneManager workplane;
	boolean intitialization = false;

	private VBox parametrics;
	private ActiveProject ap = null;
	private HashMap<CaDoodleOperation, FileChangeWatcher> myWatchers = new HashMap<>();
	private Button lockButton;
	private ImageView lockImage;
	private boolean useButton = false;
	private Button regenerate = new Button("Re-Generate");
	private HashMap<String, EventHandler<ActionEvent>> regenEvents = new HashMap<>();
	private boolean showConstituants = false;
	private MenuButton advancedGroupMenu;
	private TimelineManager timeline;
	private RulerManager ruler;
	private double max = 9999;
	private Button objectWorkplane;
	private Button dropToWorkplane;
	private boolean isObjectWorkplane=false;
	private TransformNR previousWP;
	private boolean advanced;
	private boolean robotLabOpen=true;
	private Button robotLabDrawer;
	private Runnable updateRobotLab = null;
	private LimbControlManager limbs;
	private ProgressIndicator memUsage; 
	private boolean resizeLiveMode = false;
	public boolean isResizeLiveMode() { return resizeLiveMode; }

	@SuppressWarnings("static-access")
	public SelectionSession(BowlerStudio3dEngine e, ActiveProject ap, RulerManager ruler) {
		engine = e;
		this.ruler = ruler;
		setActiveProject(ap);
		manipulation.addSaveListener(() -> {
			if (intitialization)
				return;
			TransformNR globalPose = manipulation.getGlobalPoseInReferenceFrame();
			//com.neuronrobotics.sdk.common.Log.error("Objects Moved! " + globalPose.toSimpleString());
			Thread t = ap.addOp(new MoveCenter().setLocation(globalPose)
					.setNames(selectedSnapshot(),ap.get()));
			try {
				t.join();
			} catch (InterruptedException ex) {
				// Auto-generated catch block
				com.neuronrobotics.sdk.common.Log.error(e);;
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

	public boolean moveLock() {
		boolean moveLock = false;
		for(CSG sel:selected) {
			if(sel.isMotionLock()||sel.isInGroup())
				moveLock=true;
		}
		return moveLock;
	}
	public List<String> selectedSnapshot() {
		ArrayList<String> s = new ArrayList<String>();
		for( CSG c:selected)
			s.add(c.getName());
		return s;
	}

	@Override
	public void onUpdate(List<CSG> currentState, CaDoodleOperation source, CaDoodleFile f) {
		TickToc.tic("Start On Update In Selected Session");
		clearBoundsCache();
		// this.source = source;
		intitialization = true;
		manipulation.set(0, 0, 0);
		if (isAlignActive() && Align.class.isInstance(source))
			getControls().setMode(SpriteDisplayMode.Align);
		else if (isMirrorActive() && Mirror.class.isInstance(source)) {
			getControls().setMode(SpriteDisplayMode.Mirror);
			onMirror();
		} else
			getControls().setMode(SpriteDisplayMode.Default);
		intitialization = false;
		setUpParametrics(currentState, source);
		displayCurrent();
		TickToc.tic("Finish On Update In Selected Session");
		updateMemoryDisplay();

	}

	public void updateMemoryDisplay() {
		double value = ((double)CaDoodleFile.getFreeMemory())/100.0;
		Log.info("Mem="+value);
		memUsage.setProgress(value);
		if(value>0.5&&value<0.75) {
			
		}
	}

	private void myRegenerate(CaDoodleOperation source, IFileChangeListener l, File f) {
		FileChangeWatcher fileChangeWatcher = myWatchers.get(source);
		if (fileChangeWatcher != null) {
			fileChangeWatcher.close();
			myWatchers.remove(source);
		}
		com.neuronrobotics.sdk.common.Log.error("Regenerating from CaDoodle " + source);

		// new Exception().printStackTrace();
		getExecutor().submit(() -> {
			try {
				ap.get().getCsgDBinstance().saveDatabase();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Thread t = ap.regenerateFrom(source);
			if (t == null)
				return;
			try {
				t.join();
			} catch (InterruptedException e) {
				// Auto-generated catch block
				com.neuronrobotics.sdk.common.Log.error(e);
			}
			onUpdate(ap.get().getCurrentState(), ap.get().getCurrentOpperation(), ap.get());
			if (f != null && l != null) {
				FileChangeWatcher w;
				try {
					w = FileChangeWatcher.watch(f);
					myWatchers.put(source, w);
					w.addIFileChangeListener(l);
				} catch (IOException e) {
					// Auto-generated catch block
					com.neuronrobotics.sdk.common.Log.error(e);
				}
			}
		});
	}

	private void setUpParametrics(List<CSG> currentState, CaDoodleOperation source) {
		if (AbstractAddFrom.class.isInstance(source)) {
			AbstractAddFrom s = (AbstractAddFrom) source;
			// com.neuronrobotics.sdk.common.Log.error("Adding A op for "+s.getClass());
			HashSet<String> namesAdded = s.getNamesAdded();
			// com.neuronrobotics.sdk.common.Log.error(namesAdded.size());
			File f = null;
			IFileChangeListener l = null;
			try {
				f = s.getFile();
				File myFile = f;
				l = new IFileChangeListener() {
					@Override
					public void onFileDelete(File fileThatIsDeleted) {
					}

					@Override
					public void onFileChange(File fileThatChanged, @SuppressWarnings("rawtypes") WatchEvent event) {
						com.neuronrobotics.sdk.common.Log.error("File Change updating " + source.getType());
						myRegenerate(source, this, myFile);
					}

				};
				if (myWatchers.get(source) == null) {
					try {
						FileChangeWatcher w = FileChangeWatcher.watch(f);
						myWatchers.put(source, w);
						w.addIFileChangeListener(l);
					} catch (IOException e) {
						// Auto-generated catch block
						com.neuronrobotics.sdk.common.Log.error(e);
					}
				}
			} catch (NoSuchFileException ex) {
				// thats ok
			}
			for (String nameString : namesAdded) {
				CSG n = null;
				for (CSG c : currentState) {
					if (c.getName().contentEquals(nameString)) {
						n = c;
					}
				}
				if (n == null)
					continue;
				String name = s.getName();
				//com.neuronrobotics.sdk.common.Log.error("Adding Listeners for " + name);
				// new Exception().printStackTrace();
				Set<String> parameters = n.getParameters(ap.get().getCsgDBinstance());
				IFileChangeListener myL = l;
				File myFile = f;
				if (parameters.size() > 0 && regenEvents.get(n.getName()) == null) {
					BowlerStudio.runLater(() -> regenerate.setDisable(true));
					// new RuntimeException("Regester event for " + source.getType() + " " +
					// nameString).printStackTrace();

					EventHandler<ActionEvent> value = e -> {
						BowlerStudio.runLater(() -> regenerate.setDisable(true));
						com.neuronrobotics.sdk.common.Log.error("Button Change updating " + source.getType() + " " + nameString);
						myRegenerate(source, myL, myFile);
					};
					regenEvents.put(n.getName(), value);
				}
				for (String k : parameters) {
					if (!k.contains(n.getName()) )
						continue;
					Parameter para = ap.get().getCsgDBinstance().get(k);
					//com.neuronrobotics.sdk.common.Log.error("Adding listener to " + k + " on " + nameString);
					ap.get().getCsgDBinstance().clearParameterListeners(k);
					ap.get().getCsgDBinstance().addParameterListener(k, (name1, p) -> {
						if (LengthParameter.class.isInstance(p)) {
							// new Exception().printStackTrace();
							com.neuronrobotics.sdk.common.Log.debug("Value Updating " + p.getName() + " to " + p.getMM());
						}
						CaDoodleFile caDoodleFile = ap.get();
						double percentInitialized = caDoodleFile.getPercentInitialized();
						boolean regenerating = caDoodleFile.isRegenerating();
						if (regenerating || percentInitialized < 1)
							return;
						getExecutor().submit(() -> {
							if (useButton) {
								BowlerStudio.runLater(() -> regenerate.setDisable(false));
							} else {
								myRegenerate(source, myL, myFile);
							}
						});

					});
				}
			}
		}
	}

	private void displayCurrent() {
		@SuppressWarnings("unchecked")
		List<CSG> process = (List<CSG>) CaDoodleLoader.process(ap.get(),true);
		if (ap.get().isRegenerating()) {
			return;
		}
		List<CSG> currentState = getCurrentState();
		BowlerStudio.runLater(() -> {
			clearScreen();
			if (isShowConstituants()) {
				for (CSG c : ap.get().getCurrentState()) {
					if (c.isInGroup() || c.isHide()) {
						c.setIsWireFrame(true);
					} else {
						c.setIsWireFrame(false);
					}
					displayCSG(c);
					if ((c.isInGroup() && !c.isAlwaysShow()) || c.isHide()) {
						getMeshes().get(c).setMouseTransparent(true);
					}
				}
			} else
				for (CSG c : process) {
					displayCSG(c);
				}
			ArrayList<CSG> toRemove = new ArrayList<>();
			for (CSG s : selected) {
				boolean exists = false;
				for (CSG c : currentState) {
					if (c.getName().contentEquals(s.getName()) && !c.isInGroup())
						exists = true;
				}
				if (!exists) {
					toRemove.add(s);
				}
			}
			selected.removeAll(toRemove);
			if (workplane != null)
				workplane.updateMeshes(getMeshes());
			updateControlsDisplayOfSelected();
			updateRobotLab.run();
			setKeyBindingFocus();
			TickToc.toc();
			TickToc.setEnabled(false);
		});

	}

	public void clearScreen() {
		if (getMeshes() == null)
			return;
		for (CSG c : getMeshes().keySet()) {
			engine.removeUserNode(getMeshes().get(c));
		}
		getMeshes().clear();
	}

	private void displayCSG(CSG c) {
		MeshView meshView = c.newMesh();
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
			diffuseColor = Color.color(diffuseColor.getRed(), diffuseColor.getGreen(), diffuseColor.getBlue(), diffuseColor.getOpacity());
			phongMaterial.setDiffuseColor(diffuseColor);
			double red = diffuseColor.getRed()+0.1;
			double green = diffuseColor.getGreen()+0.1;
			double blue = diffuseColor.getBlue()+0.1;
			phongMaterial.setSpecularColor(Color.color(red>1?1:red, green>1?1:green, blue>1?1:blue, diffuseColor.getOpacity()));
		}
		meshView.setViewOrder(0);
		engine.addUserNode(meshView);
		getMeshes().put(c, meshView);
		setUpControls(meshView, c);
	}

	private void setUpControls(MeshView meshView, CSG name) {
		if (name == null)
			throw new RuntimeException("Name can not be null");
		meshView.setOnMousePressed(event -> {
			if (event.getButton() == MouseButton.PRIMARY) {
				if (event.isShiftDown()) {
					if (selected.contains(name)) {
						selected.remove(name);
					} else
						selected.add(name);
				} else {
					if (!selected.contains(name)) {
						selected.clear();
						selected.add(name);
					}
				}
				if(getMode()!=SpriteDisplayMode.Align)
					setMode(SpriteDisplayMode.Default);
				updateRobotLab.run();
				updateControlsDisplayOfSelected();
				event.consume();
			}
		});

	}

	public void updateControlsDisplayOfSelected() {
		parametrics.getChildren().clear();
		clearBoundsCache();
		timeline.updateSelected(selected);
		
		getExecutor().submit(()->{
			List<CSG> cs = getCurrentState();
			BowlerStudio.runLater(() -> UpdateUIControls(cs));
		});
	}

	private void UpdateUIControls(List<CSG> cs) {
		if (selected.size() > 0) {
			dropToWorkplane.setDisable(false);
			objectWorkplane.setDisable(selected.size() != 1);

			shapeConfigurationHolder.getChildren().clear();
			shapeConfigurationHolder.getChildren().add(shapeConfigurationBox);
			CSG set = ((CSG) selected.toArray()[0]);
			if (set == null)
				return;
			Color value = set.getColor();
			colorPicker.setValue(value);
			String hexColor = String.format(Locale.US,"#%02X%02X%02X", (int) (value.getRed() * 255),
					(int) (value.getGreen() * 255), (int) (value.getBlue() * 255));

			String style = String.format(" -fx-background-color: %s;", hexColor);
			colorPicker.setStyle(style);
			showButtons();
			updateShowHideButton();
			updateLockButton();
			for (CSG c : cs) {
				MeshView meshView = getMeshes().get(c);
				if (meshView != null) {
					meshView.getTransforms().remove(selection);
					meshView.getTransforms().remove(getControls().getViewRotation());
					meshView.removeEventFilter(MouseEvent.ANY, mouseMover);
				}

			}
			TransformFactory.nrToAffine(new TransformNR(), selection);
			TransformFactory.nrToAffine(new TransformNR(), getControls().getViewRotation());
			boolean lockMove=moveLock();
			List<String> selectedSnapshot = selectedSnapshot();
			for (CSG c : getSelectedCSG(selectedSnapshot)) {
				MeshView meshView = getMeshes().get(c);
				if (meshView != null) {
					meshView.getTransforms().addAll(getControls().getViewRotation(), selection);
					if (!isLocked())
						meshView.addEventFilter(MouseEvent.ANY, mouseMover);
				}
			}
			manipulation.setUnlocked(!lockMove);
			shapeConfiguration.setText("Shape (" + selected.size() + ")");
			List<CSG> csgs = getSelectedCSG(selectedSnapshot);
			if (selectedSnapshot.size() == 1  && csgs.size()>0) {
				CSG sel = csgs.get(0);
				List<String> sortedList = new ArrayList<>(sel.getParameters(ap.get().getCsgDBinstance()));
				Collections.sort(sortedList);
				int numCadParaams = 0;
				for (String key : sortedList) {
					if (key.contains("CaDoodle")
							&& (key.contains(sel.getName().split("_")[0]))) {
						numCadParaams++;
						String[] parts = key.split("_");
						HBox thisLine = new HBox(5);
						String text = parts[parts.length - 1];
						Label e = new Label(text);
						e.setMinWidth(50);
						thisLine.getChildren().add(e);
						parametrics.getChildren().add(thisLine);
						Parameter para = ap.get().getCsgDBinstance().get(key);
						int width = 120;

						if (StringParameter.class.isInstance(para)) {
							ArrayList<String> opts = para.getOptions();
							if (opts.size() > 0) {
								setUpComboBoxParametrics(thisLine, text, para, opts, width);
							} else {
								File file = new File(para.getStrValue());
								boolean exists = file.exists();
								if (exists) {
									setUpFileBox(thisLine, text, para, width, file);
								} else {
									setUpTextBoxEnterData(thisLine, text, para, width);
								}
							}
						} else {
							if (para!=null)
								setUpNumberChoices(thisLine, text, para, width);
							else
								Log.error("Failed to set parameter in UI "+key);
						}
					}
				}
				if (numCadParaams > 2) {
					useButton = true;
					// com.neuronrobotics.sdk.common.Log.error("Using button for regeneration " + sel.getName());
					if(!parametrics.getChildren().contains(regenerate))
						parametrics.getChildren().add(regenerate);
					EventHandler<ActionEvent> value2 = regenEvents.get(sel.getName());
					if (value2 != null)
						regenerate.setOnAction(value2);
					else {
						com.neuronrobotics.sdk.common.Log.error("ERROR regenerate event is null");
					}
				} else {
					useButton = false;
					parametrics.getChildren().remove(regenerate);
				}
			}
		} else {
			for (CSG c : cs) {
				MeshView meshView = getMeshes().get(c);
				if (meshView != null) {
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
		updateControls();
	}

	public void clearBoundsCache() {
		inWorkplaneBounds.clear();
	}

	private void setUpNumberField(HBox thisLine, String text, Parameter para, int width) {
		ArrayList<String> options3 = para.getOptions();

		TextField options = new TextField();
		options.setEditable(true);
		options.setText(para.getMM() + "");
		options.setMinWidth(width);
		thisLine.getChildren().add(options);

		options.setOnAction(event -> {
			ArrayList<String> options2 = para.getOptions();
			String string = options.getText().toString();
			try {
				double parseDouble = Double.parseDouble(string);
				if (parseDouble > max) {
					parseDouble = max;
					options.setText(max + "");
				}
				if (parseDouble < -max) {
					parseDouble = -max;
					options.setText(-max + "");
				}
				com.neuronrobotics.sdk.common.Log.debug("Setting new value " + parseDouble);
				para.setMM(parseDouble);
				options2.clear();
			} catch (Throwable t) {
				com.neuronrobotics.sdk.common.Log.error(t);
				options.setText(para.getMM() + "");
			}
		});
	}

	private void setUpNumberChoices(HBox thisLine, String text, Parameter para, int width) {

		ArrayList<String> options2 = para.getOptions();
		boolean limited = false;
		if (options2.size() < 2) {
			setUpNumberField(thisLine, text, para, width);
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
		thisLine.getChildren().add(options);

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
		});
	}

	private void setUpTextBoxEnterData(HBox thisLine, String text, Parameter para, int width) {
		TextField tf = new TextField(para.getStrValue());
		tf.setOnAction(event -> {
			para.setStrValue(tf.getText());
		});
		thisLine.getChildren().add(tf);
		thisLine.setMinWidth(width);
	}

	private void setUpFileBox(HBox thisLine, String text, Parameter para, int width, File file) {
		// Button tf = new Button(new File(para.getStrValue()).getName());
		ExternalEditorController ec = new ExternalEditorController(file, new CheckBox(),()->{
			if(file.getName().endsWith("doodle")) {
				ConfigurationDatabase.put("CaDoodle", "CaDoodleActiveFile", ap.get().getSelf().getAbsolutePath());
				ConfigurationDatabase.save();
			}
		});
		Node tf = ec.getControl();
		thisLine.getChildren().add(tf);
		thisLine.setMinWidth(width);
	}

	private void setUpComboBoxParametrics(HBox thisLine, String text, Parameter para, ArrayList<String> opts,
			int width) {
		ComboBox<String> options = new ComboBox<String>();
		if (para.getName().toLowerCase().endsWith("font")) {
			options.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
				@Override
				protected void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					setFont(Font.font(item));
					setText(item);
				}
			});

			options.setButtonCell(new javafx.scene.control.ListCell<String>() {
				@Override
				protected void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					setFont(Font.font(item));
					setText(item);
				}
			});
		}
		thisLine.getChildren().add(options);
		for (String s : opts) {
			options.getItems().add(s);
			BowlerStudio.runLater(() -> options.getSelectionModel().select(s));
		}
		BowlerStudio.runLater(() -> {
			options.getSelectionModel().select(para.getStrValue());
			options.setMinWidth(width);
			options.setOnAction(event -> {
				com.neuronrobotics.sdk.common.Log.debug(System.currentTimeMillis() + " Event " + event);
				para.setStrValue(options.getSelectionModel().getSelectedItem());
				// CSGDatabase.saveDatabase();
				// com.neuronrobotics.sdk.common.Log.error("Saving "+text);
			});
		});

	}

	private CSG getSelectedCSG(String string) {
		for (CSG c : getMeshes().keySet()) {
			if (c.getName().contentEquals(string))
				return c;
		}
		return null;
	}

	public void set(TitledPane shapeConfiguration, Accordion shapeConfigurationBox, AnchorPane shapeConfigurationHolder,
			GridPane configurationGrid, AnchorPane control3d, BowlerStudio3dEngine engine, ColorPicker colorPicker,
			ComboBox<String> snapGrid, VBox parametrics, Button lockButton, ImageView lockImage,
			MenuButton advancedGroupMenu, TimelineManager tm, Button objectWorkplane, Button dropToWorkplane, ProgressIndicator memUsage) {
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
		setupSnapGrid();

	}

	public void clearAlignObjectCache() {
		getControls().clearAlign();
	}

	private void setupSnapGrid() {
        double inch = 25.4; // 1 inch is 25.4mm
		List<String> grids = Arrays.asList(
				String.format(Locale.US, "%.4f mm", 0.1000),
				String.format(Locale.US, "%.4f mm", 0.2500), 
				String.format(Locale.US, "%.4f mm", 0.5000),
				String.format(Locale.US, "%.4f mm", 1.0000), 
				String.format(Locale.US, "%.4f mm 1/16 inch", inch / 16.0), 
				String.format(Locale.US, "%.4f mm", 2.0000), 
				String.format(Locale.US, "%.4f mm 1/8 inch",  inch /  8.0), 
				String.format(Locale.US, "%.4f mm", 5.0000), 
				String.format(Locale.US, "%.4f mm 1/4 inch",  inch /  4.0),
				String.format(Locale.US, "%.4f mm Brick", 8.0000), 
				String.format(Locale.US, "%.4f mm", 10.000),
				String.format(Locale.US, "%.4f mm 1/2 inch",  inch /  2.0),
				String.format(Locale.US, "%.4f mm", 20.000),
				String.format(Locale.US, "%.4f mm 1 inch",    inch)
				);
		
		HashMap<String, Double> map = new HashMap<>();
		map.put("Off", 0.001);
		this.snapGrid.getItems().add("Off");
		for (String s : grids) {
			Number n = Double.parseDouble(s.split(" ")[0]);
			String key = s;
			map.put(key, n.doubleValue());
			this.snapGrid.getItems().add(key);
		}
		for(String key:map.keySet()) {
			Double num = map.get(key);
			if(Math.abs(currentGrid - num.doubleValue()) < 0.001) {
				snapGrid.getSelectionModel().select(key);
				break;
			}
		}

		

		this.snapGrid.setOnAction(event -> {
			String selected = this.snapGrid.getSelectionModel().getSelectedItem();
			Double num = map.get(selected);
			if (num != null) {
				currentGrid = num;
				setSnapGrid(currentGrid);
				com.neuronrobotics.sdk.common.Log.error("Snap Grid Set to " + currentGrid);
				setKeyBindingFocus();
			}
		});
	}

	public void clearSelection() {
		cancelOperationModes();
		selected.clear();
		updateControlsDisplayOfSelected();
		updateRobotLab.run();
		setKeyBindingFocus();
	}

	public void selectAll(Iterable<String> names) {
		getExecutor().submit(() -> {
		selected.clear();
		for (CSG c : getCurrentState()) {
			if ((c.isInGroup() && !c.isAlwaysShow()))
				continue;
			if (c.isHide())
				continue;
			for (String s : names)
				if (s.contentEquals(c.getName())) {
					selected.add(c);
					break;
				}
		}
		BowlerStudio.runLater(() -> {

			updateControlsDisplayOfSelected();
		});
		updateRobotLab.run();
		});
	}

	public void selectAll() {
		getExecutor().submit(() -> {
		selected.clear();
		for (CSG c : getCurrentState()) {
			if ((c.isInGroup() && !c.isAlwaysShow()))
				continue;
			if (c.isHide())
				continue;
			selected.add(c);
		}
		BowlerStudio.runLater(() -> updateControlsDisplayOfSelected());
		updateRobotLab.run();
		});
	}

	public void setKeyBindingFocus() {
		if (!SplashManager.isVisibleSplash())
			if (engine != null) {
				// new Exception("KB Focused here").printStackTrace();
				// com.neuronrobotics.sdk.common.Log.error("Setting KeyBindingFocus");
				BowlerStudio.runLater(() -> engine.getSubScene().requestFocus());
			}
	}

	public void setToSolid() {
		getExecutor().submit(() -> {
		if (selected.size() == 0)
			return;
		boolean isSilid = true;
		for (CSG s : selected) {
			CSG selectedCSG = s;
			if (selectedCSG != null)
				if (selectedCSG.isHole()) {
					isSilid = false;
				}
		}
		if (isSilid)
			return;// all solid
		ToSolid h = new ToSolid().setNames(selectedSnapshot());
		addOp(h);
		});

	}

	public void toggleTransparent() {
		getExecutor().submit(() -> {
			com.neuronrobotics.sdk.common.Log.debug("Toggel transparent");
			ArrayList<ToSolid> toChange = new ArrayList<>();
			for (Iterator<CSG> iterator = selected.iterator(); iterator.hasNext();) {
				CSG s = iterator.next();
				CSG c = s;
				if (!c.isHole()) {
					MeshView mesh = getMeshes().get(c);
					if (mesh == null)
						continue;
					PhongMaterial phongMaterial = (PhongMaterial) mesh.getMaterial();
					Color diffuseColor = phongMaterial.getDiffuseColor();
					double opacity = diffuseColor.getOpacity();
					if (opacity < 1) {
						opacity = 1;
					} else {
						opacity = 0.35;
					}
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
		for (CSG c : selected) {
			if (!c.isHole())
				opacity += ((PhongMaterial) c.getMesh().getMaterial()).getDiffuseColor().getOpacity();
		}
		return (opacity / (double) selected.size()) < 0.999;
	}

	public void setColor(Color value) {
		ToSolid solid = new ToSolid().setNames(selectedSnapshot()).setColor(value);

		addOp(solid);
	}

	public void setToHole() {
		if (selected.size() == 0)
			return;
		boolean isSilid = false;
		for (CSG s : selected) {
			if (!s.isHole()) {
				isSilid = true;
			}
		}
		if (!isSilid)
			return;// all holes
		ToHole h = new ToHole().setNames(selectedSnapshot());
		addOp(h);
	}

	public TransformNR getFocusCenter() {
		if (selected.size() == 0)
			return new TransformNR();
		Bounds b = getSellectedBounds();

		TransformNR tf = new TransformNR(b.getCenterX(), b.getCenterY(), b.getCenterZ());
		TransformNR wp = ap.get().getWorkplane();
		return wp.times(tf);
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
			for (Button b : buttons) {
				b.setDisable(true);
			}
			if(robotLabDrawer!=null) {
				if(!robotLabOpen) {
					robotLabDrawer.setDisable(true);
				}
			}
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
		});
	}

	private void showButtons() {

		BowlerStudio.runLater(() -> {
			for (Button b : buttons) {
				b.setDisable(false);
			}
			int unlockedSelected = 0;
			for (CSG c : selected) {
				if (!c.isLock())
					unlockedSelected++;
			}
			if (unlockedSelected > 1) {
				groupButton.setDisable(false);
				alignButton.setDisable(false);
			}
			if (selected.size() > 0 && advanced) {
				advancedGroupMenu.setDisable(false);
				robotLabDrawer.setDisable(false);
			}
			if (isAGroupSelected()) {
				ungroupButton.setDisable(false);
			}
		});
	}

	private boolean isAGroupSelected() {
		for (CSG c : selected) {
			if (c != null) {
				if (c.isGroupResult()) {
					return true;
				}
			}
		}
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
		com.neuronrobotics.sdk.common.Log.debug("Duplicate called ");
		getExecutor().submit(() -> performPaste(0, selectedSnapshot()));
		;
	}

	public void onPaste() {
		com.neuronrobotics.sdk.common.Log.debug("Paste called");
		getExecutor().submit(() -> performPaste(20, copySetinternal));
		;

	}

	private void performPaste(double distance, List<String> copySet) {
		if (ap.get().isOperationRunning()) {
			com.neuronrobotics.sdk.common.Log.error("Ignoring operation because previous had not finished!");
			return;
		}
		ArrayList<String> copyTarget = new ArrayList<String>();
		copyTarget.addAll(copySet);
		copySet.clear();
		try {
			Paste paste = new Paste().setNames(copyTarget);
			paste.setLocation(new TransformNR(distance, 0, 0));
			ap.addOp(paste).join();
			HashSet<String> namesAdded = paste.getNamesAdded();
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
			// new RuntimeException("Cruise called with nothing selected").printStackTrace();
			return;
		}
		getExecutor().submit(()->{
			List<CSG> selectedCSG = getSelectedCSG(selectedSnapshot);
			List<CSG> cur=getCurrentStateSelected();
			Platform.runLater(() -> {
				if(!moveLock()) {
					com.neuronrobotics.sdk.common.Log.error("On Cruise");
					CSG indicator = selectedCSG.get(0);
					if (selectedCSG.size() > 1) {
						indicator = CSG.unionAll(selectedCSG);
					}
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
                        MeshView meshView = getMeshes().get(c);
                        if (meshView != null)
                            BowlerKernel.runLater(() -> {
                                if (!isResizeLiveMode()) {
                                    meshView.setVisible(false);
                                }
                            });
                    }
					workplane.setIndicator(indicator, gemoAffine);
					workplane.setOnSelectEvent(() -> {
						for (CSG c : selectedCSG) {
							MeshView meshView = getMeshes().get(c);
							if (meshView != null)
								BowlerKernel.runLater(() -> {
									meshView.setVisible(true);
								});
						}
						if (workplane.isClicked()) {
							TransformNR finalLocation = workplane.getCurrentAbsolutePose().times(copy);
							ap.addOp(new MoveCenter().setNames(seleectedNames,ap.get()).setLocation(finalLocation));
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
		if (!isLocked(selectedSnapshot)) {

			ap.addOp(new Lock().setNames(selectedSnapshot));
		} else {
			ap.addOp(new UnLock().setNames(selectedSnapshot));
		}
	}

	private boolean isLocked(List<String> s) {
		for (String name : s) {
			CSG c = getSelectedCSG(name);
			if (c != null)
				if (c.isLock())
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
		for (CSG c : getCurrentState()) {
			if (c.isHide())
				toShow.add(c.getName());
		}
		if (toShow.size() > 0) {
			ap.addOp(new Show().setNames(toShow));
		}
	}

	public void onXor() {
		if (ap.get().isOperationRunning()) {
			com.neuronrobotics.sdk.common.Log.error("Ignoring operation because previous had not finished!");
			return;
		}
		if (selected.size() > 1) {
			getExecutor().submit(() -> {
				try {
					List<String> selectedSnapshot = selectedSnapshot();
					Paste copy = new Paste().setNames(selectedSnapshot);
					ap.addOp(copy).join();
					ArrayList<String> n = new ArrayList<>(copy.getNamesAdded());
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
						CSG g = CaDoodleFile.getByName(ap.get().getCurrentState(), e);
						if (g == null)
							continue;
						if (g.isInGroup())
							continue;
						ArrayList<String> namesToDiff = new ArrayList<String>();
						namesToDiff.add(intersectName);
						namesToDiff.add(e);
						Group cutIntersect = new Group().setNames(namesToDiff);
						ap.addOp(cutIntersect).join();

					}

					selected.clear();
					selected.addAll(results);
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
			com.neuronrobotics.sdk.common.Log.error("Ignoring operation because previous had not finished!");
			return;
		}
		if (selected.size() > 1 || hull) {
			getExecutor().submit(() -> {
				Group groups = new Group().setNames(selectedSnapshot());
				groups.setHull(hull);
				groups.setIntersect(intersect);
				try {
					ap.addOp(groups).join();
					List<CSG> got = ap.get().getCurrentState();
					selected.clear();
					selected.addAll(got);
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

	public void onUngroup() {
		if (ap.get().isOperationRunning()) {
			com.neuronrobotics.sdk.common.Log.error("Ignoring operation because previous had not finished!");
			return;
		}
		ArrayList<CSG> toSelect = new ArrayList<>();
		for (CSG c : getSelectedCSG(selectedSnapshot())) {
			if (c.isGroupResult()) {
				String name = c.getName();
				for (CSG inG : getCurrentState()) {
					if (inG.isInGroup()) {
						if (inG.checkGroupMembership(name)) {
							toSelect.add(inG);
						}
					}
				}
			}
		}
		List<String> selectedSnapshot = selectedSnapshot();

		if (isAGroupSelected()) {
			selected.clear();
			selected.addAll(toSelect);
			ap.addOp(new UnGroup().setNames(selectedSnapshot));
		}
		updateControlsDisplayOfSelected();
		updateRobotLab.run();

	}

	public void onHideShowOpperation() {
		if (ap.get().isOperationRunning()) {
			com.neuronrobotics.sdk.common.Log.error("Ignoring operation because previous had not finished!");
			return;
		}
		CaDoodleOperation op;
		if (isSelectedHidden()) {
			op = new Show().setNames(selectedSnapshot());
		} else {
			op = new Hide().setNames(selectedSnapshot());
		}
		ap.addOp(op);
		updateShowHideButton();
	}

	private void updateShowHideButton() {
		if (isSelectedHidden()) {
			showHideImage.setImage(new Image(MainController.class.getResourceAsStream("litBulb.png")));
		} else {
			showHideImage.setImage(new Image(MainController.class.getResourceAsStream("darkBulb.png")));
		}
	}

	private void updateLockButton() {
		if (isLocked()) {
			lockImage.setImage(new Image(MainController.class.getResourceAsStream("lock.png")));
		} else {
			lockImage.setImage(new Image(MainController.class.getResourceAsStream("unlock.png")));
		}
	}

	public boolean isAnyHidden() {
		boolean ishid = false;
		for (CSG c : getCurrentState()) {
			if (c.isHide()) {
				ishid = true;
			}
		}
		return ishid;
	}

	public boolean isSelectedHidden() {
		boolean ishid = true;
		for (CSG c : getSelectedCSG(selectedSnapshot())) {
			if (!c.isHide()) {
				ishid = false;
			}
		}
		return ishid;
	}

	public List<CSG> getSelectedCSG(Iterable<String> sele) {
		ArrayList<CSG> back = new ArrayList<CSG>();
		for (String sel : sele) {
			CSG t = getSelectedCSG(sel);
			if (t != null) {
				back.add(t);
			}
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
//			List<CSG> selectedCSG = getSelectedCSG(selectedSnapshot());
//			Bounds b = getSellectedBounds(selectedCSG);
//			getControls().initializeAlign(selectedCSG, b, getMeshes());
			List<String> selectedSnapshot = selectedSnapshot();
			List<CSG> selectedCSG = getSelectedCSG(selectedSnapshot);
			getControls().initializeAlign(selectedCSG, selectedSnapshot, getMeshes());
		});

	}

	public void onMirror() {
		if (getControls() == null)
			return;
		getExecutor().submit(() -> {
			getControls().setMode(SpriteDisplayMode.Mirror);
			List<CSG> selectedCSG = getSelectedCSG(selectedSnapshot());
			Bounds b = getSellectedBounds(selectedCSG);
			getControls().initializeMirror(selectedCSG, b, getMeshes());
		});
	}

	public boolean isFocused() {
		return getControls().isFocused();
	}

	public void cancelOperationModes() {
		if (getControls() == null)
			return;
		if (isAlignActive()) {
			getControls().setMode(SpriteDisplayMode.Default);
		}
		if (isMirrorActive()) {
			getControls().setMode(SpriteDisplayMode.Default);
		}
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
		ArrayList<CSG> back= new ArrayList<CSG>();
		for(CSG c:getCurrentState()) {
			for(CSG s:selected) {
				if(c.getName().contentEquals(s.getName()))
					back.add(c);
			}
		}
		return back;
	}
	public Bounds getSellectedBounds() {
		return getSellectedBounds(getCurrentStateSelected());
	}
	public Bounds getSellectedBounds(List<CSG> incoming) {
		return Align.getBounds(incoming, ap.get().getWorkplane(), inWorkplaneBounds);
	}

	public Bounds getBounds(CSG incoming, TransformNR frame) {
		return Align.getBounds(Arrays.asList(incoming), frame, null);
	}

	public Bounds getBounds(CSG incoming, TransformNR frame, HashMap<CSG, Bounds> cache) {
		return Align.getBounds(Arrays.asList(incoming), frame, cache);
	}

	public Bounds getBounds(DHParameterKinematics limb) {
		ArrayList<CSG> parts = new ArrayList<CSG>();
		for(CSG c:getCurrentState()) {
			if(c.getLimbName().isPresent()) {
				if(c.getLimbName().get().contentEquals(limb.getScriptingName())) {
					parts.add(c);
				}
			}
		}
		return getSellectedBounds(parts);
	}


	public void objectWorkplane() {
		if(selected.size()!=1 && !isObjectWorkplane)
			return;
		isObjectWorkplane = !isObjectWorkplane;
		com.neuronrobotics.sdk.common.Log.debug("Setting Object Workplane "+isObjectWorkplane);
		objectWorkplane.getStyleClass().clear();
		if(isObjectWorkplane) {
			CSG c = getSelectedCSG(selectedSnapshot().get(0));
			TransformNR nrToCSG = TransformFactory.csgToNR( MoveCenter.getTotalOffset(c));
			previousWP = ap.get().getWorkplane();
			ap.get().setWorkplane(nrToCSG);
			workplane.placeWorkplaneVisualization();
			objectWorkplane.getStyleClass().add("image-button-focus");
		}else {
			ap.get().setWorkplane(previousWP);
			workplane.placeWorkplaneVisualization();
			objectWorkplane.getStyleClass().add("image-button");
		}
		updateControls();
	}

	public void onDrop() {
		com.neuronrobotics.sdk.common.Log.error("Drop to Workplane");
		getExecutor().submit(()->{
			List<CSG> sel = ap.get().getSelect(selectedSnapshot());
			if(moveLock())
				return;
			List<CSG> cur=getCurrentStateSelected();
			TransformNR wp = ap.get().getWorkplane();
			Transform t = TransformFactory.nrToCSG(wp);
			// Run a down move for each object, since each will move a different amount
			// based on its own bottom
			for (CSG c :sel) {
				double downMove = -c.transformed(t.inverse()).getMinZ();
				TransformNR location = wp.times(new TransformNR(0, 0, downMove)).times(wp.inverse());
				Thread op = ap.addOp(new MoveCenter().setLocation(location)
						.setNames(Arrays.asList(c.getName()),ap.get()));
				try {
					op.join();// wait for the move of this object to finish
				} catch (InterruptedException e) {
					// Auto-generated catch block
					com.neuronrobotics.sdk.common.Log.error(e);

				}
			}
		});


	}

	public void moveInCameraFrame(TransformNR stateUnitVectorTmp) {
		TickToc.tic("Start Move Request");
		if (selected.size() == 0) {
			return;
		}
		getExecutor().submit(()->{
			if(moveLock())
				return;
			TransformNR wp = ap.get().getWorkplane();
			// stateUnitVectorTmp = wp.times(stateUnitVectorTmp).times(wp.inverse());
			TransformNR frameOffset = new TransformNR(0, 0, 0, wp.getRotation());

			MoveCenter m = getActiveMove();
			if (System.currentTimeMillis() - timeSinceLastMove > 2000 || m == null) {
				m = new MoveCenter().setLocation(new TransformNR())
						.setNames(selectedSnapshot(),ap.get());// force a new move event
			}
			MoveCenter mc = m;
			if (ap.get().isOperationRunning()) {
				TickToc.tic("Process running, bailing on new update");
				return;
			}
			timeSinceLastMove = System.currentTimeMillis();
			// TickToc.setEnabled(true);
			TickToc.tic("Start");
			RotationNR getCamerFrameGetRotation;
			double currentRotZ;
			Quadrant quad;
			TransformNR camerFrame = engine.getFlyingCamera().getCamerFrame();
			camerFrame = camerFrame.times(frameOffset);
			getCamerFrameGetRotation = camerFrame.getRotation();
			double toDegrees = Math.toDegrees(getCamerFrameGetRotation.getRotationAzimuthRadians());
			quad = Quadrant.getQuad(toDegrees);
			currentRotZ = Quadrant.QuadrantToAngle(quad);

			TransformNR orentationOffset = new TransformNR(0, 0, 0, new RotationNR(0, currentRotZ - 90, 0));
			TransformNR frame = new TransformNR();// BowlerStudio.getTargetFrame() ;
			TransformNR stateUnitVector = new TransformNR();
			double incement = currentGrid;
			stateUnitVector = orentationOffset.times(stateUnitVectorTmp);
			stateUnitVector.setRotation(new RotationNR());
			boolean updateTrig = false;
			double bound = 0.5;
			if (stateUnitVector.getX() > bound)
				updateTrig = true;
			if (stateUnitVector.getX() < -bound)
				updateTrig = true;
			if (stateUnitVector.getY() > bound)
				updateTrig = true;
			if (stateUnitVector.getY() < -bound)
				updateTrig = true;
			if (stateUnitVector.getZ() > bound)
				updateTrig = true;
			if (stateUnitVector.getZ() < -bound)
				updateTrig = true;
			if (!updateTrig) {
				TickToc.tic("No Update");
				TickToc.toc();
				TickToc.setEnabled(false);
				return;
			}
			stateUnitVector = new TransformNR(roundToNearist(stateUnitVector.getX() * incement, incement),
					roundToNearist(stateUnitVector.getY() * incement, incement),
					roundToNearist(stateUnitVector.getZ() * incement, incement));
			stateUnitVector = wp.times(stateUnitVector).times(wp.inverse());

			TransformNR current = mc.getLocation();
			TransformNR currentRotation = new TransformNR(0, 0, 0, current.getRotation());
			TransformNR tf = current.times(currentRotation.inverse()
					.times(frame.inverse().times(stateUnitVector).times(frame).times(currentRotation)));

			List<String> selectedSnapshot = selectedSnapshot();
			for (String s : selectedSnapshot) {
				// com.neuronrobotics.sdk.common.Log.error("\t" + s);
			}
			CaDoodleOperation op = ap.get().getCurrentOpperation();
			mc.setLocation(tf);
			if (op == mc) {
				if (compareLists(selectedSnapshot, mc.getNamesAddedInThisOperation())) {
					// com.neuronrobotics.sdk.common.Log.error("Move " + tf.toSimpleString());
					TickToc.tic("Update move here");
					//com.neuronrobotics.sdk.common.Log.debug("Update Operation "+tf);
					TickToc.tic("regenerate");
					regenerateCurrent();
					TickToc.tic("save");
					save();
//					TickToc.toc();
					//
//					TickToc.setEnabled(false);
					return;
				}
			}
			//com.neuronrobotics.sdk.common.Log.debug("Add Move Operation "+tf);
			try {
				ap.addOp(mc).join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				com.neuronrobotics.sdk.common.Log.error(e);
			}
			TickToc.setEnabled(false);

		});
	
	}

	public void save() {
		// com.neuronrobotics.sdk.common.Log.error("Save Requested");
		needsSave = true;
		//new Exception("Auto-save called here").printStackTrace();
		if (autosaveThread == null) {
			autosaveThread = new Thread(() -> {
				while(!ap.get().isInitialized()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				while (ap.isOpen()) {
					if (needsSave && ap.get().timeSinceLastUpdate() > 1000) {
						ICadoodleSaveStatusUpdate saveDisplay = ap.get().getSaveUpdate();
						ap.get().setSaveUpdate(null);
						Thread t=new Thread(() -> {
							com.neuronrobotics.sdk.common.Log.debug("Auto save " + ap.get().getSelf().getAbsolutePath());
							ap.save(ap.get());
						});
						t.start();
						needsSave = false;
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							com.neuronrobotics.sdk.common.Log.error(e);
						}
						ap.get().setSaveUpdate(saveDisplay);
						if (t.isAlive() && ap.get().isTimelineOpen()) {
							SplashManager.renderSplashFrame(1, "Saving File");
						}
						try {
							t.join();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							com.neuronrobotics.sdk.common.Log.error(e);
						}
						SplashManager.closeSplash();
					}
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// Auto-generated catch block
						com.neuronrobotics.sdk.common.Log.error(e);
					}
				}
			});
			autosaveThread.setName("Auto-save thread");
			autosaveThread.start();
		}
	}

	public boolean compareLists(List<String> list1, List<String> list2) {
		if (list1 == null || list2 == null) {
			return list1 == list2;
		}

		if (list1.size() != list2.size()) {
			return false;
		}

		HashSet<String> set1 = new HashSet<>(list1);
		HashSet<String> set2 = new HashSet<>(list2);

		return set1.equals(set2);
	}

	private MoveCenter getActiveMove() {
		CaDoodleOperation op = ap.get().getCurrentOpperation();
		if (MoveCenter.class.isInstance(op)) {
			MoveCenter active = (MoveCenter) op;
			if (compareLists(selectedSnapshot(), active.getNamesAddedInThisOperation())) {
				return active;
			}
		}
		return null;
	}

	public static double roundToNearist(double incoiming, double modulo) {
		return modulo * (Math.round(incoiming / modulo));
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
		double screenW = engine.getSubScene().getWidth();
		double screenH = engine.getSubScene().getHeight();
		onCameraChange(screenW, screenH, zoom, az, el, x, y, z);
	}

	public void onCameraChange(double screenW, double screenH, double zoom, double az, double el, double x, double y,
			double z) {
		this.screenW = screenW;
		this.screenH = screenH;
		this.zoom = zoom;
		this.az = az;
		this.el = el;
		this.x = x;
		this.y = y;
		this.z = z;
		if (ap.get() == null || getControls() == null)
			return;

		List<String> selectedSnapshot = selectedSnapshot();
		getExecutor().submit(()->{
			List<CSG> selectedCSG = ap.get().getSelect(selectedSnapshot);
			if (selectedCSG.size() == 0)
				return;
			BowlerStudio.runLater(()->{
//				TickToc.setEnabled(true);
//				TickToc.tic("Start bounds");
				Bounds sellectedBounds = getSellectedBounds(selectedCSG);
//				TickToc.tic("bounds made");
				getControls().updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedSnapshot, sellectedBounds);
//				TickToc.toc();
//				TickToc.setEnabled(false);
			});
			
		});

	}

//	public void setCadoodle(ActiveProject ap) {
//		
//	}

	public void setSnapGrid(double size) {
		this.size = size;
		manipulation.setIncrement(size);
		if (getControls() != null)
			getControls().setSnapGrid(size);
		workplane.setIncrement(size);
	}

	public void setWorkplaneManager(WorkplaneManager workplane) {
		this.workplane = workplane;
		setSnapGrid(currentGrid);
	}

	@Override
	public void onSaveSuggestion() {
		// The Main COntroller triggers this
	}

	public TransformNR getWorkplane() {
		return ap.get().getWorkplane();
	}

	public void setActiveProject(ActiveProject ap) {
		if (this.ap == null) {
			setControls(new ControlSprites(this, engine, selection, manipulation, ap, ruler));
			getControls().setSnapGrid(size);
		}
		this.ap = ap;
	}

	public CaDoodleOperation getCurrentOpperation() {
		return ap.get().getCurrentOpperation();
	}

	public void regenerateCurrent() {
		ap.get().regenerateCurrent();
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
			
			if (c.isHide())
				continue;
			if ((c.isInGroup() && !c.isAlwaysShow()))
				continue;
			if (c.isHole())
				continue;
			back.add(c.clone());
		}
		return back;
	}

	public ArrayList<CSG> getSelectable() {
		ArrayList<CSG> back = new ArrayList<CSG>();
		for (CSG c : getCurrentState()) {
			if (c.isHide())
				continue;
			if ((c.isInGroup() && !c.isAlwaysShow()))
				continue;
			back.add(c);
		}
		return back;
	}

	@Override
	public void onWorkplaneChange(TransformNR newWP) {
		clearBoundsCache();
		if(!workplane.isWorkplaneNotOrigin()) {
			objectWorkplane.getStyleClass().clear();
			objectWorkplane.getStyleClass().add("image-button");
			isObjectWorkplane=false;
		}
		BowlerStudio.runLater(()->{
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

	public HashMap<CSG, MeshView> getMeshes() {
		return meshes;
	}

	public void setMeshes(HashMap<CSG, MeshView> meshes) {
		this.meshes = meshes;
	}

	@Override
	public void onTimelineUpdate(int num) {
		// TODO Auto-generated method stub

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
		return selected.size();
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


}
