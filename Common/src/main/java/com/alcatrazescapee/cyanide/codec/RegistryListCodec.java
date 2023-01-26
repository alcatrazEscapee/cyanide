package com.alcatrazescapee.cyanide.codec;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

/**
 * Based on {@link net.minecraft.resources.HolderSetCodec}
 * Improvements made:
 * - Use {@link Codecs#list(Codec)} and apply flat map earlier for {@link #decodeWithoutRegistry(DynamicOps, Object)}
 * - Use shaped codecs for decoding the either() with tags vs. lists
 */
public final class RegistryListCodec<E> implements Codec<HolderSet<E>>
{
    private final ResourceKey<? extends Registry<E>> registryKey;

    private final Codec<List<Holder<E>>> listCodec;
    private final Codec<List<Holder.Direct<E>>> directListCodec;
    private final Codec<Either<TagKey<E>, List<Holder<E>>>> registryAwareTagOrListCodec;

    public RegistryListCodec(ResourceKey<? extends Registry<E>> registryKey, Codec<Holder<E>> elementCodec, boolean onlyLists)
    {
        this.registryKey = registryKey;

        final Function<List<Holder<E>>, DataResult<List<Holder<E>>>> check = ExtraCodecs.ensureHomogenous(Holder::kind);
        final Codec<List<Holder<E>>> listCodec = Codecs.list(elementCodec)
            .flatXmap(check, check);
        this.listCodec = onlyLists ? listCodec : Codec.either(listCodec, elementCodec)
            .xmap(listOrSingleton -> listOrSingleton.map(list -> list, List::of), list -> list.size() == 1 ? Either.right(list.get(0)) : Either.left(list));
        this.registryAwareTagOrListCodec = Codecs.either(
            ShapedCodec.likeString(TagKey.hashedCodec(registryKey)),
            ShapedCodec.likeList(listCodec)
        );
        this.directListCodec = Codecs.list(elementCodec.comapFlatMap(
            holder -> {
                if (holder instanceof Holder.Direct<E> direct)
                {
                    return DataResult.success(direct);
                }
                return DataResult.error("Can't decode element " + holder + " without registry present");
            },
            holder -> holder
        ));
    }

    @Override
    public <T> DataResult<Pair<HolderSet<E>, T>> decode(DynamicOps<T> ops, T input)
    {
        if (ops instanceof RegistryOps<T> registryOps)
        {
            final Optional<HolderGetter<E>> optionalGetter = registryOps.getter(registryKey);
            if (optionalGetter.isPresent())
            {
                final HolderGetter<E> getter = optionalGetter.get();
                return registryAwareTagOrListCodec.decode(ops, input).map(pair -> pair.mapFirst(either -> either.map(getter::getOrThrow, HolderSet::direct)));
            }
        }
        return decodeWithoutRegistry(ops, input);
    }

    @Override
    public <T> DataResult<T> encode(HolderSet<E> input, DynamicOps<T> ops, T prefix)
    {
        if (ops instanceof RegistryOps<T> registryOps)
        {
            final Optional<HolderOwner<E>> optionalOwner = registryOps.owner(registryKey);
            if (optionalOwner.isPresent())
            {
                if (!input.canSerializeIn(optionalOwner.get()))
                {
                    return DataResult.error("HolderSet " + input + " is not valid in current registry set");
                }
                return this.registryAwareTagOrListCodec.encode(input.unwrap().mapRight(List::copyOf), ops, prefix);
            }
        }
        return encodeWithoutRegistry(input, ops, prefix);
    }

    private <T> DataResult<Pair<HolderSet<E>, T>> decodeWithoutRegistry(DynamicOps<T> ops, T input)
    {
        return directListCodec.decode(ops, input).map(pair -> pair.mapFirst(HolderSet::direct));
    }

    private <T> DataResult<T> encodeWithoutRegistry(HolderSet<E> input, DynamicOps<T> ops, T prefix)
    {
        return this.listCodec.encode(input.stream().toList(), ops, prefix);
    }
}
