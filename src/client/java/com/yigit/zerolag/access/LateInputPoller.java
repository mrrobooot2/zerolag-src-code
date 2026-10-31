package com.yigit.zerolag.access;

public interface LateInputPoller {
	/**
	 * Handles queued keyboard and mouse events right away, leaving window and device events for the regular poll at
	 * the start of the next frame.
	 */
	void zerolag$pollInputEvents();
}
