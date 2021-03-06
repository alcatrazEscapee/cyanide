package com.alcatrazescapee.cyanide.platform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import com.alcatrazescapee.cyanide.codec.Codecs;

public final class ForgePlatform implements XPlatform
{
    @Nullable
    @Override
    public MinecraftServer getServer()
    {
        return ServerLifecycleHooks.getCurrentServer();
    }

    @Override
    public Codec<Biome> makeBiomeCodec(Codec<BiomeSpecialEffects> specialEffectsCodec, Codec<PlacedFeature> placedFeatureCodec, MapCodec<BiomeGenerationSettings> biomeGenerationSettingsCodec)
    {
        // Forge AT's ClimateSettings for us
        final MapCodec<Biome.ClimateSettings> climateSettingsCodec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codecs.reporting(Codecs.fromEnum("precipitation", Biome.Precipitation::values).fieldOf("precipitation"), "precipitation").forGetter(Biome.ClimateSettings::precipitation),
            Codecs.reporting(Codec.FLOAT.fieldOf("temperature"), "temperature").forGetter(Biome.ClimateSettings::temperature),
            Codecs.optionalFieldOf(Codecs.fromEnum("temperature modifier", Biome.TemperatureModifier::values), "temperature_modifier", Biome.TemperatureModifier.NONE).forGetter(Biome.ClimateSettings::temperatureModifier),
            Codecs.reporting(Codec.FLOAT.fieldOf("downfall"), "downfall").forGetter(Biome.ClimateSettings::downfall)
        ).apply(instance, Biome.ClimateSettings::new));

        // Add Codecs.reporting() to some fields
        return RecordCodecBuilder.create(instance -> instance.group(
            climateSettingsCodec.forGetter(b -> b.modifiableBiomeInfo().getOriginalBiomeInfo().climateSettings()),
            Codecs.reporting(specialEffectsCodec.fieldOf("effects"), "effects").forGetter(b -> b.modifiableBiomeInfo().getOriginalBiomeInfo().effects()),
            biomeGenerationSettingsCodec.forGetter(Biome::getGenerationSettings),
            MobSpawnSettings.CODEC.forGetter(Biome::getMobSettings)
        ).apply(instance, this::makeBiome));
    }

    private Biome makeBiome(Biome.ClimateSettings climateSettings, BiomeSpecialEffects specialEffects, BiomeGenerationSettings generationSettings, MobSpawnSettings mobSpawnSettings)
    {
        return new Biome.BiomeBuilder()
            .downfall(climateSettings.downfall())
            .temperature(climateSettings.temperature())
            .temperatureAdjustment(climateSettings.temperatureModifier())
            .precipitation(climateSettings.precipitation())
            .specialEffects(specialEffects)
            .generationSettings(generationSettings)
            .mobSpawnSettings(mobSpawnSettings)
            .build();
    }
}
