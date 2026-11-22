package com.commonwealthrobotics;

import java.util.function.BooleanSupplier;

import com.neuronrobotics.bowlerstudio.SplashManager;
import com.neuronrobotics.bowlerstudio.PsudoSplash;

import javafx.stage.Stage;

/** Temporarily suspends splash visibility while a modal dialog owns focus. */
final class DialogSplashState implements AutoCloseable {
	private final BooleanSupplier openPreventer = SplashManager.getOpenPreventer();
	private final BooleanSupplier closePreventer = SplashManager.getClosePreventer();
	private final boolean wasVisible;
	private final Runnable show;

	static DialogSplashState suspend() {
		if (!PsudoSplash.isVisibleSplash())
			return new DialogSplashState(false, () -> {
			}, () -> {
			});
		Stage splash = PsudoSplash.get().getPopupStage();
		return new DialogSplashState(true, splash::hide, splash::show);
	}

	DialogSplashState(boolean wasVisible, Runnable hide, Runnable show) {
		this.wasVisible = wasVisible;
		this.show = show;
		SplashManager.setOpenPreventer(() -> true);
		SplashManager.setClosePreventer(() -> true);
		if (wasVisible)
			hide.run();
	}

	@Override
	public void close() {
		SplashManager.setOpenPreventer(openPreventer);
		SplashManager.setClosePreventer(closePreventer);
		if (wasVisible)
			show.run();
	}
}
