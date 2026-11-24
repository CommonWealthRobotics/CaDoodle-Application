package com.commonwealthrobotics.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.commonwealthrobotics.ActiveProject;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleOperation;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.Simplify;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.parametrics.CSGDatabaseInstance;
import eu.mihosoft.vrl.v3d.parametrics.IParameterChanged;

class SimplifySelectionTest {
	@Test
	void buttonCreatesOneOperationForSelectedObjects() {
		SelectionSession session = mock(SelectionSession.class);
		when(session.selectedSnapshot()).thenReturn(Arrays.asList("first", "second"));
		doCallRealMethod().when(session).runSimplify();
		session.runSimplify();
		ArgumentCaptor<Simplify> operation = ArgumentCaptor.forClass(Simplify.class);
		verify(session).addOp(operation.capture());
		assertEquals(Arrays.asList("first", "second"), operation.getValue().getNamesAddedInThisOperation());
	}

	@Test
	void emptySelectionDoesNotCreateOperation() {
		SelectionSession session = mock(SelectionSession.class);
		when(session.selectedSnapshot()).thenReturn(Collections.emptyList());
		doCallRealMethod().when(session).runSimplify();
		session.runSimplify();
		verify(session, never()).addOp(any());
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
