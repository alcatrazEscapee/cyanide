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
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.jetbrains.annotations.Nullable;

import com.alcatrazescapee.cyanide.codec.Codecs;
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
        final MapCodec<Biome.ClimateSettings> climateSettingsCodec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codecs.reporting(Codec.BOOL.fieldOf("has_precipitation"), "precipitation").forGetter(c -> MixinHooks.<BiomeClimateSettingsAccessor>cast(c).cyanide$getHasPrecipitation()),
            Codecs.reporting(Codec.FLOAT.fieldOf("temperature"), "temperature").forGetter(c -> MixinHooks.<BiomeClimateSettingsAccessor>cast(c).cyanide$getTemperature()),
            Codecs.optionalFieldOf(Codecs.fromEnum("temperature modifier", Biome.TemperatureModifier::values), "temperature_modifier", Biome.TemperatureModifier.NONE).forGetter(c -> MixinHooks.<BiomeClimateSettingsAccessor>cast(c).cyanide$getTemperatureModifier()),
            Codecs.reporting(Codec.FLOAT.fieldOf("downfall"), "downfall").forGetter(c -> MixinHooks.<BiomeClimateSettingsAccessor>cast(c).cyanide$getDownfall())
        ).apply(instance, BiomeClimateSettingsAccessor::cyanide$new));

        // Add Codecs.reporting() to some fields
        // Use improved enum codec for biome category, and all the above codecs
        return RecordCodecBuilder.create(instance -> instance.group(
            climateSettingsCodec.forGetter(b -> MixinHooks.<BiomeAccessor>cast(b).cyanide$getClimateSettings()),
            Codecs.reporting(specialEffectsCodec.fieldOf("effects"), "effects").forGetter(Biome::getSpecialEffects),
            biomeGenerationSettingsCodec.forGetter(Biome::getGenerationSettings),
            MobSpawnSettings.CODEC.forGetter(Biome::getMobSettings)
        ).apply(instance, BiomeAccessor::cyanide$new));
    }
}
