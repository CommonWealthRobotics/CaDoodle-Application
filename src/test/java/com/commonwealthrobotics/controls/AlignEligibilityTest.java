package com.commonwealthrobotics.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;

class AlignEligibilityTest {
	@Test
	void groupingAlignedObjectsIntoOneSelectionDisablesAlign() {
		ControlSpriteState state = new ControlSpriteState();
		List<CSG> selected = List.of(new Cube(1).toCSG(), new Cube(1).toCSG());
		assertTrue(SelectionSession.canAlign(selected));
		state.setMode(SpriteDisplayMode.Align);
		assertFalse(state.selectionChanged(SelectionSession.canAlign(selected)));
		assertEquals(SpriteDisplayMode.Align, state.getMode());

		List<CSG> groupedSelection = List.of(new Cube(2).toCSG());
		assertTrue(state.selectionChanged(SelectionSession.canAlign(groupedSelection)));
		assertEquals(SpriteDisplayMode.Default, state.getMode());
		assertFalse(state.selectionChanged(SelectionSession.canAlign(groupedSelection)));
		assertFalse(state.selectionChanged(SelectionSession.canAlign(selected)));
		assertEquals(SpriteDisplayMode.Default, state.getMode());
		assertTrue(state.setMode(SpriteDisplayMode.Align));
		assertEquals(SpriteDisplayMode.Align, state.getMode());
	}

	@Test
	void emptyOrLockedSelectionDisablesAlignWithoutAffectingOtherModes() {
		for (List<CSG> selected : List.of(List.<CSG>of(),
				List.of(new Cube(1).toCSG(), new Cube(1).toCSG().setIsLock(true)))) {
			ControlSpriteState state = new ControlSpriteState();
			state.setMode(SpriteDisplayMode.Align);
			assertTrue(state.selectionChanged(SelectionSession.canAlign(selected)));
			assertEquals(SpriteDisplayMode.Default, state.getMode());
			state.setMode(SpriteDisplayMode.Mirror);
			assertFalse(state.selectionChanged(SelectionSession.canAlign(selected)));
			assertEquals(SpriteDisplayMode.Mirror, state.getMode());
		}
	}

	@Test
	void activationRequiresTwoUnlockedSelectedObjectsRegardlessOfOrdering() {
		for (int unlocked = 0; unlocked <= 3; unlocked++) {
			for (int locked = 0; locked <= 3; locked++) {
				List<CSG> selected = new ArrayList<>();
				for (int i = 0; i < unlocked; i++)
					selected.add(new Cube(1).toCSG().setIsLock(false));
				for (int i = 0; i < locked; i++)
					selected.add(new Cube(1).toCSG().setIsLock(true));
				assertEquals(unlocked > 1, SelectionSession.canAlign(selected));
				Collections.reverse(selected);
				assertEquals(unlocked > 1, SelectionSession.canAlign(selected));
			}
		}
	}
}
