package com.alcatrazescapee.cyanide.platform;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.fabricmc.fabric.impl.registry.sync.DynamicRegistryViewImpl;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import com.alcatrazescapee.cyanide.codec.MixinHooks;
import com.alcatrazescapee.cyanide.mixin.accessor.BiomeAccessor;
import com.alcatrazescapee.cyanide.mixin.accessor.BiomeClimateSettingsAccessor;

public final class FabricPlatform implements XPlatform
{
    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void doPreRegistryLoadCallback(List<? extends MixinHooks.RegistryDataPair<?>> registriesList)
    {
        final Map<ResourceKey<? extends Registry<?>>, Registry<?>> registries = new IdentityHashMap<>(registriesList.size());
        for (var pair : registriesList)
        {
            registries.put(pair.registry().key(), pair.registry());
        }

        DynamicRegistrySetupCallback.EVENT.invoker().onRegistrySetup(new DynamicRegistryViewImpl(registries));
    }

    @Override
    public Codec<Biome> makeBiomeCodec(Codec<BiomeSpecialEffects> specialEffectsCodec, Codec<PlacedFeature> placedFeatureCodec, MapCodec<BiomeGenerationSettings> biomeGenerationSettingsCodec)
    {
        // Use improved .optionalFieldOf for temperature modifier, and use an improved enum codec for it.
        // Add Codecs.reporting() to some fields.
        final MapCodec<Biome.ClimateSettings> climateSettingsCodec = RecordCodecBuilder.mapCodec(instance -> {
            Codec<Biome.TemperatureModifier> codec = StringRepresentable.fromEnum(Biome.TemperatureModifier::values);
            MapCodec<Float> codec1 = Codec.FLOAT.fieldOf("downfall");
            MapCodec<Float> codec2 = Codec.FLOAT.fieldOf("temperature");
            MapCodec<Boolean> codec3 = Codec.BOOL.fieldOf("has_precipitation");
            return instance.group(
                codec3.forGetter(c -> MixinHooks.<BiomeClimateSettingsAccessor>cast(c).cyanide$getHasPrecipitation()),
                codec2.forGetter(c -> MixinHooks.<BiomeClimateSettingsAccessor>cast(c).cyanide$getTemperature()),
                codec.optionalFieldOf("temperature_modifier", Biome.TemperatureModifier.NONE).forGetter(c -> MixinHooks.<BiomeClimateSettingsAccessor>cast(c).cyanide$getTemperatureModifier()),
                codec1.forGetter(c -> MixinHooks.<BiomeClimateSettingsAccessor>cast(c).cyanide$getDownfall())
            ).apply(instance, BiomeClimateSettingsAccessor::cyanide$new);
        });

        // Add Codecs.reporting() to some fields
        // Use improved enum codec for biome category, and all the above codecs
        return RecordCodecBuilder.create(instance -> {
            MapCodec<BiomeSpecialEffects> codec = specialEffectsCodec.fieldOf("effects");
            return instance.group(
                climateSettingsCodec.forGetter(b -> MixinHooks.<BiomeAccessor>cast(b).cyanide$getClimateSettings()),
                codec.forGetter(Biome::getSpecialEffects),
                biomeGenerationSettingsCodec.forGetter(Biome::getGenerationSettings),
                MobSpawnSettings.CODEC.forGetter(Biome::getMobSettings)
            ).apply(instance, BiomeAccessor::cyanide$new);
        });
    }
}
