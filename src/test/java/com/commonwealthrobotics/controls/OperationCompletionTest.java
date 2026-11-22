package com.commonwealthrobotics.controls;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.neuronrobotics.bowlerstudio.SplashManager;

class OperationCompletionTest {
	private BooleanSupplier originalPreventer;

	@BeforeEach
	void savePolicy() {
		originalPreventer = SplashManager.getClosePreventer();
		SplashManager.setClosePreventer(() -> false);
	}

	@AfterEach
	void restorePolicy() {
		SplashManager.setClosePreventer(originalPreventer);
	}

	@Test
	void autosaveCannotCloseDuringImportOrWhileSceneCompletionIsQueued() throws Exception {
		BooleanSupplier previous = SplashManager.getClosePreventer();
		AtomicBoolean visible = new AtomicBoolean(true);
		AtomicBoolean selected = new AtomicBoolean();
		AtomicBoolean controlsUpdated = new AtomicBoolean();
		AtomicBoolean heldDuringUiUpdate = new AtomicBoolean();
		CountDownLatch importing = new CountDownLatch(1);
		CountDownLatch releaseImport = new CountDownLatch(1);
		CountDownLatch uiQueued = new CountDownLatch(1);
		CountDownLatch releaseUi = new CountDownLatch(1);
		ExecutorService ui = Executors.newSingleThreadExecutor(r -> {
			Thread thread = new Thread(r, "test scene thread");
			thread.setDaemon(true);
			return thread;
		});
		Runnable closeRequest = () -> {
			if (!SplashManager.getClosePreventer().getAsBoolean())
				visible.set(false);
		};
		FutureTask<Void> operation = new FutureTask<Void>(() -> {
			// Nested progress/download callbacks run on the operation worker too;
			closeRequest.run();
			importing.countDown();
			await(releaseImport);
		}, null);
		FutureTask<Void> conversion = new FutureTask<Void>(() -> OperationCompletion.convert(() -> start(operation),
				() -> selected.set(true), () -> false, task -> ui.submit(() -> {
					uiQueued.countDown();
					await(releaseUi);
					task.run();
				}), () -> {
					assertTrue(selected.get());
					heldDuringUiUpdate.set(SplashManager.getClosePreventer().getAsBoolean());
					controlsUpdated.set(true);
				}), null);
		start(conversion);
		try {
			assertTrue(importing.await(5, TimeUnit.SECONDS));
			closeRequest.run();
			assertTrue(visible.get(), "autosave completion must not dismiss a running conversion");
			assertFalse(selected.get());
			releaseImport.countDown();
			assertTrue(uiQueued.await(5, TimeUnit.SECONDS));
			assertTrue(selected.get());
			assertThrows(TimeoutException.class, () -> conversion.get(700, TimeUnit.MILLISECONDS));
			closeRequest.run();
			assertTrue(visible.get(), "worker completion must not dismiss queued scene updates");
			assertFalse(controlsUpdated.get());
			releaseUi.countDown();
			conversion.get(5, TimeUnit.SECONDS);
			operation.get(5, TimeUnit.SECONDS);
			assertTrue(controlsUpdated.get());
			assertTrue(heldDuringUiUpdate.get());
			assertSame(previous, SplashManager.getClosePreventer());
			closeRequest.run();
			assertFalse(visible.get());
		} finally {
			releaseImport.countDown();
			releaseUi.countDown();
			try {
				conversion.get(5, TimeUnit.SECONDS);
			} finally {
				ui.shutdown();
				assertTrue(ui.awaitTermination(5, TimeUnit.SECONDS));
			}
		}
	}

	@Test
	void startAndSceneFailuresRestoreTheOriginalPolicy() {
		BooleanSupplier previous = SplashManager.getClosePreventer();
		RuntimeException startFailure = new RuntimeException("operation rejected");
		assertSame(startFailure, assertThrows(RuntimeException.class, () -> OperationCompletion.convert(() -> {
			throw startFailure;
		}, () -> fail("selection must not run"), () -> true, Runnable::run, () -> fail("controls must not run"))));
		assertSame(previous, SplashManager.getClosePreventer());
		RuntimeException uiFailure = new RuntimeException("scene update failed");
		assertSame(uiFailure, assertThrows(RuntimeException.class,
				() -> OperationCompletion.convert(() -> start(new FutureTask<Void>(() -> {
				}, null)), () -> {
				}, () -> false, Runnable::run, () -> {
					throw uiFailure;
				})));
		assertSame(previous, SplashManager.getClosePreventer());
	}

	@Test
	void uiThreadUpdatesRunDirectlyAndPropagateFailures() {
		AtomicBoolean ran = new AtomicBoolean();
		OperationCompletion.onUiAndWait(() -> true, task -> fail("must not queue from the UI thread"),
				() -> ran.set(true));
		assertTrue(ran.get());
		AssertionError failure = new AssertionError("display failure");
		assertSame(failure, assertThrows(AssertionError.class,
				() -> OperationCompletion.onUiAndWait(() -> false, Runnable::run, () -> {
					throw failure;
				})));
	}

	@Test
	void interruptedWaitKeepsOwnershipUntilTheWorkerFinishes() throws Exception {
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch waiting = new CountDownLatch(1);
		FutureTask<Void> operation = new FutureTask<Void>(() -> await(release), null);
		Thread operationThread = start(operation);
		FutureTask<Boolean> completion = new FutureTask<Boolean>(() -> {
			waiting.countDown();
			OperationCompletion.await(operationThread);
			return Thread.currentThread().isInterrupted();
		});
		Thread waiter = start(completion);
		try {
			assertTrue(waiting.await(5, TimeUnit.SECONDS));
			waiter.interrupt();
			assertThrows(TimeoutException.class, () -> completion.get(100, TimeUnit.MILLISECONDS));
			release.countDown();
			assertTrue(completion.get(5, TimeUnit.SECONDS));
			operation.get(5, TimeUnit.SECONDS);
		} finally {
			release.countDown();
			completion.get(5, TimeUnit.SECONDS);
		}
	}

	@Test
	void overlappingScopesCanFinishOutOfOrderWithoutClosingAnotherOperation() {
		BooleanSupplier previous = SplashManager.getClosePreventer();
		SplashManager.CloseHold first = SplashManager.preventClosing();
		SplashManager.CloseHold second = SplashManager.preventClosing();
		first.close();
		first.close();
		assertTrue(SplashManager.getClosePreventer().getAsBoolean());
		second.close();
		assertSame(previous, SplashManager.getClosePreventer());
	}

	@Test
	void approvalDialogOverrideCanHideAndThenRestoreTheOperationGuard() {
		BooleanSupplier previous = SplashManager.getClosePreventer();
		SplashManager.CloseHold operation = SplashManager.preventClosing();
		BooleanSupplier saved = SplashManager.getClosePreventer();
		BooleanSupplier dialog = () -> false;
		SplashManager.setClosePreventer(dialog);
		assertFalse(SplashManager.getClosePreventer().getAsBoolean());
		SplashManager.setClosePreventer(saved);
		assertTrue(SplashManager.getClosePreventer().getAsBoolean());
		SplashManager.setClosePreventer(dialog);
		operation.close();
		assertSame(dialog, SplashManager.getClosePreventer());
		SplashManager.setClosePreventer(saved);
		assertSame(previous, SplashManager.getClosePreventer());
	}

	@Test
	void previousInitializationPolicyIsRestoredRatherThanReplacedWithFalse() {
		AtomicBoolean loading = new AtomicBoolean(true);
		BooleanSupplier previous = loading::get;
		SplashManager.setClosePreventer(previous);
		try (SplashManager.CloseHold ignored = SplashManager.preventClosing()) {
			assertTrue(SplashManager.getClosePreventer().getAsBoolean());
		}
		assertSame(previous, SplashManager.getClosePreventer());
		assertTrue(SplashManager.getClosePreventer().getAsBoolean());
		loading.set(false);
		assertFalse(SplashManager.getClosePreventer().getAsBoolean());
	}

	private static Thread start(FutureTask<?> task) {
		Thread thread = new Thread(task, "test operation thread");
		thread.setDaemon(true);
		thread.start();
		return thread;
	}

	private static void await(CountDownLatch latch) {
		try {
			assertTrue(latch.await(5, TimeUnit.SECONDS));
		} catch (InterruptedException failure) {
			throw new AssertionError(failure);
		}
	}
}
