package com.yigit.zerolag.mixin;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DeltaTracker.Timer.class)
public interface DeltaTrackerTimerAccessor {
	@Accessor("deltaTickResidual")
	float zerolag$deltaTickResidual();

	@Accessor("lastMs")
	long zerolag$lastMs();

	@Accessor("msPerTick")
	float zerolag$msPerTick();

	@Accessor("targetMsptProvider")
	FloatUnaryOperator zerolag$targetMsptProvider();
}
