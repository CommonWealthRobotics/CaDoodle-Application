package com.commonwealthrobotics;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class BreakUpResourcesTest {
	@Test
	void everyLanguageHasOneTranslatedLabel() throws Exception {
		try (Stream<Path> files = Files.list(Paths.get("src/main/resources/lang"))) {
			List<Path> bundles = files.filter(path -> path.getFileName().toString().startsWith("Messages"))
					.filter(path -> path.toString().endsWith(".properties")).collect(Collectors.toList());
			assertEquals(61, bundles.size());
			for (Path path : bundles) {
				assertEquals(1, Files.readAllLines(path, StandardCharsets.UTF_8).stream()
						.filter(line -> line.startsWith("BreakUp=")).count(), path.toString());
				try (InputStream input = Files.newInputStream(path)) {
					PropertyResourceBundle bundle = new PropertyResourceBundle(
							new InputStreamReader(input, StandardCharsets.UTF_8));
					String label = bundle.getString("BreakUp");
					assertFalse(label.codePoints().allMatch(Character::isWhitespace), path.toString());
					assertNotEquals("BreakUp", label);
				}
			}
		}
	}

	@Test
	void buttonPrecedesSimplifyAndUsesTranslatedTooltipAndBundledIcon() throws Exception {
		try (InputStream input = getClass().getResourceAsStream("MainWindow.fxml")) {
			NodeList buttons = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)
					.getElementsByTagName("Button");
			Element button = null;
			for (int i = 0; i < buttons.getLength(); i++) {
				Element candidate = (Element) buttons.item(i);
				if (candidate.getAttribute("fx:id").equals("breakUpButton"))
					button = candidate;
			}
			assertNotNull(button);
			assertEquals("true", button.getAttribute("disable"));
			assertEquals("#onBreakUp", button.getAttribute("onAction"));
			assertNotNull(MainController.class.getDeclaredMethod("onBreakUp", javafx.event.ActionEvent.class));
			assertNotNull(MainController.class.getDeclaredField("breakUpButton"));
			assertEquals("%BreakUp", ((Element) button.getElementsByTagName("Tooltip").item(0)).getAttribute("text"));
			assertEquals("break-up-image-view",
					((Element) button.getElementsByTagName("ImageView").item(0)).getAttribute("styleClass"));
			Node next = button.getNextSibling();
			while (next != null && next.getNodeType() != Node.ELEMENT_NODE)
				next = next.getNextSibling();
			assertNotNull(next);
			assertEquals("simplifyButton", ((Element) next).getAttribute("fx:id"));
			Element parent = (Element) button.getParentNode().getParentNode();
			assertEquals("advancedButtons", parent.getAttribute("fx:id"));
		}
		try (InputStream input = getClass().getResourceAsStream("light/breakUp.png")) {
			assertNotNull(input);
			java.awt.image.BufferedImage icon = javax.imageio.ImageIO.read(input);
			assertNotNull(icon);
			assertTrue(icon.getColorModel().hasAlpha());
		}
	}
}
