package com.yigit.zerolag;

import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

/**
 * State for running {@code Minecraft#handleKeybinds} on the frame of a press instead of waiting for the next tick.
 * While a dispatch is running, the parts of that method that must stay once per tick are skipped by
 * {@code MinecraftMixin}.
 */
public final class InstantDispatch {
	private static ZeroLagConfig.@Nullable InstantMode dispatching;
	private static long handledClicks;

	private InstantDispatch() {
	}

	public static ZeroLagConfig.InstantMode modeFor(final Minecraft minecraft) {
		return ZeroLagClient.config().effectiveInstantActions(!minecraft.hasSingleplayerServer());
	}

	public static void run(final ZeroLagConfig.InstantMode mode, final Runnable handleKeybinds) {
		dispatching = mode;
		try {
			handleKeybinds.run();
		} finally {
			dispatching = null;
		}
	}

	public static boolean isDispatching() {
		return dispatching != null;
	}

	/**
	 * In SAFE mode attack, use and pick block are left for the tick.
	 */
	public static boolean defersCombat() {
		return dispatching == ZeroLagConfig.InstantMode.SAFE;
	}

	public static void countClick() {
		handledClicks++;
	}

	public static long handledClicks() {
		return handledClicks;
	}
}
