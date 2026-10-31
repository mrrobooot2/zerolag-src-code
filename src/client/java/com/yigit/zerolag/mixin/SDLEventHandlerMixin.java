package com.yigit.zerolag.mixin;

import com.mojang.blaze3d.platform.SDLEventHandler;
import com.mojang.blaze3d.platform.Window;
import com.yigit.zerolag.access.LateInputPoller;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SDLEventHandler.class)
public abstract class SDLEventHandlerMixin implements LateInputPoller {
	@Unique
	private static final int FIRST_INPUT_EVENT = SDLEvents.SDL_EVENT_KEY_DOWN;
	// SDL keeps keyboard events in 0x300-0x3FF and mouse events in 0x400-0x4FF.
	@Unique
	private static final int LAST_INPUT_EVENT = 0x4FF;
	@Unique
	private static final int BATCH_SIZE = 64;

	@Shadow
	@Final
	private Window window;

	@Shadow
	private void handleKeyEvent(final SDL_Event event) {
	}

	@Shadow
	private void handleTextEditingEvent(final SDL_Event event) {
	}

	@Shadow
	private void handleTextInputEvent(final SDL_Event event) {
	}

	@Shadow
	private void handleKeymapChangedEvent() {
	}

	@Shadow
	private void handleMouseMotionEvent(final SDL_Event event) {
	}

	@Shadow
	private void handleMouseButtonEvent(final SDL_Event event) {
	}

	@Shadow
	private void handleMouseWheelEvent(final SDL_Event event) {
	}

	@Override
	public void zerolag$pollInputEvents() {
		SDLEvents.SDL_PumpEvents();
		try (MemoryStack stack = MemoryStack.stackPush()) {
			SDL_Event.Buffer events = SDL_Event.malloc(BATCH_SIZE, stack);
			int count;
			do {
				count = SDLEvents.SDL_PeepEvents(events, SDLEvents.SDL_GETEVENT, FIRST_INPUT_EVENT, LAST_INPUT_EVENT);
				for (int i = 0; i < count; i++) {
					this.zerolag$dispatch(events.get(i));
				}
			} while (count == BATCH_SIZE);
		}
	}

	@Unique
	private void zerolag$dispatch(final SDL_Event event) {
		switch (event.type()) {
			case SDLEvents.SDL_EVENT_KEY_DOWN, SDLEvents.SDL_EVENT_KEY_UP -> this.handleKeyEvent(event);
			case SDLEvents.SDL_EVENT_TEXT_EDITING -> this.handleTextEditingEvent(event);
			case SDLEvents.SDL_EVENT_TEXT_INPUT -> this.handleTextInputEvent(event);
			case SDLEvents.SDL_EVENT_KEYMAP_CHANGED -> this.handleKeymapChangedEvent();
			case SDLEvents.SDL_EVENT_MOUSE_MOTION -> this.handleMouseMotionEvent(event);
			case SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN, SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP -> this.handleMouseButtonEvent(event);
			case SDLEvents.SDL_EVENT_MOUSE_WHEEL -> this.handleMouseWheelEvent(event);
			default -> this.window.handleEvent(event);
		}
	}
}
