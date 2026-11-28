package com.commonwealthrobotics.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.neuronrobotics.bowlerstudio.scripting.PasswordManager;

class OfflineHttpTestSessionTest {
	@ParameterizedTest
	@ValueSource(strings = {"http", "https"})
	void networkConnectionsAreRejectedBeforeAnyIo(String protocol) throws Exception {
		URL url = new URL(protocol + "://example.invalid/test");
		assertThrows(OfflineHttpTestSession.HttpDisabledException.class, url::openConnection);
		assertThrows(OfflineHttpTestSession.HttpDisabledException.class, () -> url.openConnection(Proxy.NO_PROXY));
	}

	@Test
	void libraryConnectivityProbeSeesOfflineInsteadOfBlocking() {
		assertFalse(PasswordManager.hasNetwork());
		PasswordManager.checkInternet();
		assertFalse(PasswordManager.hasNetwork());
	}

	@Test
	void localResourcesRemainReadable(@TempDir Path directory) throws Exception {
		Path file = directory.resolve("resource.txt");
		Files.write(file, new byte[]{42});
		try (InputStream input = file.toUri().toURL().openStream()) {
			assertEquals(42, input.read());
			assertEquals(-1, input.read());
		}
	}
}
