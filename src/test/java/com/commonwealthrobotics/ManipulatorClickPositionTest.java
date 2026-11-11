package com.commonwealthrobotics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import javafx.application.Platform;
import javafx.scene.transform.Affine;

/**
 * Regression tests for the manipulator handling in the click position capture:
 * the pose returned by WorkplaneManager.pickInteractionToPose is world space
 * with the source CSG's manipulator applied exactly once, so the focus center
 * (screenPositionOfLatestMeshClick) must not compose the manipulator again.
 *
 * The reported bug: the manipulator was composed both in pickInteractionToPose
 * and again in SelectionSession.setUpControls, so with a pure-translation
 * manipulator M the stored point was M*(M*p) = 2t + p instead of M*p = t + p.
 */
class ManipulatorClickPositionTest {

	@BeforeAll
	static void startFX() throws Exception {
		CountDownLatch fxStarted = new CountDownLatch(1);
		Thread t = new Thread(() -> {
			com.sun.javafx.application.PlatformImpl.startup(() -> {
			});
			fxStarted.countDown();
		}, "fx-test-init");
		t.setDaemon(true);
		t.start();
		assertTrue(fxStarted.await(15, TimeUnit.SECONDS));
		awaitFxIdle();
	}

	private static void awaitFxIdle() throws Exception {
		CountDownLatch done = new CountDownLatch(1);
		Platform.runLater(done::countDown);
		assertTrue(done.await(15, TimeUnit.SECONDS));
	}

	// setManipulator stores the affine in a static registry keyed by the CSG;
	// it must be called on the FX thread
	private static void setManip(CSG csg, Affine manip) throws Exception {
		CountDownLatch done = new CountDownLatch(1);
		Platform.runLater(() -> {
			csg.setManipulator(manip);
			done.countDown();
		});
		assertTrue(done.await(15, TimeUnit.SECONDS));
		awaitFxIdle();
	}

	// A mesh-local point and a translation-only manipulator must compose to
	// local + offset, applied exactly once
	@Test
	@Timeout(60)
	void translationManipulatorAppliedExactlyOnce() throws Exception {
		CSG csg = new Cube(10).toCSG();
		double tx = 12.5, ty = -3.25, tz = 40;
		setManip(csg, TransformFactory.nrToAffine(new TransformNR(tx, ty, tz, new RotationNR())));

		TransformNR local = new TransformNR(1, 2, 3);
		TransformNR world = WorkplaneManager.applySourceManipulator(csg, local);

		assertEquals(tx + 1, world.getX(), 1e-9);
		assertEquals(ty + 2, world.getY(), 1e-9);
		assertEquals(tz + 3, world.getZ(), 1e-9);
	}

	// A rotated-then-translated manipulator must map the local point through
	// the full affine, not just its translation
	@Test
	@Timeout(60)
	void rotatedManipulatorAppliedExactlyOnce() throws Exception {
		CSG csg = new Cube(10).toCSG();
		setManip(csg, TransformFactory
				.nrToAffine(new TransformNR(100, 0, 0, new RotationNR(0, 0, 90))));

		// rotation by 90 deg about Z: (x, y) -> (-y, x) in the rotated frame
		TransformNR local = new TransformNR(2, 5, 7);
		TransformNR world = WorkplaneManager.applySourceManipulator(csg, local);

		assertEquals(100 - 5, world.getX(), 1e-6);
		assertEquals(2, world.getY(), 1e-6);
		assertEquals(7, world.getZ(), 1e-6);
	}

	// A CSG without a manipulator passes the pose through unchanged; this is
	// the path every plain object takes
	@Test
	@Timeout(60)
	void noManipulatorLeavesPoseUnchanged() {
		CSG csg = new Cube(10).toCSG();
		TransformNR local = new TransformNR(4, -6, 8);

		TransformNR world = WorkplaneManager.applySourceManipulator(csg, local);

		assertEquals(4, world.getX(), 1e-9);
		assertEquals(-6, world.getY(), 1e-9);
		assertEquals(8, world.getZ(), 1e-9);
	}

	// A null source (pick landed on a non-session mesh) must not throw and
	// must keep the pose unchanged
	@Test
	@Timeout(60)
	void nullSourceLeavesPoseUnchanged() {
		TransformNR local = new TransformNR(1.5, 2.5, 3.5);

		TransformNR world = WorkplaneManager.applySourceManipulator(null, local);

		assertEquals(1.5, world.getX(), 1e-9);
		assertEquals(2.5, world.getY(), 1e-9);
		assertEquals(3.5, world.getZ(), 1e-9);
	}

	// Two successive captures on the same CSG must give the same world point:
	// each pick composes the manipulator once from the fresh local pick point,
	// it must never accumulate between captures
	@Test
	@Timeout(60)
	void repeatedCapturesDoNotAccumulateManipulator() throws Exception {
		CSG csg = new Cube(10).toCSG();
		setManip(csg, TransformFactory.nrToAffine(new TransformNR(20, 0, 0, new RotationNR())));

		TransformNR first = WorkplaneManager.applySourceManipulator(csg, new TransformNR(1, 1, 1));
		TransformNR second = WorkplaneManager.applySourceManipulator(csg, new TransformNR(1, 1, 1));

		assertEquals(21, first.getX(), 1e-9);
		assertEquals(21, second.getX(), 1e-9);
		assertEquals(first.getY(), second.getY(), 1e-9);
		assertEquals(first.getZ(), second.getZ(), 1e-9);
	}

	// The double-application that caused the bug: composing the manipulator a
	// second time on top of the world-space pose displaces the point by the
	// manipulator's translation again. Pinned so a reintroduction fails loudly.
	@Test
	@Timeout(60)
	void doubleApplicationDisplacesByExtraTranslation() throws Exception {
		CSG csg = new Cube(10).toCSG();
		double tx = 30, ty = 0, tz = 0;
		setManip(csg, TransformFactory.nrToAffine(new TransformNR(tx, ty, tz, new RotationNR())));

		TransformNR local = new TransformNR(0, 0, 0);
		TransformNR world = WorkplaneManager.applySourceManipulator(csg, local);
		TransformNR doubled = TransformFactory.affineToNr(csg.getManipulator()).times(world);

		assertEquals(tx, world.getX(), 1e-9);
		// the wrong value the focus used to receive
		assertEquals(2 * tx, doubled.getX(), 1e-9);
	}
}
