package com.commonwealthrobotics.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.RulerManager;
import com.commonwealthrobotics.WorkplaneManager;
import com.commonwealthrobotics.controls.ControlSprites;
import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.VirtualCameraMobileBase;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.input.PickResultChooser;

import javafx.application.Platform;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

class LimbRotationNumberTest {
	@BeforeAll
	static void startFx() {
		com.sun.javafx.application.PlatformImpl.startup(() -> {
		});
	}

	@Test
	void limbRotationNumbersRenderAndPickAboveOverlappingHandles() throws Exception {
		onFx(() -> {
			Fixture f = new Fixture();
			assertEquals(3, f.numbers.size());
			Rectangle handle = new Rectangle(300, 100, Color.RED);
			handle.setTranslateZ(-100);
			handle.setViewOrder(-2);
			handle.setDepthTest(DepthTest.DISABLE);
			f.userGroup.getChildren().add(handle);
			Scene scene = new Scene(f.userGroup, 300, 100, true);
			for (TextField field : f.numbers) {
				field.setManaged(false);
				field.resizeRelocate(20, 20, 120, 40);
				field.setStyle(
						"-fx-background-color: lime; -fx-background-radius: 0; -fx-border-width: 0; -fx-padding: 0;");
				field.setVisible(true);
				f.userGroup.applyCss();
				f.userGroup.layout();
				Node picked = pick(f.userGroup);
				while (picked != null && picked != field)
					picked = picked.getParent();
				assertSame(field, picked, "The limb angle editor must receive the click instead of the handle");
				assertEquals(Color.LIME, scene.snapshot(null).getPixelReader().getColor(30, 30));
				field.setVisible(false);
				assertSame(handle, pick(f.userGroup), "Hiding the angle editor must restore handle picking");
			}
		});
	}

	private static Node pick(Group root) {
		PickRay ray = new PickRay(new Vec3d(30, 30, -1000), new Vec3d(0, 0, 1), 0, Double.POSITIVE_INFINITY);
		PickResultChooser chooser = new PickResultChooser();
		NodeHelper.pickNode(root, ray, chooser);
		assertNotNull(chooser.toPickResult());
		return chooser.toPickResult().getIntersectedNode();
	}

	private static void onFx(Runnable action) throws Exception {
		FutureTask<Void> task = new FutureTask<>(action, null);
		Platform.runLater(task);
		task.get(30, TimeUnit.SECONDS);
	}

	private static final class Fixture {
		final Group userGroup = new Group();
		final List<TextField> numbers = new ArrayList<>();

		Fixture() {
			BowlerStudio3dEngine engine = mock(BowlerStudio3dEngine.class);
			VirtualCameraMobileBase camera = mock(VirtualCameraMobileBase.class);
			when(camera.getCamera()).thenReturn(new PerspectiveCamera());
			when(engine.getFlyingCamera()).thenReturn(camera);
			when(engine.getOverlayPane()).thenReturn(new Pane());
			doAnswer(invocation -> {
				Node node = (Node) invocation.getArguments()[0];
				userGroup.getChildren().add(0, node);
				if (node instanceof TextField)
					numbers.add((TextField) node);
				return null;
			}).when(engine).addUserNode(any(Node.class));
			new LimbControlManager(engine, mock(SelectionSession.class), mock(ActiveProject.class),
					mock(RulerManager.class), mock(WorkplaneManager.class), mock(ControlSprites.class));
		}
	}
}
