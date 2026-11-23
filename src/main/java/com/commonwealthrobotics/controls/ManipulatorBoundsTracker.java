package com.commonwealthrobotics.controls;

import java.util.ArrayList;
import java.util.List;

import javafx.event.EventHandler;
import javafx.scene.transform.Affine;
import javafx.scene.transform.TransformChangedEvent;

/** Tracks the render transforms whose changes invalidate displayed bounds. */
final class ManipulatorBoundsTracker {
	private final List<Runnable> detach = new ArrayList<>();

	void track(Affine manipulator, Runnable invalidateAndRefresh) {
		EventHandler<TransformChangedEvent> listener = event -> invalidateAndRefresh.run();
		manipulator.addEventHandler(TransformChangedEvent.TRANSFORM_CHANGED, listener);
		detach.add(() -> manipulator.removeEventHandler(TransformChangedEvent.TRANSFORM_CHANGED, listener));
		// The pose may have changed between bounds computation and listener attachment
		invalidateAndRefresh.run();
	}

	void clear() {
		for (Runnable remove : detach)
			remove.run();
		detach.clear();
	}
}
