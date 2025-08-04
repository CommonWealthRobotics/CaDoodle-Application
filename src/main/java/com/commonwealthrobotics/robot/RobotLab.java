package com.commonwealthrobotics.robot;

import java.util.HashMap;
import java.util.Optional;

import com.commonwealthrobotics.ActiveProject;
import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.creature.MobileBaseBuilder;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.robot.MakeRobot;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class RobotLab {

	private SelectionSession session;
	private ActiveProject ap;
	private MobileBaseBuilder builder = null;
	private VBox baseRobotBox;
	private Button makeRobotButton;

	public RobotLab(SelectionSession session, ActiveProject ap, VBox baseRobotBox, Button makeRobotButton) {
		this.session = session;
		this.ap = ap;
		this.baseRobotBox = baseRobotBox;
		this.makeRobotButton = makeRobotButton;

	}

	public void setRobotLabOpenState(boolean isOpen) {
		if (isOpen) {
			updateDisplay();
		} else {
			builder = null;
		}
	}

	private void updateDisplay() {
		HashMap<String, MobileBaseBuilder> robots = ap.get().getRobots();
		if (builder == null)
			for (String s : robots.keySet()) {
				if (builder != null)
					break;
				for (CSG c : session.getSelectedCSG(session.selectedSnapshot())) {
					Optional<String> mobileBaseName = c.getMobileBaseName();
					if (mobileBaseName.isPresent()) {
						if (mobileBaseName.get().contentEquals(s)) {
							builder = robots.get(s);
							break;
						}
					}
				}
			}
		if (builder == null) {
			if (!baseRobotBox.getChildren().contains(makeRobotButton)) {
				baseRobotBox.getChildren().add(makeRobotButton);
			}
		} else {
			if (baseRobotBox.getChildren().contains(makeRobotButton)) {
				baseRobotBox.getChildren().remove(makeRobotButton);
			}
		}
	}

	public void makeRobot() {
		new Thread(() -> {
			MakeRobot mr = new MakeRobot();
			mr.setNames(session.selectedSnapshot());
			
			ap.addOp(mr);
			builder = ap.get().getRobots().get(mr.getName());
			if (builder == null)
				throw new RuntimeException("Failed to create robot!");
			updateDisplay();
		}).start();
	}

}
