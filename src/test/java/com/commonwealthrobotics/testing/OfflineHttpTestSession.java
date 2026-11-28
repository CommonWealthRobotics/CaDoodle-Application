package com.commonwealthrobotics.testing;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/** Install before test discovery; library static initializers can probe the network. */
public final class OfflineHttpTestSession implements LauncherSessionListener {
	private static final AtomicBoolean installed = new AtomicBoolean();

	@Override
	public void launcherSessionOpened(LauncherSession session) {
		if (installed.compareAndSet(false, true)) {
			URL.setURLStreamHandlerFactory(protocol -> {
				if (!"http".equals(protocol) && !"https".equals(protocol))
					return null;
				return new URLStreamHandler() {
					@Override
					protected URLConnection openConnection(URL url) throws IOException {
						throw new HttpDisabledException();
					}

					@Override
					protected URLConnection openConnection(URL url, Proxy proxy) throws IOException {
						throw new HttpDisabledException();
					}
				};
			});
		}
	}

	static final class HttpDisabledException extends IOException {
		private static final long serialVersionUID = 1L;

		HttpDisabledException() {
			super("HTTP(S) access is disabled in unit tests");
		}
	}
}
