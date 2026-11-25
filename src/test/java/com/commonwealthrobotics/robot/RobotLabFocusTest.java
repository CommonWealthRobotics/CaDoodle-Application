package com.commonwealthrobotics.robot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

class RobotLabFocusTest {
	@BeforeAll
	static void startFx() {
		com.sun.javafx.application.PlatformImpl.startup(() -> {
		});
	}

	@Test
	void recognizesTextInputsAcrossTabsAndDynamicEditorDescendants() throws Exception {
		onFx(() -> {
			TabPane tabs = new TabPane();
			LimbControlManager manager = mock(LimbControlManager.class);
			RobotLab lab = lab(tabs, manager);
			FocusField angle = new FocusField();
			FocusArea description = new FocusArea();
			VBox editors = new VBox();
			tabs.getTabs().addAll(new Tab("Body", new VBox(angle)), new Tab("Advanced", editors));
			assertFalse(lab.isFocused());
			angle.focus(true);
			assertTrue(lab.isFocused());
			angle.focus(false);
			editors.getChildren().add(new VBox(new VBox(description)));
			description.focus(true);
			assertTrue(lab.isFocused(), "Unselected tab contents and nested TextAreas belong to the lab");
			description.focus(false);
			assertFalse(lab.isFocused());
			FocusButton button = new FocusButton();
			editors.getChildren().add(button);
			button.focus();
			assertFalse(lab.isFocused(), "Non-text controls must not suppress shortcuts");
		});
	}

	@Test
	void recognizesEditorsInsideControlSkinsAndNestedTabs() throws Exception {
		onFx(() -> {
			FocusField editor = new FocusField();
			Control control = new Control() {
			};
			control.setSkin(new SkinBase<Control>(control) {
				{
					getChildren().add(editor);
				}
			});
			TabPane nestedTabs = new TabPane(new Tab("Editor", control));
			TabPane tabs = new TabPane(new Tab("Empty"), new Tab("Nested", nestedTabs));
			RobotLab lab = lab(tabs, mock(LimbControlManager.class));
			assertFalse(lab.isFocused());
			editor.focus(true);
			assertTrue(lab.isFocused());
			editor.focus(false);
			assertFalse(lab.isFocused());
		});
	}

	@Test
	void includesThreeDimensionalLimbEditors() throws Exception {
		onFx(() -> {
			LimbControlManager manager = mock(LimbControlManager.class);
			RobotLab lab = lab(new TabPane(), manager);
			when(manager.isFocused()).thenReturn(true);
			assertTrue(lab.isFocused());
			when(manager.isFocused()).thenReturn(false);
			assertFalse(lab.isFocused());
		});
	}

	private static RobotLab lab(TabPane tabs, LimbControlManager manager) {
		RobotLab lab = mock(RobotLab.class, CALLS_REAL_METHODS);
		lab.setManager(manager);
		try {
			Field field = RobotLab.class.getDeclaredField("robotLabTabPane");
			field.setAccessible(true);
			field.set(lab, tabs);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
		return lab;
	}

	private static void onFx(Runnable action) throws Exception {
		FutureTask<Void> task = new FutureTask<>(action, null);
		Platform.runLater(task);
		task.get(30, TimeUnit.SECONDS);
	}

	private static class FocusField extends TextField {
		void focus(boolean focused) {
			setFocused(focused);
		}
	}

	private static class FocusArea extends TextArea {
		void focus(boolean focused) {
			setFocused(focused);
		}
	}

	private static class FocusButton extends Button {
		void focus() {
			setFocused(true);
		}
	}
}
