package com.yigit.zerolag.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.yigit.zerolag.FramePacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderMixin {
	@Inject(
		method = "submit",
		at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanCommandEncoder;awaitSubmitCompletion(JJ)Z")
	)
	private void zerolag$beforeSubmitWait(final CallbackInfo info) {
		FramePacer.beginWait();
	}

	@Inject(
		method = "submit",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanCommandEncoder;awaitSubmitCompletion(JJ)Z",
			shift = At.Shift.AFTER
		)
	)
	private void zerolag$afterSubmitWait(final CallbackInfo info) {
		FramePacer.endWait(0L);
	}
}
