package com.yigit.zerolag;

import java.util.concurrent.locks.LockSupport;

final class PreciseSleep {
	private static final long SPIN_NS = 200_000L;
	private static final long MAX_OVERSHOOT_NS = 2_000_000L;
	private static long averageOvershootNs;

	private PreciseSleep() {
	}

	static void sleep(final long durationNs) {
		long deadline = System.nanoTime() + durationNs;
		long remaining;
		while ((remaining = deadline - System.nanoTime()) > 0L) {
			long parkNs = remaining - averageOvershootNs - SPIN_NS;
			if (parkNs > 0L) {
				long start = System.nanoTime();
				LockSupport.parkNanos(parkNs);
				long overshoot = System.nanoTime() - start - parkNs;
				if (overshoot > 0L) {
					averageOvershootNs = Math.min(MAX_OVERSHOOT_NS, (averageOvershootNs * 9L + overshoot) / 10L);
				}
			} else {
				Thread.onSpinWait();
			}
		}
	}
}
