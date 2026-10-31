package com.yigit.zerolag;

import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.GpuFence;
import org.jspecify.annotations.Nullable;

/**
 * Moves the time a frame spends blocked on the GPU or the display to before the next frame reads its input.
 *
 * <p>Vanilla lets the CPU run up to two submits ahead and blocks inside {@code submit()}, the swap and the swapchain
 * acquire. All of that happens after the frame has already read the mouse and keyboard, so the input that ends up on
 * screen is older than it has to be. In SMART mode the measured blocking is turned into a sleep before input is
 * polled, keeping just enough of it to stay ahead of the GPU. In STRICT mode every frame waits for the GPU to finish
 * before the next one starts.
 *
 * <p>Everything here runs on the render thread.
 */
public final class FramePacer {
	private static final long INITIAL_MARGIN_NS = 1_000_000L;
	private static final long MIN_MARGIN_NS = 500_000L;
	private static final long MAX_MARGIN_NS = 4_000_000L;
	// Raising the margin after a late frame and lowering it this slowly settles at about one late frame in 500.
	private static final long MARGIN_STEP_NS = 250_000L;
	private static final long MARGIN_DECAY_NS = 500L;
	private static final long LATE_WAIT_NS = 100_000L;
	private static final long MAX_SLEEP_NS = 25_000_000L;
	private static final long DISCARD_WAIT_NS = 100_000_000L;
	private static final long STRICT_TIMEOUT_NS = 1_000_000_000L;
	private static final long MAX_INPUT_AGE_NS = 250_000_000L;
	private static final double AVERAGE_WEIGHT = 0.05;
	private static final double TICK_AVERAGE_WEIGHT = 0.1;

	private static long waitStartNs;
	private static long frameWaitNs;
	private static long sleepNs;
	private static long sleptNs;
	private static long marginNs = INITIAL_MARGIN_NS;
	private static long inputPolledNs;
	private static @Nullable GpuFence strictFence;
	private static double averageWaitNs;
	private static double averageSleepNs;
	private static double averageInputAgeNs;
	private static double averageTickNs;

	private FramePacer() {
	}

	public static void beginWait() {
		waitStartNs = System.nanoTime();
	}

	/**
	 * @param allowanceNs time the call takes even when it does not block, so its normal CPU work is not counted
	 */
	public static void endWait(final long allowanceNs) {
		if (waitStartNs != 0L) {
			frameWaitNs += Math.max(0L, System.nanoTime() - waitStartNs - allowanceNs);
			waitStartNs = 0L;
		}
	}

	public static void beforeInputPoll() {
		long waited = frameWaitNs;
		frameWaitNs = 0L;
		averageWaitNs += (waited - averageWaitNs) * AVERAGE_WEIGHT;

		if (ZeroLagClient.config().effectiveFramePacing() != ZeroLagConfig.FramePacing.SMART || waited > DISCARD_WAIT_NS) {
			sleepNs = 0L;
		} else {
			// Having slept and then not waited at all means the GPU or the display sat idle for this frame.
			if (sleptNs > 0L && waited < LATE_WAIT_NS) {
				marginNs = Math.min(MAX_MARGIN_NS, marginNs + MARGIN_STEP_NS);
			} else {
				marginNs = Math.max(MIN_MARGIN_NS, marginNs - MARGIN_DECAY_NS);
			}

			long error = waited - marginNs;
			// Back off faster than we creep up, so a frame that starves the GPU costs at most a couple of frames.
			sleepNs += error > 0L ? error * 3L / 10L : error * 6L / 10L - sleepNs / 20L;
			sleepNs = Math.clamp(sleepNs, 0L, MAX_SLEEP_NS);
		}

		sleptNs = 0L;
		if (sleepNs > 0L) {
			// A frame that runs a client tick does that much more work before it can submit, so it has to wake earlier.
			int ticks = TickForecast.ticksDueAfter(sleepNs);
			long sleep = Math.max(0L, sleepNs - ticks * (long) averageTickNs);
			if (sleep > 0L) {
				PreciseSleep.sleep(sleep);
				sleptNs = sleep;
			}
		}

		averageSleepNs += (sleptNs - averageSleepNs) * AVERAGE_WEIGHT;
		inputPolledNs = System.nanoTime();
	}

	public static void recordTick(final long durationNs) {
		averageTickNs += (durationNs - averageTickNs) * TICK_AVERAGE_WEIGHT;
	}

	/**
	 * Records how old this frame's input was once the frame was handed to the display.
	 */
	public static void afterPresent() {
		if (inputPolledNs == 0L) {
			return;
		}

		long age = System.nanoTime() - inputPolledNs;
		inputPolledNs = 0L;
		if (age < MAX_INPUT_AGE_NS) {
			averageInputAgeNs += (age - averageInputAgeNs) * AVERAGE_WEIGHT;
		}
	}

	public static void beforeSubmit(final CommandEncoder encoder) {
		closeStrictFence();
		if (ZeroLagClient.config().effectiveFramePacing() == ZeroLagConfig.FramePacing.STRICT) {
			strictFence = encoder.createFence();
		}
	}

	public static void afterFrame() {
		GpuFence fence = strictFence;
		if (fence == null) {
			return;
		}

		strictFence = null;
		long start = System.nanoTime();
		try {
			fence.awaitCompletion(STRICT_TIMEOUT_NS);
		} finally {
			fence.close();
		}

		frameWaitNs += System.nanoTime() - start;
	}

	public static void reset() {
		sleepNs = 0L;
		sleptNs = 0L;
		marginNs = INITIAL_MARGIN_NS;
		closeStrictFence();
	}

	public static double averageSleepMillis() {
		return averageSleepNs / 1_000_000.0;
	}

	public static double averageWaitMillis() {
		return averageWaitNs / 1_000_000.0;
	}

	public static double averageInputAgeMillis() {
		return averageInputAgeNs / 1_000_000.0;
	}

	public static double marginMillis() {
		return marginNs / 1_000_000.0;
	}

	private static void closeStrictFence() {
		if (strictFence != null) {
			strictFence.close();
			strictFence = null;
		}
	}
}
