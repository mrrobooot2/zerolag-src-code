package com.yigit.zerolag.mixin;

import com.yigit.zerolag.access.TapBufferedKey;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin implements TapBufferedKey {
	// Long enough to survive a lag spike. Opening any screen releases all keys, which clears older presses anyway.
	@Unique
	private static final long TAP_WINDOW_NS = 1_000_000_000L;

	@Shadow
	@Final
	private static Map<String, KeyMapping> ALL;

	@Unique
	private long zerolag$pressedAt;

	@Inject(method = "setDown", at = @At("HEAD"))
	private void zerolag$rememberPress(final boolean down, final CallbackInfo info) {
		if (down) {
			this.zerolag$pressedAt = System.nanoTime();
		}
	}

	@Inject(method = "releaseAll", at = @At("TAIL"))
	private static void zerolag$forgetPresses(final CallbackInfo info) {
		for (KeyMapping mapping : ALL.values()) {
			((TapBufferedKey) mapping).zerolag$clearTap();
		}
	}

	@Override
	public boolean zerolag$consumeTap() {
		long pressedAt = this.zerolag$pressedAt;
		this.zerolag$pressedAt = 0L;
		return pressedAt != 0L && System.nanoTime() - pressedAt < TAP_WINDOW_NS;
	}

	@Override
	public void zerolag$clearTap() {
		this.zerolag$pressedAt = 0L;
	}
}
