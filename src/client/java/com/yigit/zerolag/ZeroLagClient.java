package com.yigit.zerolag;

import com.mojang.blaze3d.platform.InputConstants;
import com.yigit.zerolag.screen.ZeroLagScreen;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ZeroLagClient implements ClientModInitializer {
	public static final String MOD_ID = "zerolag";
	public static final Logger LOGGER = LoggerFactory.getLogger("ZeroLag");

	private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
	private static final KeyMapping OPEN_SETTINGS = new KeyMapping("key.zerolag.settings", InputConstants.KEY_F8, KEY_CATEGORY);
	private static final KeyMapping TOGGLE = new KeyMapping("key.zerolag.toggle", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);

	private static ZeroLagConfig config = new ZeroLagConfig();

	public static ZeroLagConfig config() {
		return config;
	}

	public static void saveConfig() {
		try {
			config.save(configPath());
		} catch (Exception exception) {
			LOGGER.error("Couldn't save {}", configPath(), exception);
		}
	}

	public static void surfaceSettingsChanged() {
		FramePacer.reset();
		Minecraft.getInstance().invalidateSurfaceConfiguration();
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
	}

	@Override
	public void onInitializeClient() {
		try {
			config = ZeroLagConfig.load(configPath());
		} catch (Exception exception) {
			LOGGER.error("Couldn't read {}, using defaults", configPath(), exception);
			config = new ZeroLagConfig();
		}

		KeyMappingHelper.registerKeyMapping(OPEN_SETTINGS);
		KeyMappingHelper.registerKeyMapping(TOGGLE);
		ClientTickEvents.END_CLIENT_TICK.register(ZeroLagClient::handleKeys);
	}

	private static void handleKeys(final Minecraft minecraft) {
		while (OPEN_SETTINGS.consumeClick()) {
			minecraft.gui.setScreen(new ZeroLagScreen(minecraft.gui.screen()));
		}

		while (TOGGLE.consumeClick()) {
			config.enabled = !config.enabled;
			saveConfig();
			surfaceSettingsChanged();
			minecraft.gui.hud.setOverlayMessage(Component.translatable(config.enabled ? "zerolag.toggled.on" : "zerolag.toggled.off"), false);
		}
	}
}
