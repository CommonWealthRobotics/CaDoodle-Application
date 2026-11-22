package com.commonwealthrobotics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import com.neuronrobotics.bowlerstudio.SplashManager;

class DialogSplashStateTest {
	@Test
	void restoresVisibleSplashAndPoliciesAfterEveryDialogOutcome() {
		for (boolean initiallyVisible : new boolean[]{true, false}) {
			for (boolean failure : new boolean[]{true, false}) {
				BooleanSupplier originalOpen = SplashManager.getOpenPreventer();
				BooleanSupplier originalClose = SplashManager.getClosePreventer();
				AtomicBoolean visible = new AtomicBoolean(initiallyVisible);
				try {
					try (DialogSplashState state = new DialogSplashState(initiallyVisible,
							() -> visible.set(false), () -> visible.set(true))) {
						assertFalse(visible.get());
						assertTrue(SplashManager.getOpenPreventer().getAsBoolean());
						assertTrue(SplashManager.getClosePreventer().getAsBoolean());
						if (failure)
							throw new IllegalStateException("dialog failure");
					} catch (IllegalStateException expected) {
						assertTrue(failure);
					}
					assertEquals(initiallyVisible, visible.get());
					assertSame(originalOpen, SplashManager.getOpenPreventer());
					assertSame(originalClose, SplashManager.getClosePreventer());
				} finally {
					SplashManager.setOpenPreventer(originalOpen);
					SplashManager.setClosePreventer(originalClose);
				}
			}
		}
	}
}
