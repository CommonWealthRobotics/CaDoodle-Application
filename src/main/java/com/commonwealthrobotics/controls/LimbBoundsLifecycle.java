package com.commonwealthrobotics.controls;

import java.util.OptionalLong;

/** Revisions prevent queued previews from outliving a limb move. */
final class LimbBoundsLifecycle {
	private enum State {
		READY, MOVING, REFRESHING
	}
	private State state = State.READY;
	private long revision;

	synchronized void begin() {
		if (state != State.MOVING) {
			state = State.MOVING;
			revision++;
		}
	}

	synchronized OptionalLong finish() {
		if (state != State.MOVING)
			return OptionalLong.empty();
		state = State.REFRESHING;
		return OptionalLong.of(++revision);
	}

	synchronized boolean active() {
		return state != State.READY;
	}

	synchronized long revision() {
		return revision;
	}

	synchronized OptionalLong preview() {
		return state == State.READY ? OptionalLong.of(revision) : OptionalLong.empty();
	}

	synchronized boolean accepts(long token, boolean finalRefresh) {
		return token == revision && state == (finalRefresh ? State.REFRESHING : State.READY);
	}

	synchronized boolean abandon(long token) {
		if (!complete(token))
			return false;
		revision++;
		return true;
	}

	synchronized boolean complete(long token) {
		if (!accepts(token, true))
			return false;
		state = State.READY;
		return true;
	}
}
