package com.commonwealthrobotics.controls;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.SplashManager;

import javafx.application.Platform;

/** Completion boundaries for owned worker and scene updates. */
final class OperationCompletion {
	private OperationCompletion() {
	}

	static void convert(Supplier<Thread> start, Runnable select, Runnable updateControls) {
		convert(start, select, Platform::isFxApplicationThread, BowlerStudio::runLater, updateControls);
	}

	static void convert(Supplier<Thread> start, Runnable select, BooleanSupplier onUiThread,
			Consumer<Runnable> dispatch, Runnable updateControls) {
		try (SplashManager.CloseHold ignored = SplashManager.preventClosing()) {
			await(start.get());
			select.run();
			onUiAndWait(onUiThread, dispatch, updateControls);
		}
	}

	static void await(Thread worker) {
		if (worker == Thread.currentThread())
			throw new IllegalArgumentException("An operation cannot wait for itself");
		boolean interrupted = false;
		try {
			for (;;) {
				try {
					worker.join();
					return;
				} catch (InterruptedException interruption) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt();
		}
	}

	static void onUiAndWait(Runnable action) {
		onUiAndWait(Platform::isFxApplicationThread, BowlerStudio::runLater, action);
	}

	static void onUiAndWait(BooleanSupplier onUiThread, Consumer<Runnable> dispatch, Runnable action) {
		if (onUiThread.getAsBoolean()) {
			action.run();
			return;
		}
		FutureTask<Void> update = new FutureTask<Void>(action, null);
		dispatch.accept(update);
		boolean interrupted = false;
		try {
			for (;;) {
				try {
					update.get();
					return;
				} catch (InterruptedException interruption) {
					interrupted = true;
				} catch (ExecutionException failure) {
					Throwable cause = failure.getCause();
					if (cause instanceof RuntimeException)
						throw (RuntimeException) cause;
					if (cause instanceof Error)
						throw (Error) cause;
					throw new IllegalStateException("Scene update failed", cause);
				}
			}
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt();
		}
	}
}
