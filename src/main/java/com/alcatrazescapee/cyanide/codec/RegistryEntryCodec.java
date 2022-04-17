/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.*;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;

/**
 * Based on {@link RegistryFileCodec}
 */
public record RegistryEntryCodec<E>(ResourceKey<? extends Registry<E>> registryKey, Codec<E> elementCodec) implements Codec<Holder<E>>
{
    @Override
    public <T> DataResult<T> encode(Holder<E> input, DynamicOps<T> ops, T prefix)
    {
        if (ops instanceof RegistryOps<T> registryOps)
        {
            final Optional<? extends Registry<E>> optionalRegistry = registryOps.registry(registryKey);
            if (optionalRegistry.isPresent())
            {
                final Registry<E> registry = optionalRegistry.get();
                if (!input.isValidInRegistry(registry))
                {
                    return DataResult.error("Element - " + input + " - not valid in current registry " + registryKey.location());
                }

                return input.unwrap().map(
                    key -> ResourceLocation.CODEC.encode(key.location(), ops, prefix),
                    e -> this.elementCodec.encode(e, ops, prefix)
                );
            }
        }
        return this.elementCodec.encode(input.value(), ops, prefix);
    }

    @Override
    public <T> DataResult<Pair<Holder<E>, T>> decode(DynamicOps<T> ops, T input)
    {
        if (ops instanceof RegistryOps<T> registryOps)
        {
            final Optional<? extends Registry<E>> optionalRegistry = registryOps.registry(registryKey);
            if (optionalRegistry.isEmpty())
            {
                return DataResult.error("Unknown registry " + registryKey);
            }

            final DataResult<Pair<ResourceLocation, T>> optionalResult = ResourceLocation.CODEC.decode(ops, input);
            if (optionalResult.result().isPresent())
            {
                final Pair<ResourceLocation, T> result = optionalResult.result().get();
                final ResourceLocation id = result.getFirst();
                final ResourceKey<E> key = ResourceKey.create(registryKey, id);

                final Optional<RegistryLoader.Bound> loader = registryOps.registryLoader();
                if (loader.isPresent())
                {
                    DataResult<Holder<E>> decoded = loader.get().overrideElementFromResources(registryKey, elementCodec, key, registryOps.getAsJson());
                    if (decoded.error().isPresent())
                    {
                        decoded = MixinHooks.appendRegistryReferenceError(decoded, id, registryKey);
                    }
                    return decoded.map(value -> Pair.of(value, result.getSecond()));
                }
                else
                {
                    final Holder<E> holder = optionalRegistry.get().getOrCreateHolder(key);
                    return DataResult.success(Pair.of(holder, optionalResult.result().get().getSecond()), Lifecycle.stable());
                }
            }
        }
        DataResult<Pair<Holder<E>, T>> result = elementCodec.decode(ops, input)
            .map(r -> r.mapFirst(Holder::direct));
        if (result.error().isPresent())
        {
            result = MixinHooks.appendRegistryError(result, registryKey);
        }
        return result;
    }

    public String toString()
    {
        return "RegistryEntryCodec[" + registryKey + ']';
    }
}
