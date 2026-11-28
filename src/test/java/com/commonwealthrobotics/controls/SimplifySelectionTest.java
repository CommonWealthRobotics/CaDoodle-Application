package com.commonwealthrobotics.controls;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.commonwealthrobotics.ActiveProject;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Simplify;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.parametrics.CSGDatabaseInstance;
import eu.mihosoft.vrl.v3d.parametrics.IParameterChanged;

class SimplifySelectionTest {
	@TempDir
	File directory;

	@Test
	void buttonCreatesOneOperationAndSelectsItsOutputsBeforeUpdatingClipboard() {
		SelectionSession session = selectedSession();
		Runnable queued = queue(session);
		when(session.addOp(any(Simplify.class))).thenAnswer(invocation -> {
			Simplify operation = (Simplify) invocation.getArguments()[0];
			processSelection(operation);
			return new Thread();
		});
		queued.run();
		ArgumentCaptor<Simplify> operation = ArgumentCaptor.forClass(Simplify.class);
		InOrder order = inOrder(session);
		order.verify(session).addOp(operation.capture());
		order.verify(session).selectAllFromCurrentState(operation.getValue().getNamesAddedInThisOperation());
		order.verify(session).setCopyListToCurrentSelected();
		order.verify(session).setKeyBindingFocus();
	}

	@Test
	void emptySelectionDoesNotScheduleOperation() {
		SelectionSession session = mock(SelectionSession.class);
		when(session.selectedSnapshot()).thenReturn(Collections.emptyList());
		doCallRealMethod().when(session).runSimplify();
		session.runSimplify();
		verify(session, never()).submit(any(Runnable.class));
		verify(session, never()).addOp(any());
	}

	@Test
	void rejectedOperationPreservesSelectionAndClipboard() {
		SelectionSession session = selectedSession();
		Runnable queued = queue(session);
		when(session.addOp(any(Simplify.class))).thenReturn(null);
		queued.run();
		verify(session, never()).selectAllFromCurrentState(any());
		verify(session, never()).setCopyListToCurrentSelected();
		verify(session, never()).setKeyBindingFocus();
	}

	@Test
	void selectionWaitsForSimplificationWorker() throws Exception {
		SelectionSession session = selectedSession();
		Runnable queued = queue(session);
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicReference<Throwable> workerFailure = new AtomicReference<>();
		when(session.addOp(any(Simplify.class))).thenAnswer(invocation -> {
			Simplify operation = (Simplify) invocation.getArguments()[0];
			Thread worker = new Thread(() -> {
				started.countDown();
				try {
					release.await();
					processSelection(operation);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					workerFailure.set(e);
				} catch (Throwable failure) {
					workerFailure.set(failure);
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
		assertNull(workerFailure.get());
		ArgumentCaptor<Simplify> operation = ArgumentCaptor.forClass(Simplify.class);
		verify(session).addOp(operation.capture());
		assertEquals(2, operation.getValue().getNamesAddedInThisOperation().size());
		verify(session).selectAllFromCurrentState(operation.getValue().getNamesAddedInThisOperation());
	}

	private SelectionSession selectedSession() {
		SelectionSession session = mock(SelectionSession.class);
		when(session.selectedSnapshot()).thenReturn(Arrays.asList("first", "second"));
		doCallRealMethod().when(session).runSimplify();
		return session;
	}

	private Runnable queue(SelectionSession session) {
		session.runSimplify();
		ArgumentCaptor<Runnable> queued = ArgumentCaptor.forClass(Runnable.class);
		verify(session).submit(queued.capture());
		verify(session, never()).addOp(any());
		return queued.getValue();
	}

	private void processSelection(Simplify operation) {
		operation.setCaDoodleFile(new CaDoodleFile().setSelf(new File(directory, "selection.doodle")));
		// Exercise selection without native mesh processing
		operation.setSimplify(0);
		CSG first = new Cube(1).toCSG().setName("first");
		CSG second = new Cube(2).toCSG().setName("second");
		CSG untouched = new Cube(3).toCSG().setName("untouched");
		List<CSG> result = operation.process(Arrays.asList(first, untouched, second));
		assertEquals(3, result.size());
		assertSame(untouched, result.get(1));
		assertNotEquals(first.getName(), result.get(0).getName());
		assertNotEquals(second.getName(), result.get(2).getName());
		assertEquals(Arrays.asList(result.get(0).getName(), result.get(2).getName()),
				operation.getNamesAddedInThisOperation());
	}

	@Test
	void ownParameterEditsScheduleRegenerationWithoutReplacingUpstreamListeners() throws Exception {
		SelectionSession session = mock(SelectionSession.class);
		ActiveProject project = mock(ActiveProject.class);
		CaDoodleFile file = mock(CaDoodleFile.class);
		CSGDatabaseInstance db = mock(CSGDatabaseInstance.class);
		when(project.get()).thenReturn(file);
		when(file.getCsgDBinstance()).thenReturn(db);
		when(file.getPercentInitialized()).thenReturn(1.0);
		Field activeProject = SelectionSession.class.getDeclaredField("ap");
		activeProject.setAccessible(true);
		activeProject.set(session, project);
		Simplify operation = new Simplify();
		String key = operation.getName() + "_CaDoodle_Simplify_simplify";
		CSG shape = mock(CSG.class);
		when(shape.getParameters(db)).thenReturn(new HashSet<>(Arrays.asList(key, "upstream_CaDoodle_radius")));
		Method setup = SelectionSession.class.getDeclaredMethod("setUpParametrics", List.class,
				CaDoodleOperation.class);
		setup.setAccessible(true);
		setup.invoke(session, Collections.singletonList(shape), operation);
		ArgumentCaptor<IParameterChanged> listener = ArgumentCaptor.forClass(IParameterChanged.class);
		verify(db).addParameterListener(eq(key), listener.capture());
		verify(db, never()).clearParameterListeners("upstream_CaDoodle_radius");
		listener.getValue().parameterChanged(key, null);
		verify(session).submit(any(Runnable.class));
		when(file.isRegenerating()).thenReturn(true);
		listener.getValue().parameterChanged(key, null);
		verify(session, times(1)).submit(any(Runnable.class));
	}
}
