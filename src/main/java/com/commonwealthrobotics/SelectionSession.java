package com.commonwealthrobotics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.CaDoodleLoader;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleOpperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.MeshView;

public class SelectionSession
implements ICaDoodleStateUpdate{
	private HashMap<CSG, MeshView> meshes = new HashMap<CSG, MeshView>();
	private List<CSG> currentState;
	private ICaDoodleOpperation source;
	private CaDoodleFile cadoodle;
	private TitledPane shapeConfiguration;
	private Accordion shapeConfigurationBox;
	private AnchorPane shapeConfigurationHolder;
	private GridPane configurationGrid;
	private AnchorPane control3d;
	private BowlerStudio3dEngine engine;
	private HashSet<String> selected = new HashSet<String>();
	
	public List<String> selectedSnapshot(){
		ArrayList<String> s = new ArrayList<String>();
		s.addAll(selected);
		return s;
	}

	@Override
	public void onUpdate(List<CSG> currentState, ICaDoodleOpperation source, CaDoodleFile file) {
		this.currentState = currentState;
		// TODO Auto-generated method stub
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
			ArrayList<String> toRemove =  new ArrayList<String>();
			for(String s:selected) {
				boolean exists =false;
				for(CSG c:currentState) {
					if(c.getName().contentEquals(s))
						exists=true;
				}
				if(!exists) {
					toRemove.add(s);
				}
			}
			selected.removeAll(toRemove);
			updateSelection();
		});
	}

	private void displayCSG(CSG c) {
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
		setUpControls(meshView,c.getName());
	}
	private void setUpControls(MeshView meshView,String name) {
		meshView.setOnMousePressed(event->{
			if(event.getButton() == MouseButton.PRIMARY) {
				if(event.isShiftDown()) {
					if(selected.contains(name)) {
						selected.remove(name);
					}else
						selected.add(name);
				}else {
					selected.clear();
					selected.add(name);
				}
				updateSelection();
				event.consume();
			}
		});
	}

	private void updateSelection() {
		System.out.println("\n");
		if(selected.size()>0) {
			for(String s:selected) {
				System.out.println("Current Selection "+s);
			}
		}else {
			System.out.println("None selected");
		}
	}

	public void set(TitledPane shapeConfiguration, Accordion shapeConfigurationBox, AnchorPane shapeConfigurationHolder,
			GridPane configurationGrid, AnchorPane control3d, BowlerStudio3dEngine engine) {
				this.shapeConfiguration = shapeConfiguration;
				this.shapeConfigurationBox = shapeConfigurationBox;
				this.shapeConfigurationHolder = shapeConfigurationHolder;
				this.configurationGrid = configurationGrid;
				this.control3d = control3d;
				this.engine = engine;
				shapeConfigurationHolder.getChildren().clear();
				setupEngineControls();
	}

	private void setupEngineControls() {
		engine.getSubScene().setOnMousePressed(event->{
			System.out.println("Background Click "+event.getSource());
			selected.clear();
			updateSelection();
		});
	}

}
