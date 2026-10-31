package com.yigit.zerolag.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.yigit.zerolag.ZeroLagClient;
import com.yigit.zerolag.access.TapBufferedKey;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
	private boolean zerolag$keepQuickTaps(final KeyMapping mapping, final Operation<Boolean> original) {
		boolean tapped = ((TapBufferedKey) mapping).zerolag$consumeTap();
		return original.call(mapping) || tapped && ZeroLagClient.config().tapBufferActive();
	}
}
