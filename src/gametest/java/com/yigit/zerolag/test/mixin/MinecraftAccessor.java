package com.yigit.zerolag.test.mixin;

import com.mojang.blaze3d.platform.SDLEventHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
	@Accessor("sdlEventHandler")
	SDLEventHandler zerolagtest$sdlEventHandler();
}
