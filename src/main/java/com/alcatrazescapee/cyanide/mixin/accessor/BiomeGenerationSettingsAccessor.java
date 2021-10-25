package com.alcatrazescapee.cyanide.mixin.accessor;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.surfacebuilders.ConfiguredSurfaceBuilder;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BiomeGenerationSettings.class)
public interface BiomeGenerationSettingsAccessor
{
    @Invoker("<init>")
    static BiomeGenerationSettings cyanide$new(Supplier<ConfiguredSurfaceBuilder<?>> surfaceBuilder, Map<GenerationStep.Carving, List<Supplier<ConfiguredWorldCarver<?>>>> carvers, List<List<Supplier<ConfiguredFeature<?, ?>>>> features, List<Supplier<ConfiguredStructureFeature<?, ?>>> structures)
    {
        throw new AssertionError();
    }

    @Accessor("carvers")
    Map<GenerationStep.Carving, List<Supplier<ConfiguredWorldCarver<?>>>> cyanide$getCarvers();
}
