package com.alcatrazescapee.cyanide.mixin.accessor;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BiomeGenerationSettings.class)
public interface BiomeGenerationSettingsAccessor
{
    @Invoker("<init>")
    static BiomeGenerationSettings cyanide$new(Map<GenerationStep.Carving, HolderSet<ConfiguredWorldCarver<?>>> map, List<HolderSet<PlacedFeature>> list)
    {
        throw new AssertionError();
    }

    @Accessor("carvers")
    Map<GenerationStep.Carving, HolderSet<ConfiguredWorldCarver<?>>> cyanide$getCarvers();
}
