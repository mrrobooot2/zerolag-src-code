package com.yigit.zerolag.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.yigit.zerolag.FramePacer;
import com.yigit.zerolag.SurfaceStatus;
import com.yigit.zerolag.ZeroLagClient;
import com.yigit.zerolag.ZeroLagConfig;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

public final class ZeroLagScreen extends OptionsSubScreen {
	private static final int STATUS_WIDTH = 310;
	private static final int STATUS_HEIGHT = 12;

	private @Nullable StringWidget surfaceLine;
	private @Nullable StringWidget inputAgeLine;
	private @Nullable StringWidget pacingLine;

	public ZeroLagScreen(final @Nullable Screen lastScreen) {
		super(lastScreen, Minecraft.getInstance().options, Component.translatable("zerolag.title"));
	}

	@Override
	protected void addOptions() {
		ZeroLagConfig config = ZeroLagClient.config();
		this.list.addBig(toggle("enabled", config.enabled, value -> {
			config.enabled = value;
			ZeroLagClient.surfaceSettingsChanged();
		}));

		this.list.addHeader(Component.translatable("zerolag.header.frame"));
		this.list.addBig(choice("frame_pacing", ZeroLagConfig.FramePacing.values(), config.framePacing, value -> {
			config.framePacing = value;
			FramePacer.reset();
		}));
		this.list.addBig(choice("vsync_mode", ZeroLagConfig.VsyncMode.values(), config.vsyncMode, value -> {
			config.vsyncMode = value;
			ZeroLagClient.surfaceSettingsChanged();
		}));

		this.list.addHeader(Component.translatable("zerolag.header.input"));
		this.list.addSmall(
			toggle("late_input", config.lateInput, value -> config.lateInput = value),
			toggle("tap_buffer", config.tapBuffer, value -> config.tapBuffer = value)
		);
		this.list.addBig(choice("instant_actions", ZeroLagConfig.InstantMode.values(), config.instantActions, value -> config.instantActions = value));
		this.list.addBig(toggle("instant_combat_on_servers", config.instantCombatOnServers, value -> config.instantCombatOnServers = value));

		this.list.addHeader(Component.translatable("zerolag.header.status"));
		this.surfaceLine = new StringWidget(STATUS_WIDTH, STATUS_HEIGHT, Component.empty(), this.font);
		this.inputAgeLine = new StringWidget(STATUS_WIDTH, STATUS_HEIGHT, Component.empty(), this.font);
		this.pacingLine = new StringWidget(STATUS_WIDTH, STATUS_HEIGHT, Component.empty(), this.font);
		this.list.addBig(this.surfaceLine);
		this.list.addBig(this.inputAgeLine);
		this.list.addBig(this.pacingLine);
		this.updateStatus();
	}

	@Override
	public void tick() {
		super.tick();
		this.updateStatus();
	}

	@Override
	public void removed() {
		super.removed();
		ZeroLagClient.saveConfig();
	}

	private void updateStatus() {
		if (this.surfaceLine == null || this.inputAgeLine == null || this.pacingLine == null) {
			return;
		}

		this.surfaceLine.setMessage(describeSurface());
		this.inputAgeLine.setMessage(Component.translatable("zerolag.status.input_age", millis(FramePacer.averageInputAgeMillis())));
		this.pacingLine.setMessage(
			ZeroLagClient.config().enabled
				? Component.translatable("zerolag.status.pacing", millis(FramePacer.averageSleepMillis()), millis(FramePacer.averageWaitMillis()))
				: Component.translatable("zerolag.status.disabled")
		);
	}

	private static Component describeSurface() {
		String backend = RenderSystem.getDevice().getDeviceInfo().backendName();
		GpuSurface.PresentMode mode = SurfaceStatus.presentMode();
		Component present = mode == null
			? Component.literal("?")
			: Component.translatable("zerolag.status.present." + mode.name().toLowerCase(Locale.ROOT));
		int images = SurfaceStatus.swapchainImages();
		return images > 0
			? Component.translatable("zerolag.status.surface.images", backend, present, images)
			: Component.translatable("zerolag.status.surface", backend, present);
	}

	private static String millis(final double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	private static OptionInstance<Boolean> toggle(final String name, final boolean value, final Consumer<Boolean> onChange) {
		String key = "zerolag.option." + name;
		return OptionInstance.createBoolean(key, OptionInstance.cachedConstantTooltip(Component.translatable(key + ".tooltip")), value, onChange::accept);
	}

	private static <T extends Enum<T> & StringRepresentable> OptionInstance<T> choice(
		final String name, final T[] values, final T value, final Consumer<T> onChange
	) {
		String key = "zerolag.option." + name;
		return new OptionInstance<>(
			key,
			choice -> Tooltip.create(Component.translatable(key + "." + choice.getSerializedName() + ".tooltip")),
			(caption, choice) -> Component.translatable(key + "." + choice.getSerializedName()),
			new OptionInstance.Enum<>(List.of(values), StringRepresentable.fromEnum(() -> values)),
			value,
			onChange::accept
		);
	}
}
