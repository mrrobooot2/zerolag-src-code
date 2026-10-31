package com.yigit.zerolag.test;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.yigit.zerolag.FramePacer;
import com.yigit.zerolag.InstantDispatch;
import com.yigit.zerolag.SurfaceStatus;
import com.yigit.zerolag.ZeroLagClient;
import com.yigit.zerolag.ZeroLagConfig;
import com.yigit.zerolag.access.InstantActionHost;
import com.yigit.zerolag.access.LateInputPoller;
import com.yigit.zerolag.mixin.ReentrantBlockableEventLoopAccessor;
import com.yigit.zerolag.screen.ZeroLagScreen;
import com.yigit.zerolag.test.mixin.MinecraftAccessor;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ZeroLagGameTest implements FabricClientGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("ZeroLag test");
	private static final int MEASURE_TICKS = 100;
	private static final String[] BACKEND_CLASSES = {
		"com.mojang.renderpearl.backend.opengl.GlCommandEncoder",
		"com.mojang.renderpearl.backend.opengl.GlSurface",
		"com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder",
		"com.mojang.renderpearl.backend.vulkan.VulkanGpuSurface"
	};

	@Override
	public void runTest(final ClientGameTestContext context) {
		context.runOnClient(client -> {
			ZeroLagConfig defaults = new ZeroLagConfig();
			ZeroLagClient.config().enabled = defaults.enabled;
			ZeroLagClient.config().framePacing = defaults.framePacing;
			ZeroLagClient.config().instantActions = defaults.instantActions;
			ZeroLagClient.config().tapBuffer = defaults.tapBuffer;
			LOGGER.info("Backend: {}", RenderSystem.getDevice().getDeviceInfo().backendName());
		});
		loadBackendClasses();

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(20);
			context.setScreen(() -> null);

			context.runOnClient(ZeroLagGameTest::lateInputTakesOnlyInputEvents);
			context.runOnClient(ZeroLagGameTest::hotbarPressActsBeforeTick);
			context.runOnClient(ZeroLagGameTest::safeModeLeavesAttackForTick);
			context.runOnClient(ZeroLagGameTest::fullModeAttacksOnFrame);
			tapBufferKeepsQuickJump(context);
			comparePacing(context, true);
			comparePacing(context, false);

			// On Vulkan, the test window resize blits past the end of the real swapchain image and hangs the GPU.
			if (context.computeOnClient(client -> !"Vulkan".equals(RenderSystem.getDevice().getDeviceInfo().backendName()))) {
				context.getInput().resizeWindow(1280, 960);
				context.runOnClient(client -> client.options.guiScale().set(2));
			}
			context.setScreen(() -> new ZeroLagScreen(null));
			context.waitTicks(60);
			context.takeScreenshot("zerolag-settings-en");

			context.runOnClient(client -> {
				client.getLanguageManager().setSelected("tr_tr");
				client.reloadResourcePacks();
			});
			context.waitFor(client -> client.gui.overlay() == null);
			context.setScreen(() -> new ZeroLagScreen(null));
			context.waitTicks(40);
			context.takeScreenshot("zerolag-settings-tr");
			context.setScreen(() -> null);
		}
	}

	// Mixins into these only apply once the class loads, which a run on the other backend never does.
	private static void loadBackendClasses() {
		for (String name : BACKEND_CLASSES) {
			try {
				Class.forName(name, true, ZeroLagGameTest.class.getClassLoader());
			} catch (ClassNotFoundException exception) {
				throw new AssertionError("Missing backend class " + name, exception);
			}
		}
	}

	private static void lateInputTakesOnlyInputEvents(final Minecraft client) {
		// Inside a task the SDL handlers defer their work, and the deferred work reads the event after it is freed.
		if (((ReentrantBlockableEventLoopAccessor) client).zerolag$runningTask()) {
			LOGGER.warn("Skipping the late input check: test code runs inside a client task");
			return;
		}

		SDLEvents.SDL_PumpEvents();
		SDLEvents.SDL_FlushEvents(SDLEvents.SDL_EVENT_USER, SDLEvents.SDL_EVENT_USER);
		try (MemoryStack stack = MemoryStack.stackPush()) {
			SDL_Event key = SDL_Event.calloc(stack);
			key.type(SDLEvents.SDL_EVENT_KEY_UP);
			check(SDLEvents.SDL_PushEvent(key), "couldn't queue a key event");
			SDL_Event user = SDL_Event.calloc(stack);
			user.type(SDLEvents.SDL_EVENT_USER);
			check(SDLEvents.SDL_PushEvent(user), "couldn't queue a user event");

			((LateInputPoller) ((MinecraftAccessor) client).zerolagtest$sdlEventHandler()).zerolag$pollInputEvents();

			SDL_Event.Buffer peek = SDL_Event.malloc(8, stack);
			int keysLeft = SDLEvents.SDL_PeepEvents(peek, SDLEvents.SDL_PEEKEVENT, SDLEvents.SDL_EVENT_KEY_DOWN, SDLEvents.SDL_EVENT_KEY_UP);
			int usersLeft = SDLEvents.SDL_PeepEvents(peek, SDLEvents.SDL_PEEKEVENT, SDLEvents.SDL_EVENT_USER, SDLEvents.SDL_EVENT_USER);
			SDLEvents.SDL_FlushEvents(SDLEvents.SDL_EVENT_USER, SDLEvents.SDL_EVENT_USER);
			check(keysLeft == 0, "late input left a key event in the queue");
			check(usersLeft == 1, "late input took an event that is neither keyboard nor mouse");
		}
	}

	private static void hotbarPressActsBeforeTick(final Minecraft client) {
		client.player.getInventory().setSelectedSlot(0);
		KeyMapping.click(KeyMappingHelper.getBoundKeyOf(client.options.keyHotbarSlots[4]));
		((InstantActionHost) client).zerolag$runInstantActions();
		check(client.player.getInventory().getSelectedSlot() == 4, "hotbar press waited for the tick");
	}

	private static void safeModeLeavesAttackForTick(final Minecraft client) {
		ZeroLagClient.config().instantActions = ZeroLagConfig.InstantMode.SAFE;
		try {
			KeyMapping.click(KeyMappingHelper.getBoundKeyOf(client.options.keyAttack));
			((InstantActionHost) client).zerolag$runInstantActions();
			check(client.options.keyAttack.consumeClick(), "safe mode handled an attack outside the tick");
		} finally {
			ZeroLagClient.config().instantActions = ZeroLagConfig.InstantMode.FULL;
		}
	}

	private static void fullModeAttacksOnFrame(final Minecraft client) {
		long before = InstantDispatch.handledClicks();
		KeyMapping.click(KeyMappingHelper.getBoundKeyOf(client.options.keyAttack));
		((InstantActionHost) client).zerolag$runInstantActions();
		check(!client.options.keyAttack.consumeClick(), "full mode left the attack for the tick");
		check(InstantDispatch.handledClicks() > before, "full mode did not count the attack click");
	}

	private static void tapBufferKeepsQuickJump(final ClientGameTestContext context) {
		check(tapJumpBetweenTicks(context), "a jump tap shorter than a tick was dropped");

		context.runOnClient(client -> ZeroLagClient.config().tapBuffer = false);
		try {
			check(!tapJumpBetweenTicks(context), "without the tap buffer the tap should be dropped, so the check above proves nothing");
		} finally {
			context.runOnClient(client -> ZeroLagClient.config().tapBuffer = true);
		}
	}

	private static boolean tapJumpBetweenTicks(final ClientGameTestContext context) {
		context.runOnClient(client -> {
			InputConstants.Key jump = KeyMappingHelper.getBoundKeyOf(client.options.keyJump);
			KeyMapping.set(jump, true);
			KeyMapping.set(jump, false);
		});
		context.waitTick();
		return context.computeOnClient(client -> client.player.input.keyPresses.jump());
	}

	private static void comparePacing(final ClientGameTestContext context, final boolean vsync) {
		context.runOnClient(client -> {
			client.options.enableVsync().set(vsync);
			client.options.framerateLimit().set(260);
		});
		context.waitTicks(20);

		Map<ZeroLagConfig.FramePacing, double[]> results = new EnumMap<>(ZeroLagConfig.FramePacing.class);
		for (ZeroLagConfig.FramePacing mode : ZeroLagConfig.FramePacing.values()) {
			context.runOnClient(client -> {
				ZeroLagClient.config().framePacing = mode;
				FramePacer.reset();
			});
			context.waitTicks(MEASURE_TICKS);
			double[] sample = context.computeOnClient(client -> new double[] {
				client.getFps(),
				FramePacer.averageInputAgeMillis(),
				FramePacer.averageSleepMillis(),
				FramePacer.averageWaitMillis(),
				FramePacer.marginMillis()
			});
			results.put(mode, sample);
			LOGGER.info(String.format(
				Locale.ROOT,
				"VSync %s, %s: %.0f fps, input to present %.2f ms, sleep %.2f ms, GPU wait %.2f ms, margin %.2f ms, present %s, swapchain images %d",
				vsync ? "on" : "off",
				mode,
				sample[0],
				sample[1],
				sample[2],
				sample[3],
				sample[4],
				SurfaceStatus.presentMode(),
				SurfaceStatus.swapchainImages()
			));
		}

		double[] vanilla = results.get(ZeroLagConfig.FramePacing.OFF);
		double[] smart = results.get(ZeroLagConfig.FramePacing.SMART);
		if (vanilla[3] > 3.0) {
			check(smart[1] < vanilla[1] * 0.8, "smart pacing kept the input as old as vanilla (" + smart[1] + " vs " + vanilla[1] + " ms)");
		}

		context.runOnClient(client -> {
			ZeroLagClient.config().framePacing = ZeroLagConfig.FramePacing.SMART;
			FramePacer.reset();
		});
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
