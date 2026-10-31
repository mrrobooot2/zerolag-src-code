package com.yigit.zerolag;

import com.yigit.zerolag.mixin.DeltaTrackerTimerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

final class TickForecast {
	private TickForecast() {
	}

	/**
	 * @return how many client ticks the next frame will run if it starts {@code delayNs} from now, using the same
	 * arithmetic as {@code DeltaTracker.Timer#advanceGameTime}
	 */
	static int ticksDueAfter(final long delayNs) {
		if (!(Minecraft.getInstance().getDeltaTracker() instanceof DeltaTrackerTimerAccessor timer)) {
			return 0;
		}

		float msPerTick = timer.zerolag$targetMsptProvider().apply(timer.zerolag$msPerTick());
		long startMs = Util.getMillis() + delayNs / 1_000_000L;
		float residual = timer.zerolag$deltaTickResidual() + (startMs - timer.zerolag$lastMs()) / msPerTick;
		return Math.min(10, (int) residual);
	}
}
