package com.yigit.zerolag.mixin;

import com.mojang.renderpearl.backend.opengl.GlSurface;
import com.yigit.zerolag.FramePacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlSurface.class)
public abstract class GlSurfaceMixin {
	// A swap that doesn't block still flushes the driver's command buffer.
	@Unique
	private static final long SWAP_WORK_NS = 500_000L;

	@Inject(method = "present", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLVideo;SDL_GL_SwapWindow(J)Z"))
	private void zerolag$beforeSwap(final CallbackInfo info) {
		FramePacer.beginWait();
	}

	@Inject(method = "present", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLVideo;SDL_GL_SwapWindow(J)Z", shift = At.Shift.AFTER))
	private void zerolag$afterSwap(final CallbackInfo info) {
		FramePacer.endWait(SWAP_WORK_NS);
	}
}
