package com.alcatrazescapee.cyanide.platform;

import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public interface XPlatform
{
    XPlatform INSTANCE = find(XPlatform.class);

    static <T> T find(Class<T> clazz)
    {
        return ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }

    default void postFabricBeforeRegistryLoadEvent(Map<ResourceKey<? extends Registry<?>>, Registry<?>> registryMap) {}

    /**
     * @return {@code true} to skip this registry element from loading
     */
    default boolean checkFabricConditions(JsonElement json, ResourceKey<?> key, HolderLookup.Provider lookup)
    {
        return false;
    }

    default <T> Decoder<Optional<T>> getNeoForgeConditionalCodec(Codec<T> codec)
    {
        return codec.map(Optional::of);
    }
}
