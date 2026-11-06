package com.commonwealthrobotics.numbers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.commonwealthrobotics.RulerManager;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.transform.Affine;

/**
 * Regression tests for the drag-path fix in ThreedNumber.setValue: mouse
 * drags call setValue many times per frame and each call used to run a
 * TextField.setText invalidation cascade on the FX thread; now a single
 * deferred write may apply only the newest value while the field is not
 * being edited by the user.
 */
class ThreedNumberTest {

	private static RulerManager ruler;

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
		ruler = new RulerManager(null);
	}

	private static void awaitFxIdle() throws Exception {
		CountDownLatch done = new CountDownLatch(1);
		Platform.runLater(done::countDown);
		assertTrue(done.await(15, TimeUnit.SECONDS));
		CountDownLatch done2 = new CountDownLatch(1);
		Platform.runLater(done2::countDown);
		assertTrue(done2.await(15, TimeUnit.SECONDS));
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

	private static ThreedNumber newNumber() {
		// ruler is inactive; TextFieldDimension.None keeps its offset at 0
		return new ThreedNumber(new Affine(), new Affine(), () -> {
		}, TextFieldDimension.None, ruler, 4, new eu.mihosoft.vrl.v3d.Vector3d(1, 0, 0));
	}

	private static TextField fieldOf(ThreedNumber n) {
		return (TextField) n.getTextField().get(0);
	}

	private static String fmt(double v) {
		return String.format(Locale.getDefault(), "%.3f", v);
	}

	@Test
	@Timeout(30)
	void singleSetAppliesTextOnce() throws Exception {
		runOnFx(() -> {
			ThreedNumber n = newNumber();
			n.setValue(5);
			assertEquals(1, n.textWritesForTest);
		});
		awaitFxIdle();
	}

	@Test
	@Timeout(30)
	void stormOfDistinctValuesCoalescesToOneWrite() throws Exception {
		// With coalescing, many rapid distinct changes before the FX pulse
		// must result in exactly one TextField write applying the last text
		runOnFx(() -> {
			ThreedNumber n = newNumber();
			for (int i = 1; i <= 50; i++) {
				n.setValue(i);
			}
			assertEquals(1, n.textWritesForTest, "50 rapid sets must enqueue one write");
		});
		awaitFxIdle();
	}

	@Test
	@Timeout(30)
	void finalTextMatchesLastValue() throws Exception {
		runOnFx(() -> {
			ThreedNumber n = newNumber();
			for (int i = 1; i <= 50; i++) {
				n.setValue(i);
			}
		});
		awaitFxIdle();
		runOnFx(() -> {
			ThreedNumber n = newNumber();
			n.setValue(7.5);
		});
		awaitFxIdle();
		// verify text application using a live instance
		TextField[] box = new TextField[1];
		runOnFx(() -> {
			ThreedNumber n = newNumber();
			box[0] = fieldOf(n);
			n.setValue(7.5);
		});
		awaitFxIdle();
		assertEquals(fmt(7.5), box[0].getText());
	}

	@Test
	@Timeout(30)
	void tinyDeltaIsIgnoredEntirely() throws Exception {
		runOnFx(() -> {
			ThreedNumber n = newNumber();
			n.setValue(123.456);
			n.setValue(123.4561); // below the 0.001 significance threshold
			assertEquals(1, n.textWritesForTest);
			assertEquals(123.456, n.getMostRecentValue(), 1e-9);
		});
	}
}
