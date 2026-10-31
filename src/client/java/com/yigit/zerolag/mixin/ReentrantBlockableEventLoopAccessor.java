package com.yigit.zerolag.mixin;

import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ReentrantBlockableEventLoop.class)
public interface ReentrantBlockableEventLoopAccessor {
	@Invoker("runningTask")
	boolean zerolag$runningTask();
}
