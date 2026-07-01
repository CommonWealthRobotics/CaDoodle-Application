package com.commonwealthrobotics;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Arrays;
import java.util.Optional;

public enum PluginType {

	SVG("inkscape"), BLENDER("blender"), FREECAD("freecad"), BUILD123D("build123d"), OPENSCAD("openscad"), ECLIPSE(
			"eclipse");

	private final String key;
	private final ObservableList<String> styleClass;

	PluginType(String key) {
		this.key = key;
		this.styleClass = FXCollections.observableArrayList(getCssClass());
	}

	/**
	 * Returns the ObservableList<String> that can be passed directly to
	 * Node.getStyleClass().setAll(...)
	 */
	public ObservableList<String> getStyleClass() {
		return styleClass;
	}

	/**
	 * Returns the CSS class name (e.g. "svg-image-view")
	 */
	public String getCssClass() {
		if (key.contentEquals("inkscape"))
			return "svg-image-view";
		return key + "-image-view";
	}

	/**
	 * Returns the lookup key (e.g. "svg")
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Loads from the first part of the CSS class. Examples: "svg" -> SVG "SVG" ->
	 * SVG "build123d" -> BUILD123D
	 */
	public static Optional<PluginType> fromString(String value) {
		if (value == null) {
			return Optional.empty();
		}
		return Arrays.stream(values()).filter(v -> v.key.equalsIgnoreCase(value.trim())).findFirst();
	}
}
