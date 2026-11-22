package com.commonwealthrobotics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ConvertToBlender;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ConvertToFreeCAD;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.ConvertToOpenSCAD;

class ConversionTranslationsTest {
	@TestFactory
	Stream<DynamicTest> conversionOperationsHaveLabelsInEveryLanguage() throws Exception {
		List<String> keys = Arrays.asList(ConvertToBlender.class.getSimpleName(),
				ConvertToFreeCAD.class.getSimpleName(), ConvertToOpenSCAD.class.getSimpleName());
		try (Stream<Path> files = Files.list(Paths.get("src/main/resources/lang"))) {
			return files.filter(path -> path.getFileName().toString().endsWith(".properties")).sorted()
					.collect(Collectors.toList()).stream()
					.map(path -> DynamicTest.dynamicTest(path.getFileName().toString(), () -> {
						String resource = "/lang/" + path.getFileName();
						try (InputStream input = ConversionTranslationsTest.class.getResourceAsStream(resource)) {
							assertNotNull(input, resource);
							PropertyResourceBundle bundle = new PropertyResourceBundle(
									new InputStreamReader(input, StandardCharsets.UTF_8));
							for (String key : keys) {
								String label = bundle.getString(key);
								assertFalse(label.codePoints().allMatch(Character::isWhitespace), key);
								assertNotEquals(key, label);
							}
						}
					}));
		}
	}
}
