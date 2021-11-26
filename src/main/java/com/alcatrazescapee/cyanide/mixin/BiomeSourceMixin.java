/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin;

import java.util.List;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;

import com.alcatrazescapee.cyanide.codec.MixinHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeSource.class)
public abstract class BiomeSourceMixin
{
    /**
     * Replace this with a method that has a much better error tracing and is probably more efficient
     */
    @Inject(method = "buildFeaturesPerStep", at = @At("HEAD"), cancellable = true)
    private void buildFeaturesPerStepWithAdvancedCycleDetection(List<Biome> biomes, boolean topLevel, CallbackInfoReturnable<List<BiomeSource.StepFeatureData>> cir)
    {
        cir.setReturnValue(MixinHooks.buildFeaturesPerStepAndPopulateErrors(biomes));
    }
}
