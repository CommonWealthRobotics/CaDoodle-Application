package com.commonwealthrobotics;

import static org.junit.jupiter.api.Assertions.*;

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

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class SimplifyResourcesTest {
	@Test
	void everyLanguageHasTooltipAndOperationLabel() throws Exception {
		try (Stream<Path> files = Files.list(Paths.get("src/main/resources/lang"))) {
			List<Path> bundles = files.filter(p -> p.getFileName().toString().startsWith("Messages"))
					.filter(p -> p.toString().endsWith(".properties")).collect(Collectors.toList());
			assertEquals(61, bundles.size());
			for (Path path : bundles) {
				try (InputStream input = Files.newInputStream(path)) {
					PropertyResourceBundle bundle = new PropertyResourceBundle(
							new InputStreamReader(input, StandardCharsets.UTF_8));
					for (String key : Arrays.asList("Simplify", "SimplifyMesh")) {
						assertFalse(bundle.getString(key).codePoints().allMatch(Character::isWhitespace),
								path + ": " + key);
						assertNotEquals(key, bundle.getString(key));
					}
				}
			}
		}
	}

	@Test
	void simplifyPrecedesConversionsWithSeparatorAndUsesBundledGraphic() throws Exception {
		try (InputStream input = getClass().getResourceAsStream("MainWindow.fxml")) {
			NodeList buttons = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)
					.getElementsByTagName("Button");
			Element simplify = null;
			for (int i = 0; i < buttons.getLength(); i++) {
				Element button = (Element) buttons.item(i);
				if (button.getAttribute("fx:id").equals("simplifyButton"))
					simplify = button;
			}
			assertNotNull(simplify);
			assertEquals("#onSimplify", simplify.getAttribute("onAction"));
			Element tooltip = (Element) simplify.getElementsByTagName("Tooltip").item(0);
			assertEquals("%SimplifyMesh", tooltip.getAttribute("text"));
			Node separator = nextElement(simplify);
			assertEquals("Separator", separator.getNodeName());
			assertEquals("VERTICAL", ((Element) separator).getAttribute("orientation"));
			assertEquals("convertToBlenderButton", ((Element) nextElement(separator)).getAttribute("fx:id"));
		}
		try (InputStream icon = getClass().getResourceAsStream("light/simplify.png")) {
			assertNotNull(icon);
			assertNotNull(javax.imageio.ImageIO.read(icon));
		}
	}

	private Node nextElement(Node node) {
		Node next = node.getNextSibling();
		while (next != null && next.getNodeType() != Node.ELEMENT_NODE)
			next = next.getNextSibling();
		assertNotNull(next);
		return next;
	}
}
