package com.alcatrazescapee.cyanide.platform;

import java.util.Map;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.fabricmc.fabric.impl.registry.sync.DynamicRegistryViewImpl;
import net.fabricmc.fabric.impl.resource.conditions.ResourceConditionsImpl;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import com.alcatrazescapee.cyanide.core.MixinHooks;

public final class FabricPlatform implements XPlatform
{
    /**
     * @see net.fabricmc.fabric.mixin.registry.sync.RegistryLoaderMixin
     */
    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void postFabricBeforeRegistryLoadEvent(Map<ResourceKey<? extends Registry<?>>, Registry<?>> registryMap)
    {
        DynamicRegistrySetupCallback.EVENT.invoker().onRegistrySetup(new DynamicRegistryViewImpl(registryMap));
    }

    /**
     * @see net.fabricmc.fabric.mixin.resource.conditions.RegistryLoaderMixin
     */
    @Override
    @SuppressWarnings("UnstableApiUsage")
    public boolean checkFabricConditions(JsonElement json, ResourceKey<?> key, HolderLookup.Provider lookup)
    {
        return json.isJsonObject() && !ResourceConditionsImpl.applyResourceConditions(json.getAsJsonObject(), key.registry().toString(), key.location(), lookup);
    }
}
