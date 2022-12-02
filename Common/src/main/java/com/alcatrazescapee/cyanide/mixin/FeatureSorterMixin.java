package com.alcatrazescapee.cyanide.mixin;

import java.util.List;
import java.util.function.Function;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import com.alcatrazescapee.cyanide.codec.FeatureCycleDetector;
import com.alcatrazescapee.cyanide.codec.MixinHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FeatureSorter.class)
public abstract class FeatureSorterMixin
{
    /**
     * Replace this with a method that has a much better error tracing and is probably more efficient
     */
    @Inject(method = "buildFeaturesPerStep", at = @At("HEAD"), cancellable = true)
    private static <T> void buildFeaturesPerStepWithAdvancedCycleDetection(List<T> biomes, Function<T, List<HolderSet<PlacedFeature>>> biomeFeatures, boolean topLevel, CallbackInfoReturnable<List<FeatureSorter.StepFeatureData>> cir)
    {
        cir.setReturnValue(FeatureCycleDetector.buildFeaturesPerStep(MixinHooks.cast(biomes), MixinHooks.cast(biomeFeatures)));
    }
}
