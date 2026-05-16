package com.commonwealthrobotics;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.commonwealthrobotics.controls.SelectionSession;
import javafx.collections.ListChangeListener;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ComponentTreePanel implements ICaDoodleStateUpdate {

	private final SelectionSession session;
	private final TreeView<CSG> treeView;
	private final TreeItem<CSG> root;
	private final ContextMenu contextMenu;
	private final MenuItem menuItemGroup;
	private final Menu menuItemMoreGroup;
	private final MenuItem menuItemUngroup;
	private final MenuItem menuItemHideShow;
	private Set<String> selectedNames = new HashSet<>();
	private boolean rebuilding = false;

	public ComponentTreePanel(AnchorPane holder, SelectionSession session) {
		this.session = session;

		root = new TreeItem<>(null);
		root.setExpanded(true);

		treeView = new TreeView<>(root);
		treeView.setShowRoot(true);
		treeView.setCellFactory(tv -> new ComponentTreeCell());
		treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		treeView.focusedProperty().addListener((obs, old, focused) -> treeView.refresh());

		contextMenu = buildContextMenu();
		menuItemGroup = (MenuItem) contextMenu.getItems().get(0);
		menuItemMoreGroup = (Menu) contextMenu.getItems().get(1);
		menuItemUngroup = contextMenu.getItems().get(2);
		menuItemHideShow = contextMenu.getItems().get(4);

		treeView.setOnContextMenuRequested(this::onContextMenuRequested);

		treeView.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			if (contextMenu.isShowing()) {
				contextMenu.hide();
				return;
			}
			if (findNonEmptyTreeCell(event.getPickResult().getIntersectedNode()) != null)
				return;
			treeView.getSelectionModel().clearSelection();
		});

		treeView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TreeItem<CSG>>) change -> {
			if (rebuilding)
				return;
			LinkedHashSet<String> names = new LinkedHashSet<>();
			for (TreeItem<CSG> item : treeView.getSelectionModel().getSelectedItems()) {
				if (item == null)
					continue;
				CSG csg = item.getValue();
				if (csg == null)
					continue; // root
				if (csg.isInGroup()) {
					// Group children are hidden in the viewport; redirect to parent group
					TreeItem<CSG> parent = item.getParent();
					if (parent == null || parent.getValue() == null)
						continue;
					names.add(parent.getValue().getName());
				} else {
					names.add(csg.getName());
				}
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

	private TreeCell<?> findNonEmptyTreeCell(Node start) {
		Node n = start;
		while (n != null && n != treeView) {
			if (n instanceof TreeCell && !((TreeCell<?>) n).isEmpty())
				return (TreeCell<?>) n;
			n = n.getParent();
		}
		return null;
	}

	private void onContextMenuRequested(ContextMenuEvent event) {
		if (findNonEmptyTreeCell(event.getPickResult().getIntersectedNode()) != null) {
			updateContextMenuState();
			contextMenu.show(treeView, event.getScreenX(), event.getScreenY());
			event.consume();
		} else {
			contextMenu.hide();
		}
	}

	private void updateContextMenuState() {
		boolean anySelected = !session.getSelected().isEmpty();
		boolean anyTreeSelected = !treeView.getSelectionModel().getSelectedItems().isEmpty();
		long unlockedCount = session.getSelected().stream().filter(c -> !c.isLock()).count();
		boolean anyGroup = session.getSelected().stream().anyMatch(CSG::isGroupResult);

		menuItemGroup.setDisable(unlockedCount < 2);
		menuItemMoreGroup.setDisable(unlockedCount < 2);
		menuItemUngroup.setDisable(!anyGroup);
		menuItemHideShow.setDisable(!anyTreeSelected);
		contextMenu.getItems().forEach(item -> {
			if (item instanceof SeparatorMenuItem || item == menuItemGroup || item == menuItemMoreGroup
					|| item == menuItemUngroup || item == menuItemHideShow)
				return;
			item.setDisable(!anySelected);
		});
	}

	private ContextMenu buildContextMenu() {
		MenuItem groupItem = new MenuItem("Group");
		groupItem.setOnAction(e -> session.onGroup(false, false));

		MenuItem hullItem = new MenuItem("Hull");
		hullItem.setOnAction(e -> session.onGroup(true, false));

		MenuItem intersectItem = new MenuItem("Intersect");
		intersectItem.setOnAction(e -> session.onGroup(false, true));

		MenuItem xorItem = new MenuItem("Xor");
		xorItem.setOnAction(e -> session.onXor());

		Menu moreGroupMenu = new Menu("More Group");
		moreGroupMenu.getItems().addAll(hullItem, intersectItem, xorItem);

		MenuItem ungroupItem = new MenuItem("Ungroup");
		ungroupItem.setOnAction(e -> session.onUngroup());

		MenuItem makeHoleItem = new MenuItem("Make Hole");
		makeHoleItem.setOnAction(e -> session.setToHole());

		MenuItem makeSolidItem = new MenuItem("Make Solid");
		makeSolidItem.setOnAction(e -> session.setToSolid());

		MenuItem deleteItem = new MenuItem("Delete");
		deleteItem.setOnAction(e -> session.onDelete());

		MenuItem hideShowItem = new MenuItem("Hide / Show");
		hideShowItem.setOnAction(e -> {
			// Hidden items are filtered by selectAll(); re-inject them so the operation sees them
			for (TreeItem<CSG> item : treeView.getSelectionModel().getSelectedItems()) {
				if (item != null && item.getValue() != null && item.getValue().isHide())
					session.getSelected().add(item.getValue());
			}
			session.onHideShowOperation();
		});

		MenuItem lockToggleItem = new MenuItem("Lock / Unlock");
		lockToggleItem.setOnAction(e -> session.lockToggle());

		ContextMenu menu = new ContextMenu(groupItem, moreGroupMenu, ungroupItem, new SeparatorMenuItem(), makeHoleItem,
				makeSolidItem, new SeparatorMenuItem(), deleteItem, hideShowItem, lockToggleItem);
		menu.setAutoHide(true);
		return menu;
	}

	private void rebuildTree(List<CSG> state) {
		rebuilding = true;
		try {
			root.getChildren().clear();
			selectedNames = new HashSet<>();
			for (CSG s : session.getSelected())
				selectedNames.add(s.getName());
			for (CSG csg : state) {
				if (csg.isInGroup())
					continue;
				root.getChildren().add(makeItem(csg, state));
			}
		} finally {
			rebuilding = false;
		}
		treeView.refresh();
	}

	private TreeItem<CSG> makeItem(CSG csg, List<CSG> all) {
		TreeItem<CSG> item = new TreeItem<>(csg);
		item.setExpanded(true);
		if (csg.isGroupResult()) {
			addGroupChildren(item, csg.getName(), all);
		}
		return item;
	}

	private void addGroupChildren(TreeItem<CSG> parent, String groupID, List<CSG> all) {
		for (CSG child : all) {
			if (!child.checkGroupMembership(groupID))
				continue;
			TreeItem<CSG> item = new TreeItem<>(child);
			item.setExpanded(true);
			parent.getChildren().add(item);
			if (child.isGroupResult()) {
				addGroupChildren(item, child.getName(), all);
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
			if (empty) {
				setText(null);
				setGraphic(null);
				setStyle("");
				return;
			}
			if (csg == null) {
				setText("World Origin");
				setStyle("-fx-text-fill: #263d8c; -fx-font-weight: bold;");
				return;
			}
			StringBuilder style = new StringBuilder("-fx-text-fill: #263d8c;");
			if (csg.isGroupResult()) {
				setText("Group: " + csg.getName());
				style.append(" -fx-font-weight: bold;");
			} else {
				setText(csg.getName());
			}
			if (csg.isHide() || isAncestorHidden(getTreeItem())) {
				style.append(" -fx-text-fill: #aaaaaa; -fx-font-style: italic;");
			}
			if (selectedNames.contains(csg.getName())) {
				style.append(" -fx-background-color: #3399ff44;");
			}
			setStyle(style.toString());
		}

		private boolean isAncestorHidden(TreeItem<CSG> item) {
			TreeItem<CSG> parent = item == null ? null : item.getParent();
			while (parent != null) {
				CSG parentCsg = parent.getValue();
				if (parentCsg != null && parentCsg.isHide())
					return true;
				parent = parent.getParent();
			}
			return false;
		}
	}
}
