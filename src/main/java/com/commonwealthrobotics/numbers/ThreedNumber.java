package com.commonwealthrobotics.numbers;

import com.commonwealthrobotics.RulerManager;
import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.util.StringConverter;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import javafx.util.StringConverter;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Locale;

public class ThreedNumber {
	private double screenW;
	private double screenH;
	private double zoom;
	private TransformNR cf;
	// private SelectionSession session;
	private Affine move;
	private Affine workplaneOffset;
	private TextField textField = new TextField("20.00");
	private TransformNR positionPin;
	private double mostRecentValue = 20;

	private Affine location = new Affine();
	private Affine cameraOrent = new Affine();
	private Scale scaleTF = new Scale();
	private double scale;
	private Affine resizeHandleLocation = new Affine();
	private Runnable onSelect;
	private Affine reorent;
	private TextFieldDimention dim;
	private RulerManager ruler;
	private boolean lockout = false;
	private static HashMap<ThreedNumber, Long> lastPressed = new HashMap<>();
	private static Thread timeout = null;
	private static final long timeoutTime = 5000;

	private static void startThread() {
		if (timeout == null) {
			timeout = new Thread(() -> {
				while (true) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						return;
					}
					if (lastPressed.size() > 0) {

						ArrayList<ThreedNumber> torem = new ArrayList<>();
						ArrayList<ThreedNumber> tocheck = new ArrayList<>(lastPressed.keySet());
						for (ThreedNumber key : tocheck) {
							long val = lastPressed.get(key);
							if ((System.currentTimeMillis() - val) > timeoutTime) {
								key.runEnter();
								torem.add(key);
								// com.neuronrobotics.sdk.common.Log.debug(key + " cleared on " + val);
							}

						}
						for (ThreedNumber key : torem)
							lastPressed.remove(key);
					}
				}

			});
			timeout.start();
		}
	}

	public void clearTimeout() {
		lastPressed.remove(this);
	}

	public ThreedNumber(Affine move, Affine workplaneOffset, Runnable onSelect, TextFieldDimention dim,
			RulerManager ruler) {
		startThread();
		// this.session = session;
		this.move = move;
		this.workplaneOffset = workplaneOffset;
		this.onSelect = onSelect;
		this.dim = dim;
		this.ruler = ruler;
		// Set the preferred width to use computed size
		textField.setPrefWidth(50);
		Pattern validPattern = Pattern.compile("[0-9.\\-]*");

		// TextFormatter with filter
		TextFormatter<String> textFormatter = new TextFormatter<>(change -> {
			String newText = change.getControlNewText();
			if (validPattern.matcher(newText).matches()) {
				return change; // Accept the change
			}
			return null; // Reject the change
		});
		textField.setTextFormatter(textFormatter);
		textField.prefWidthProperty().bind(textField.textProperty().length().multiply(8).add(20));
		textField.setOnKeyPressed(event -> {
			if (lockout)
				return;
			if (event.getCode() == KeyCode.ENTER) {
				new Thread(() -> {
					runEnter();
				}).start();
				event.consume(); // prevent parent from stealing the event
				return;
			}
			long timeMillis = System.currentTimeMillis();
			// com.neuronrobotics.sdk.common.Log.debug(this + " set on " + timeMillis);

			lastPressed.put(this, timeMillis);
		});
		reorent = new Affine();
		textField.getTransforms().add(move);
		textField.getTransforms().add(resizeHandleLocation);
		textField.getTransforms().add(workplaneOffset);
		textField.getTransforms().add(location);
		textField.getTransforms().add(cameraOrent);
		textField.getTransforms().add(scaleTF);
		textField.getTransforms().add(reorent);

	}

	public boolean isFocused() {
		return textField.isFocused();
	}

	private void runEnter() {
		BowlerStudio.runLater(this::hide);
		validate();
		onSelect.run();
		clearTimeout();
	}

	private void validate() {
		// Number set from event
		String t = textField.getText();
		if (t.length() == 0) {
			// empty string, do nothing
			return;
		}
		try {
			setMostRecentValue(Double.parseDouble(t) + ruler.getOffset(dim));
		} catch (NumberFormatException ex) {
			setValue(getMostRecentValue());
		}
	}

	public void setValue(double v) {
		lockout = true;
		String formatted3 = String.format(Locale.US, "%.3f", v - ruler.getOffset(dim));
		textField.setText(formatted3);
		setMostRecentValue(v);
		lockout = false;
		// validate();
	}

	public Node get() {
		return textField;
	}

	public void hide() {
		textField.setVisible(false);
	}

	public void show() {
		textField.setVisible(true);
	}

	public void threeDTarget(double w, double h, double zo, TransformNR positionPin, TransformNR c) {
		this.screenW = w;
		this.screenH = h;
		this.zoom = zo;
		this.positionPin = positionPin;
		this.cf = c;
		if (c == null)
			return;

		// com.neuronrobotics.sdk.common.Log.error(cf.toSimpleString());
		// Calculate the vector from camera to target
		double x = positionPin.getX() - cf.getX();
		double y = positionPin.getY() - cf.getY();
		double z = positionPin.getZ() - cf.getZ();

		// Calculate the distance between camera and target
		double distance = Math.sqrt(x * x + y * y + z * z);

		// Define a base scale and distance
		double baseScale = 0.75;
		double baseDistance = 1000.0;

		// Calculate the scale factor
		double scaleFactor = ((distance / baseDistance) * baseScale);

		// Clamp the scale factor to a reasonable range
		scaleFactor = Math.max(0.001, Math.min(90.0, scaleFactor));

		setScale(scaleFactor * 1.25);
		TransformNR pureRot = new TransformNR(cf.getRotation());
		TransformNR wp = new TransformNR(TransformFactory.affineToNr(workplaneOffset).getRotation());
		TransformNR pr = wp.inverse().times(pureRot);

		// com.neuronrobotics.sdk.common.Log.error("Point From Cam distaance "+vect+"
		// scale "+scale);
		// com.neuronrobotics.sdk.common.Log.error("");
		BowlerStudio.runLater(() -> {
			TransformFactory.nrToAffine(new TransformNR(10, 5, 0, new RotationNR(0, 180, 0)), reorent);

			scaleTF.setX(getScale());
			scaleTF.setY(getScale());
			scaleTF.setZ(getScale());
			TransformFactory.nrToAffine(pr, cameraOrent);
			TransformFactory.nrToAffine(positionPin.setRotation(new RotationNR()), location);
		});
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}

	public double getMostRecentValue() {
		validate();
		return mostRecentValue;
	}

	public void setMostRecentValue(double mostRecentValue) {
		// new Exception().printStackTrace();
		// com.neuronrobotics.sdk.common.Log.error("internal number set to
		// "+mostRecentValue);

		this.mostRecentValue = mostRecentValue;
	}
}
