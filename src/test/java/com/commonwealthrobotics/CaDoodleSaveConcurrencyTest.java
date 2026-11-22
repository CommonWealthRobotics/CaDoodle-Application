package com.commonwealthrobotics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.SaveOverwriteException;

class CaDoodleSaveConcurrencyTest {
	@Test
	void unfreezeSaveWaitsForAutosaveAndCanProceedAfterItsFailure() throws Exception {
		CountDownLatch autosaveEntered = new CountDownLatch(1);
		CountDownLatch releaseAutosave = new CountDownLatch(1);
		RuntimeException writeFailure = new RuntimeException("test serialization failure");
		CaDoodleFile file = new CaDoodleFile() {
			@Override
			public boolean isInitialized() {
				return true;
			}

			@Override
			public String toJson() {
				autosaveEntered.countDown();
				try {
					if (!releaseAutosave.await(5, TimeUnit.SECONDS))
						throw new AssertionError("autosave was not released");
				} catch (InterruptedException e) {
					throw new AssertionError(e);
				}
				throw writeFailure;
			}
		};
		FutureTask<Throwable> autosave = saving(file);
		start(autosave);
		assertTrue(autosaveEntered.await(5, TimeUnit.SECONDS));
		assertThrows(SaveOverwriteException.class, () -> file.save(true));
		CountDownLatch unfreezeStarted = new CountDownLatch(1);
		CountDownLatch unfreezeFinished = new CountDownLatch(1);
		FutureTask<Throwable> unfreeze = new FutureTask<>(() -> {
			unfreezeStarted.countDown();
			try {
				return saveFailure(file);
			} finally {
				unfreezeFinished.countDown();
			}
		});
		start(unfreeze);
		try {
			assertTrue(unfreezeStarted.await(5, TimeUnit.SECONDS));
			assertFalse(unfreezeFinished.await(200, TimeUnit.MILLISECONDS),
					"unfreeze must wait for autosave rather than reject the save");
		} finally {
			releaseAutosave.countDown();
		}
		assertSame(writeFailure, autosave.get(5, TimeUnit.SECONDS));
		assertSame(writeFailure, unfreeze.get(5, TimeUnit.SECONDS));
		assertFalse(file.isSaveing());
	}

	private static FutureTask<Throwable> saving(CaDoodleFile file) {
		return new FutureTask<>(() -> {
			try {
				file.save(true);
				throw new AssertionError("serialization should fail");
			} catch (SaveOverwriteException e) {
				return e.getCause();
			}
		});
	}

	@Test
	void waitingSaveHonorsInterruption() {
		CaDoodleFile file = new CaDoodleFile();
		Thread.currentThread().interrupt();
		try {
			assertThrows(InterruptedException.class, file::saveWhenReady);
			assertFalse(file.isSaveing());
		} finally {
			Thread.interrupted();
		}
	}

	private static Throwable saveFailure(CaDoodleFile file) throws Exception {
		try {
			file.saveWhenReady();
			throw new AssertionError("serialization should fail");
		} catch (SaveOverwriteException e) {
			return e.getCause();
		}
	}

	private static void start(Runnable task) {
		Thread thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();
	}
}
