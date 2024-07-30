package com.commonwealthrobotics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.bowlerstudio.scripting.CaDoodleLoader;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.*;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Transform;
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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;

public class SelectionSession implements ICaDoodleStateUpdate {
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

	public List<String> selectedSnapshot() {
		ArrayList<String> s = new ArrayList<String>();
		s.addAll(selected);
		return s;
	}

	@Override
	public void onUpdate(List<CSG> currentState, ICaDoodleOpperation source, CaDoodleFile file) {
		this.source = source;
		this.cadoodle = file;
		displayCurrent();
	}

	private void displayCurrent() {
		BowlerStudio.runLater(() -> {
			// for(CSG c:onDisplay) {
			engine.clearUserNode();
			// }
			meshes.clear();
			for (CSG c : (List<CSG>) CaDoodleLoader.process(cadoodle)) {
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
			updateSelection();
		});
	}

	private void displayCSG(CSG c) {
		if(c.isHide())
			return;
		if(c.isInGroup())
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
					selected.clear();
					selected.add(name);
				}
				updateSelection();
				event.consume();
			}
		});
	}

	private void updateSelection() {
		//System.out.println("\n");
		if (selected.size() > 0) {
			for (String s : selected) {
				System.out.println("Current Selection " + s);
			}
			shapeConfigurationHolder.getChildren().clear();
			shapeConfigurationHolder.getChildren().add(shapeConfigurationBox);
			CSG set = getSelectedCSG((String)selected.toArray()[0]);
			if(set==null)
				return;
			Color value = set.getColor();
			colorPicker.setValue(value);
			String hexColor = String.format("#%02X%02X%02X", (int) (value.getRed() * 255), (int) (value.getGreen() * 255),
					(int) (value.getBlue() * 255));

			String style = String.format(" -fx-background-color: %s;", hexColor);
			colorPicker.setStyle(style);
			showButtons();
			updateShowHideButton();
		} else {
			//System.out.println("None selected");
			shapeConfigurationHolder.getChildren().clear();
			hideButtons();
		}
	}

	private CSG getSelectedCSG(String string) {
		for(CSG c: meshes.keySet()) {
			if(c.getName().contentEquals(string))
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
		List<Number> grids = Arrays.asList(0.1, 0.25, 0.5, 1, 2, 5, 10);
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

		snapGrid.getSelectionModel().select(starting);
		this.snapGrid.setOnAction(event -> {
			String selected = this.snapGrid.getSelectionModel().getSelectedItem();
			Double num = map.get(selected);
			if (num != null) {
				currentGrid = num;
				System.out.println("Snap Grid Set to " + currentGrid);
				setKeyBindingFocus();
			}
		});
	}


	public void clearSelection() {
		//System.out.println("Background Click " + event.getSource());
		selected.clear();
		updateSelection();
		setKeyBindingFocus();
	}
	public void selectAll() {
		selected.clear();
		for(CSG c:getCurrentState()) {
			if(c.isInGroup())
				continue;
			if(c.isHide())
				continue;
			selected.add(c.getName());
		}
		updateSelection();
	}

	public void setKeyBindingFocus() {
		if(engine!=null)
			BowlerStudio.runLater(()->	engine.getSubScene().requestFocus());
	}
	public void save() {
		if(cadoodle!=null)
		new Thread(()->{
			try {
				cadoodle.save();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}).start();
	}
	public void setToSolid() {
		if(selected.size()==0)
			return;
		boolean isSilid=true;
		for(String s:selected) {
			if(getSelectedCSG(s).isHole()) {
				isSilid=false;
			}
		}
		if(isSilid)
			return;// all solid
		ToSolid h=  new ToSolid().setNames(selectedSnapshot());
		addOp(h);
		save();
		
	}

	public void setColor(Color value) {
		ToSolid solid = new ToSolid()
				.setNames(selectedSnapshot())
				.setColor(value);
		
		addOp(solid);
		save();
	}

	public void setToHole() {
		if(selected.size()==0)
			return;
		boolean isSilid=false;
		for(String s:selected) {
			if(!getSelectedCSG(s).isHole()) {
				isSilid=true;
			}
		}
		if(!isSilid)
			return;// all holes
		ToHole h=  new ToHole().setNames(selectedSnapshot());
		addOp(h);
		save();
	}
	
	public TransformNR getFocusCenter() {
		if(selected.size()==0)
			return new TransformNR();
		CSG boxes =null;
		for(String c:selected) {
			CSG s=getSelectedCSG(c);
			if(boxes==null)
				boxes=s.getBoundingBox();
			else
				boxes=boxes.union(s.getBoundingBox());
		}
		
		return new TransformNR(boxes.getCenterX(),-boxes.getCenterY(),-boxes.getCenterZ());
	}

	private void addOp(ICaDoodleOpperation h) {
		if(cadoodle==null)
			return;
		System.out.println("Adding "+h.getType());
		cadoodle.addOpperation(h);
	}

	public void setButtons(Button ... buttonsList) {
		buttons = Arrays.asList(buttonsList);
		hideButtons();
	}

	private void hideButtons() {
		BowlerStudio.runLater(()->{
			for(Button b:buttons) {
				b.setDisable(true);
			}
			if(ungroupButton!=null)
				ungroupButton.setDisable(true);
			if(groupButton!=null)
				groupButton.setDisable(true);
			if(allignButton!=null)
				allignButton.setDisable(true);
		});
	}
	private void showButtons() {
		
		BowlerStudio.runLater(()->{
			for(Button b:buttons) {
				b.setDisable(false);
			}
			System.out.println("Number Selected is "+selected.size());
			if(selected.size()>1) {
				groupButton.setDisable(false);
				allignButton.setDisable(false);
			}
			if(isAGroupSelected()) {
				ungroupButton.setDisable(false);
			}
		});
	}

	private boolean isAGroupSelected() {
		for(String s:selected) {
			CSG c=getSelectedCSG(s);
			if(c!=null) {
				if(c.isGroupResult()) {
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
		System.out.println("Delete");
		cadoodle.addOpperation(new Delete().setNames(selectedSnapshot()));		
	}

	public void onCopy() {
		copySetinternal=selectedSnapshot();
	}

	public void onPaste() {
		performPaste(20,copySetinternal);
	}

	private void performPaste(double distance,List<String> copySet) {
		List<CSG> before = cadoodle.getCurrentState();
		ArrayList<String> copyTarget=new ArrayList<String>();
		copyTarget.addAll(copySet);
		cadoodle.addOpperation(new Paste()
				.setOffset(distance)
				.setNames(copyTarget));	
		copySet.clear();
		System.out.println("\n");
		for(int i=before.size();i<getCurrentState().size();i++) {
			String name = getCurrentState().get(i).getName();
			System.out.println("Resetting copy target to "+name);
			copySet.add(name);
		}
	}
	public void Duplicate() {
		performPaste(0,selectedSnapshot());
	}

	public void conCruse() {
		// TODO Auto-generated method stub
		
	}



	public void onLock() {
		cadoodle.addOpperation(new Lock().setNames(selectedSnapshot()));		
	}

	public void showAll() {
		ArrayList<String> toShow = new ArrayList<String>();
		for(CSG c:getCurrentState()) {
			if(c.isHide())
				toShow.add(c.getName());
		}
		if(toShow.size()>0) {
			cadoodle.addOpperation(new Show().setNames(toShow));
		}
	}
	public void onGroup() {
		if(selected.size()>1)
			cadoodle.addOpperation(new Group().setNames(selectedSnapshot()));
		CSG newOne = getCurrentState().get(getCurrentState().size()-1);
		selected.clear();
		selected.add(newOne.getName());
		updateSelection();
	}
	public void onUngroup() {
		ArrayList<String> toSelect = new ArrayList<String>();
		for(CSG c:getSelectedCSG()) {
			if(c.isGroupResult()) {
				String name = c.getName();
				for(CSG inG: getCurrentState()) {
					if(inG.isInGroup()) {
						if(inG.checkGroupMembership(name)) {
							toSelect.add(inG.getName());
						}
					}
				}
			}
		}
		List<String> selectedSnapshot = selectedSnapshot();

		if(isAGroupSelected() ) {
			selected.clear();
			selected.addAll(toSelect);
			cadoodle.addOpperation(new UnGroup().setNames(selectedSnapshot));
		}
		updateSelection();
		
	}

	public void onHideShowOpperation() {
		ICaDoodleOpperation op;
		if(isSelectedHidden()) {
			op=new Show().setNames(selectedSnapshot());
		}else {
			op= new Hide().setNames(selectedSnapshot());
		}
		cadoodle.addOpperation(op);
		updateShowHideButton();
	}

	private void updateShowHideButton() {
		if(isSelectedHidden()) {
			showHideImage.setImage(new Image(MainController.class.getResourceAsStream("litBulb.png")));
		}else {
			showHideImage.setImage(new Image(MainController.class.getResourceAsStream("darkBulb.png")));
		}
	}
	public boolean isAnyHidden() {
		boolean ishid = false;
		for(CSG c:getCurrentState()) {
			if(c.isHide()) {
				ishid=true;
			}
		}
		return ishid;
	}
	public boolean isSelectedHidden() {
		boolean ishid = true;
		for(CSG c:getSelectedCSG()) {
			if(!c.isHide()) {
				ishid=false;
			}
		}
		return ishid;
	}
	
	public List<CSG> getSelectedCSG(){
		ArrayList<CSG> back = new ArrayList<CSG>();
		for(String sel:selected) {
			CSG t=getSelectedCSG(sel);
			if(t!=null) {
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
		return cadoodle.getCurrentState();
	}



	public void dropToWorkplane() {
		System.out.println("Drop to Workplane");
	}
	public enum Quadrent {
		first,second,third,fourth
	}
	Quadrent getQuad(double angle) {
		if(angle>45 && angle<135)
			return Quadrent.first;
		if(angle>135 ||angle < (-135))
			return Quadrent.second;
		if(angle>-135&& angle<-45)
			return Quadrent.third;
		if(angle>-45&&angle<45)
			return Quadrent.fourth;
		throw new RuntimeException("Impossible nummber! "+angle);
	}
	double 	QuadrentToAngle(Quadrent q) {
		switch(q) {
			case first:
			return 90;
			case second:
			return 180;
			case third:
			return -90;
			case fourth:
			default:
			return 0;
		}
	}
	public void moveInCameraFrame(TransformNR stateUnitVectorTmp) {
		if(selected.size()==0)
			return;
		RotationNR getCamerFrameGetRotation;
		double currentRotZ ;
		Quadrent quad ;
		getCamerFrameGetRotation = engine.getFlyingCamera().getCamerFrame().getRotation();
		double toDegrees = Math.toDegrees(getCamerFrameGetRotation.getRotationAzimuth());
		quad = getQuad(toDegrees);
		currentRotZ = QuadrentToAngle(quad);
		
		TransformNR orentationOffset = new TransformNR(0,0,0,new RotationNR(0,currentRotZ-90,0));
		TransformNR frame = new TransformNR();// BowlerStudio.getTargetFrame() ;
		TransformNR frameOffset = new TransformNR(0,0,0,frame.getRotation());
		TransformNR stateUnitVector = new TransformNR();
		double incement = currentGrid;
		stateUnitVector= orentationOffset.times(stateUnitVectorTmp);
		stateUnitVector.setRotation(new RotationNR());
		boolean updateTrig = false;
		double bound =0.5;
		if(stateUnitVector.getX()>bound)
			updateTrig=true;
		if(stateUnitVector.getX()<-bound)
			updateTrig=true;
		if(stateUnitVector.getY()>bound)
			updateTrig=true;
		if(stateUnitVector.getY()<-bound)
			updateTrig=true;
		if(stateUnitVector.getZ()>bound)
			updateTrig=true;
		if(stateUnitVector.getZ()<-bound)
			updateTrig=true;
		if(!updateTrig)
			return;
		stateUnitVector=new TransformNR(
			roundToNearist(stateUnitVector.getX()*incement,incement),
			roundToNearist(stateUnitVector.getY()*incement,incement),
			roundToNearist(stateUnitVector.getZ()*incement,incement));
		MoveCenter mc =getActiveMove();
		if(System.currentTimeMillis()-timeSinceLastMove>2000 || mc==null) {
			mc=new MoveCenter()
					.setLocation(new TransformNR())
					.setNames(selectedSnapshot());// force a new move event
		}
		timeSinceLastMove=System.currentTimeMillis();
		TransformNR current = mc.getLocation();
		TransformNR currentRotation = new TransformNR(0,0,0,current.getRotation());
		TransformNR tf= current.times(	
								currentRotation.inverse().times(
									frameOffset.inverse().times(stateUnitVector).times(frameOffset)						
								.times(currentRotation)
							)
						);
		
		
		List<String> selectedSnapshot = selectedSnapshot();
		for(String s:selectedSnapshot) {
			System.out.println("\t"+s);
		}
		ICaDoodleOpperation op = cadoodle.currentOpperation();

		if(op==mc) {
			if(compareLists(selectedSnapshot, mc.getNames())) {
				System.out.println("Move "+tf.toSimpleString());
				mc.setLocation(tf);
				cadoodle.regenerateCurrent();
				return;
			}
		}
		cadoodle.addOpperation(mc);
		
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
		ICaDoodleOpperation op = cadoodle.currentOpperation();
		if(MoveCenter.class.isInstance(op)) {
			MoveCenter active = (MoveCenter)op;
			if(compareLists(selectedSnapshot(), active.getNames())) {
				return active;
			}
		}
		return null;
    }


	double roundToNearist(double incoiming, double modulo) {
		return modulo*(Math.round(incoiming/modulo));
	}

}
