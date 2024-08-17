package com.commonwealthrobotics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.neuronrobotics.bowlerkernel.Bezier3d.IInteractiveUIElementProvider;
import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;
import com.neuronrobotics.bowlerstudio.BowlerKernel;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.CaDoodleLoader;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.*;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.VirtualCameraMobileBase;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Transform;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.SubScene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;

@SuppressWarnings("unused")
public class SelectionSession implements ICaDoodleStateUpdate {
	private ActiveProject ap = new ActiveProject();

	private ControlSprites controls;
	private HashMap<CSG, MeshView> meshes = new HashMap<CSG, MeshView>();
	private ICaDoodleOpperation source;
	private CaDoodleFile cadoodle;
	private TitledPane shapeConfiguration;
	private Accordion shapeConfigurationBox;
	private AnchorPane shapeConfigurationHolder;
	private GridPane configurationGrid;
	private AnchorPane control3d;
	private BowlerStudio3dEngine engine;
	private HashSet<String> selected = new HashSet<String>();
	private ColorPicker colorPicker;
	private ComboBox<String> snapGrid;
	private double currentGrid = 1.0;
	private List<Button> buttons;
	private Button ungroupButton;
	private Button groupButton;
	private ImageView showHideImage;
	private List<String> copySetinternal;
	private Button allignButton;
	private long timeSinceLastMove = System.currentTimeMillis();
	private double screenW;
	private double screenH;
	private double zoom;
	private double az;
	private double el;
	private double x;
	private double y;
	private double z;
	private Thread autosaveThread=null;
	private boolean needsSave=false;
	private Affine selection = new Affine();
	private Manipulation manipulation = new Manipulation(selection, new Vector3d(1, 1, 0), new TransformNR());
	private EventHandler<MouseEvent> mouseMover = manipulation.getMouseEvents();

	private double size;

	private WorkplaneManager workplane;
	
	public SelectionSession(){
		manipulation.addSaveListener(()->{
			System.out.println("Objects Moved! "+manipulation.getGlobalPose().toSimpleString());
			Thread t= getCadoodle().addOpperation(new MoveCenter()
					.setLocation(manipulation.getGlobalPose().copy())
					.setNames(selectedSnapshot()));
			try {
				t.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		});
		manipulation.addEventListener(()->{
			BowlerKernel.runLater(()->updateControls()) ;
		});
		manipulation.setUi(new IInteractiveUIElementProvider() {
			public void runLater(Runnable r) {
				BowlerKernel.runLater(r);
			}

			public TransformNR getCamerFrame() {
				return engine.getFlyingCamera().getCamerFrame();
			}

			public double getCamerDepth() {
				return engine.getFlyingCamera().getZoomDepth();
			}
		});
	}
	
	public List<String> selectedSnapshot() {
		ArrayList<String> s = new ArrayList<String>();
		s.addAll(selected);
		return s;
	}

	@Override
	public void onUpdate(List<CSG> currentState, ICaDoodleOpperation source, CaDoodleFile file) {
		this.source = source;
		this.setCadoodle(file);
		manipulation.set(0, 0, 0);
		displayCurrent();
		
	}

	private void displayCurrent() {
		BowlerStudio.runLater(() -> {
			for (CSG c : meshes.keySet()) {
				engine.removeUserNode(meshes.get(c));
			}
			meshes.clear();
			for (CSG c : (List<CSG>) CaDoodleLoader.process(getCadoodle())) {
				displayCSG(c);
			}
			ArrayList<String> toRemove = new ArrayList<String>();
			for (String s : selected) {
				boolean exists = false;
				for (CSG c : getCurrentState()) {
					if (c.getName().contentEquals(s) && !c.isInGroup())
						exists = true;
				}
				if (!exists) {
					toRemove.add(s);
				}
			}
			selected.removeAll(toRemove);
			workplane.updateMeshes(meshes);
			updateSelection();
		});
	}

	private void displayCSG(CSG c) {
		if (c.isHide())
			return;
		if (c.isInGroup())
			return;
		MeshView meshView = c.getMesh();
		if (c.isHole()) {
			Image texture = new Image(getClass().getResourceAsStream("holeTexture.png"));

			meshView = new TexturedCSG(c, texture);
			// addTextureCoordinates(meshView);
			// Create a new PhongMaterial

			// Set opacity for semi-transparency
			meshView.setOpacity(0.75); // Adjust this value between 0.0 and 1.0 as needed
		}
		engine.addUserNode(meshView);
		meshes.put(c, meshView);
		setUpControls(meshView, c.getName());
	}

	private void setUpControls(MeshView meshView, String name) {
		meshView.setOnMousePressed(event -> {
			if (event.getButton() == MouseButton.PRIMARY) {
				if (event.isShiftDown()) {
					if (selected.contains(name)) {
						selected.remove(name);
					} else
						selected.add(name);
				} else {
					if(!selected.contains(name)) {
						selected.clear();
						selected.add(name);
					}
				}
				updateSelection();
				event.consume();
			}
		});
		
	}

	private void updateSelection() {
		// System.out.println("\n");

		if (selected.size() > 0) {
//			for (String s : selected) {
//				System.out.println("Current Selection " + s);
//			}
			
			shapeConfigurationHolder.getChildren().clear();
			shapeConfigurationHolder.getChildren().add(shapeConfigurationBox);
			CSG set = getSelectedCSG((String) selected.toArray()[0]);
			if (set == null)
				return;
			Color value = set.getColor();
			colorPicker.setValue(value);
			String hexColor = String.format("#%02X%02X%02X", (int) (value.getRed() * 255),
					(int) (value.getGreen() * 255), (int) (value.getBlue() * 255));

			String style = String.format(" -fx-background-color: %s;", hexColor);
			colorPicker.setStyle(style);
			showButtons();
			updateShowHideButton();
			for(CSG c: getCurrentState()) {
				MeshView meshView = meshes.get(c);
				if(meshView!=null) {
					meshView.getTransforms().remove(selection);
					meshView.getTransforms().remove(controls.getViewRotation());
					meshView.removeEventFilter(MouseEvent.ANY, mouseMover);
				}

			}
			TransformFactory.nrToAffine(new TransformNR(), selection);
			TransformFactory.nrToAffine(new TransformNR(), controls.getViewRotation());
			for(CSG c: getSelectedCSG()) {
				MeshView meshView = meshes.get(c);
				if(meshView!=null) {
					meshView.getTransforms().addAll(
							controls.getViewRotation(),
							selection);
					meshView.addEventFilter(MouseEvent.ANY, mouseMover);
				}
			}
			
		} else {
			for(CSG c: getCurrentState()) {
				MeshView meshView = meshes.get(c);
				if(meshView!=null) {
					meshView.getTransforms().remove(selection);
					meshView.getTransforms().remove(controls.getViewRotation());
					meshView.removeEventFilter(MouseEvent.ANY, mouseMover);
				}
			}
			// System.out.println("None selected");
			shapeConfigurationHolder.getChildren().clear();
			hideButtons();
			controls.clearSelection();
		}
		updateControls();
	}

	private CSG getSelectedCSG(String string) {
		for (CSG c : meshes.keySet()) {
			if (c.getName().contentEquals(string))
				return c;
		}
		return null;
	}

	public void set(TitledPane shapeConfiguration, Accordion shapeConfigurationBox, AnchorPane shapeConfigurationHolder,
			GridPane configurationGrid, AnchorPane control3d, BowlerStudio3dEngine engine, ColorPicker colorPicker,
			ComboBox<String> snapGrid) {
		this.shapeConfiguration = shapeConfiguration;
		this.shapeConfigurationBox = shapeConfigurationBox;
		this.shapeConfigurationHolder = shapeConfigurationHolder;
		this.configurationGrid = configurationGrid;
		this.control3d = control3d;
		this.engine = engine;
		this.colorPicker = colorPicker;
		this.snapGrid = snapGrid;
		setupSnapGrid();
		

	}

	private void setupSnapGrid() {
		List<Number> grids = Arrays.asList(0.1, 0.25, 0.5, 1,2,(25.4/8.0),  5,(25.4/4.0), 10);
		HashMap<String, Double> map = new HashMap<>();
		String starting = String.format("%.2f", currentGrid);
		map.put("Off", 0.001);
		this.snapGrid.getItems().add("Off");
		for (Number n : grids) {
			String result = String.format("%.2f", n.doubleValue());
			String key = result + " mm";
			map.put(key, n.doubleValue());
			this.snapGrid.getItems().add(key);
		}

		snapGrid.getSelectionModel().select(starting + " mm");
		setSnapGrid(currentGrid);
		this.snapGrid.setOnAction(event -> {
			String selected = this.snapGrid.getSelectionModel().getSelectedItem();
			Double num = map.get(selected);
			if (num != null) {
				currentGrid = num;
				setSnapGrid(currentGrid);
				System.out.println("Snap Grid Set to " + currentGrid);
				setKeyBindingFocus();
			}
		});
	}

	public void clearSelection() {
		// System.out.println("Background Click " + event.getSource());
		selected.clear();
		updateSelection();
		setKeyBindingFocus();
	}
	public void selectAll(HashSet<String> select) {
		selected.clear();
		for (CSG c : getCurrentState()) {
			if (c.isInGroup())
				continue;
			if (c.isHide())
				continue;
			if(select.contains(c.getName()))
				selected.add(c.getName());
		}
		BowlerStudio.runLater(()->{

			updateSelection();
		});
	}
	public void selectAll() {
		selected.clear();
		for (CSG c : getCurrentState()) {
			if (c.isInGroup())
				continue;
			if (c.isHide())
				continue;
			selected.add(c.getName());
		}
		updateSelection();
	}

	public void setKeyBindingFocus() {
		if (engine != null)
			BowlerStudio.runLater(() -> engine.getSubScene().requestFocus());
	}

	public void setToSolid() {
		if (selected.size() == 0)
			return;
		boolean isSilid = true;
		for (String s : selected) {
			if (getSelectedCSG(s).isHole()) {
				isSilid = false;
			}
		}
		if (isSilid)
			return;// all solid
		ToSolid h = new ToSolid().setNames(selectedSnapshot());
		addOp(h);

	}

	public void setColor(Color value) {
		ToSolid solid = new ToSolid().setNames(selectedSnapshot()).setColor(value);

		addOp(solid);
	}

	public void setToHole() {
		if (selected.size() == 0)
			return;
		boolean isSilid = false;
		for (String s : selected) {
			if (!getSelectedCSG(s).isHole()) {
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
		CSG boxes = null;
		for (String c : selected) {
			CSG s = getSelectedCSG(c);
			if (boxes == null)
				boxes = s.getBoundingBox();
			else
				boxes = boxes.union(s.getBoundingBox());
		}

		return new TransformNR(boxes.getCenterX(), -boxes.getCenterY(), -boxes.getCenterZ());
	}

	private void addOp(ICaDoodleOpperation h) {
		if (getCadoodle() == null)
			return;
		if (getCadoodle().isOperationRunning()) {
			System.err.println("Ignoring operation because previous had not finished!");
			return;
		}
		//System.out.println("Adding " + h.getType());
		getCadoodle().addOpperation(h);
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
			if (ungroupButton != null)
				ungroupButton.setDisable(true);
			if (groupButton != null)
				groupButton.setDisable(true);
			if (allignButton != null)
				allignButton.setDisable(true);
		});
	}

	private void showButtons() {

		BowlerStudio.runLater(() -> {
			for (Button b : buttons) {
				b.setDisable(false);
			}
			//System.out.println("Number Selected is " + selected.size());
			if (selected.size() > 1) {
				groupButton.setDisable(false);
				allignButton.setDisable(false);
			}
			if (isAGroupSelected()) {
				ungroupButton.setDisable(false);
			}
		});
	}

	private boolean isAGroupSelected() {
		for (String s : selected) {
			CSG c = getSelectedCSG(s);
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
		if (getCadoodle().isOperationRunning()) {
			System.err.println("Ignoring operation because previous had not finished!");
			return;
		}
		System.out.println("Delete");
		getCadoodle().addOpperation(new Delete().setNames(selectedSnapshot()));
	}

	public void onCopy() {
		copySetinternal = selectedSnapshot();
	}
	public void Duplicate() {
		new Thread(()->performPaste(0, selectedSnapshot())).start();;
	}
	public void onPaste() {
		new Thread(()->performPaste(20, copySetinternal)).start();;

	}

	private void performPaste(double distance, List<String> copySet) {
		if (getCadoodle().isOperationRunning()) {
			System.err.println("Ignoring operation because previous had not finished!");
			return;
		}
		ArrayList<String> copyTarget = new ArrayList<String>();
		copyTarget.addAll(copySet);
		copySet.clear();
		try {
			Paste setNames = new Paste().setOffset(distance).setNames(copyTarget);
			getCadoodle().addOpperation(setNames).join();
			selectAll(setNames.getNewNames());
			onCopy();
			BowlerStudio.runLater(()->updateSelection());
		} catch (CadoodleConcurrencyException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}



	public void onCruse() {
		TransformNR wp = cadoodle.getWorkplane();
		
		
		System.out.println("On Cruse");
		List<CSG> selectedCSG = getSelectedCSG();
		CSG indicator = selectedCSG.get(0);
		if(selectedCSG.size()>1) {
			indicator=CSG.unionAll(selectedCSG);
		}
		List<String> seleectedNames=selectedSnapshot();
		TransformNR o = new TransformNR(RotationNR.getRotationY(180));

		TransformNR g =o.times(wp.inverse());
		Transform tf = TransformFactory.nrToCSG(g);
		Bounds bounds = indicator.transformed(tf).getBounds();
		Vector3d center=bounds.getCenter();
		TransformNR b = new TransformNR(-center.x,-center.y,-bounds.getMin().z);
		g=b.times(g);
		
		Affine gemoAffine = new Affine();
		TransformNR copy = g.copy();
		BowlerKernel.runLater(()->{
			TransformFactory.nrToAffine(copy, gemoAffine);
		});

		for(CSG c:selectedCSG) {
			MeshView meshView = meshes.get(c);
			if(meshView!=null)
			BowlerKernel.runLater(()->{
				meshView.setVisible(false);
			});
		}
		workplane.setIndicator(indicator, gemoAffine);
		workplane.setOnSelectEvent(()->{
			for(CSG c:selectedCSG) {
				MeshView meshView = meshes.get(c);
				if(meshView!=null)
				BowlerKernel.runLater(()->{
					meshView.setVisible(true);
				});
			}
			if(workplane.isClicked()) {
				TransformNR finalLocation = workplane.getCurrentAbsolutePose().times(copy);
				cadoodle.addOpperation(new MoveCenter().setNames(seleectedNames).setLocation(finalLocation));
			}	
		});
		workplane.setCurrentAbsolutePose(copy.inverse());
		workplane.activate();
	}

	public void onLock() {
		if (getCadoodle().isOperationRunning()) {
			System.err.println("Ignoring operation because previous had not finished!");
			return;
		}
		getCadoodle().addOpperation(new Lock().setNames(selectedSnapshot()));
	}

	public void showAll() {
		if (getCadoodle().isOperationRunning()) {
			System.err.println("Ignoring operation because previous had not finished!");
			return;
		}
		ArrayList<String> toShow = new ArrayList<String>();
		for (CSG c : getCurrentState()) {
			if (c.isHide())
				toShow.add(c.getName());
		}
		if (toShow.size() > 0) {
			getCadoodle().addOpperation(new Show().setNames(toShow));
		}
	}

	public void onGroup() {
		if (getCadoodle().isOperationRunning()) {
			System.err.println("Ignoring operation because previous had not finished!");
			return;
		}
		if (selected.size() > 1) {
			new Thread(()->{
				Group setNames = new Group().setNames(selectedSnapshot());
				try {
					getCadoodle().addOpperation(setNames).join();
					selected.clear();
					selected.add(setNames.getGroupID());
					BowlerStudio.runLater(()->updateSelection());
				} catch (CadoodleConcurrencyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}).start();
		}else
			updateSelection();

	}

	public void onUngroup() {
		if (getCadoodle().isOperationRunning()) {
			System.err.println("Ignoring operation because previous had not finished!");
			return;
		}
		ArrayList<String> toSelect = new ArrayList<String>();
		for (CSG c : getSelectedCSG()) {
			if (c.isGroupResult()) {
				String name = c.getName();
				for (CSG inG : getCurrentState()) {
					if (inG.isInGroup()) {
						if (inG.checkGroupMembership(name)) {
							toSelect.add(inG.getName());
						}
					}
				}
			}
		}
		List<String> selectedSnapshot = selectedSnapshot();

		if (isAGroupSelected()) {
			selected.clear();
			selected.addAll(toSelect);
			getCadoodle().addOpperation(new UnGroup().setNames(selectedSnapshot));
		}
		updateSelection();

	}

	public void onHideShowOpperation() {
		if (getCadoodle().isOperationRunning()) {
			System.err.println("Ignoring operation because previous had not finished!");
			return;
		}
		ICaDoodleOpperation op;
		if (isSelectedHidden()) {
			op = new Show().setNames(selectedSnapshot());
		} else {
			op = new Hide().setNames(selectedSnapshot());
		}
		getCadoodle().addOpperation(op);
		updateShowHideButton();
	}

	private void updateShowHideButton() {
		if (isSelectedHidden()) {
			showHideImage.setImage(new Image(MainController.class.getResourceAsStream("litBulb.png")));
		} else {
			showHideImage.setImage(new Image(MainController.class.getResourceAsStream("darkBulb.png")));
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
		for (CSG c : getSelectedCSG()) {
			if (!c.isHide()) {
				ishid = false;
			}
		}
		return ishid;
	}

	public List<CSG> getSelectedCSG() {
		ArrayList<CSG> back = new ArrayList<CSG>();
		for (String sel : selected) {
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

	public void onAllign() {
		// TODO Auto-generated method stub

	}

	public void setAllignButton(Button allignButton) {
		this.allignButton = allignButton;
	}

	public List<CSG> getCurrentState() {
		if (getCadoodle()==null)
			return new ArrayList<CSG>();
		return getCadoodle().getCurrentState();
	}

	public Bounds getSellectedBounds(List<CSG> incoming) {
		Vector3d min = null;
		Vector3d max = null;
		for (CSG c : incoming) {
			Vector3d min2 = c.getBounds().getMin().clone();
			Vector3d max2 = c.getBounds().getMax().clone();
			if (min == null)
				min = min2;
			if (max == null)
				max = max2;
			if (min2.x < min.x)
				min.x = min2.x;
			if (min2.y < min.y)
				min.y = min2.y;
			if (min2.z < min.z)
				min.z = min2.z;
			if (max.x < max2.x)
				max.x = max2.x;
			if (max.y < max2.y)
				max.y = max2.y;
			if (max.z < max2.z)
				max.z = max2.z;
		}

		return new Bounds(min, max);
	}

	public void onDrop() {
		System.out.println("Drop to Workplane");
		new Thread(()->{
			TransformNR wp = cadoodle.getWorkplane();
			Transform t= TransformFactory.nrToCSG(wp);
			// Run a down move for each object, since each will move a different amount based on its own bottom
			for(CSG c:getSelectedCSG()) {
				double downMove = -c.transformed(t.inverse()).getMinZ();
				TransformNR location = wp.times(new TransformNR(0,0,downMove)).times(wp.inverse());
				Thread op = getCadoodle().addOpperation(
						new MoveCenter()
						.setLocation(location)
						.setNames(Arrays.asList(c.getName()))
						);
				try {
					op.join();// wait for the move of this object to finish
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					
				}
			}
		}).start();
	}

	public void moveInCameraFrame(TransformNR stateUnitVectorTmp) {
		if (selected.size() == 0)
			return;
		TransformNR wp=cadoodle.getWorkplane();
		//stateUnitVectorTmp = wp.times(stateUnitVectorTmp).times(wp.inverse());

		MoveCenter mc = getActiveMove();
		if (System.currentTimeMillis() - timeSinceLastMove > 2000 || mc == null) {
			mc = new MoveCenter().setLocation(new TransformNR()).setNames(selectedSnapshot());// force a new move event
		}
		timeSinceLastMove = System.currentTimeMillis();
		if (getCadoodle().isOperationRunning()) {
			System.err.println("Ignoring operation because previous had not finished!");
			return;
		}
		RotationNR getCamerFrameGetRotation;
		double currentRotZ;
		Quadrent quad;
		getCamerFrameGetRotation = engine.getFlyingCamera().getCamerFrame().getRotation();
		double toDegrees = Math.toDegrees(getCamerFrameGetRotation.getRotationAzimuth());
		quad = Quadrent.getQuad(toDegrees);
		currentRotZ = Quadrent.QuadrentToAngle(quad);

		TransformNR orentationOffset = new TransformNR(0, 0, 0, new RotationNR(0, currentRotZ - 90, 0));
		TransformNR frame = new TransformNR();// BowlerStudio.getTargetFrame() ;
		TransformNR frameOffset = new TransformNR(0, 0, 0, frame.getRotation());
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
		if (!updateTrig)
			return;
		stateUnitVector = new TransformNR(roundToNearist(stateUnitVector.getX() * incement, incement),
				roundToNearist(stateUnitVector.getY() * incement, incement),
				roundToNearist(stateUnitVector.getZ() * incement, incement));
		stateUnitVector=wp.times(stateUnitVector).times(wp.inverse());

		
		TransformNR current = mc.getLocation();
		TransformNR currentRotation = new TransformNR(0, 0, 0, current.getRotation());
		TransformNR tf = current.times(currentRotation.inverse()
				.times(frameOffset.inverse().times(stateUnitVector).times(frameOffset).times(currentRotation)));

		List<String> selectedSnapshot = selectedSnapshot();
		for (String s : selectedSnapshot) {
			//System.out.println("\t" + s);
		}
		ICaDoodleOpperation op = getCadoodle().currentOpperation();
		if (op == mc) {
			if (compareLists(selectedSnapshot, mc.getNames())) {
				//System.out.println("Move " + tf.toSimpleString());
				mc.setLocation(tf);
				getCadoodle().regenerateCurrent();
				save();
				return;
			}
		}

		getCadoodle().addOpperation(mc);

	}
	public void save() {
		//System.out.println("Save Requested");
		needsSave=true;
		if(autosaveThread == null) {
			autosaveThread= new Thread(()->{
				while(ap.isOpen()) {
					if(needsSave) {
						System.out.println("Auto save "+getCadoodle().getSelf().getAbsolutePath());
						ap.save(getCadoodle());
						needsSave=false;
					}
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
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
		ICaDoodleOpperation op = getCadoodle().currentOpperation();
		if (MoveCenter.class.isInstance(op)) {
			MoveCenter active = (MoveCenter) op;
			if (compareLists(selectedSnapshot(), active.getNames())) {
				return active;
			}
		}
		return null;
	}

	double roundToNearist(double incoiming, double modulo) {
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
		if (getCadoodle() == null || controls == null)
			return;

		List<CSG> selectedCSG = getCadoodle().getSelect(selectedSnapshot());
		if (selectedCSG.size() == 0)
			return;
		controls.updateControls(screenW, screenH, zoom, az, el, x, y, z, selectedSnapshot(), getSellectedBounds(selectedCSG));
	}

	public void loadActive(MainController mainController) throws Exception {
		ap.loadActive(mainController);
	}

	public CaDoodleFile getCadoodle() {
		return cadoodle;
	}

	public void setCadoodle(CaDoodleFile cadoodle) {
		if(this.cadoodle==null) {
			controls = new ControlSprites( this, engine,selection,manipulation,cadoodle);
			controls.setSnapGrid(size);
		}
		this.cadoodle = cadoodle;
	}
	public void setSnapGrid(double size) {
		this.size = size;
		manipulation.setIncrement(size);
		if(controls!=null)
			controls.setSnapGrid(size);
	}

	public void setWorkplaneManager(WorkplaneManager workplane) {
		this.workplane = workplane;
		
	}

}
