/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.*;

public final class Codecs
{
    /**
     * Improvements for {@link Codec#either(Codec, Codec)} that use shaped codecs where possible and have improved error retention.
     */
    public static <F, S> Codec<Either<F, S>> either(Codec<F> first, Codec<S> second)
    {
        return new ImprovedEitherCodec<>(ShapedCodec.likeAny(first), ShapedCodec.likeAny(second));
    }

    public static <F, S> Codec<Either<F, S>> either(ShapedCodec<F> first, Codec<S> second)
    {
        return new ImprovedEitherCodec<>(first, ShapedCodec.likeAny(second));
    }

    public static <F, S> Codec<Either<F, S>> either(Codec<F> first, ShapedCodec<S> second)
    {
        return new ImprovedEitherCodec<>(ShapedCodec.likeAny(first), second);
    }

    public static <F, S> Codec<Either<F, S>> either(ShapedCodec<F> first, ShapedCodec<S> second)
    {
        return new ImprovedEitherCodec<>(first, second);
    }

    public static <E> Codec<List<E>> list(Codec<E> elementCodec)
    {
        return new ImprovedListCodec<>(elementCodec, (e, i) -> e + " at index " + i);
    }

    public static <E> Codec<List<E>> list(Codec<E> elementCodec, ImprovedListCodec.Reporter indexReporter)
    {
        return new ImprovedListCodec<>(elementCodec, indexReporter);
    }

    public static <E> Codec<E> reporting(Codec<E> codec, String at)
    {
        return new ReportingCodec<>(codec, e -> MixinHooks.appendErrorLocation(e, '"' + at + '"'));
    }

    public static <E> Codec<E> reporting(Codec<E> codec, UnaryOperator<String> errorReporter)
    {
        return new ReportingCodec<>(codec, errorReporter);
    }

    public static <E> MapCodec<E> reporting(MapCodec<E> codec, String at)
    {
        return new ReportingMapCodec<>(codec, e -> MixinHooks.appendErrorLocation(e, '"' + at + '"'));
    }

    public static <E> MapCodec<E> reporting(MapCodec<E> codec, UnaryOperator<String> errorReporter)
    {
        return new ReportingMapCodec<>(codec, errorReporter);
    }

    public static <E> MapCodec<E> optionalFieldOf(Codec<E> codec, String name, E defaultValue) {
        return optionalFieldOf(codec, name).xmap(
            o -> o.orElse(defaultValue),
            a -> Objects.equals(a, defaultValue) ? Optional.empty() : Optional.of(a)
        );
    }

    public static <F> MapCodec<Optional<F>> optionalFieldOf(Codec<F> elementCodec, String name)
    {
        return new ImprovedOptionalCodec<>(name, elementCodec);
    }

    /**
     * Like {@link StringRepresentable#fromEnum(Supplier, Function)} but with named errors.
     */
    public static <E extends Enum<E> & StringRepresentable> Codec<E> fromEnum(String id, Supplier<E[]> enumValues, Function<? super String, ? extends E> enumName)
    {
        final E[] values = enumValues.get();
        return ExtraCodecs.orCompressed(
            Codec.STRING.flatXmap(
                name -> Optional.ofNullable(enumName.apply(name))
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error("Unknown " + id + " name: " + name + ", expected one of [" + Arrays.stream(values).map(StringRepresentable::getSerializedName).collect(Collectors.joining(", ")) + "]")),
                value -> Optional.of(value.getSerializedName())
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error("Unknown name for " + id + ": " + value))),
            ExtraCodecs.idResolverCodec(Enum::ordinal, index -> index >= 0 && index < values.length ? values[index] : null, -1)
        );
    }

    /**
     * Replacement for {@link net.minecraft.resources.RegistryFileCodec#create(ResourceKey, Codec)}
     */
    public static <E> Codec<Holder<E>> registryEntryCodec(ResourceKey<? extends Registry<E>> registryKey, Codec<E> elementCodec)
    {
        return new RegistryEntryCodec<>(registryKey, elementCodec);
    }

    /**
     * Note this is <strong>not</strong> a homogeneous list, and rather accepts mixed lists, prioritizing the registry name.
    */
    public static <E> Codec<HolderSet<E>> registryEntryListCodec(ResourceKey<? extends Registry<E>> registryKey, Codec<E> elementCodec)
    {
        return new RegistryListCodec<>(registryKey, registryEntryCodec(registryKey, elementCodec), false);
    }
}
