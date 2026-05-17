package com.commonwealthrobotics;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.commonwealthrobotics.controls.SelectionSession;
import javafx.collections.ListChangeListener;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Align;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ICaDoodleStateUpdate;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Mirror;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.MoveCenter;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Resize;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.Cursor;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ComponentTreePanel implements ICaDoodleStateUpdate {
	private static final String ROOT_NODE_ID = "__root__";
	private static final double DEFAULT_WIDTH = 220.0;
	private static final double MIN_WIDTH = 180.0;
	private static final double MAX_WIDTH = 520.0;
	private static final double RESIZE_HANDLE_WIDTH = 7.0;

	private final SelectionSession session;
	private final ActiveProject ap;
	private final AnchorPane holder;
	private final TreeView<CSG> treeView;
	private final TreeItem<CSG> root;
	private final ContextMenu contextMenu;
	private MenuItem menuItemEdit;
	private MenuItem menuItemGroup;
	private Menu menuItemMoreGroup;
	private MenuItem menuItemUngroup;
	private MenuItem menuItemHideShow;
	private TreeItem<CSG> contextMenuTarget;
	private String rootLabel = "Project";
	private boolean rebuilding = false;
	private boolean syncingTreeSelection = false;
	private double resizeStartSceneX;
	private double resizeStartWidth;
	private static final String TREE_CELL_GROUP = "component-tree-cell-group";
	private static final String TREE_CELL_HIDDEN = "component-tree-cell-hidden";
	private static final String TREE_CELL_SELECTED = "component-tree-cell-selected";
	private static final String TREE_CELL_ROOT = "component-tree-cell-root";

	public ComponentTreePanel(AnchorPane holder, SelectionSession session, ActiveProject ap) {
		this.session = session;
		this.ap = ap;
		this.holder = holder;

		root = new TreeItem<>(null);
		root.setExpanded(true);

		treeView = new TreeView<>(root);
		treeView.setShowRoot(true);
		treeView.setCellFactory(tv -> new ComponentTreeCell());
		treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		treeView.getStyleClass().add("component-tree-view");
		session.addSelectionListener(() -> BowlerStudio.runLater(() -> {
			syncTreeSelectionFromSession();
			treeView.refresh();
		}));

		treeView.focusedProperty().addListener((obs, old, focused) -> treeView.refresh());

		contextMenu = buildContextMenu();

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
			if (rebuilding || syncingTreeSelection)
				return;
			syncSessionFromTreeSelectionModel();
		});

		treeView.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
			if (rebuilding || syncingTreeSelection || event.getButton() != MouseButton.PRIMARY)
				return;
			if (findNonEmptyTreeCell(event.getPickResult().getIntersectedNode()) == null)
				return;
			syncSessionFromTreeSelectionModel();
		});

		Label header = new Label("Component Tree");
		header.getStyleClass().add("component-tree-header");
		header.setMaxWidth(Double.MAX_VALUE);

		VBox.setVgrow(treeView, Priority.ALWAYS);
		VBox content = new VBox(header, treeView);
		content.getStyleClass().add("component-tree-panel");
		content.setFillWidth(true);

		AnchorPane.setTopAnchor(content, 0.0);
		AnchorPane.setBottomAnchor(content, 0.0);
		AnchorPane.setLeftAnchor(content, 0.0);
		AnchorPane.setRightAnchor(content, 0.0);

		holder.getChildren().add(content);
		holder.getChildren().add(buildResizeHandle());
		holder.setMinWidth(MIN_WIDTH);
		if (holder.getPrefWidth() <= 0)
			holder.setPrefWidth(DEFAULT_WIDTH);
	}

	private Region buildResizeHandle() {
		Region handle = new Region();
		handle.getStyleClass().add("component-tree-resize-handle");
		handle.setCursor(Cursor.H_RESIZE);
		handle.setMinWidth(RESIZE_HANDLE_WIDTH);
		handle.setPrefWidth(RESIZE_HANDLE_WIDTH);
		handle.setMaxWidth(RESIZE_HANDLE_WIDTH);
		AnchorPane.setTopAnchor(handle, 0.0);
		AnchorPane.setBottomAnchor(handle, 0.0);
		AnchorPane.setRightAnchor(handle, 0.0);
		handle.setOnMousePressed(event -> {
			resizeStartSceneX = event.getSceneX();
			resizeStartWidth = currentHolderWidth();
			event.consume();
		});
		handle.setOnMouseDragged(event -> {
			double width = resizeStartWidth + event.getSceneX() - resizeStartSceneX;
			holder.setPrefWidth(clamp(width, MIN_WIDTH, MAX_WIDTH));
			event.consume();
		});
		return handle;
	}

	private double currentHolderWidth() {
		double width = holder.getWidth();
		if (width > 0)
			return width;
		width = holder.getPrefWidth();
		return width > 0 ? width : DEFAULT_WIDTH;
	}

	private double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
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
		TreeCell<?> cell = findNonEmptyTreeCell(event.getPickResult().getIntersectedNode());
		if (cell != null) {
			contextMenuTarget = treeItemFromCell(cell);
			if (contextMenuTarget != null && contextMenuTarget.getValue() != null) {
				treeView.getSelectionModel().clearSelection();
				treeView.getSelectionModel().select(contextMenuTarget);
			}
			updateContextMenuState();
			contextMenu.show(treeView, event.getScreenX(), event.getScreenY());
			event.consume();
		} else {
			contextMenuTarget = null;
			contextMenu.hide();
		}
	}

	private TreeItem<CSG> treeItemFromCell(TreeCell<?> cell) {
		TreeItem<?> item = cell.getTreeItem();
		if (item == null)
			return null;
		if (item.getValue() != null && !(item.getValue() instanceof CSG))
			return null;
		@SuppressWarnings("unchecked")
		TreeItem<CSG> csgItem = (TreeItem<CSG>) item;
		return csgItem;
	}

	private void updateContextMenuState() {
		boolean anySelected = !session.getSelected().isEmpty();
		boolean anyTreeSelected = !treeView.getSelectionModel().getSelectedItems().isEmpty();
		CSG editTarget = contextMenuEditTargetCsg();
		long unlockedCount = session.getSelected().stream().filter(c -> !c.isLock()).count();
		boolean anyGroup = session.getSelected().stream().anyMatch(CSG::isGroupResult);

		menuItemEdit.setDisable(editTarget == null || findLastTransformIndex(editTarget.getName()) < 0);
		menuItemGroup.setDisable(unlockedCount < 2);
		menuItemMoreGroup.setDisable(unlockedCount < 2);
		menuItemUngroup.setDisable(!anyGroup);
		menuItemHideShow.setDisable(!anyTreeSelected);
		contextMenu.getItems().forEach(item -> {
			if (item instanceof SeparatorMenuItem || item == menuItemEdit || item == menuItemGroup
					|| item == menuItemMoreGroup || item == menuItemUngroup || item == menuItemHideShow)
				return;
			item.setDisable(!anySelected);
		});
	}

	private ContextMenu buildContextMenu() {
		menuItemEdit = new MenuItem("Edit");
		menuItemEdit.setOnAction(e -> editContextMenuTarget());

		menuItemGroup = new MenuItem("Group");
		menuItemGroup.setOnAction(e -> session.onGroup(false, false));

		MenuItem hullItem = new MenuItem("Hull");
		hullItem.setOnAction(e -> session.onGroup(true, false));

		MenuItem intersectItem = new MenuItem("Intersect");
		intersectItem.setOnAction(e -> session.onGroup(false, true));

		MenuItem xorItem = new MenuItem("Xor");
		xorItem.setOnAction(e -> session.onXor());

		menuItemMoreGroup = new Menu("More Group");
		menuItemMoreGroup.getItems().addAll(hullItem, intersectItem, xorItem);

		menuItemUngroup = new MenuItem("Ungroup");
		menuItemUngroup.setOnAction(e -> session.onUngroup());

		MenuItem makeHoleItem = new MenuItem("Make Hole");
		makeHoleItem.setOnAction(e -> session.setToHole());

		MenuItem makeSolidItem = new MenuItem("Make Solid");
		makeSolidItem.setOnAction(e -> session.setToSolid());

		MenuItem deleteItem = new MenuItem("Delete");
		deleteItem.setOnAction(e -> session.onDelete());

		menuItemHideShow = new MenuItem("Hide / Show");
		menuItemHideShow.setOnAction(e -> {
			// Hidden items are filtered by selectAll(); re-inject them so the operation sees them
			for (TreeItem<CSG> item : treeView.getSelectionModel().getSelectedItems()) {
				if (item != null && item.getValue() != null && item.getValue().isHide())
					session.getSelected().add(item.getValue());
			}
			session.onHideShowOperation();
		});

		MenuItem lockToggleItem = new MenuItem("Lock / Unlock");
		lockToggleItem.setOnAction(e -> session.lockToggle());

		ContextMenu menu = new ContextMenu(menuItemEdit, new SeparatorMenuItem(), menuItemGroup, menuItemMoreGroup,
				menuItemUngroup, new SeparatorMenuItem(), makeHoleItem, makeSolidItem, new SeparatorMenuItem(),
				deleteItem, menuItemHideShow, lockToggleItem);
		menu.setAutoHide(true);
		return menu;
	}

	private void editContextMenuTarget() {
		CSG target = contextMenuEditTargetCsg();
		if (target == null)
			return;
		String targetName = target.getName();
		CaDoodleFile file = ap.get();
		if (file == null || file.isRegenerating() || !file.isInitialized())
			return;
		int transformIndex = findLastTransformIndex(targetName);
		if (transformIndex < 0)
			return;

		session.selectAll(List.of(targetName));
		new Thread(() -> {
			file.moveToOpIndex(transformIndex);
			BowlerStudio.runLater(() -> session.selectAll(List.of(targetName)));
		}).start();
	}

	private CSG contextMenuTargetCsg() {
		if (contextMenuTarget == null)
			return null;
		CSG csg = contextMenuTarget.getValue();
		if (csg == null)
			return null;
		if (csg.isInGroup()) {
			TreeItem<CSG> parent = contextMenuTarget.getParent();
			if (parent == null || parent.getValue() == null)
				return null;
			return parent.getValue();
		}
		return csg;
	}

	private CSG contextMenuEditTargetCsg() {
		if (contextMenuTarget == null)
			return null;
		return contextMenuTarget.getValue();
	}

	private int findLastTransformIndex(String targetName) {
		CaDoodleFile file = ap.get();
		if (file == null)
			return -1;
		List<CaDoodleOperation> operations = file.getOperations();
		int start = Math.min(file.getCurrentIndex() - 1, operations.size() - 1);
		for (int i = start; i >= 0; i--) {
			CaDoodleOperation operation = operations.get(i);
			if (!isTransformOperation(operation))
				continue;
			for (String name : operation.getNamesAddedInThisOperation()) {
				if (targetName.contentEquals(name))
					return i;
			}
		}
		return -1;
	}

	private boolean isTransformOperation(CaDoodleOperation operation) {
		return operation instanceof MoveCenter || operation instanceof Resize || operation instanceof Align
				|| operation instanceof Mirror;
	}

	private LinkedHashSet<String> selectedTreeNames() {
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
				csg = parent.getValue();
			}
			names.add(csg.getName());
		}
		return names;
	}

	private void syncSessionFromTreeSelectionModel() {
		LinkedHashSet<String> names = selectedTreeNames();
		if (names.isEmpty())
			session.clearSelection();
		else
			session.selectAll(names);
	}

	private void syncTreeSelectionFromSession() {
		if (rebuilding)
			return;
		syncingTreeSelection = true;
		try {
			Set<String> selectedNames = session.getSelected().stream().map(CSG::getName).collect(Collectors.toSet());
			MultipleSelectionModel<TreeItem<CSG>> selectionModel = treeView.getSelectionModel();
			selectionModel.clearSelection();
			selectTreeItemsByName(root, selectedNames, selectionModel);
		} finally {
			syncingTreeSelection = false;
		}
	}

	private void selectTreeItemsByName(TreeItem<CSG> item, Set<String> selectedNames,
			MultipleSelectionModel<TreeItem<CSG>> selectionModel) {
		if (item == null)
			return;
		CSG csg = item.getValue();
		if (csg != null && selectedNames.contains(csg.getName()))
			selectionModel.select(item);
		for (TreeItem<CSG> child : item.getChildren())
			selectTreeItemsByName(child, selectedNames, selectionModel);
	}

	private void rebuildTree(List<CSG> state) {
		Map<String, List<String>> previousChildOrder = snapshotChildOrder();
		rebuilding = true;
		try {
			root.getChildren().clear();
			for (CSG csg : stableOrder(topLevelItems(state), previousChildOrder.get(ROOT_NODE_ID))) {
				root.getChildren().add(makeItem(csg, state, previousChildOrder));
			}
		} finally {
			rebuilding = false;
		}
		syncTreeSelectionFromSession();
		treeView.refresh();
	}

	private TreeItem<CSG> makeItem(CSG csg, List<CSG> all, Map<String, List<String>> previousChildOrder) {
		TreeItem<CSG> item = new TreeItem<>(csg);
		item.setExpanded(true);
		if (csg.isGroupResult()) {
			addGroupChildren(item, csg.getName(), all, previousChildOrder);
		}
		return item;
	}

	private void addGroupChildren(TreeItem<CSG> parent, String groupName, List<CSG> all,
			Map<String, List<String>> previousChildOrder) {
		for (CSG child : stableOrder(groupChildren(groupName, all), previousChildOrder.get(groupName))) {
			if (!child.checkGroupMembership(groupName))
				continue;
			TreeItem<CSG> item = new TreeItem<>(child);
			item.setExpanded(true);
			parent.getChildren().add(item);
			if (child.isGroupResult()) {
				addGroupChildren(item, child.getName(), all, previousChildOrder);
			}
		}
	}

	private List<CSG> topLevelItems(List<CSG> state) {
		List<CSG> topLevel = new ArrayList<>();
		for (CSG csg : state) {
			if (!csg.isInGroup())
				topLevel.add(csg);
		}
		return topLevel;
	}

	private List<CSG> groupChildren(String groupID, List<CSG> state) {
		List<CSG> children = new ArrayList<>();
		for (CSG csg : state) {
			if (csg.checkGroupMembership(groupID))
				children.add(csg);
		}
		return children;
	}

	private List<CSG> stableOrder(List<CSG> items, List<String> previousOrder) {
		if (items.isEmpty() || previousOrder == null || previousOrder.isEmpty())
			return items;

		Map<String, CSG> byName = new HashMap<>();
		for (CSG item : items) {
			byName.put(item.getName(), item);
		}

		List<CSG> ordered = new ArrayList<>(items.size());
		Set<String> addedNames = new LinkedHashSet<>();
		for (String name : previousOrder) {
			CSG existing = byName.get(name);
			if (existing != null) {
				ordered.add(existing);
				addedNames.add(name);
			}
		}
		for (CSG item : items) {
			if (addedNames.add(item.getName()))
				ordered.add(item);
		}
		return ordered;
	}

	private Map<String, List<String>> snapshotChildOrder() {
		Map<String, List<String>> childOrder = new HashMap<>();
		snapshotChildOrder(root, ROOT_NODE_ID, childOrder);
		return childOrder;
	}

	private void snapshotChildOrder(TreeItem<CSG> parent, String parentId, Map<String, List<String>> childOrder) {
		List<String> names = new ArrayList<>();
		for (TreeItem<CSG> child : parent.getChildren()) {
			CSG childValue = child.getValue();
			if (childValue == null)
				continue;
			names.add(childValue.getName());
			if (childValue.isGroupResult()) {
				snapshotChildOrder(child, childValue.getName(), childOrder);
			}
		}
		childOrder.put(parentId, names);
	}

	@Override
	public void onUpdate(List<CSG> currentState, CaDoodleOperation source, CaDoodleFile file) {
		BowlerStudio.runLater(() -> {
			rootLabel = file != null && file.getMyProjectName() != null && !file.getMyProjectName().isBlank()
					? file.getMyProjectName()
					: "Project";
			rebuildTree(currentState);
		});
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
			getStyleClass().removeAll(TREE_CELL_GROUP, TREE_CELL_HIDDEN, TREE_CELL_SELECTED, TREE_CELL_ROOT);
			if (empty) {
				setText(null);
				setGraphic(null);
				return;
			}
			if (csg == null) {
				setText(rootLabel);
				getStyleClass().add(TREE_CELL_ROOT);
				return;
			}
			if (csg.isGroupResult()) {
				setText("Group: " + csg.getName());
				getStyleClass().add(TREE_CELL_GROUP);
			} else {
				setText(csg.getName());
			}
			if (csg.isHide() || isAncestorHidden(getTreeItem())) {
				getStyleClass().add(TREE_CELL_HIDDEN);
			}
			if (isSessionSelected(csg)) {
				getStyleClass().add(TREE_CELL_SELECTED);
			}
		}

		private boolean isSessionSelected(CSG csg) {
			for (CSG selected : session.getSelected()) {
				if (selected.getName().contentEquals(csg.getName()))
					return true;
			}
			return false;
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
