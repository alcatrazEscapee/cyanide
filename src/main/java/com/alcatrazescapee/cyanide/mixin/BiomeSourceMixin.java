/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin;

import java.util.List;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;

import com.alcatrazescapee.cyanide.codec.BiomeSourceCodec;
import com.alcatrazescapee.cyanide.codec.FeatureCycleDetector;
import com.mojang.serialization.Codec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeSource.class)
public abstract class BiomeSourceMixin
{
    @Shadow @Final @Mutable public static Codec<BiomeSource> CODEC;

    static
    {
        // Wrap the biome source codec with one that handles the exceptions thrown by feature cycles
        CODEC = new BiomeSourceCodec(CODEC);
    }

    /**
     * Replace this with a method that has a much better error tracing, is probably more efficient, and interops with the custom codec to not throw an exception during parsing.
     */
    @Inject(method = "buildFeaturesPerStep", at = @At("HEAD"), cancellable = true)
    private void buildFeaturesPerStepWithAdvancedCycleDetection(List<Biome> biomes, boolean topLevel, CallbackInfoReturnable<List<BiomeSource.StepFeatureData>> cir)
    {
        cir.setReturnValue(FeatureCycleDetector.buildFeaturesPerStep(biomes));
    }
}
