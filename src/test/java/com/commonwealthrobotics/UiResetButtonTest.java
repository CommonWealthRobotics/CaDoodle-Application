package com.commonwealthrobotics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.PropertyResourceBundle;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import org.w3c.dom.Document;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

class UiResetButtonTest {
	@Test
	void resetActionIsBelowHandleScaleInTheSameGrid() throws Exception {
		try (InputStream input = getClass().getResourceAsStream("/com/commonwealthrobotics/Settings.fxml")) {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
			XPath xpath = XPathFactory.newInstance().newXPath();
			Element button = (Element) xpath.evaluate("//Button[@*[name()='fx:id']='uiResetDefaults']", document,
					javax.xml.xpath.XPathConstants.NODE);
			Node scale = (Node) xpath.evaluate("//TextField[@*[name()='fx:id']='uiHandleScale']", document,
					javax.xml.xpath.XPathConstants.NODE);
			assertNotNull(button);
			assertNotNull(scale);
			assertEquals(scale.getParentNode(), button.getParentNode());
			assertEquals("1", button.getAttribute("GridPane.rowIndex"));
			assertEquals("%settings.ui_reset_defaults", button.getAttribute("text"));
			assertEquals("#onResetUiDefaults", button.getAttribute("onAction"));
			assertNotNull(SettingsManager.class.getDeclaredMethod("onResetUiDefaults", javafx.event.ActionEvent.class));
		}
	}

	@Test
	void everyLanguageDefinesTheResetLabelWithoutFallback() throws Exception {
		try (Stream<Path> files = Files.list(Paths.get("src/main/resources/lang"))) {
			List<Path> bundles = files.filter(path -> path.getFileName().toString().startsWith("Messages_"))
					.filter(path -> path.toString().endsWith(".properties")).collect(Collectors.toList());
			assertFalse(bundles.isEmpty());
			for (Path path : bundles) {
				try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path),
						StandardCharsets.UTF_8)) {
					PropertyResourceBundle bundle = new PropertyResourceBundle(reader);
					assertFalse(bundle.getString("settings.ui_reset_defaults").codePoints()
							.allMatch(Character::isWhitespace), path.toString());
				}
			}
		}
	}
}
