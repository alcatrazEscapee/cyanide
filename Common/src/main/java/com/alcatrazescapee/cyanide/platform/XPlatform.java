package com.alcatrazescapee.cyanide.platform;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import com.alcatrazescapee.cyanide.codec.MixinHooks;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.jetbrains.annotations.Nullable;

public interface XPlatform
{
    XPlatform INSTANCE = find(XPlatform.class);

    static <T> T find(Class<T> clazz)
    {
        return ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }

    /**
     * Mirrors the Fabric API mixin to trigger a callback before registries are loaded
     */
    default void doPreRegistryLoadCallback(List<? extends MixinHooks.RegistryDataPair<?>> registriesList) {}

    /**
     * Mirrors the Forge patch to allow conditions in {@link RegistryDataLoader#loadRegistryContents(RegistryOps.RegistryInfoLookup, ResourceManager, ResourceKey, WritableRegistry, Decoder, Map)}
     */
    default boolean shouldRegisterEntry(JsonElement json)
    {
        return true;
    }

    Codec<Biome> makeBiomeCodec(Codec<BiomeSpecialEffects> specialEffectsCodec, Codec<PlacedFeature> placedFeatureCodec, MapCodec<BiomeGenerationSettings> biomeGenerationSettingsCodec);
}
