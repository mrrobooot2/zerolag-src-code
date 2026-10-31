package com.yigit.zerolag.access;

public interface TapBufferedKey {
	/**
	 * @return whether the key went down since the previous call, even if it has been released again
	 */
	boolean zerolag$consumeTap();

	void zerolag$clearTap();
}
