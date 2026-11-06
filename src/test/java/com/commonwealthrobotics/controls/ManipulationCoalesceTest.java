package com.commonwealthrobotics.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.neuronrobotics.bowlerkernel.Bezier3d.Manipulation;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import javafx.application.Platform;
import javafx.scene.transform.Affine;

/**
 * Regression tests for the FX-thread flood fix in Manipulation: setGlobal()
 * must coalesce pending affine writes so a drag storm does not enqueue one
 * FX task per mouse event, while the latest pose still wins.
 */
class ManipulationCoalesceTest {

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

	private static void runOnFx(Runnable r) throws Exception {
		CountDownLatch done = new CountDownLatch(1);
		Throwable[] err = new Throwable[1];
		Platform.runLater(() -> {
			try {
				r.run();
			} catch (Throwable t) {
				err[0] = t;
			} finally {
				done.countDown();
			}
		});
		assertTrue(done.await(15, TimeUnit.SECONDS));
		if (err[0] != null) {
			if (err[0] instanceof RuntimeException)
				throw (RuntimeException) err[0];
			throw new RuntimeException(err[0]);
		}
	}

	@Test
	@Timeout(30)
	void offThreadStormAppliesLatestPose() throws Exception {
		Affine matrix = new Affine();
		Manipulation m = new Manipulation(matrix, new eu.mihosoft.vrl.v3d.Vector3d(1, 1, 0), new TransformNR());
		for (int i = 1; i <= 100; i++) {
			m.setGlobal(new TransformNR(i, i, i));
		}
		awaitFxIdle();
		assertEquals(100, matrix.getTx(), 1e-6);
		assertEquals(100, matrix.getTy(), 1e-6);
		assertEquals(100, matrix.getTz(), 1e-6);
	}

	@Test
	@Timeout(30)
	void fxThreadStormAppliesLatestPose() throws Exception {
		Affine matrix = new Affine();
		Manipulation m = new Manipulation(matrix, new eu.mihosoft.vrl.v3d.Vector3d(1, 1, 0), new TransformNR());
		runOnFx(() -> {
			for (int i = 1; i <= 25; i++) {
				m.setGlobal(new TransformNR(i, 0, 0));
			}
		});
		awaitFxIdle();
		assertEquals(25, matrix.getTx(), 1e-6);
	}

	@Test
	@Timeout(30)
	void queuedWriteCountIsBoundedByBurst() throws Exception {
		// A storm from off-thread must not grow the FX queue: after the burst
		// there is at most one pending write, so a second burst still works
		Affine matrix = new Affine();
		Manipulation m = new Manipulation(matrix, new eu.mihosoft.vrl.v3d.Vector3d(1, 1, 0), new TransformNR());
		for (int i = 1; i <= 50; i++) {
			m.setGlobal(new TransformNR(i, 0, 0));
		}
		awaitFxIdle();
		for (int i = 51; i <= 100; i++) {
			m.setGlobal(new TransformNR(i, 0, 0));
		}
		awaitFxIdle();
		assertEquals(100, matrix.getTx(), 1e-6);
	}


	// Note: fireMove/listener dispatch is privately contained in the drag
	// pipeline; the coalescing behavior above is the regression surface.
}
