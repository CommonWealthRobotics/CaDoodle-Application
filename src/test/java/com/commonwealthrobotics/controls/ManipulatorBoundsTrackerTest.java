package com.commonwealthrobotics.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import javafx.scene.transform.Affine;

class ManipulatorBoundsTrackerTest {
	@Test
	void boundsFollowUndoRedoAndRepeatedPoseChanges() throws Exception {
		CaDoodleFile file = new CaDoodleFile();
		CSG part = new Cube(10).toCSG().setName("limb-part");
		Affine pose = new Affine();
		part.setManipulator(pose);
		List<CSG> selected = List.of(part);
		pose.setTx(30);
		assertEquals(30, file.getBounds(selected).getCenterX(), 1e-9);
		ManipulatorBoundsTracker tracker = new ManipulatorBoundsTracker();
		AtomicInteger refreshes = new AtomicInteger();
		tracker.track(pose, () -> {
			file.getBoundsCache().remove(part.getName());
			refreshes.incrementAndGet();
		});
		assertEquals(30, file.getBounds(selected).getCenterX(), 1e-9);
		for (double x : new double[]{0, 30, -15, 30, 0}) {
			int before = refreshes.get();
			pose.setTx(x);
			assertEquals(x, file.getBounds(selected).getCenterX(), 1e-9);
			assertEquals(before + 1, refreshes.get());
		}
		tracker.clear();
	}

	@Test
	void boundsFollowRotationAsWellAsTranslation() throws Exception {
		CaDoodleFile file = new CaDoodleFile();
		CSG part = new Cube(10, 20, 30).toCSG().setName("rotating-link");
		Affine pose = new Affine();
		part.setManipulator(pose);
		ManipulatorBoundsTracker tracker = new ManipulatorBoundsTracker();
		tracker.track(pose, () -> file.getBoundsCache().remove(part.getName()));
		assertEquals(10, file.getBounds(List.of(part)).getTotalX(), 1e-9);
		pose.appendRotation(90, 0, 0);
		assertEquals(20, file.getBounds(List.of(part)).getTotalX(), 1e-9);
		pose.setToIdentity();
		assertEquals(10, file.getBounds(List.of(part)).getTotalX(), 1e-9);
		tracker.clear();
	}

	@Test
	void attachmentInvalidatesPoseCachedBeforeDisplayAndClearDetachesListeners() {
		ManipulatorBoundsTracker tracker = new ManipulatorBoundsTracker();
		Affine pose = new Affine();
		AtomicInteger refreshes = new AtomicInteger();
		tracker.track(pose, refreshes::incrementAndGet);
		tracker.track(pose, refreshes::incrementAndGet);
		assertEquals(2, refreshes.get());
		pose.setTy(10);
		assertEquals(4, refreshes.get());
		tracker.clear();
		tracker.clear();
		pose.setTy(20);
		assertEquals(4, refreshes.get());
		tracker.track(pose, refreshes::incrementAndGet);
		pose.setTz(40);
		assertEquals(6, refreshes.get());
		tracker.clear();
	}
}
