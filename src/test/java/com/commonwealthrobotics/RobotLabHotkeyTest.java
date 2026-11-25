package com.commonwealthrobotics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.commonwealthrobotics.controls.SelectionSession;
import com.commonwealthrobotics.robot.RobotLab;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;

import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

class RobotLabHotkeyTest {
	@Test
	void editingLeavesPressedAndTypedEventsUntouchedAndDoesNotTrackKeys() throws Exception {
		Fixture f = new Fixture();
		when(f.lab.isFocused()).thenReturn(true);
		for (KeyCode code : new KeyCode[]{KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT, KeyCode.DELETE,
				KeyCode.BACK_SPACE, KeyCode.ESCAPE, KeyCode.TAB, KeyCode.DIGIT1}) {
			KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
			f.fire(event);
			assertFalse(event.isConsumed());
			assertTrue(f.held.isEmpty(), "Editing keys must not affect later movement");
		}
		for (String character : new String[]{"1", "-", "+", "l", "w", "r", "\u0001", "\u001a"}) {
			KeyEvent event = new KeyEvent(KeyEvent.KEY_TYPED, character, "", KeyCode.UNDEFINED, false,
					character.charAt(0) < 32, false, false);
			f.fire(event);
			assertFalse(event.isConsumed());
		}
		verify(f.session, never()).onDelete();
		verify(f.session, never()).onAlign();
		verify(f.lab, never()).onCancel();
	}

	@Test
	void typedAlignmentShortcutStaysAvailableToTextEditor() throws Exception {
		Fixture f = new Fixture();
		when(f.lab.isFocused()).thenReturn(true);
		KeyEvent event = new KeyEvent(KeyEvent.KEY_TYPED, "l", "", KeyCode.UNDEFINED, false, false, false, false);
		f.fire(event);
		assertFalse(event.isConsumed());
		verify(f.session, never()).onAlign();
	}

	@Test
	void releaseClearsKeysEvenWhenEditingAndShortcutsResumeAfterEditing() throws Exception {
		Fixture f = new Fixture();
		f.held.add(KeyCode.UP);
		when(f.lab.isFocused()).thenReturn(true);
		KeyEvent release = new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.UP, false, false, false, false);
		f.fire(release);
		assertTrue(f.held.isEmpty());
		assertFalse(release.isConsumed());
		when(f.lab.isFocused()).thenReturn(false);
		KeyEvent align = new KeyEvent(KeyEvent.KEY_TYPED, "l", "", KeyCode.UNDEFINED, false, false, false, false);
		f.fire(align);
		verify(f.session).onAlign();
		assertTrue(align.isConsumed());
	}

	@Test
	void sessionEditingAlsoDoesNotPolluteHeldKeys() throws Exception {
		Fixture f = new Fixture();
		when(f.session.isFocused()).thenReturn(true);
		KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.UP, false, false, false, false);
		f.fire(event);
		assertTrue(f.held.isEmpty());
		assertFalse(event.isConsumed());
	}

	private static class Fixture {
		final SelectionSession session = mock(SelectionSession.class);
		final RobotLab lab = mock(RobotLab.class);
		final Set<KeyCode> held = EnumSet.noneOf(KeyCode.class);
		final Map<EventType<KeyEvent>, EventHandler<KeyEvent>> filters = new HashMap<>();

		@SuppressWarnings("unchecked")
		Fixture() throws Exception {
			MainController controller = mock(MainController.class, CALLS_REAL_METHODS);
			BowlerStudio3dEngine engine = mock(BowlerStudio3dEngine.class);
			doAnswer(invocation -> {
				filters.put((EventType<KeyEvent>) invocation.getArguments()[0],
						(EventHandler<KeyEvent>) invocation.getArguments()[1]);
				return null;
			}).when(engine).addKeyFilter(any(), any());
			ActiveProject project = mock(ActiveProject.class);
			when(project.get()).thenReturn(mock(CaDoodleFile.class));
			session.workplane = mock(WorkplaneManager.class);
			set(controller, "ap", project);
			set(controller, "session", session);
			set(controller, "robotLab", lab);
			set(controller, "engine", engine);
			set(controller, "selectionBox", mock(SelectionBox.class));
			set(controller, "heldKeys", held);
			Method setup = MainController.class.getDeclaredMethod("setupEngineControls");
			setup.setAccessible(true);
			setup.invoke(controller);
		}

		void fire(KeyEvent event) {
			filters.get(event.getEventType()).handle(event);
		}

		private static void set(MainController controller, String name, Object value) throws Exception {
			Field field = MainController.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(controller, value);
		}
	}
}
