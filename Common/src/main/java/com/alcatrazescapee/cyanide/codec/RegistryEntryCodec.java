package com.alcatrazescapee.cyanide.codec;

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
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
            final Optional<HolderOwner<E>> optionalOwner = registryOps.owner(registryKey);
            if (optionalOwner.isPresent())
            {
                final HolderOwner<E> owner = optionalOwner.get();
                if (!input.canSerializeIn(owner))
                {
                    return DataResult.error(() -> "Element - " + input + " - not valid in current registry " + registryKey.location());
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
            final Optional<HolderGetter<E>> optionalGetter = registryOps.getter(registryKey);
            if (optionalGetter.isEmpty())
            {
                return DataResult.error(() -> "Unknown registry " + registryKey);
            }

            final HolderGetter<E> getter = optionalGetter.get();
            final DataResult<Pair<ResourceLocation, T>> optionalResult = ResourceLocation.CODEC.decode(ops, input);
            if (optionalResult.result().isPresent())
            {
                final Pair<ResourceLocation, T> result = optionalResult.result().get();
                final ResourceLocation id = result.getFirst();
                final ResourceKey<E> key = ResourceKey.create(registryKey, id);

                return getter.get(key)
                    .map(DataResult::success)
                    .orElseGet(() -> MixinHooks.appendRegistryReferenceError(DataResult.error(() -> "Missing " + registryKey.location().getPath() + ": " + key.location()), id, registryKey))
                    .<Pair<Holder<E>, T>>map(reference -> Pair.of(reference, result.getSecond()))
                    .setLifecycle(Lifecycle.stable());
            }
        }
        DataResult<Pair<Holder<E>, T>> result = elementCodec.decode(ops, input).map(r -> r.mapFirst(Holder::direct));
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
