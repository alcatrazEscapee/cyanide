/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import com.alcatrazescapee.cyanide.mixin.accessor.RegistryReadOpsAccessor;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

/**
 * A custom codec for {@link BiomeSource} which is able to take an exception thrown by the delegate codec, and reinterpret it with additional error context.
 */
public record BiomeSourceCodec(Codec<BiomeSource> codec) implements Codec<BiomeSource>
{
    @Override
    public <T> DataResult<Pair<BiomeSource, T>> decode(DynamicOps<T> ops, T input)
    {
        try
        {
            return codec.decode(ops, input);
        }
        catch (FeatureCycleDetector.FeatureCycleException error)
        {
            if (ops instanceof RegistryReadOps registryOps)
            {
                final RegistryAccess registryAccess = ((RegistryReadOpsAccessor) registryOps).cyanide$getRegistryAccess();
                final Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registry.BIOME_REGISTRY);
                final Registry<PlacedFeature> placedFeatureRegistry = registryAccess.registryOrThrow(Registry.PLACED_FEATURE_REGISTRY);
                return DataResult.error(error.messageWithContext(b -> idFor(biomeRegistry, b), f -> idFor(placedFeatureRegistry, f)));
            }
            return DataResult.error(error.getMessage());
        }
    }

    @Override
    public <T> DataResult<T> encode(BiomeSource input, DynamicOps<T> ops, T prefix)
    {
        return codec.encode(input, ops, prefix);
    }

    private <T> String idFor(Registry<T> registry, T element)
    {
        return registry.getResourceKey(element)
            .map(e -> e.location().toString())
            .orElseGet(() -> "[Unknown " + registry.key().location().toString() + ']');
    }
}
