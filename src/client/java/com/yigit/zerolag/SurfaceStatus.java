package com.yigit.zerolag;

import com.mojang.renderpearl.api.device.GpuSurface;
import org.jspecify.annotations.Nullable;

public final class SurfaceStatus {
	private static GpuSurface.@Nullable PresentMode presentMode;
	private static int swapchainImages;

	private SurfaceStatus() {
	}

	public static void presentMode(final GpuSurface.PresentMode mode) {
		presentMode = mode;
	}

	public static void swapchainImages(final int count) {
		swapchainImages = count;
	}

	public static GpuSurface.@Nullable PresentMode presentMode() {
		return presentMode;
	}

	/**
	 * @return the swapchain image count, or 0 when the backend has no swapchain of its own (OpenGL)
	 */
	public static int swapchainImages() {
		return swapchainImages;
	}
}
