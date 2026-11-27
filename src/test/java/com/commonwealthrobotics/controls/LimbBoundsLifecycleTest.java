package com.commonwealthrobotics.controls;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LimbBoundsLifecycleTest {
	@Test
	void failedRefreshReleasesStateAndRejectsQueuedDisplayWork() {
		LimbBoundsLifecycle lifecycle = new LimbBoundsLifecycle();
		lifecycle.begin();
		long refresh = lifecycle.finish().getAsLong();
		assertTrue(lifecycle.abandon(refresh));
		assertFalse(lifecycle.active());
		assertFalse(lifecycle.accepts(refresh, false));
		assertFalse(lifecycle.accepts(refresh, true));
	}

	@Test
	void repeatedBeginIsIdempotentAndSaveRemainsHiddenUntilFinalApply() {
		LimbBoundsLifecycle lifecycle = new LimbBoundsLifecycle();
		long oldPreview = lifecycle.preview().getAsLong();
		lifecycle.begin();
		long movingRevision = lifecycle.revision();
		for (int i = 0; i < 100; i++) {
			lifecycle.begin();
			assertEquals(movingRevision, lifecycle.revision());
			assertFalse(lifecycle.preview().isPresent());
		}
		assertFalse(lifecycle.accepts(oldPreview, false));
		long finalRefresh = lifecycle.finish().getAsLong();
		assertTrue(lifecycle.active());
		assertFalse(lifecycle.finish().isPresent());
		assertFalse(lifecycle.accepts(oldPreview, false));
		assertTrue(lifecycle.accepts(finalRefresh, true));
		assertTrue(lifecycle.complete(finalRefresh));
		assertFalse(lifecycle.active());
		assertFalse(lifecycle.accepts(oldPreview, false));
		assertFalse(lifecycle.accepts(-1, false));
		assertTrue(lifecycle.accepts(lifecycle.preview().getAsLong(), false));
	}

	@Test
	void anotherDragRejectsQueuedFinalApply() {
		LimbBoundsLifecycle lifecycle = new LimbBoundsLifecycle();
		lifecycle.begin();
		long oldFinal = lifecycle.finish().getAsLong();
		lifecycle.begin();
		assertFalse(lifecycle.complete(oldFinal));
		assertFalse(lifecycle.accepts(oldFinal, true));
		assertTrue(lifecycle.active());
		assertTrue(lifecycle.complete(lifecycle.finish().getAsLong()));
	}

	@Test
	void noMoveNeedsNoFinalRefreshAndAbandoningRefreshReleasesState() {
		LimbBoundsLifecycle lifecycle = new LimbBoundsLifecycle();
		assertFalse(lifecycle.finish().isPresent());
		lifecycle.begin();
		long refresh = lifecycle.finish().getAsLong();
		assertTrue(lifecycle.complete(refresh));
		assertFalse(lifecycle.complete(refresh));
		assertFalse(lifecycle.active());
	}
}
