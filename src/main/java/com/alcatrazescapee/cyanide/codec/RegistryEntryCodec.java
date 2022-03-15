/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.*;

import com.alcatrazescapee.cyanide.mixin.accessor.DelegatingOpsAccessor;
import com.alcatrazescapee.cyanide.mixin.accessor.RegistryReadOpsAccessor;
import com.alcatrazescapee.cyanide.mixin.accessor.RegistryWriteOpsAccessor;
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
            final RegistryAccess registryAccess = ((RegistryWriteOpsAccessor) registryOps).cyanide$getRegistryAccess();
            final Optional<Registry<E>> optionalRegistry = registryAccess.ownedRegistry(registryKey);
            if (optionalRegistry.isEmpty())
            {
                return DataResult.error("Unknown registry " + registryKey.location());
            }

            final Optional<ResourceKey<E>> optionalKey = optionalRegistry.get().getResourceKey(input.value());
            if (optionalKey.isPresent())
            {
                return ResourceLocation.CODEC.encode(optionalKey.get().location(), ops, prefix);
            }
        }
        return input.unwrap().map((key) -> ResourceLocation.CODEC.encode(key.location(), ops, prefix), (e) -> this.elementCodec.encode(e, ops, prefix));
    }

    @Override
    public <T> DataResult<Pair<Holder<E>, T>> decode(DynamicOps<T> ops, T input)
    {
        if (ops instanceof RegistryOps<T> registryOps)
        {
            final RegistryAccess registryAccess = ((RegistryReadOpsAccessor) registryOps).cyanide$getRegistryAccess();
            final Optional<Registry<E>> optionalRegistry = registryAccess.ownedRegistry(registryKey);
            if (optionalRegistry.isEmpty())
            {
                return DataResult.error("Unknown registry " + registryKey);
            }

            @SuppressWarnings("unchecked") final DataResult<Pair<ResourceLocation, T>> optionalResult = ResourceLocation.CODEC.decode(((DelegatingOpsAccessor<T>) registryOps).cyanide$getDelegate(), input);
            if (optionalResult.result().isPresent())
            {
                final Pair<ResourceLocation, T> result = optionalResult.result().get();
                final ResourceLocation id = result.getFirst();
                final ResourceKey<E> key = ResourceKey.create(registryKey, id);

                Optional<RegistryLoader.Bound> loader = registryOps.registryLoader();
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
                    Holder<E> holder = optionalRegistry.get().getOrCreateHolder(key);
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
