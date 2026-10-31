package com.yigit.zerolag.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuSurface;
import com.yigit.zerolag.FramePacer;
import com.yigit.zerolag.SurfaceStatus;
import com.yigit.zerolag.ZeroLagClient;
import com.yigit.zerolag.ZeroLagConfig;
import it.unimi.dsi.fastutil.longs.LongList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanGpuSurface.class)
public abstract class VulkanGpuSurfaceMixin {
	@Unique
	private static final long CALL_WORK_NS = 500_000L;
	@Unique
	private static final int DOUBLE_BUFFERED = 2;

	@Shadow
	@Final
	private LongList swapchainImages;

	@WrapOperation(method = "configure", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"))
	private int zerolag$chooseImageCount(
		final int requested, final int surfaceMinimum, final Operation<Integer> original, @Local(argsOnly = true) final GpuSurface.Configuration config
	) {
		// With FIFO every extra image is one more refresh between the frame being drawn and it reaching the screen.
		if (config.presentMode() == GpuSurface.PresentMode.FIFO && ZeroLagClient.config().effectiveVsyncMode() != ZeroLagConfig.VsyncMode.VANILLA) {
			return original.call(DOUBLE_BUFFERED, surfaceMinimum);
		}

		return original.call(requested, surfaceMinimum);
	}

	@Inject(method = "configure", at = @At("RETURN"))
	private void zerolag$recordImageCount(final GpuSurface.Configuration config, final CallbackInfo info) {
		SurfaceStatus.swapchainImages(this.swapchainImages.size());
	}

	@Inject(
		method = "acquireNextTexture",
		at = @At(
			value = "INVOKE",
			target = "Lorg/lwjgl/vulkan/KHRSwapchain;vkAcquireNextImageKHR(Lorg/lwjgl/vulkan/VkDevice;JJJJLjava/nio/IntBuffer;)I"
		)
	)
	private void zerolag$beforeAcquire(final CallbackInfo info) {
		FramePacer.beginWait();
	}

	@Inject(
		method = "acquireNextTexture",
		at = @At(
			value = "INVOKE",
			target = "Lorg/lwjgl/vulkan/KHRSwapchain;vkAcquireNextImageKHR(Lorg/lwjgl/vulkan/VkDevice;JJJJLjava/nio/IntBuffer;)I",
			shift = At.Shift.AFTER
		)
	)
	private void zerolag$afterAcquire(final CallbackInfo info) {
		FramePacer.endWait(CALL_WORK_NS);
	}

	@Inject(
		method = "present",
		at = @At(
			value = "INVOKE",
			target = "Lorg/lwjgl/vulkan/KHRSwapchain;vkQueuePresentKHR(Lorg/lwjgl/vulkan/VkQueue;Lorg/lwjgl/vulkan/VkPresentInfoKHR;)I"
		)
	)
	private void zerolag$beforePresent(final CallbackInfo info) {
		FramePacer.beginWait();
	}

	@Inject(
		method = "present",
		at = @At(
			value = "INVOKE",
			target = "Lorg/lwjgl/vulkan/KHRSwapchain;vkQueuePresentKHR(Lorg/lwjgl/vulkan/VkQueue;Lorg/lwjgl/vulkan/VkPresentInfoKHR;)I",
			shift = At.Shift.AFTER
		)
	)
	private void zerolag$afterPresent(final CallbackInfo info) {
		FramePacer.endWait(CALL_WORK_NS);
	}
}
