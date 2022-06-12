package com.alcatrazescapee.cyanide.mixin.accessor;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Biome.class)
public interface BiomeAccessor
{
    @Invoker("<init>")
    static Biome cyanide$new(Biome.ClimateSettings climateSettings, BiomeSpecialEffects specialEffects, BiomeGenerationSettings generationSettings, MobSpawnSettings mobSettings)
    {
        throw new AssertionError();
    }

    @Accessor("climateSettings")
    Biome.ClimateSettings cyanide$getClimateSettings();
}
