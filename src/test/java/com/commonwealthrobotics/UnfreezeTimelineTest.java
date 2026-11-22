package com.commonwealthrobotics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.SaveOverwriteException;

class UnfreezeTimelineTest {
	@Test
	void persistsUnfreezeBeforeReloadingAndInitializingTheReplacement() throws Exception {
		List<String> steps = new ArrayList<>();
		File path = new File("project.doodle");
		CaDoodleFile original = new CaDoodleFile() {
			@Override
			public void clearFrozenIndex(int index) {
				assertEquals(8, index);
				steps.add("unfreeze");
			}

			@Override
			public File saveWhenReady() {
				steps.add("save");
				return path;
			}

			@Override
			public File getSelf() {
				return path;
			}
		};
		CaDoodleFile replacement = new CaDoodleFile() {
			@Override
			public void initialize() {
				steps.add("initialize");
			}
		};
		ActiveProject project = new ActiveProject() {
			@Override
			public CaDoodleFile get() {
				return original;
			}

			@Override
			public CaDoodleFile setActiveProject(File file) {
				assertEquals(path, file);
				steps.add("reload");
				return replacement;
			}
		};
		project.unfreezeTimeline(8);
		assertEquals(List.of("unfreeze", "save", "reload", "initialize"), steps);
	}

	@Test
	void failedSaveDoesNotReloadStaleProjectData() {
		SaveOverwriteException failure = new SaveOverwriteException("test write failure");
		CaDoodleFile original = new CaDoodleFile() {
			@Override
			public void clearFrozenIndex(int index) {
			}

			@Override
			public File saveWhenReady() throws SaveOverwriteException {
				throw failure;
			}
		};
		ActiveProject project = new ActiveProject() {
			@Override
			public CaDoodleFile get() {
				return original;
			}

			@Override
			public CaDoodleFile setActiveProject(File file) {
				throw new AssertionError("failed save must not reload");
			}
		};
		assertSame(failure, assertThrows(SaveOverwriteException.class, () -> project.unfreezeTimeline(8)));
	}
}
