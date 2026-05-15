package com.commonwealthrobotics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.commonwealthrobotics.controls.SelectionSession;
import javafx.collections.ListChangeListener;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ComponentTreePanel implements ICaDoodleStateUpdate {

	private final SelectionSession session;
	private final AnchorPane holder;
	private final TreeView<CSG> treeView;
	private final TreeItem<CSG> root;
	private boolean rebuilding = false;

	public ComponentTreePanel(AnchorPane holder, SelectionSession session) {
		this.holder = holder;
		this.session = session;

		root = new TreeItem<>(null);
		root.setExpanded(true);

		treeView = new TreeView<>(root);
		treeView.setShowRoot(true);
		treeView.setCellFactory(tv -> new ComponentTreeCell());
		treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		treeView.focusedProperty().addListener((obs, old, focused) -> treeView.refresh());

		treeView.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			Node n = event.getPickResult().getIntersectedNode();
			while (n != null && n != treeView) {
				if (n instanceof TreeCell) {
					if (!((TreeCell<?>) n).isEmpty())
						return;
					break;
				}
				n = n.getParent();
			}
			treeView.getSelectionModel().clearSelection();
		});

		treeView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TreeItem<CSG>>) change -> {
			if (rebuilding)
				return;
			List<String> names = new ArrayList<>();
			for (TreeItem<CSG> item : treeView.getSelectionModel().getSelectedItems()) {
				if (item == null)
					continue;
				CSG csg = item.getValue();
				if (csg == null)
					continue; // root
				String name;
				if (csg.isInGroup()) {
					// Group children are hidden in the viewport; redirect to parent group
					TreeItem<CSG> parent = item.getParent();
					if (parent == null || parent.getValue() == null)
						continue;
					name = parent.getValue().getName();
				} else {
					name = csg.getName();
				}
				if (!names.contains(name))
					names.add(name);
			}
			if (names.isEmpty())
				session.clearSelection();
			else
				session.selectAll(names);
		});

		Label header = new Label("Component Tree");
		header.setStyle(
				"-fx-font-weight: bold; -fx-padding: 4 6 4 6; -fx-border-color: #BBBBBB; -fx-border-width: 0 0 1 0;");
		header.setMaxWidth(Double.MAX_VALUE);

		VBox.setVgrow(treeView, Priority.ALWAYS);
		VBox content = new VBox(header, treeView);
		content.setFillWidth(true);

		AnchorPane.setTopAnchor(content, 0.0);
		AnchorPane.setBottomAnchor(content, 0.0);
		AnchorPane.setLeftAnchor(content, 0.0);
		AnchorPane.setRightAnchor(content, 0.0);

		holder.getChildren().add(content);
	}

	private void rebuildTree(List<CSG> state) {
		rebuilding = true;
		try {
			root.getChildren().clear();
			List<String> selectedNames = new ArrayList<>();
			for (CSG s : session.getSelected()) {
				selectedNames.add(s.getName());
			}
			for (CSG csg : state) {
				if (csg.isInGroup())
					continue;
				if (csg.isHide())
					continue;
				TreeItem<CSG> item = makeItem(csg, state, selectedNames);
				root.getChildren().add(item);
			}
		} finally {
			rebuilding = false;
		}
		treeView.refresh();
	}

	private TreeItem<CSG> makeItem(CSG csg, List<CSG> all, List<String> selectedNames) {
		TreeItem<CSG> item = new TreeItem<>(csg);
		item.setExpanded(true);
		if (csg.isGroupResult()) {
			addGroupChildren(item, csg.getName(), all, selectedNames);
		}
		return item;
	}

	private void addGroupChildren(TreeItem<CSG> parent, String groupID, List<CSG> all, List<String> selectedNames) {
		for (CSG child : all) {
			if (!child.checkGroupMembership(groupID))
				continue;
			TreeItem<CSG> item = new TreeItem<>(child);
			item.setExpanded(true);
			parent.getChildren().add(item);
			if (child.isGroupResult()) {
				addGroupChildren(item, child.getName(), all, selectedNames);
			}
		}
	}

	@Override
	public void onUpdate(List<CSG> currentState, CaDoodleOperation source, CaDoodleFile file) {
		BowlerStudio.runLater(() -> rebuildTree(currentState));
	}

	@Override
	public void onRegenerateDone() {
		BowlerStudio.runLater(() -> treeView.refresh());
	}

	@Override
	public void onSaveSuggestion() {
	}

	@Override
	public void onInitializationDone() {
	}

	@Override
	public void onInitializationStart() {
	}

	@Override
	public void onRegenerateStart(CaDoodleOperation source) {
	}

	@Override
	public void onWorkplaneChange(TransformNR newWP) {
	}

	@Override
	public void onTimelineUpdate(int numberOfNew, File image) {
	}

	private class ComponentTreeCell extends TreeCell<CSG> {

		@Override
		protected void updateItem(CSG csg, boolean empty) {
			super.updateItem(csg, empty);
			// Always set -fx-text-fill inline so it overrides Modena's :focused:selected white text
			setStyle("-fx-text-fill: #263d8c;");
			if (empty) {
				setText(null);
				setGraphic(null);
				return;
			}
			if (csg == null) {
				setText("World Origin");
				setStyle("-fx-text-fill: #263d8c; -fx-font-weight: bold;");
				return;
			}
			if (csg.isGroupResult()) {
				setText("Group: " + csg.getName());
				setStyle("-fx-text-fill: #263d8c; -fx-font-weight: bold;");
			} else {
				setText(csg.getName());
			}
			boolean selected = session.getSelected().stream().anyMatch(s -> s.getName().contentEquals(csg.getName()));
			if (selected) {
				setStyle(getStyle() + " -fx-background-color: #3399ff44;");
			}
		}
	}
}
