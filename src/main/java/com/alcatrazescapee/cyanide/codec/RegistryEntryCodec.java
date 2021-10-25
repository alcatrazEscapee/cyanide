/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.util.Optional;
import java.util.function.Supplier;

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

/**
 * Based on {@link RegistryFileCodec}
 */
public record RegistryEntryCodec<E>(ResourceKey<? extends Registry<E>> registryKey, Codec<E> elementCodec) implements Codec<Supplier<E>>
{
    @Override
    public <T> DataResult<T> encode(Supplier<E> input, DynamicOps<T> ops, T prefix)
    {
        if (ops instanceof RegistryWriteOps<T> registryOps)
        {
            final RegistryAccess registryAccess = ((RegistryWriteOpsAccessor) registryOps).cyanide$getRegistryAccess();
            final Optional<WritableRegistry<E>> optionalRegistry = registryAccess.ownedRegistry(registryKey);
            if (optionalRegistry.isEmpty())
            {
                return DataResult.error("Unknown registry " + registryKey.location());
            }

            final Optional<ResourceKey<E>> optionalKey = optionalRegistry.get().getResourceKey(input.get());
            if (optionalKey.isPresent())
            {
                return ResourceLocation.CODEC.encode(optionalKey.get().location(), ops, prefix);
            }
        }
        return elementCodec.encode(input.get(), ops, prefix);
    }

    @Override
    public <T> DataResult<Pair<Supplier<E>, T>> decode(DynamicOps<T> ops, T input)
    {
        if (ops instanceof RegistryReadOps<T> registryOps)
        {
            final RegistryAccess registryAccess = ((RegistryReadOpsAccessor) registryOps).cyanide$getRegistryAccess();
            final Optional<WritableRegistry<E>> optionalRegistry = registryAccess.ownedRegistry(registryKey);
            if (optionalRegistry.isEmpty())
            {
                return DataResult.error("Unknown registry " + registryKey);
            }

            @SuppressWarnings("unchecked") final DataResult<Pair<ResourceLocation, T>> optionalResult = ResourceLocation.CODEC.decode(((DelegatingOpsAccessor<T>) registryOps).cyanide$getDelegate(), input);
            if (optionalResult.result().isEmpty())
            {
                return DataResult.error("Cannot decode  " + optionalResult.error().map(Object::toString).orElse("Unknown id?") + " from registry " + registryKey.location());
            }

            final Pair<ResourceLocation, T> result = optionalResult.result().get();
            final ResourceLocation id = result.getFirst();

            DataResult<Supplier<E>> decoded = ((RegistryReadOpsAccessor) registryOps).cyanide$readAndRegisterElement(registryKey, optionalRegistry.get(), elementCodec, id);
            if (decoded.error().isPresent())
            {
                decoded = MixinHooks.appendRegistryEntryErrors(decoded, registryOps, id, registryKey);
            }
            return decoded.map(e -> Pair.of(e, result.getSecond()));
        }
        else
        {
            DataResult<Pair<Supplier<E>, T>> result = elementCodec.decode(ops, input)
                .map(r -> r.mapFirst(e -> () -> e));
            if (result.error().isPresent())
            {
                result = MixinHooks.appendRegistryJsonError(result, input, registryKey);
            }
            return result;
        }
    }

    public String toString()
    {
        return "RegistryEntryCodec[" + registryKey + ']';
    }
}
