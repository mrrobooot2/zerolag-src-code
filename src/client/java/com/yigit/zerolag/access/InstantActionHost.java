package com.yigit.zerolag.access;

public interface InstantActionHost {
	/**
	 * Runs the presses waiting in the key mappings now instead of on the next tick, if the current mode allows it.
	 */
	void zerolag$runInstantActions();
}
