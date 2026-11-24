package com.commonwealthrobotics.controls;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.neuronrobotics.bowlerstudio.scripting.cadoodle.BreakUp;

class BreakUpSelectionTest {
	@Test
	void emptySelectionDoesNotScheduleOperation() {
		SelectionSession session = mock(SelectionSession.class);
		when(session.selectedSnapshot()).thenReturn(Collections.emptyList());
		doCallRealMethod().when(session).runBreakUp();
		session.runBreakUp();
		verify(session, never()).submit(any(Runnable.class));
		verify(session, never()).addOp(any());
	}

	@Test
	void rejectedOperationPreservesSelectionAndClipboard() {
		SelectionSession session = selectedSession();
		Runnable queued = queue(session);
		when(session.addOp(any(BreakUp.class))).thenReturn(null);
		queued.run();
		verify(session, never()).selectAllFromCurrentState(any());
		verify(session, never()).setCopyListToCurrentSelected();
	}

	@Test
	void selectsCompletedPartsBeforeUpdatingClipboard() {
		SelectionSession session = selectedSession();
		Runnable queued = queue(session);
		when(session.addOp(any(BreakUp.class))).thenAnswer(invocation -> {
			BreakUp operation = (BreakUp) invocation.getArguments()[0];
			operation.addNameInThisOperation("first-part");
			operation.addNameInThisOperation("second-part");
			return new Thread();
		});
		queued.run();
		InOrder order = inOrder(session);
		order.verify(session).addOp(any(BreakUp.class));
		order.verify(session).selectAllFromCurrentState(Arrays.asList("first-part", "second-part"));
		order.verify(session).setCopyListToCurrentSelected();
		order.verify(session).setKeyBindingFocus();
	}

	@Test
	void selectionWaitsForDecompositionWorker() throws Exception {
		SelectionSession session = selectedSession();
		Runnable queued = queue(session);
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		when(session.addOp(any(BreakUp.class))).thenAnswer(invocation -> {
			BreakUp operation = (BreakUp) invocation.getArguments()[0];
			Thread worker = new Thread(() -> {
				started.countDown();
				try {
					release.await();
					operation.addNameInThisOperation("finished-part");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
			worker.start();
			return worker;
		});
		FutureTask<Void> completion = new FutureTask<>(queued, null);
		new Thread(completion).start();
		try {
			assertTrue(started.await(5, TimeUnit.SECONDS));
			assertThrows(TimeoutException.class, () -> completion.get(100, TimeUnit.MILLISECONDS));
			verify(session, never()).selectAllFromCurrentState(any());
		} finally {
			release.countDown();
			completion.get(5, TimeUnit.SECONDS);
		}
		verify(session).selectAllFromCurrentState(Collections.singletonList("finished-part"));
	}

	private SelectionSession selectedSession() {
		SelectionSession session = mock(SelectionSession.class);
		when(session.selectedSnapshot()).thenReturn(Arrays.asList("first", "second"));
		doCallRealMethod().when(session).runBreakUp();
		return session;
	}

	private Runnable queue(SelectionSession session) {
		session.runBreakUp();
		ArgumentCaptor<Runnable> queued = ArgumentCaptor.forClass(Runnable.class);
		verify(session).submit(queued.capture());
		verify(session, never()).addOp(any());
		return queued.getValue();
	}
}
