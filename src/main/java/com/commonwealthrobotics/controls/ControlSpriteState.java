package com.commonwealthrobotics.controls;

final class ControlSpriteState {
	private volatile SpriteDisplayMode mode = SpriteDisplayMode.Default;

	SpriteDisplayMode getMode() {
		return mode;
	}

	synchronized boolean setMode(SpriteDisplayMode requested) {
		if (mode == SpriteDisplayMode.Align || mode == requested)
			return false;
		mode = requested;
		return true;
	}

	synchronized boolean disableAlign() {
		if (mode != SpriteDisplayMode.Align)
			return false;
		mode = SpriteDisplayMode.Default;
		return true;
	}

	synchronized boolean selectionChanged(boolean canAlign) {
		return !canAlign && disableAlign();
	}
}
