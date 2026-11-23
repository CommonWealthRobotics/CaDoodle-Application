package com.commonwealthrobotics.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ControlSpriteStateTest {
	@Test
	void alignOverridesEveryPreviousDisplayMode() {
		for (SpriteDisplayMode previous : SpriteDisplayMode.values()) {
			ControlSpriteState state = new ControlSpriteState();
			state.setMode(previous);
			state.setMode(SpriteDisplayMode.Align);
			assertEquals(SpriteDisplayMode.Align, state.getMode());
		}
	}

	@Test
	void groupRefreshAndOtherDisplayRequestsCannotDisableAlign() {
		ControlSpriteState state = new ControlSpriteState();
		state.setMode(SpriteDisplayMode.Align);
		for (SpriteDisplayMode requested : SpriteDisplayMode.values()) {
			assertFalse(state.setMode(requested));
			assertEquals(SpriteDisplayMode.Align, state.getMode());
		}
	}

	@Test
	void explicitCancellationReleasesAlignAndAllowsReactivation() {
		ControlSpriteState state = new ControlSpriteState();
		state.setMode(SpriteDisplayMode.Align);
		assertTrue(state.disableAlign());
		assertEquals(SpriteDisplayMode.Default, state.getMode());
		assertFalse(state.disableAlign());
		assertTrue(state.setMode(SpriteDisplayMode.Resize));
		assertTrue(state.setMode(SpriteDisplayMode.Align));
		assertFalse(state.setMode(SpriteDisplayMode.Default));
	}

	@Test
	void ordinaryModesRemainSwitchable() {
		ControlSpriteState state = new ControlSpriteState();
		for (SpriteDisplayMode mode : SpriteDisplayMode.values()) {
			if (mode == SpriteDisplayMode.Align)
				continue;
			state.setMode(mode);
			assertEquals(mode, state.getMode());
			assertFalse(state.disableAlign());
			assertEquals(mode, state.getMode());
		}
	}
}
