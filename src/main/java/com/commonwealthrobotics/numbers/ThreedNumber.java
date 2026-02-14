package com.commonwealthrobotics.numbers;

import com.commonwealthrobotics.RulerManager;
import com.commonwealthrobotics.controls.SelectionSession;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Bounds;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.application.Platform;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.function.UnaryOperator;

import java.text.ParseException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class ThreedNumber {
	private double screenW;
	private double screenH;
	private double zoom;
	private TransformNR cf;
	// private SelectionSession session;
	private Affine move;
	private Affine workplaneOffset;
	private TextField textField = new TextField("20.000");
	private TransformNR positionPin;
	private double mostRecentValue = 20;

	private Affine location = new Affine();
	private Affine cameraOrent = new Affine();
	private Scale scaleTF = new Scale();
	private double scale;
//	private Affine resizeHandleLocation = new Affine();
	private Runnable onChange;
	private Affine reOrient;
	private TextFieldDimension tfDimension;
	private RulerManager ruler;
	private boolean lockout = false;
//	private static HashMap<ThreedNumber, Long> lastPressed = new HashMap<>();
	private double initialValue;
	private int wholeDigits;   // Allowed whole digits before the dot
	private char decimalSeparator = '.';
	public boolean canceled = false; // Edit was canceled, keep old value

// DISABLE TIMEOUT FOR NOW
/*
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
*/

	public ThreedNumber(Affine move, Affine workplaneOffset, Runnable onChange, TextFieldDimension tfDimension,
			RulerManager ruler, int wholeDigits) {
		// startThread(); // TIMEOUT DISABLED
		// this.session = session;
		this.move = move;
		this.workplaneOffset = workplaneOffset;
		this.onChange = onChange;
		this.tfDimension = tfDimension;
		this.ruler = ruler;
		this.wholeDigits = wholeDigits;

		getSystemDecimalSeparator();

		// Select number at edit box select
		textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal) {
				Platform.runLater(() -> {
					textField.selectAll();
					textField.requestFocus();
				});
			}
		});

		textField.setMinWidth(62);
		textField.setStyle("-fx-padding: 0; -fx-alignment: center;");

		// Regular expression: -aaaa.bbb, digitsin "aaaa" is defined in wholeDigits
		Pattern validPattern = decimalSeparator == '.' ?
			Pattern.compile(String.format("^-?\\d{0,%d}(?:\\.\\d{0,3})?$", wholeDigits)) :
			Pattern.compile(String.format("^-?\\d{0,%d}(?:\\,\\d{0,3})?$", wholeDigits));

		TextFormatter<String> textFormatter = new TextFormatter<>(change -> {
			String oldText = change.getControlText();
			String addText = change.getText();
			int	pos	 = change.getCaretPosition();

			if ("-".equals(addText)) {
				if (oldText.startsWith("-")) { // Remove "-" in front
					change.setText("");
					change.setRange(0, 1);
					change.setCaretPosition(Math.max(0, pos - 2));
					change.setAnchor(Math.max(0, pos - 2));
				} else {
					change.setText("-");
					change.setRange(0, 0);
				}
				return validPattern.matcher(change.getControlNewText()).matches() ? change : null;
			}

			return validPattern.matcher(change.getControlNewText()).matches() ? change : null;
		});

		textField.setTextFormatter(textFormatter);
		textField.prefWidthProperty().bind(textField.textProperty().length().multiply(7).add(20));
		textField.setOnKeyPressed(event -> {

			if (lockout)
				return;

			if (event.getCode() == KeyCode.ESCAPE) {
				event.consume();
				canceled = true;
				new Thread(()-> {
					runEnter();
				}).start();

				return;
			}

			if (event.getCode() == KeyCode.ENTER) {
				event.consume();
				new Thread(()->{
					runEnter();
				}).start();

				return;
			}

			// Up arrow increases value by 1.0
			if (event.getCode() == KeyCode.UP) {
				event.consume();
				double currentValue = getMostRecentValue();
				if (currentValue + 1.0 < Math.pow(10, wholeDigits))
					setValue(currentValue + 1.0);
				return;
			}

			// Down arrow increases value by 1.0
			if (event.getCode() == KeyCode.DOWN) {
				event.consume();
				double currentValue = getMostRecentValue();
				if (currentValue - 1.0 > -Math.pow(10, wholeDigits))
					setValue(currentValue - 1.0);
				return;
			}

			// Page up increase the value by 10.0
			if (event.getCode() == KeyCode.PAGE_UP) {
				event.consume();
				double currentValue = getMostRecentValue();
				if (currentValue + 10.0 < Math.pow(10, wholeDigits))
					setValue(currentValue + 10.0);
				return;
			}

			// Page down decreases the value by 10.0
			if (event.getCode() == KeyCode.PAGE_DOWN) {
				event.consume();
				double currentValue = getMostRecentValue();
				if (currentValue - 10.0 > -Math.pow(10, wholeDigits))
					setValue(currentValue - 10.0);
				return;
			}

			//long timeMillis = System.currentTimeMillis();
			//lastPressed.put(this, timeMillis);

		});

		reOrient = new Affine();
		textField.getTransforms().add(move);
//		textField.getTransforms().add(resizeHandleLocation);
		textField.getTransforms().add(workplaneOffset);
		textField.getTransforms().add(location);
		textField.getTransforms().add(cameraOrent);
		textField.getTransforms().add(scaleTF);
		textField.getTransforms().add(reOrient);

	} // Constructor

	public void getSystemDecimalSeparator() {

		Locale systemLocale = Locale.getDefault();
		this.decimalSeparator = DecimalFormatSymbols.getInstance(systemLocale).getDecimalSeparator();
	}

	public boolean isFocused() {
		return textField.isFocused();
	}

	private void runEnter() {

		// If value didn't change, report operation canceled, prevents save
		if (Math.abs(getMostRecentValue() - initialValue) < 1e-6)
			canceled = true;

		BowlerStudio.runLater(this::hide);
		validate();
		onChange.run();
	}

	private void validate() {
		// parseDouble needs a dot as decimal separator
		String t = textField.getText().replace(',', '.');
		// com.neuronrobotics.sdk.common.Log.error(" Validating string " + t);
		if (t.length() == 0) {
			// empty string, do nothing
			return;
		}
		try {
			setMostRecentValue(Double.parseDouble(t) + ruler.getOffset(tfDimension));
		} catch (NumberFormatException ex) {
			setValue(getMostRecentValue());
		}
	}

	public void setValue(double v) {
		lockout = true;
		canceled = false;
		double maxValue = Math.pow(10, wholeDigits) - Math.pow(10, -3); // 9999.999
		v = Math.max(-maxValue, Math.min(maxValue, v));
		v += 0.0; // Kill -0.000
		String formatted3 = String.format(Locale.getDefault(), "%.3f", v - ruler.getOffset(tfDimension));
		textField.setText(formatted3);
		setMostRecentValue(v);
		lockout = false;
		// validate();
	}

	public Node getTextField() {
		return textField;
	}

	public void hide() {
		textField.setVisible(false);
	}

	public void show() {
		initialValue += 0.0; // Kill -0.000
		initialValue = getMostRecentValue();
		textField.setText(String.format(Locale.getDefault(), "%.3f", initialValue));
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
			TransformFactory.nrToAffine(new TransformNR(10, 5, 0, new RotationNR(0, 180, 0)), reOrient);

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

	public void mouseTransparent(boolean b) {
		textField.setMouseTransparent(b);
	}
}
