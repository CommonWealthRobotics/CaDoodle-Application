package com.neuronrobotics.bowlerstudio.threed;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class SceneStyleDefaultsTest {

	private static final Map<String, String> EXPECTED = createDefaults();

	private static Map<String, String> createDefaults() {
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put("camera.orbit.fallMs", "120.0");
		defaults.put("camera.orbit.half", "700.0");
		defaults.put("camera.orbit.max", "0.08");
		defaults.put("camera.orbit.min", "0.012");
		defaults.put("camera.orbit.riseMs", "250.0");
		defaults.put("edges.angleDegrees", "35.0");
		defaults.put("edges.brightness", "0.0");
		defaults.put("edges.cameraSettleDelayMs", "120.0");
		defaults.put("edges.enabled", "true");
		defaults.put("edges.highlightWidthMultiplier", "1.8");
		defaults.put("edges.radius", "0.15");
		defaults.put("edges.sceneSettleDelayMs", "500.0");
		defaults.put("edges.stitchTolerance", "0.002");
		defaults.put("edges.topologyPrecision", "1.0E-5");
		defaults.put("edges.widthMode", "screen");
		defaults.put("edges.widthPx", "1.6");
		defaults.put("grid.ground.renderOpacity", "1.0");
		defaults.put("grid.major.brightnessMultiplier", "1.0");
		defaults.put("grid.major.lineWidthPx", "4.0");
		defaults.put("grid.major.opacityMultiplier", "1.0");
		defaults.put("grid.motionMode", "texture");
		defaults.put("grid.small.brightnessMultiplier", "0.85");
		defaults.put("grid.small.lineWidthPx", "2.0");
		defaults.put("grid.small.opacityMultiplier", "1.0");
		defaults.put("grid.small.visibleZoomDepth", "350.0");
		defaults.put("grid.small.zOffsetMm", "-0.05");
		defaults.put("halos.enabled", "true");
		defaults.put("handle.scale", "0.75");
		defaults.put("lighting.ambient", "0.47");
		defaults.put("lighting.fill.enabled", "true");
		defaults.put("lighting.fill.intensity", "0.16");
		defaults.put("lighting.fill.x", "0.65");
		defaults.put("lighting.fill.y", "-0.1");
		defaults.put("lighting.fill.z", "-0.25");
		defaults.put("lighting.key.enabled", "true");
		defaults.put("lighting.key.intensity", "0.52");
		defaults.put("lighting.key.x", "-0.8");
		defaults.put("lighting.key.y", "-1.0");
		defaults.put("lighting.key.z", "-0.45");
		defaults.put("lighting.mode", "stable");
		defaults.put("lighting.rim.enabled", "true");
		defaults.put("lighting.rim.intensity", "0.08");
		defaults.put("lighting.rim.x", "0.2");
		defaults.put("lighting.rim.y", "0.9");
		defaults.put("lighting.rim.z", "-0.15");
		return Collections.unmodifiableMap(defaults);
	}

	@Test
	void defaultsAreCompleteAndImmutable() {
		assertEquals(45, EXPECTED.size());
		assertEquals(EXPECTED, SceneStyleConfig.defaults());
		assertThrows(UnsupportedOperationException.class, () -> SceneStyleConfig.defaults().put("handle.scale", "2"));
	}

	@Test
	void resetOverwritesOnlyRegisteredKeysAndSavesAfterWriting() {
		Map<String, String> storage = new HashMap<>();
		EXPECTED.keySet().forEach(key -> storage.put(key, "changed"));
		storage.put("project.name", "untouched");
		storage.put("edges.futureSetting", "untouched");
		AtomicInteger writes = new AtomicInteger();
		AtomicInteger saves = new AtomicInteger();
		SceneStyleConfig.resetToDefaults((key, value) -> {
			assertTrue(EXPECTED.containsKey(key));
			storage.put(key, value);
			writes.incrementAndGet();
		}, () -> {
			assertEquals(EXPECTED.size(), writes.get());
			EXPECTED.forEach((key, value) -> assertEquals(value, storage.get(key)));
			saves.incrementAndGet();
		});
		assertEquals(1, saves.get());
		assertEquals(EXPECTED.size() + 2, storage.size());
		assertEquals("untouched", storage.get("project.name"));
		assertEquals("untouched", storage.get("edges.futureSetting"));
	}

	@Test
	void registeredDefaultsOverrideCallerFallbacksForMissingAndInvalidValues() {
		EXPECTED.forEach((key, value) -> {
			assertEquals(value, SceneStyleConfig.getString(key, "drift", (k, fallback) -> fallback));
			assertEquals(value, SceneStyleConfig.getString(key, "drift", (k, fallback) -> null));
			assertEquals(value, SceneStyleConfig.getString(key, "drift", (k, fallback) -> "  "));
			if (Arrays.asList("lighting.mode", "edges.widthMode", "grid.motionMode").contains(key)) {
				assertEquals("custom", SceneStyleConfig.getString(key, "drift", (k, fallback) -> " custom "));
			} else if (value.equals("true")) {
				assertTrue(SceneStyleConfig.getBoolean(key, false, (k, fallback) -> fallback));
				assertTrue(SceneStyleConfig.getBoolean(key, false, (k, fallback) -> null));
				assertTrue(SceneStyleConfig.getBoolean(key, false, (k, fallback) -> "invalid"));
				assertFalse(SceneStyleConfig.getBoolean(key, true, (k, fallback) -> " FALSE "));
			} else {
				double expected = Double.parseDouble(value);
				assertEquals(expected, SceneStyleConfig.getDouble(key, -999, (k, fallback) -> fallback));
				assertEquals(expected, SceneStyleConfig.getDouble(key, -999, (k, fallback) -> null));
				for (String invalid : Arrays.asList("", " ", "invalid", "NaN", "Infinity", "-Infinity", "1e999"))
					assertEquals(expected, SceneStyleConfig.getDouble(key, -999, (k, fallback) -> invalid));
				assertEquals(1.25, SceneStyleConfig.getDouble(key, -999, (k, fallback) -> " 1.25 "));
			}
		});
	}

	@Test
	void unregisteredKeysRetainCallerFallbacksAndValidValues() {
		assertEquals("fallback", SceneStyleConfig.getString("other", "fallback", (k, f) -> " "));
		assertEquals("custom", SceneStyleConfig.getString("other", "fallback", (k, f) -> " custom "));
		assertEquals(12.0, SceneStyleConfig.getDouble("other", 12, (k, f) -> "NaN"));
		assertTrue(SceneStyleConfig.getBoolean("other", true, (k, f) -> "invalid"));
		assertFalse(SceneStyleConfig.getBoolean("other", false, (k, f) -> null));
		assertEquals("WORLD", SceneStyleConfig.getString("edges.widthMode", "screen", (k, f) -> " WORLD "));
	}
}
