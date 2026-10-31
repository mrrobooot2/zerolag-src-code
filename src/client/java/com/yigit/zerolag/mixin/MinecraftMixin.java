package com.yigit.zerolag.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.SDLEventHandler;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.yigit.zerolag.FramePacer;
import com.yigit.zerolag.InstantDispatch;
import com.yigit.zerolag.SurfaceStatus;
import com.yigit.zerolag.ZeroLagClient;
import com.yigit.zerolag.ZeroLagConfig;
import com.yigit.zerolag.access.InstantActionHost;
import com.yigit.zerolag.access.LateInputPoller;
import java.util.Collection;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin implements InstantActionHost {
	@Shadow
	@Final
	private SDLEventHandler sdlEventHandler;

	@Shadow
	@Final
	public Gui gui;

	@Shadow
	@Final
	public Options options;

	@Shadow
	@Final
	public MouseHandler mouseHandler;

	@Shadow
	public @Nullable LocalPlayer player;

	@Shadow
	public @Nullable ClientLevel level;

	@Shadow
	public @Nullable MultiPlayerGameMode gameMode;

	@Shadow
	private volatile boolean pause;

	@Shadow
	private void handleKeybinds() {
	}

	@Inject(
		method = "run",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;pollEvents(Lcom/mojang/blaze3d/platform/SDLEventHandler;)V")
	)
	private void zerolag$paceBeforeInput(final CallbackInfo info) {
		FramePacer.beforeInputPoll();
	}

	@WrapOperation(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"))
	private void zerolag$timeTick(final Minecraft minecraft, final Operation<Void> original) {
		long start = System.nanoTime();
		original.call(minecraft);
		FramePacer.recordTick(System.nanoTime() - start);
	}

	@Inject(
		method = "renderFrame",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;update(Lnet/minecraft/client/DeltaTracker;)V")
	)
	private void zerolag$readInputLate(final boolean advanceGameTime, final CallbackInfo info) {
		if (!ZeroLagClient.config().lateInputActive() || this.player == null || this.gui.screen() != null || this.gui.overlay() != null) {
			return;
		}

		// The SDL handlers pass each event through Minecraft#execute, which only runs it immediately outside a task.
		if (((ReentrantBlockableEventLoopAccessor) this).zerolag$runningTask()) {
			return;
		}

		((LateInputPoller) this.sdlEventHandler).zerolag$pollInputEvents();
		this.mouseHandler.handleAccumulatedMovement();
	}

	@Inject(
		method = "renderFrame",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;pick(F)V", shift = At.Shift.AFTER)
	)
	private void zerolag$actOnPressFrame(final boolean advanceGameTime, final CallbackInfo info) {
		this.zerolag$runInstantActions();
	}

	@Override
	public void zerolag$runInstantActions() {
		ZeroLagConfig.InstantMode mode = InstantDispatch.modeFor((Minecraft) (Object) this);
		if (mode == ZeroLagConfig.InstantMode.OFF
			|| this.player == null
			|| this.level == null
			|| this.gameMode == null
			|| this.gui.screen() != null
			|| this.gui.overlay() != null
			|| this.pause) {
			return;
		}

		InstantDispatch.run(mode, () -> this.handleKeybinds());
	}

	@WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z"))
	private boolean zerolag$routeClick(final KeyMapping mapping, final Operation<Boolean> original) {
		if (!InstantDispatch.isDispatching()) {
			return original.call(mapping);
		}

		if (InstantDispatch.defersCombat() && (mapping == this.options.keyAttack || mapping == this.options.keyUse || mapping == this.options.keyPickItem)) {
			return false;
		}

		boolean clicked = original.call(mapping);
		if (clicked) {
			InstantDispatch.countClick();
		}

		return clicked;
	}

	@WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;handleKeybinds()V"))
	private void zerolag$keepChatKeysOnTick(final Gui gui, final Operation<Void> original) {
		// A key's text event can arrive after the key event; chat opened before the tick would receive that character.
		if (!InstantDispatch.isDispatching()) {
			original.call(gui);
		}
	}

	@WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;continueAttack(Z)V"))
	private void zerolag$keepMiningOnTick(final Minecraft minecraft, final boolean down, final Operation<Void> original) {
		// Mining progress is added per call; outside the tick it would break blocks faster than vanilla.
		if (!InstantDispatch.isDispatching()) {
			original.call(minecraft, down);
		}
	}

	@WrapOperation(
		method = "handleKeybinds",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;startUseItem()V", ordinal = 1)
	)
	private void zerolag$keepHeldUseOnTick(final Minecraft minecraft, final Operation<Void> original) {
		// Ordinal 1 repeats use while the button is held; the press itself is ordinal 0.
		if (!InstantDispatch.isDispatching()) {
			original.call(minecraft);
		}
	}

	@WrapOperation(
		method = "handleKeybinds",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V"
		)
	)
	private void zerolag$releaseOnFrameOnlyInFull(final MultiPlayerGameMode gameMode, final Player player, final Operation<Void> original) {
		if (!InstantDispatch.defersCombat()) {
			original.call(gameMode, player);
		}
	}

	@WrapOperation(
		method = "renderFrame",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/renderpearl/api/device/GpuSurface$PresentMode;getSupportedVsyncMode(Ljava/util/Collection;Z)Lcom/mojang/renderpearl/api/device/GpuSurface$PresentMode;"
		)
	)
	private GpuSurface.PresentMode zerolag$choosePresentMode(
		final Collection<GpuSurface.PresentMode> supported, final boolean vsync, final Operation<GpuSurface.PresentMode> original
	) {
		GpuSurface.PresentMode mode = vsync
				&& ZeroLagClient.config().effectiveVsyncMode() == ZeroLagConfig.VsyncMode.MAILBOX
				&& supported.contains(GpuSurface.PresentMode.MAILBOX)
			? GpuSurface.PresentMode.MAILBOX
			: original.call(supported, vsync);
		SurfaceStatus.presentMode(mode);
		return mode;
	}

	@WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;submit()V"))
	private void zerolag$fenceFrame(final CommandEncoder encoder, final Operation<Void> original) {
		FramePacer.beforeSubmit(encoder);
		original.call(encoder);
	}

	@Inject(
		method = "renderFrame",
		at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuSurface;present()V", shift = At.Shift.AFTER)
	)
	private void zerolag$recordInputAge(final boolean advanceGameTime, final CallbackInfo info) {
		FramePacer.afterPresent();
	}

	@Inject(method = "renderFrame", at = @At("RETURN"))
	private void zerolag$finishFrame(final boolean advanceGameTime, final CallbackInfo info) {
		FramePacer.afterFrame();
	}
}
