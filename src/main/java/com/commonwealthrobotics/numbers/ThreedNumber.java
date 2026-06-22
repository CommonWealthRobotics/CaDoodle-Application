package com.commonwealthrobotics.numbers;

import com.commonwealthrobotics.RulerManager;
import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.assets.FontSizeManager;
import com.neuronrobotics.bowlerstudio.assets.IFontSizeReciver;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.Vector3d;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

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
	private Affine lineOffset = new Affine();
	private Affine cameraOrient = new Affine();
	private Scale scaleTF = new Scale();
	private Scale scaleMesh = new Scale();

	private double scale;
	// private Affine resizeHandleLocation = new Affine();
	private Runnable onChange;
	private Affine reOrient;
	private TextFieldDimension tfDimension;
	private RulerManager ruler;
	private boolean lockout = false;
	// private static HashMap<ThreedNumber, Long> lastPressed = new HashMap<>();
	private double initialValue;
	private int wholeDigits; // Allowed whole digits before the dot
	private char decimalSeparator = '.';
	public boolean canceled = false; // Edit was canceled, keep old value
	private MeshView mesh = new Cube(1, 1, 1).toCSG().setColor(Color.GREY).getMesh();
	private PhongMaterial material;
	private Affine alignLoc = new Affine();
	private Vector3d vector3d;

	// DISABLE TIMEOUT FOR NOW
	/*
	 * private static Thread timeout = null; private static final long timeoutTime =
	 * 5000; private static void startThread() { if (timeout == null) { timeout =
	 * new Thread(() -> { while (true) { try { Thread.sleep(100); } catch
	 * (InterruptedException e) { return; } if (lastPressed.size() > 0) {
	 *
	 * ArrayList<ThreedNumber> torem = new ArrayList<>(); ArrayList<ThreedNumber>
	 * tocheck = new ArrayList<>(lastPressed.keySet()); for (ThreedNumber key :
	 * tocheck) { long val = lastPressed.get(key); if ((System.currentTimeMillis() -
	 * val) > timeoutTime) { key.runEnter(); torem.add(key); //
	 * com.neuronrobotics.sdk.common.Log.debug(key + " cleared on " + val); }
	 *
	 * } for (ThreedNumber key : torem) lastPressed.remove(key); } }
	 *
	 * }); timeout.start(); } }
	 *
	 * public void clearTimeout() { lastPressed.remove(this); }
	 */

	public ThreedNumber(Affine move, Affine workplaneOffset, Runnable onChange, TextFieldDimension tfDimension,
			RulerManager ruler, int wholeDigits, Vector3d orent) {
		// startThread(); // TIMEOUT DISABLED
		// this.session = session;
		this.move = move;
		this.workplaneOffset = workplaneOffset;
		this.onChange = onChange;
		this.tfDimension = tfDimension;
		this.ruler = ruler;
		this.wholeDigits = wholeDigits;
		this.vector3d = orent;

		mesh.getTransforms().add(move);
		mesh.getTransforms().add(alignLoc);
		mesh.getTransforms().add(workplaneOffset);
		mesh.getTransforms().add(location);
		mesh.getTransforms().add(lineOffset);
		mesh.getTransforms().add(scaleMesh);
		mesh.setVisible(false);
		material = new PhongMaterial();
		if (vector3d != null) {
			material.setDiffuseColor(new Color(vector3d.x, vector3d.y, vector3d.z, 0.75));
			material.setSpecularColor(new Color(vector3d.x, vector3d.y, vector3d.z, 0.75));
		}
		mesh.setCullFace(CullFace.BACK);
		mesh.setMaterial(material);

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
		FontSizeManager.addListener(new IFontSizeReciver() {
			@Override
			public void fontSizeChange(int tmp) {
				textField.setStyle("-fx-padding: 0; -fx-alignment: center; " + "-fx-font-size: " + tmp + "pt");
			}
		});
		// Regular expression: -aaaa.bbb, digitsin "aaaa" is defined in wholeDigits
		Pattern validPattern = decimalSeparator == '.'
				? Pattern.compile(String.format("^-?\\d{0,%d}(?:\\.\\d{0,3})?$", wholeDigits))
				: Pattern.compile(String.format("^-?\\d{0,%d}(?:\\,\\d{0,3})?$", wholeDigits));

		TextFormatter<String> textFormatter = new TextFormatter<>(change -> {
			String oldText = change.getControlText();
			String addText = change.getText();
			int pos = change.getCaretPosition();

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
		textField.prefWidthProperty().bind(
				textField.textProperty().length().multiply(7.0 / 12.0 * FontSizeManager.getDefaultSize()).add(20));
		textField.setOnKeyPressed(event -> {

			if (lockout)
				return;

			if (event.getCode() == KeyCode.ESCAPE) {
				event.consume();
				canceled = true;
				new Thread(() -> {
					runEnter();
				}).start();

				return;
			}

			if (event.getCode() == KeyCode.ENTER) {
				event.consume();
				new Thread(() -> {
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

			// long timeMillis = System.currentTimeMillis();
			// lastPressed.put(this, timeMillis);

		});

		reOrient = new Affine();
		textField.getTransforms().add(move);
		// textField.getTransforms().add(resizeHandleLocation);
		textField.getTransforms().add(workplaneOffset);
		textField.getTransforms().add(location);
		textField.getTransforms().add(lineOffset);
		textField.getTransforms().add(cameraOrient);
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
			setMostRecentValue(Double.parseDouble(t) + getMyOffset());
		} catch (NumberFormatException ex) {
			com.neuronrobotics.sdk.common.Log.error(ex);
			setValue(getMostRecentValue());
		}
	}

	public void setValue(double v) {
		lockout = true;
		canceled = false;
		double maxValue = Math.pow(10, wholeDigits) - Math.pow(10, -3); // 9999.999
		v = Math.max(-maxValue, Math.min(maxValue, v));
		v += 0.0; // Kill -0.000
		double offset = getMyOffset();
		com.neuronrobotics.sdk.common.Log.info(tfDimension + " to " + v + " offset by " + offset);
		String formatted3 = String.format(Locale.getDefault(), "%.3f", v - offset);
		textField.setText(formatted3);
		setMostRecentValue(v);
		lockout = false;
		// validate();
	}

	public ArrayList<Node> getTextField() {
		ArrayList<Node> parts = new ArrayList<Node>();
		parts.add(textField);
		if (vector3d != null)
			parts.add(mesh);

		return parts;
	}

	public void hide() {
		textField.setVisible(false);
		mesh.setVisible(false);
	}

	public void show() {
		initialValue += 0.0; // Kill -0.000
		initialValue = getMostRecentValue();
		setValue(initialValue);
		textField.setVisible(true);
		mesh.setVisible(true);
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
		double sf = ((distance / baseDistance) * baseScale);

		// Clamp the scale factor to a reasonable range
		double scaleFactor = Math.max(0.001, Math.min(90.0, sf));

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
			TransformFactory.nrToAffine(pr, cameraOrient);
			TransformFactory.nrToAffine(positionPin.setRotation(new RotationNR()), location);
		});
		if (vector3d != null)
			BowlerStudio.runLater(() -> {
				boolean isX = isXvector();
				boolean isY = isYvector();
				boolean isZ = isZvector();
				double X = -move.getTx() * vector3d.x / 2;
				double Y = -move.getTy() * vector3d.y / 2;
				double Z = -move.getTz() * vector3d.z / 2;
				double offset = getMyOffset();

				double length = getMostRecentValue() - offset;

				double barsize = scaleFactor * 5;
				scaleMesh.setX(isX ? length : barsize);
				scaleMesh.setY(isY ? length : barsize);
				scaleMesh.setZ(isZ ? length : barsize);
				mesh.setViewOrder(2);
				TransformFactory.nrToAffine(new TransformNR(X, Y, Z), lineOffset);

			});
	}

	public double getMyOffset() {
		return ruler.getOffset(tfDimension);
	}

	private boolean isZvector() {
		return vector3d.z > 0;
	}

	private boolean isYvector() {
		return vector3d.y > 0;
	}

	private boolean isXvector() {
		return vector3d.x > 0;
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
